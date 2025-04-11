package models;

import services.file.LyricsRequestManager;

public class LyricsEditRequest {
    private final String email;
    private final String artistNickname;
    private final String songTitle;
    private final String albumName;
    private final String suggestedLyrics;
    private final String timestamp;
    private final String status;

    private LyricsRequestManager lyricsRequestManager;

    public LyricsEditRequest(String email, String artistNickname, String songTitle, String albumName,
                             String suggestedLyrics, String timestamp, String status) {
        this.email = email;
        this.artistNickname = artistNickname;
        this.songTitle = songTitle;
        this.albumName = albumName;
        this.suggestedLyrics = suggestedLyrics;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters
    public String getEmail() {
        return email;
    }

    public String getArtistNickname() {
        return artistNickname;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getSuggestedLyrics() {
        return suggestedLyrics;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String[][] getPendingLyricsEditRequests() {
        if (lyricsRequestManager == null) {
            throw new IllegalStateException("LyricsRequestManager is not set in LyricsEditRequest");
        }
        return lyricsRequestManager.getLyricsEditRequests("Pending");
    }

    public String[][] getApprovedLyricsEditRequests() {
        if (lyricsRequestManager == null) {
            throw new IllegalStateException("LyricsRequestManager is not set in LyricsEditRequest");
        }
        return lyricsRequestManager.getLyricsEditRequests("Approved");
    }

    public String[][] getRejectedLyricsEditRequests() {
        if (lyricsRequestManager == null) {
            throw new IllegalStateException("LyricsRequestManager is not set in LyricsEditRequest");
        }
        return lyricsRequestManager.getLyricsEditRequests("Rejected");
    }

    public void setLyricsRequestManager(LyricsRequestManager lyricsRequestManager) {
        this.lyricsRequestManager = lyricsRequestManager;
    }
}