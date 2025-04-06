package models.music;

import models.account.Artist;

import java.util.ArrayList;
import java.util.List;

public class Song {
    private final String title;
    private String lyrics;
    private final String releaseDate;
    private int likes;
    private int views;
    private String filePath;
    private Album album;
    private final List<Artist> artists;
    private final List<Comment> comments;

    public Song(String title, String lyrics, String releaseDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (releaseDate == null || releaseDate.trim().isEmpty()) {
            throw new IllegalArgumentException("Release date cannot be null or empty");
        }
        this.title = title;
        this.lyrics = lyrics;
        this.releaseDate = releaseDate;
        this.likes = 0;
        this.views = 0;
        this.artists = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        if (lyrics != null && lyrics.trim().isEmpty()) {
            throw new IllegalArgumentException("Lyrics cannot be empty if provided");
        }
        this.lyrics = lyrics;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        if (likes < 0) {
            throw new IllegalArgumentException("Likes cannot be negative");
        }
        this.likes = likes;
    }

    public void incrementLikes() {
        this.likes++;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        if (views < 0) {
            throw new IllegalArgumentException("Views cannot be negative");
        }
        this.views = views;
    }

    public void incrementViews() {
        this.views++;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        if (filePath != null && filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty if provided");
        }
        this.filePath = filePath;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public void addArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null");
        }
        if (!artists.contains(artist)) {
            artists.add(artist);
        }
    }

    public boolean removeArtist(Artist artist) {
        return artists.remove(artist);
    }

    public List<Artist> getArtists() {
        return new ArrayList<>(artists);
    }

    public String getArtistNickName() {
        return artists.isEmpty() ? "" : artists.getFirst().getNickName();
    }

    public String getAlbumName() {
        return album != null ? album.getTitle() : "";
    }

    public void addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Comment cannot be null");
        }
        comments.add(comment);
    }

    public boolean removeComment(Comment comment) {
        return comments.remove(comment);
    }

    public List<Comment> getComments() {
        return new ArrayList<>(comments);
    }

    @Override
    public String toString() {
        return "Song{title='" + title + "', artists=" + artists +
                ", album=" + (album != null ? album.getTitle() : "N/A") +
                ", releaseDate='" + releaseDate + "', likes=" + likes +
                ", views=" + views + "}";
    }
}