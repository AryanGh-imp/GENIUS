package services.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.music.Lyrics;
import static utils.FileUtil.*;

public class LyricsRequestManager extends FileManager {
    private static final String LYRICS_REQUESTS_DIR = DATA_DIR + "lyrics_requests/";
    private static final String LYRICS_REQUESTS_PENDING = LYRICS_REQUESTS_DIR + "pending/";
    private static final String LYRICS_REQUESTS_APPROVED = LYRICS_REQUESTS_DIR + "approved/";
    private static final String LYRICS_REQUESTS_REJECTED = LYRICS_REQUESTS_DIR + "rejected/";

    private static final String ARTIST_KEY = "Artist: ";
    private static final String SONG_KEY = "Song: ";
    private static final String ALBUM_KEY = "Album: ";
    private static final String SUGGESTED_LYRICS_KEY = "SuggestedLyrics: ";
    private static final String REQUESTER_KEY = "Requester: ";
    private static final String STATUS_KEY = "Status: ";
    private static final String TIMESTAMP_KEY = "Timestamp: ";

    private final SongFileManager songFileManager = new SongFileManager();

    public synchronized void saveLyricsEditRequest(String artistNickName, String songTitle, String albumName, String suggestedLyrics, String requester) {
        validateInputs(artistNickName, songTitle, suggestedLyrics, requester);

        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeSongTitle = sanitizeFileName(songTitle);
        String requestDir = LYRICS_REQUESTS_PENDING + safeArtistNickName + "/" + safeSongTitle + "/";
        System.out.println("Attempting to create request directory: " + requestDir);
        ensureDataDirectoryExists(requestDir);

        String timestamp = LocalDateTime.now().format(formatter);
        String formattedTimestamp = timestamp.replace(":", "-").replace(" ", "_");
        String requestFile = requestDir + safeSongTitle + "-" + formattedTimestamp + ".txt";
        System.out.println("Request file path: " + requestFile);

        String songDir = songFileManager.getSongDir(artistNickName, songTitle, albumName);
        System.out.println("Song directory: " + songDir);
        Path songFilePath = Paths.get(songDir + safeSongTitle + ".txt");
        Path lyricsFilePath = Paths.get(songDir + safeSongTitle + "_lyrics.txt");

        if (!Files.exists(songFilePath)) {
            System.err.println("Song file not found: " + songFilePath);
            throw new IllegalStateException("Song file not found: " + songFilePath);
        }
        if (!Files.exists(lyricsFilePath)) {
            System.err.println("Lyrics file not found: " + lyricsFilePath);
            throw new IllegalStateException("Lyrics file not found: " + lyricsFilePath);
        }

        String originalLyrics = songFileManager.loadLyrics(songDir + safeSongTitle + ".txt");
        if (originalLyrics == null || originalLyrics.trim().isEmpty()) {
            System.err.println("Original lyrics not found for song: " + songTitle + " in directory: " + songDir);
            throw new IllegalStateException("Original lyrics not found for song: " + songTitle);
        }
        System.out.println("Original lyrics loaded: " + originalLyrics);

        Lyrics lyrics = new Lyrics(originalLyrics);
        lyrics.suggestEdit(suggestedLyrics);
        System.out.println("Lyrics object created and edit suggested: " + lyrics);

        List<String> requestData = createRequestData(artistNickName, songTitle, albumName, suggestedLyrics, requester, timestamp);
        System.out.println("Request data to save: " + requestData);
        writeFile(requestFile, requestData);
        System.out.println("Request file saved successfully: " + requestFile);

        Path requestFilePath = Paths.get(requestFile);
        if (Files.exists(requestFilePath)) {
            System.out.println("Verified: Request file exists at " + requestFile);
        } else {
            System.err.println("Error: Request file was not created at " + requestFile);
            throw new IllegalStateException("Failed to create request file at " + requestFile);
        }
    }

    public synchronized List<String[]> loadLyricsEditRequestsForArtist(String artistNickName) {
        validateInput(artistNickName, "Artist nickname");
        String safeArtistNickName = sanitizeFileName(artistNickName);
        return loadRequestsFromMultipleDirs(
                LYRICS_REQUESTS_PENDING + safeArtistNickName + "/",
                LYRICS_REQUESTS_APPROVED + safeArtistNickName + "/",
                LYRICS_REQUESTS_REJECTED + safeArtistNickName + "/"
        );
    }

    public String[][] getLyricsEditRequests(String status) {
        String dirPath = switch (status) {
            case "Pending" -> LYRICS_REQUESTS_PENDING;
            case "Approved" -> LYRICS_REQUESTS_APPROVED;
            case "Rejected" -> LYRICS_REQUESTS_REJECTED;
            default -> null;
        };
        if (dirPath == null) return new String[0][];
        return loadRequestsFromDir(dirPath).toArray(new String[0][]);
    }

    public synchronized List<String[]> loadAllLyricsEditRequests() {
        return loadRequestsFromMultipleDirs(LYRICS_REQUESTS_PENDING, LYRICS_REQUESTS_APPROVED, LYRICS_REQUESTS_REJECTED);
    }

