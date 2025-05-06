package services.file;

import models.account.Account;
import models.account.User;
import models.account.Artist;
import utils.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static utils.FileUtil.*;

public class ArtistFileManager extends FileManager {
    private static final String ARTISTS_DIR = DATA_DIR + "artists/";
    private static final String ARTISTS_INDEX_FILE = ARTISTS_DIR + "index_artists.txt";
    private static final String FOLLOWERS_FILE_NAME = "followers.txt";
    private static final String FOLLOWERS_PREFIX = "Followers: ";

    private final LyricsRequestManager lyricsRequestManager = new LyricsRequestManager();
    private Map<String, Artist> artistMapCache;

    @Override
    public synchronized void saveAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (!account.getRole().equalsIgnoreCase("artist")) {
            throw new IllegalArgumentException("ArtistFileManager can only save artists, not " + account.getRole());
        }
        if (isEmailOrNickNameTaken(account.getEmail(), account.getNickName())) {
            throw new IllegalStateException("Email or nickname already registered: " + account.getEmail() + "/" + account.getNickName());
        }

        try {
            super.saveAccount(account);
            String safeNickName = sanitizeFileName(account.getNickName());
            String artistDir = ARTISTS_DIR + safeNickName + "/";
            ensureDataDirectoryExists(artistDir + "singles/");
            ensureDataDirectoryExists(artistDir + "albums/");
            saveFollowers((Artist) account, new ArrayList<>());
            updateArtistIndex();
            invalidateArtistCache();
        } catch (Exception e) {
            System.err.println("Failed to save account for artist: " + account.getNickName() + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to save account", e);
        }
    }

    public List<Artist> loadAllArtists() {
        Map<String, Artist> artistMap = getArtistMap();
        return new ArrayList<>(artistMap.values());
    }

    public synchronized void saveFollowers(Artist artist, List<User> followers) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (followers == null) {
            throw new IllegalArgumentException("Followers list cannot be null");
        }

        try {
            String safeNickName = sanitizeFileName(artist.getNickName());
            String fileName = ARTISTS_DIR + safeNickName + "/" + FOLLOWERS_FILE_NAME;
            ensureDataDirectoryExists(ARTISTS_DIR + safeNickName + "/");

            List<String> data = new ArrayList<>();
            String followersLine = followers.isEmpty() ? FOLLOWERS_PREFIX + "(None)" :
                    FOLLOWERS_PREFIX + followers.stream().map(User::getNickName).collect(Collectors.joining(","));
            data.add(followersLine);
            writeFile(fileName, data);
            invalidateArtistCache();
        } catch (Exception e) {
            System.err.println("Failed to save followers for artist: " + artist.getNickName() + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to save followers", e);
        }
    }

    private Map<String, Artist> getArtistMap() {
        if (artistMapCache == null) {
            try {
                List<Artist> artists = new ArrayList<>();
                Path artistsDir = Paths.get(ARTISTS_DIR);

                if (!Files.exists(artistsDir) || !Files.isDirectory(artistsDir)) {
                    System.err.println("Artists directory does not exist or is not a directory: " + ARTISTS_DIR);
                } else {
                    try (Stream<Path> artistDirs = Files.list(artistsDir).filter(Files::isDirectory)) {
                        artistDirs.forEach(artistDir -> {
                            try (Stream<Path> artistFiles = Files.list(artistDir)) {
                                artistFiles
                                        .filter(file -> file.toString().endsWith(".txt") && !file.getFileName().toString().equals(FOLLOWERS_FILE_NAME))
                                        .forEach(file -> {
                                            try {
                                                Account account = loadAccountFromFile(file.toFile());
                                                if (account instanceof Artist artist) {
                                                    artists.add(artist);
                                                }
                                            } catch (IllegalStateException e) {
                                                System.err.println("Failed to load artist from file: " + file + ", error: " + e.getMessage());
                                            }
                                        });
                            } catch (IOException e) {
                                System.err.println("Failed to list files in directory: " + artistDir + ", error: " + e.getMessage());
                            }
                        });
                    } catch (IOException e) {
                        System.err.println("Failed to list artist directories: " + ARTISTS_DIR + ", error: " + e.getMessage());
                    }
                }
                artistMapCache = artists.stream()
                        .collect(Collectors.toMap(Artist::getNickName, a -> a, (a1, a2) -> a1));
                System.err.println("Artist map cache initialized with " + artistMapCache.size() + " artists");
            } catch (Exception e) {
                System.err.println("Failed to load artists for cache: " + e.getMessage());
                throw new RuntimeException("Failed to load artists", e);
            }
        }
        return artistMapCache;
    }

