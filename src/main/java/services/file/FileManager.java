package services.file;

import models.account.Account;
import models.account.User;
import models.account.Artist;
import models.account.Admin;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static utils.FileUtil.*;

public abstract class FileManager {
    protected static final String DATA_DIR = FileUtil.DATA_DIR;

    static {
        ensureIndexFilesExist();
    }

    protected String findIndexFile(String role) {
        if (role == null) {
            return DATA_DIR + "users/index_users.txt";
        }
        return switch (role.toLowerCase()) {
            case "user" -> DATA_DIR + "users/index_users.txt";
            case "admin" -> DATA_DIR + "admin/index_admins.txt";
            case "artist" -> DATA_DIR + "artists/index_artists.txt";
            default -> DATA_DIR + "users/index_users.txt";
        };
    }

    public static void ensureIndexFilesExist() {
        String[] roles = {"user", "admin", "artist"};
        for (String role : roles) {
            String indexFile = new FileManager() {}.findIndexFile(role);
            File file = new File(indexFile);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                    System.out.println("Created index file: " + indexFile);
                    FileUtil.writeFile(indexFile, new ArrayList<>());
                } catch (IOException e) {
                    System.err.println("Failed to create index file: " + indexFile + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isEmailOrNickNameTaken(String email, String nickName) {
        String[] rolesToCheck = {"user", "artist", "admin"};
        for (String role : rolesToCheck) {
            String indexFile = findIndexFile(role);
            List<String> indexData = FileUtil.readFile(indexFile);
            if (indexData.isEmpty()) {
                continue;
            }
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
        String dir = switch (role != null ? role.toLowerCase() : "user") {
            case "artist" -> DATA_DIR + "artists/" + safeNickName + "/";
            case "admin" -> DATA_DIR + "admin/" + safeNickName + "/";
            default -> DATA_DIR + "users/" + safeNickName + "/";
        };

        FileUtil.ensureDataDirectoryExists(dir);

        try {
            InputStream imageStream = getClass().getResourceAsStream("/pics/user_icon.png");
            if (imageStream == null) {
                System.err.println("user_icon.png not found in resources: /pics/user_icon.png");
            } else {
                Path targetPath = Paths.get(dir + "user_icon.png");
                Files.copy(imageStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Successfully copied user_icon.png to: " + targetPath);
                imageStream.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to copy user_icon.png to " + dir + ": " + e.getMessage());
        }

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
        ensureDataDirectoryExists(dir);
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
        System.out.println("Checking directory: " + requestsDir);

        if (!Files.exists(requestsDir) || !Files.isDirectory(requestsDir)) {
            System.out.println("Directory not found or not a directory: " + dirPath);
            return requests;
        }

        try (Stream<Path> dirStream = Files.walk(requestsDir)) {
            dirStream.forEach(path -> {
                if (Files.isDirectory(path)) {
                    System.out.println("Found directory: " + path);
                    try (Stream<Path> fileStream = Files.list(path)) {
                        List<Path> files = fileStream
                                .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".txt"))
                                .toList();
                        System.out.println("Found " + files.size() + " .txt files in " + path);
                        for (Path file : files) {
                            System.out.println("Processing file: " + file);
                            String[] keys = dirPath.contains("lyrics_requests") ?
                                    new String[]{"Requester: ", "Artist: ", "Song: ", "Album: ", "SuggestedLyrics: ", "Status: ", "Timestamp: "} :
                                    new String[]{"Email: ", "Nickname: ", "Password: ", "Status: ", "Timestamp: "};
                            String[] requestData = parseRequestFile(file, keys);
                            if (requestData != null) {
                                requests.add(requestData);
                                System.out.println("Successfully parsed request: " + Arrays.toString(requestData));
                            } else {
                                System.out.println("Failed to parse request from file: " + file);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to list files in directory " + path + ": " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Failed to walk directory " + dirPath + ": " + e.getMessage());
            throw new RuntimeException("Failed to walk directory " + dirPath + ": " + e.getMessage(), e);
        }
        System.out.println("Total requests loaded from " + dirPath + ": " + requests.size());
        return requests;
    }

    protected String[] parseRequestFile(Path file, String[] keys) {
        List<String> lines = FileUtil.readFile(file.toString());
        System.out.println("Parsing file: " + file + " with lines: " + lines);
        String[] requestData = new String[keys.length];

        for (int i = 0; i < keys.length; i++) {
            requestData[i] = extractField(lines, keys[i]);
            if (requestData[i] == null) {
                System.out.println("Field not found for key: " + keys[i]);
            }
        }

        // Checking the minimum required data
        if (keys[0].startsWith("Email") || keys[0].startsWith("Requester")) {
            if (requestData[0] != null && requestData[1] != null && requestData[keys.length - 1] != null) {
                return requestData;
            }
        }
        System.out.println("Invalid request data format in file: " + file);
        return null;
    }

    protected void moveRequestToDir(List<String> requestData, String targetDirPath, String targetFileName, String newStatus, Path pendingFile) {
        try {
            FileUtil.ensureDataDirectoryExists(targetDirPath);

            List<String> updatedRequestData = new ArrayList<>(requestData);
            updatedRequestData.removeIf(line -> line.startsWith("Status: "));
            updatedRequestData.add("Status: " + newStatus);

            String targetFilePath = targetDirPath + targetFileName;
            Path targetPath = Paths.get(targetFilePath);
            Files.move(pendingFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Successfully moved file from " + pendingFile + " to " + targetPath);

            FileUtil.writeFile(targetFilePath, updatedRequestData);

            if (!Files.exists(pendingFile)) {
                System.out.println("Original file deleted successfully.");
                Path pendingDir = pendingFile.getParent();
                if (Files.exists(pendingDir) && isDirectoryEmpty(pendingDir)) {
                    Files.delete(pendingDir);
                    System.out.println("Successfully deleted empty source directory: " + pendingDir);
                }
            } else {
                System.err.println("Original file still exists after move: " + pendingFile);
            }

        } catch (IOException e) {
            System.err.println("Failed to move request to " + targetDirPath + " with status " + newStatus + ": " + e.getMessage());
            throw new IllegalStateException("Failed to move artist request: " + e.getMessage(), e);
        }
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findFirst().isEmpty();
        }
    }

    protected void deletePendingFileAndDir(Path pendingFile) {
        try {
            if (Files.exists(pendingFile)) {
                Files.deleteIfExists(pendingFile);
                System.out.println("Deleted file: " + pendingFile);
            }

            Path pendingDirPath = pendingFile.getParent();
            File pendingDir = pendingDirPath.toFile();
            File[] files = pendingDir.listFiles();

            if (files != null && files.length == 0) {
                if (deleteDirectory(pendingDir)) {
                    System.out.println("Successfully deleted empty directory: " + pendingDirPath);
                } else {
                    System.err.println("Failed to delete directory " + pendingDirPath + " (possibly due to permissions or system lock)");
                }
            } else if (files != null) {
                System.out.println("Directory " + pendingDirPath + " is not empty, skipping deletion. Remaining files:");
                for (File file : files) {
                    System.out.println(" - " + file.getName());
                }
            } else {
                System.err.println("Unable to list files in directory " + pendingDirPath + ", possibly due to access issues");
            }
        } catch (IOException e) {
            System.err.println("Error while deleting file or directory " + pendingFile + ": " + e.getMessage());
        }
    }

    public String findNickNameByEmail(String email, String role) {
        if (role == null) {
            String[] rolesToCheck = {"user", "artist", "admin"};
            for (String r : rolesToCheck) {
                String nickName = findNickNameByEmail(email, r);
                if (nickName != null) {
                    return nickName;
                }
            }
            return null;
        }

        String indexFile = findIndexFile(role);
        List<String> indexData = FileUtil.readFile(indexFile);
        for (String line : indexData) {
            String[] parts = line.split(":");
            if (parts.length == 2 && parts[0].equals(email)) {
                return parts[1];
            }
        }

        String dir;
        switch (role.toLowerCase()) {
            case "user":
                dir = DATA_DIR + "users/";
                break;
            case "artist":
                dir = DATA_DIR + "artists/";
                break;
            case "admin":
                dir = DATA_DIR + "admin/";
                break;
            default:
                return null;
        }

        try {
            Path roleDir = Paths.get(dir);
            if (!Files.exists(roleDir) || !Files.isDirectory(roleDir)) {
                return null;
            }

            try (Stream<Path> subDirs = Files.list(roleDir).filter(Files::isDirectory)) {
                for (Path subDir : subDirs.toList()) {
                    try (Stream<Path> files = Files.list(subDir)) {
                        for (Path file : files.toList()) {
                            if (file.toString().endsWith(".txt") && !file.getFileName().toString().equals("followers.txt") && !file.getFileName().toString().equals("followings.txt")) {
                                Account account = loadAccountFromFile(file.toFile());
                                if (account.getEmail().equals(email)) {
                                    indexData.add(account.getEmail() + ":" + account.getNickName());
                                    FileUtil.writeFile(indexFile, indexData);
                                    return account.getNickName();
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error while searching for email in directories: " + e.getMessage());
        }
        return null;
    }

    public String findEmailByNickName(String nickName, String role) {
        if (nickName == null || nickName.trim().isEmpty()) {
            System.out.println("Nickname is null or empty in findEmailByNickName");
            return null;
        }

        String indexFile = findIndexFile(role);
        List<String> indexData = FileUtil.readFile(indexFile);
        System.out.println("Read file " + indexFile + " with lines: " + indexData);

        for (String line : indexData) {
            line = line.trim();
            if (!line.contains(":")) continue;

            String[] parts = line.split(":");
            if (parts.length != 2) {
                System.out.println("Invalid format in index file line: " + line);
                continue;
            }

            String email = parts[0].trim();
            String storedNickname = parts[1].trim();

            if (email.startsWith("[")) email = email.substring(1);
            if (email.endsWith("]")) email = email.substring(0, email.length() - 1);
            if (storedNickname.startsWith("[")) storedNickname = storedNickname.substring(1);
            if (storedNickname.endsWith("]")) storedNickname = storedNickname.substring(0, storedNickname.length() - 1);

            System.out.println("Comparing nickname: '" + nickName.trim() + "' with stored: '" + storedNickname + "'");
            if (storedNickname.equalsIgnoreCase(nickName.trim())) {
                System.out.println("Found email: " + email + " for nickname: " + nickName);
                return email;
            }
        }

        String dir;
        switch (role.toLowerCase()) {
            case "user":
                dir = DATA_DIR + "users/";
                break;
            case "artist":
                dir = DATA_DIR + "artists/";
                break;
            case "admin":
                dir = DATA_DIR + "admin/";
                break;
            default:
                return null;
        }

        try {
            Path roleDir = Paths.get(dir);
            if (!Files.exists(roleDir) || !Files.isDirectory(roleDir)) {
                return null;
            }

            try (Stream<Path> subDirs = Files.list(roleDir).filter(Files::isDirectory)) {
                for (Path subDir : subDirs.toList()) {
                    if (subDir.getFileName().toString().equals(FileUtil.sanitizeFileName(nickName))) {
                        try (Stream<Path> files = Files.list(subDir)) {
                            for (Path file : files.toList()) {
                                if (file.toString().endsWith(".txt") && !file.getFileName().toString().equals("followers.txt") && !file.getFileName().toString().equals("followings.txt")) {
                                    Account account = loadAccountFromFile(file.toFile());
                                    if (account.getNickName().equals(nickName)) {
                                        indexData.add(account.getEmail() + ":" + account.getNickName());
                                        FileUtil.writeFile(indexFile, indexData);
                                        return account.getEmail();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error while searching for nickname in directories: " + e.getMessage());
        }
        System.out.println("No email found for nickname: " + nickName + " in role: " + role);
        return null;
    }

    protected void validateInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}