    public synchronized void approveLyricsEditRequest(String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName) {
        String[] safeParams = validateAndSanitize(artistNickName, songTitle, timestamp);
        String safeArtistNickName = safeParams[0];
        String safeSongTitle = safeParams[1];

        String formattedTimestamp = timestamp.replace(":", "-").replace(" ", "_");
        String pendingFilePath = LYRICS_REQUESTS_PENDING + safeArtistNickName + "/" + safeSongTitle + "/" + safeSongTitle + "-" + formattedTimestamp + ".txt";
        System.out.println("Looking for file at: " + pendingFilePath);
        Path pendingFile = Paths.get(pendingFilePath);
        if (!Files.exists(pendingFile)) {
            throw new IllegalStateException("Lyrics edit request not found for song: " + songTitle + " at timestamp: " + timestamp);
        }

        String songDir = songFileManager.getSongDir(artistNickName, songTitle, albumName);
        System.out.println("Song directory: " + songDir);
        Path songFilePath = Paths.get(songDir + safeSongTitle + ".txt");
        Path lyricsFilePath = Paths.get(songDir + safeSongTitle + "_lyrics.txt");

        if (!Files.exists(songFilePath)) {
            System.err.println("Song file not found: " + songFilePath);
            throw new IllegalStateException("Song file not found: " + songFilePath);
        }
        if (!Files.exists(lyricsFilePath)) {
            System.err.println("Lyrics file not found: " + lyricsFilePath);
            throw new IllegalStateException("Lyrics file not found: " + lyricsFilePath);
        }

        String currentLyrics = songFileManager.loadLyrics(songDir + safeSongTitle + ".txt");
        if (currentLyrics == null || currentLyrics.trim().isEmpty()) {
            System.err.println("Current lyrics not found or empty for song: " + songTitle + " at: " + lyricsFilePath);
            throw new IllegalStateException("Current lyrics not found for song: " + songTitle);
        }

        Lyrics lyrics = new Lyrics(currentLyrics);
        lyrics.approveEdit(suggestedLyrics);
        String approvedLyrics = lyrics.getApprovedLyrics();

        writeFile(songDir + safeSongTitle + "_lyrics.txt", Collections.singletonList(approvedLyrics));

        moveRequest(pendingFile, safeArtistNickName, safeSongTitle, LYRICS_REQUESTS_APPROVED, "Approved");
    }

    public synchronized void rejectLyricsEditRequest(String artistNickName, String songTitle, String timestamp) {
        String[] safeParams = validateAndSanitize(artistNickName, songTitle, timestamp);
        String safeArtistNickName = safeParams[0];
        String safeSongTitle = safeParams[1];

        String formattedTimestamp = timestamp.replace(":", "-").replace(" ", "_");
        String pendingFilePath = LYRICS_REQUESTS_PENDING + safeArtistNickName + "/" + safeSongTitle + "/" + safeSongTitle + "-" + formattedTimestamp + ".txt";
        System.out.println("Looking for file at: " + pendingFilePath);
        Path pendingFile = Paths.get(pendingFilePath);
        if (!Files.exists(pendingFile)) {
            throw new IllegalStateException("Lyrics edit request not found for song: " + songTitle + " at timestamp: " + timestamp);
        }

        moveRequest(pendingFile, safeArtistNickName, safeSongTitle, LYRICS_REQUESTS_REJECTED, "Rejected");
    }

    private void validateInputs(String artistNickName, String songTitle, String suggestedLyrics, String requester) {
        validateInput(artistNickName, "Artist nickname");
        validateInput(songTitle, "Song title");
        validateInput(suggestedLyrics, "Suggested lyrics");
        validateInput(requester, "Requester");
    }

    private String[] validateAndSanitize(String artistNickName, String songTitle, String timestamp) {
        validateInput(artistNickName, "Artist nickname");
        validateInput(songTitle, "Song title");
        validateInput(timestamp, "Timestamp");
        return new String[]{sanitizeFileName(artistNickName), sanitizeFileName(songTitle)};
    }

    private List<String> createRequestData(String artistNickName, String songTitle, String albumName, String suggestedLyrics, String requester, String timestamp) {
        List<String> requestData = new ArrayList<>();
        requestData.add(ARTIST_KEY + artistNickName);
        requestData.add(SONG_KEY + songTitle);
        requestData.add(ALBUM_KEY + (albumName != null ? albumName : "Single"));
        requestData.add(SUGGESTED_LYRICS_KEY + suggestedLyrics);
        requestData.add(REQUESTER_KEY + requester);
        requestData.add(STATUS_KEY + "Pending");
        requestData.add(TIMESTAMP_KEY + timestamp);
        return requestData;
    }

    private List<String[]> loadRequestsFromMultipleDirs(String... dirs) {
        List<String[]> allRequests = new ArrayList<>();
        for (String dir : dirs) {
            allRequests.addAll(loadRequestsFromDir(dir));
        }
        return allRequests;
    }

    private void moveRequest(Path sourceFile, String safeArtistNickName, String safeSongTitle, String targetDir, String newStatus) {
        List<String> requestData = readFile(sourceFile.toString());
        List<String> updatedRequestData = new ArrayList<>(requestData);
        updatedRequestData.removeIf(line -> line.startsWith("Status: "));
        updatedRequestData.add("Status: " + newStatus);

        String targetDirPath = targetDir + safeArtistNickName + "/" + safeSongTitle + "/";
        String targetFileName = sourceFile.getFileName().toString();
        moveRequestToDir(updatedRequestData, targetDirPath, targetFileName, newStatus, sourceFile);
    }
}