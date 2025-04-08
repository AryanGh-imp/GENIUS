package models.music;

import models.account.Artist;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a song in the system.
 */
public class Song {
    private final String title;
    private String lyrics;
    private String releaseDate;
    private int likes;
    private int views;
    private Album album;
    private final List<Artist> artists;

    /**
     * Constructs a new Song with the specified title, lyrics, and release date.
     *
     * @param title       The title of the song.
     * @param lyrics      The lyrics of the song.
     * @param releaseDate The release date of the song.
     */
    public Song(String title, String lyrics, String releaseDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Song title cannot be null or empty");
        }
        this.title = title;
        this.lyrics = lyrics != null ? lyrics : "";
        this.releaseDate = releaseDate != null ? releaseDate : "Not set";
        this.likes = 0;
        this.views = 0;
        this.artists = new ArrayList<>();
    }

    /**
     * Adds an artist to the song.
     *
     * @param artist The artist to add.
     * @throws IllegalArgumentException if the artist is null.
     */
    public void addArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (!artists.contains(artist)) {
            artists.add(artist);
        }
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
     * Gets the lyrics of the song.
     *
     * @return The song lyrics.
     */
    public String getLyrics() {
        return lyrics;
    }

    /**
     * Sets the lyrics of the song.
     *
     * @param lyrics The new lyrics.
     */
    public void setLyrics(String lyrics) {
        this.lyrics = lyrics != null ? lyrics : "";
    }

    /**
     * Gets the release date of the song.
     *
     * @return The release date.
     */
    public String getReleaseDate() {
        return releaseDate;
    }

    /**
     * Sets the release date of the song.
     *
     * @param releaseDate The new release date.
     */
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate != null ? releaseDate : "Not set";
    }

    /**
     * Gets the number of likes for the song.
     *
     * @return The number of likes.
     */
    public int getLikes() {
        return likes;
    }

    /**
     * Sets the number of likes for the song.
     *
     * @param likes The new number of likes.
     */
    public void setLikes(int likes) {
        this.likes = Math.max(0, likes);
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
     * Sets the number of views for the song.
     *
     * @param views The new number of views.
     */
    public void setViews(int views) {
        this.views = Math.max(0, views);
    }

    /**
     * Gets the album the song belongs to.
     *
     * @return The album, or null if the song is a single.
     */
    public Album getAlbum() {
        return album;
    }

    /**
     * Sets the album the song belongs to.
     *
     * @param album The album.
     */
    public void setAlbum(Album album) {
        this.album = album;
    }

    /**
     * Gets a copy of the list of artists for the song.
     *
     * @return A new list containing the artists.
     */
    public List<Artist> getArtists() {
        return new ArrayList<>(artists);
    }

    @Override
    public String toString() {
        String artistNames = artists.stream()
                .map(Artist::getNickName)
                .collect(Collectors.joining(", "));
        return "Song: " + title + (artistNames.isEmpty() ? "" : " by " + artistNames);
    }
}