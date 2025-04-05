package services.file;

import models.account.Account;
import models.account.Artist;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static utils.FileUtil.*;

public class UserFileManager extends FileManager {
    private static final String FOLLOWINGS_FILE_NAME = "followings.txt";
    private static final String FOLLOWING_PREFIX = "Following:";
    private static Map<String, Artist> artistMap;

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

        super.saveAccount(account);

        String safeNickName = sanitizeFileName(account.getNickName());
        saveFollowingArtists(safeNickName, new ArrayList<>());
    }


    public static synchronized void saveFollowingArtists(String nickName, List<Artist> followingArtists) {
        if (nickName == null || nickName.isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (followingArtists == null) {
            throw new IllegalArgumentException("Following artists list cannot be null");
        }

        String safeNickName = sanitizeFileName(nickName);
        String fileName = DATA_DIR + "users/" + safeNickName + "/" + FOLLOWINGS_FILE_NAME;
        ensureDataDirectoryExists(DATA_DIR + "users/" + safeNickName + "/");

        List<String> data = new ArrayList<>();
        if (followingArtists.isEmpty()) {
            data.add(FOLLOWING_PREFIX + " (None)");
        } else {
            data.add(FOLLOWING_PREFIX + followingArtists.stream()
                    .map(Artist::getNickName)
                    .collect(Collectors.joining(",")));
        }
        writeFile(fileName, data);
    }

    public static void initializeArtistMap(List<Artist> allArtists) {
        if (allArtists == null) {
            throw new IllegalArgumentException("All artists list cannot be null");
        }
        artistMap = allArtists.stream()
                .collect(Collectors.toMap(Artist::getNickName, a -> a, (a1, a2) -> a1));
    }

    public static List<Artist> loadFollowingArtists(String nickName, List<Artist> allArtists) {
        if (nickName == null || nickName.isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (allArtists == null) {
            throw new IllegalArgumentException("All artists list cannot be null");
        }

        if (artistMap == null) {
            initializeArtistMap(allArtists);
        }

        String safeNickName = sanitizeFileName(nickName);
        String fileName = DATA_DIR + "users/" + safeNickName + "/" + FOLLOWINGS_FILE_NAME;
        File followFile = new File(fileName);
        if (!followFile.exists()) {
            return new ArrayList<>();
        }

        List<String> userData = readFile(fileName);
        List<Artist> followingArtists = new ArrayList<>();

        for (String line : userData) {
            if (line.startsWith(FOLLOWING_PREFIX)) {
                String[] artistNames = line.replace(FOLLOWING_PREFIX, "").split(",");
                for (String artistName : artistNames) {
                    String trimmedName = artistName.trim();
                    if (!trimmedName.isEmpty() && !" (None)".equals(trimmedName)) {
                        Artist artist = artistMap.get(trimmedName);
                        if (artist != null) {
                            followingArtists.add(artist);
                        }
                    }
                }
            }
        }
        return followingArtists;
    }
}
