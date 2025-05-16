import controllers.dashBoard.admin.AdminDashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.account.Admin;
import services.file.AdminFileManager;
import services.file.ArtistFileManager;
import services.file.UserFileManager;
import utils.ConfigLoader;


public class AdminTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Set up test data using TestDataSetup
        //TestDataSetup.setupTestData();

        // 2. Get ConfigLoader instance
        ConfigLoader configLoader = ConfigLoader.getInstance();

        // 3. Read admin details
        String adminEmail = configLoader.getAdminEmail();
        String adminPassword = configLoader.getAdminPassword();
        String adminNickname = configLoader.getAdminNickname();
        String dataDirectory = configLoader.getDataDirectory();
        long maxFileSize = configLoader.getMaxFileSize();

        // Print details for testing
        System.out.println("Admin Email: " + adminEmail);
        System.out.println("Admin Nickname: " + adminNickname);
        System.out.println("Admin Password: " + adminPassword);
        System.out.println("Data Directory: " + dataDirectory);
        System.out.println("Max File Size: " + maxFileSize);

        // 4. Create Admin instance
        Admin admin = new Admin(adminEmail, adminNickname, adminPassword);

        // 5. Set up dependencies
        UserFileManager userFileManager = new UserFileManager();
        ArtistFileManager artistFileManager = new ArtistFileManager();
        AdminFileManager adminFileManager = new AdminFileManager();

        admin.setAdminFileManager(adminFileManager);

        // 7. Load FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML-files/admin/AdminDashboard.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);

        // 8. Access controller and set Admin
        AdminDashboardController controller = loader.getController();
        controller.setAdmin(admin);

        // 9. Set up and show Stage
        primaryStage.setTitle("Admin Dashboard Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}