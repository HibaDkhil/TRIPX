package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main Application Entry Point for TripX Platform
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("========================================");
            System.out.println("TripX Platform - Initializing");
            System.out.println("Version: 1.0");
            System.out.println("========================================");

            // Load the launcher/main menu FXML
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/user/AccommodationDetailsView.fxml")
            );

            Parent root = loader.load();

            // Create scene
            Scene scene = new Scene(root);

            // Setup stage
            primaryStage.setTitle("TripX Platform");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            System.out.println("========================================");
            System.out.println("TripX Application - Started Successfully");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to start application");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("TripX Application - Starting");
        System.out.println("========================================");
        launch(args);
    }
}