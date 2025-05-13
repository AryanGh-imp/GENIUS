package controllers.dashBoard.artist;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.DTO.LyricsEditRequestDTO;
import models.music.Lyrics;
import services.file.FileManager;
import services.file.LyricsRequestManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ArtistManagementController extends BaseArtistController {

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

    private final LyricsRequestManager lyricsRequestManager = new LyricsRequestManager();
    private final FileManager fileManager = new LyricsRequestManager();
    private ObservableList<LyricsEditRequestDTO> requests;
    private LyricsEditRequestDTO selectedLyricsEditRequestDTO;

    @FXML
    private void initialize() {
        if (!validateSession(signOutButton)) return;
        initializeUI();
    }

    private void initializeUI() {
        setArtistInfo(welcomeLabel);
        requests = FXCollections.observableArrayList();
        requestListView.setItems(requests);
        setupListView();
        setupListViewListener();
        showRequests("Pending");
    }

    private void setupListView() {
        checkComponent(requestListView, "requestListView");
        requestListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(LyricsEditRequestDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Lyrics Edit: %s by %s (%s)",
                            item.songTitle(),
                            item.artistNickname(),
                            item.status()));
                }
            }
        });
    }

    private void setupListViewListener() {
        checkComponent(requestListView, "requestListView");
        if (requestListView != null) {
            requestListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    displayRequestDetails(newSelection);
                } else {
                    hideRequestDetails();
                }
            });
        }
    }

    private void displayRequestDetails(LyricsEditRequestDTO request) {
        selectedLyricsEditRequestDTO = request;
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
        if (lyricsAlbumNameLabel != null) lyricsAlbumNameLabel.setText("Album: " + (request.albumName() != null && !request.albumName().equals("Single") ? request.albumName() : "N/A"));
        if (lyricsRequesterLabel != null) lyricsRequesterLabel.setText("Requester: " + request.email());
        if (suggestedLyricsTextArea != null) suggestedLyricsTextArea.setText(request.suggestedLyrics());
        if (lyricsTimestampLabel != null) lyricsTimestampLabel.setText("Timestamp: " + request.timestamp());
        if (lyricsStatusLabel != null) lyricsStatusLabel.setText("Status: " + request.status());
        if (lyricsButtonsBox != null) lyricsButtonsBox.setVisible("Pending".equals(request.status()));
    }

    private void loadLyricsDetails(LyricsEditRequestDTO request) {
        checkComponent(originalLyricsTextArea, "originalLyricsTextArea");

        SongFileManager songFileManager = new SongFileManager();
        String songDir = songFileManager.getSongDir(request.artistNickname(), request.songTitle(), request.albumName());
        File songFile = new File(songDir + FileUtil.sanitizeFileName(request.songTitle()) + ".txt");
        File lyricsFile = new File(songDir + FileUtil.sanitizeFileName(request.songTitle()) + "_lyrics.txt");

        System.out.println("Checking song file: " + songFile.getPath());
        System.out.println("Checking lyrics file: " + lyricsFile.getPath());

        if (!songFile.exists()) {
            if (originalLyricsTextArea != null) originalLyricsTextArea.setText("Song file not found.");
            return;
        }

        List<String> songData = FileUtil.readFile(songFile.getPath());
        System.out.println("Song data: " + songData);

        String originalLyricsFromFile = lyricsFile.exists() ? songFileManager.loadLyrics(songFile.getPath()) : null;

        if (originalLyricsFromFile == null || originalLyricsFromFile.trim().isEmpty()) {
            System.out.println("No original lyrics found for song: " + request.songTitle());
            if (originalLyricsTextArea != null) originalLyricsTextArea.setText("No original lyrics found.");
        } else {
            try {
                Lyrics lyrics = new Lyrics(originalLyricsFromFile);
                if (originalLyricsTextArea != null) originalLyricsTextArea.setText(lyrics.getOriginalLyrics());
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to create Lyrics object: " + e.getMessage());
                if (originalLyricsTextArea != null) originalLyricsTextArea.setText("Error loading lyrics.");
            }
        }
    }

    private void hideRequestDetails() {
        checkComponent(lyricsDetailsPane, "lyricsDetailsPane");
        checkComponent(lyricsButtonsBox, "lyricsButtonsBox");
        checkComponent(originalLyricsTextArea, "originalLyricsTextArea");

        if (lyricsDetailsPane != null) lyricsDetailsPane.setVisible(false);
        if (lyricsButtonsBox != null) lyricsButtonsBox.setVisible(false);
        if (originalLyricsTextArea != null) originalLyricsTextArea.clear();
    }

    private void showRequests(String status) {
        if (artist == null) {
            AlertUtil.showError("Artist is not set. Please ensure artist is initialized.");
            return;
        }

        requests.clear();
        hideRequestDetails();

        try {
            String[][] requestsData = lyricsRequestManager.loadLyricsEditRequestsForArtist(artist.getNickName())
                    .toArray(new String[0][]);

            System.out.println("Loaded " + requestsData.length + " lyrics edit requests for status: " + status);
            for (String[] requestData : requestsData) {
                if (requestData.length >= 7) {
                    LyricsEditRequestDTO lyricsRequest = createLyricsEditRequest(requestData);
                    if (lyricsRequest != null && status.equals(lyricsRequest.status())) {
                        requests.add(lyricsRequest);
                        System.out.println("Added LyricsEditRequestDTO: " + lyricsRequest);
                    } else {
                        System.err.println("Failed to create LyricsEditRequestDTO or status mismatch: " + Arrays.toString(requestData));
                    }
                } else {
                    System.err.println("Invalid request data: " + Arrays.toString(requestData));
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading requests: " + e.getMessage());
            AlertUtil.showError("Error loading requests: " + e.getMessage());
        }
        System.out.println("Total requests added to ListView: " + requests.size());
    }

    @FXML
    private void handleMenuAction(ActionEvent event) {
        MenuItem menuItem = (MenuItem) event.getSource();
        String menuText = menuItem.getText();
        String status = switch (menuText) {
            case "Pending Requests" -> "Pending";
            case "Approved Requests" -> "Approved";
            case "Rejected Requests" -> "Rejected";
            default -> "Pending";
        };
        showRequests(status);
    }

    private LyricsEditRequestDTO createLyricsEditRequest(String[] requestData) {
        // [Artist: aryan2, Song: a1s1, Album: al11, SuggestedLyrics: a1s1 new, Requester: aryan, Status: Approved, Timestamp: 2025-05-13 11:04:49]
        if (requestData.length < 7) {
            System.err.println("Insufficient data in requestData: " + Arrays.toString(requestData));
            return null;
        }

        String artistNickname = extractField(requestData, "Artist: ");
        String songTitle = extractField(requestData, "Song: ");
        String albumName = extractField(requestData, "Album: ");
        String suggestedLyrics = extractField(requestData, "SuggestedLyrics: ");
        String requester = extractField(requestData, "Requester: ");
        String status = extractField(requestData, "Status: ");
        String timestamp = extractField(requestData, "Timestamp: ");

        System.out.println("Extracted Lyrics Edit Request - Artist: " + artistNickname +
                ", Song: " + songTitle + ", Album: " + albumName + ", SuggestedLyrics: " + suggestedLyrics +
                ", Requester: " + requester + ", Status: " + status + ", Timestamp: " + timestamp);

        String email = requester;
        if (!email.contains("@")) {
            System.out.println("Requester '" + requester + "' is not an email, attempting to find email by nickname...");
            email = fileManager.findEmailByNickName(requester, "user");
            if (email == null) {
                email = fileManager.findEmailByNickName(requester, "artist");
            }
            if (email == null) {
                System.err.println("Could not find email for requester nickname: " + requester);
                return null;
            }
            System.out.println("Found email '" + email + "' for requester nickname '" + requester + "'");
        }

        if (email == null || artistNickname == null || songTitle == null || suggestedLyrics == null ||
                timestamp == null || status == null) {
            System.err.println("One or more fields are null in request data: " + Arrays.toString(requestData));
            return null;
        }

        String normalizedTimestamp = timestamp.replace("T", " ").replaceAll("[Z]", "");
        if (normalizedTimestamp.length() > 19) {
            normalizedTimestamp = normalizedTimestamp.substring(0, 19);
        }

        LyricsEditRequestDTO request = new LyricsEditRequestDTO(
                email, artistNickname, songTitle, albumName, suggestedLyrics, normalizedTimestamp, status
        );
        request.setLyricsRequestManager(lyricsRequestManager);
        return request;
    }

    private String extractField(String[] data, String prefix) {
        for (String line : data) {
            if (line.startsWith(prefix)) {
                System.out.println("Extracting field with prefix: '" + prefix + "' from data: " + Arrays.toString(data));
                String value = line.substring(prefix.length()).trim();
                System.out.println("Extracted " + prefix + " as: '" + value + "' from line: '" + line + "'");
                return value;
            }
        }
        System.err.println("Field with prefix '" + prefix + "' not found in data: " + Arrays.toString(data));
        return null;
    }

    private void executeRequestOperation(String successMessage, Runnable operation) {
        try {
            operation.run();
            AlertUtil.showSuccess(successMessage);
            showRequests("Pending");
            selectedLyricsEditRequestDTO = null;
        } catch (Exception e) {
            System.err.println("Error executing operation: " + e.getMessage());
            AlertUtil.showError("Error executing operation: " + e.getMessage());
        }
    }

    @FXML
    private void approveLyricsEditRequest() {
        if (selectedLyricsEditRequestDTO != null) {
            executeRequestOperation("Lyrics edit request approved successfully", () ->
                    lyricsRequestManager.approveLyricsEditRequest(
                            selectedLyricsEditRequestDTO.artistNickname(),
                            selectedLyricsEditRequestDTO.songTitle(),
                            selectedLyricsEditRequestDTO.timestamp(),
                            selectedLyricsEditRequestDTO.suggestedLyrics(),
                            selectedLyricsEditRequestDTO.albumName()
                    ));
        }
    }

    @FXML
    private void rejectLyricsEditRequest() {
        if (selectedLyricsEditRequestDTO != null) {
            executeRequestOperation("Lyrics edit request rejected successfully", () ->
                    lyricsRequestManager.rejectLyricsEditRequest(
                            selectedLyricsEditRequestDTO.artistNickname(),
                            selectedLyricsEditRequestDTO.songTitle(),
                            selectedLyricsEditRequestDTO.timestamp()
                    ));
        }
    }

    @FXML public void goToProfile() { super.goToProfile(); }
    @FXML public void goToAddSong() { super.goToAddSong(); }
    @FXML public void goToDeleteSong() { super.goToDeleteSong(); }
    @FXML public void goToEditSong() { super.goToEditSong(); }
    @FXML public void goToCreateAlbum() { super.goToCreateAlbum(); }
    @FXML public void goToDeleteAlbum() { super.goToDeleteAlbum(); }
    @FXML public void goToEditAlbum() { super.goToEditAlbum(); }
    @FXML public void goToPendingRequests() { showRequests("Pending"); }
    @FXML public void goToApprovedRequests() { showRequests("Approved"); }
    @FXML public void goToRejectedRequests() { showRequests("Rejected"); }
    @FXML public void signOut() { super.signOut(); }
}