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

import java.io.File;
import java.util.List;

public class ArtistProfileController extends BaseUserController {

    @FXML private Label artistNameLabel;
    @FXML private Label totalSongsLabel;
    @FXML private ListView<String> singlesListView;
    @FXML private ListView<String> albumsListView;
    @FXML private Button followButton;

    private Artist selectedArtist;
    private final ArtistFileManager artistFileManager = new ArtistFileManager();

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        loadArtistProfile();
        setupListViews();
        setupFollowButton();
    }

    private void loadArtistProfile() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        if (artistNameLabel != null) {
            artistNameLabel.setText(artistName != null ? artistName : "Unknown Artist");
        }

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
        if (singlesDir.exists() && singlesDir.isDirectory()) {
            totalSongs += loadItemsFromDirectory(singlesDir, singlesListView, "Song Name:", false);
        } else {
            System.out.println("Singles directory not found: " + singlesDir.getPath());
            singlesListView.getItems().clear();
            singlesListView.getItems().add("None - No singles available");
        }

        File albumsDir = new File(FileUtil.DATA_DIR + "artists/" + artistName + "/albums/");
        if (albumsDir.exists() && albumsDir.isDirectory()) {
            totalSongs += loadItemsFromDirectory(albumsDir, albumsListView, "Album Title:", true);
        } else {
            System.out.println("Albums directory not found: " + albumsDir.getPath());
            albumsListView.getItems().clear();
            albumsListView.getItems().add("None - No albums available");
        }

        if (totalSongsLabel != null) {
            totalSongsLabel.setText("Total Songs: " + totalSongs);
        }
    }

    private int loadItemsFromDirectory(File directory, ListView<String> listView, String titlePrefix, boolean isAlbum) {
        int itemCount = 0;
        if (listView == null) {
            System.err.println("ListView is null for " + (isAlbum ? "albums" : "singles"));
            return itemCount;
        }

        listView.getItems().clear();
        File[] folders = directory.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            System.out.println("No folders found in directory: " + directory.getPath());
            listView.getItems().add("None - No " + (isAlbum ? "albums" : "singles") + " available");
            return itemCount;
        }

        for (File folder : folders) {
            String folderName = folder.getName();
            File dataFile = new File(folder, isAlbum ? "album.txt" : folderName + ".txt");
            System.out.println("Checking file: " + dataFile.getPath());

            if (dataFile.exists()) {
                List<String> data = loadFileData(dataFile.getPath());
                String title = parseTitle(data, titlePrefix, folderName);
                if (!title.isEmpty()) {
                    listView.getItems().add(title);
                    if (isAlbum) {
                        String songsLine = data.stream()
                                .filter(line -> line.startsWith("Songs: "))
                                .findFirst()
                                .orElse("Songs: ");
                        String[] songTitles = songsLine.substring("Songs: ".length()).split(",");
                        itemCount += songTitles.length;
                    } else {
                        itemCount++;
                    }
                    System.out.println("Added " + (isAlbum ? "album" : "single") + ": " + title);
                } else {
                    System.err.println("Failed to parse title for folder: " + folderName);
                }
            } else {
                System.err.println("Data file not found: " + dataFile.getPath() + ". Using folder name as fallback.");
                listView.getItems().add(folderName); // Using directory name as fallback
                if (isAlbum) {
                    File[] songDirs = folder.listFiles(File::isDirectory);
                    itemCount += (songDirs != null) ? songDirs.length : 0;
                } else {
                    itemCount++;
                }
                System.out.println("Added fallback " + (isAlbum ? "album" : "single") + ": " + folderName);
            }
        }

        if (listView.getItems().isEmpty()) {
            listView.getItems().add("None - No " + (isAlbum ? "albums" : "singles") + " available");
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

    private void setupListViews() {
        setupListView(singlesListView, "/FXML-files/user/SongAndAlbumDetails.fxml", "song");
        setupListView(albumsListView, "/FXML-files/user/SongAndAlbumDetails.fxml", "album");
    }

    private void setupFollowButton() {
        if (followButton == null) return;

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
        if (followButton == null) return;

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
}