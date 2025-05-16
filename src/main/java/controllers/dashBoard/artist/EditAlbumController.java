package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.music.Album;
import models.music.Song;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private final ArtistFileManager artistFileManager = new ArtistFileManager();
    private File selectedImageFile;

    @FXML
    public void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        songFileManager.loadSongsAndAlbumsForArtist(artist, artistFileManager);
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
        System.out.println("Checking album file: " + albumFile.getAbsolutePath() + " - Exists: " + albumFile.exists());
        if (albumFile.exists()) {
            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            String albumArtPath = null;

            for (String line : albumData) {
                if (line.startsWith("AlbumArtPath: ")) {
                    albumArtPath = line.substring("AlbumArtPath: ".length()).trim();
                    System.out.println("Loaded AlbumArtPath: " + albumArtPath);
                }
            }

            checkComponent(titleField, "titleField");
            if (titleField != null) titleField.setText(selectedAlbum);
            updateImageView(albumArtImageView, albumArtPath, albumDir); // Pass the album directory
        } else {
            clearMetadata();
        }
    }

    private void clearMetadata() {
        checkComponent(titleField, "titleField");
        if (titleField != null) titleField.clear();
        updateImageView(albumArtImageView, null);
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
                updateImageView(albumArtImageView, file.toURI().toString());
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
            System.out.println("Artist albums before update: " + artist.getAlbums());
            String oldAlbumDir = songFileManager.getAlbumDir(artist.getNickName(), selectedAlbum);
            File albumFile = new File(oldAlbumDir + "album.txt");
            if (!albumFile.exists()) {
                throw new IllegalStateException("Album file not found: " + oldAlbumDir);
            }

            if (artist.getAlbums().stream().noneMatch(a -> a.getTitle().equals(selectedAlbum))) {
                System.out.println("Album not found in artist, reloading: " + selectedAlbum);
                songFileManager.loadSongsAndAlbumsForArtist(artist, artistFileManager);
            }

            Album albumToUpdate = artist.getAlbums().stream()
                    .filter(a -> a.getTitle().equals(selectedAlbum))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Album not found in artist: " + selectedAlbum));

            // Extract the path of the old image
            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            String oldAlbumArtPath = null;
            for (String line : albumData) {
                if (line.startsWith("AlbumArtPath: ")) {
                    oldAlbumArtPath = line.substring("AlbumArtPath: ".length()).trim();
                }
            }

            String newAlbumArtPath = oldAlbumArtPath;

            // Rename the directory if the album title has changed
            if (!selectedAlbum.equals(newTitle)) {
                String newAlbumDir = songFileManager.getAlbumDir(artist.getNickName(), newTitle);
                File oldDir = new File(oldAlbumDir);
                File newDir = new File(newAlbumDir);

                // If the new directory exists, delete it
                if (newDir.exists()) {
                    FileUtil.deleteDirectory(newDir);
                }

                // Rename the directory
                FileUtil.renameDirectory(oldDir, newDir);

                // Update image path if it exists
                if (oldAlbumArtPath != null && !oldAlbumArtPath.trim().isEmpty()) {
                    File oldArtFile = new File(oldAlbumDir, oldAlbumArtPath);
                    System.out.println("Checking old album art file: " + oldArtFile.getAbsolutePath() + " - Exists: " + oldArtFile.exists());
                    if (oldArtFile.exists()) {
                        String artFileName = oldArtFile.getName();
                        File newArtFile = new File(newDir, artFileName);
                        FileUtil.ensureDataDirectoryExists(newDir.getPath());
                        Files.copy(oldArtFile.toPath(), newArtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        newAlbumArtPath = "data/artists/" + FileUtil.sanitizeFileName(artist.getNickName()) + "/albums/" + FileUtil.sanitizeFileName(newTitle) + "/" + artFileName;
                        System.out.println("Updated AlbumArtPath after rename: " + newAlbumArtPath);
                    } else {
                        // Check if the file exists in the new directory
                        File newArtCheck = new File(newDir, oldArtFile.getName());
                        if (newArtCheck.exists()) {
                            newAlbumArtPath = "data/artists/" + FileUtil.sanitizeFileName(artist.getNickName()) + "/albums/" + FileUtil.sanitizeFileName(newTitle) + "/" + newArtCheck.getName();
                            System.out.println("Found art file in new directory: " + newAlbumArtPath);
                        } else {
                            System.out.println("Old album art file not found: " + oldArtFile.getAbsolutePath());
                            newAlbumArtPath = null;
                        }
                    }
                }

                albumToUpdate.setTitle(newTitle);
            }

            // If a new image is selected, save it
            if (selectedImageFile != null) {
                newAlbumArtPath = songFileManager.saveAlbumArt(artist.getNickName(), newTitle, selectedImageFile);
                System.out.println("Updated AlbumArtPath after new image: " + newAlbumArtPath);
            } else if (newAlbumArtPath != null && new File(newAlbumArtPath.replace("data/data", "data")).exists()) {
                System.out.println("Reusing existing AlbumArtPath: " + newAlbumArtPath);
            } else {
                newAlbumArtPath = null; // Reset if the file doesn't exist
            }

            // Update image path in album
            albumToUpdate.setAlbumArtPath(newAlbumArtPath);

            // Save album with new metadata
            songFileManager.saveAlbum(
                    artist.getNickName(),
                    newTitle,
                    albumToUpdate.getReleaseDate(),
                    albumToUpdate.getSongs().stream().map(Song::getTitle).toList(),
                    albumToUpdate.getAlbumArtPath()
            );

            // Update album tracks
            for (String songTitle : albumToUpdate.getSongs().stream().map(Song::getTitle).toList()) {
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
                                albumToUpdate.getReleaseDate(),
                                likes,
                                plays,
                                albumToUpdate.getAlbumArtPath()
                        );
                    }
                }
            }

            // Reload songs and albums
            songFileManager.loadSongsAndAlbumsForArtist(artist, artistFileManager);
            loadAlbums();
            clearMetadata();
            AlertUtil.showSuccess("Album updated successfully to '" + newTitle + "'!");
        } catch (Exception e) {
            AlertUtil.showError("Error updating album: " + e.getMessage());
            e.printStackTrace();
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