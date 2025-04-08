package models.account;

import models.music.Album;
import models.music.Song;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import services.file.UserFileManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an Artist account in the system, extending the base Account class.
 * An Artist can have singles, albums, and followers.
 */
public class Artist extends Account {
    private boolean approved;
    private final List<User> followers;
    private final List<Song> singles;
    private final List<Album> albums;
    private ArtistFileManager artistFileManager;
    private SongFileManager songFileManager;
    private UserFileManager userFileManager;

    private boolean isDirty = false;
    private boolean followersDirty = false;

    /**
     * Constructs a new Artist with the specified email, nickname, and password.
     *
     * @param email    The artist's email address.
     * @param nickName The artist's nickname.
     * @param password The artist's password.
     */
    public Artist(String email, String nickName, String password) {
        super(email, nickName, password);
        this.approved = false;
        this.followers = Collections.synchronizedList(new ArrayList<>());
        this.singles = Collections.synchronizedList(new ArrayList<>());
        this.albums = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Checks if the artist is approved.
     *
     * @return True if the artist is approved, false otherwise.
     */
    public boolean isApproved() {
        return approved;
    }

    /**
     * Sets the approval status of the artist.
     *
     * @param approved The approval status.
     */
    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public void addFollower(User user) {
        if (!approved) {
            throw new IllegalStateException("Cannot add followers to an unapproved artist.");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (!followers.contains(user)) {
            followers.add(user);
            followersDirty = true;
            saveFollowers();
        }
    }

    public void removeFollower(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        if (followers.remove(user)) {
            followersDirty = true;
            saveFollowers();
        }
    }

    public void saveFollowers() {
        if (!followersDirty) {
            return;
        }
        if (artistFileManager == null) {
            throw new IllegalStateException("ArtistFileManager is not set for artist '" + getNickName() + "'.");
        }
        try {
            artistFileManager.saveFollowers(this, followers);
            followersDirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save followers for artist '" + getNickName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Adds a single song to the artist's singles list.
     *
     * @param song The song to add.
     * @throws IllegalArgumentException if the song is null.
     * @throws IllegalStateException    if the artist is not approved or saving fails.
     */
    public void addSingle(Song song) {
        if (!approved) {
            throw new IllegalStateException("Cannot add singles to an unapproved artist.");
        }
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null.");
        }
        if (!singles.contains(song)) {
            singles.add(song);
            isDirty = true;
        }
    }

    public void removeSingle(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null.");
        }
        if (singles.remove(song)) {
            isDirty = true;
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
            isDirty = true;
            saveSongsAndAlbums();
        }
    }

    public void removeAlbum(Album album) {
        if (album == null) {
            throw new IllegalArgumentException("Album cannot be null.");
        }
        if (albums.remove(album)) {
            isDirty = true;
            saveSongsAndAlbums();
        }
    }

    /**
     * Returns a copy of the list of followers.
     *
     * @return A new list containing the followers.
     */
    public List<User> getFollowers() {
        return new ArrayList<>(followers);
    }

    /**
     * Returns a copy of the list of singles.
     *
     * @return A new list containing the singles.
     */
    public List<Song> getSingles() {
        return new ArrayList<>(singles);
    }

    /**
     * Returns a copy of the list of albums.
     *
     * @return A new list containing the albums.
     */
    public List<Album> getAlbums() {
        return new ArrayList<>(albums);
    }

    /**
     * Gets the ArtistFileManager instance.
     *
     * @return The ArtistFileManager instance.
     */
    public ArtistFileManager getArtistFileManager() {
        return artistFileManager;
    }

    /**
     * Gets the SongFileManager instance.
     *
     * @return The SongFileManager instance.
     */
    public SongFileManager getSongFileManager() {
        return songFileManager;
    }

    /**
     * Gets the UserFileManager instance.
     *
     * @return The UserFileManager instance.
     */
    public UserFileManager getUserFileManager() {
        return userFileManager;
    }

    /**
     * Gets the role of the account.
     *
     * @return The role "Artist".
     */
    @Override
    public final String getRole() {
        return "Artist";
    }

    /**
     * Loads the list of followers from the file system.
     *
     * @throws IllegalStateException if the ArtistFileManager or UserFileManager is not set, or loading fails.
     */
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

    /**
     * Saves the artist's songs and albums to the file system.
     *
     * @throws IllegalStateException if the artist is not approved, SongFileManager is not set, or saving fails.
     */
    public void saveSongsAndAlbums() {
        if (!isDirty) {
            return; // If no changes have occurred, do not save.
        }
        if (!approved) {
            throw new IllegalStateException("Cannot save songs and albums for an unapproved artist.");
        }
        if (songFileManager == null) {
            throw new IllegalStateException("SongFileManager is not set for artist '" + getNickName() + "'.");
        }
        try {
            songFileManager.saveSongsAndAlbumsForArtist(this);
            isDirty = false; // After saving, reset the flag.
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save songs and albums for artist '" + getNickName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Loads the artist's songs and albums from the file system.
     *
     * @throws IllegalStateException if the artist is not approved, SongFileManager is not set, or loading fails.
     */
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