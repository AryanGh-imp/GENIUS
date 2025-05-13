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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

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
    private final ArtistFileManager artistFileManager = new ArtistFileManager();
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
        updateImageView(imagePreview, null);
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
                    String songTitle = songFolder.getName();
                    File songFile = new File(songFolder, songTitle + ".txt");
                    if (songFile.exists()) {
                        List<String> songData = FileUtil.readFile(songFile.getPath());
                        String parsedTitle = parseSongTitle(songData);
                        if (parsedTitle != null && !parsedTitle.isEmpty()) {
                            songListView.getItems().add(parsedTitle + " (Single)");
                            songToPathMap.put(parsedTitle + " (Single)", songFile.getPath());
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
                    if (albumFile.exists()) {
                        List<String> albumData = FileUtil.readFile(albumFile.getPath());
                        String songsLine = albumData.stream().filter(line -> line.startsWith("Songs: ")).findFirst().orElse("Songs: ");
                        String[] songTitles = songsLine.substring("Songs: ".length()).split(",");
                        for (String songTitle : songTitles) {
                            if (!songTitle.trim().isEmpty()) {
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
        }
    }

    private String parseSongTitle(List<String> songData) {
        return songData.stream()
                .filter(line -> line.trim().startsWith("Song Name: "))
                .map(line -> line.trim().substring("Song Name: ".length()).trim())
                .findFirst()
                .orElse(null);
    }

    private void addSongSelectionListener() {
        checkComponent(songListView, "songListView");
        if (songListView != null) {
            songListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) displaySongMetadata(newValue);
                else clearMetadata();
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
            String lyrics = songFileManager.loadLyrics(songPath);
            updateMetadata(songData, lyrics, selectedSong);
        } else {
            clearMetadata();
        }
    }

    private void updateMetadata(List<String> songData, String lyrics, String selectedSong) {
        String title = "";
        String albumArtPath = null;

        for (String line : songData) {
            if (line.startsWith("Song Name: ")) title = line.substring("Song Name: ".length());
            else if (line.startsWith("AlbumArtPath: ")) albumArtPath = line.substring("AlbumArtPath: ".length());
            else if (line.startsWith("SongArtPath: ")) albumArtPath = line.substring("SongArtPath: ".length());
        }

        updateFields(title, lyrics != null ? lyrics : "");
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
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
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
            int views = 0;

            for (String line : songData) {
                if (line.startsWith("Release Date: ")) releaseDate = line.substring("Release Date: ".length());
                else if (line.startsWith("Artists: ")) artists = line.substring("Artists: ".length());
                else if (line.startsWith("Likes: ")) likes = Integer.parseInt(line.substring("Likes: ".length()));
                else if (line.startsWith("Views: ")) views = Integer.parseInt(line.substring("Views: ".length()));
                else if (line.startsWith("AlbumArtPath: ")) oldAlbumArtPath = line.substring("AlbumArtPath: ".length());
                else if (line.startsWith("SongArtPath: ")) oldAlbumArtPath = line.substring("SongArtPath: ".length());
            }

            String originalTitle = selectedSong.contains(" (") ? selectedSong.substring(0, selectedSong.indexOf(" (")) : selectedSong;
            String albumTitle = selectedSong.contains(" (") && !selectedSong.contains(" (Single)")
                    ? selectedSong.substring(selectedSong.indexOf(" (") + 2, selectedSong.length() - 1)
                    : null;

            // Finding a song in the artist list
            Song targetSong = findSong(originalTitle, albumTitle);
            if (targetSong == null) {
                AlertUtil.showError("Song not found in artist's list.");
                return;
            }

            // Set a new path for the image file.
            String newSongDir = songFileManager.getSongDir(artist.getNickName(), newTitle, albumTitle);
            String newAlbumArtPath = oldAlbumArtPath;

            // If the song name has changed, update the directory and files.
            if (!originalTitle.equals(newTitle)) {
                String oldSongDir = songFileManager.getSongDir(artist.getNickName(), originalTitle, albumTitle);
                File oldFolder = new File(oldSongDir);
                File newFolder = new File(newSongDir);

                // If the new directory exists, delete it first.
                if (newFolder.exists()) {
                    FileUtil.deleteDirectory(newFolder);
                }

                // Rename a directory
                FileUtil.renameDirectory(oldFolder, newFolder);

                // Transfer image file if present and update path (only for single songs)
                if (oldAlbumArtPath != null && (albumTitle == null || albumTitle.isEmpty())) {
                    File oldArtFile = new File(FileUtil.DATA_DIR + oldAlbumArtPath.replace("data/data", "data"));
                    System.out.println("Checking old art file: " + oldArtFile.getAbsolutePath() + " - Exists: " + oldArtFile.exists());
                    if (oldArtFile.exists()) {
                        String artFileName = oldArtFile.getName();
                        File newArtFile = new File(newSongDir + artFileName);
                        FileUtil.ensureDataDirectoryExists(newSongDir);
                        Files.copy(oldArtFile.toPath(), newArtFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        newAlbumArtPath = "data/artists/" + FileUtil.sanitizeFileName(artist.getNickName()) + "/singles/" + newTitle + "/" + artFileName;
                        System.out.println("Updated SongArtPath after rename: " + newAlbumArtPath);
                    } else {
                        System.out.println("Old art file does not exist: " + oldArtFile.getAbsolutePath());
                        // Automatically check for files in new directory
                        File newArtCheck = new File(newSongDir + oldArtFile.getName());
                        if (newArtCheck.exists()) {
                            newAlbumArtPath = "data/artists/" + FileUtil.sanitizeFileName(artist.getNickName()) + "/singles/" + newTitle + "/" + newArtCheck.getName();
                            System.out.println("Found art file in new directory: " + newAlbumArtPath);
                        }
                    }
                }
            }

            // If a new image is selected, save it.
            if (selectedImageFile != null) {
                newAlbumArtPath = songFileManager.saveSingleSongArt(artist.getNickName(), newTitle, selectedImageFile);
                System.out.println("Updated SongArtPath after new image: " + newAlbumArtPath);
            } else if (newAlbumArtPath != null && new File(FileUtil.DATA_DIR + newAlbumArtPath).exists()) {
                // Keep old path if file still exists
                System.out.println("Reusing existing SongArtPath: " + newAlbumArtPath);
            }

            // Save song with new metadata
            System.out.println("Saving edited song: " + newTitle + ", Likes: " + likes + ", Views: " + views + ", SongArtPath: " + newAlbumArtPath);
            songFileManager.saveSong(
                    List.of(artist.getNickName()),
                    newTitle,
                    albumTitle,
                    lyrics,
                    releaseDate,
                    likes,
                    views,
                    newAlbumArtPath
            );

            // Reload songs and albums to update artist list
            songFileManager.loadSongsAndAlbumsForArtist(artist, artistFileManager);

            // رفرش لیست آهنگ‌ها
            loadSongs();
            clearMetadata();
            AlertUtil.showSuccess("Song '" + newTitle + "' updated successfully!");
        } catch (IOException e) {
            AlertUtil.showError("Error updating song: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            AlertUtil.showError("Invalid numeric data in song file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertUtil.showError("Unexpected error while updating song: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Song findSong(String songTitle, String albumTitle) {
        if (albumTitle != null && !albumTitle.equals("Single")) {
            for (Album album : artist.getAlbums()) {
                if (album.getTitle().equals(albumTitle)) {
                    for (Song song : album.getSongs()) {
                        if (song.getTitle().equals(songTitle)) {
                            return song;
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < artist.getSingles().size(); i++) {
                if (artist.getSingles().get(i).getTitle().equals(songTitle)) {
                    return artist.getSingles().get(i);
                }
            }
        }
        return null;
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