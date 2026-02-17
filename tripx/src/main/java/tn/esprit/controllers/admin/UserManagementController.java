package tn.esprit.controllers.admin;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UserManagementController {

    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private Button sortBtn;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    @FXML private Button slideArrowBtn;
    @FXML private VBox statsPanel;
    @FXML private Label totalUsersStat;
    @FXML private Label adminCountStat;
    @FXML private Label userCountStat;
    @FXML private Label newUsersStat;

    private UserService userService;
    private ObservableList<User> userList;
    private boolean statsVisible = false;

    // Ajoute cette méthode pour recevoir les données de l'utilisateur connecté
    public void setUserData(User user, String role) {
        // Tu peux stocker ces données si nécessaire pour plus tard
        System.out.println("UserManagement opened by: " + user.getEmail() + " with role: " + role);

        // Optionnel : si tu veux restreindre l'accès basé sur le rôle
        if (!"SUPER_ADMIN".equals(role) && !"USER_ADMIN".equals(role)) {
            // Masquer certaines fonctionnalités pour les admins non autorisés
            // Par exemple, désactiver le bouton delete pour les non-admins
        }
    }

    @FXML
    public void initialize() {
        userService = new UserService();

        // Setup table columns
        setupTableColumns();

        // Load REAL users from database
        loadUsersFromDatabase();

        // Setup search functionality
        setupSearch();

        // Setup slide arrow
        setupSlideArrow();

        // Update statistics
        updateStatistics();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Status column
        statusColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String status = "Active";
            return javafx.beans.binding.Bindings.createStringBinding(() -> status);
        });

        // Actions column with buttons
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button suspendBtn = new Button("Suspend");
            private final Button banBtn = new Button("Ban");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, suspendBtn, banBtn);

            {
                // Style buttons
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 5 10;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 5 10;");
                suspendBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 5 10;");
                banBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 5 10;");

                // Add hover effects
                addHoverEffect(editBtn, "#2196F3", "#1976D2");
                addHoverEffect(deleteBtn, "#f44336", "#d32f2f");
                addHoverEffect(suspendBtn, "#ff9800", "#f57c00");
                addHoverEffect(banBtn, "#333333", "#000000");

                // Set actions
                editBtn.setOnAction(e -> handleEdit(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDelete(getTableRow().getItem()));
                suspendBtn.setOnAction(e -> handleSuspend(getTableRow().getItem()));
                banBtn.setOnAction(e -> handleBan(getTableRow().getItem()));

                pane.setAlignment(Pos.CENTER);
                pane.setPadding(new Insets(5));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void addHoverEffect(Button btn, String normalColor, String hoverColor) {
        btn.setOnMouseEntered(e -> {
            btn.setStyle(btn.getStyle().replace(normalColor, hoverColor) + "; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(btn.getStyle().replace(hoverColor, normalColor) + "; -fx-cursor: default;");
        });
    }

    private void loadUsersFromDatabase() {
        List<User> users = userService.getAllUsers();
        userList = FXCollections.observableArrayList(users);
        userTable.setItems(userList);
        System.out.println("Loaded " + users.size() + " users from database");
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers(newValue);
        });

        searchBtn.setOnAction(e -> filterUsers(searchField.getText()));
        sortBtn.setOnAction(e -> sortUsers());
    }

    private void filterUsers(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            userTable.setItems(userList);
            return;
        }

        ObservableList<User> filteredList = FXCollections.observableArrayList();
        String lowerSearch = searchText.toLowerCase();

        for (User user : userList) {
            if (user.getFirstName().toLowerCase().contains(lowerSearch) ||
                    user.getLastName().toLowerCase().contains(lowerSearch) ||
                    user.getEmail().toLowerCase().contains(lowerSearch) ||
                    String.valueOf(user.getUserId()).contains(lowerSearch)) {
                filteredList.add(user);
            }
        }

        userTable.setItems(filteredList);
    }

    private void sortUsers() {
        FXCollections.sort(userTable.getItems(),
                (u1, u2) -> Integer.compare(u1.getUserId(), u2.getUserId()));
    }

    private void setupSlideArrow() {
        statsPanel.setVisible(false);
        statsPanel.setManaged(false);

        slideArrowBtn.setOnAction(e -> toggleStatsPanel());
    }

    private void toggleStatsPanel() {
        statsVisible = !statsVisible;
        statsPanel.setVisible(statsVisible);
        statsPanel.setManaged(statsVisible);
        slideArrowBtn.setText(statsVisible ? "▶" : "◀");

        if (statsVisible) {
            updateStatistics();
        }
    }

    private void updateStatistics() {
        int total = userList.size();
        int admins = 0;
        int regularUsers = 0;

        for (User user : userList) {
            if ("SUPER_ADMIN".equals(user.getRole()) || "ADMIN".equals(user.getRole())) {
                admins++;
            } else {
                regularUsers++;
            }
        }

        totalUsersStat.setText(String.valueOf(total));
        adminCountStat.setText(String.valueOf(admins));
        userCountStat.setText(String.valueOf(regularUsers));
        newUsersStat.setText("5"); // Placeholder
    }

    // ==================== DELETE OPERATION ====================
    private void handleDelete(User user) {
        if (user == null) return;

        // Create confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete this user?");
        alert.setContentText("User: " + user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")");

        // Style the dialog buttons
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getButtonTypes().stream()
                .map(dialogPane::lookupButton)
                .forEach(button -> {
                    button.setStyle("-fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");

                    if (button instanceof Button) {
                        Button btn = (Button) button;
                        if (btn.getText().equals("OK")) {
                            btn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");
                            // Add hover for YES button
                            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-cursor: hand;"));
                            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-cursor: default;"));
                        } else if (btn.getText().equals("Cancel")) {
                            btn.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");
                            // Add hover for NO button
                            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-cursor: hand;"));
                            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-cursor: default;"));
                        }
                    }
                });

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Perform delete
            boolean deleted = userService.deleteUser(user.getUserId());

            if (deleted) {
                // Show success message
                showSuccessMessage("User deleted successfully!");

                // Refresh the table
                loadUsersFromDatabase();
                updateStatistics();
            } else {
                // Show error message
                showErrorMessage("Failed to delete user!");
            }
        }
    }

    // ==================== EDIT OPERATION ====================
    private void handleEdit(User user) {
        if (user == null) return;

        try {
            // Create a new stage (popup window) for editing
            Stage editStage = new Stage();
            editStage.setTitle("Edit User - " + user.getFirstName() + " " + user.getLastName());

            // Create the edit form
            VBox editForm = new VBox(20);
            editForm.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 15;");
            editForm.setAlignment(Pos.TOP_CENTER);
            editForm.setPrefWidth(500);
            editForm.setPrefHeight(500);

            // Title
            Label titleLabel = new Label("✏️ Edit User");
            titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Poppins'; -fx-text-fill: #2c3e50;");

            // Form fields
            GridPane formGrid = new GridPane();
            formGrid.setHgap(15);
            formGrid.setVgap(15);
            formGrid.setAlignment(Pos.CENTER);

            // First Name
            Label firstNameLabel = new Label("First Name:");
            firstNameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
            TextField firstNameField = new TextField(user.getFirstName());
            firstNameField.setStyle("-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 14px; -fx-font-family: 'Poppins';");
            firstNameField.setPrefWidth(250);

            // Last Name
            Label lastNameLabel = new Label("Last Name:");
            lastNameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
            TextField lastNameField = new TextField(user.getLastName());
            lastNameField.setStyle("-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 14px; -fx-font-family: 'Poppins';");
            lastNameField.setPrefWidth(250);

            // Email
            Label emailLabel = new Label("Email:");
            emailLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
            TextField emailField = new TextField(user.getEmail());
            emailField.setStyle("-fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 14px; -fx-font-family: 'Poppins';");
            emailField.setPrefWidth(250);

            // Role
            Label roleLabel = new Label("Role:");
            roleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
            ComboBox<String> roleCombo = new ComboBox<>();
            roleCombo.getItems().addAll("USER", "ADMIN", "SUPER_ADMIN");
            roleCombo.setValue(user.getRole() != null ? user.getRole() : "USER");
            roleCombo.setStyle("-fx-background-radius: 8; -fx-padding: 5; -fx-font-size: 14px; -fx-font-family: 'Poppins';");
            roleCombo.setPrefWidth(250);

            // Add to grid
            formGrid.add(firstNameLabel, 0, 0);
            formGrid.add(firstNameField, 1, 0);
            formGrid.add(lastNameLabel, 0, 1);
            formGrid.add(lastNameField, 1, 1);
            formGrid.add(emailLabel, 0, 2);
            formGrid.add(emailField, 1, 2);
            formGrid.add(roleLabel, 0, 3);
            formGrid.add(roleCombo, 1, 3);

            // Buttons
            HBox buttonBox = new HBox(15);
            buttonBox.setAlignment(Pos.CENTER);

            Button saveBtn = new Button("Save Changes");
            saveBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30;");

            Button cancelBtn = new Button("Cancel");
            cancelBtn.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30;");

            // Hover effects
            saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30; -fx-cursor: hand;"));
            saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30; -fx-cursor: default;"));

            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30; -fx-cursor: hand;"));
            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle("-fx-background-color: #9e9e9e; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 12 30; -fx-cursor: default;"));

            buttonBox.getChildren().addAll(saveBtn, cancelBtn);

            // Add everything to the form
            editForm.getChildren().addAll(titleLabel, formGrid, buttonBox);

            // Save action
            saveBtn.setOnAction(e -> {
                // Update user object
                user.setFirstName(firstNameField.getText());
                user.setLastName(lastNameField.getText());
                user.setEmail(emailField.getText());
                user.setRole(roleCombo.getValue());

                // Call update service
                boolean updated = userService.updateUser(user);

                if (updated) {
                    showSuccessMessage("User updated successfully!");
                    editStage.close();
                    loadUsersFromDatabase();
                    updateStatistics();
                } else {
                    showErrorMessage("Failed to update user!");
                }
            });

            // Cancel action
            cancelBtn.setOnAction(e -> editStage.close());

            // Create scene and show
            Scene scene = new Scene(editForm);
            editStage.setScene(scene);
            editStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            editStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Error opening edit window!");
        }
    }

    // ==================== HELPER METHODS ====================
    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: #4cccad; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");

        alert.showAndWait();
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");
        dialogPane.lookupButton(ButtonType.OK).setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 20; -fx-font-family: 'Poppins'; -fx-font-weight: bold;");

        alert.showAndWait();
    }

    // Placeholder methods
    private void handleSuspend(User user) {
        System.out.println("Suspend user: " + user.getEmail());
        // TODO: Implement suspend user
    }

    private void handleBan(User user) {
        System.out.println("Ban user: " + user.getEmail());
        // TODO: Implement ban user
    }
}