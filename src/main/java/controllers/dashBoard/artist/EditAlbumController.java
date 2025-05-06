package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.account.Artist;
import services.SessionManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EditAlbumController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> albumListView;
    @FXML private Button signOutButton;
    @FXML private TextField titleField;
    @FXML private ImageView albumArtImageView;
    @FXML private Button chooseImageButton;
    @FXML private Button submitButton;

    private Artist artist;
    private final SongFileManager songFileManager = new SongFileManager();
    private final Map<String, String> albumToPathMap = new HashMap<>();
    private File selectedImageFile;
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
        setArtistInfo();
        loadAlbums();
        addAlbumSelectionListener();
        updateImageView(null); // Set default image
    }

    private void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }

    private void setArtistInfo() {
        checkComponent(welcomeLabel, "welcomeLabel");
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + artist.getNickName() + "!");
        }
    }

    private void loadAlbums() {
        checkComponent(albumListView, "albumListView");
        if (albumListView == null) return;

        albumListView.getItems().clear();
        albumToPathMap.clear();

        String safeNickName = FileUtil.sanitizeFileName(artist.getNickName());
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
        String albumArtPath = null;

        for (String line : albumData) {
            if (line.startsWith("AlbumArtPath: ")) {
                albumArtPath = line.substring("AlbumArtPath: ".length());
            }
        }

        updateFields(albumTitle);
        updateImageView(albumArtPath);
    }

    private void updateFields(String albumTitle) {
        checkComponent(titleField, "titleField");
        if (titleField != null) titleField.setText(albumTitle);
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
        updateFields("");
        loadDefaultImage();
        selectedImageFile = null;
    }

    @FXML
    public void chooseImage() {
        checkComponent(chooseImageButton, "chooseImageButton");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Album Art");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            updateImageView(file.toURI().toString());
        }
    }

    @FXML
    public void editAlbum() {
        checkComponent(albumListView, "albumListView");
        String selectedAlbum = albumListView != null ? albumListView.getSelectionModel().getSelectedItem() : null;
        if (selectedAlbum == null) {
            AlertUtil.showError("No album selected for editing.");
            return;
        }

        checkComponent(titleField, "titleField");
        String newTitle = titleField != null ? titleField.getText().trim() : "";
        if (newTitle.isEmpty()) {
            AlertUtil.showError("Title is required.");
            return;
        }

        try {
            String albumPath = albumToPathMap.get(selectedAlbum);
            if (albumPath == null) {
                AlertUtil.showError("Album path not found.");
                return;
            }

            File albumFile = new File(albumPath, "album.txt");
            if (!albumFile.exists()) {
                AlertUtil.showError("album.txt not found for the selected album.");
                return;
            }

            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            String songs = "";
            String releaseDate = "N/A";
            String oldAlbumArtPath = null;

            for (String line : albumData) {
                if (line.startsWith("Songs: ")) {
                    songs = line.substring("Songs: ".length());
                } else if (line.startsWith("Release Date: ")) {
                    releaseDate = line.substring("Release Date: ".length());
                } else if (line.startsWith("AlbumArtPath: ")) {
                    oldAlbumArtPath = line.substring("AlbumArtPath: ".length());
                }
            }

            String newAlbumArtPath = selectedImageFile != null
                    ? songFileManager.saveAlbumArt(artist.getNickName(), newTitle, selectedImageFile)
                    : oldAlbumArtPath;

            updateAlbumFile(albumFile, newTitle, releaseDate, songs, newAlbumArtPath);

            if (!selectedAlbum.equals(newTitle)) {
                renameAlbumFolder(selectedAlbum, newTitle, albumPath, songs);
            }

            loadAlbums();
            clearMetadata();
            AlertUtil.showSuccess("Album '" + newTitle + "' updated successfully!");
        } catch (IOException e) {
            AlertUtil.showError("Error updating album: " + e.getMessage());
        }
    }

    private void updateAlbumFile(File albumFile, String newTitle, String releaseDate, String songs, String albumArtPath) throws IOException {
        List<String> albumData = FileUtil.readFile(albumFile.getPath());
        StringBuilder updatedData = new StringBuilder();

        for (String line : albumData) {
            if (line.startsWith("Album Title: ")) {
                updatedData.append("Album Title: ").append(newTitle).append("\n");
            } else if (line.startsWith("Release Date: ")) {
                updatedData.append("Release Date: ").append(releaseDate).append("\n");
            } else if (line.startsWith("Songs: ")) {
                updatedData.append("Songs: ").append(songs).append("\n");
            } else if (line.startsWith("AlbumArtPath: ")) {
                updatedData.append("AlbumArtPath: ").append(albumArtPath).append("\n");
            } else {
                updatedData.append(line).append("\n");
            }
        }

        FileUtil.writeFile(albumFile.getPath(), Collections.singletonList(updatedData.toString()));
    }

    private void renameAlbumFolder(String selectedAlbum, String newTitle, String albumPath, String songs) {
        File oldFolder = new File(albumPath);
        String newFolderPath = oldFolder.getParent() + "/" + newTitle;
        File newFolder = new File(newFolderPath);

        if (!oldFolder.renameTo(newFolder)) {
            AlertUtil.showError("Failed to rename album folder.");
            return;
        }

        albumToPathMap.remove(selectedAlbum);
        albumToPathMap.put(newTitle, newFolder.getPath());

        if (!songs.isEmpty()) {
            String[] songTitles = songs.split(",");
            for (String songTitle : songTitles) {
                if (songTitle.trim().isEmpty()) continue;
                File songFolder = new File(newFolder, songTitle.trim());
                if (songFolder.exists()) {
                    File songFile = new File(songFolder, songTitle.trim() + ".txt");
                    if (songFile.exists()) {
                        List<String> songData = FileUtil.readFile(songFile.getPath());
                        FileUtil.writeFile(songFile.getPath(), songData);
                    }
                }
            }
        }
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