package tn.esprit.controllers.admin;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.shape.Circle;


public class SettingsController {

    @FXML private TextField claimSubjectField;
    @FXML private ComboBox<String> claimCategoryCombo;
    @FXML private TextArea claimMessageArea;
    @FXML private Button priorityLowBtn, priorityMediumBtn, priorityHighBtn, priorityUrgentBtn;
    @FXML private Button submitClaimBtn, attachFileBtn;

    @FXML private TableView ticketsTable;
    @FXML private TableColumn ticketIdColumn, ticketSubjectColumn, ticketStatusColumn, ticketPriorityColumn, ticketDateColumn;

    // REMOVED ALL PROFILE-RELATED FIELDS
    // REMOVED: profileNameLabel, profileRoleLabel, profileEmailLabel
    // REMOVED: profileFirstNameField, profileLastNameField, profileEmailField
    // REMOVED: currentPwdField, newPwdField, confirmPwdField
    // REMOVED: saveProfileBtn, changePwdBtn
    // REMOVED: memberSinceLabel, lastLoginLabel, credentialRoleLabel

    @FXML private Tab adminPanelTab;
    @FXML private TableView allClaimsTable;
    @FXML private TableColumn allClaimIdColumn, allClaimAdminColumn, allClaimSubjectColumn,
            allClaimStatusColumn, allClaimPriorityColumn, allClaimDateColumn, allClaimActionColumn;

    @FXML private TabPane tabPane;

    private UserService userService;
    private User currentUser;
    private String userRole;
    private String selectedPriority = "Medium";

    public void setUserData(User user, String role, int selectedTab) {
        this.currentUser = user;
        this.userRole = role;
        // REMOVED loadUserProfile() call
        checkAdminAccess();

        // Sélectionner l'onglet approprié
        if (tabPane != null) {
            tabPane.getSelectionModel().select(selectedTab);
        }
    }

    // Garder l'ancienne méthode pour compatibilité
    public void setUserData(User user, String role) {
        setUserData(user, role, 0);
    }

    @FXML
    public void initialize() {
        userService = new UserService();

        // Setup claim category combo
        claimCategoryCombo.setItems(FXCollections.observableArrayList(
                "Bug Report", "Feature Request", "Account Issue", "Permission Problem", "Other"
        ));
        claimCategoryCombo.getSelectionModel().selectFirst();

        // Setup priority buttons
        setupPriorityButtons();

        // Setup submit claim button
        submitClaimBtn.setOnAction(e -> submitClaim());

        // REMOVED profile save and change password setup

        // Setup ticket tables
        setupTicketTables();
    }

    private void setupPriorityButtons() {
        // Low priority
        priorityLowBtn.setOnAction(e -> {
            selectedPriority = "Low";
            updatePriorityStyles();
            priorityLowBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
        });

        // Medium priority
        priorityMediumBtn.setOnAction(e -> {
            selectedPriority = "Medium";
            updatePriorityStyles();
            priorityMediumBtn.setStyle("-fx-background-color: #ed6c02; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
        });

        // High priority
        priorityHighBtn.setOnAction(e -> {
            selectedPriority = "High";
            updatePriorityStyles();
            priorityHighBtn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
        });

        // Urgent priority
        priorityUrgentBtn.setOnAction(e -> {
            selectedPriority = "Urgent";
            updatePriorityStyles();
            priorityUrgentBtn.setStyle("-fx-background-color: #7b1fa2; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
        });

        // Set default (Medium)
        priorityMediumBtn.setStyle("-fx-background-color: #ed6c02; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
    }

    private void updatePriorityStyles() {
        priorityLowBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
        priorityMediumBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
        priorityHighBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
        priorityUrgentBtn.setStyle("-fx-background-color: #9c27b0; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
    }

    // REMOVED loadUserProfile() method

    private void checkAdminAccess() {
        // Only show Admin Panel tab for SUPER_ADMIN
        if (!"SUPER_ADMIN".equals(userRole)) {
            adminPanelTab.setDisable(true);
            // Remove the tab for non-super admins
            if (tabPane != null) {
                tabPane.getTabs().remove(adminPanelTab);
            }
        }
    }

    private void submitClaim() {
        String subject = claimSubjectField.getText().trim();
        String category = claimCategoryCombo.getValue();
        String message = claimMessageArea.getText().trim();

        if (subject.isEmpty() || message.isEmpty()) {
            showAlert("Error", "Please fill in all required fields", Alert.AlertType.ERROR);
            return;
        }

        // TODO: Save claim to database with REAL user ID
        System.out.println("===== CLAIM SUBMITTED =====");
        System.out.println("User ID: " + (currentUser != null ? currentUser.getUserId() : "Unknown"));
        System.out.println("From: " + (currentUser != null ? currentUser.getEmail() : "Unknown") + " (" + userRole + ")");
        System.out.println("Name: " + (currentUser != null ? currentUser.getFirstName() + " " + currentUser.getLastName() : "Unknown"));
        System.out.println("Subject: " + subject);
        System.out.println("Category: " + category);
        System.out.println("Priority: " + selectedPriority);
        System.out.println("Message: " + message);
        System.out.println("===========================");

        showAlert("Success", "Your claim has been submitted to the Super Admin!", Alert.AlertType.INFORMATION);

        // Clear form
        claimSubjectField.clear();
        claimMessageArea.clear();
        claimCategoryCombo.getSelectionModel().selectFirst();
        selectedPriority = "Medium";
        updatePriorityStyles();
        priorityMediumBtn.setStyle("-fx-background-color: #ed6c02; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20;");
    }

    // REMOVED saveProfile() method
    // REMOVED changePassword() method

    private void setupTicketTables() {
        // TODO: Setup ticket table columns
        System.out.println("Ticket tables setup");
    }

    // ============ HELPER METHODS ============
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: #4cccad; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");
        }

        alert.showAndWait();
    }
}