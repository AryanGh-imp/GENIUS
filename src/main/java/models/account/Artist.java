package models.account;

import models.music.Album;
import models.music.Song;
import services.file.ArtistFileManager;
import services.file.SongFileManager;

import java.util.ArrayList;
import java.util.List;

public class Artist extends Account {
    private boolean approved;
    private final List<User> followers;
    private final List<Song> singles;
    private final List<Album> albums;

    public Artist(String email, String nickName, String password) {
        super(email, nickName, password);
        this.approved = false;
        this.followers = new ArrayList<>();
        this.singles = new ArrayList<>();
        this.albums = new ArrayList<>();
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public void addSingle(Song song) {
        if (!approved) {
            throw new IllegalStateException("Cannot add singles to an unapproved artist.");
        }
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null.");
        }
        if (!singles.contains(song)) {
            singles.add(song);
            saveSongsAndAlbums();
        }
    }

    public void addAlbum(Album album) {
        if (!approved) {
            throw new IllegalStateException("Cannot add albums to an unapproved artist.");
        }
        if (album == null) {
            throw new IllegalArgumentException("Album cannot be null.");
        }
        if (!albums.contains(album)) {
            albums.add(album);
            saveSongsAndAlbums();
        }
    }

    public List<User> getFollowers() {
        return new ArrayList<>(followers);
    }

    public List<Song> getSingles() {
        return new ArrayList<>(singles);
    }

    public List<Album> getAlbums() {
        return new ArrayList<>(albums);
    }

    @Override
    public final String getRole() {
        return "Artist";
    }

    public void loadSongsAndAlbums(SongFileManager songFileManager, ArtistFileManager artistFileManager) {
        if (!approved) {
            throw new IllegalStateException("Cannot load songs and albums for an unapproved artist.");
        }
        if (songFileManager == null) {
            throw new IllegalArgumentException("SongFileManager cannot be null.");
        }
        if (artistFileManager == null) {
            throw new IllegalArgumentException("ArtistFileManager cannot be null.");
        }
        try {
            songFileManager.loadSongsAndAlbumsForArtist(this);
        } catch (Exception e) {
            System.err.println("Failed to load songs and albums for artist '" + getNickName() + "': " + e.getMessage());
            throw new IllegalStateException("Failed to load songs and albums for artist '" + getNickName() + "': " + e.getMessage(), e);
        }
    }

    public void loadFollowers(ArtistFileManager artistFileManager, List<User> allUsers) {
        if (artistFileManager == null) {
            throw new IllegalArgumentException("ArtistFileManager cannot be null.");
        }
        if (allUsers == null) {
            throw new IllegalArgumentException("All users list cannot be null.");
        }
        followers.clear();
        followers.addAll(artistFileManager.loadFollowers(this, allUsers));
    }

    private void saveSongsAndAlbums() {
        if (!approved) {
            throw new IllegalStateException("Cannot save songs and albums for an unapproved artist.");
        }
        SongFileManager songFileManager = new SongFileManager();
        try {
            songFileManager.saveSongsAndAlbumsForArtist(this);
        } catch (Exception e) {
            System.err.println("Failed to save songs and albums for artist '" + getNickName() + "': " + e.getMessage());
            throw new IllegalStateException("Failed to save songs and albums for artist '" + getNickName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    protected void addAdditionalData(List<String> data) {
        data.add("Approved: " + approved);
    }

    @Override
    public String toString() {
        return "Artist: " + getNickName() + " (" + getEmail() + ") - Approved: " + approved;
    }
}