package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import models.account.Artist;
import models.music.Album;
import models.music.Song;
import services.AccountManager;
import services.SessionManager;
import services.file.ArtistFileManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;
import utils.SceneUtil;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public abstract class BaseUserController {

    @FXML protected Label welcomeLabel;
    @FXML protected Node signOutButton;

    protected UserMenuBarHandler menuBarHandler;
    protected static final String DEFAULT_IMAGE_PATH = "/pics/Genius.com_logo_yellow.png";
    private final ArtistFileManager artistFileManager = new ArtistFileManager();

    @FXML
    protected void initialize() {
        menuBarHandler = new UserMenuBarHandler(signOutButton);
        setupWelcomeLabel();
    }

    protected void setupWelcomeLabel() {
        if (welcomeLabel != null) {
            String username = SessionManager.getInstance().getCurrentUsername();
            welcomeLabel.setText(username != null ? "Welcome, " + username + "!" : "Welcome, Guest!");
        }
    }

    protected List<String> loadFileData(String filePath) {
        try {
            return FileUtil.readFile(filePath);
        } catch (Exception e) {
            System.err.println("Error loading file data from " + filePath + ": " + e.getMessage());
            return List.of();
        }
    }

    protected void loadImage(ImageView imageView, String imagePath) {
        if (imageView == null) return;
        File imageFile = new File(imagePath);
        try {
            if (imageFile.exists()) {
                imageView.setImage(new Image(imageFile.toURI().toString()));
            } else {
                InputStream imageStream = getClass().getResourceAsStream(DEFAULT_IMAGE_PATH);
                if (imageStream == null) {
                    System.err.println("Default image not found in resources: " + DEFAULT_IMAGE_PATH);
                    return;
                }
                imageView.setImage(new Image(imageStream));
            }
        } catch (Exception e) {
            System.err.println("Error loading image from " + imagePath + ": " + e.getMessage());
            try {
                InputStream imageStream = getClass().getResourceAsStream(DEFAULT_IMAGE_PATH);
                if (imageStream == null) {
                    System.err.println("Default image not found in resources: " + DEFAULT_IMAGE_PATH);
                    return;
                }
                imageView.setImage(new Image(imageStream));
            } catch (Exception ex) {
                System.err.println("Error loading default image: " + ex.getMessage());
            }
        }
    }

    protected void setupListView(ListView<String> listView, String targetFxmlPath, String itemType) {
        if (listView == null) return;

        Label placeholder = new Label("None");
        placeholder.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-alignment: center;");
        listView.setPlaceholder(placeholder);

        listView.setOnMouseClicked(event -> {
            String selectedItem = listView.getSelectionModel().getSelectedItem();
            if (selectedItem == null || selectedItem.isEmpty() ||
                    selectedItem.equals("No artists followed.") ||
                    selectedItem.contains("No results") ||
                    selectedItem.contains("No artists")) {
                return;
            }

            switch (itemType.toLowerCase()) {
                case "artist":
                    String email = artistFileManager.findEmailByNickName(selectedItem, "artist");
                    if (email != null) {
                        SessionManager.getInstance().setSelectedArtist(selectedItem);
                        SessionManager.getInstance().setSelectedArtistEmail(email);
                        try {
                            SceneUtil.changeScene(listView, targetFxmlPath);
                        } catch (Exception e) {
                            AlertUtil.showError("Failed to load page: " + e.getMessage());
                        }
                    } else {
                        AlertUtil.showError("Email not found for artist: " + selectedItem);
                    }
                    break;

                case "album":
                    String currentArtist = SessionManager.getInstance().getSelectedArtist();
                    if (currentArtist == null) {
                        AlertUtil.showError("No artist selected. Please select an artist first.");
                        return;
                    }
                    SessionManager.getInstance().setSelectedAlbum(selectedItem);
                    SessionManager.getInstance().setSelectedSong(null);
                    try {
                        SceneUtil.changeScene(listView, targetFxmlPath);
                    } catch (Exception e) {
                        AlertUtil.showError("Failed to load page: " + e.getMessage());
                    }
                    break;

                case "song":
                    String currentArtistForSong = SessionManager.getInstance().getSelectedArtist();
                    String currentAlbum = SessionManager.getInstance().getSelectedAlbum();
                    if (currentArtistForSong == null) {
                        AlertUtil.showError("No artist selected. Please select an artist first.");
                        return;
                    }
                    // Increase views when clicking on a song
                    incrementViewsForItem(currentArtistForSong, selectedItem, currentAlbum, false);
                    SessionManager.getInstance().setSelectedSong(selectedItem);
                    System.out.println("Selected song: " + selectedItem + " for artist: " + currentArtistForSong + ", album: " + currentAlbum);
                    // If the current controller is SongAndAlbumDetailsController, just call loadDetails
                    if (this instanceof SongAndAlbumDetailsController) {
                        ((SongAndAlbumDetailsController) this).loadDetails();
                    } else {
                        try {
                            SceneUtil.changeScene(listView, targetFxmlPath);
                        } catch (Exception e) {
                            AlertUtil.showError("Failed to load page: " + e.getMessage());
                        }
                    }
                    break;

                default:
                    System.err.println("Unsupported item type for ListView: " + itemType);
                    AlertUtil.showError("Unsupported item type: " + itemType);
            }
        });
    }

    protected void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }


    protected void incrementViewsForItem(String artistName, String songTitle, String albumTitle, boolean isAlbum) {
        if (artistName == null || artistName.trim().isEmpty()) {
            throw new IllegalArgumentException("Artist name cannot be null or empty");
        }

        String basePath = FileUtil.DATA_DIR + "artists/" + artistName + (albumTitle != null ? "/albums/" + albumTitle : "/singles/");
        if (isAlbum) {
            System.out.println("Viewing album: " + albumTitle + ". Song views will not be incremented.");
        } else {
            if (songTitle == null || songTitle.trim().isEmpty()) {
                System.out.println("No song title provided, skipping view increment for song.");
                return;
            }
            File songFile = new File(basePath + "/" + songTitle + "/" + songTitle + ".txt");
            if (songFile.exists()) {
                SongFileManager songFileManager = new SongFileManager();
                List<String> songData = FileUtil.readFile(songFile.getPath());
                String artistEmail = artistFileManager.findEmailByNickName(artistName, "artist");
                if (artistEmail == null || !AccountManager.validateEmail(artistEmail)) {
                    System.err.println("Artist email not found or invalid for artist: " + artistName + ". Using default email.");
                    artistEmail = "default@example.com"; // Valid default email
                }

                Artist artist = artistFileManager.getArtistByNickName(artistName);
                if (artist == null) {
                    artist = new Artist(artistEmail, artistName, "defaultPassword");
                    System.out.println("Created new Artist instance for: " + artistName);
                }

                Song song = songFileManager.parseSongFromFile(
                        songData,
                        albumTitle != null ? new Album(albumTitle, "Not set", artist) : null,
                        songFileManager.loadLyrics(songFile.getPath()),
                        artist
                );
                song.incrementViews();
                songFileManager.saveSong(Collections.singletonList(artistName), songTitle, albumTitle, song.getLyrics(), song.getReleaseDate(), song.getLikes(), song.getViews(), song.getAlbumArtPath());
                System.out.println("Views updated and saved for song: " + songTitle + ", New Views: " + song.getViews());
            }
        }
    }

    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToSearch() { menuBarHandler.goToSearch(); }
    @FXML public void goToCharts() { menuBarHandler.goToCharts(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}