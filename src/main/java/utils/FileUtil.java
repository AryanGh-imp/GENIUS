package utils;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FileUtil {
    public static final String DATA_DIR = ConfigLoader.getInstance().getDataDirectory();

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void ensureDataDirectoryExists(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            System.err.println("Error creating directory: " + e.getMessage());
            AlertUtil.showError("Failed to create directory: " + dir);
        }
    }

    public static String sanitizeFileName(String name) {
        return name != null ? name.replaceAll("[^a-zA-Z0-9 _.-]", "_") : "";
    }

    public static synchronized List<String> readFile(String fileName) {
        List<String> lines = new ArrayList<>();
        File file = new File(fileName);
        if (!file.exists()) {
            System.err.println("File not found: " + fileName);
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + fileName + " - " + e.getMessage());
            AlertUtil.showError("Failed to read file: " + fileName);
        }
        return lines;
    }

    public static synchronized void writeFile(String filePath, List<String> data) {
        try {
            Files.write(Paths.get(filePath), data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error writing file: " + filePath + " - " + e.getMessage());
            AlertUtil.showError("Failed to write file: " + filePath);
        }
    }

    public static synchronized void writeFile(String filePath, String data) {
        try {
            Files.write(Paths.get(filePath), data.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error writing file: " + filePath + " - " + e.getMessage());
            AlertUtil.showError("Failed to write file: " + filePath);
        }
    }

    public static void deleteDirectory(File directory) {
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
                if (!directory.delete()) {
                    throw new IOException("Failed to delete directory: " + directory.getPath());
                }
            }
        } catch (IOException e) {
            System.err.println("Error deleting directory or file: " + e.getMessage());
        }
    }

    public static File ensureAndGetFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalStateException("File not found: " + filePath);
        }
        return file;
    }

    public static synchronized void readAndUpdateFile(String filePath, List<String> updatedData) {
        try {
            writeFile(filePath, updatedData);
            readFile(filePath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update file: " + filePath, e);
        }
    }
}