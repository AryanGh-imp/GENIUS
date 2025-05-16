package models.DTO;

import services.file.LyricsRequestManager;

public class LyricsEditRequestDTO {
    private final String email;
    private final String artistNickname;
    private final String songTitle;
    private final String albumName;
    private final String suggestedLyrics;
    private final String timestamp;
    private final String status;
    private LyricsRequestManager lyricsRequestManager;

    public LyricsEditRequestDTO(String email, String artistNickname, String songTitle, String albumName,
                                String suggestedLyrics, String timestamp, String status) {
        this.email = email;
        this.artistNickname = artistNickname;
        this.songTitle = songTitle;
        this.albumName = albumName;
        this.suggestedLyrics = suggestedLyrics;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String email() { return email; }
    public String artistNickname() { return artistNickname; }
    public String songTitle() { return songTitle; }
    public String albumName() { return albumName; }
    public String suggestedLyrics() { return suggestedLyrics; }
    public String timestamp() { return timestamp; }
    public String status() { return status; }

    public void setLyricsRequestManager(LyricsRequestManager lyricsRequestManager) {
        this.lyricsRequestManager = lyricsRequestManager;
    }

    public LyricsRequestManager getLyricsRequestManager() {
        return lyricsRequestManager;
    }
}