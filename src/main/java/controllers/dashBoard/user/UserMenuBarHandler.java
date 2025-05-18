package controllers.dashBoard.user;

import javafx.scene.Node;
import services.SessionManager;
import utils.AlertUtil;
import utils.SceneUtil;

public class UserMenuBarHandler {

    private final Node node;

    public UserMenuBarHandler(Node node) {
        this.node = node;
    }

    public void goToProfile() {
        navigate("/FXML-files/user/UserDashboard.fxml", "userDashboardRoot", "User Dashboard");
    }

    public void goToSearch() {
        navigate("/FXML-files/user/SearchPage.fxml", "searchPageRoot", "Search Page");
    }

    public void goToCharts() {
        navigate("/FXML-files/user/ChartsPage.fxml", "chartsPageRoot", "Charts Page");
    }

    public void signOut() {
        if (!validateNode()) return;
        SessionManager.getInstance().clearSession();
        AlertUtil.showSuccess("You have been signed out.");
        SceneUtil.changeScene(node, "/FXML-files/signIn.fxml");
    }

    private void navigate(String fxmlPath, String rootId, String pageName) {
        if (!validateNode()) return;
        String currentRootId = node.getScene().getRoot().getId();
        if (currentRootId != null && currentRootId.equals(rootId)) {
            AlertUtil.showSuccess("You are already on the " + pageName + " page!");
        } else {
            SceneUtil.changeScene(node, fxmlPath);
        }
    }

    private boolean validateNode() {
        if (node == null) {
            AlertUtil.showError("Cannot navigate: No valid node provided.");
            return false;
        }
        return true;
    }
}