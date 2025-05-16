package controllers.dashBoard.artist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import models.music.Album;
import models.music.Song;
import services.file.ArtistFileManager;
import services.file.SongFileManager;

public class ArtistDashboardController extends BaseArtistController {

    @FXML private Label welcomeLabel;
    @FXML private Label artistNicknameLabel;
    @FXML private Label totalSongsLabel;
    @FXML private Label totalAlbumsLabel;
    @FXML private Label totalLikesLabel;
    @FXML private Label totalViewsLabel;
    @FXML private Button signOutButton;

    private final SongFileManager songFileManager = new SongFileManager();
    private final ArtistFileManager artistFileManager = new ArtistFileManager();

    @FXML
    private void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        artist.loadSongsAndAlbums(songFileManager, artistFileManager);
        saveChanges();
        setArtistNickname();
        displayStatistics();
    }

    private void setArtistNickname() {
        checkComponent(artistNicknameLabel, "artistNicknameLabel");
        if (artistNicknameLabel != null) {
            artistNicknameLabel.setText(artist.getNickName());
        }
    }

    private void displayStatistics() {
        int totalSongs = calculateTotalSongs();
        int totalAlbums = artist.getAlbums().size();
        int[] likesAndViews = calculateLikesAndViews();

        updateStatisticsLabels(totalSongs, totalAlbums, likesAndViews);
    }

    private void updateStatisticsLabels(int totalSongs, int totalAlbums, int[] likesAndViews) {
        checkComponent(totalSongsLabel, "totalSongsLabel");
        checkComponent(totalAlbumsLabel, "totalAlbumsLabel");
        checkComponent(totalLikesLabel, "totalLikesLabel");
        checkComponent(totalViewsLabel, "totalViewsLabel");

        if (totalSongsLabel != null) totalSongsLabel.setText("Total Songs: " + totalSongs);
        if (totalAlbumsLabel != null) totalAlbumsLabel.setText("Total Albums: " + totalAlbums);
        if (totalLikesLabel != null) totalLikesLabel.setText("Total Likes: " + likesAndViews[0]);
        if (totalViewsLabel != null) totalViewsLabel.setText("Total Views: " + likesAndViews[1]);
    }

    private int calculateTotalSongs() {
        int total = artist.getSingles().size();
        for (Album album : artist.getAlbums()) {
            total += album.getSongs().size();
        }
        return total;
    }

    private int[] calculateLikesAndViews() {
        int totalLikes = 0;
        int totalViews = 0;

        System.out.println("Singles Likes and Views:");
        for (Song song : artist.getSingles()) {
            totalLikes += song.getLikes();
            totalViews += song.getViews();
            System.out.println("Song: " + song.getTitle() + ", Likes: " + song.getLikes() + ", Views: " + song.getViews());
        }

        System.out.println("Album Songs Likes and Views:");
        for (Album album : artist.getAlbums()) {
            for (Song song : album.getSongs()) {
                totalLikes += song.getLikes();
                totalViews += song.getViews();
                System.out.println("Album: " + album.getTitle() + ", Song: " + song.getTitle() + ", Likes: " + song.getLikes() + ", Views: " + song.getViews());
            }
        }

        return new int[]{totalLikes, totalViews};
    }

    public void saveChanges() {
        songFileManager.saveSongsAndAlbumsForArtist(artist);
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