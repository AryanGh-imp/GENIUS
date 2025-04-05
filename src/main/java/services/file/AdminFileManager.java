package services.file;

import models.account.Artist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static utils.FileUtil.*;

public class AdminFileManager extends FileManager {
    private static final String ARTIST_REQUESTS_DIR = DATA_DIR + "admin/artist_requests/";
    private static final String ARTIST_REQUESTS_PENDING = ARTIST_REQUESTS_DIR + "pending/";
    private static final String ARTIST_REQUESTS_APPROVED = ARTIST_REQUESTS_DIR + "approved/";
    private static final String ARTIST_REQUESTS_REJECTED = ARTIST_REQUESTS_DIR + "rejected/";

    private final ArtistFileManager artistFileManager = new ArtistFileManager();
    private final LyricsRequestManager lyricsRequestManager = new LyricsRequestManager();

    public synchronized void saveArtistRequest(String email, String nickName, String password) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (nickName == null || nickName.isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String safeNickName = sanitizeFileName(nickName);
        String requestDir = ARTIST_REQUESTS_PENDING + safeNickName + "/";
        ensureDataDirectoryExists(requestDir);
        String requestFile = requestDir + safeNickName + "-" + email + ".txt";
        List<String> requestData = new ArrayList<>();
        requestData.add("Email: " + email);
        requestData.add("Nickname: " + safeNickName);
        requestData.add("Password: " + password);
        requestData.add("Status: Pending");
        requestData.add("Timestamp: " + System.currentTimeMillis());
        writeFile(requestFile, requestData);
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

    private List<String[]> loadRequestsFromDir(String dirPath) {
        List<String[]> requests = new ArrayList<>();
        File requestsDir = new File(dirPath);
        if (!requestsDir.exists() || !requestsDir.isDirectory()) {
            return requests;
        }

        File[] requestDirs = requestsDir.listFiles(File::isDirectory);
        if (requestDirs == null) {
            return requests;
        }

        for (File dir : requestDirs) {
            File[] requestFiles = dir.listFiles((d, name) -> name.endsWith(".txt"));
            if (requestFiles == null) {
                continue;
            }

            for (File file : requestFiles) {
                String[] requestData = new String[5];
                List<String> lines = readFile(file.getPath());
                for (String line : lines) {
                    int index = line.indexOf(": ");
                    if (index != -1) {
                        String key = line.substring(0, index);
                        String value = line.substring(index + 2);
                        switch (key) {
                            case "Email": requestData[0] = value; break;
                            case "Nickname": requestData[1] = value; break;
                            case "Password": requestData[2] = value; break;
                            case "Status": requestData[3] = value; break;
                            case "Timestamp": requestData[4] = value; break;
                        }
                    }
                }
                if (requestData[0] != null && requestData[1] != null && requestData[2] != null && requestData[3] != null) {
                    requests.add(requestData);
                }
            }
        }
        return requests;
    }

    public synchronized void approveArtistRequest(String email, String nickName) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (nickName == null || nickName.isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }

        String safeNickName = sanitizeFileName(nickName);
        String pendingFile = ARTIST_REQUESTS_PENDING + safeNickName + "/" + safeNickName + "-" + email + ".txt";
        File file = new File(pendingFile);
        if (!file.exists()) {
            throw new IllegalStateException("Artist request not found for " + nickName);
        }

        List<String> requestData = readFile(pendingFile);
        String password = null;
        for (String line : requestData) {
            if (line.startsWith("Password: ")) {
                password = line.split(": ")[1];
                break;
            }
        }
        if (password == null) {
            throw new IllegalStateException("Invalid artist request data: Password not found for " + nickName);
        }

        Artist artist = new Artist(email, nickName, password);
        artist.setApproved(true);
        artistFileManager.saveAccount(artist);

        String approvedDir = ARTIST_REQUESTS_APPROVED + safeNickName + "/";
        ensureDataDirectoryExists(approvedDir);
        String approvedFile = approvedDir + safeNickName + "-" + email + ".txt";
        requestData = new ArrayList<>(requestData);
        requestData.removeIf(line -> line.startsWith("Status:"));
        requestData.add("Status: Approved");
        writeFile(approvedFile, requestData);

        if (!file.delete()) {
            throw new IllegalStateException("Failed to delete pending artist request file: " + pendingFile);
        }
    }

    public synchronized void rejectArtistRequest(String email, String nickName) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (nickName == null || nickName.isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }

        String safeNickName = sanitizeFileName(nickName);
        String pendingFile = ARTIST_REQUESTS_PENDING + safeNickName + "/" + safeNickName + "-" + email + ".txt";
        File file = new File(pendingFile);
        if (!file.exists()) {
            throw new IllegalStateException("Artist request not found for " + nickName);
        }

        String rejectedDir = ARTIST_REQUESTS_REJECTED + safeNickName + "/";
        ensureDataDirectoryExists(rejectedDir);
        String rejectedFile = rejectedDir + safeNickName + "-" + email + ".txt";
        List<String> requestData = readFile(pendingFile);
        requestData = new ArrayList<>(requestData);
        requestData.removeIf(line -> line.startsWith("Status:"));
        requestData.add("Status: Rejected");
        writeFile(rejectedFile, requestData);

        if (!file.delete()) {
            throw new IllegalStateException("Failed to delete pending artist request file: " + pendingFile);
        }
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