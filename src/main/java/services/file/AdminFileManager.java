package services.file;

import models.account.Artist;
import utils.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages file operations for admin-related tasks, such as artist requests and lyrics edit requests.
 */
public class AdminFileManager extends FileManager {
    // Directory paths for artist requests
    private static final String ARTIST_REQUESTS_DIR = DATA_DIR + "admin/artist_requests/";
    private static final String ARTIST_REQUESTS_PENDING = ARTIST_REQUESTS_DIR + "pending/";
    private static final String ARTIST_REQUESTS_APPROVED = ARTIST_REQUESTS_DIR + "approved/";
    private static final String ARTIST_REQUESTS_REJECTED = ARTIST_REQUESTS_DIR + "rejected/";

    private ArtistFileManager artistFileManager;
    private UserFileManager userFileManager;
    private final LyricsRequestManager lyricsRequestManager;

    /**
     * Constructs an AdminFileManager instance.
     */
    public AdminFileManager() {
        this.lyricsRequestManager = new LyricsRequestManager();
    }

    /**
     * Sets the ArtistFileManager instance.
     *
     * @param artistFileManager The ArtistFileManager instance.
     * @throws IllegalArgumentException If artistFileManager is null.
     */
    public void setArtistFileManager(ArtistFileManager artistFileManager) {
        if (artistFileManager == null) {
            throw new IllegalArgumentException("ArtistFileManager cannot be null");
        }
        this.artistFileManager = artistFileManager;
        // Inject UserFileManager into ArtistFileManager if already set
        if (this.userFileManager != null) {
            this.artistFileManager.setUserFileManager(this.userFileManager);
        }
    }

    /**
     * Sets the UserFileManager instance.
     *
     * @param userFileManager The UserFileManager instance.
     * @throws IllegalArgumentException If userFileManager is null.
     */
    public void setUserFileManager(UserFileManager userFileManager) {
        if (userFileManager == null) {
            throw new IllegalArgumentException("UserFileManager cannot be null");
        }
        this.userFileManager = userFileManager;
        // Inject UserFileManager into ArtistFileManager if already set
        if (this.artistFileManager != null) {
            this.artistFileManager.setUserFileManager(this.userFileManager);
        }
    }

    /**
     * Gets the UserFileManager instance.
     *
     * @return The UserFileManager instance.
     * @throws IllegalStateException If UserFileManager is not set.
     */
    public UserFileManager getUserFileManager() {
        if (userFileManager == null) {
            throw new IllegalStateException("UserFileManager is not set in AdminFileManager");
        }
        return userFileManager;
    }

    /**
     * Saves an artist request to the pending directory.
     *
     * @param email    The email of the artist.
     * @param nickName The nickname of the artist.
     * @param password The password of the artist.
     * @throws IllegalArgumentException If any of the parameters are null or empty.
     */
    public synchronized void saveArtistRequest(String email, String nickName, String password) {
        validateInput(email, "Email");
        validateInput(nickName, "Nickname");
        validateInput(password, "Password");

        String safeNickName = FileUtil.sanitizeFileName(nickName);
        String requestDir = ARTIST_REQUESTS_PENDING + safeNickName + "/";
        FileUtil.ensureDataDirectoryExists(requestDir);
        String requestFile = requestDir + safeNickName + "-" + email + ".txt";

        List<String> requestData = new ArrayList<>();
        requestData.add("Email: " + email);
        requestData.add("Nickname: " + safeNickName);
        requestData.add("Password: " + password);
        requestData.add("Status: Pending");
        requestData.add("Timestamp: " + Instant.now().toString());
        FileUtil.writeFile(requestFile, requestData);
    }

    /**
     * Loads all pending artist requests.
     *
     * @return A list of pending artist requests, where each request is an array of [email, nickname, password, status, timestamp].
     */
    public synchronized List<String[]> loadPendingArtistRequests() {
        return loadRequestsFromDir(ARTIST_REQUESTS_PENDING);
    }

