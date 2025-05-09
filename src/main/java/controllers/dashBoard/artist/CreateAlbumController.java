package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import services.file.SongFileManager;
import utils.AlertUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

public class CreateAlbumController extends BaseArtistController {

    @FXML private Label welcomeLabel;
    @FXML private Button signOutButton;
    @FXML private TextField albumTitleField;
    @FXML private Button chooseImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Button submitButton;

    private String currentArtistNickName;
    private final SongFileManager songFileManager = new SongFileManager();
    private File selectedImageFile;

    @FXML
    private void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        currentArtistNickName = artist.getNickName();
        loadDefaultImage(imagePreview);
    }

    public void loadDefaultImage(ImageView imageView) {
        try {
            imageView.setImage(new Image(Objects.requireNonNull(getClass().getResource(DEFAULT_IMAGE_PATH)).toExternalForm()));
        } catch (Exception e) {
            System.err.println("Failed to load default image: " + e.getMessage());
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
            checkComponent(imagePreview, "imagePreview");
            if (imagePreview != null) {
                imagePreview.setImage(new Image(file.toURI().toString()));
            }
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
            String albumArtPath = selectedImageFile != null ? saveAlbumArt(albumTitle) : null;
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
        checkComponent(imagePreview, "imagePreview");
        if (imagePreview != null) loadDefaultImage(imagePreview);
        selectedImageFile = null;
    }

    @FXML public void goToProfile() { super.goToProfile(); }
    @FXML public void goToAddSong() { super.goToAddSong(); }
    @FXML public void goToDeleteSong() { super.goToDeleteSong(); }
    @FXML public void goToEditSong() { super.goToEditSong(); }
    @FXML public void goToCreateAlbum() { super.goToCreateAlbum(); }
    @FXML public void goToDeleteAlbum() { super.goToDeleteAlbum(); }
    @FXML public void goToEditAlbum() { super.goToEditAlbum(); }
    @FXML public void goToPendingRequests() { super.goToPendingRequests(); }
    @FXML public void goToApprovedRequests() { super.goToApprovedRequests(); }
    @FXML public void goToRejectedRequests() { super.goToRejectedRequests(); }
    @FXML public void signOut() { super.signOut(); }
}