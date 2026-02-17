package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.entities.UserPreferences;
import tn.esprit.services.UserPreferencesService;
import tn.esprit.services.UserService;
import tn.esprit.utils.ValidationUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ProfileController {

    @FXML private VBox personalInfoView;
    @FXML private VBox searchPreferencesView;
    @FXML private StackPane contentArea;

    @FXML private Button btnPersonalInfo;
    @FXML private Button btnPreferences;

    // Personal Info Fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private DatePicker birthDatePicker;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;
    @FXML private Label passwordStatusLabel;
    
    // Error Labels
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label genderError;
    @FXML private Label currentPasswordError;
    @FXML private Label newPasswordError;
    @FXML private Label confirmPasswordError;

    // Preferences Labels
    @FXML private Label budgetLabel;
    @FXML private Label styleLabel;
    @FXML private Label paceLabel;

    private User currentUser;
    private UserPreferences currentPreferences;
    private final UserService userService = new UserService();
    private final UserPreferencesService userPreferencesService = new UserPreferencesService();

    @FXML
    public void initialize() {
        // Initialize ComboBox
        if (genderComboBox != null) {
            genderComboBox.setItems(FXCollections.observableArrayList("Male", "Female"));
        }
        
        // Default visibility check
        if (personalInfoView != null) personalInfoView.setVisible(true);
        if (searchPreferencesView != null) searchPreferencesView.setVisible(false);
        if (btnPersonalInfo != null) setActiveButton(btnPersonalInfo);
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            loadUserData();
            loadUserPreferences();
            // Ensure default view is shown
            showPersonalInfo(null);
        }
    }

    private void loadUserData() {
        if (firstNameField != null) firstNameField.setText(currentUser.getFirstName());
        if (lastNameField != null) lastNameField.setText(currentUser.getLastName());
        if (emailField != null) emailField.setText(currentUser.getEmail());
        if (phoneField != null) phoneField.setText(currentUser.getPhoneNumber()); // Might be null
        if (genderComboBox != null) genderComboBox.setValue(currentUser.getGender());
        
        if (currentUser.getBirthYear() != null && !currentUser.getBirthYear().isEmpty() && birthDatePicker != null) {
            try {
                birthDatePicker.setValue(LocalDate.parse(currentUser.getBirthYear()));
            } catch (Exception e) {
                // Ignore parsing errors for now
            }
        }
    }

    private void loadUserPreferences() {
        currentPreferences = userPreferencesService.getPreferencesByUserId(currentUser.getUserId());
        if (currentPreferences == null) {
            // Create default empty preferences object
            currentPreferences = new UserPreferences();
            currentPreferences.setUserId(currentUser.getUserId());
            userPreferencesService.addPreferences(currentPreferences);
            // Re-fetch to get ID
            currentPreferences = userPreferencesService.getPreferencesByUserId(currentUser.getUserId());
        }

        updatePreferencesUI();
    }

    private void updatePreferencesUI() {
        if (currentPreferences != null) {
            // Budget
            if (budgetLabel != null) {
                if (currentPreferences.getBudgetMinPerNight() != null && currentPreferences.getBudgetMaxPerNight() != null) {
                    budgetLabel.setText(currentPreferences.getBudgetMinPerNight() + " - " + currentPreferences.getBudgetMaxPerNight());
                } else {
                    budgetLabel.setText("Not set");
                }
            }

            // Style
            if (styleLabel != null) {
                styleLabel.setText(currentPreferences.getStylePreferences() != null ? currentPreferences.getStylePreferences() : "Not set");
            }

            // Pace
            if (paceLabel != null) {
                paceLabel.setText(currentPreferences.getTravelPace() != null ? currentPreferences.getTravelPace() : "Not set");
            }
        }
    }

    // Navigation Logic
    @FXML
    void showPersonalInfo(ActionEvent event) {
        if (personalInfoView != null) personalInfoView.setVisible(true);
        if (searchPreferencesView != null) searchPreferencesView.setVisible(false);
        setActiveButton(btnPersonalInfo);
    }

    @FXML
    void showSearchPreferences(ActionEvent event) {
        if (personalInfoView != null) personalInfoView.setVisible(false);
        if (searchPreferencesView != null) searchPreferencesView.setVisible(true);
        setActiveButton(btnPreferences);
    }

    private void setActiveButton(Button activeButton) {
        // Reset styles
        if (btnPersonalInfo != null) {
            btnPersonalInfo.getStyleClass().removeAll("rail-item-active");
            btnPersonalInfo.getStyleClass().add("rail-item");
        }
        if (btnPreferences != null) {
            btnPreferences.getStyleClass().removeAll("rail-item-active");
            btnPreferences.getStyleClass().add("rail-item");
        }
        
        if (activeButton != null) {
            activeButton.getStyleClass().removeAll("rail-item");
            activeButton.getStyleClass().add("rail-item-active");
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/home.fxml"));
            Parent root = loader.load();
            
            // Pass user back
            HomeController controller = loader.getController();
            controller.setUser(currentUser); 
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // UPDATE PROFILE
    @FXML
    void handleUpdateProfile(ActionEvent event) {
        if (statusLabel != null) statusLabel.setVisible(false);
        
        String validationError = validateInput();
        if (validationError != null) {
            if (statusLabel != null) {
                statusLabel.setText(validationError);
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: red;");
            }
            return;
        }

        // Update User Object
        if (firstNameField != null) currentUser.setFirstName(firstNameField.getText());
        if (lastNameField != null) currentUser.setLastName(lastNameField.getText());
        if (emailField != null) currentUser.setEmail(emailField.getText());
        if (phoneField != null) currentUser.setPhoneNumber(phoneField.getText());
        if (genderComboBox != null) currentUser.setGender(genderComboBox.getValue());
        
        if (birthDatePicker != null && birthDatePicker.getValue() != null) {
            currentUser.setBirthYear(birthDatePicker.getValue().toString());
        }

        boolean updateSuccess = userService.updateUser(currentUser);

        // Update Password if fields not empty
        if (newPasswordField != null && !newPasswordField.getText().isEmpty()) {
            currentUser.setPassword(newPasswordField.getText()); // In real app, hash this!
            userService.updateUserPassword(currentUser);
        }

        if (statusLabel != null) {
            if (updateSuccess) {
                statusLabel.setText("Profile updated successfully!");
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("Failed to update profile.");
                statusLabel.setVisible(true);
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    private String validateInput() {
        boolean isValid = true;
        clearAllErrors();
        
        // Validate First Name
        if (!ValidationUtils.isValidName(firstNameField.getText())) {
            showFieldError(firstNameField, firstNameError, 
                ValidationUtils.isNotEmpty(firstNameField.getText()) ? 
                ValidationUtils.getNameError("First name") : 
                ValidationUtils.getRequiredFieldError("First name"));
            isValid = false;
        }
        
        // Validate Last Name
        if (!ValidationUtils.isValidName(lastNameField.getText())) {
            showFieldError(lastNameField, lastNameError, 
                ValidationUtils.isNotEmpty(lastNameField.getText()) ? 
                ValidationUtils.getNameError("Last name") : 
                ValidationUtils.getRequiredFieldError("Last name"));
            isValid = false;
        }
        
        // Validate Email
        if (!ValidationUtils.isValidEmail(emailField.getText())) {
            showFieldError(emailField, emailError, 
                ValidationUtils.isNotEmpty(emailField.getText()) ? 
                ValidationUtils.getEmailError() : 
                ValidationUtils.getRequiredFieldError("Email"));
            isValid = false;
        }
        
        // Validate Phone (optional)
        if (!ValidationUtils.isValidPhone(phoneField.getText())) {
            showFieldError(phoneField, phoneError, ValidationUtils.getPhoneError());
            isValid = false;
        }
        
        // Validate Gender
        if (genderComboBox.getValue() == null || genderComboBox.getValue().isEmpty()) {
            showFieldError(genderComboBox, genderError, ValidationUtils.getRequiredFieldError("Gender"));
            isValid = false;
        }
        
        return isValid ? null : "Please fix the errors above.";
    }
    
    private void clearAllErrors() {
        clearFieldError(firstNameField, firstNameError);
        clearFieldError(lastNameField, lastNameError);
        clearFieldError(emailField, emailError);
        clearFieldError(phoneField, phoneError);
        clearFieldError(genderComboBox, genderError);
    }
    
    private void showFieldError(Control field, Label errorLabel, String message) {
        if (field != null && errorLabel != null) {
            field.getStyleClass().add("input-error");
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }
    
    private void clearFieldError(Control field, Label errorLabel) {
        if (field != null && errorLabel != null) {
            field.getStyleClass().remove("input-error");
            errorLabel.setVisible(false);
        }
    }

    // EDIT PREFERENCES LOGIC

    @FXML
    void handleEditBudget(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(budgetLabel.getText().equals("Not set") ? "50-200" : budgetLabel.getText());
        dialog.setTitle("Edit Budget");
        dialog.setHeaderText("Enter Budget Range (Min-Max)");
        dialog.setContentText("Range:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(range -> {
            try {
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    BigDecimal min = new BigDecimal(parts[0].trim());
                    BigDecimal max = new BigDecimal(parts[1].trim());
                    
                    currentPreferences.setBudgetMinPerNight(min);
                    currentPreferences.setBudgetMaxPerNight(max);
                    
                    savePreferences();
                } else {
                    showAlert("Invalid Format", "Please use format: Min-Max (e.g., 50-200)");
                }
            } catch (NumberFormatException e) {
                 showAlert("Invalid Number", "Please enter valid numeric values.");
            }
        });
    }

    @FXML
    void handleEditStyle(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentPreferences.getStylePreferences());
        dialog.setTitle("Edit Travel Style");
        dialog.setHeaderText("Enter your travel style");
        dialog.setContentText("Style (e.g., Modern, Rustic, Luxury):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(style -> {
            currentPreferences.setStylePreferences(style);
            savePreferences();
        });
    }

    @FXML
    void handleEditPace(ActionEvent event) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Moderate", "Relaxed", "Moderate", "Fast-paced");
        dialog.setTitle("Edit Travel Pace");
        dialog.setHeaderText("Select your preferred travel pace");
        dialog.setContentText("Pace:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pace -> {
            currentPreferences.setTravelPace(pace);
            savePreferences();
        });
    }
    @FXML
    void handleEditPreferences(ActionEvent event) {
        showSearchPreferences(event);
    }

    @FXML
    void handleEditPriorities(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentPreferences.getPriorities());
        dialog.setTitle("Edit Travel Priorities");
        dialog.setHeaderText("Enter your travel priorities");
        dialog.setContentText("Priorities (e.g., Culture, Adventure, Relaxation):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(priorities -> {
            currentPreferences.setPriorities(priorities);
            savePreferences();
        });
    }

    @FXML
    void handleEditLocation(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentPreferences.getLocationPreferences());
        dialog.setTitle("Edit Location Preferences");
        dialog.setHeaderText("Enter your location preferences");
        dialog.setContentText("Locations (e.g., Beach, Mountains, City):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(location -> {
            currentPreferences.setLocationPreferences(location);
            savePreferences();
        });
    }

    @FXML
    void handleEditAccommodation(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentPreferences.getAccommodationTypes());
        dialog.setTitle("Edit Accommodation Types");
        dialog.setHeaderText("Enter your preferred accommodation types");
        dialog.setContentText("Types (e.g., Hotel, Hostel, Resort, Airbnb):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(accommodation -> {
            currentPreferences.setAccommodationTypes(accommodation);
            savePreferences();
        });
    }

    @FXML
    void handleEditDietary(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentPreferences.getDietaryRestrictions());
        dialog.setTitle("Edit Dietary Restrictions");
        dialog.setHeaderText("Enter your dietary restrictions");
        dialog.setContentText("Restrictions (e.g., Vegetarian, Vegan, Gluten-free):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(dietary -> {
            currentPreferences.setDietaryRestrictions(dietary);
            savePreferences();
        });
    }

    @FXML
    void handleEditClimate(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog(currentPreferences.getPreferredClimate());
        dialog.setTitle("Edit Preferred Climate");
        dialog.setHeaderText("Enter your preferred climate");
        dialog.setContentText("Climate (e.g., Tropical, Temperate, Cold):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(climate -> {
            currentPreferences.setPreferredClimate(climate);
            savePreferences();
        });
    }

    @FXML
    void handleEditGroup(ActionEvent event) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Solo", "Solo", "Couple", "Family", "Friends", "Business");
        dialog.setTitle("Edit Group Type");
        dialog.setHeaderText("Select your group type");
        dialog.setContentText("Group:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(group -> {
            currentPreferences.setGroupType(group);
            savePreferences();
        });
    }
    
    @FXML
    void handleChangePassword(ActionEvent event) {
        // Clear previous errors
        clearPasswordErrors();
        
        boolean isValid = true;
        
        // Validate current password
        if (!ValidationUtils.isNotEmpty(currentPasswordField.getText())) {
            showFieldError(currentPasswordField, currentPasswordError, 
                ValidationUtils.getRequiredFieldError("Current password"));
            isValid = false;
        }
        
        // Validate new password
        if (!ValidationUtils.isValidPassword(newPasswordField.getText())) {
            showFieldError(newPasswordField, newPasswordError, 
                ValidationUtils.isNotEmpty(newPasswordField.getText()) ? 
                ValidationUtils.getPasswordError() : 
                ValidationUtils.getRequiredFieldError("New password"));
            isValid = false;
        }
        
        // Validate password match
        if (!ValidationUtils.passwordsMatch(newPasswordField.getText(), confirmPasswordField.getText())) {
            showFieldError(confirmPasswordField, confirmPasswordError, 
                ValidationUtils.getPasswordMismatchError());
            isValid = false;
        }
        
        if (!isValid) {
            if (passwordStatusLabel != null) {
                passwordStatusLabel.setText("Please fix the errors above.");
                passwordStatusLabel.setStyle("-fx-text-fill: red;");
                passwordStatusLabel.setVisible(true);
            }
            return;
        }
        
        // Verify current password with database
        // Note: You'll need to add a method in UserService to verify password
        // For now, we'll just update the password
        try {
            currentUser.setPassword(newPasswordField.getText());
            boolean success = userService.updateUser(currentUser);
            
            if (success) {
                if (passwordStatusLabel != null) {
                    passwordStatusLabel.setText("Password changed successfully!");
                    passwordStatusLabel.setStyle("-fx-text-fill: green;");
                    passwordStatusLabel.setVisible(true);
                }
                // Clear password fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            } else {
                if (passwordStatusLabel != null) {
                    passwordStatusLabel.setText("Failed to change password.");
                    passwordStatusLabel.setStyle("-fx-text-fill: red;");
                    passwordStatusLabel.setVisible(true);
                }
            }
        } catch (Exception e) {
            if (passwordStatusLabel != null) {
                passwordStatusLabel.setText("Error: " + e.getMessage());
                passwordStatusLabel.setStyle("-fx-text-fill: red;");
                passwordStatusLabel.setVisible(true);
            }
        }
    }
    
    private void clearPasswordErrors() {
        clearFieldError(currentPasswordField, currentPasswordError);
        clearFieldError(newPasswordField, newPasswordError);
        clearFieldError(confirmPasswordField, confirmPasswordError);
        if (passwordStatusLabel != null) {
            passwordStatusLabel.setVisible(false);
        }
    }

    private void savePreferences() {
        boolean success = userPreferencesService.updatePreferences(currentPreferences);
        if (success) {
            updatePreferencesUI();
        } else {
            showAlert("Error", "Failed to save preferences.");
        }
    }

    @FXML
    void handleDeleteAccount(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure you want to delete your account?");
        alert.setContentText("This action is permanent and cannot be undone. Your profile and preferences will be permanently removed.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // 1. Delete preferences first
                userPreferencesService.deletePreferencesByUserId(currentUser.getUserId());
                
                // 2. Delete user
                boolean success = userService.deleteUser(currentUser.getUserId());
                
                if (success) {
                    // Redirect to login
                    handleLogout(event);
                } else {
                    showAlert("Error", "Failed to delete account. Please try again later.");
                }
            } catch (Exception e) {
                showAlert("Error", "An error occurred: " + e.getMessage());
            }
        }
    }

    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
