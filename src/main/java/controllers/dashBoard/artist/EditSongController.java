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

public class EditSongController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> songListView;
    @FXML private Button signOutButton;
    @FXML private TextField titleField;
    @FXML private TextArea lyricsArea;
    @FXML private ImageView imagePreview;
    @FXML private Button chooseImageButton;
    @FXML private Button submitButton;

    private Artist artist;
    private final SongFileManager songFileManager = new SongFileManager();
    private final Map<String, String> songToPathMap = new HashMap<>();
    private File selectedImageFile;
    private ArtistMenuBarHandler menuBarHandler;
    private static final String DEFAULT_IMAGE_PATH = "/pics/Genius.com_logo_yellow.png";

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
            signOut();
            return false;
        }
    }

    private void initializeUI() {
        setArtistInfo();
        loadSongs();
        addSongSelectionListener();
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

    private void loadSongs() {
        checkComponent(songListView, "songListView");
        if (songListView == null) return;

        songListView.getItems().clear();
        songToPathMap.clear();

        String safeNickName = FileUtil.sanitizeFileName(artist.getNickName());
        String artistDir = FileUtil.DATA_DIR + "artists/" + safeNickName + "/";
        File singlesDir = new File(artistDir + "singles/");
        File albumsDir = new File(artistDir + "albums/");

        // Load singles
        if (singlesDir.exists() && singlesDir.isDirectory()) {
            File[] songFolders = singlesDir.listFiles(File::isDirectory);
            if (songFolders != null) {
                for (File songFolder : songFolders) {
                    File songFile = new File(songFolder, songFolder.getName() + ".txt");
                    if (songFile.exists()) {
                        List<String> songData = FileUtil.readFile(songFile.getPath());
                        String songTitle = parseSongTitle(songData);
                        if (songTitle != null && !songTitle.isEmpty()) {
                            songListView.getItems().add(songTitle + " (Single)");
                            songToPathMap.put(songTitle + " (Single)", songFile.getPath());
                        }
                    }
                }
            }
        }

        // Load songs from albums
        if (albumsDir.exists() && albumsDir.isDirectory()) {
            File[] albumFolders = albumsDir.listFiles(File::isDirectory);
            if (albumFolders != null) {
                for (File albumFolder : albumFolders) {
                    String albumTitle = albumFolder.getName();
                    File albumFile = new File(albumFolder, "album.txt");
                    if (!albumFile.exists()) continue;

                    List<String> albumData = FileUtil.readFile(albumFile.getPath());
                    String songsLine = albumData.stream()
                            .filter(line -> line.startsWith("Songs: "))
                            .findFirst()
                            .orElse("Songs: ");
                    String songs = songsLine.substring("Songs: ".length());
                    if (songs.isEmpty()) continue;

                    String[] songTitles = songs.split(",");
                    for (String songTitle : songTitles) {
                        if (songTitle.trim().isEmpty()) continue;
                        File songFile = new File(albumFolder, songTitle.trim() + "/" + songTitle.trim() + ".txt");
                        if (songFile.exists()) {
                            songListView.getItems().add(songTitle.trim() + " (" + albumTitle + ")");
                            songToPathMap.put(songTitle.trim() + " (" + albumTitle + ")", songFile.getPath());
                        }
                    }
                }
            }
        }
    }

    private String parseSongTitle(List<String> songData) {
        return songData.stream()
                .filter(line -> line.trim().startsWith("Song Title: "))
                .map(line -> line.trim().substring("Song Title: ".length()).trim())
                .findFirst()
                .orElse(null);
    }

    private void addSongSelectionListener() {
        checkComponent(songListView, "songListView");
        if (songListView != null) {
            songListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    displaySongMetadata(newValue);
                } else {
                    clearMetadata();
                }
            });
        }
    }

    private void displaySongMetadata(String selectedSong) {
        String songPath = songToPathMap.get(selectedSong);
        if (songPath == null) {
            clearMetadata();
            return;
        }

        File songFile = new File(songPath);
        if (songFile.exists()) {
            List<String> songData = FileUtil.readFile(songFile.getPath());
            updateMetadata(songData, selectedSong);
        } else {
            clearMetadata();
        }
    }

    private void updateMetadata(List<String> songData, String selectedSong) {
        String title = "";
        String lyrics = "";
        String albumArtPath = null;

        for (String line : songData) {
            if (line.startsWith("Song Title: ")) {
                title = line.substring("Song Title: ".length());
            } else if (line.startsWith("Lyrics: ")) {
                lyrics = line.substring("Lyrics: ".length());
            } else if (line.startsWith("AlbumArtPath: ")) {
                albumArtPath = line.substring("AlbumArtPath: ".length());
            }
        }

        updateFields(title, lyrics);
        updateImageView(albumArtPath);
    }

    private void updateFields(String title, String lyrics) {
        checkComponent(titleField, "titleField");
        checkComponent(lyricsArea, "lyricsArea");

        if (titleField != null) titleField.setText(title);
        if (lyricsArea != null) lyricsArea.setText(lyrics);
    }

    private void updateImageView(String albumArtPath) {
        checkComponent(imagePreview, "imagePreview");
        if (imagePreview == null) return;

        try {
            if (albumArtPath != null && !albumArtPath.isEmpty()) {
                File albumArtFile = new File(albumArtPath);
                if (albumArtFile.exists()) {
                    imagePreview.setImage(new Image(albumArtFile.toURI().toString()));
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
            imagePreview.setImage(new Image(Objects.requireNonNull(getClass().getResource(DEFAULT_IMAGE_PATH)).toExternalForm()));
        } catch (Exception e) {
            System.err.println("Failed to load default image: " + e.getMessage());
        }
    }

    private void clearMetadata() {
        updateFields("", "");
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
    public void editSong() {
        checkComponent(songListView, "songListView");
        String selectedSong = songListView != null ? songListView.getSelectionModel().getSelectedItem() : null;
        if (selectedSong == null) {
            AlertUtil.showError("No song selected for editing.");
            return;
        }

        checkComponent(titleField, "titleField");
        checkComponent(lyricsArea, "lyricsArea");
        String newTitle = titleField != null ? titleField.getText().trim() : "";
        String lyrics = lyricsArea != null ? lyricsArea.getText().trim() : "";
        if (newTitle.isEmpty() || lyrics.isEmpty()) {
            AlertUtil.showError("Title and lyrics are required.");
            return;
        }

        try {
            String songPath = songToPathMap.get(selectedSong);
            if (songPath == null) {
                AlertUtil.showError("Song path not found.");
                return;
            }

            File songFile = new File(songPath);
            if (!songFile.exists()) {
                AlertUtil.showError("Song file not found for the selected song.");
                return;
            }

            List<String> songData = FileUtil.readFile(songFile.getPath());
            String oldAlbumArtPath = null;
            String releaseDate = "";
            String artists = "";
            int likes = 0;
            int plays = 0;

            for (String line : songData) {
                if (line.startsWith("Release Date: ")) {
                    releaseDate = line.substring("Release Date: ".length());
                } else if (line.startsWith("Artists: ")) {
                    artists = line.substring("Artists: ".length());
                } else if (line.startsWith("Likes: ")) {
                    likes = Integer.parseInt(line.substring("Likes: ".length()));
                } else if (line.startsWith("Plays: ")) {
                    plays = Integer.parseInt(line.substring("Plays: ".length()));
                } else if (line.startsWith("AlbumArtPath: ")) {
                    oldAlbumArtPath = line.substring("AlbumArtPath: ".length());
                }
            }

            String newAlbumArtPath = selectedImageFile != null
                    ? songFileManager.saveAlbumArt(artist.getNickName(), newTitle, selectedImageFile)
                    : oldAlbumArtPath;

            updateSongFile(songFile, newTitle, lyrics, releaseDate, artists, likes, plays, newAlbumArtPath);

            String originalTitle = selectedSong.contains(" (") ? selectedSong.substring(0, selectedSong.indexOf(" (")) : selectedSong;
            if (!originalTitle.equals(newTitle)) {
                renameSongFolder(selectedSong, newTitle, songPath);
                if (selectedSong.contains(" (") && !selectedSong.contains(" (Single)")) {
                    String albumTitle = selectedSong.substring(selectedSong.indexOf(" (") + 2, selectedSong.length() - 1);
                    updateAlbumSongList(albumTitle, originalTitle, newTitle);
                }
            }

            loadSongs();
            clearMetadata();
            AlertUtil.showSuccess("Song '" + newTitle + "' updated successfully!");
        } catch (IOException e) {
            AlertUtil.showError("Error updating song: " + e.getMessage());
        }
    }

    private void updateSongFile(File songFile, String newTitle, String lyrics, String releaseDate, String artists, int likes, int plays, String albumArtPath) throws IOException {
        List<String> songData = FileUtil.readFile(songFile.getPath());
        StringBuilder updatedData = new StringBuilder();

        for (String line : songData) {
            if (line.startsWith("Song Title: ")) {
                updatedData.append("Song Title: ").append(newTitle).append("\n");
            } else if (line.startsWith("Lyrics: ")) {
                updatedData.append("Lyrics: ").append(lyrics).append("\n");
            } else if (line.startsWith("Release Date: ")) {
                updatedData.append("Release Date: ").append(releaseDate).append("\n");
            } else if (line.startsWith("Artists: ")) {
                updatedData.append("Artists: ").append(artists).append("\n");
            } else if (line.startsWith("Likes: ")) {
                updatedData.append("Likes: ").append(likes).append("\n");
            } else if (line.startsWith("Plays: ")) {
                updatedData.append("Plays: ").append(plays).append("\n");
            } else if (line.startsWith("AlbumArtPath: ")) {
                updatedData.append("AlbumArtPath: ").append(albumArtPath).append("\n");
            } else {
                updatedData.append(line).append("\n");
            }
        }

        FileUtil.writeFile(songFile.getPath(), Collections.singletonList(updatedData.toString()));
    }

    private void renameSongFolder(String selectedSong, String newTitle, String songPath) {
        File songFile = new File(songPath);
        File oldFolder = songFile.getParentFile();
        String newFolderPath = oldFolder.getParent() + "/" + newTitle;
        File newFolder = new File(newFolderPath);

        if (!oldFolder.renameTo(newFolder)) {
            AlertUtil.showError("Failed to rename song folder.");
            return;
        }

        songToPathMap.remove(selectedSong);
        songToPathMap.put(newTitle + (selectedSong.contains(" (") ? selectedSong.substring(selectedSong.indexOf(" (")) : ""), newFolderPath + "/" + newTitle + ".txt");
    }

    private void updateAlbumSongList(String albumTitle, String originalTitle, String newTitle) throws IOException {
        String albumPath = FileUtil.DATA_DIR + "artists/" + FileUtil.sanitizeFileName(artist.getNickName()) + "/albums/" + albumTitle;
        File albumFile = new File(albumPath, "album.txt");
        if (albumFile.exists()) {
            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            StringBuilder updatedAlbumData = new StringBuilder();

            for (String line : albumData) {
                if (line.startsWith("Songs: ")) {
                    String songs = line.substring("Songs: ".length());
                    String updatedSongs = songs.replace(originalTitle, newTitle);
                    updatedAlbumData.append("Songs: ").append(updatedSongs).append("\n");
                } else {
                    updatedAlbumData.append(line).append("\n");
                }
            }

            FileUtil.writeFile(albumFile.getPath(), Collections.singletonList(updatedAlbumData.toString()));
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