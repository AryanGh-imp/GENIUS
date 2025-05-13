package controllers.dashBoard.artist;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import models.account.Artist;
import services.SessionManager;
import utils.AlertUtil;

import java.io.File;
import java.util.Objects;

public abstract class BaseArtistController {

    protected Artist artist;
    protected ArtistMenuBarHandler menuBarHandler;
    protected ArtistDashboardController dashboardController;
    protected static final String DEFAULT_IMAGE_PATH = "/pics/Genius.com_logo_yellow.png";

    protected boolean validateSession(Button signOutButton) {
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
            if (menuBarHandler != null) menuBarHandler.signOut();
            return false;
        }
    }

    protected void setArtistInfo(Label welcomeLabel) {
        checkComponent(welcomeLabel, "welcomeLabel");
        if (welcomeLabel != null && artist != null) {
            welcomeLabel.setText("Welcome, " + artist.getNickName() + "!");
        }
    }

    protected void loadDefaultImage(ImageView imageView) {
        checkComponent(imageView, "imageView");
        if (imageView != null) {
            try {
                imageView.setImage(new Image(Objects.requireNonNull(getClass().getResource(DEFAULT_IMAGE_PATH)).toExternalForm()));
            } catch (Exception e) {
                System.err.println("Failed to load default image: " + e.getMessage());
            }
        }
    }

    protected void updateImageView(ImageView imageView, String albumArtPath) {
        checkComponent(imageView, "imageView");
        if (imageView == null) return;

        try {
            if (albumArtPath != null && !albumArtPath.isEmpty()) {
                if (albumArtPath.startsWith("file:/")) {
                    System.out.println("Loading image from URL: " + albumArtPath);
                    imageView.setImage(new Image(albumArtPath));
                } else {
                    File albumArtFile = new File(albumArtPath);
                    System.out.println("Checking image file: " + albumArtFile.getAbsolutePath() + " - Exists: " + albumArtFile.exists());
                    if (albumArtFile.exists()) {
                        imageView.setImage(new Image(albumArtFile.toURI().toString()));
                    } else {
                        System.out.println("Image file does not exist, loading default image.");
                        loadDefaultImage(imageView);
                    }
                }
            } else {
                System.out.println("Album art path is null or empty, loading default image.");
                loadDefaultImage(imageView);
            }
        } catch (Exception e) {
            System.err.println("Error loading image from " + albumArtPath + ": " + e.getMessage());
            e.printStackTrace();
            loadDefaultImage(imageView);
        }
    }

    protected void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }

    // Navigation methods
    protected void goToProfile() { menuBarHandler.goToProfile(); }
    protected void goToAddSong() { menuBarHandler.goToAddSong(); }
    protected void goToDeleteSong() { menuBarHandler.goToDeleteSong(); }
    protected void goToEditSong() { menuBarHandler.goToEditSong(); }
    protected void goToCreateAlbum() { menuBarHandler.goToCreateAlbum(); }
    protected void goToDeleteAlbum() { menuBarHandler.goToDeleteAlbum(); }
    protected void goToEditAlbum() { menuBarHandler.goToEditAlbum(); }
    protected void goToPendingRequests() { menuBarHandler.goToPendingRequests(); }
    protected void goToApprovedRequests() { menuBarHandler.goToApprovedRequests(); }
    protected void goToRejectedRequests() { menuBarHandler.goToRejectedRequests(); }
    protected void signOut() { menuBarHandler.signOut(); }
}