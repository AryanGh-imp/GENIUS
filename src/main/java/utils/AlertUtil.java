package utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.util.Objects;

public class AlertUtil {

    private static final String LOGO_PATH = "/pics/Genius_logo_black.jpg";

    public static void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(Objects.requireNonNull(
                AlertUtil.class.getResourceAsStream(LOGO_PATH)
        )));

        alert.showAndWait();
    }

    public static void showError(String message) {
        showAlert(AlertType.ERROR, "Error", message);
    }

    public static void showSuccess(String message) {
        showAlert(AlertType.INFORMATION, "Success", message);
    }

    public static void showWarning(String message) {
        showAlert(AlertType.WARNING, "Warning", message);
    }
}
