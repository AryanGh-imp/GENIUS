package utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class AlertUtil {

    public static void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper method for displaying error messages
    public static void showError(String message) {
        showAlert(AlertType.ERROR, "Error", message);
    }

    // Helper method for displaying success messages
    public static void showSuccess(String message) {
        showAlert(AlertType.INFORMATION, "Success", message);
    }

    // Helper method for displaying warnings
    public static void showWarning(String message) {
        showAlert(AlertType.WARNING, "Warning", message);
    }
}
