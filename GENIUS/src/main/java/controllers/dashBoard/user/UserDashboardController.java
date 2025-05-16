package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.account.Account;
import models.account.Artist;
import models.account.User;
import services.SessionManager;
import services.file.ArtistFileManager;
import utils.AlertUtil;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class UserDashboardController extends BaseUserController {

    @FXML private Label usernameLabel;
    @FXML private Label joinedDateLabel;
    @FXML private ListView<String> followingArtistsListView;
    @FXML private ImageView profileImageView;
    @FXML private Button changeProfileImageButton;

    private final ArtistFileManager artistFileManager = new ArtistFileManager();
    String username = SessionManager.getInstance().getCurrentUsername();
    String email = SessionManager.getInstance().getCurrentEmail();

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        loadUserProfile();
        loadFollowingArtists();
        loadProfileImage();
        setupFollowingArtistsListView();
    }

    private void loadUserProfile() {
        checkComponent(usernameLabel, "usernameLabel");
        if (usernameLabel != null) usernameLabel.setText("Username: " + username);

        File userProfileFile = new File(FileUtil.DATA_DIR + "users/" + username + "/" + username + "-" + email + ".txt");
        List<String> profileData = loadFileData(userProfileFile.getPath());
        String joinedDate = profileData.stream()
                .filter(line -> line.startsWith("Joined Date: "))
                .findFirst()
                .map(line -> line.substring("Joined Date: ".length()))
                .orElse(String.valueOf(LocalDate.now()));
        checkComponent(joinedDateLabel, "joinedDateLabel");
        if (joinedDateLabel != null) joinedDateLabel.setText("Joined Date: " + joinedDate);
    }

    private void loadFollowingArtists() {
        Account currentAccount = SessionManager.getInstance().getCurrentAccount();
        if (currentAccount == null) {
            AlertUtil.showError("User not logged in.");
            return;
        }

        if (!(currentAccount instanceof User currentUser)) {
            AlertUtil.showError("This dashboard is only for users, not " + currentAccount.getRole());
            return;
        }

        checkComponent(followingArtistsListView, "followingArtistsListView");
        if (followingArtistsListView == null) return;

        try {
            // Loading followed artists
            currentUser.loadFollowingArtistsFromFile(artistFileManager, SessionManager.getInstance().getUserFileManager());
            List<String> artistNames = currentUser.getFollowingArtists().stream()
                    .map(Artist::getNickName)
                    .collect(Collectors.toList());

            // Adjust the list and ensure it is empty if necessary
            followingArtistsListView.getItems().clear(); // Empty the list first.
            if (!artistNames.isEmpty()) {
                followingArtistsListView.getItems().addAll(artistNames);
            } else {
                followingArtistsListView.getItems().add("No artists followed.");
            }
        } catch (Exception e) {
            AlertUtil.showError("Failed to load following artists: " + e.getMessage());
            followingArtistsListView.getItems().clear(); // If there is an error, clear the list.
            followingArtistsListView.getItems().add("No artists followed.");
        }
    }

    private void loadProfileImage() {
        loadImage(profileImageView, FileUtil.DATA_DIR + "users/" + username + "/user_icon.png");
    }

    private void setupFollowingArtistsListView() {
        setupListView(followingArtistsListView, "/FXML-files/user/ArtistProfile.fxml", "artist");
    }

    @FXML
    public void changeProfileImage() {
        checkComponent(changeProfileImageButton, "changeProfileImageButton");
        if (changeProfileImageButton == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(changeProfileImageButton.getScene().getWindow());

        if (selectedFile != null) {
            File destinationDir = new File(FileUtil.DATA_DIR + "users/" + username);
            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }

            File destinationFile = new File(destinationDir, "user_icon.png");
            try {
                Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                loadImage(profileImageView, destinationFile.getPath());
            } catch (IOException e) {
                AlertUtil.showError("Failed to change profile image: " + e.getMessage());
            }
        }
    }
}