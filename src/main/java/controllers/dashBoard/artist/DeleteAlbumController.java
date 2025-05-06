package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.SessionManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DeleteAlbumController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> albumListView;
    @FXML private Button signOutButton;
    @FXML private Label titleLabel;
    @FXML private Label releaseDateLabel;
    @FXML private ImageView albumArtImageView;

    private String currentArtistNickName;
    private final SongFileManager songFileManager = new SongFileManager();
    private final Map<String, String> albumToPathMap = new HashMap<>();
    private ArtistMenuBarHandler menuBarHandler;
    private static final String DEFAULT_ALBUM_ART_PATH = "/pics/Genius.com_logo_yellow.png";

    @FXML
    public void initialize() {
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
        loadAlbums();
        addAlbumSelectionListener();
        clearMetadata();
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

    private void loadAlbums() {
        checkComponent(albumListView, "albumListView");
        if (albumListView == null) return;

        albumListView.getItems().clear();
        albumToPathMap.clear();

        String safeNickName = FileUtil.sanitizeFileName(currentArtistNickName);
        File albumsDir = new File(FileUtil.DATA_DIR + "artists/" + safeNickName + "/albums/");

        if (!albumsDir.exists() || !albumsDir.isDirectory()) {
            return;
        }

        File[] albumFolders = albumsDir.listFiles(File::isDirectory);
        if (albumFolders == null) {
            return;
        }

        for (File albumFolder : albumFolders) {
            File albumFile = new File(albumFolder, "album.txt");
            if (!albumFile.exists()) {
                continue;
            }

            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            String albumTitle = parseAlbumTitle(albumData);
            if (albumTitle != null && !albumTitle.isEmpty()) {
                albumListView.getItems().add(albumTitle);
                albumToPathMap.put(albumTitle, albumFolder.getPath());
            }
        }
    }

    private String parseAlbumTitle(List<String> albumData) {
        return albumData.stream()
                .filter(line -> line.trim().startsWith("Album Title: "))
                .map(line -> line.trim().substring("Album Title: ".length()).trim())
                .findFirst()
                .orElse(null);
    }

    private void addAlbumSelectionListener() {
        checkComponent(albumListView, "albumListView");
        if (albumListView != null) {
            albumListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    displayAlbumMetadata(newValue);
                } else {
                    clearMetadata();
                }
            });
        }
    }

    private void displayAlbumMetadata(String selectedAlbum) {
        String albumPath = albumToPathMap.get(selectedAlbum);
        if (albumPath == null) {
            clearMetadata();
            return;
        }

        File albumFile = new File(albumPath, "album.txt");
        if (albumFile.exists()) {
            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            updateMetadata(albumData, selectedAlbum);
        } else {
            clearMetadata();
        }
    }

    private void updateMetadata(List<String> albumData, String albumTitle) {
        String releaseDate = "N/A";
        String albumArtPath = null;

        for (String line : albumData) {
            if (line.startsWith("Release Date: ")) {
                releaseDate = line.substring("Release Date: ".length());
            } else if (line.startsWith("AlbumArtPath: ")) {
                albumArtPath = line.substring("AlbumArtPath: ".length());
            }
        }

        updateLabels(albumTitle, releaseDate);
        updateImageView(albumArtPath);
    }

    private void updateLabels(String albumTitle, String releaseDate) {
        checkComponent(titleLabel, "titleLabel");
        checkComponent(releaseDateLabel, "releaseDateLabel");

        if (titleLabel != null) titleLabel.setText("Title: " + albumTitle);
        if (releaseDateLabel != null) releaseDateLabel.setText("Release Date: " + releaseDate);
    }

    private void updateImageView(String albumArtPath) {
        checkComponent(albumArtImageView, "albumArtImageView");
        if (albumArtImageView == null) return;

        try {
            if (albumArtPath != null && !albumArtPath.isEmpty()) {
                File albumArtFile = new File(albumArtPath);
                if (albumArtFile.exists()) {
                    albumArtImageView.setImage(new Image(albumArtFile.toURI().toString()));
                } else {
                    loadDefaultImage();
                }
            } else {
                loadDefaultImage();
            }
        } catch (Exception e) {
            loadDefaultImage();
        }
    }

    private void loadDefaultImage() {
        try {
            albumArtImageView.setImage(new Image(Objects.requireNonNull(getClass().getResource(DEFAULT_ALBUM_ART_PATH)).toExternalForm()));
        } catch (Exception e) {
            System.err.println("Failed to load default image: " + e.getMessage());
        }
    }

    private void clearMetadata() {
        updateLabels("", "N/A");
        loadDefaultImage();
    }

    @FXML
    public void deleteAlbum() {
        checkComponent(albumListView, "albumListView");
        String selectedAlbum = albumListView != null ? albumListView.getSelectionModel().getSelectedItem() : null;
        if (selectedAlbum == null) {
            AlertUtil.showError("No album selected for deletion.");
            return;
        }

        songFileManager.deleteAlbum(currentArtistNickName, selectedAlbum);
        loadAlbums();
        clearMetadata();
        AlertUtil.showSuccess("Deleted album: " + selectedAlbum);
    }

    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToAddSong() { menuBarHandler.goToAddSong(); }
    @FXML public void goToDeleteSong() { menuBarHandler.goToDeleteSong(); }
    @FXML public void goToEditSong() { menuBarHandler.goToEditSong(); }
    @FXML public void goToCreateAlbum() { menuBarHandler.goToCreateAlbum(); }
    @FXML public void goToDeleteAlbum() { menuBarHandler.goToDeleteAlbum(); }
    @FXML public void goToEditAlbum() { menuBarHandler.goToEditAlbum(); }
    @FXML public void goToPendingRequests() { menuBarHandler.goToPendingRequests(); }
    @FXML public void goToApprovedRequests() { menuBarHandler.goToApprovedRequests(); }
    @FXML public void goToRejectedRequests() { menuBarHandler.goToRejectedRequests(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}