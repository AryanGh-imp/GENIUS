package controllers.dashBoard.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.DTO.ArtistRequestDTO;
import models.DTO.LyricsEditRequestDTO;
import models.account.Admin;
import models.music.Lyrics;
import services.file.FileManager;
import services.file.LyricsRequestManager;
import services.file.SongFileManager;
import utils.AlertUtil;
import utils.FileUtil;
import utils.SceneUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AdminDashboardController {
    @FXML private ListView<Object> requestListView;
    @FXML private Button approveArtistButton;
    @FXML private Button rejectArtistButton;
    @FXML private Button signOutButton;
    @FXML private Label welcomeLabel;

    @FXML private VBox artistDetailsPane;
    @FXML private Label artistEmailLabel;
    @FXML private Label artistNicknameLabel;
    @FXML private Label artistPasswordLabel;
    @FXML private Label artistStatusLabel;
    @FXML private Label artistTimestampLabel;
    @FXML private HBox artistButtonsBox;

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

    private Admin admin;
    private ArtistRequestDTO selectedArtistRequestDTO;
    private LyricsEditRequestDTO selectedLyricsEditRequestDTO;
    private ObservableList<Object> requests;

    private final LyricsRequestManager lyricsRequestManager = new LyricsRequestManager();
    private final FileManager fileManager = new LyricsRequestManager();

    @FXML
    private void initialize() {
        requests = FXCollections.observableArrayList();
        requestListView.setItems(requests);
        setupListView();
        setupListViewListener();
    }

    private void setupListView() {
        requestListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof ArtistRequestDTO artistRequestDTO) {
                    setText(artistRequestDTO.toString());
                } else if (item instanceof LyricsEditRequestDTO lyricsRequest) {
                    setText(String.format("Lyrics Edit: %s by %s (%s)",
                            lyricsRequest.songTitle(),
                            lyricsRequest.artistNickname(),
                            lyricsRequest.status()));
                }
            }
        });
    }

    private void setupListViewListener() {
        requestListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            displayRequestDetails(newSelection);
        });
    }

    public void setAdmin(Admin admin) {
        if (admin == null) {
            throw new IllegalArgumentException("Admin cannot be null");
        }
        this.admin = admin;
        welcomeLabel.setText("Welcome, " + admin.getNickName() + "!");
        showRequests(true, "Pending");
    }

    private void displayRequestDetails(Object request) {
        hideAllPanes();
        if (request instanceof ArtistRequestDTO artistRequest) {
            selectedArtistRequestDTO = artistRequest;
            selectedLyricsEditRequestDTO = null;
            artistEmailLabel.setText("Email: " + artistRequest.email());
            artistNicknameLabel.setText("Nickname: " + artistRequest.nickname());
            artistPasswordLabel.setText("Password: " + artistRequest.password());
            artistStatusLabel.setText("Status: " + artistRequest.status());
            artistTimestampLabel.setText("Timestamp: " + artistRequest.timestamp());
            artistDetailsPane.setVisible(true);
            artistButtonsBox.setVisible("Pending".equals(artistRequest.status()));
        } else if (request instanceof LyricsEditRequestDTO lyricsRequest) {
            selectedLyricsEditRequestDTO = lyricsRequest;
            selectedArtistRequestDTO = null;
            lyricsArtistNicknameLabel.setText("Artist: " + lyricsRequest.artistNickname());
            lyricsSongTitleLabel.setText("Song Title: " + lyricsRequest.songTitle());
            lyricsAlbumNameLabel.setText("Album: " + (lyricsRequest.albumName() != null && !lyricsRequest.albumName().equals("Single") ? lyricsRequest.albumName() : "N/A"));
            lyricsRequesterLabel.setText("Requester: " + lyricsRequest.email());
            suggestedLyricsTextArea.setText(lyricsRequest.suggestedLyrics());
            lyricsTimestampLabel.setText("Timestamp: " + lyricsRequest.timestamp());
            lyricsStatusLabel.setText("Status: " + lyricsRequest.status());
            loadLyricsDetails(lyricsRequest);
            lyricsDetailsPane.setVisible(true);
            lyricsButtonsBox.setVisible("Pending".equals(lyricsRequest.status()));
        }
    }

    private void loadLyricsDetails(LyricsEditRequestDTO request) {
        if (originalLyricsTextArea != null) {
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
    }

    private void hideAllPanes() {
        artistDetailsPane.setVisible(false);
        lyricsDetailsPane.setVisible(false);
        artistButtonsBox.setVisible(false);
        lyricsButtonsBox.setVisible(false);
        originalLyricsTextArea.clear();
    }

    private void showRequests(boolean isArtistRequest, String status) {
        if (admin == null) {
            AlertUtil.showError("Admin is not set. Please ensure admin is initialized.");
            return;
        }

        requests.clear();
        hideAllPanes();

        try {
            String[][] requestsData = isArtistRequest
                    ? switch (status) {
                case "Pending" -> admin.getPendingArtistRequests();
                case "Approved" -> admin.getApprovedArtistRequests();
                case "Rejected" -> admin.getRejectedArtistRequests();
                default -> new String[0][];
            }
                    : lyricsRequestManager.getLyricsEditRequests(status);

            System.out.println("Loaded " + requestsData.length + " requests for type: " + (isArtistRequest ? "Artist" : "Lyrics") + ", status: " + status);
            for (String[] requestData : requestsData) {
                if (isArtistRequest && requestData.length >= 5) {
                    requests.add(new ArtistRequestDTO(requestData[0], requestData[1], requestData[2], status, requestData[4]));
                    System.out.println("Added ArtistRequestDTO: " + Arrays.toString(requestData));
                } else if (!isArtistRequest && requestData.length >= 7) {
                    LyricsEditRequestDTO lyricsRequest = createLyricsEditRequest(requestData);
                    if (lyricsRequest != null) {
                        requests.add(lyricsRequest);
                        System.out.println("Added LyricsEditRequestDTO: " + lyricsRequest);
                    } else {
                        System.err.println("Failed to create LyricsEditRequestDTO from data: " + Arrays.toString(requestData));
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
        boolean isArtistRequest = menuItem.getParentMenu().getText().contains("Artist");
        String status = switch (menuText) {
            case "Pending Requests" -> "Pending";
            case "Approved Requests" -> "Approved";
            case "Rejected Requests" -> "Rejected";
            default -> "Pending";
        };
        showRequests(isArtistRequest, status);
    }

    private LyricsEditRequestDTO createLyricsEditRequest(String[] requestData) {
        // [Requester, Artist, Song, Album, SuggestedLyrics, Status, Timestamp]
        if (requestData.length < 7) {
            System.err.println("Insufficient data in requestData: " + Arrays.toString(requestData));
            return null;
        }

        String requester = requestData[0];
        String artistNickname = requestData[1];
        String songTitle = requestData[2];
        String albumName = requestData[3];
        String suggestedLyrics = requestData[4];
        String status = requestData[5];
        String timestamp = requestData[6];

        System.out.println("Extracted Lyrics Edit Request - Requester: " + requester + ", Artist: " + artistNickname +
                ", Song: " + songTitle + ", Album: " + albumName + ", SuggestedLyrics: " + suggestedLyrics +
                ", Timestamp: " + timestamp + ", Status: " + status);

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

        if (artistNickname == null || songTitle == null || suggestedLyrics == null || timestamp == null || status == null) {
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

    private void executeRequestOperation(String successMessage, Runnable operation) {
        try {
            operation.run();
            AlertUtil.showSuccess(successMessage);
            if (selectedArtistRequestDTO != null) showRequests(true, "Pending");
            else if (selectedLyricsEditRequestDTO != null) showRequests(false, "Pending");
            selectedArtistRequestDTO = null;
            selectedLyricsEditRequestDTO = null;
        } catch (Exception e) {
            System.err.println("Error executing operation: " + e.getMessage());
            AlertUtil.showError("Error executing operation: " + e.getMessage());
        }
    }

    @FXML
    private void approveArtistRequest() {
        if (selectedArtistRequestDTO != null) {
            executeRequestOperation("Artist request approved successfully", () -> admin.approveArtist(selectedArtistRequestDTO.email(), selectedArtistRequestDTO.nickname()));
        }
    }

    @FXML
    private void rejectArtistRequest() {
        if (selectedArtistRequestDTO != null) {
            executeRequestOperation("Artist request rejected successfully", () -> admin.rejectArtist(selectedArtistRequestDTO.email(), selectedArtistRequestDTO.nickname()));
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

    @FXML
    private void signOut() {
        services.SessionManager.getInstance().clearSession();
        AlertUtil.showSuccess("You have been signed out.");
        SceneUtil.changeScene(signOutButton, "/FXML-files/signIn.fxml");
    }
}