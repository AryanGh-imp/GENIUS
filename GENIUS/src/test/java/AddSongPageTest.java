import controllers.dashBoard.artist.AddSongPageController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.account.Artist;
import services.SessionManager;

import java.io.IOException;

public class AddSongPageTest extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // 1. Create a test artist (Kendrick Lamar)
            Artist artist = new Artist("kendrick.lamar@example.com", "KendrickLamar", "KDot2023!");
            artist.setApproved(true); // Artist must be approved to add songs

            // 2. Set up SessionManager
            SessionManager.getInstance().setCurrentAccount(artist);

            // 3. Load FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML-files/artist/AddSong.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("FXML file not found at /FXML-files/artist/AddSong.fxml");
            }
            Scene scene = new Scene(loader.load());

            // 4. Access the controller
            AddSongPageController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Controller is null. Check fx:controller attribute in FXML file.");
            }

            // 5. Set up and show Stage
            primaryStage.setTitle("Add Song Page Test - Kendrick Lamar");
            primaryStage.setScene(scene);
            primaryStage.show();

            // 6. Clear SessionManager on window close
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