    public void invalidateArtistCache() {
        artistMapCache = null;
        System.err.println("Artist cache invalidated");
    }

    public List<User> loadFollowers(Artist artist, List<User> allUsers) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (allUsers == null || allUsers.isEmpty()) {
            System.err.println("No users provided for artist: " + artist.getNickName() + ", returning empty list");
            return new ArrayList<>();
        }

        String safeNickName = sanitizeFileName(artist.getNickName());
        String fileName = ARTISTS_DIR + safeNickName + "/" + FOLLOWERS_FILE_NAME;
        Path followFile = Paths.get(fileName);

        if (!Files.exists(followFile)) {
            return new ArrayList<>();
        }

        try {
            List<String> userData = readFile(fileName);
            String followersLine = extractField(userData, FOLLOWERS_PREFIX);
            if (followersLine == null || followersLine.isEmpty() || followersLine.equals("(None)")) {
                return new ArrayList<>();
            }

            Map<String, User> userMap = allUsers.stream()
                    .collect(Collectors.toMap(User::getNickName, u -> u, (u1, u2) -> u1));

            return Stream.of(followersLine.split(","))
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Failed to load followers for artist: " + artist.getNickName() + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to load followers", e);
        }
    }

    public List<String[]> getLyricsEditRequests(String artistNickName) {
        return lyricsRequestManager.loadLyricsEditRequestsForArtist(artistNickName);
    }

    public void approveLyricsEditRequest(String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName) {
        try {
            lyricsRequestManager.approveLyricsEditRequest(artistNickName, songTitle, timestamp, suggestedLyrics, albumName);
            invalidateArtistCache();
        } catch (Exception e) {
            System.err.println("Failed to approve lyrics edit request for artist: " + artistNickName + ", song: " + songTitle + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to approve lyrics edit request", e);
        }
    }

    public void rejectLyricsEditRequest(String artistNickName, String songTitle, String timestamp) {
        try {
            lyricsRequestManager.rejectLyricsEditRequest(artistNickName, songTitle, timestamp);
            invalidateArtistCache();
        } catch (Exception e) {
            System.err.println("Failed to reject lyrics edit request for artist: " + artistNickName + ", song: " + songTitle + ", error: " + e.getMessage());
            throw new RuntimeException("Failed to reject lyrics edit request", e);
        }
    }

    public void saveArtistIndex(List<String> artistNicknames) {
        try {
            ensureDataDirectoryExists(ARTISTS_DIR);
            FileUtil.writeFile(ARTISTS_INDEX_FILE, artistNicknames);
        } catch (Exception e) {
            System.err.println("Failed to write artist index file: " + ARTISTS_INDEX_FILE + ", error: " + e.getMessage());
            throw new IllegalStateException("Failed to write artist index file", e);
        }
    }

    public List<String> loadArtistIndex() {
        Path indexFile = Paths.get(ARTISTS_INDEX_FILE);
        if (!Files.exists(indexFile)) {
            return new ArrayList<>();
        }
        try {
            return FileUtil.readFile(ARTISTS_INDEX_FILE);
        } catch (Exception e) {
            System.err.println("Failed to read artist index file: " + ARTISTS_INDEX_FILE + ", error: " + e.getMessage());
            throw new IllegalStateException("Failed to read artist index file", e);
        }
    }

    private void updateArtistIndex() {
        try {
            List<String> artistNicknames = loadAllArtists().stream()
                    .map(Artist::getNickName)
                    .collect(Collectors.toList());
            saveArtistIndex(artistNicknames);
        } catch (Exception e) {
            System.err.println("Failed to update artist index: " + e.getMessage());
            throw new RuntimeException("Failed to update artist index", e);
        }
    }
}