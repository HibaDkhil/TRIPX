package tn.esprit.controllers.user;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import tn.esprit.entities.User;

import java.io.IOException;

public class HomeController {

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/profile.fxml"));
            Parent root = loader.load();
            
            ProfileController controller = loader.getController();
            controller.setUser(currentUser); // Pass the user
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePackages(ActionEvent event) {
        // defined in fxml but logic to be added
        System.out.println("Navigate to Packages");
    }

    @FXML
    private void handleBooking(ActionEvent event) {
        System.out.println("Booking Now clicked");
    }

    @FXML
    private void handlePlayVideo(ActionEvent event) {
        System.out.println("Play Video clicked");
    }

    // Reuse navigateTo if needed, but handleProfile is specific now
    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Navigate back to login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
