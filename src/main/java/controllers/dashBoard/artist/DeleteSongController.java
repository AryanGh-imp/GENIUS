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

public class DeleteSongController {

    @FXML private Label welcomeLabel;
    @FXML private ListView<String> songListView;
    @FXML private Button signOutButton;
    @FXML private Label titleLabel;
    @FXML private Label albumLabel;
    @FXML private Label releaseDateLabel;
    @FXML private Label viewsLabel;
    @FXML private Label likesLabel;
    @FXML private ImageView albumArtImageView;

    private String currentArtistNickName;
    private final SongFileManager songFileManager = new SongFileManager();
    private final Map<String, String> songToPathMap = new HashMap<>();
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
        loadSongs();
        addSongSelectionListener();
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

    private void loadSongs() {
        checkComponent(songListView, "songListView");
        if (songListView == null) return;

        songListView.getItems().clear();
        songToPathMap.clear();

        String safeNickName = FileUtil.sanitizeFileName(currentArtistNickName);
        String artistDir = FileUtil.DATA_DIR + "artists/" + safeNickName + "/";
        loadSongsFromDirectory(artistDir + "singles/", null);
        loadSongsFromDirectory(artistDir + "albums/", true);
    }

    private void loadSongsFromDirectory(String directoryPath, Boolean isAlbumDir) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] folders = directory.listFiles(File::isDirectory);
        if (folders == null) {
            return;
        }

        for (File folder : folders) {
            if (isAlbumDir != null && isAlbumDir) {
                String albumTitle = folder.getName();
                File[] songFolders = folder.listFiles(File::isDirectory);
                if (songFolders != null) {
                    for (File songFolder : songFolders) {
                        loadSong(songFolder, albumTitle);
                    }
                }
            } else {
                loadSong(folder, null);
            }
        }
    }

    private void loadSong(File songFolder, String albumTitle) {
        File songFile = new File(songFolder, songFolder.getName() + ".txt");
        if (songFile.exists()) {
            List<String> songData = FileUtil.readFile(songFile.getPath());
            String songTitle = parseSongTitle(songData);
            if (songTitle != null) {
                String displayTitle = albumTitle != null ? songTitle + " (Album: " + albumTitle + ")" : songTitle;
                songListView.getItems().add(displayTitle);
                songToPathMap.put(displayTitle, songFolder.getPath());
            }
        }
    }

    private String parseSongTitle(List<String> songData) {
        return songData.stream()
                .filter(line -> line.startsWith("Song Name: "))
                .map(line -> line.substring("Song Name: ".length()))
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

    private String[] extractSongInfo(String selectedSong) {
        String songTitle = selectedSong.contains("(Album: ") ?
                selectedSong.substring(0, selectedSong.indexOf(" (Album: ")) : selectedSong;
        String albumName = selectedSong.contains("(Album: ") ?
                selectedSong.substring(selectedSong.indexOf("(Album: ") + 8, selectedSong.length() - 1) : null;
        return new String[]{songTitle, albumName};
    }

    private void displaySongMetadata(String selectedSong) {
        String[] songInfo = extractSongInfo(selectedSong);
        String songTitle = songInfo[0];
        String albumName = songInfo[1];

        String songPath = songToPathMap.get(selectedSong);
        if (songPath == null) {
            clearMetadata();
            return;
        }

        File songFile = new File(songPath, FileUtil.sanitizeFileName(songTitle) + ".txt");
        if (songFile.exists()) {
            List<String> songData = FileUtil.readFile(songFile.getPath());
            updateMetadata(songData, songTitle, albumName);
        } else {
            clearMetadata();
        }
    }

    private void updateMetadata(List<String> songData, String songTitle, String albumName) {
        String releaseDate = "N/A";
        String views = "0";
        String likes = "0";
        String albumArtPath = null;

        for (String line : songData) {
            if (line.startsWith("Release Date: ")) {
                releaseDate = line.substring("Release Date: ".length());
            } else if (line.startsWith("Views: ")) {
                views = line.substring("Views: ".length());
            } else if (line.startsWith("Likes: ")) {
                likes = line.substring("Likes: ".length());
            } else if (line.startsWith("AlbumArtPath: ")) {
                albumArtPath = line.substring("AlbumArtPath: ".length());
            }
        }

        updateLabels(songTitle, albumName, releaseDate, views, likes);
        updateImageView(albumArtPath);
    }

    private void updateLabels(String songTitle, String albumName, String releaseDate, String views, String likes) {
        checkComponent(titleLabel, "titleLabel");
        checkComponent(albumLabel, "albumLabel");
        checkComponent(releaseDateLabel, "releaseDateLabel");
        checkComponent(viewsLabel, "viewsLabel");
        checkComponent(likesLabel, "likesLabel");

        if (titleLabel != null) titleLabel.setText("Title: " + songTitle);
        if (albumLabel != null) albumLabel.setText("Album: " + (albumName != null ? albumName : "Single"));
        if (releaseDateLabel != null) releaseDateLabel.setText("Release Date: " + releaseDate);
        if (viewsLabel != null) viewsLabel.setText("Views: " + views);
        if (likesLabel != null) likesLabel.setText("Likes: " + likes);
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
        updateLabels("", null, "N/A", "", "");
        loadDefaultImage();
    }

    @FXML
    public void deleteSong() {
        checkComponent(songListView, "songListView");
        String selectedSong = songListView != null ? songListView.getSelectionModel().getSelectedItem() : null;
        if (selectedSong == null) {
            AlertUtil.showError("No song selected for deletion.");
            return;
        }

        String[] songInfo = extractSongInfo(selectedSong);
        String songTitle = songInfo[0];
        String albumName = songInfo[1];

        songFileManager.deleteSong(currentArtistNickName, songTitle, albumName);
        loadSongs();
        clearMetadata();
        AlertUtil.showSuccess("Deleted song: " + songTitle + (albumName != null ? " from album: " + albumName : ""));
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