    /**
     * Loads all approved artist requests.
     *
     * @return A list of approved artist requests.
     */
    public synchronized List<String[]> loadApprovedArtistRequests() {
        return loadRequestsFromDir(ARTIST_REQUESTS_APPROVED);
    }

    /**
     * Loads all rejected artist requests.
     *
     * @return A list of rejected artist requests.
     */
    public synchronized List<String[]> loadRejectedArtistRequests() {
        return loadRequestsFromDir(ARTIST_REQUESTS_REJECTED);
    }

    /**
     * Loads requests from the specified directory.
     *
     * @param dirPath The directory path to load requests from.
     * @return A list of requests, where each request is an array of [email, nickname, password, status, timestamp].
     */
    private List<String[]> loadRequestsFromDir(String dirPath) {
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
                                        String[] requestData = parseRequestFile(file);
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

    /**
     * Parses a request file and extracts its data.
     *
     * @param file The path to the request file.
     * @return An array of [email, nickname, password, status, timestamp], or null if the file is invalid.
     */
    private String[] parseRequestFile(Path file) {
        String[] requestData = new String[5]; // [email, nickname, password, status, timestamp]
        List<String> lines = FileUtil.readFile(file.toString());
        for (String line : lines) {
            int index = line.indexOf(": ");
            if (index != -1) {
                String key = line.substring(0, index);
                String value = line.substring(index + 2);
                switch (key) {
                    case "Email":
                        requestData[0] = value;
                        break;
                    case "Nickname":
                        requestData[1] = value;
                        break;
                    case "Password":
                        requestData[2] = value;
                        break;
                    case "Status":
                        requestData[3] = value;
                        break;
                    case "Timestamp":
                        requestData[4] = value;
                        break;
                }
            }
        }
        // Check if all required fields are present
        if (requestData[0] != null && requestData[1] != null && requestData[2] != null && requestData[3] != null) {
            return requestData;
        }
        return null;
    }

    /**
     * Approves an artist request and moves it to the approved directory.
     *
     * @param email    The email of the artist.
     * @param nickName The nickname of the artist.
     * @throws IllegalArgumentException If email or nickName is null or empty.
     * @throws IllegalStateException    If the request is not found or cannot be processed.
     */
    public synchronized void approveArtistRequest(String email, String nickName) {
        validateInput(email, "Email");
        validateInput(nickName, "Nickname");

        String safeNickName = FileUtil.sanitizeFileName(nickName);
        String pendingDir = ARTIST_REQUESTS_PENDING + safeNickName + "/";
        String pendingFilePath = pendingDir + safeNickName + "-" + email + ".txt";
        Path pendingFile = Paths.get(pendingFilePath);
        Path pendingDirPath = Paths.get(pendingDir);
        if (!Files.exists(pendingFile)) {
            throw new IllegalStateException("Artist request not found for " + nickName + " at " + pendingFilePath);
        }

        List<String> requestData = FileUtil.readFile(pendingFilePath);
        String password = extractField(requestData, "Password");
        if (password == null) {
            throw new IllegalStateException("Invalid artist request data: Password not found for " + nickName + " in file: " + pendingFilePath);
        }

        // Create and configure the Artist
        Artist artist = new Artist(email, nickName, password);
        if (artistFileManager == null) {
            throw new IllegalStateException("ArtistFileManager is not set in AdminFileManager");
        }
        artist.setArtistFileManager(artistFileManager);
        artist.setSongFileManager(new SongFileManager());
        artist.setUserFileManager(getUserFileManager());
        artist.setApproved(true);
        artistFileManager.saveAccount(artist);

        // Move the request to the approved directory
        String approvedDir = ARTIST_REQUESTS_APPROVED + safeNickName + "/";
        FileUtil.ensureDataDirectoryExists(approvedDir);
        String approvedFilePath = approvedDir + safeNickName + "-" + email + ".txt";
        requestData = new ArrayList<>(requestData);
        requestData.removeIf(line -> line.startsWith("Status:"));
        requestData.add("Status: Approved");
        FileUtil.writeFile(approvedFilePath, requestData);

        try {
            Files.delete(pendingFile);
            try (Stream<Path> dirStream = Files.list(pendingDirPath)) {
                if (dirStream.findAny().isEmpty()) {
                    Files.delete(pendingDirPath);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete pending artist request file or directory: " + pendingFilePath, e);
        }
    }

    /**
     * Rejects an artist request and moves it to the rejected directory.
     *
     * @param email    The email of the artist.
     * @param nickName The nickname of the artist.
     * @throws IllegalArgumentException If email or nickName is null or empty.
     * @throws IllegalStateException    If the request is not found or cannot be processed.
     */
    public synchronized void rejectArtistRequest(String email, String nickName) {
        validateInput(email, "Email");
        validateInput(nickName, "Nickname");

        String safeNickName = FileUtil.sanitizeFileName(nickName);
        String pendingDir = ARTIST_REQUESTS_PENDING + safeNickName + "/";
        String pendingFilePath = pendingDir + safeNickName + "-" + email + ".txt";
        Path pendingFile = Paths.get(pendingFilePath);
        Path pendingDirPath = Paths.get(pendingDir);
        if (!Files.exists(pendingFile)) {
            throw new IllegalStateException("Artist request not found for " + nickName + " at " + pendingFilePath);
        }

        String rejectedDir = ARTIST_REQUESTS_REJECTED + safeNickName + "/";
        FileUtil.ensureDataDirectoryExists(rejectedDir);
        String rejectedFilePath = rejectedDir + safeNickName + "-" + email + ".txt";
        List<String> requestData = FileUtil.readFile(pendingFilePath);
        requestData = new ArrayList<>(requestData);
        requestData.removeIf(line -> line.startsWith("Status:"));
        requestData.add("Status: Rejected");
        FileUtil.writeFile(rejectedFilePath, requestData);

        try {
            Files.delete(pendingFile);
            try (Stream<Path> dirStream = Files.list(pendingDirPath)) {
                if (dirStream.findAny().isEmpty()) {
                    Files.delete(pendingDirPath);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete pending artist request file or directory: " + pendingFilePath, e);
        }
    }

    /**
     * Retrieves all lyrics edit requests.
     *
     * @return A list of lyrics edit requests.
     */
    public List<String[]> getAllLyricsEditRequests() {
        return lyricsRequestManager.loadAllLyricsEditRequests();
    }

    /**
     * Approves a lyrics edit request.
     *
     * @param artistNickName   The nickname of the artist.
     * @param songTitle        The title of the song.
     * @param timestamp        The timestamp of the request.
     * @param suggestedLyrics  The suggested lyrics.
     * @param albumName        The name of the album (can be null if the song is a single).
     */
    public void approveLyricsEditRequest(String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName) {
        lyricsRequestManager.approveLyricsEditRequest(artistNickName, songTitle, timestamp, suggestedLyrics, albumName);
    }

    /**
     * Rejects a lyrics edit request.
     *
     * @param artistNickName The nickname of the artist.
     * @param songTitle      The title of the song.
     * @param timestamp      The timestamp of the request.
     */
    public void rejectLyricsEditRequest(String artistNickName, String songTitle, String timestamp) {
        lyricsRequestManager.rejectLyricsEditRequest(artistNickName, songTitle, timestamp);
    }

    /**
     * Validates that the input is not null or empty.
     *
     * @param input     The input to validate.
     * @param fieldName The name of the field for error messaging.
     * @throws IllegalArgumentException If the input is null or empty.
     */
    private void validateInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Extracts a field value from the request data.
     *
     * @param requestData The list of request data lines.
     * @param fieldName   The name of the field to extract.
     * @return The value of the field, or null if not found.
     */
    private String extractField(List<String> requestData, String fieldName) {
        for (String line : requestData) {
            if (line.startsWith(fieldName + ": ")) {
                return line.split(": ", 2)[1];
            }
        }
        return null;
    }
}