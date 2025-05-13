import controllers.dashBoard.artist.DeleteSongController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.account.Artist;
import services.SessionManager;

import java.io.IOException;

public class DeleteSongPageTest extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // 1. Create a test artist (Lil Wayne)
            Artist artist = new Artist("lilwayne@example.com", "LilWayne", "Lollipop123!");
            artist.setApproved(true); // Artist must be approved to access delete song page

            // 2. Set up SessionManager
            SessionManager.getInstance().setCurrentAccount(artist);

            // 3. Load FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML-files/artist/DeleteSong.fxml"));
            if (loader.getLocation() == null) {
                throw new IOException("FXML file not found at /FXML-files/artist/DeleteSong.fxml");
            }
            Scene scene = new Scene(loader.load(), 912, 754); // Scene size aligned with FXML

            // 4. Access the controller
            DeleteSongController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("Controller is null. Check fx:controller attribute in FXML file.");
            }

            // 5. Set up and show Stage
            primaryStage.setTitle("Delete Song Page Test - Lil Wayne");
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