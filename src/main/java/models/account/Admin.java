package models.account;

import services.file.AdminFileManager;

import java.util.List;

/**
 * Represents an Admin account in the system, extending the base Account class.
 * An Admin can manage artist requests and lyrics edit requests.
 */
public class Admin extends Account {
    private AdminFileManager adminFileManager;

    /**
     * Constructs a new Admin with the specified email, nickname, and password.
     *
     * @param email    The admin's email address.
     * @param nickName The admin's nickname.
     * @param password The admin's password.
     */
    public Admin(String email, String nickName, String password) {
        super(email, nickName, password);
    }

    /**
     * Sets the AdminFileManager instance.
     *
     * @param adminFileManager The AdminFileManager instance.
     */
    public void setAdminFileManager(AdminFileManager adminFileManager) {
        if (adminFileManager == null) {
            throw new IllegalArgumentException("AdminFileManager cannot be null");
        }
        this.adminFileManager = adminFileManager;
    }

    /**
     * Gets the role of the account.
     *
     * @return The role "Admin".
     */
    @Override
    public final String getRole() {
        return "Admin";
    }

    /**
     * Gets all pending artist requests.
     *
     * @return A 2D array of pending artist requests, where each inner array contains
     *         [email, nickname, password, status, timestamp].
     */
    public String[][] getPendingArtistRequests() {
        checkAdminFileManager();
        List<String[]> requests = adminFileManager.loadPendingArtistRequests();
        return requests.toArray(new String[0][]);
    }

    /**
     * Gets all approved artist requests.
     *
     * @return A 2D array of approved artist requests.
     */
    public String[][] getApprovedArtistRequests() {
        checkAdminFileManager();
        List<String[]> requests = adminFileManager.loadApprovedArtistRequests();
        return requests.toArray(new String[0][]);
    }

    /**
     * Gets all rejected artist requests.
     *
     * @return A 2D array of rejected artist requests.
     */
    public String[][] getRejectedArtistRequests() {
        checkAdminFileManager();
        List<String[]> requests = adminFileManager.loadRejectedArtistRequests();
        return requests.toArray(new String[0][]);
    }

    /**
     * Gets all lyrics edit requests.
     *
     * @return A 2D array of lyrics edit requests, where each inner array contains
     *         [artistNickname, songTitle, albumName, suggestedLyrics, requester, status, timestamp].
     */
    public String[][] getAllLyricsEditRequests() {
        checkAdminFileManager();
        List<String[]> requests = adminFileManager.getAllLyricsEditRequests();
        return requests.toArray(new String[0][]);
    }

    /**
     * Approves an artist request and creates a new Artist account.
     *
     * @param email    The email of the artist.
     * @param nickName The nickname of the artist.
     */
    public void approveArtist(String email, String nickName) {
        checkAdminFileManager();
        adminFileManager.approveArtistRequest(email, nickName);
    }

    /**
     * Rejects an artist request.
     *
     * @param email The email of the artist.
     */
    public void rejectArtist(String email) {
        checkAdminFileManager();
        List<String[]> pendingRequests = adminFileManager.loadPendingArtistRequests();
        String nickName = null;
        for (String[] request : pendingRequests) {
            if (request[0].equals(email)) {
                nickName = request[1];
                break;
            }
        }
        if (nickName == null) {
            throw new IllegalStateException("Artist request not found for email: " + email);
        }
        adminFileManager.rejectArtistRequest(email, nickName);
    }

    /**
     * Approves a lyrics edit request.
     *
     * @param artistNickName   The nickname of the artist.
     * @param songTitle        The title of the song.
     * @param albumName        The album name (can be null for singles).
     * @param suggestedLyrics  The suggested lyrics.
     */
    public void approveLyricsEdit(String artistNickName, String songTitle, String albumName, String suggestedLyrics) {
        checkAdminFileManager();
        List<String[]> allRequests = adminFileManager.getAllLyricsEditRequests();
        String timestamp = null;
        for (String[] request : allRequests) {
            if (request[0].equals(artistNickName) && request[1].equals(songTitle) &&
                    (albumName == null || albumName.equals(request[2])) && request[5].equals("Pending")) {
                timestamp = request[6];
                break;
            }
        }
        if (timestamp == null) {
            throw new IllegalStateException("Lyrics edit request not found for " + songTitle + " by " + artistNickName);
        }
        adminFileManager.approveLyricsEditRequest(artistNickName, songTitle, timestamp, suggestedLyrics, albumName);
    }

    /**
     * Rejects a lyrics edit request.
     *
     * @param artistNickName The nickname of the artist.
     * @param songTitle      The title of the song.
     * @param albumName      The album name (can be null for singles).
     */
    public void rejectLyricsEdit(String artistNickName, String songTitle, String albumName) {
        checkAdminFileManager();
        List<String[]> allRequests = adminFileManager.getAllLyricsEditRequests();
        String timestamp = null;
        for (String[] request : allRequests) {
            if (request[0].equals(artistNickName) && request[1].equals(songTitle) &&
                    (albumName == null || albumName.equals(request[2])) && request[5].equals("Pending")) {
                timestamp = request[6];
                break;
            }
        }
        if (timestamp == null) {
            throw new IllegalStateException("Lyrics edit request not found for " + songTitle + " by " + artistNickName);
        }
        adminFileManager.rejectLyricsEditRequest(artistNickName, songTitle, timestamp);
    }

    private void checkAdminFileManager() {
        if (adminFileManager == null) {
            throw new IllegalStateException("AdminFileManager is not set for this Admin instance.");
        }
    }
}