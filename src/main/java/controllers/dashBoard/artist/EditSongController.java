package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditSongController extends BaseArtistController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> songListView;
    @FXML private Button signOutButton;
    @FXML private TextField titleField;
    @FXML private TextArea lyricsArea;
    @FXML private ImageView imagePreview;
    @FXML private Button chooseImageButton;
    @FXML private Button submitButton;

    private final SongFileManager songFileManager = new SongFileManager();
    private final Map<String, String> songToPathMap = new HashMap<>();
    private File selectedImageFile;

    @FXML
    public void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        loadSongs();
        addSongSelectionListener();
        updateImageView(imagePreview, null); // Set default image
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
        updateImageView(imagePreview, albumArtPath);
    }

    private void updateFields(String title, String lyrics) {
        checkComponent(titleField, "titleField");
        checkComponent(lyricsArea, "lyricsArea");

        if (titleField != null) titleField.setText(title);
        if (lyricsArea != null) lyricsArea.setText(lyrics);
    }

    private void clearMetadata() {
        updateFields("", "");
        updateImageView(imagePreview, null);
        selectedImageFile = null;
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
            updateImageView(imagePreview, file.toURI().toString());
        }
    }

    @FXML
    private void editSong() {
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
                renameSongFolder(originalTitle, newTitle, songPath, selectedSong);
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
        } catch (NumberFormatException e) {
            AlertUtil.showError("Invalid numeric data in song file: " + e.getMessage());
        }
    }

    private void updateSongFile(File songFile, String newTitle, String lyrics, String releaseDate, String artists, int likes, int plays, String albumArtPath) throws IOException {
        List<String> updatedData = new ArrayList<>();
        boolean titleUpdated = false, lyricsUpdated = false, artUpdated = false;

        for (String line : FileUtil.readFile(songFile.getPath())) {
            if (line.startsWith("Song Title: ") && !titleUpdated) {
                updatedData.add("Song Title: " + newTitle);
                titleUpdated = true;
            } else if (line.startsWith("Lyrics: ") && !lyricsUpdated) {
                updatedData.add("Lyrics: " + lyrics);
                lyricsUpdated = true;
            } else if (line.startsWith("Release Date: ")) {
                updatedData.add("Release Date: " + releaseDate);
            } else if (line.startsWith("Artists: ")) {
                updatedData.add("Artists: " + artists);
            } else if (line.startsWith("Likes: ")) {
                updatedData.add("Likes: " + likes);
            } else if (line.startsWith("Plays: ")) {
                updatedData.add("Plays: " + plays);
            } else if (line.startsWith("AlbumArtPath: ") && !artUpdated) {
                updatedData.add("AlbumArtPath: " + albumArtPath);
                artUpdated = true;
            } else {
                updatedData.add(line);
            }
        }

        if (!titleUpdated) updatedData.add("Song Title: " + newTitle);
        if (!lyricsUpdated) updatedData.add("Lyrics: " + lyrics);
        if (!artUpdated && albumArtPath != null) updatedData.add("AlbumArtPath: " + albumArtPath);

        FileUtil.writeFile(songFile.getPath(), updatedData);
    }

    private void renameSongFolder(String originalTitle, String newTitle, String songPath, String selectedSong) {
        File songFile = new File(songPath);
        File oldFolder = songFile.getParentFile();
        String newFolderPath = oldFolder.getParent() + "/" + FileUtil.sanitizeFileName(newTitle);
        File newFolder = new File(newFolderPath);

        try {
            FileUtil.renameDirectory(oldFolder, newFolder);
            String newSongPath = newFolderPath + "/" + FileUtil.sanitizeFileName(newTitle) + ".txt";
            songToPathMap.remove(selectedSong);
            songToPathMap.put(newTitle + (selectedSong.contains(" (") ? selectedSong.substring(selectedSong.indexOf(" (")) : " (Single)"), newSongPath);
        } catch (IOException e) {
            AlertUtil.showError("Failed to rename song folder: " + e.getMessage());
        }
    }

    private void updateAlbumSongList(String albumTitle, String originalTitle, String newTitle) throws IOException {
        String albumPath = FileUtil.DATA_DIR + "artists/" + FileUtil.sanitizeFileName(artist.getNickName()) + "/albums/" + albumTitle + "/album.txt";
        File albumFile = new File(albumPath);
        if (albumFile.exists()) {
            List<String> albumData = FileUtil.readFile(albumFile.getPath());
            List<String> updatedData = new ArrayList<>();

            for (String line : albumData) {
                if (line.startsWith("Songs: ")) {
                    String songs = line.substring("Songs: ".length());
                    String[] songArray = songs.split(",");
                    StringBuilder newSongs = new StringBuilder("Songs: ");
                    boolean found = false;
                    for (String song : songArray) {
                        if (song.trim().equals(originalTitle)) {
                            newSongs.append(newTitle).append(",");
                            found = true;
                        } else {
                            newSongs.append(song.trim()).append(",");
                        }
                    }
                    if (!found) newSongs.append(newTitle).append(",");
                    updatedData.add(newSongs.toString().replaceAll(",$", ""));
                } else {
                    updatedData.add(line);
                }
            }

            FileUtil.writeFile(albumFile.getPath(), updatedData);
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