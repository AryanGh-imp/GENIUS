package services;

import models.account.Account;
import models.account.Admin;
import models.account.Artist;
import models.account.User;
import services.file.ArtistFileManager;
import services.file.UserFileManager;

import java.util.List;

public class SessionManager {
    private static Account currentAccount;
    private static String selectedArtist;
    private static String selectedArtistEmail;
    private static String selectedSong;
    private static String selectedAlbum;
    private String selectedOriginalLyrics;

    private static final SessionManager INSTANCE = new SessionManager();
    private static final UserFileManager userFileManager = new UserFileManager();
    private static final ArtistFileManager artistFileManager = new ArtistFileManager();

    private SessionManager() {}

    public static SessionManager getInstance() {
        System.out.println("SessionManager instance accessed");
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
                        List<User> followers = artistFileManager.loadFollowers(artist, allUsers);
                        artist.getFollowers().clear();
                        artist.getFollowers().addAll(followers);
                    } catch (Exception e) {
                        System.err.println("Failed to load followers for artist '" + artist.getNickName() + "': " + e.getMessage());
                        throw new IllegalStateException("Failed to load followers", e);
                    }
                }
                case Admin admin -> {}
                default -> System.err.println("Unknown account type for: " + account.getNickName());
            }
            currentAccount = account;
            System.out.println("Current account set to: " + currentAccount.getNickName());
        } catch (Exception e) {
            System.err.println("Failed to set current account for: " + account.getNickName() + ", error: " + e.getMessage());
            throw new IllegalStateException("Failed to set current account", e);
        }
    }

    public static void validateSession() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String artistEmail = SessionManager.getInstance().getSelectedArtistEmail();
        if (artistName != null && (artistEmail == null || artistEmail.trim().isEmpty())) {
            artistEmail = artistFileManager.findEmailByNickName(artistName, "artist");
            if (artistEmail != null) {
                SessionManager.getInstance().setSelectedArtistEmail(artistEmail);
                System.out.println("Validated and set artist email: " + artistEmail);
            }
        }
    }

    public Account getCurrentAccount() {
        System.out.println("Getting current account: " + (currentAccount != null ? currentAccount.getNickName() : "null"));
        return currentAccount;
    }

    public String getCurrentUsername() {
        String username = currentAccount != null ? currentAccount.getNickName() : "Guest";
        System.out.println("Getting current username: " + username);
        return username;
    }

    public String getCurrentEmail() {
        String email = currentAccount != null ? currentAccount.getEmail() : null;
        System.out.println("Getting current email: " + email);
        return email;
    }

    public void setSelectedArtist(String artist) {
        selectedArtist = artist;
        System.out.println("Set selectedArtist to: " + selectedArtist);
    }

    public String getSelectedArtist() {
        System.out.println("Getting selectedArtist: " + selectedArtist);
        return selectedArtist;
    }

    public String getSelectedOriginalLyrics() {
        return selectedOriginalLyrics;
    }

    public void setSelectedOriginalLyrics(String selectedOriginalLyrics) {
        this.selectedOriginalLyrics = selectedOriginalLyrics;
    }

    public void setSelectedArtistEmail(String email) {
        selectedArtistEmail = email;
        System.out.println("Setting selectedArtistEmail to: " + selectedArtistEmail);
    }

    public String getSelectedArtistEmail() {
        System.out.println("Getting selectedArtistEmail: " + selectedArtistEmail);
        return selectedArtistEmail;
    }

    public void setSelectedSong(String song) {
        selectedSong = song;
        System.out.println("Set selectedSong to: " + selectedSong);
    }

    public String getSelectedSong() {
        System.out.println("Getting selectedSong: " + selectedSong);
        return selectedSong;
    }

    public void setSelectedAlbum(String album) {
        selectedAlbum = album;
        System.out.println("Set selectedAlbum to: " + selectedAlbum);
    }

    public String getSelectedAlbum() {
        System.out.println("Getting selectedAlbum: " + selectedAlbum);
        return selectedAlbum;
    }

    public void clearSession() {
        System.out.println("Clearing session...");
        currentAccount = null;
        selectedArtist = null;
        selectedArtistEmail = null;
        selectedSong = null;
        selectedAlbum = null;
        System.out.println("Session cleared.");
    }

    public boolean isLoggedIn() {
        boolean loggedIn = currentAccount != null;
        System.out.println("Is logged in: " + loggedIn);
        return loggedIn;
    }

    public boolean isAdmin() {
        boolean isAdmin = isLoggedIn() && "admin".equalsIgnoreCase(currentAccount.getRole());
        System.out.println("Is admin: " + isAdmin);
        return isAdmin;
    }

    public boolean isArtist() {
        boolean isArtist = isLoggedIn() && "artist".equalsIgnoreCase(currentAccount.getRole());
        System.out.println("Is artist: " + isArtist);
        return isArtist;
    }

    public boolean isUser() {
        boolean isUser = isLoggedIn() && "user".equalsIgnoreCase(currentAccount.getRole());
        System.out.println("Is user: " + isUser);
        return isUser;
    }

    public UserFileManager getUserFileManager() {
        return userFileManager;
    }

    public ArtistFileManager getArtistFileManager() {
        return artistFileManager;
    }
}