package services.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static utils.FileUtil.*;

public class LyricsRequestManager {
    private static final String LYRICS_REQUESTS_DIR = DATA_DIR + "lyrics_requests/";
    private static final String LYRICS_REQUESTS_PENDING = LYRICS_REQUESTS_DIR + "pending/";
    private static final String LYRICS_REQUESTS_APPROVED = LYRICS_REQUESTS_DIR + "approved/";
    private static final String LYRICS_REQUESTS_REJECTED = LYRICS_REQUESTS_DIR + "rejected/";

    private final SongFileManager songFileManager = new SongFileManager();

    public synchronized void saveLyricsEditRequest(String artistNickName, String songTitle, String albumName, String suggestedLyrics, String requester) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        if (suggestedLyrics == null || suggestedLyrics.isEmpty()) {
            throw new IllegalArgumentException("Suggested lyrics cannot be null or empty");
        }
        if (requester == null || requester.isEmpty()) {
            throw new IllegalArgumentException("Requester cannot be null or empty");
        }

        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeSongTitle = sanitizeFileName(songTitle);
        String requestDir = LYRICS_REQUESTS_PENDING + safeArtistNickName + "/" + safeSongTitle + "/";
        ensureDataDirectoryExists(requestDir);
        String requestFile = requestDir + safeSongTitle + "-" + System.currentTimeMillis() + ".txt";
        List<String> requestData = new ArrayList<>();
        requestData.add("Artist: " + artistNickName);
        requestData.add("Song: " + songTitle);
        requestData.add("Album: " + (albumName != null ? albumName : "Single"));
        requestData.add("SuggestedLyrics: " + suggestedLyrics);
        requestData.add("Requester: " + requester);
        requestData.add("Status: Pending");
        requestData.add("Timestamp: " + System.currentTimeMillis());
        writeFile(requestFile, requestData);
    }

    public synchronized List<String[]> loadLyricsEditRequestsForArtist(String artistNickName) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }

        String safeArtistNickName = sanitizeFileName(artistNickName);
        List<String[]> requests = new ArrayList<>();
        requests.addAll(loadRequestsFromDir(LYRICS_REQUESTS_PENDING + safeArtistNickName + "/"));
        requests.addAll(loadRequestsFromDir(LYRICS_REQUESTS_APPROVED + safeArtistNickName + "/"));
        requests.addAll(loadRequestsFromDir(LYRICS_REQUESTS_REJECTED + safeArtistNickName + "/"));
        return requests;
    }

    public String[][] getLyricsEditRequests(String status) {
        List<String[]> requests = new ArrayList<>();
        String dirPath;
        switch (status) {
            case "Pending":
                dirPath = LYRICS_REQUESTS_PENDING;
                break;
            case "Approved":
                dirPath = LYRICS_REQUESTS_APPROVED;
                break;
            case "Rejected":
                dirPath = LYRICS_REQUESTS_REJECTED;
                break;
            default:
                return new String[0][];
        }

        File requestsDir = new File(dirPath);
        if (!requestsDir.exists() || !requestsDir.isDirectory()) {
            return new String[0][];
        }

        File[] subDirs = requestsDir.listFiles(File::isDirectory);
        if (subDirs == null) {
            return new String[0][];
        }

        for (File subDir : subDirs) {
            File[] songDirs = subDir.listFiles(File::isDirectory);
            if (songDirs == null) {
                continue;
            }

            for (File songDir : songDirs) {
                File[] requestFiles = songDir.listFiles((dir, name) -> name.endsWith(".txt"));
                if (requestFiles == null) {
                    continue;
                }

                for (File requestFile : requestFiles) {
                    try {
                        List<String> lines = Files.readAllLines(requestFile.toPath());
                        String[] requestData = parseLyricsEditRequest(lines, status);
                        if (requestData != null) {
                            requests.add(requestData);
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + requestFile.getPath());
                    }
                }
            }
        }

        return requests.toArray(new String[0][]);
    }

    private String[] parseLyricsEditRequest(List<String> lines, String status) {
        String artistNickname = null;
        String songTitle = null;
        String timestamp = null;
        String suggestedLyrics = null;
        String email = null;
        String currentStatus = null;
        String albumName = null;

        for (String line : lines) {
            int index = line.indexOf(": ");
            if (index != -1) {
                String key = line.substring(0, index);
                String value = line.substring(index + 2);
                switch (key) {
                    case "Artist":
                        artistNickname = value;
                        break;
                    case "Song":
                        songTitle = value;
                        break;
                    case "Timestamp":
                        timestamp = value;
                        break;
                    case "SuggestedLyrics":
                        suggestedLyrics = value;
                        break;
                    case "Requester":
                        email = value;
                        break;
                    case "Status":
                        currentStatus = value;
                        break;
                    case "Album":
                        albumName = value;
                        break;
                }
            }
        }

        if (currentStatus != null && currentStatus.equals(status)) {
            return new String[]{
                    artistNickname,
                    songTitle,
                    timestamp,
                    suggestedLyrics,
                    email,
                    currentStatus,
                    albumName
            };
        }
        return null;
    }

    public synchronized List<String[]> loadAllLyricsEditRequests() {
        List<String[]> requests = new ArrayList<>();
        requests.addAll(loadRequestsFromDir(LYRICS_REQUESTS_PENDING));
        requests.addAll(loadRequestsFromDir(LYRICS_REQUESTS_APPROVED));
        requests.addAll(loadRequestsFromDir(LYRICS_REQUESTS_REJECTED));
        return requests;
    }

    private List<String[]> loadRequestsFromDir(String dirPath) {
        List<String[]> requests = new ArrayList<>();
        File requestsDir = new File(dirPath);
        if (!requestsDir.exists() || !requestsDir.isDirectory()) {
            return requests;
        }

        File[] subDirs = requestsDir.listFiles(File::isDirectory);
        if (subDirs == null) {
            return requests;
        }

        for (File subDir : subDirs) {
            File[] songDirs = subDir.listFiles(File::isDirectory);
            if (songDirs == null) {
                continue;
            }

            for (File songDir : songDirs) {
                File[] requestFiles = songDir.listFiles((d, name) -> name.endsWith(".txt"));
                if (requestFiles == null) {
                    continue;
                }

                for (File file : requestFiles) {
                    String[] requestData = new String[7]; // Artist, Song, Album, SuggestedLyrics, Requester, Status, Timestamp
                    List<String> lines = readFile(file.getPath());
                    for (String line : lines) {
                        int index = line.indexOf(": ");
                        if (index != -1) {
                            String key = line.substring(0, index);
                            String value = line.substring(index + 2);
                            switch (key) {
                                case "Artist": requestData[0] = value; break;
                                case "Song": requestData[1] = value; break;
                                case "Album": requestData[2] = value; break;
                                case "SuggestedLyrics": requestData[3] = value; break;
                                case "Requester": requestData[4] = value; break;
                                case "Status": requestData[5] = value; break;
                                case "Timestamp": requestData[6] = value; break;
                            }
                        }
                    }
                    if (requestData[0] != null && requestData[1] != null && requestData[5] != null) {
                        requests.add(requestData);
                    }
                }
            }
        }
        return requests;
    }

    public synchronized void approveLyricsEditRequest(String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        if (timestamp == null || timestamp.isEmpty()) {
            throw new IllegalArgumentException("Timestamp cannot be null or empty");
        }

        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeSongTitle = sanitizeFileName(songTitle);
        String pendingFile = LYRICS_REQUESTS_PENDING + safeArtistNickName + "/" + safeSongTitle + "/" + safeSongTitle + "-" + timestamp + ".txt";
        File file = new File(pendingFile);
        if (!file.exists()) {
            throw new IllegalStateException("Lyrics edit request not found for " + songTitle);
        }

        songFileManager.approveLyricEdit(artistNickName, songTitle, albumName, suggestedLyrics);

        String approvedDir = LYRICS_REQUESTS_APPROVED + safeArtistNickName + "/" + safeSongTitle + "/";
        ensureDataDirectoryExists(approvedDir);
        String approvedFile = approvedDir + safeSongTitle + "-" + timestamp + ".txt";
        List<String> requestData = readFile(pendingFile);
        requestData = new ArrayList<>(requestData);
        requestData.removeIf(line -> line.startsWith("Status:"));
        requestData.add("Status: Approved");
        writeFile(approvedFile, requestData);

        if (!file.delete()) {
            System.err.println("Warning: Failed to delete pending artist request file: " + pendingFile);
        }
    }

    public synchronized void rejectLyricsEditRequest(String artistNickName, String songTitle, String timestamp) {
        if (artistNickName == null || artistNickName.isEmpty()) {
            throw new IllegalArgumentException("Artist nickname cannot be null or empty");
        }
        if (songTitle == null || songTitle.isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        if (timestamp == null || timestamp.isEmpty()) {
            throw new IllegalArgumentException("Timestamp cannot be null or empty");
        }

        String safeArtistNickName = sanitizeFileName(artistNickName);
        String safeSongTitle = sanitizeFileName(songTitle);
        String pendingFile = LYRICS_REQUESTS_PENDING + safeArtistNickName + "/" + safeSongTitle + "/" + safeSongTitle + "-" + timestamp + ".txt";
        File file = new File(pendingFile);
        if (!file.exists()) {
            throw new IllegalStateException("Lyrics edit request not found for " + songTitle);
        }

        String rejectedDir = LYRICS_REQUESTS_REJECTED + safeArtistNickName + "/" + safeSongTitle + "/";
        ensureDataDirectoryExists(rejectedDir);
        String rejectedFile = rejectedDir + safeSongTitle + "-" + timestamp + ".txt";
        List<String> requestData = readFile(pendingFile);
        requestData = new ArrayList<>(requestData);
        requestData.removeIf(line -> line.startsWith("Status:"));
        requestData.add("Status: Rejected");
        writeFile(rejectedFile, requestData);

        if (!file.delete()) {
            System.err.println("Warning: Failed to delete pending artist request file: " + pendingFile);
        }
    }
}