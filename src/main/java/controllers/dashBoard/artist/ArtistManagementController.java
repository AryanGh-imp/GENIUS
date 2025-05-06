package controllers.dashBoard.artist;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.DTO.LyricsEditRequestDTO;
import models.account.Artist;
import models.music.Lyrics;
import services.SessionManager;
import services.file.LyricsRequestManager;
import services.file.SongFileManager;
import utils.AlertUtil;

import java.io.File;
import java.util.List;

import static utils.FileUtil.readFile;
import static utils.FileUtil.sanitizeFileName;

public class ArtistManagementController {
    @FXML private Label welcomeLabel;
    @FXML private Button signOutButton;
    @FXML private AnchorPane listPane;
    @FXML private AnchorPane contentPane;
    @FXML private ListView<LyricsEditRequestDTO> requestListView;
    @FXML private VBox lyricsDetailsPane;
    @FXML private Label lyricsArtistNicknameLabel;
    @FXML private Label lyricsSongTitleLabel;
    @FXML private Label lyricsAlbumNameLabel;
    @FXML private Label lyricsRequesterLabel;
    @FXML private TextArea suggestedLyricsTextArea;
    @FXML private Label lyricsTimestampLabel;
    @FXML private Label lyricsStatusLabel;
    @FXML private HBox lyricsButtonsBox;
    @FXML private Button approveLyricsButton;
    @FXML private Button rejectLyricsButton;
    @FXML private TextArea originalLyricsTextArea;
    @FXML private TextArea allSuggestedEditsTextArea;

    private Artist artist;
    private final LyricsRequestManager lyricsRequestManager = new LyricsRequestManager();
    private ArtistMenuBarHandler menuBarHandler;

    @FXML
    private void initialize() {
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
            this.artist = (Artist) SessionManager.getInstance().getCurrentAccount();
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
        loadRequests();
        addRequestSelectionListener();
    }

