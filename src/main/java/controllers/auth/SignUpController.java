package controllers.auth;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import services.AccountManager;
import services.file.AdminFileManager;
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

    private String selectedRole = "User"; // Default role
    private final AdminFileManager adminFileManager;

    private final String SIGN_IN_FXML = "/FXML-files/SignIn.fxml";
    private final String CSS_STYLE = "/CSS/styles.css";

    public SignUpController() {
        this.adminFileManager = new AdminFileManager();
    }

    @FXML
    private void initialize() {
        setupRoleSelection();
        userBtn.setSelected(true); // Ensure User role is selected by default
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
                System.out.println("Selected role: " + selectedRole); // Debug log
            } else {
                selectedRole = "User";
                userBtn.setSelected(true);
                System.out.println("No role selected, defaulting to User"); // Debug log
            }
        });
    }

    private void handleSignUp() {
        String nickname = nicknameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validate input fields
        if (nickname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
            return;
        }

        // Validate email format
        if (!AccountManager.validateEmail(email)) {
            AlertUtil.showWarning("Invalid email format!");
            return;
        }

        // Validate password strength
        if (!isValidPassword(password)) {
            AlertUtil.showWarning("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
            return;
        }

        // Check if email is already registered
        if (AccountManager.findByEmail(email) != null) {
            AlertUtil.showError("This email is already registered. Please use another email.");
            return;
        }

        try {
            // Register the user based on the selected role
            if ("Artist".equals(selectedRole)) {
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
            e.printStackTrace();
        }
    }

    private boolean isValidPassword(String password) {
        return AccountManager.validatePassword(password);
    }

    private void handleSignInRedirect() {
        SceneUtil.changeScene(signInLabel, SIGN_IN_FXML);
    }
}