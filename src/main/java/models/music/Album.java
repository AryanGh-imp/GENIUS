package models.music;

import models.account.Artist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Album {
    private static final String DEFAULT_RELEASE_DATE = "Not set";

    private String title;
    private String releaseDate;
    private final Artist artist;
    private final List<Song> songs;
    private String albumArtPath;

    public Album(String title, String releaseDate, Artist artist) {
        this.title = Objects.requireNonNull(title, "Album title cannot be null").trim();
        if (this.title.isEmpty()) {
            throw new IllegalArgumentException("Album title cannot be empty");
        }
        this.artist = Objects.requireNonNull(artist, "Artist cannot be null");
        this.releaseDate = releaseDate != null ? releaseDate : DEFAULT_RELEASE_DATE;
        this.songs = new ArrayList<>();
        // Reverse reference: Add album to artist list
        if (!artist.getAlbums().contains(this)) {
            artist.addAlbum(this); // Using the addAlbum method to manage
        }
    }

    public void setTitle(String newTitle) {
        this.title = Objects.requireNonNull(newTitle, "New album title cannot be null").trim();
        if (this.title.isEmpty()) {
            throw new IllegalArgumentException("New album title cannot be empty");
        }
    }

    public Album addSong(Song song) {
        Objects.requireNonNull(song, "Song cannot be null");
        songs.add(song);
        song.setAlbum(this);
        return this;
    }

    public boolean removeSong(Song song) {
        if (song == null) return false;
        if (songs.remove(song)) {
            song.setAlbum(null);
            return true;
        }
        return false;
    }

    public boolean hasSong(Song song) {
        return song != null && songs.contains(song);
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate != null ? releaseDate : DEFAULT_RELEASE_DATE;
    }

    public Artist getArtist() {
        return artist;
    }

    public List<Song> getSongs() {
        return Collections.unmodifiableList(songs);
    }

    public int getSongCount() {
        return songs.size();
    }

    public String getAlbumArtPath() {
        return albumArtPath;
    }

    public void setAlbumArtPath(String albumArtPath) {
        this.albumArtPath = albumArtPath;
    }

    public void setSongs(List<Song> songs) {
        Objects.requireNonNull(songs, "Songs list cannot be null");
        this.songs.clear();
        for (Song song : songs) {
            this.songs.add(song);
            song.setAlbum(this);
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
        return "Album: " + title +
                " by " + artist.getNickName() +
                " (" + songs.size() + " songs)";
    }
}