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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteSongController extends BaseArtistController {

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

    @FXML
    public void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        currentArtistNickName = artist.getNickName();
        loadSongs();
        addSongSelectionListener();
        loadDefaultImage(albumArtImageView);
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
            String songTitle = songData.stream()
                    .filter(line -> line.startsWith("Song Name: "))
                    .map(line -> line.substring("Song Name: ".length()))
                    .findFirst()
                    .orElse(null);
            if (songTitle != null) {
                String displayTitle = albumTitle != null ? songTitle + " (Album: " + albumTitle + ")" : songTitle;
                songListView.getItems().add(displayTitle);
                songToPathMap.put(displayTitle, songFolder.getPath());
            }
        }
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
            updateImageView(albumArtImageView, albumArtPath);
        } else {
            clearMetadata();
        }
    }

    private void clearMetadata() {
        checkComponent(titleLabel, "titleLabel");
        checkComponent(albumLabel, "albumLabel");
        checkComponent(releaseDateLabel, "releaseDateLabel");
        checkComponent(viewsLabel, "viewsLabel");
        checkComponent(likesLabel, "likesLabel");
        if (titleLabel != null) titleLabel.setText("Title: ");
        if (albumLabel != null) albumLabel.setText("Album: ");
        if (releaseDateLabel != null) releaseDateLabel.setText("Release Date: N/A");
        if (viewsLabel != null) viewsLabel.setText("Views: 0");
        if (likesLabel != null) likesLabel.setText("Likes: 0");
        loadDefaultImage(albumArtImageView);
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