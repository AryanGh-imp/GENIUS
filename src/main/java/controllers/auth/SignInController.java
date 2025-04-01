package controllers.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import models.account.Admin;
import services.file.UserFileManager;
import services.file.ArtistFileManager;
import utils.AlertUtil;
import utils.SceneUtil;

public class SignInController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label signUpLabel;

    private final UserFileManager userManager = new UserFileManager();
    private final ArtistFileManager artistManager = new ArtistFileManager();

    @FXML
    private void initialize() {
        loginButton.setOnAction(event -> handleLogin());
        signUpLabel.setOnMouseClicked(event -> handleSignUpRedirect());
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
            return;
        }

        // Check admin login
        if (email.equals(Admin.getAdminEmail()) && password.equals(Admin.getAdminPassword())) {
            AlertUtil.showSuccess("Login successful! Welcome, Admin");
            SceneUtil.changeScene(loginButton, "/fxml/adminDashboard.fxml");
            return;
        }

        // Check user account
        String userNickName = userManager.findNickNameByEmail(email);
        if (userNickName != null) {
            String[] userData = userManager.readUser(email, userNickName);
            if (userData != null && userData[2].equals(password)) {
                String nickname = userData[1];
                AlertUtil.showSuccess("Login successful! Welcome, " + nickname);
                SceneUtil.changeScene(loginButton, "/fxml/dashboard.fxml");
                return;
            }
        }

        // Check artist account
        String artistNickName = artistManager.findNickNameByEmail(email);
        if (artistNickName != null) {
            String[] artistData = artistManager.readArtist(email, artistNickName);
            if (artistData != null && artistData[2].equals(password)) {
                String nickname = artistData[1];
                AlertUtil.showSuccess("Login successful! Welcome, " + nickname);
                SceneUtil.changeScene(loginButton, "/fxml/artistDashboard.fxml");
                return;
            }
        }

        // If no account is found ->
        AlertUtil.showError("Invalid email or password.");
    }

    @FXML
    private void handleSignUpRedirect() {
        SceneUtil.changeScene(signUpLabel, "/fxml/signUp.fxml");
    }
}