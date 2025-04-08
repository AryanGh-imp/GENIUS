package services.file;

import models.account.Account;
import models.account.User;
import models.account.Artist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static utils.FileUtil.*;

public class ArtistFileManager extends FileManager {
    private static final String FOLLOWERS_FILE_NAME = "followers.txt";
    private static final String FOLLOWERS_PREFIX = "Followers:";
    private final LyricsRequestManager lyricsRequestManager = new LyricsRequestManager();
    private UserFileManager userFileManager;

    /**
     * Sets the UserFileManager instance.
     *
     * @param userFileManager The UserFileManager instance.
     */
    public void setUserFileManager(UserFileManager userFileManager) {
        this.userFileManager = userFileManager;
    }

    @Override
    public synchronized void saveAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (!account.getRole().equalsIgnoreCase("artist")) {
            throw new IllegalArgumentException("ArtistFileManager can only save artists, not " + account.getRole());
        }

        if (isEmailOrNickNameTaken(account.getEmail(), account.getNickName())) {
            throw new IllegalStateException("This email or nickname is already registered. Please try another one.");
        }

        super.saveAccount(account);

        String safeNickName = sanitizeFileName(account.getNickName());
        String artistDir = DATA_DIR + "artists/" + safeNickName + "/";
        ensureDataDirectoryExists(artistDir + "singles/");
        ensureDataDirectoryExists(artistDir + "albums/");
        saveFollowers((Artist) account, new ArrayList<>());
    }

    public List<Artist> loadAllArtists() {
        List<Artist> artists = new ArrayList<>();
        String artistsDir = DATA_DIR + "artists/";
        File dir = new File(artistsDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Artists directory does not exist or is not a directory: " + artistsDir);
            return artists;
        }

        File[] artistDirs = dir.listFiles(File::isDirectory);
        if (artistDirs == null) {
            System.err.println("No artist directories found in: " + artistsDir);
            return artists;
        }

        for (File artistDir : artistDirs) {
            File[] artistFiles = artistDir.listFiles((d, name) -> name.endsWith(".txt"));
            if (artistFiles == null) {
                System.err.println("No files found in artist directory: " + artistDir.getPath());
                continue;
            }

            for (File file : artistFiles) {
                if (!file.getName().equals(FOLLOWERS_FILE_NAME)) {
                    try {
                        Account account = loadAccountFromFile(file);
                        if (account instanceof Artist artist) {
                            artists.add(artist);
                        }
                    } catch (IllegalStateException e) {
                        System.err.println("Failed to load artist from file: " + file.getPath() + " - " + e.getMessage());
                    }
                }
            }
        }
        return artists;
    }

    public synchronized void saveFollowers(Artist artist, List<User> followers) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (followers == null) {
            throw new IllegalArgumentException("Followers list cannot be null");
        }

        String safeNickName = sanitizeFileName(artist.getNickName());
        String fileName = DATA_DIR + "artists/" + safeNickName + "/" + FOLLOWERS_FILE_NAME;
        ensureDataDirectoryExists(DATA_DIR + "artists/" + safeNickName + "/");

        List<String> data = new ArrayList<>();
        if (followers.isEmpty()) {
            data.add(FOLLOWERS_PREFIX + " (None)");
        } else {
            data.add(FOLLOWERS_PREFIX + followers.stream()
                    .map(User::getNickName)
                    .collect(Collectors.joining(",")));
        }
        writeFile(fileName, data);
    }

    public List<User> loadFollowers(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (userFileManager == null) {
            throw new IllegalStateException("UserFileManager is not set in ArtistFileManager.");
        }

        List<User> allUsers = userFileManager.loadAllUsers();
        var userMap = allUsers.stream()
                .collect(Collectors.toMap(User::getNickName, u -> u, (u1, u2) -> u1));

        String safeNickName = sanitizeFileName(artist.getNickName());
        String fileName = DATA_DIR + "artists/" + safeNickName + "/" + FOLLOWERS_FILE_NAME;
        File followFile = new File(fileName);
        if (!followFile.exists()) {
            return new ArrayList<>();
        }

        List<String> userData = readFile(fileName);
        List<User> followers = new ArrayList<>();

        for (String line : userData) {
            if (line.startsWith(FOLLOWERS_PREFIX)) {
                String[] followerNames = line.replace(FOLLOWERS_PREFIX, "").split(",");
                for (String followerName : followerNames) {
                    String trimmedName = followerName.trim();
                    if (!trimmedName.isEmpty() && !" (None)".equals(trimmedName)) {
                        User user = userMap.get(trimmedName);
                        if (user != null) {
                            followers.add(user);
                        }
                    }
                }
            }
        }
        return followers;
    }

    public List<String[]> getLyricsEditRequests(String artistNickName) {
        return lyricsRequestManager.loadLyricsEditRequestsForArtist(artistNickName);
    }

    public void approveLyricsEditRequest(String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName) {
        lyricsRequestManager.approveLyricsEditRequest(artistNickName, songTitle, timestamp, suggestedLyrics, albumName);
    }

    public void rejectLyricsEditRequest(String artistNickName, String songTitle, String timestamp) {
        lyricsRequestManager.rejectLyricsEditRequest(artistNickName, songTitle, timestamp);
    }
}