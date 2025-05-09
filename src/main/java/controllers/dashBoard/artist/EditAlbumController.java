package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.music.Album;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class EditAlbumController extends BaseArtistController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> albumListView;
    @FXML private Button signOutButton;
    @FXML private TextField titleField;
    @FXML private ImageView albumArtImageView;
    @FXML private Button chooseImageButton;
    @FXML private Button submitButton;

    private final SongFileManager songFileManager = new SongFileManager();
    private File selectedImageFile;

    @FXML
    public void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        loadAlbums();
        addAlbumSelectionListener();
        loadDefaultImage(albumArtImageView);
    }

    private void loadAlbums() {
        checkComponent(albumListView, "albumListView");
        if (albumListView == null) return;

        albumListView.getItems().clear();
        String safeNickName = FileUtil.sanitizeFileName(artist.getNickName());
        String albumsDir = FileUtil.DATA_DIR + "artists/" + safeNickName + "/albums/";
        File albumsDirFile = new File(albumsDir);

        if (!albumsDirFile.exists() || !albumsDirFile.isDirectory()) {
            System.out.println("Albums directory not found or not a directory: " + albumsDir);
            return;
        }

        File[] albumFolders = albumsDirFile.listFiles(File::isDirectory);
        if (albumFolders == null || albumFolders.length == 0) {
            System.out.println("No album folders found in: " + albumsDir);
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
            String albumArtPath = null;

            for (String line : albumData) {
                if (line.startsWith("AlbumArtPath: ")) {
                    albumArtPath = line.substring("AlbumArtPath: ".length());
                }
            }

            checkComponent(titleField, "titleField");
            if (titleField != null) titleField.setText(selectedAlbum);
            updateImageView(albumArtImageView, albumArtPath);
        } else {
            clearMetadata();
        }
    }

    private void clearMetadata() {
        checkComponent(titleField, "titleField");
        if (titleField != null) titleField.clear();
        loadDefaultImage(albumArtImageView);
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
            checkComponent(albumArtImageView, "albumArtImageView");
            if (albumArtImageView != null) {
                albumArtImageView.setImage(new Image(file.toURI().toString()));
            }
        }
    }

    @FXML
    private void submitEdit() {
        checkComponent(albumListView, "albumListView");
        checkComponent(titleField, "titleField");
        String selectedAlbum = albumListView != null ? albumListView.getSelectionModel().getSelectedItem() : null;
        String newTitle = titleField != null ? titleField.getText().trim() : "";
        if (selectedAlbum == null || newTitle.isEmpty()) {
            AlertUtil.showError("Please select an album and provide a new title.");
            return;
        }

        if (selectedAlbum.equals(newTitle) && selectedImageFile == null) {
            AlertUtil.showError("No changes detected. Please provide a new title or image.");
            return;
        }

        try {
            // Load the current album data
            String oldAlbumDir = songFileManager.getAlbumDir(artist.getNickName(), selectedAlbum);
            File albumFile = new File(oldAlbumDir + "album.txt");
            if (!albumFile.exists()) {
                throw new IllegalStateException("Album file not found: " + oldAlbumDir);
            }

            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            String releaseDate = albumData.stream()
                    .filter(line -> line.startsWith("Release Date: "))
                    .map(line -> line.substring("Release Date: ".length()))
                    .findFirst()
                    .orElse("Not set");
            String songs = albumData.stream()
                    .filter(line -> line.startsWith("Songs: "))
                    .map(line -> line.substring("Songs: ".length()))
                    .findFirst()
                    .orElse("");
            List<String> songTitles = songs.isEmpty() ? Collections.emptyList() : List.of(songs.split(","));

            // Update the album in the model
            Album albumToUpdate = artist.getAlbums().stream()
                    .filter(a -> a.getTitle().equals(selectedAlbum))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Album not found in artist: " + selectedAlbum));

            // If the title is changed, update the directory and files
            if (!selectedAlbum.equals(newTitle)) {
                String newAlbumDir = songFileManager.getAlbumDir(artist.getNickName(), newTitle);
                FileUtil.renameDirectory(new File(oldAlbumDir), new File(newAlbumDir));

                // Update the album title in the model
                albumToUpdate.setTitle(newTitle);
            }

            // Update the album art if a new image is selected
            String albumArtPath = selectedImageFile != null ? songFileManager.saveAlbumArt(artist.getNickName(), newTitle, selectedImageFile) : null;
            if (albumArtPath != null) {
                albumToUpdate.setAlbumArtPath(albumArtPath);
            }

            // Save the updated album data
            songFileManager.saveAlbum(artist.getNickName(), newTitle, releaseDate, songTitles, albumToUpdate.getAlbumArtPath());

            // Update songs to reflect the new album title and preserve likes/plays
            for (String songTitle : songTitles) {
                if (!songTitle.isEmpty()) {
                    String songPath = songFileManager.getSongDir(artist.getNickName(), songTitle, newTitle) + FileUtil.sanitizeFileName(songTitle) + ".txt";
                    File songFile = new File(songPath);
                    if (songFile.exists()) {
                        List<String> songData = FileUtil.readFile(songPath);
                        int likes = songData.stream()
                                .filter(line -> line.startsWith("Likes: "))
                                .map(line -> Integer.parseInt(line.substring("Likes: ".length())))
                                .findFirst()
                                .orElse(0);
                        int plays = songData.stream()
                                .filter(line -> line.startsWith("Plays: "))
                                .map(line -> Integer.parseInt(line.substring("Plays: ".length())))
                                .findFirst()
                                .orElse(0);
                        String lyrics = songFileManager.loadLyrics(songPath);
                        songFileManager.saveSong(
                                List.of(artist.getNickName()),
                                songTitle,
                                newTitle,
                                lyrics,
                                releaseDate,
                                likes,
                                plays,
                                albumToUpdate.getAlbumArtPath());
                    }
                }
            }

            loadAlbums();
            clearMetadata();
            AlertUtil.showSuccess("Album updated successfully to '" + newTitle + "'!");
        } catch (Exception e) {
            AlertUtil.showError("Error updating album: " + e.getMessage());
        }
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