package models.music;

public class SongDTO {
    private final String title;
    private final String artistName;
    private final String albumName; // null for singles
    private final int views;
    private final String metaFilePath;

    public SongDTO(String title, String artistName, String albumName, int views, String metaFilePath) {
        this.title = title;
        this.artistName = artistName;
        this.albumName = albumName;
        this.views = views;
        this.metaFilePath = metaFilePath;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public int getViews() {
        return views;
    }

    public String getMetaFilePath() {
        return metaFilePath;
    }

    @Override
    public String toString() {
        return "Song: " + title + " by " + artistName + (albumName != null ? " (Album: " + albumName + ")" : "") + " - Views: " + views;
    }
}