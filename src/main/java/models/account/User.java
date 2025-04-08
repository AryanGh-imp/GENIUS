package models.account;

import services.file.ArtistFileManager;
import services.file.UserFileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a User account in the system, extending the base Account class.
 * A User can follow and unfollow artists.
 */
public class User extends Account {
    private final List<Artist> followingArtists;
    private ArtistFileManager artistFileManager;
    private UserFileManager userFileManager;

    private boolean followingDirty = false;

    /**
     * Constructs a new User with the specified email, nickname, and password.
     *
     * @param email    The user's email address.
     * @param nickName The user's nickname.
     * @param password The user's password.
     */
    public User(String email, String nickName, String password) {
        super(email, nickName, password);
        this.followingArtists = new ArrayList<>();
    }

    public void followArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null.");
        }
        if (!artist.isApproved()) {
            throw new IllegalStateException("Cannot follow an unapproved artist.");
        }
        if (!followingArtists.contains(artist)) {
            followingArtists.add(artist);
            followingDirty = true;
            saveFollowingArtists();
        }
    }

    public void unfollowArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null.");
        }
        if (followingArtists.remove(artist)) {
            followingDirty = true;
            saveFollowingArtists();
        }
    }

    public void saveFollowingArtists() {
        if (!followingDirty) {
            return;
        }
        if (userFileManager == null) {
            throw new IllegalStateException("UserFileManager is not set for user '" + getNickName() + "'.");
        }
        try {
            userFileManager.saveFollowingArtists(getNickName(), followingArtists);
            followingDirty = false;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save following artists for user '" + getNickName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Returns a copy of the list of artists the user is following.
     *
     * @return A new list containing the followed artists.
     */
    public List<Artist> getFollowingArtists() {
        return new ArrayList<>(followingArtists);
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
     * @return The role "User".
     */
    @Override
    public final String getRole() {
        return "User";
    }

    /**
     * Loads the list of followed artists from the file system.
     *
     * @throws IllegalStateException if the UserFileManager or ArtistFileManager is not set, or loading fails.
     */
    public void loadFollowingArtistsFromFile() {
        if (userFileManager == null) {
            throw new IllegalStateException("UserFileManager is not set for user '" + getNickName() + "'.");
        }
        if (artistFileManager == null) {
            throw new IllegalStateException("ArtistFileManager is not set for user '" + getNickName() + "'.");
        }
        try {
            followingArtists.clear();
            followingArtists.addAll(userFileManager.loadFollowingArtists(getNickName(), artistFileManager.loadAllArtists()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load following artists for user '" + getNickName() + "': " + e.getMessage(), e);
        }
    }
}