package models.music;

import models.account.Artist;
import services.file.SongFileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Album {
    private final String title;
    private final String releaseDate;
    private final List<Song> tracklist;
    private final Artist artist;
    private final SongFileManager songManager = new SongFileManager();

    public Album(String title, String releaseDate, Artist artist) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (releaseDate == null || releaseDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Release date cannot be null or empty");
        }
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        this.title = title;
        this.releaseDate = releaseDate;
        this.artist = artist;
        this.tracklist = new ArrayList<>();
        saveAlbum();
    }

    public void addSong(Song song) {
        if (!artist.isApproved()) {
            throw new IllegalStateException("Cannot add song to album: Artist is not approved");
        }
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null");
        }
        File sourceFile = new File(song.getFilePath());
        if (!sourceFile.exists()) {
            throw new IllegalStateException("Song file does not exist: " + song.getFilePath());
        }
        tracklist.add(song);
        song.setAlbum(this);
        List<String> artistNickNames = song.getArtists().stream()
                .map(Artist::getNickName)
                .collect(Collectors.toList());
        try {
            songManager.saveSong(artistNickNames, song.getTitle(), title, song.getLyrics(), new File(song.getFilePath()), song.getReleaseDate());
            saveAlbum();
        } catch (Exception e) {
            tracklist.remove(song);
            song.setAlbum(null);
            throw new IllegalStateException("Failed to save song: " + e.getMessage(), e);
        }
    }

    public boolean removeSong(Song song) {
        if (song == null) {
            return false;
        }
        boolean removed = tracklist.remove(song);
        if (removed) {
            song.setAlbum(null);
            saveAlbum();
        }
        return removed;
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public List<Song> getSongs() {
        return new ArrayList<>(tracklist);
    }

    public Artist getArtist() {
        return artist;
    }

    private void saveAlbum() {
        if (!artist.isApproved()) {
            throw new IllegalStateException("Cannot save album: Artist is not approved");
        }
        List<String> songTitles = tracklist.stream()
                .map(Song::getTitle)
                .collect(Collectors.toList());
        songManager.saveAlbum(artist.getNickName(), title, releaseDate, songTitles);
    }

    @Override
    public String toString() {
        return "Album{title='" + title + "', releaseDate='" + releaseDate +
                "', tracklist=" + tracklist + ", artist=" + artist.getNickName() + "}";
    }
}