package controllers.auth;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import models.account.Account;
import models.account.Admin;
import models.account.Artist;
import services.SessionManager;
import services.file.ArtistFileManager;
import services.file.UserFileManager;
import utils.AlertUtil;
import utils.SceneUtil;
import utils.ConfigLoader;

public class SignInController {
    private static final String USER_DASHBOARD_FXML = "/FXML-files/ArtistDashboard.fxml"; // فرض می‌کنیم برای کاربر عادی
    private static final String ARTIST_DASHBOARD_FXML = "/FXML-files/ArtistDashboard.fxml";
    private static final String ADMIN_DASHBOARD_FXML = "/FXML-files/AdminDashboard.fxml";
    private static final String SIGN_UP_FXML = "/FXML-files/signUp.fxml";

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label signUpLabel;

    private final UserFileManager userManager;
    private final ArtistFileManager artistManager;
    private final ConfigLoader configLoader;

    public SignInController() {
        this.userManager = new UserFileManager();
        this.artistManager = new ArtistFileManager();
        this.configLoader = ConfigLoader.getInstance();
        this.artistManager.setUserFileManager(this.userManager);
    }

    public SignInController(UserFileManager userManager, ArtistFileManager artistManager, ConfigLoader configLoader) {
        this.userManager = userManager;
        this.artistManager = artistManager;
        this.configLoader = configLoader;
        this.artistManager.setUserFileManager(this.userManager);
    }

    @FXML
    private void initialize() {
        loginButton.setOnAction(event -> handleLogin());
        signUpLabel.setOnMouseClicked(event -> handleSignUpRedirect());
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
            return;
        }

        email = email.toLowerCase().trim();

        try {
            if (tryAdminLogin(email, password) ||
                    tryUserLogin(email, password) ||
                    tryArtistLogin(email, password)) {
                return;
            }

            AlertUtil.showError("Invalid email or password.");
        } catch (Exception e) {
            AlertUtil.showError("Login error: " + e.getMessage());
        }
    }

    private boolean tryAdminLogin(String email, String password) {
        String adminEmail = configLoader.getAdminEmail();
        String adminPassword = configLoader.getAdminPassword();

        if (email.equals(adminEmail.toLowerCase()) && password.equals(adminPassword)) {
            Account admin = new Admin(adminEmail, "admin", adminPassword);
            SessionManager.getInstance().setCurrentAccount(admin);
            AlertUtil.showSuccess("Login successful! Welcome, Admin");
            SceneUtil.changeScene(loginButton, ADMIN_DASHBOARD_FXML);
            return true;
        }
        return false;
    }

    private boolean tryUserLogin(String email, String password) {
        try {
            String userNickName = userManager.findNickNameByEmail(email, "user");
            if (userNickName == null) {
                return false;
            }

            Account account = userManager.loadAccountByNickName(userNickName);
            if (account == null) {
                return false;
            }

            if (!password.equals(account.getPassword())) {
                AlertUtil.showError("Incorrect password for user account.");
                return false;
            }

            SessionManager.getInstance().setCurrentAccount(account);
            AlertUtil.showSuccess("Login successful! Welcome, " + userNickName);
            SceneUtil.changeScene(loginButton, USER_DASHBOARD_FXML);
            return true;

        } catch (Exception e) {
            String errorMessage = "Error loading user account: " + e.getMessage();
            throw new RuntimeException(errorMessage, e);
        }
    }

    private boolean tryArtistLogin(String email, String password) {
        try {
            String artistNickName = artistManager.findNickNameByEmail(email, "artist");
            if (artistNickName == null) {
                return false;
            }

            Account account = artistManager.loadAccountByNickName(artistNickName);
            if (account == null) {
                return false;
            }

            if (!password.equals(account.getPassword())) {
                AlertUtil.showError("Incorrect password for artist account.");
                return false;
            }

            if (account instanceof Artist artist && !artist.isApproved()) {
                AlertUtil.showError("Your artist account is not yet approved.");
                return false;
            }

            SessionManager.getInstance().setCurrentAccount(account);
            AlertUtil.showSuccess("Login successful! Welcome, " + artistNickName);
            SceneUtil.changeScene(loginButton, ARTIST_DASHBOARD_FXML);
            return true;

        } catch (Exception e) {
            String errorMessage = "Error loading artist account: " + e.getMessage();
            throw new RuntimeException(errorMessage, e);
        }
    }

    private void handleSignUpRedirect() {
        SceneUtil.changeScene(signUpLabel, SIGN_UP_FXML);
    }
}