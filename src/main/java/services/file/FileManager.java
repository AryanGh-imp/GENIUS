package services.file;

import models.account.Account;
import models.account.User;
import models.account.Artist;
import models.account.Admin;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static utils.FileUtil.deleteDirectory;
import static utils.FileUtil.extractField;

public abstract class FileManager {
    protected static final String DATA_DIR = FileUtil.DATA_DIR;

    protected String findIndexFile(String role) {
        if (role == null) {
            return DATA_DIR + "users/index.txt";
        }
        return switch (role.toLowerCase()) {
            case "user" -> DATA_DIR + "users/index_users.txt";
            case "admin" -> DATA_DIR + "admin/index_admins.txt";
            case "artist" -> DATA_DIR + "artists/index_artists.txt";
            default -> DATA_DIR + "users/index.txt";
        };
    }

    public boolean isEmailOrNickNameTaken(String email, String nickName) {
        String[] rolesToCheck = {"user", "artist", "admin"};
        for (String role : rolesToCheck) {
            String indexFile = findIndexFile(role);
            List<String> indexData = FileUtil.readFile(indexFile);
            for (String line : indexData) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2 && (parts[0].trim().equals(email) || parts[1].trim().equals(nickName))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void saveAccount(Account account) throws RuntimeException {
        String safeNickName = FileUtil.sanitizeFileName(account.getNickName());
        String role = account.getRole();
        String dir = switch (role.toLowerCase()) {
            case "artist" -> DATA_DIR + "artists/" + safeNickName + "/";
            case "admin" -> DATA_DIR + "admin/" + safeNickName + "/";
            default -> DATA_DIR + "users/" + safeNickName + "/";
        };

        FileUtil.ensureDataDirectoryExists(dir);
        String fileName = dir + safeNickName + "-" + account.getEmail() + ".txt";

        List<String> accountData = account.toFileString();
        FileUtil.writeFile(fileName, accountData);

        String indexFile = findIndexFile(role);
        List<String> indexData = FileUtil.readFile(indexFile);
        indexData.add(account.getEmail() + ":" + safeNickName);
        FileUtil.writeFile(indexFile, indexData);
    }

    public Account loadAccountByNickName(String nickName) throws IllegalStateException {
        String safeNickName = FileUtil.sanitizeFileName(nickName);

        String email = null;
        String role = null;
        String[] rolesToCheck = {"user", "artist", "admin"};
        for (String r : rolesToCheck) {
            email = findEmailByNickName(nickName, r);
            if (email != null) {
                role = r;
                break;
            }
        }

        if (email == null) {
            throw new IllegalStateException("Account with nickname '" + nickName + "' not found in any index file.");
        }

        String dir = switch (role.toLowerCase()) {
            case "artist" -> DATA_DIR + "artists/" + safeNickName + "/";
            case "admin" -> DATA_DIR + "admin/" + safeNickName + "/";
            default -> DATA_DIR + "users/" + safeNickName + "/";
        };

        File accountDir = new File(dir);
        if (!accountDir.exists() || !accountDir.isDirectory()) {
            throw new IllegalStateException("Directory not found for account with nickname '" + nickName + "': " + dir);
        }

        String fileName = safeNickName + "-" + email + ".txt";
        File accountFile = new File(dir, fileName);
        if (!accountFile.exists()) {
            throw new IllegalStateException("Account file not found for nickname '" + nickName + "' at: " + accountFile.getPath());
        }

        return loadAccountFromFile(accountFile);
    }

    protected Account loadAccountFromFile(File file) throws IllegalStateException {
        List<String> lines = FileUtil.readFile(file.getPath());
        String[] keys = {"Email", "Nickname", "Password", "Role", "Approved"};
        String[] values = new String[keys.length];
        boolean approved = false;

        for (int i = 0; i < keys.length; i++) {
            values[i] = extractField(lines, keys[i] + ": ");
        }
        if (values[4] != null) {
            approved = Boolean.parseBoolean(values[4]);
        }

        if (values[0] == null || values[1] == null || values[2] == null || values[3] == null) {
            throw new IllegalStateException("Corrupted account data in file: " + file.getPath());
        }

        String email = values[0], nickName = values[1], password = values[2], role = values[3];
        return switch (role.toLowerCase()) {
            case "user" -> new User(email, nickName, password);
            case "artist" -> {
                Artist artist = new Artist(email, nickName, password);
                artist.setApproved(approved);
                yield artist;
            }
            case "admin" -> new Admin(email, nickName, password);
            default -> throw new IllegalStateException("Invalid role in file: " + file.getPath());
        };
    }

    protected List<String[]> loadRequestsFromDir(String dirPath) {
        List<String[]> requests = new ArrayList<>();
        Path requestsDir = Paths.get(dirPath);
        if (!Files.exists(requestsDir) || !Files.isDirectory(requestsDir)) {
            return requests;
        }

        try (Stream<Path> dirStream = Files.list(requestsDir)) {
            dirStream
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        try (Stream<Path> fileStream = Files.list(dir)) {
                            fileStream
                                    .filter(path -> path.toString().endsWith(".txt"))
                                    .forEach(file -> {
                                        String[] requestData = parseRequestFile(file, new String[]{"Email: ", "Nickname: ", "Password: ", "Status: ", "Timestamp: "});
                                        if (requestData != null) {
                                            requests.add(requestData);
                                        }
                                    });
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to list files in directory " + dir + ": " + e.getMessage(), e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list directories in " + dirPath + ": " + e.getMessage(), e);
        }
        return requests;
    }

    protected String[] parseRequestFile(Path file, String[] keys) {
        List<String> lines = FileUtil.readFile(file.toString());
        String[] requestData = new String[keys.length];

        for (int i = 0; i < keys.length; i++) {
            requestData[i] = extractField(lines, keys[i]);
        }

        if (requestData[0] != null && requestData[1] != null) {
            return requestData;
        }
        return null;
    }

    protected void moveRequestToDir(List<String> requestData, String targetDirPath, String targetFileName, String newStatus, Path pendingFile) {
        FileUtil.ensureDataDirectoryExists(targetDirPath);
        String targetFilePath = targetDirPath + targetFileName;

        List<String> updatedRequestData = new ArrayList<>(requestData);
        updatedRequestData.removeIf(line -> line.startsWith("Status: "));
        updatedRequestData.add("Status: " + newStatus);
        FileUtil.writeFile(targetFilePath, updatedRequestData);

        deletePendingFileAndDir(pendingFile);
    }

    protected void deletePendingFileAndDir(Path pendingFile) {
        Path pendingDirPath = pendingFile.getParent();
        try {
            Files.deleteIfExists(pendingFile);

            File pendingDir = pendingDirPath.toFile();
            File[] files = pendingDir.listFiles();
            if (files == null || files.length == 0) {
                deleteDirectory(pendingDir);
            } else {
                System.err.println("Warning: Directory " + pendingDirPath + " is not empty after deleting the request file. Remaining files: ");
                for (File file : files) {
                    System.err.println(" - " + file);
                }
            }
        } catch (IOException e) {
            System.err.println("Error while deleting file or directory: " + pendingFile + " - " + e.getMessage());
        }
    }

    public String findNickNameByEmail(String email, String role) {
        String indexFile = findIndexFile(role);
        List<String> indexData = FileUtil.readFile(indexFile);
        for (String line : indexData) {
            String[] parts = line.split(":");
            if (parts.length == 2 && parts[0].equals(email)) {
                return parts[1];
            }
        }
        return null;
    }

    private String findEmailByNickName(String nickName, String role) {
        String indexFile = findIndexFile(role);
        List<String> indexData = FileUtil.readFile(indexFile);
        for (String line : indexData) {
            String[] parts = line.split(":");
            if (parts.length == 2 && parts[1].equals(nickName)) {
                return parts[0];
            }
        }
        return null;
    }

    protected void validateInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}