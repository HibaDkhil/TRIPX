package tn.esprit.controllers.admin;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

public class ProfileAdminController {

    @FXML private Circle profileCircle;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileEmailLabel;

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private Label roleDisplayField;

    @FXML private Label memberSinceLabel;
    @FXML private Label lastLoginLabel;

    @FXML private PasswordField currentPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;

    @FXML private Button saveProfileBtn;
    @FXML private Button changePwdBtn;

    private User currentUser;
    private String userRole;
    private UserService userService;

    public void setUserData(User user, String role) {
        System.out.println("=== ProfileAdminController.setUserData ===");
        System.out.println("User received: " + user);
        System.out.println("Role received: " + role);

        this.currentUser = user;
        this.userRole = role;
        this.userService = new UserService();

        if (currentUser != null) {
            System.out.println("User ID: " + currentUser.getUserId());
            System.out.println("User Email: " + currentUser.getEmail());
            loadUserData();
        } else {
            System.err.println("ERROR: User is null in setUserData!");
            showAlert("Error", "No user data received!", Alert.AlertType.ERROR);
        }
    }

    private void loadUserData() {
        if (currentUser == null) {
            System.err.println("ERROR: Cannot load user data - currentUser is null");
            return;
        }

        try {
            System.out.println("Loading user data for: " + currentUser.getEmail());

            // Display labels with REAL data
            String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();
            profileNameLabel.setText(fullName);
            profileRoleLabel.setText(userRole);
            profileEmailLabel.setText(currentUser.getEmail());

            // Form fields with REAL data
            firstNameField.setText(currentUser.getFirstName());
            lastNameField.setText(currentUser.getLastName());
            emailField.setText(currentUser.getEmail());
            roleDisplayField.setText(userRole);

            // Set member since date from database
            if (currentUser.getCreatedAt() != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy");
                memberSinceLabel.setText(sdf.format(currentUser.getCreatedAt()));
            } else {
                memberSinceLabel.setText("N/A");
            }

            // Set last login
            lastLoginLabel.setText(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

            // Set profile circle color
            profileCircle.setFill(Color.web("#4E8EA2"));

            System.out.println("User data loaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error loading profile data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void initialize() {
        System.out.println("ProfileAdminController.initialize() called");

        saveProfileBtn.setOnAction(e -> saveProfile());
        changePwdBtn.setOnAction(e -> changePassword());

        // Add hover effects
        addHoverEffect(saveProfileBtn, "#4E8EA2", "#0A4174");
        addHoverEffect(changePwdBtn, "#2196F3", "#1976D2");
    }

    private void addHoverEffect(Button btn, String normalColor, String hoverColor) {
        String originalStyle = btn.getStyle();
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: " + normalColor + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30; -fx-cursor: default;");
        });
    }

    private void saveProfile() {
        if (currentUser == null) {
            showAlert("Error", "No user data found!", Alert.AlertType.ERROR);
            return;
        }

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showAlert("Error", "Fields cannot be empty", Alert.AlertType.ERROR);
            return;
        }

        // Update user object with REAL DATA
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setEmail(email);

        boolean updated = userService.updateUser(currentUser);

        if (updated) {
            showAlert("Success", "Profile updated successfully!", Alert.AlertType.INFORMATION);
            loadUserData(); // Refresh with updated data
        } else {
            showAlert("Error", "Failed to update profile", Alert.AlertType.ERROR);
        }
    }

    private void changePassword() {
        if (currentUser == null) {
            showAlert("Error", "No user data found!", Alert.AlertType.ERROR);
            return;
        }

        String currentPwd = currentPwdField.getText();
        String newPwd = newPwdField.getText();
        String confirmPwd = confirmPwdField.getText();

        if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showAlert("Error", "All password fields are required", Alert.AlertType.ERROR);
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showAlert("Error", "New passwords do not match", Alert.AlertType.ERROR);
            return;
        }

        // Verify current password
        if (!currentPwd.equals(currentUser.getPassword())) {
            showAlert("Error", "Current password is incorrect", Alert.AlertType.ERROR);
            return;
        }

        // Update password
        currentUser.setPassword(newPwd);
        boolean updated = userService.updateUserPassword(currentUser);

        if (updated) {
            showAlert("Success", "Password changed successfully!", Alert.AlertType.INFORMATION);
            currentPwdField.clear();
            newPwdField.clear();
            confirmPwdField.clear();
        } else {
            showAlert("Error", "Failed to change password", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}