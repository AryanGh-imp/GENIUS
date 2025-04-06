package services.file;

import models.account.Account;
import models.account.User;
import models.account.Artist;
import models.account.Admin;
import utils.FileUtil;

import java.io.File;
import java.util.List;

public abstract class FileManager {
    protected static final String DATA_DIR = FileUtil.DATA_DIR;

    protected String findIndexFile(String role) {
        if (role == null) {
            return DATA_DIR + "users/index.txt";
        }
        return switch (role.toLowerCase()) {
            case "user" -> DATA_DIR + "users/index_users.txt";
            case "admin" -> DATA_DIR + "admin/index_admins.txt";
            case "artist" -> DATA_DIR + "artist/index_artists.txt";
            default -> DATA_DIR + "users/index.txt";
        };
    }

    public boolean isEmailOrNickNameTaken(String email, String nickName) {
        String safeNickName = FileUtil.sanitizeFileName(nickName);
        String userDir = DATA_DIR + "users/" + safeNickName + "/";
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        String adminDir = DATA_DIR + "admin/" + safeNickName + "/";
        String userFileName = safeNickName + "-" + email + ".txt";
        return new File(userDir, userFileName).exists() ||
                new File(artistDir, userFileName).exists() ||
                new File(adminDir, userFileName).exists();
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
        File userFile = new File(DATA_DIR + "users/" + safeNickName + "/" + safeNickName + "-" + nickName + ".txt");
        if (userFile.exists()) {
            return loadAccountFromFile(userFile);
        }
        File artistFile = new File(DATA_DIR + "artists/" + safeNickName + "/" + safeNickName + "-" + nickName + ".txt");
        if (artistFile.exists()) {
            return loadAccountFromFile(artistFile);
        }
        File adminFile = new File(DATA_DIR + "admin/" + safeNickName + "/" + safeNickName + "-" + nickName + ".txt");
        if (adminFile.exists()) {
            return loadAccountFromFile(adminFile);
        }
        throw new IllegalStateException("Account with nickname " + nickName + " not found.");
    }

    protected Account loadAccountFromFile(File file) throws IllegalStateException {
        List<String> lines = FileUtil.readFile(file.getPath());
        String email = null, nickName = null, password = null, role = null;
        boolean approved = false;

        for (String line : lines) {
            int index = line.indexOf(": ");
            if (index != -1) {
                String key = line.substring(0, index);
                String value = line.substring(index + 2);
                switch (key) {
                    case "Email": email = value; break;
                    case "Nickname": nickName = value; break;
                    case "Password": password = value; break;
                    case "Role": role = value; break;
                    case "Approved": approved = Boolean.parseBoolean(value); break;
                }
            }
        }

        if (email == null || nickName == null || password == null || role == null) {
            throw new IllegalStateException("Corrupted user data in file: " + file.getPath());
        }

        switch (role.toLowerCase()) {
            case "user":
                return new User(email, nickName, password);
            case "artist":
                Artist artist = new Artist(email, nickName, password);
                artist.setApproved(approved);
                return artist;
            case "admin":
                return new Admin(email, nickName, password);
            default:
                throw new IllegalStateException("Invalid role in file: " + file.getPath());
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
}