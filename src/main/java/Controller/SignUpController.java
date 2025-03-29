package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import utils.FileUtil;
import utils.AlertUtil;

public class SignUpController {

    @FXML
    private TextField nicknameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signUpButton;

    @FXML
    private Label signInLabel;

    @FXML
    private void initialize() {
        signUpButton.setOnAction(event -> handleSignUp());

        signInLabel.setOnMouseClicked(event -> handleSignInRedirect());
    }

    private void handleSignUp() {
        String nickname = nicknameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (nickname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
            return;
        }

        if (password.length() < 8) {
            AlertUtil.showWarning("Password must be at least 8 characters long.");
            return;
        }

        // Saving user
        FileUtil.saveUser(email, nickname, password);
        AlertUtil.showSuccess("Sign-up successful for: " + email);
    }

    private void handleSignInRedirect() {
        System.out.println("Redirecting to Sign In page...");
    }
}

