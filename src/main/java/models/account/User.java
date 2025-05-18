package models.account;

import services.SessionManager;
import services.file.ArtistFileManager;
import services.file.UserFileManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class User extends Account {
    private final Set<Artist> followingArtists;

    public User(String email, String nickName, String password) {
        super(email, nickName, password);
        this.followingArtists = new HashSet<>();
    }

    public void followArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null.");
        }
        if (!artist.isApproved()) {
            throw new IllegalStateException("Cannot follow an unapproved artist.");
        }
        if (followingArtists.add(artist)) {
            saveFollowingArtists();
        }
    }

    public void unfollowArtist(Artist artist) {
        if (artist == null) {
            throw new IllegalArgumentException("Artist cannot be null.");
        }
        if (followingArtists.remove(artist)) {
            saveFollowingArtists();
        }
    }

    public List<Artist> getFollowingArtists() {
        return new ArrayList<>(followingArtists);
    }

    public void setFollowingArtists(List<Artist> artists) {
        if (artists == null) {
            throw new IllegalArgumentException("Artists list cannot be null.");
        }
        followingArtists.clear();
        followingArtists.addAll(artists);
        saveFollowingArtists();
    }

    private void saveFollowingArtists() {
        UserFileManager userFileManager = SessionManager.getInstance().getUserFileManager();
        userFileManager.saveFollowingArtists(getNickName(), getFollowingArtists());
    }

    @Override
    public final String getRole() {
        return "User";
    }

    public void loadFollowingArtistsFromFile(ArtistFileManager artistFileManager, UserFileManager userFileManager) {
        if (artistFileManager == null) {
            throw new IllegalArgumentException("ArtistFileManager cannot be null.");
        }
        if (userFileManager == null) {
            throw new IllegalArgumentException("UserFileManager cannot be null.");
        }
        List<Artist> loadedArtists = userFileManager.loadFollowingArtistsFromFile(getNickName(), artistFileManager.loadAllArtists());
        setFollowingArtists(loadedArtists);
    }
}