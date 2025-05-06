package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import services.SessionManager;
import services.file.SongFileManager;
import utils.AlertUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

public class CreateAlbumController {

    @FXML private Label welcomeLabel;
    @FXML private Button signOutButton;
    @FXML private TextField albumTitleField;
    @FXML private Button chooseImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Button submitButton;

    private String currentArtistNickName;
    private final SongFileManager songFileManager = new SongFileManager();
    private File selectedImageFile;
    private ArtistMenuBarHandler menuBarHandler;
    private static final String DEFAULT_IMAGE_PATH = "/pics/Genius.com_logo_yellow.png";

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
            this.currentArtistNickName = SessionManager.getInstance().getCurrentAccount().getNickName();
            this.menuBarHandler = new ArtistMenuBarHandler(signOutButton);
            return true;
        } catch (IllegalStateException e) {
            AlertUtil.showError(e.getMessage());
            menuBarHandler.signOut();
            return false;
        }
    }

    private void initializeUI() {
        setArtistInfo();
        updateImagePreview(new Image(Objects.requireNonNull(getClass().getResource(DEFAULT_IMAGE_PATH)).toExternalForm()));
    }

    private void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }

    private void setArtistInfo() {
        checkComponent(welcomeLabel, "welcomeLabel");
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + currentArtistNickName + "!");
        }
    }

    private void updateImagePreview(Image image) {
        checkComponent(imagePreview, "imagePreview");
        if (imagePreview != null) {
            try {
                imagePreview.setImage(image);
            } catch (Exception e) {
                System.err.println("Failed to update image preview: " + e.getMessage());
            }
        }
    }

    @FXML
    private void chooseImage() {
        checkComponent(chooseImageButton, "chooseImageButton");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Album Art");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            updateImagePreview(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void createAlbum() {
        checkComponent(albumTitleField, "albumTitleField");
        String albumTitle = albumTitleField != null ? albumTitleField.getText().trim() : "";
        if (albumTitle.isEmpty()) {
            AlertUtil.showError("Album title is required.");
            return;
        }

        try {
            String albumArtPath = saveAlbumArt(albumTitle);
            songFileManager.saveAlbum(currentArtistNickName, albumTitle, LocalDate.now().toString(), null, albumArtPath);
            resetForm();
            AlertUtil.showSuccess("Album '" + albumTitle + "' created successfully!");
        } catch (Exception e) {
            AlertUtil.showError("Error creating album: " + e.getMessage());
        }
    }

    private String saveAlbumArt(String albumTitle) throws IOException {
        checkComponent(selectedImageFile, "selectedImageFile");
        if (selectedImageFile == null) return null;

        return songFileManager.saveAlbumArt(currentArtistNickName, albumTitle, selectedImageFile);
    }

    private void resetForm() {
        clearFields();
        resetImage();
    }

    private void clearFields() {
        checkComponent(albumTitleField, "albumTitleField");
        if (albumTitleField != null) albumTitleField.clear();
    }

    private void resetImage() {
        updateImagePreview(new Image(Objects.requireNonNull(getClass().getResource(DEFAULT_IMAGE_PATH)).toExternalForm()));
        selectedImageFile = null;
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