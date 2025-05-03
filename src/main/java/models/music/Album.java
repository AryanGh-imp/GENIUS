package models.music;

import models.account.Artist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Album {
    private final String title;
    private String releaseDate;
    private final Artist artist;
    private final List<Song> songs;
    private String albumArtPath;

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

    public void addSong(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null");
        }
        songs.add(song);
        song.setAlbum(this);
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate != null ? releaseDate : "Not set";
    }

    public Artist getArtist() {
        return artist;
    }

    public List<Song> getSongs() {
            return new ArrayList<>(songs);
    }

    public String getAlbumArtPath() {
        return albumArtPath;
    }

    public void setAlbumArtPath(String albumArtPath) {
        this.albumArtPath = albumArtPath;
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