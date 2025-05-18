import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.account.User;
import services.SessionManager;

import java.io.IOException;

public class ChartsPageTest extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // 1. Set up SessionManager with a test user
            User user = new User("fan@example.com", "MusicFan", "FanPass2023!");
            SessionManager.getInstance().setCurrentAccount(user);

            // 2. Load FXML file for Charts page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML-files/user/ChartsPage.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("FXML file not found at /FXML-files/user/ChartsPage.fxml");
            }
            Scene scene = new Scene(loader.load(), 900, 600);

            // 3. Set up and show Stage
            primaryStage.setTitle("Charts Page Test - Music Fan");
            primaryStage.setScene(scene);
            primaryStage.show();

            // 4. Clear SessionManager on window close
            primaryStage.setOnCloseRequest(event -> {
                SessionManager.getInstance().clearSession();
            });

        } catch (IOException e) {
            System.err.println("Error loading FXML file: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}