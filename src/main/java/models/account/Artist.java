package models.account;

import models.music.Album;
import models.music.Song;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import services.file.UserFileManager;

import java.util.ArrayList;
import java.util.List;

public class Artist extends Account {
    private boolean approved;
    private final List<User> followers;
    private final List<Song> singles;
    private final List<Album> albums;
    private ArtistFileManager artistFileManager;
    private SongFileManager songFileManager;
    private UserFileManager userFileManager;

    public Artist(String email, String nickName, String password) {
        super(email, nickName, password);
        this.approved = false;
        this.followers = new ArrayList<>();
        this.singles = new ArrayList<>();
        this.albums = new ArrayList<>();
        this.artistFileManager = null;
        this.songFileManager = null;
        this.userFileManager = null;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public void setArtistFileManager(ArtistFileManager artistFileManager) {
        this.artistFileManager = artistFileManager;
    }

    public void setSongFileManager(SongFileManager songFileManager) {
        this.songFileManager = songFileManager;
    }

    public void setUserFileManager(UserFileManager userFileManager) {
        this.userFileManager = userFileManager;
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

    public ArtistFileManager getArtistFileManager() {
        return artistFileManager;
    }

    public SongFileManager getSongFileManager() {
        return songFileManager;
    }

    public UserFileManager getUserFileManager() {
        return userFileManager;
    }

    @Override
    public final String getRole() {
        return "Artist";
    }

    public void loadFollowersFromFile() {
        if (artistFileManager == null) {
            throw new IllegalStateException("ArtistFileManager is not set for artist '" + getNickName() + "'.");
        }
        if (userFileManager == null) {
            throw new IllegalStateException("UserFileManager is not set for artist '" + getNickName() + "'.");
        }
        try {
            artistFileManager.setUserFileManager(userFileManager);
            followers.clear();
            followers.addAll(artistFileManager.loadFollowers(this));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load followers for artist '" + getNickName() + "': " + e.getMessage(), e);
        }
    }

    private void saveSongsAndAlbums() {
        if (!approved) {
            throw new IllegalStateException("Cannot save songs and albums for an unapproved artist.");
        }
        if (songFileManager == null) {
            throw new IllegalStateException("SongFileManager is not set for artist '" + getNickName() + "'.");
        }
        try {
            songFileManager.saveSongsAndAlbumsForArtist(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save songs and albums for artist '" + getNickName() + "': " + e.getMessage(), e);
        }
    }

    public void loadSongsAndAlbums() {
        if (!approved) {
            throw new IllegalStateException("Cannot load songs and albums for an unapproved artist.");
        }
        if (songFileManager == null) {
            throw new IllegalStateException("SongFileManager is not set for artist '" + getNickName() + "'.");
        }
        try {
            songFileManager.loadSongsAndAlbumsForArtist(this);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load songs and albums for artist '" + getNickName() + "': " + e.getMessage(), e);
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