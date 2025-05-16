package services.file;

import models.account.Account;
import models.account.User;
import models.account.Artist;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;

import static utils.FileUtil.*;

public class UserFileManager extends FileManager {
    private static final String USERS_DIR = DATA_DIR + "users/";
    private static final String FOLLOWINGS_FILE_NAME = "followings.txt";
    private static final String FOLLOWING_PREFIX = "Following:";

    private Map<String, User> userMapCache;

    @Override
    public synchronized void saveAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (!account.getRole().equalsIgnoreCase("user")) {
            throw new IllegalArgumentException("UserFileManager can only save users, not " + account.getRole());
        }
        if (isEmailOrNickNameTaken(account.getEmail(), account.getNickName())) {
            throw new IllegalStateException("This email or nickname is already registered. Please try another one.");
        }

        try {
            super.saveAccount(account);
            String userDir = getUserDir(account.getNickName());
            ensureDataDirectoryExists(userDir);
            saveFollowingArtists(account.getNickName(), new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Failed to save account for user: " + account.getNickName() + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to save account", e);
        }
    }

    public synchronized void saveFollowingArtists(String nickName, List<Artist> followingArtists) {
        validateInput(nickName, "Nickname");
        if (followingArtists == null) {
            throw new IllegalArgumentException("Following artists list cannot be null");
        }

        try {
            String filePath = getFollowingsFilePath(nickName);
            List<String> data = new ArrayList<>();
            if (followingArtists.isEmpty()) {
                data.add(FOLLOWING_PREFIX + " (None)");
            } else {
                String artistNames = followingArtists.stream()
                        .map(Artist::getNickName)
                        .collect(Collectors.joining(","));
                data.add(FOLLOWING_PREFIX + artistNames);
            }
            writeFile(filePath, data);
            invalidateUserCache();
        } catch (Exception e) {
            System.err.println("Failed to save following artists for user: " + nickName + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to save following artists", e);
        }
    }

    public List<Artist> loadFollowingArtistsFromFile(String nickName, List<Artist> allArtists) {
        validateInput(nickName, "Nickname");
        String filePath = getFollowingsFilePath(nickName);
        if (!Files.exists(Paths.get(filePath))) {
            return new ArrayList<>();
        }

        try {
            if (allArtists == null || allArtists.isEmpty()) {
                System.out.println("No artists provided for user: " + nickName + ", returning empty list");
                return new ArrayList<>();
            }

            Map<String, Artist> artistMap = allArtists.stream()
                    .collect(Collectors.toMap(Artist::getNickName, a -> a, (a1, a2) -> a1));

            List<String> artistData = readFile(filePath);
            List<Artist> followingArtists = new ArrayList<>();
            for (String line : artistData) {
                if (line.startsWith(FOLLOWING_PREFIX)) {
                    String[] artistNames = line.replace(FOLLOWING_PREFIX, "").split(",");
                    for (String artistName : artistNames) {
                        String trimmedName = artistName.trim();
                        if (!trimmedName.isEmpty() && !trimmedName.equals("(None)")) {
                            Artist artist = artistMap.get(trimmedName);
                            if (artist != null) {
                                followingArtists.add(artist);
                            } else {
                                System.err.println("Artist not found: " + trimmedName + " for user: " + nickName);
                            }
                        }
                    }
                }
            }
            return followingArtists;
        } catch (Exception e) {
            System.err.println("Failed to load following artists for user: " + nickName + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to load following artists", e);
        }
    }

    public List<User> loadAllUsers() {
        Map<String, User> userMap = getUserMap();
        return new ArrayList<>(userMap.values());
    }

    private Map<String, User> getUserMap() {
        if (userMapCache == null) {
            try {
                List<User> users = new ArrayList<>();
                File usersDir = new File(USERS_DIR);
                if (!usersDir.exists() || !usersDir.isDirectory()) {
                    System.err.println("Users directory does not exist or is not a directory: " + USERS_DIR);
                } else {
                    File[] userDirs = usersDir.listFiles(File::isDirectory);
                    if (userDirs == null) {
                        System.err.println("Failed to list user directories: " + USERS_DIR);
                        throw new IllegalStateException("Failed to list user directories: " + USERS_DIR);
                    }

                    for (File userDir : userDirs) {
                        File[] userFiles = userDir.listFiles((d, name) -> name.endsWith(".txt") && !name.equals(FOLLOWINGS_FILE_NAME));
                        if (userFiles == null) {
                            System.err.println("Failed to list files in user directory: " + userDir.getPath());
                            throw new IllegalStateException("Failed to list files in user directory: " + userDir.getPath());
                        }

                        for (File file : userFiles) {
                            try {
                                Account account = loadAccountFromFile(file);
                                if (account instanceof User user) {
                                    users.add(user);
                                }
                            } catch (IllegalStateException e) {
                                System.err.println("Failed to load user from file: " + file.getPath() + ", error: " + e.getMessage());
                            }
                        }
                    }
                }
                userMapCache = users.stream()
                        .collect(Collectors.toMap(User::getNickName, u -> u, (u1, u2) -> u1));
                System.out.println("User map cache initialized with " + userMapCache.size() + " users");
            } catch (Exception e) {
                System.err.println("Failed to load users for cache: " + e.getMessage());
                throw new RuntimeException("Failed to load users", e);
            }
        }
        return userMapCache;
    }

    public void invalidateUserCache() {
        userMapCache = null;
        System.out.println("User cache invalidated");
    }

    private String getUserDir(String nickName) {
        String safeNickName = sanitizeFileName(nickName);
        return USERS_DIR + safeNickName + "/";
    }

    private String getFollowingsFilePath(String nickName) {
        String userDir = getUserDir(nickName);
        ensureDataDirectoryExists(userDir);
        return userDir + FOLLOWINGS_FILE_NAME;
    }
}