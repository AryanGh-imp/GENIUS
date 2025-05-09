package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.util.List;

public class DeleteAlbumController extends BaseArtistController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> albumListView;
    @FXML private Button signOutButton;
    @FXML private Label titleLabel;
    @FXML private Label releaseDateLabel;
    @FXML private ImageView albumArtImageView;

    private final SongFileManager songFileManager = new SongFileManager();

    @FXML
    public void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        loadAlbums();
        addAlbumSelectionListener();
        updateImageView(albumArtImageView, null); // Set default image
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
        String albumDir = songFileManager.getAlbumDir(artist.getNickName(), selectedAlbum);
        File albumFile = new File(albumDir + "album.txt");
        if (albumFile.exists()) {
            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            String releaseDate = "N/A";
            String albumArtPath = null;

            for (String line : albumData) {
                if (line.startsWith("Release Date: ")) {
                    releaseDate = line.substring("Release Date: ".length());
                } else if (line.startsWith("AlbumArtPath: ")) {
                    albumArtPath = line.substring("AlbumArtPath: ".length());
                }
            }

            checkComponent(titleLabel, "titleLabel");
            checkComponent(releaseDateLabel, "releaseDateLabel");
            if (titleLabel != null) titleLabel.setText("Title: " + selectedAlbum);
            if (releaseDateLabel != null) releaseDateLabel.setText("Release Date: " + releaseDate);
            updateImageView(albumArtImageView, albumArtPath);
        } else {
            clearMetadata();
        }
    }

    private void clearMetadata() {
        checkComponent(titleLabel, "titleLabel");
        checkComponent(releaseDateLabel, "releaseDateLabel");
        if (titleLabel != null) titleLabel.setText("Title: ");
        if (releaseDateLabel != null) releaseDateLabel.setText("Release Date: N/A");
        updateImageView(albumArtImageView, null);
    }

    @FXML
    public void deleteAlbum() {
        checkComponent(albumListView, "albumListView");
        String selectedAlbum = albumListView != null ? albumListView.getSelectionModel().getSelectedItem() : null;
        if (selectedAlbum == null) {
            AlertUtil.showError("No album selected for deletion.");
            return;
        }

        songFileManager.deleteAlbum(artist.getNickName(), selectedAlbum);
        loadAlbums();
        clearMetadata();
        AlertUtil.showSuccess("Deleted album: " + selectedAlbum);
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