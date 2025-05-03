package models.music;


public record SongDTO(String title, String artistName, String albumName, int views, String metaFilePath, String releaseDate, String albumArtPath) {
    public SongDTO {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (artistName == null || artistName.trim().isEmpty()) {
            throw new IllegalArgumentException("Artist name cannot be null or empty");
        }
        if (views < 0) {
            throw new IllegalArgumentException("Views cannot be negative");
        }
        if (metaFilePath == null || metaFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Meta file path cannot be null or empty");
        }
        if (releaseDate == null || releaseDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Release date cannot be null or empty");
        }
    }

    @Override
    public String toString() {
        return title + " by " + artistName + (albumName != null ? " (Album: " + albumName + ")" : "") + " - Views: " + views;
    }
}