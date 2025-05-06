package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import services.SessionManager;
import services.file.LyricsRequestManager;
import utils.AlertUtil;

public class RequestLyricsEditController {

    @FXML private Label welcomeLabel;
    @FXML private Label songInfoLabel;
    @FXML private TextArea suggestedLyricsArea;
    @FXML private Button submitRequestButton;
    @FXML private Button signOutButton;

    private UserMenuBarHandler menuBarHandler;
    private LyricsRequestManager lyricsRequestManager;

    @FXML
    public void initialize() {
        menuBarHandler = new UserMenuBarHandler(signOutButton);
        lyricsRequestManager = new LyricsRequestManager();
        welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUsername() + "!");
        loadSongInfo();
    }

    private void loadSongInfo() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        if (songTitle != null) {
            songInfoLabel.setText("Song: " + songTitle + " - " + artistName + (albumTitle != null ? " (Album: " + albumTitle + ")" : ""));
        } else {
            songInfoLabel.setText("Album: " + albumTitle + " - " + artistName);
        }
    }

    @FXML
    public void submitRequest() {
        String suggestedLyrics = suggestedLyricsArea.getText().trim();
        if (suggestedLyrics.isEmpty()) {
            AlertUtil.showError("Please provide suggested lyrics.");
            return;
        }

        String username = SessionManager.getInstance().getCurrentUsername();
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        try {
            lyricsRequestManager.saveLyricsEditRequest(artistName, songTitle, albumTitle, suggestedLyrics, username);
            AlertUtil.showSuccess("Lyrics edit request submitted successfully!");
            suggestedLyricsArea.clear();
        } catch (IllegalStateException e) {
            AlertUtil.showError("Failed to submit request: " + e.getMessage());
        }
    }

    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToSearch() { menuBarHandler.goToSearch(); }
    @FXML public void goToCharts() { menuBarHandler.goToCharts(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}