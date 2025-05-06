package services.file;

import models.account.Artist;
import utils.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AdminFileManager extends FileManager {
    // Directory paths for artist requests
    private static final String ARTIST_REQUESTS_DIR = DATA_DIR + "admin/artist_requests/";
    private static final String ARTIST_REQUESTS_PENDING = ARTIST_REQUESTS_DIR + "pending/";
    private static final String ARTIST_REQUESTS_APPROVED = ARTIST_REQUESTS_DIR + "approved/";
    private static final String ARTIST_REQUESTS_REJECTED = ARTIST_REQUESTS_DIR + "rejected/";

    // Constants for request file keys
    private static final String EMAIL_KEY = "Email: ";
    private static final String NICKNAME_KEY = "Nickname: ";
    private static final String PASSWORD_KEY = "Password: ";
    private static final String STATUS_KEY = "Status: ";
    private static final String TIMESTAMP_KEY = "Timestamp: ";

    private final LyricsRequestManager lyricsRequestManager;

    public AdminFileManager() {
        this.lyricsRequestManager = new LyricsRequestManager();
    }

    public synchronized void saveArtistRequest(String email, String nickName, String password) {
        validateInput(email, "Email");
        validateInput(nickName, "Nickname");
        validateInput(password, "Password");

        String safeNickName = FileUtil.sanitizeFileName(nickName);
        String requestDir = ARTIST_REQUESTS_PENDING + safeNickName + "/";
        FileUtil.ensureDataDirectoryExists(requestDir);
        String requestFile = requestDir + safeNickName + "-" + email + ".txt";

        List<String> requestData = new ArrayList<>();
        requestData.add(EMAIL_KEY + email);
        requestData.add(NICKNAME_KEY + safeNickName);
        requestData.add(PASSWORD_KEY + password);
        requestData.add(STATUS_KEY + "Pending");
        requestData.add(TIMESTAMP_KEY + Instant.now().toString());
        FileUtil.writeFile(requestFile, requestData);
    }

    public synchronized List<String[]> loadPendingArtistRequests() {
        return loadRequestsFromDir(ARTIST_REQUESTS_PENDING);
    }

    public synchronized List<String[]> loadApprovedArtistRequests() {
        return loadRequestsFromDir(ARTIST_REQUESTS_APPROVED);
    }

    public synchronized List<String[]> loadRejectedArtistRequests() {
        return loadRequestsFromDir(ARTIST_REQUESTS_REJECTED);
    }

    public synchronized void approveArtistRequest(String email, String nickName) {
        validateInput(email, "Email");
        validateInput(nickName, "Nickname");

        String safeNickName = FileUtil.sanitizeFileName(nickName);
        String pendingFilePath = ARTIST_REQUESTS_PENDING + safeNickName + "/" + safeNickName + "-" + email + ".txt";
        Path pendingFile = Paths.get(pendingFilePath);
        if (!Files.exists(pendingFile)) {
            throw new IllegalStateException("Artist request not found for " + nickName + " at " + pendingFilePath);
        }

        List<String> requestData = FileUtil.readFile(pendingFilePath);
        String password = extractField(requestData, PASSWORD_KEY);
        if (password == null) {
            throw new IllegalStateException("Invalid artist request data: Password not found for " + nickName + " in file: " + pendingFilePath);
        }

        // Create and save the Artist
        Artist artist = new Artist(email, nickName, password);
        artist.setApproved(true);
        saveAccount(artist);

        // Move the request to the approved directory
        String targetDirPath = ARTIST_REQUESTS_APPROVED + safeNickName + "/";
        String targetFileName = safeNickName + "-" + email + ".txt";
        moveRequestToDir(requestData, targetDirPath, targetFileName, "Approved", pendingFile);
    }

    public synchronized void rejectArtistRequest(String email, String nickName) {
        validateInput(email, "Email");
        validateInput(nickName, "Nickname");

        String safeNickName = FileUtil.sanitizeFileName(nickName);
        String pendingFilePath = ARTIST_REQUESTS_PENDING + safeNickName + "/" + safeNickName + "-" + email + ".txt";
        Path pendingFile = Paths.get(pendingFilePath);
        if (!Files.exists(pendingFile)) {
            throw new IllegalStateException("Artist request not found for " + nickName + " at " + pendingFilePath);
        }

        List<String> requestData = FileUtil.readFile(pendingFilePath);
        // Move the request to the rejected directory
        String targetDirPath = ARTIST_REQUESTS_REJECTED + safeNickName + "/";
        String targetFileName = safeNickName + "-" + email + ".txt";
        moveRequestToDir(requestData, targetDirPath, targetFileName, "Rejected", pendingFile);
    }

    public List<String[]> getAllLyricsEditRequests() {
        return lyricsRequestManager.loadAllLyricsEditRequests();
    }

    public void approveLyricsEditRequest(String artistNickName, String songTitle, String timestamp, String suggestedLyrics, String albumName) {
        lyricsRequestManager.approveLyricsEditRequest(artistNickName, songTitle, timestamp, suggestedLyrics, albumName);
    }

    public void rejectLyricsEditRequest(String artistNickName, String songTitle, String timestamp) {
        lyricsRequestManager.rejectLyricsEditRequest(artistNickName, songTitle, timestamp);
    }
}