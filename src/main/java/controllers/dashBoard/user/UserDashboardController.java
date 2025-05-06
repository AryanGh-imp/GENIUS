package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.account.Account;
import models.account.Artist;
import models.account.User;
import services.SessionManager;
import services.file.ArtistFileManager;
import utils.AlertUtil;
import utils.FileUtil;
import utils.SceneUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class UserDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label usernameLabel;
    @FXML private Label joinedDateLabel;
    @FXML private ListView<String> followingArtistsListView;
    @FXML private ImageView profileImageView;
    @FXML private Button changeProfileImageButton;
    @FXML private Button signOutButton;

    private UserMenuBarHandler menuBarHandler;
    private final ArtistFileManager artistFileManager = new ArtistFileManager();

    @FXML
    public void initialize() {
        menuBarHandler = new UserMenuBarHandler(signOutButton);
        loadUserProfile();
        loadFollowingArtists();
        loadProfileImage();
        setupFollowingArtistsListView();
    }

    private void loadUserProfile() {
        UserInfo userInfo = getCurrentUserInfo();
        welcomeLabel.setText("Welcome, " + userInfo.username + "!");
        usernameLabel.setText("Username: " + userInfo.username);

        File userProfileFile = new File(FileUtil.DATA_DIR + "users/" + userInfo.username + "/profile.txt");
        List<String> profileData = loadFileData(userProfileFile.getPath());
        String joinedDate = profileData.stream()
                .filter(line -> line.startsWith("Joined Date: "))
                .findFirst()
                .map(line -> line.substring("Joined Date: ".length()))
                .orElse(String.valueOf(LocalDate.now()));
        joinedDateLabel.setText("Joined Date: " + joinedDate);
    }

    private void loadFollowingArtists() {
        UserInfo userInfo = getCurrentUserInfo();
        Account currentAccount = SessionManager.getInstance().getCurrentAccount();
        if (currentAccount == null) {
            AlertUtil.showError("User not logged in.");
            return;
        }

        if (!(currentAccount instanceof User currentUser)) {
            AlertUtil.showError("This dashboard is only for users, not " + currentAccount.getRole());
            return;
        }

        List<String> artistNames = loadFileData(new File(FileUtil.DATA_DIR + "users/" + userInfo.username + "/following.txt").getPath())
                .stream()
                .filter(line -> !line.trim().isEmpty())
                .map(line -> {
                    try {
                        currentUser.loadFollowingArtistsFromFile(artistFileManager, SessionManager.getInstance().getUserFileManager());
                        return currentUser.getFollowingArtists().stream()
                                .filter(artist -> artist.getNickName().equals(line.trim()))
                                .findFirst()
                                .map(Artist::getNickName)
                                .orElse(line.trim());
                    } catch (Exception e) {
                        AlertUtil.showError("Failed to load artist: " + e.getMessage());
                        return line.trim();
                    }
                })
                .collect(Collectors.toList());

        followingArtistsListView.getItems().setAll(artistNames.isEmpty() ? List.of("No artists followed.") : artistNames);
    }

    private void loadProfileImage() {
        UserInfo userInfo = getCurrentUserInfo();
        File profileImageFile = new File(FileUtil.DATA_DIR + "users/" + userInfo.username + "/profile_image.png");
        loadOrUpdateImage(profileImageFile);
    }

    private void setupFollowingArtistsListView() {
        followingArtistsListView.setOnMouseClicked(event -> {
            String selectedArtist = followingArtistsListView.getSelectionModel().getSelectedItem();
            if (selectedArtist != null && !selectedArtist.equals("No artists followed.")) {
                SessionManager.getInstance().setSelectedArtist(selectedArtist);
                SceneUtil.changeScene(followingArtistsListView, "/FXML-files/user/ArtistProfile.fxml");
            }
        });
    }

    @FXML
    public void changeProfileImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(changeProfileImageButton.getScene().getWindow());

        if (selectedFile != null) {
            UserInfo userInfo = getCurrentUserInfo();
            File destinationDir = new File(FileUtil.DATA_DIR + "users/" + userInfo.username);
            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }

            File destinationFile = new File(destinationDir, "profile_image.png");
            try {
                Files.copy(selectedFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                loadOrUpdateImage(destinationFile);
            } catch (IOException e) {
                AlertUtil.showError("Failed to change profile image: " + e.getMessage());
            }
        }
    }

    private void loadOrUpdateImage(File imageFile) {
        if (imageFile.exists()) {
            profileImageView.setImage(new Image(imageFile.toURI().toString()));
        }
    }

    private List<String> loadFileData(String filePath) {
        try {
            return FileUtil.readFile(filePath);
        } catch (Exception e) {
            System.err.println("Error loading file data: " + e.getMessage());
            return List.of();
        }
    }

    private UserInfo getCurrentUserInfo() {
        String username = SessionManager.getInstance().getCurrentUsername();
        return new UserInfo(username);
    }

    private static class UserInfo {
        private final String username;

        public UserInfo(String username) {
            this.username = username;
        }
    }

    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToSearch() { menuBarHandler.goToSearch(); }
    @FXML public void goToCharts() { menuBarHandler.goToCharts(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}