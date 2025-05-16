package controllers.dashBoard.artist;

import javafx.scene.Node;
import services.SessionManager;
import utils.AlertUtil;
import utils.SceneUtil;

public class ArtistMenuBarHandler {

    private final Node node;

    public ArtistMenuBarHandler(Node node) {
        this.node = node;
    }

    public void goToProfile() {
        navigate("/FXML-files/artist/ArtistDashboard.fxml", "artistDashboardRoot", "Artist Dashboard");
    }

    public void goToAddSong() {
        navigate("/FXML-files/artist/AddSong.fxml", "addSongRoot", "Add Song");
    }

    public void goToDeleteSong() {
        navigate("/FXML-files/artist/DeleteSong.fxml", "deleteSongRoot", "Delete Song");
    }

    public void goToEditSong() {
        navigate("/FXML-files/artist/EditSong.fxml", "editSongRoot", "Edit Song");
    }

    public void goToCreateAlbum() {
        navigate("/FXML-files/artist/CreateAlbum.fxml", "createAlbumRoot", "Create Album");
    }

    public void goToDeleteAlbum() {
        navigate("/FXML-files/artist/DeleteAlbum.fxml", "deleteAlbumRoot", "Delete Album");
    }

    public void goToEditAlbum() {
        navigate("/FXML-files/artist/EditAlbum.fxml", "editAlbumRoot", "Edit Album");
    }

    public void goToPendingRequests() {
        navigate("/FXML-files/artist/ArtistManagement.fxml", "artistManagementRoot", "Artist Management");
    }

    public void goToApprovedRequests() {
        navigate("/FXML-files/artist/ArtistManagement.fxml", "artistManagementRoot", "Artist Management");
    }

    public void goToRejectedRequests() {
        navigate("/FXML-files/artist/ArtistManagement.fxml", "artistManagementRoot", "Artist Management");
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
        if (currentRootId != null && currentRootId.equals(rootId) && !currentRootId.equals("artistManagementRoot")) {
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