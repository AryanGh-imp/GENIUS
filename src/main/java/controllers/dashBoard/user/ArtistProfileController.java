package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import models.account.Account;
import models.account.Artist;
import models.account.User;
import services.SessionManager;
import services.file.ArtistFileManager;
import utils.AlertUtil;
import utils.FileUtil;
import utils.SceneUtil;

import java.io.File;
import java.util.List;

public class ArtistProfileController {

    @FXML private Label welcomeLabel;
    @FXML private Label artistNameLabel;
    @FXML private Label totalSongsLabel;
    @FXML private ListView<String> singlesListView;
    @FXML private ListView<String> albumsListView;
    @FXML private Button signOutButton;
    @FXML private Button followButton;

    private UserMenuBarHandler menuBarHandler;
    private Artist selectedArtist;
    private final ArtistFileManager artistFileManager = new ArtistFileManager();

    @FXML
    public void initialize() {
        menuBarHandler = new UserMenuBarHandler(signOutButton);
        initializeUI();
    }

    private void initializeUI() {
        welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUsername() + "!");
        loadArtistProfile();
        setupListViews();
        setupFollowButton();
    }

    private void loadArtistProfile() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        artistNameLabel.setText(artistName);
        int totalSongs = 0;

        List<Artist> allArtists = artistFileManager.loadAllArtists();
        selectedArtist = allArtists.stream()
                .filter(artist -> artist.getNickName().equals(artistName))
                .findFirst()
                .orElse(null);

        if (selectedArtist == null) {
            AlertUtil.showError("Artist not found.");
            return;
        }

        File singlesDir = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/singles/");
        if (checkFileExists(singlesDir) && singlesDir.isDirectory()) {
            totalSongs += loadItemsFromDirectory(singlesDir, singlesListView, "Song Name:", false);
        }

        File albumsDir = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/");
        if (checkFileExists(albumsDir) && albumsDir.isDirectory()) {
            totalSongs += loadItemsFromDirectory(albumsDir, albumsListView, "Album Title:", true);
        }

        totalSongsLabel.setText("Total Songs: " + totalSongs);
    }

    private int loadItemsFromDirectory(File directory, ListView<String> listView, String titlePrefix, boolean isAlbum) {
        int itemCount = 0;
        File[] folders = directory.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                File dataFile = new File(folder, folder.getName() + (titlePrefix.equals("Album Title:") ? "/album.txt" : ".txt"));
                if (checkFileExists(dataFile)) {
                    List<String> data = FileUtil.readFile(dataFile.getPath());
                    String title = parseTitle(data, titlePrefix, folder.getName());
                    if (!title.isEmpty()) {
                        listView.getItems().add(title);
                        if (isAlbum) {
                            // Count songs in the album
                            String songsLine = data.stream()
                                    .filter(line -> line.startsWith("Songs: "))
                                    .findFirst()
                                    .orElse("Songs: ");
                            String[] songTitles = songsLine.substring("Songs: ".length()).split(",");
                            itemCount += songTitles.length;
                        } else {
                            itemCount++; // For singles, each item is one song
                        }
                    }
                }
            }
        }
        return itemCount;
    }

    private String parseTitle(List<String> data, String prefix, String defaultTitle) {
        return data.stream()
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .map(line -> line.substring(prefix.length()).trim())
                .orElse(defaultTitle);
    }

    private boolean checkFileExists(File file) {
        return file != null && file.exists();
    }

    private void setupListViews() {
        setupListView(singlesListView);
        setupListView(albumsListView);
    }

    private void setupListView(ListView<String> listView) {
        listView.setOnMouseClicked(event -> {
            String selectedItem = listView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                SceneUtil.changeScene(listView, "/FXML-files/user/SongAndAlbumDetails.fxml");
            }
        });
    }

    private void setupFollowButton() {
        Account currentAccount = SessionManager.getInstance().getCurrentAccount();
        if (!(currentAccount instanceof User currentUser)) {
            followButton.setDisable(true);
            followButton.setText("Not Available");
            return;
        }

        followButton.setText(isFollowingArtist(currentUser) ? "Unfollow" : "Follow");
    }

    private boolean isFollowingArtist(User currentUser) {
        return currentUser.getFollowingArtists().stream()
                .anyMatch(artist -> artist.getNickName().equals(selectedArtist.getNickName()));
    }

    @FXML
    public void toggleFollowArtist() {
        Account currentAccount = SessionManager.getInstance().getCurrentAccount();
        if (!(currentAccount instanceof User currentUser)) {
            AlertUtil.showError("Only users can follow artists.");
            return;
        }

        try {
            if (isFollowingArtist(currentUser)) {
                currentUser.unfollowArtist(selectedArtist);
                followButton.setText("Follow");
                AlertUtil.showSuccess("Unfollowed " + selectedArtist.getNickName() + " successfully.");
            } else {
                currentUser.followArtist(selectedArtist);
                followButton.setText("Unfollow");
                AlertUtil.showSuccess("Followed " + selectedArtist.getNickName() + " successfully.");
            }
        } catch (Exception e) {
            AlertUtil.showError("Failed to update follow status: " + e.getMessage());
        }
    }

    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToSearch() { menuBarHandler.goToSearch(); }
    @FXML public void goToCharts() { menuBarHandler.goToCharts(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}