package controllers.auth;

import controllers.dashBoard.admin.AdminDashboardController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.account.Account;
import models.account.Admin;
import models.account.Artist;
import services.SessionManager;
import services.file.AdminFileManager;
import services.file.ArtistFileManager;
import services.file.UserFileManager;
import utils.AlertUtil;
import utils.ConfigLoader;
import utils.SceneUtil;

public class SignInController {
    private static final String USER_DASHBOARD_FXML = "/FXML-files/user/UserDashboard.fxml";
    private static final String ARTIST_DASHBOARD_FXML = "/FXML-files/artist/ArtistDashboard.fxml";
    private static final String ADMIN_DASHBOARD_FXML = "/FXML-files/admin/AdminDashboard.fxml";
    private static final String SIGN_UP_FXML = "/FXML-files/signUp.fxml";

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label signUpLabel;

    private final UserFileManager userManager;
    private final ArtistFileManager artistManager;
    private final AdminFileManager adminFileManager;
    private final ConfigLoader configLoader;

    public SignInController() {
        this.userManager = new UserFileManager();
        this.artistManager = new ArtistFileManager();
        this.adminFileManager = new AdminFileManager();
        this.configLoader = ConfigLoader.getInstance();
    }

    @FXML
    private void initialize() {
        loginButton.setOnAction(event -> {
            System.out.println("Login button clicked!");
            handleLogin();
        });
        signUpLabel.setOnMouseClicked(event -> {
            System.out.println("Sign up label clicked!");
            handleSignUpRedirect();
        });
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
            System.out.println("Validation failed: Email or password is empty.");
            return;
        }

        email = email.toLowerCase().trim();

        try {
            if (tryAdminLogin(email, password)) {
                System.out.println("Admin login successful.");
                return;
            }
            if (tryUserLogin(email, password)) {
                System.out.println("User login successful.");
                return;
            }
            if (tryArtistLogin(email, password)) {
                System.out.println("Artist login successful.");
                return;
            }
            AlertUtil.showError("Invalid email or password.");
            System.out.println("Login failed: Invalid email or password.");
        } catch (Exception e) {
            AlertUtil.showError("Login error: " + e.getMessage());
            System.out.println("Login error occurred: " + e.getMessage());
        }
    }

    private boolean tryAdminLogin(String email, String password) {
        String adminEmail = configLoader.getAdminEmail();
        String adminPassword = configLoader.getAdminPassword();
        String adminNickName = configLoader.getAdminNickname();

        if (adminEmail == null || adminEmail.trim().isEmpty() ||
                adminPassword == null || adminPassword.trim().isEmpty() ||
                adminNickName == null || adminNickName.trim().isEmpty()) {
            AlertUtil.showError("Admin configuration is missing or invalid. Please check config file.");
            System.out.println("Admin configuration invalid: adminEmail=" + adminEmail +
                    ", adminPassword=" + adminPassword +
                    ", adminNickName=" + adminNickName);
            return false;
        }

        if (email.equals(adminEmail.toLowerCase()) && password.equals(adminPassword)) {
            Admin admin = new Admin(adminEmail, adminNickName, adminPassword);
            admin.setAdminFileManager(adminFileManager);
            return loginSuccess(admin, ADMIN_DASHBOARD_FXML, "Admin Dashboard");
        }
        System.out.println("Admin login failed: Email or password mismatch.");
        return false;
    }

    private boolean tryUserLogin(String email, String password) {
        try {
            String userNickName = userManager.findNickNameByEmail(email, "user");
            if (userNickName == null) {
                System.out.println("User not found with email: " + email);
                return false;
            }

            Account account = userManager.loadAccountByNickName(userNickName);
            if (account == null) {
                System.out.println("User account not found for nickname: " + userNickName);
                return false;
            }

            if (!password.equals(account.getPassword())) {
                System.out.println("Incorrect password for user: " + userNickName);
                return false;
            }

            return loginSuccess(account, USER_DASHBOARD_FXML, "User Dashboard");
        } catch (Exception e) {
            System.out.println("Error loading user account: " + e.getMessage());
            return false;
        }
    }

    private boolean tryArtistLogin(String email, String password) {
        try {
            String artistNickName = artistManager.findNickNameByEmail(email, "artist");
            if (artistNickName == null) {
                System.out.println("Artist not found with email: " + email);
                return false;
            }

            Account account = artistManager.loadAccountByNickName(artistNickName);
            if (account == null) {
                System.out.println("Artist account not found for nickname: " + artistNickName);
                return false;
            }

            if (!password.equals(account.getPassword())) {
                System.out.println("Incorrect password for artist: " + artistNickName);
                return false;
            }

            if (account instanceof Artist artist && !artist.isApproved()) {
                AlertUtil.showError("Your artist account is not yet approved.");
                System.out.println("Artist account not approved: " + artistNickName);
                return false;
            }

            return loginSuccess(account, ARTIST_DASHBOARD_FXML, "Artist Dashboard");
        } catch (Exception e) {
            System.out.println("Error loading artist account: " + e.getMessage());
            return false;
        }
    }

    private boolean loginSuccess(Account account, String fxmlPath, String title) {
        try {
            SessionManager.getInstance().setCurrentAccount(account);
            AlertUtil.showSuccess("Login successful! Welcome, " + account.getNickName());
            System.out.println("Attempting to load dashboard: " + fxmlPath);

            if (account instanceof Admin admin) {
                // Load Admin Dashboard manually for admin
                FXMLLoader loader = new FXMLLoader(getClass().getResource(ADMIN_DASHBOARD_FXML));
                Scene scene = new Scene(loader.load());

                // Access AdminDashboardController and set admin
                AdminDashboardController controller = loader.getController();
                if (controller == null) {
                    throw new RuntimeException("AdminDashboardController is null. Check fx:controller attribute in AdminDashboard.fxml.");
                }
                controller.setAdmin(admin);

                // Change scene to Admin Dashboard
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Genius Music - " + title);
                stage.show();
            } else {
                // Use SceneUtil.changeScene for other users (User and Artist)
                SceneUtil.changeScene(loginButton, fxmlPath);
            }

            System.out.println("Dashboard loaded successfully: " + title);
            return true;
        } catch (Exception e) {
            AlertUtil.showError("Error loading dashboard '" + title + "': " + e.getMessage());
            System.out.println("Error loading dashboard '" + title + "': " + e.getMessage());
            return false;
        }
    }

    private void handleSignUpRedirect() {
        try {
            System.out.println("Sign up label clicked!");
            SceneUtil.changeScene(signUpLabel, SIGN_UP_FXML);
            System.out.println("Sign-up page loaded successfully.");
        } catch (Exception e) {
            AlertUtil.showError("Failed to load sign-up page: " + e.getMessage());
            System.out.println("Failed to load sign-up page: " + e.getMessage());
        }
    }
}