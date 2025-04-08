package models.music;

import utils.FileUtil;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object (DTO) for a song, used for search and chart operations.
 */
public class SongDTO {
    private final String title;
    private final String artistName;
    private final String albumName;
    private final int views;
    private final String metaFilePath;
    private final String releaseDate;

    public SongDTO(String title, String artistName, String albumName, int views, String metaFilePath, String releaseDate) {
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
        this.title = title;
        this.artistName = artistName;
        this.albumName = albumName;
        this.views = views;
        this.metaFilePath = metaFilePath;
        this.releaseDate = releaseDate;
    }

    /**
     * Gets the title of the song.
     *
     * @return The song title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the name of the artist.
     *
     * @return The artist name.
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Gets the name of the album.
     *
     * @return The album name, or null if the song is a single.
     */
    public String getAlbumName() {
        return albumName;
    }

    /**
     * Gets the number of views for the song.
     *
     * @return The number of views.
     */
    public int getViews() {
        return views;
    }

    /**
     * Gets the path to the song's metadata file.
     *
     * @return The metadata file path.
     */
    public String getMetaFilePath() {
        return metaFilePath;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    /**
     * Lazily loads the lyrics of the song from the lyrics file.
     *
     * @return The lyrics of the song, or null if the lyrics file does not exist.
     */
    public String getLyrics() {
        String lyricsFilePath = metaFilePath.replace(".txt", "-current-lyrics.txt");
        File lyricsFile = new File(lyricsFilePath);
        if (lyricsFile.exists()) {
            List<String> lines = FileUtil.readFile(lyricsFilePath);
            return String.join("\n", lines);
        }
        return null;
    }

    @Override
    public String toString() {
        return title + " by " + artistName + (albumName != null ? " (Album: " + albumName + ")" : "") + " - Views: " + views;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SongDTO songDTO = (SongDTO) o;
        return views == songDTO.views &&
                Objects.equals(title, songDTO.title) &&
                Objects.equals(artistName, songDTO.artistName) &&
                Objects.equals(albumName, songDTO.albumName) &&
                Objects.equals(metaFilePath, songDTO.metaFilePath) &&
                Objects.equals(releaseDate, songDTO.releaseDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artistName, albumName, views, metaFilePath, releaseDate);
    }
}