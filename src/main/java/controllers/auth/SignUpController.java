package controllers.auth;

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

    @FXML
    private TextField nicknameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private GridPane roleGrid;

    @FXML
    private Button signUpButton;

    @FXML
    private Label signInLabel;

    @FXML
    private ToggleGroup roleGroup;

    @FXML
    private ToggleButton userBtn;

    private String selectedRole = "User";
    private final AdminFileManager adminFileManager;

    // Constructor with Dependency Injection
    public SignUpController() {
        this.adminFileManager = new AdminFileManager();
        // Set dependencies
        UserFileManager userFileManager = new UserFileManager();
        ArtistFileManager artistFileManager = new ArtistFileManager();
        artistFileManager.setUserFileManager(userFileManager);
        this.adminFileManager.setUserFileManager(userFileManager);
        this.adminFileManager.setArtistFileManager(artistFileManager);
    }

    // For testing or manual injection
    public SignUpController(AdminFileManager adminFileManager) {
        this.adminFileManager = adminFileManager;
    }

    @FXML
    private void initialize() {
        setupRoleSelection();
        roleGrid.getScene().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());
        userBtn.setSelected(true);
        signUpButton.setOnAction(event -> handleSignUp());
        signInLabel.setOnMouseClicked(event -> handleSignInRedirect());
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

        // اعتبارسنجی رمز عبور
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
                // For the artist, the request is sent to the admin.
                adminFileManager.saveArtistRequest(email, nickname, password);
                AlertUtil.showSuccess("Your artist registration request has been submitted for approval.");
                SceneUtil.changeScene(signUpButton, "/fxml/signIn.fxml");
            } else {
                // For regular users, registration is done directly.
                AccountManager.registerUser(email, nickname, password, selectedRole);
                AlertUtil.showSuccess("Sign-up successful for: " + email);
                SceneUtil.changeScene(signUpButton, "/fxml/signIn.fxml");
            }
        } catch (Exception e) {
            AlertUtil.showError("Sign-up error: " + e.getMessage());
        }
    }

    private boolean isValidPassword(String password) {
        return AccountManager.validatePassword(password);
    }

    private void handleSignInRedirect() {
        SceneUtil.changeScene(signInLabel, "/fxml/signIn.fxml");
    }
}