package controllers.auth;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import services.AccountManager;
import services.file.AdminFileManager;
import services.file.ArtistFileManager;
import services.file.UserFileManager;
import utils.AlertUtil;
import utils.SceneUtil;

import java.util.Objects;

public class SignUpController {

    @FXML private TextField nicknameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private GridPane roleGrid;
    @FXML private Button signUpButton;
    @FXML private Label signInLabel;
    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton userBtn;

    private String selectedRole = "User";
    private final AdminFileManager adminFileManager;

    private final String SIGN_IN_FXML = "/FXML-files/signIn.fxml";
    private final String CSS_STYLE = "/CSS/styles.css";

    public SignUpController() {
        this.adminFileManager = new AdminFileManager();
        UserFileManager userFileManager = new UserFileManager();
        ArtistFileManager artistFileManager = new ArtistFileManager();
        artistFileManager.setUserFileManager(userFileManager);
    }

    @FXML
    private void initialize() {
        setupRoleSelection();
        userBtn.setSelected(true);
        signUpButton.setOnAction(event -> handleSignUp());
        signInLabel.setOnMouseClicked(event -> handleSignInRedirect());

        Platform.runLater(() -> {
            try {
                roleGrid.getScene().getStylesheets().add(
                        Objects.requireNonNull(getClass().getResource(CSS_STYLE)).toExternalForm()
                );
            } catch (Exception e) {
                System.err.println("Failed to load stylesheet: " + e.getMessage());
            }
        });
    }

    private void setupRoleSelection() {
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                ToggleButton selectedButton = (ToggleButton) newToggle;
                selectedRole = selectedButton.getText();
            } else {
                selectedRole = "User";
                userBtn.setSelected(true);
            }
        });
    }

    private void handleSignUp() {
        String nickname = nicknameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (nickname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
            return;
        }

        if (!AccountManager.validateEmail(email)) {
            AlertUtil.showWarning("Invalid email format!");
            return;
        }

        if (!isValidPassword(password)) {
            AlertUtil.showWarning("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
            return;
        }

        if (AccountManager.findByEmail(email) != null) {
            AlertUtil.showError("This email is already registered. Please use another email.");
            return;
        }

        try {
            if (selectedRole.equals("Artist")) {
                adminFileManager.saveArtistRequest(email, nickname, password);
                AlertUtil.showSuccess("Your artist registration request has been submitted for approval.");
                SceneUtil.changeScene(signUpButton, SIGN_IN_FXML);
            } else {
                AccountManager.registerUser(email, nickname, password, selectedRole);
                AlertUtil.showSuccess("Sign-up successful for: " + email);
                SceneUtil.changeScene(signUpButton, SIGN_IN_FXML);
            }
        } catch (Exception e) {
            AlertUtil.showError("Sign-up error: " + e.getMessage());
        }
    }

    private boolean isValidPassword(String password) {
        return AccountManager.validatePassword(password);
    }

    private void handleSignInRedirect() {
        SceneUtil.changeScene(signInLabel, SIGN_IN_FXML);
    }
}