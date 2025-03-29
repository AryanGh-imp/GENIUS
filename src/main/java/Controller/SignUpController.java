package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import utils.FileUtil;
import utils.AlertUtil;
import utils.SceneUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        if (!validateEmail(email)) {
            AlertUtil.showWarning("Invalid email !");
            return;
        }

        if (password.length() < 8) {
            AlertUtil.showWarning("Password must be at least 8 characters long !");
            return;
        }

        if (FileUtil.isEmailTaken(email)) {
            AlertUtil.showError("This email is already registered. Please use another email.");
            return;
        }

        FileUtil.saveUser(email, nickname, password);
        AlertUtil.showSuccess("Sign-up successful for: " + email);
        SceneUtil.changeScene(signUpButton, "/FXML-files/signIn.fxml");
    }

    private void handleSignInRedirect() {
        SceneUtil.changeScene(signInLabel, "/FXML-files/signIn.fxml");
    }

    public boolean validateEmail(String email) {
        String regex = "^[a-zA-Z0-9]+([._+-][a-zA-Z0-9]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);

        return matcher.matches();
    }

}

