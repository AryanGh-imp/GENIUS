package models.music;

import models.account.Artist;
import services.file.SongFileManager;
import utils.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Song {
    private final String title;
    private String lyrics;
    private final String releaseDate;
    private int likes;
    private int views;
    private Album album;
    private String albumArtPath;
    private final List<Artist> artists;

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

    public void addArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (!artists.contains(artist)) {
            artists.add(artist);
            saveChanges();
        }
    }

    // TODO : In the future ...
    public void removeArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (artists.remove(artist)) {
            saveChanges();
        }
    }

    public void incrementLikes() {
        this.likes++;
        saveChanges();
    }

    public void incrementViews() {
        this.views++;
        saveChanges();
    }

    private int validateNonNegative(int value) {
        return Math.max(0, value);
    }

    public String getTitle() {
        return title;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics != null ? lyrics : "";
        saveChanges();
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = validateNonNegative(likes);
        saveChanges();
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = validateNonNegative(views);
        saveChanges();
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
        saveChanges();
    }

    public String getAlbumArtPath() {
        return albumArtPath;
    }

    public void setAlbumArtPath(String albumArtPath) {
        this.albumArtPath = albumArtPath;
        saveChanges();
    }

    public List<Artist> getArtists() {
        return new ArrayList<>(artists);
    }

    public String getMetaFilePath() {
        if (artists.isEmpty()) {
            throw new IllegalStateException("Cannot determine meta file path without at least one artist");
        }
        String safeArtistNickName = FileUtil.sanitizeFileName(artists.getFirst().getNickName());
        String safeSongTitle = FileUtil.sanitizeFileName(title);
        String songDir = album != null
                ? FileUtil.DATA_DIR + "artists/" + safeArtistNickName + "/albums/" + FileUtil.sanitizeFileName(album.getTitle()) + "/" + safeSongTitle + "/"
                : FileUtil.DATA_DIR + "artists/" + safeArtistNickName + "/singles/" + safeSongTitle + "/";
        return songDir + safeSongTitle + ".txt";
    }

    private void saveChanges() {
        if (artists.isEmpty()) {
            throw new IllegalStateException("Cannot save song without at least one artist");
        }
        SongFileManager songFileManager = new SongFileManager();
        List<String> artistNickNames = artists.stream()
                .map(Artist::getNickName)
                .collect(Collectors.toList());
        songFileManager.saveSong(artistNickNames, title, album != null ? album.getTitle() : null,
                lyrics, releaseDate, likes, views, albumArtPath);
    }

    @Override
    public String toString() {
        String artistNames = artists.stream()
                .map(Artist::getNickName)
                .collect(Collectors.joining(", "));
        return "Song: " + title + (artistNames.isEmpty() ? "" : " by " + artistNames);
    }
}