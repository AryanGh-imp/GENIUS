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
                String[] parts = line.split(":");
                if (parts.length == 2 && (parts[0].equals(email) || parts[1].equals(nickName))) {
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
        String email = null, nickName = null, password = null, role = null;
        boolean approved = false;

        for (String line : lines) {
            int index = line.indexOf(": ");
            if (index != -1) {
                String key = line.substring(0, index);
                String value = line.substring(index + 2);
                switch (key) {
                    case "Email" -> email = value;
                    case "Nickname" -> nickName = value;
                    case "Password" -> password = value;
                    case "Role" -> role = value;
                    case "Approved" -> approved = Boolean.parseBoolean(value);
                }
            }
        }

        if (email == null || nickName == null || password == null || role == null) {
            throw new IllegalStateException("Corrupted account data in file: " + file.getPath());
        }

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
}