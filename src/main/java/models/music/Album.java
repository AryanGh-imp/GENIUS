package models.music;

import models.account.Artist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an album in the system.
 */
public class Album {
    private final String title;
    private String releaseDate;
    private final Artist artist;
    private final List<Song> songs;

    private boolean isDirty = true; // Defaults to true, because it's a new album

    /**
     * Constructs a new Album with the specified title, release date, and artist.
     *
     * @param title       The title of the album.
     * @param releaseDate The release date of the album.
     * @param artist      The artist who created the album.
     */
    public Album(String title, String releaseDate, Artist artist) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Album title cannot be null or empty");
        }
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        this.title = title;
        this.releaseDate = releaseDate != null ? releaseDate : "Not set";
        this.artist = artist;
        this.songs = Collections.synchronizedList(new ArrayList<>());
    }

    public boolean isDirty() {
        return isDirty || songs.stream().anyMatch(Song::isDirty);
    }

    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    public void addSong(Song song) {
        songs.add(song);
        this.isDirty = true;
    }

    /**
     * Removes a song from the album.
     *
     * @param song The song to remove.
     * @return true if the song was removed, false if it wasn't in the album
     */
    public boolean removeSong(Song song) {
        if (song == null) {
            return false;
        }
        synchronized (songs) {
            boolean removed = songs.remove(song);
            if (removed) {
                song.setAlbum(null);
            }
            return removed;
        }
    }

    /**
     * Gets the title of the album.
     *
     * @return The album title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the release date of the album.
     *
     * @return The release date.
     */
    public String getReleaseDate() {
        return releaseDate;
    }

    /**
     * Sets the release date of the album.
     *
     * @param releaseDate The new release date.
     */
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate != null ? releaseDate : "Not set";
    }

    /**
     * Gets the artist of the album.
     *
     * @return The artist.
     */
    public Artist getArtist() {
        return artist;
    }

    /**
     * Gets a copy of the list of songs in the album.
     *
     * @return A new list containing the songs.
     */
    public List<Song> getSongs() {
        synchronized (songs) {
            return new ArrayList<>(songs);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album album)) return false;
        return Objects.equals(title, album.title) && 
               Objects.equals(artist, album.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, artist);
    }

    @Override
    public String toString() {
        synchronized (songs) {
            return "Album: " + title + 
                   " by " + artist.getNickName() + 
                   " (" + songs.size() + " songs)";
        }
    }
}