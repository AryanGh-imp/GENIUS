package models;

/**
 * Represents a lyrics edit request with details such as artist nickname, email, song title, album name,
 * suggested lyrics, timestamp, and status.
 */
public class LyricsEditRequest {
    private final String email;
    private final String artistNickname;
    private final String songTitle;
    private final String albumName;
    private final String suggestedLyrics;
    private final String timestamp;
    private final String status;

    /**
     * Constructs a new LyricsEditRequest with the specified details.
     *
     * @param email            The email of the artist.
     * @param artistNickname   The nickname of the artist.
     * @param songTitle        The title of the song.
     * @param albumName        The name of the album (null for singles).
     * @param suggestedLyrics  The suggested lyrics.
     * @param timestamp        The timestamp of the request.
     * @param status           The status of the request (e.g., Pending, Approved, Rejected).
     */
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
    public String getEmail() { return email; }
    public String getArtistNickname() { return artistNickname; }
    public String getSongTitle() { return songTitle; }
    public String getAlbumName() { return albumName; }
    public String getSuggestedLyrics() { return suggestedLyrics; }
    public String getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "Lyrics Edit Request: " + songTitle + " by " + artistNickname + " (" + status + ")";
    }
}
