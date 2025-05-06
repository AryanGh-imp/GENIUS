package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import models.DTO.SongDTO;
import services.SearchAndChartManager;
import services.file.ArtistFileManager;
import services.file.SongFileManager;

import java.util.List;

public class ChartsPageController {

    @FXML private Label welcomeLabel;
    @FXML private Label song1Label;
    @FXML private Label song2Label;
    @FXML private Label song3Label;
    @FXML private Label song4Label;
    @FXML private Label song5Label;
    @FXML private Label song6Label;
    @FXML private Label song7Label;
    @FXML private Label song8Label;
    @FXML private Label song9Label;
    @FXML private Label song10Label;
    @FXML private Button signOutButton;

    private UserMenuBarHandler menuBarHandler;
    private final SearchAndChartManager searchManager;

    public ChartsPageController() {
        this.searchManager = new SearchAndChartManager(new ArtistFileManager(), new SongFileManager());
    }

    @FXML
    public void initialize() {
        menuBarHandler = new UserMenuBarHandler(signOutButton);
        initializeUI();
    }

    private void initializeUI() {
        welcomeLabel.setText("Welcome, User!");
        loadTopSongs();
    }

    private void loadTopSongs() {
        List<SongDTO> topSongs = searchManager.getTopSongs(10);
        Label[] labels = {song1Label, song2Label, song3Label, song4Label, song5Label,
                song6Label, song7Label, song8Label, song9Label, song10Label};

        for (int i = 0; i < labels.length; i++) {
            labels[i].setText(i < topSongs.size() ? formatSongLabel(i + 1, topSongs.get(i)) : formatEmptyLabel(i + 1));
        }
    }

    private String formatSongLabel(int rank, SongDTO song) {
        return String.format("%d. %s - %s (Views: %d, Likes: %d)%s",
                rank,
                song.title(),
                song.artistName(),
                song.views(),
                song.likes(),
                song.albumName() != null ? " (Album: " + song.albumName() + ")" : " (Single)");
    }

    private String formatEmptyLabel(int rank) {
        return String.format("%d. No Song Available", rank);
    }

    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToSearch() { menuBarHandler.goToSearch(); }
    @FXML public void goToCharts() { menuBarHandler.goToCharts(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}