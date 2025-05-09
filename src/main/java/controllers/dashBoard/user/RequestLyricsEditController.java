package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import services.SessionManager;
import services.file.LyricsRequestManager;
import utils.AlertUtil;

public class RequestLyricsEditController extends BaseUserController {

    @FXML private Label songInfoLabel;
    @FXML private TextArea suggestedLyricsArea;
    @FXML private Button submitRequestButton;

    private LyricsRequestManager lyricsRequestManager;

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        lyricsRequestManager = new LyricsRequestManager();
        loadSongInfo();
    }

    private void loadSongInfo() {
        String artistName = SessionManager.getInstance().getSelectedArtist();
        String songTitle = SessionManager.getInstance().getSelectedSong();
        String albumTitle = SessionManager.getInstance().getSelectedAlbum();

        checkComponent(songInfoLabel, "songInfoLabel");
        if (songInfoLabel == null) return;

        if (songTitle != null) {
            songInfoLabel.setText("Song: " + songTitle + " - " + artistName + (albumTitle != null ? " (Album: " + albumTitle + ")" : ""));
        } else {
            songInfoLabel.setText("Album: " + albumTitle + " - " + artistName);
        }
    }

    @FXML
    public void submitRequest() {
        checkComponent(suggestedLyricsArea, "suggestedLyricsArea");
        checkComponent(submitRequestButton, "submitRequestButton");
        if (suggestedLyricsArea == null) return;

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
}