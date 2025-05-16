package app;

import controllers.auth.SignInController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.AlertUtil;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file for the sign-in page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML-files/SignIn.fxml"));
            Scene scene = new Scene(loader.load());

            // Access the controller
            SignInController controller = loader.getController();
            if (controller == null) {
                throw new RuntimeException("SignInController is null. Check fx:controller attribute in SignIn.fxml.");
            }

            // Set up and show Stage
            primaryStage.setTitle("Genius Music");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.getIcons().add(
                    new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/pics/Genius_logo_black.jpg")))
            );
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Error loading SignIn.fxml: " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("Failed to load the Sign In page. Please check the application resources and try again.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error during application startup: " + e.getMessage());
            e.printStackTrace();
            AlertUtil.showError("An unexpected error occurred during startup: ");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}