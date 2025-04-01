package controllers.auth;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import services.AccountManager;
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

    @FXML
    private void initialize() {

        setupRoleSelection();

        // Attaching the CSS file to the scene
        roleGrid.getScene().getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/styles.css")).toExternalForm());

        // Default setting
        userBtn.setSelected(true);

        signUpButton.setOnAction(event -> handleSignUp());
        signInLabel.setOnMouseClicked(event -> handleSignInRedirect());
    }

    private void setupRoleSelection() {
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                ToggleButton selectedButton = (ToggleButton) newToggle;
                selectedRole = selectedButton.getText(); // "User" or "Artist"
            } else {
                // If no button is selected, we default to "User"
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

        if (password.length() < 8) {
            AlertUtil.showWarning("Password must be at least 8 characters long!");
            return;
        }

        if (AccountManager.findByEmail(email) != null) {
            AlertUtil.showError("This email is already registered. Please use another email.");
            return;
        }

        if (!AccountManager.registerUser(email, nickname, password, selectedRole)) {
            AlertUtil.showError("Sign-up failed. Please try again.");
            return;
        }

        AlertUtil.showSuccess("Sign-up successful for: " + email);
        SceneUtil.changeScene(signUpButton, "/FXML-files/signIn.fxml");
    }

    private void handleSignInRedirect() {
        SceneUtil.changeScene(signInLabel, "/FXML-files/signIn.fxml");
    }
}