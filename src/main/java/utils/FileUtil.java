package utils;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FileUtil {
    public static final String DATA_DIR;
    static {
        String loadedDir = ConfigLoader.getInstance().getDataDirectory();
        if (loadedDir == null || loadedDir.trim().isEmpty()) {
            System.err.println("ConfigLoader returned invalid DATA_DIR, using default: data/");
            DATA_DIR = "data/"; // Fallback to default path
        } else {
            DATA_DIR = loadedDir.endsWith("/") ? loadedDir : loadedDir + "/";
            System.out.println("DATA_DIR set to: " + DATA_DIR); // Debug log
        }
    }
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void ensureDataDirectoryExists(String dir) {
        try {
            Path path = Paths.get(dir);
            Files.createDirectories(path);
            if (!Files.isWritable(path)) {
                throw new IOException("Directory is not writable: " + dir);
            }
            System.out.println("Ensured directory exists and is writable: " + dir);
        } catch (IOException e) {
            System.err.println("Error creating directory: " + dir + " - " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("Failed to create directory: " + dir + " - " + e.getMessage());
            throw new IllegalStateException("Cannot create directory: " + dir, e);
        }
    }

    public static String sanitizeFileName(String name) {
        return name != null ? name.replaceAll("[^a-zA-Z0-9 _.-]", "_") : "";
    }

    public static synchronized List<String> readFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.err.println("File not found: " + fileName);
            throw new IllegalStateException("File not found: " + fileName);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) { // Filtering empty lines
                    lines.add(trimmedLine);
                }
            }
            System.out.println("Read file " + fileName + " with lines: " + lines);
            return lines;
        } catch (IOException e) {
            System.err.println("Error reading file: " + fileName + " - " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("Failed to read file: " + fileName + " - " + e.getMessage());
            throw new IllegalStateException("Cannot read file: " + fileName, e);
        }
    }

    public static synchronized void writeFile(String filePath, List<String> data) {
        try {
            Path path = Paths.get(filePath);
            Path parentDir = path.getParent();
            if (parentDir != null && (!Files.exists(parentDir) || !Files.isWritable(parentDir))) {
                ensureDataDirectoryExists(parentDir.toString());
                if (!Files.isWritable(parentDir)) {
                    throw new IOException("No write permission for directory: " + parentDir);
                }
            }
            Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Successfully wrote to file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing file: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("Failed to write file: " + filePath + " - " + e.getMessage());
            throw new IllegalStateException("Cannot write to file: " + filePath, e);
        }
    }

    public static synchronized void writeFile(String filePath, String data) {
        try {
            Path path = Paths.get(filePath);
            Path parentDir = path.getParent();
            if (parentDir != null && (!Files.exists(parentDir) || !Files.isWritable(parentDir))) {
                ensureDataDirectoryExists(parentDir.toString());
                if (!Files.isWritable(parentDir)) {
                    throw new IOException("No write permission for directory: " + parentDir);
                }
            }
            Files.write(path, data.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Successfully wrote to file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing file: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("Failed to write file: " + filePath + " - " + e.getMessage());
            throw new IllegalStateException("Cannot write to file: " + filePath, e);
        }
    }

    public static boolean deleteDirectory(File directory) {
        try {
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            deleteDirectory(file);
                        } else if (!file.delete()) {
                            throw new IOException("Failed to delete file: " + file.getPath());
                        }
                    }
                }
                if (directory.delete()) {
                    System.out.println("Successfully deleted directory: " + directory.getPath());
                    return true;
                } else {
                    throw new IOException("Failed to delete directory: " + directory.getPath());
                }
            }
            return false;
        } catch (IOException e) {
            System.err.println("Error deleting directory or file " + directory.getPath() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void renameDirectory(File oldDir, File newDir) throws IOException {
        if (!oldDir.exists()) {
            throw new IOException("Source directory does not exist: " + oldDir.getPath());
        }
        if (newDir.exists()) {
            throw new IOException("Destination directory already exists: " + newDir.getPath());
        }
        if (!oldDir.renameTo(newDir)) {
            throw new IOException("Failed to rename directory from " + oldDir.getPath() + " to " + newDir.getPath());
        }
        System.out.println("Successfully renamed directory from " + oldDir.getPath() + " to " + newDir.getPath());
    }

    public static void renameFile(String oldPath, String newPath) throws IOException {
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        if (!oldFile.exists()) {
            throw new IOException("Source file does not exist: " + oldPath);
        }
        if (newFile.exists()) {
            Files.deleteIfExists(newFile.toPath());
        }
        if (!oldFile.renameTo(newFile)) {
            throw new IOException("Failed to rename file from " + oldPath + " to " + newPath);
        }
        System.out.println("Successfully renamed file from " + oldPath + " to " + newPath);
    }

    public static File ensureAndGetFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                ensureDataDirectoryExists(parentDir.getPath());
            }
            if (!file.createNewFile()) {
                throw new IOException("Failed to create file: " + filePath);
            }
            System.out.println("Created new file: " + filePath);
        }
        return file;
    }

    public static synchronized void readAndUpdateFile(String filePath, List<String> updatedData) {
        try {
            writeFile(filePath, updatedData);
            readFile(filePath);
        } catch (Exception e) {
            System.err.println("Failed to update file: " + filePath + " - " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Failed to update file: " + filePath, e);
        }
    }

    public static String extractField(List<String> data, String prefix) {
        System.out.println("Extracting field with prefix: '" + prefix + "' from data: " + data);
        for (String line : data) {
            line = line.trim();
            if (line.startsWith(prefix)) {
                String value = line.substring(prefix.length()).trim();
                System.out.println("Extracted " + prefix + " as: '" + value + "' from line: '" + line + "'");
                return value;
            }
        }
        System.out.println("No " + prefix + " found in data: " + data);
        return null;
    }
}