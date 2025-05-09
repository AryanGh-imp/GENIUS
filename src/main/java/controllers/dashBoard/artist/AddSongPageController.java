package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.music.Album;
import models.music.Song;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class AddSongPageController extends BaseArtistController {

    @FXML private Label welcomeLabel;
    @FXML private Button signOutButton;
    @FXML private TextField artistField;
    @FXML private TextField titleField;
    @FXML private RadioButton singleTrackRadio;
    @FXML private ToggleGroup songTypeGroup;
    @FXML private ListView<String> albumListView;
    @FXML private TextArea lyricsArea;
    @FXML private Button chooseImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Button submitButton;

    private final SongFileManager songFileManager = new SongFileManager();
    private File selectedImageFile;

    @FXML
    private void initialize() {
        if (!validateSession(signOutButton)) return;
        configureUI();
    }

    private void configureUI() {
        setArtistInfo(welcomeLabel);
        updateImageView(imagePreview, null); // Set default image
        configureRadioButtons();
        configureAlbumListView();
        setArtistField();
        addSongTypeListener();
        loadAlbums();
    }

    private void configureRadioButtons() {
        checkComponent(singleTrackRadio, "singleTrackRadio");
        if (singleTrackRadio != null) {
            singleTrackRadio.setSelected(true);
        }
    }

    private void configureAlbumListView() {
        checkComponent(albumListView, "albumListView");
        if (albumListView != null) {
            albumListView.setDisable(true);
        }
    }

    private void setArtistField() {
        checkComponent(artistField, "artistField");
        if (artistField != null) {
            artistField.setText(artist.getNickName());
            artistField.setEditable(false);
        }
    }

    private void loadAlbums() {
        checkComponent(albumListView, "albumListView");
        if (albumListView == null) return;

        albumListView.getItems().clear();
        String safeNickName = FileUtil.sanitizeFileName(artist.getNickName());
        String albumsDir = FileUtil.DATA_DIR + "artists/" + safeNickName + "/albums/";
        File albumsDirFile = new File(albumsDir);

        if (!albumsDirFile.exists() || !albumsDirFile.isDirectory()) {
            AlertUtil.showWarning("Albums directory not found or not a directory: " + albumsDir);
            return;
        }

        File[] albumFolders = albumsDirFile.listFiles(File::isDirectory);
        if (albumFolders == null || albumFolders.length == 0) {
            AlertUtil.showWarning("No album folders found in: " + albumsDir);
            return;
        }

        for (File albumFolder : albumFolders) {
            File albumFile = new File(albumFolder, "album.txt");
            if (albumFile.exists()) {
                List<String> albumData = FileUtil.readFile(albumFile.getPath());
                String albumTitle = albumData.stream()
                        .filter(line -> line.trim().startsWith("Album Title: "))
                        .map(line -> line.trim().substring("Album Title: ".length()).trim())
                        .findFirst()
                        .orElse(null);
                if (albumTitle != null && !albumTitle.isEmpty()) {
                    albumListView.getItems().add(albumTitle);
                }
            }
        }
    }

    private void addSongTypeListener() {
        checkComponent(songTypeGroup, "songTypeGroup");
        checkComponent(singleTrackRadio, "singleTrackRadio");
        checkComponent(albumListView, "albumListView");
        if (songTypeGroup == null || singleTrackRadio == null || albumListView == null) return;

        songTypeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            albumListView.setDisable(newValue == singleTrackRadio);
            if (newValue == singleTrackRadio) {
                albumListView.getSelectionModel().clearSelection();
            }
        });
    }

    @FXML
    private void chooseImage() {
        checkComponent(chooseImageButton, "chooseImageButton");
        if (chooseImageButton == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Album Art");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            checkComponent(imagePreview, "imagePreview");
            if (imagePreview != null) {
                updateImageView(imagePreview, file.toURI().toString());
            }
        }
    }

    @FXML
    private void submitSong() {
        if (!validateInputs()) return;

        String artistName = artistField.getText().trim();
        String title = titleField.getText().trim();
        String selectedAlbum = albumListView.getSelectionModel().getSelectedItem();
        String lyrics = lyricsArea.getText().trim();
        boolean isSingleTrack = singleTrackRadio.isSelected();

        try {
            Song song = new Song(title, lyrics, LocalDate.now().toString());
            song.addArtist(artist);

            String albumArtPath = selectedImageFile != null ? saveAlbumArt(artistName, isSingleTrack ? title : selectedAlbum) : null;
            if (albumArtPath != null) {
                song.setAlbumArtPath(albumArtPath);
            }

            Album album = manageSongPlacement(isSingleTrack, selectedAlbum, song);
            saveSongAndUpdateUI(artistName, title, selectedAlbum, lyrics, isSingleTrack, album);
        } catch (Exception e) {
            AlertUtil.showError("Error adding song: " + e.getMessage());
        }
    }

    private boolean validateInputs() {
        checkComponent(titleField, "titleField");
        checkComponent(lyricsArea, "lyricsArea");
        checkComponent(albumListView, "albumListView");
        checkComponent(singleTrackRadio, "singleTrackRadio");

        String title = titleField != null ? titleField.getText().trim() : "";
        String lyrics = lyricsArea != null ? lyricsArea.getText().trim() : "";
        String selectedAlbum = albumListView != null ? albumListView.getSelectionModel().getSelectedItem() : null;
        boolean isSingleTrack = singleTrackRadio != null && singleTrackRadio.isSelected();

        if (title.isEmpty() || lyrics.isEmpty()) {
            AlertUtil.showError("Title and lyrics are required.");
            return false;
        }
        if (!isSingleTrack && selectedAlbum == null) {
            AlertUtil.showError("Please select an album for the song.");
            return false;
        }
        return true;
    }

    private String saveAlbumArt(String artistName, String folderName) throws IOException {
        checkComponent(selectedImageFile, "selectedImageFile");
        if (selectedImageFile == null) return null;

        return songFileManager.saveAlbumArt(artistName, folderName, selectedImageFile);
    }

    private Album manageSongPlacement(boolean isSingleTrack, String selectedAlbum, Song song) {
        if (isSingleTrack) {
            artist.addSingle(song);
            return null;
        } else {
            return artist.getAlbums().stream()
                    .filter(a -> a.getTitle().equals(selectedAlbum))
                    .findFirst()
                    .orElseGet(() -> {
                        Album newAlbum = new Album(selectedAlbum, LocalDate.now().toString(), artist);
                        artist.addAlbum(newAlbum);
                        return newAlbum;
                    })
                    .addSong(song);
        }
    }

    private void saveSongAndUpdateUI(String artistName, String title, String selectedAlbum, String lyrics, boolean isSingleTrack, Album album) throws IOException {
        songFileManager.saveSong(
                List.of(artistName),
                title,
                isSingleTrack ? null : selectedAlbum,
                lyrics,
                LocalDate.now().toString(),
                0, 0,
                album != null ? album.getAlbumArtPath() : (isSingleTrack ? saveAlbumArt(artistName, title) : null));
        resetForm();
        loadAlbums();
        AlertUtil.showSuccess("Song '" + title + "' added successfully!");
    }

    private void resetForm() {
        checkComponent(titleField, "titleField");
        checkComponent(albumListView, "albumListView");
        checkComponent(lyricsArea, "lyricsArea");
        checkComponent(singleTrackRadio, "singleTrackRadio");
        checkComponent(imagePreview, "imagePreview");

        if (titleField != null) titleField.clear();
        if (albumListView != null) albumListView.getSelectionModel().clearSelection();
        if (lyricsArea != null) lyricsArea.clear();
        if (imagePreview != null) updateImageView(imagePreview, null);
        if (singleTrackRadio != null) singleTrackRadio.setSelected(true);
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