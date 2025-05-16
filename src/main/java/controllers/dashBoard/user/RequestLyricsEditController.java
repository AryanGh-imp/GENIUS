package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import services.SessionManager;
import services.file.LyricsRequestManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;

public class RequestLyricsEditController extends BaseUserController {

    @FXML private Label songInfoLabel;
    @FXML private TextArea suggestedLyricsArea;
    @FXML private Button submitRequestButton;

    private LyricsRequestManager lyricsRequestManager;
    private final SongFileManager songFileManager = new SongFileManager();

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
            // Upload the original song lyrics to verify its existence
            File songFile = new File(FileUtil.DATA_DIR + "artists/" + artistName +
                    (albumTitle != null ? "/albums/" + albumTitle + "/" + songTitle + "/" + songTitle + ".txt" :
                            "/singles/" + songTitle + "/" + songTitle + ".txt"));
            if (songFile.exists()) {
                String lyricsFilePath = songFile.getPath().replace(".txt", "_lyrics.txt");
                System.out.println("Attempting to load lyrics from: " + lyricsFilePath);
                String originalLyrics = songFileManager.loadLyrics(songFile.getPath());
                if (originalLyrics == null || originalLyrics.trim().isEmpty()) {
                    System.out.println("Lyrics file not found or empty at: " + lyricsFilePath);
                    AlertUtil.showWarning("Original lyrics not found for this song.");
                    suggestedLyricsArea.setText(""); // Leave the box empty if lyrics are not found.
                } else {
                    System.out.println("Loaded lyrics: " + originalLyrics.substring(0, Math.min(50, originalLyrics.length())) + "...");
                    SessionManager.getInstance().setSelectedOriginalLyrics(originalLyrics);
                    suggestedLyricsArea.setText(originalLyrics); // Display the original text in the box
                }
            } else {
                System.out.println("Song file not found: " + songFile.getPath());
                AlertUtil.showError("Song file not found: " + songFile.getPath());
                suggestedLyricsArea.setText(""); // Leave the box empty if the file is not found.
            }
        } else {
            songInfoLabel.setText("Album: " + albumTitle + " - " + artistName);
            suggestedLyricsArea.setText(""); // If it's an album, leave the box empty.
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