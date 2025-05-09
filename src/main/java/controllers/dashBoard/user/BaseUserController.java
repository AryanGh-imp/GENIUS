package controllers.dashBoard.user;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.SessionManager;
import utils.FileUtil;
import utils.SceneUtil;

import java.io.File;
import java.util.List;

public abstract class BaseUserController {

    @FXML protected Label welcomeLabel;
    @FXML protected Node signOutButton;

    protected UserMenuBarHandler menuBarHandler;
    protected static final String DEFAULT_IMAGE_PATH = "/pics/Genius.com_logo_yellow.png";

    @FXML
    protected void initialize() {
        menuBarHandler = new UserMenuBarHandler(signOutButton);
        setupWelcomeLabel();
    }

    protected void setupWelcomeLabel() {
        if (welcomeLabel != null) {
            String username = SessionManager.getInstance().getCurrentUsername();
            welcomeLabel.setText(username != null ? "Welcome, " + username + "!" : "Welcome, Guest!");
        }
    }

    protected List<String> loadFileData(String filePath) {
        try {
            return FileUtil.readFile(filePath);
        } catch (Exception e) {
            System.err.println("Error loading file data from " + filePath + ": " + e.getMessage());
            return List.of();
        }
    }

    protected void loadImage(ImageView imageView, String imagePath) {
        if (imageView == null) return;
        File imageFile = new File(imagePath);
        try {
            imageView.setImage(imageFile.exists() ? new Image(imageFile.toURI().toString()) : new Image(DEFAULT_IMAGE_PATH));
        } catch (Exception e) {
            System.err.println("Error loading image from " + imagePath + ": " + e.getMessage());
            imageView.setImage(new Image(DEFAULT_IMAGE_PATH));
        }
    }

    protected void setupListView(ListView<String> listView, String targetFxmlPath) {
        if (listView == null) return;
        listView.setOnMouseClicked(event -> {
            String selectedItem = listView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && !selectedItem.contains("No results") && !selectedItem.contains("No artists")) {
                SceneUtil.changeScene(listView, targetFxmlPath);
            }
        });
    }

    protected void checkComponent(Object component, String name) {
        if (component == null) {
            System.err.println(name + " is null. Check FXML file.");
        }
    }

    // Navigation methods
    @FXML public void goToProfile() { menuBarHandler.goToProfile(); }
    @FXML public void goToSearch() { menuBarHandler.goToSearch(); }
    @FXML public void goToCharts() { menuBarHandler.goToCharts(); }
    @FXML public void signOut() { menuBarHandler.signOut(); }
}