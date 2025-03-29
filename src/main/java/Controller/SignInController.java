package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import utils.SceneUtil;
import utils.AlertUtil;
import utils.FileUtil;

public class SignInController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label signUpLabel;


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


        if (!FileUtil.isValidUser(email, password)) {
            AlertUtil.showError("Invalid email or password.");
            return;
        }

        String nickname = FileUtil.readUser(email)[1];
        AlertUtil.showSuccess("Login successful! Welcome, " + nickname);
        // Dashboard ...
    }

    @FXML
    private void handleSignUpRedirect() {
        SceneUtil.changeScene(signUpLabel, "/FXML-files/signUp.fxml");
    }
}