    private void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }

    private void setArtistInfo() {
        checkComponent(welcomeLabel, "welcomeLabel");
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + artist.getNickName() + "!");
        }
    }

    private void loadRequests() {
        checkComponent(requestListView, "requestListView");
        if (requestListView == null) return;

        List<LyricsEditRequestDTO> requests = lyricsRequestManager.loadLyricsEditRequestsForArtist(artist.getNickName())
                .stream()
                .map(this::createLyricsEditRequest)
                .filter(request -> "Pending".equals(request.status()))
                .toList();
        requestListView.setItems(FXCollections.observableArrayList(requests));
    }

    private LyricsEditRequestDTO createLyricsEditRequest(String[] requestData) {
        LyricsEditRequestDTO request = new LyricsEditRequestDTO(
                requestData[4], requestData[0], requestData[1], requestData[2],
                requestData[3], requestData[6], requestData[5]);
        request.setLyricsRequestManager(lyricsRequestManager);
        return request;
    }

    private void addRequestSelectionListener() {
        checkComponent(requestListView, "requestListView");
        if (requestListView != null) {
            requestListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    displayRequestDetails(newValue);
                } else {
                    hideRequestDetails();
                }
            });
        }
    }

    private void displayRequestDetails(LyricsEditRequestDTO request) {
        updateLyricsDetailsPane(request);
        loadLyricsDetails(request);
    }

    private void updateLyricsDetailsPane(LyricsEditRequestDTO request) {
        checkComponent(lyricsDetailsPane, "lyricsDetailsPane");
        checkComponent(lyricsArtistNicknameLabel, "lyricsArtistNicknameLabel");
        checkComponent(lyricsSongTitleLabel, "lyricsSongTitleLabel");
        checkComponent(lyricsAlbumNameLabel, "lyricsAlbumNameLabel");
        checkComponent(lyricsRequesterLabel, "lyricsRequesterLabel");
        checkComponent(suggestedLyricsTextArea, "suggestedLyricsTextArea");
        checkComponent(lyricsTimestampLabel, "lyricsTimestampLabel");
        checkComponent(lyricsStatusLabel, "lyricsStatusLabel");
        checkComponent(lyricsButtonsBox, "lyricsButtonsBox");

        if (lyricsDetailsPane != null) lyricsDetailsPane.setVisible(true);
        if (lyricsArtistNicknameLabel != null) lyricsArtistNicknameLabel.setText("Artist: " + request.artistNickname());
        if (lyricsSongTitleLabel != null) lyricsSongTitleLabel.setText("Song Title: " + request.songTitle());
        if (lyricsAlbumNameLabel != null) lyricsAlbumNameLabel.setText("Album: " + (request.albumName() != null ? request.albumName() : "None"));
        if (lyricsRequesterLabel != null) lyricsRequesterLabel.setText("Requester: " + request.email());
        if (suggestedLyricsTextArea != null) suggestedLyricsTextArea.setText(request.suggestedLyrics());
        if (lyricsTimestampLabel != null) lyricsTimestampLabel.setText("Timestamp: " + request.timestamp());
        if (lyricsStatusLabel != null) lyricsStatusLabel.setText("Status: " + request.status());
        if (lyricsButtonsBox != null) lyricsButtonsBox.setVisible("Pending".equals(request.status()));
    }

    private void loadLyricsDetails(LyricsEditRequestDTO request) {
        checkComponent(originalLyricsTextArea, "originalLyricsTextArea");
        checkComponent(allSuggestedEditsTextArea, "allSuggestedEditsTextArea");

        String songDir = new SongFileManager().getSongDir(request.artistNickname(), request.songTitle(), request.albumName());
        File songFile = new File(songDir + sanitizeFileName(request.songTitle()) + ".txt");

        if (!songFile.exists()) {
            if (originalLyricsTextArea != null) originalLyricsTextArea.setText("Lyrics not found.");
            if (allSuggestedEditsTextArea != null) allSuggestedEditsTextArea.setText("No suggested edits available.");
            return;
        }

        List<String> songData = readFile(songFile.getPath());
        String lyricsText = songData.stream()
                .filter(line -> line.startsWith("Lyrics: "))
                .map(line -> line.substring("Lyrics: ".length()))
                .findFirst()
                .orElse("");
        Lyrics lyrics = new Lyrics(lyricsText);

        if (originalLyricsTextArea != null) originalLyricsTextArea.setText(lyrics.getOriginalLyrics());
        if (allSuggestedEditsTextArea != null) allSuggestedEditsTextArea.setText(String.join("\n", lyrics.getSuggestedEdits()));
    }

    private void hideRequestDetails() {
        checkComponent(lyricsDetailsPane, "lyricsDetailsPane");
        checkComponent(lyricsButtonsBox, "lyricsButtonsBox");
        checkComponent(originalLyricsTextArea, "originalLyricsTextArea");
        checkComponent(allSuggestedEditsTextArea, "allSuggestedEditsTextArea");

        if (lyricsDetailsPane != null) lyricsDetailsPane.setVisible(false);
        if (lyricsButtonsBox != null) lyricsButtonsBox.setVisible(false);
        if (originalLyricsTextArea != null) originalLyricsTextArea.clear();
        if (allSuggestedEditsTextArea != null) allSuggestedEditsTextArea.clear();
    }

    private void handleRequestOperation(LyricsEditRequestDTO request, String operation, String successMessage, String errorMessage) {
        if (request == null) return;

        try {
            if ("approve".equals(operation)) {
                lyricsRequestManager.approveLyricsEditRequest(
                        request.artistNickname(),
                        request.songTitle(),
                        request.timestamp(),
                        request.suggestedLyrics(),
                        request.albumName());
            } else if ("reject".equals(operation)) {
                lyricsRequestManager.rejectLyricsEditRequest(
                        request.artistNickname(),
                        request.songTitle(),
                        request.timestamp());
            }
            AlertUtil.showSuccess(successMessage);
            loadRequests();
            displayRequestDetails(request);
        } catch (Exception e) {
            AlertUtil.showError(errorMessage + ": " + e.getMessage());
        }
    }

    @FXML
    private void approveLyricsEditRequest() {
        LyricsEditRequestDTO selectedRequest = requestListView.getSelectionModel().getSelectedItem();
        handleRequestOperation(selectedRequest, "approve",
                "Lyrics edit request approved successfully!",
                "Failed to approve lyrics edit request");
    }

    @FXML
    private void rejectLyricsEditRequest() {
        LyricsEditRequestDTO selectedRequest = requestListView.getSelectionModel().getSelectedItem();
        handleRequestOperation(selectedRequest, "reject",
                "Lyrics edit request rejected successfully!",
                "Failed to reject lyrics edit request");
    }

    @FXML private void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML private void goToAddSong() { menuBarHandler.goToAddSong(); }
    @FXML private void goToDeleteSong() { menuBarHandler.goToDeleteSong(); }
    @FXML private void goToEditSong() { menuBarHandler.goToEditSong(); }
    @FXML private void goToCreateAlbum() { menuBarHandler.goToCreateAlbum(); }
    @FXML private void goToDeleteAlbum() { menuBarHandler.goToDeleteAlbum(); }
    @FXML private void goToEditAlbum() { menuBarHandler.goToEditAlbum(); }
    @FXML private void goToPendingRequests() { menuBarHandler.goToPendingRequests(); }
    @FXML private void goToApprovedRequests() { menuBarHandler.goToApprovedRequests(); }
    @FXML private void goToRejectedRequests() { menuBarHandler.goToRejectedRequests(); }
    @FXML private void signOut() { menuBarHandler.signOut(); }
}