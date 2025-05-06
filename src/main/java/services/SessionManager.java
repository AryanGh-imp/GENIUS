package services;

import models.account.Account;
import models.account.Artist;
import models.account.User;
import services.file.ArtistFileManager;
import services.file.UserFileManager;

import java.util.List;

public class SessionManager {
    private static Account currentAccount;
    private static String selectedArtist;
    private static String selectedSong;
    private static String selectedAlbum;

    private static final SessionManager INSTANCE = new SessionManager();
    private static final UserFileManager userFileManager = new UserFileManager();
    private static final ArtistFileManager artistFileManager = new ArtistFileManager();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setCurrentAccount(Account account) {
        if (account == null) {
            System.err.println("Account cannot be null");
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (account.getEmail() == null || account.getEmail().isEmpty()) {
            System.err.println("Email cannot be null or empty");
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (account.getNickName() == null || account.getNickName().isEmpty()) {
            System.err.println("Nickname cannot be null or empty");
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        if (account.getRole() == null || account.getRole().isEmpty()) {
            System.err.println("Role cannot be null or empty");
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        try {
            switch (account) {
                case User user -> {
                    try {
                        user.loadFollowingArtistsFromFile(artistFileManager, userFileManager);
                    } catch (Exception e) {
                        System.err.println("Failed to load following artists for user '" + user.getNickName() + "': " + e.getMessage());
                        throw new IllegalStateException("Failed to load following artists", e);
                    }
                }
                case Artist artist -> {
                    try {
                        List<User> allUsers = userFileManager.loadAllUsers();
                        // Directly use artistFileManager to load followers
                        List<User> followers = artistFileManager.loadFollowers(artist, allUsers);
                        // Update followers in the artist instance
                        artist.getFollowers().clear();
                        artist.getFollowers().addAll(followers);
                    } catch (Exception e) {
                        System.err.println("Failed to load followers for artist '" + artist.getNickName() + "': " + e.getMessage());
                        throw new IllegalStateException("Failed to load followers", e);
                    }
                }
                default -> System.err.println("Unknown account type for: " + account.getNickName());
            }
            currentAccount = account;
        } catch (Exception e) {
            System.err.println("Failed to set current account for: " + account.getNickName() + ", error: " + e.getMessage());
            throw new IllegalStateException("Failed to set current account", e);
        }
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public String getCurrentUsername() {
        return currentAccount != null ? currentAccount.getNickName() : "Guest";
    }

    public void setSelectedArtist(String artist) {
        selectedArtist = artist;
    }

    public String getSelectedArtist() {
        return selectedArtist;
    }

    public void setSelectedSong(String song) {
        selectedSong = song;
    }

    public String getSelectedSong() {
        return selectedSong;
    }

    public void setSelectedAlbum(String album) {
        selectedAlbum = album;
    }

    public String getSelectedAlbum() {
        return selectedAlbum;
    }

    public void clearSession() {
        currentAccount = null;
        selectedArtist = null;
        selectedSong = null;
        selectedAlbum = null;
    }

    public boolean isLoggedIn() {
        return currentAccount != null;
    }

    public boolean isAdmin() {
        return isLoggedIn() && "admin".equalsIgnoreCase(currentAccount.getRole());
    }

    public boolean isArtist() {
        return isLoggedIn() && "artist".equalsIgnoreCase(currentAccount.getRole());
    }

    public boolean isUser() {
        return isLoggedIn() && "user".equalsIgnoreCase(currentAccount.getRole());
    }

    public UserFileManager getUserFileManager() {
        return userFileManager;
    }

    public ArtistFileManager getArtistFileManager() {
        return artistFileManager;
    }
}