package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import models.account.Artist;
import models.music.Album;
import models.music.Song;
import services.SessionManager;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import utils.AlertUtil;

public class ArtistDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label artistNicknameLabel;
    @FXML private Label totalSongsLabel;
    @FXML private Label totalAlbumsLabel;
    @FXML private Label totalLikesLabel;
    @FXML private Label totalViewsLabel;
    @FXML private Button signOutButton;

    private Artist artist;
    private ArtistMenuBarHandler menuBarHandler;
    private final SongFileManager songFileManager = new SongFileManager();
    private final ArtistFileManager artistFileManager = new ArtistFileManager();

    @FXML
    private void initialize() {
        initializeSessionAndUI();
    }

    private void initializeSessionAndUI() {
        if (!validateSession()) return;
        initializeUI();
    }

    private boolean validateSession() {
        try {
            if (!SessionManager.getInstance().isLoggedIn()) {
                throw new IllegalStateException("No user is logged in. Please sign in first.");
            }
            if (!SessionManager.getInstance().isArtist()) {
                throw new IllegalStateException("Only artists can access this page.");
            }
            this.artist = (Artist) SessionManager.getInstance().getCurrentAccount();
            this.menuBarHandler = new ArtistMenuBarHandler(signOutButton);
            return true;
        } catch (IllegalStateException e) {
            AlertUtil.showError(e.getMessage());
            menuBarHandler.signOut();
            return false;
        }
    }

    private void initializeUI() {
        artist.loadSongsAndAlbums(songFileManager, artistFileManager);
        setArtistInfo();
        displayStatistics();
    }

    private void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }

    private void setArtistInfo() {
        checkComponent(welcomeLabel, "welcomeLabel");
        checkComponent(artistNicknameLabel, "artistNicknameLabel");
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + artist.getNickName() + "!");
        }
        if (artistNicknameLabel != null) {
            artistNicknameLabel.setText(artist.getNickName());
        }
    }

    private void displayStatistics() {
        int totalSongs = calculateTotalSongs();
        int totalAlbums = artist.getAlbums().size();
        int[] likesAndViews = calculateLikesAndViews();

        updateStatisticsLabels(totalSongs, totalAlbums, likesAndViews);
    }

    private void updateStatisticsLabels(int totalSongs, int totalAlbums, int[] likesAndViews) {
        checkComponent(totalSongsLabel, "totalSongsLabel");
        checkComponent(totalAlbumsLabel, "totalAlbumsLabel");
        checkComponent(totalLikesLabel, "totalLikesLabel");
        checkComponent(totalViewsLabel, "totalViewsLabel");

        if (totalSongsLabel != null) totalSongsLabel.setText("Total Songs: " + totalSongs);
        if (totalAlbumsLabel != null) totalAlbumsLabel.setText("Total Albums: " + totalAlbums);
        if (totalLikesLabel != null) totalLikesLabel.setText("Total Likes: " + likesAndViews[0]);
        if (totalViewsLabel != null) totalViewsLabel.setText("Total Views: " + likesAndViews[1]);
    }

    private int calculateTotalSongs() {
        int totalSongs = artist.getSingles().size();
        for (Album album : artist.getAlbums()) {
            totalSongs += album.getSongs().size();
        }
        return totalSongs;
    }

    private int[] calculateLikesAndViews() {
        int totalLikes = 0;
        int totalViews = 0;

        for (Song song : artist.getSingles()) {
            totalLikes += song.getLikes();
            totalViews += song.getViews();
        }

        for (Album album : artist.getAlbums()) {
            for (Song song : album.getSongs()) {
                totalLikes += song.getLikes();
                totalViews += song.getViews();
            }
        }

        return new int[]{totalLikes, totalViews};
    }

    @FXML private void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML private void goToAddSong() { menuBarHandler.goToAddSong(); }
    @FXML private void goToDeleteSong() { menuBarHandler.goToDeleteSong(); }
    @FXML private void goToEditSong() { menuBarHandler.goToEditSong(); }
    @FXML private void goToCreateAlbum() { menuBarHandler.goToCreateAlbum(); }
    @FXML private void goToDeleteAlbum() { menuBarHandler.goToDeleteAlbum(); }
    @FXML private void goToEditAlbum() { menuBarHandler.goToEditAlbum(); }
    @FXML private void goToPendingRequests() { menuBarHandler.goToPendingRequests(); }
    @FXML private void goToApprovedRequests() { menuBarHandler.goToApprovedRequests(); }
    @FXML private void goToRejectedRequests() { menuBarHandler.goToRejectedRequests(); }
    @FXML private void signOut() { menuBarHandler.signOut(); }
}