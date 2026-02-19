package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/destination_management.fxml"));

            if (loader.getLocation() == null) {
                System.out.println("ERROR: Cannot find FXML file!");
                System.out.println("Looking for: /fxml/admin/destination_management.fxml");

                // List resources to debug
                System.out.println("\n=== Available Resources ===");
                listResources("/fxml");
                return;
            }

            // Load without CSS first
            Scene scene = new Scene(loader.load(), 1280, 720);

            // Try to add CSS but don't fail if not found
            try {
                String cssPath = getClass().getResource("/css/destination.css").toExternalForm();
                scene.getStylesheets().add(cssPath);
                System.out.println("CSS loaded successfully");
            } catch (Exception e) {
                System.out.println("CSS not found, continuing without it");
            }

            primaryStage.setTitle("TRIPX - Destination Management");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.show();

            System.out.println("Application started successfully!");

        } catch (Exception e) {
            System.out.println("ERREUR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to list resources for debugging
    private void listResources(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                java.io.File file = new java.io.File(url.toURI());
                listFiles(file, "");
            } else {
                System.out.println("No resources found at: " + path);
            }
        } catch (Exception e) {
            System.out.println("Error listing resources: " + e.getMessage());
        }
    }

    private void listFiles(java.io.File dir, String indent) {
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                System.out.println(indent + (file.isDirectory() ? "📁 " : "📄 ") + file.getName());
                if (file.isDirectory()) {
                    listFiles(file, indent + "  ");
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}