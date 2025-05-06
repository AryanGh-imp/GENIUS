package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.account.Artist;
import models.music.Album;
import models.music.Song;
import services.SessionManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AddSongPageController {
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

    private Artist artist;
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
        configureUI();
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

    private void configureUI() {
        setDefaultImage();
        configureRadioButtons();
        configureAlbumListView();
        setArtistInfo();
        addSongTypeListener();
        loadAlbums();
    }

    private void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }

    private void setDefaultImage() {
        checkComponent(imagePreview, "imagePreview");
        if (imagePreview != null) {
            try {
                imagePreview.setImage(new Image(Objects.requireNonNull(getClass().getResource(DEFAULT_IMAGE_PATH)).toExternalForm()));
            } catch (Exception e) {
                System.err.println("Failed to load default image: " + e.getMessage());
            }
        }
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

    private void setArtistInfo() {
        checkComponent(welcomeLabel, "welcomeLabel");
        checkComponent(artistField, "artistField");
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + artist.getNickName() + "!");
        }
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
        File albumsDir = new File(FileUtil.DATA_DIR + "artists/" + safeNickName + "/albums/");

        if (!albumsDir.exists() || !albumsDir.isDirectory()) {
            System.out.println("Albums directory not found or not a directory: " + albumsDir.getPath());
            return;
        }

        File[] albumFolders = albumsDir.listFiles(File::isDirectory);
        if (albumFolders == null || albumFolders.length == 0) {
            System.out.println("No album folders found in: " + albumsDir.getPath());
            return;
        }

        for (File albumFolder : albumFolders) {
            File albumFile = new File(albumFolder, "album.txt");
            if (albumFile.exists()) {
                List<String> albumData = FileUtil.readFile(albumFile.getPath());
                String albumTitle = parseAlbumTitle(albumData);
                if (albumTitle != null && !albumTitle.isEmpty()) {
                    albumListView.getItems().add(albumTitle);
                }
            }
        }
    }

    private String parseAlbumTitle(List<String> albumData) {
        return albumData.stream()
                .filter(line -> line.trim().startsWith("Album Title: "))
                .findFirst()
                .map(line -> line.trim().substring("Album Title: ".length()).trim())
                .orElse(null);
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
                imagePreview.setImage(new Image(file.toURI().toString()));
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
                Collections.singletonList(artistName),
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
        if (imagePreview != null) setDefaultImage();
        if (singleTrackRadio != null) singleTrackRadio.setSelected(true);
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