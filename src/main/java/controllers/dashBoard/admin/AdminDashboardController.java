package controllers.dashBoard.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.account.Admin;
import models.ArtistRequest;
import models.LyricsEditRequest;
import services.file.LyricsRequestManager;
import utils.AlertUtil;
import utils.SceneUtil;

import java.util.Arrays;

public class AdminDashboardController {
    // FXML injected controls
    @FXML private ListView<Object> requestListView;
    @FXML private Button approveArtistButton;
    @FXML private Button rejectArtistButton;
    @FXML private Button signOutButton;
    @FXML private Label welcomeLabel;

    // Artist request controls
    @FXML private VBox artistDetailsPane;
    @FXML private Label artistEmailLabel;
    @FXML private Label artistNicknameLabel;
    @FXML private Label artistPasswordLabel;
    @FXML private Label artistStatusLabel;
    @FXML private Label artistTimestampLabel;
    @FXML private HBox artistButtonsBox;

    // Lyrics request controls
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

    private Admin admin;
    private ArtistRequest selectedArtistRequest;
    private LyricsEditRequest selectedLyricsEditRequest;
    private ObservableList<Object> requests;

    private final LyricsRequestManager lyricsRequestManager;

    public AdminDashboardController() {
        this.lyricsRequestManager = new LyricsRequestManager();
    }

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
                } else if (item instanceof ArtistRequest artistRequest) {
                    setText(String.format("Artist: %s (%s)",
                            artistRequest.getNickname(),
                            artistRequest.getStatus()));
                } else if (item instanceof LyricsEditRequest lyricsRequest) {
                    setText(String.format("Lyrics Edit: %s by %s (%s)",
                            lyricsRequest.getSongTitle(),
                            lyricsRequest.getArtistNickname(),
                            lyricsRequest.getStatus()));
                }
            }
        });
    }

    private void setupListViewListener() {
        requestListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection instanceof ArtistRequest) {
                displayArtistRequest((ArtistRequest) newSelection);
            } else if (newSelection instanceof LyricsEditRequest) {
                displayLyricsEditRequest((LyricsEditRequest) newSelection);
            } else {
                hideAllPanes();
            }
        });
    }

    public void setAdmin(Admin admin) {
        if (admin == null) {
            throw new IllegalArgumentException("Admin cannot be null");
        }
        this.admin = admin;
        // Setting the welcome label text with the admin account name
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + admin.getNickName() + "!");
        }
        showPendingArtistRequests();
    }

    private void displayArtistRequest(ArtistRequest request) {
        hideAllPanes();
        selectedArtistRequest = request;
        selectedLyricsEditRequest = null;

        artistEmailLabel.setText("Email: " + request.getEmail());
        artistNicknameLabel.setText("Nickname: " + request.getNickname());
        artistPasswordLabel.setText("Password: " + request.getPassword());
        artistStatusLabel.setText("Status: " + request.getStatus());
        artistTimestampLabel.setText("Timestamp: " + request.getTimestamp());

        artistDetailsPane.setVisible(true);
        artistButtonsBox.setVisible("Pending".equals(request.getStatus()));
    }

    private void displayLyricsEditRequest(LyricsEditRequest request) {
        hideAllPanes();
        selectedLyricsEditRequest = request;
        selectedArtistRequest = null;

        lyricsArtistNicknameLabel.setText("Artist: " + request.getArtistNickname());
        lyricsSongTitleLabel.setText("Song Title: " + request.getSongTitle());
        lyricsAlbumNameLabel.setText("Album: " + (request.getAlbumName() != null && !request.getAlbumName().equals("Single") ? request.getAlbumName() : "N/A"));
        lyricsRequesterLabel.setText("Requester: " + request.getEmail());
        suggestedLyricsTextArea.setText(request.getSuggestedLyrics());
        lyricsTimestampLabel.setText("Timestamp: " + request.getTimestamp());
        lyricsStatusLabel.setText("Status: " + request.getStatus());

        lyricsDetailsPane.setVisible(true);
        lyricsButtonsBox.setVisible("Pending".equals(request.getStatus()));
    }

    private void hideAllPanes() {
        artistDetailsPane.setVisible(false);
        lyricsDetailsPane.setVisible(false);
        artistButtonsBox.setVisible(false);
        lyricsButtonsBox.setVisible(false);
    }

    @FXML
    private void showPendingArtistRequests() {
        if (admin == null) {
            AlertUtil.showError("Admin is not set. Please ensure admin is initialized.");
            return;
        }
        loadArtistRequests("Pending");
    }

    @FXML
    private void showApprovedArtistRequests() {
        if (admin == null) {
            AlertUtil.showError("Admin is not set. Please ensure admin is initialized.");
            return;
        }
        loadArtistRequests("Approved");
    }

    @FXML
    private void showRejectedArtistRequests() {
        if (admin == null) {
            AlertUtil.showError("Admin is not set. Please ensure admin is initialized.");
            return;
        }
        loadArtistRequests("Rejected");
    }

    private void loadArtistRequests(String status) {
        try {
            requests.clear();
            hideAllPanes();

            String[][] requestsData = switch (status) {
                case "Pending" -> admin.getPendingArtistRequests();
                case "Approved" -> admin.getApprovedArtistRequests();
                case "Rejected" -> admin.getRejectedArtistRequests();
                default -> new String[0][];
            };

            for (String[] requestData : requestsData) {
                if (requestData.length < 5) {
                    System.err.println("Invalid artist request data: " + Arrays.toString(requestData));
                    continue;
                }
                requests.add(new ArtistRequest(
                        requestData[0],  // email
                        requestData[1],  // nickname
                        requestData[2],  // password
                        status,
                        requestData[4]   // timestamp
                ));
            }
        } catch (Exception e) {
            AlertUtil.showError("Error loading artist requests: " + e.getMessage());
        }
    }

    @FXML
    private void showPendingLyricsRequests() {
        loadLyricsRequests("Pending");
    }

    @FXML
    private void showApprovedLyricsRequests() {
        loadLyricsRequests("Approved");
    }

    @FXML
    private void showRejectedLyricsRequests() {
        loadLyricsRequests("Rejected");
    }

    private void loadLyricsRequests(String status) {
        try {
            requests.clear();
            hideAllPanes();

            var allRequests = lyricsRequestManager.loadAllLyricsEditRequests();
            for (String[] requestData : allRequests) {
                if (requestData.length < 7) {
                    System.err.println("Invalid lyrics edit request data: " + Arrays.toString(requestData));
                    continue;
                }

                if (requestData[5].equals(status)) {
                    LyricsEditRequest lyricsRequest = getLyricsEditRequest(requestData);
                    requests.add(lyricsRequest);
                }
            }
        } catch (Exception e) {
            AlertUtil.showError("Error loading lyrics edit requests: " + e.getMessage());
        }
    }

    private LyricsEditRequest getLyricsEditRequest(String[] requestData) {
        LyricsEditRequest lyricsRequest = new LyricsEditRequest(
                requestData[4],  // email (requester)
                requestData[0],  // artistNickname
                requestData[1],  // songTitle
                requestData[2],  // albumName
                requestData[3],  // suggestedLyrics
                requestData[6],  // timestamp
                requestData[5]   // status
        );
        lyricsRequest.setLyricsRequestManager(lyricsRequestManager);
        return lyricsRequest;
    }

    @FXML
    private void approveArtistRequest() {
        if (selectedArtistRequest != null) {
            try {
                admin.approveArtist(selectedArtistRequest.getEmail(), selectedArtistRequest.getNickname());
                AlertUtil.showSuccess("Artist request approved successfully");
                showPendingArtistRequests();
                selectedArtistRequest = null;
            } catch (Exception e) {
                AlertUtil.showError("Error approving artist request: " + e.getMessage());
            }
        }
    }

    @FXML
    private void rejectArtistRequest() {
        if (selectedArtistRequest != null) {
            try {
                admin.rejectArtist(selectedArtistRequest.getEmail());
                AlertUtil.showSuccess("Artist request rejected successfully");
                showPendingArtistRequests();
                selectedArtistRequest = null;
            } catch (Exception e) {
                AlertUtil.showError("Error rejecting artist request: " + e.getMessage());
            }
        }
    }

    @FXML
    private void approveLyricsEditRequest() {
        if (selectedLyricsEditRequest != null) {
            try {
                lyricsRequestManager.approveLyricsEditRequest(
                        selectedLyricsEditRequest.getArtistNickname(),
                        selectedLyricsEditRequest.getSongTitle(),
                        selectedLyricsEditRequest.getTimestamp(),
                        selectedLyricsEditRequest.getSuggestedLyrics(),
                        selectedLyricsEditRequest.getAlbumName()
                );
                AlertUtil.showSuccess("Lyrics edit request approved successfully");
                showPendingLyricsRequests();
                selectedLyricsEditRequest = null;
            } catch (Exception e) {
                AlertUtil.showError("Error approving lyrics edit request: " + e.getMessage());
            }
        }
    }

    @FXML
    private void rejectLyricsEditRequest() {
        if (selectedLyricsEditRequest != null) {
            try {
                lyricsRequestManager.rejectLyricsEditRequest(
                        selectedLyricsEditRequest.getArtistNickname(),
                        selectedLyricsEditRequest.getSongTitle(),
                        selectedLyricsEditRequest.getTimestamp()
                );
                AlertUtil.showSuccess("Lyrics edit request rejected successfully");
                showPendingLyricsRequests();
                selectedLyricsEditRequest = null;
            } catch (Exception e) {
                AlertUtil.showError("Error rejecting lyrics edit request: " + e.getMessage());
            }
        }
    }

    @FXML
    private void signOut() {
        services.SessionManager.getInstance().clearSession();
        SceneUtil.changeScene(signOutButton, "/FXML-files/signIn.fxml");
    }
}