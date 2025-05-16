import controllers.auth.SignUpController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.ConfigLoader;

import java.io.IOException;

public class SignUpControllerTest extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // 1. Load FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML-Files/signUp.fxml"));
            Scene scene = new Scene(loader.load(), 877, 711); // Scene size aligned with FXML

            // 2. Access the controller
            SignUpController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Controller is null. Check fx:controller attribute in FXML file.");
            }

            // 3. Set up and show Stage
            primaryStage.setTitle("Sign Up Page Test");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        ConfigLoader configLoader = ConfigLoader.getInstance();

        // 2. Read admin details (moved here for consistency)
        String adminEmail = configLoader.getAdminEmail();
        String adminPassword = configLoader.getAdminPassword();
        String adminNickname = configLoader.getAdminNickname();
        String dataDirectory = configLoader.getDataDirectory();
        long maxFileSize = configLoader.getMaxFileSize();
    }

    public static void main(String[] args) {
        launch(args);
    }
}