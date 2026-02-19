package tn.esprit.controllers.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import tn.esprit.entities.Destination;
import tn.esprit.services.DestinationService;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DestinationManagementController implements Initializable {

    // --- FXML Bindings ---
    @FXML private TextField searchField;
    @FXML private ComboBox<Destination.DestinationType> typeFilterCombo;
    @FXML private Button searchBtn;
    @FXML private Button refreshBtn;
    @FXML private Button addNewBtn;
    @FXML private Button userViewBtn;
    @FXML private Button activityBtn;
    @FXML private Button bookingBtn; // New Button
    @FXML private Button themeBtn; // Added missing field

    @FXML private TableView<Destination> destinationTable;
    @FXML private TableColumn<Destination, Long> colId;
    @FXML private TableColumn<Destination, String> colName;
    @FXML private TableColumn<Destination, String> colType;
    @FXML private TableColumn<Destination, String> colCountry;
    @FXML private TableColumn<Destination, String> colCity;
    @FXML private TableColumn<Destination, String> colBestSeason;
    @FXML private TableColumn<Destination, Double> colRating;
    @FXML private TableColumn<Destination, Void> colActions;

    // Form fields
    @FXML private TextField nameField;
    @FXML private ComboBox<Destination.DestinationType> typeCombo;
    @FXML private TextField countryField;
    @FXML private TextField cityField;
    @FXML private ComboBox<Destination.Season> seasonCombo;
    @FXML private TextArea descriptionArea;
    @FXML private TextField timezoneField;
    @FXML private TextField ratingField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Button clearBtn;

    @FXML private Label statusLabel;
    @FXML private Label totalDestinationsLabel;

    // --- Services & Data ---
    private DestinationService destinationService;
    private ObservableList<Destination> destinationList;
    private Destination selectedDestination;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        destinationService = new DestinationService();
        destinationList = FXCollections.observableArrayList();

        // 1. Setup UI
        initializeComboBoxes();
        setupTableColumns();
        setupActions();
        setupValidationListeners(); // Add real-time validation clearing

        // 2. Load Data
        loadDestinations();
    }

    private void initializeComboBoxes() {
        typeFilterCombo.setItems(FXCollections.observableArrayList(Destination.DestinationType.values()));
        typeCombo.setItems(FXCollections.observableArrayList(Destination.DestinationType.values()));
        seasonCombo.setItems(FXCollections.observableArrayList(Destination.Season.values()));
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("destinationId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colCountry.setCellValueFactory(new PropertyValueFactory<>("country"));
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colBestSeason.setCellValueFactory(new PropertyValueFactory<>("bestSeason"));
        colRating.setCellValueFactory(new PropertyValueFactory<>("averageRating"));

        // Custom Rating Display
        colRating.setCellFactory(column -> new TableCell<Destination, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f ⭐", item));
                    // Highlight high ratings
                    if (item >= 4.5) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if (item < 3.0) {
                        setStyle("-fx-text-fill: #e74c3c;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Action Buttons (Edit/Delete)
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Destination, Void> call(final TableColumn<Destination, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = createIconButton("/images/edit.png", "Edit", "green");
                    private final Button deleteBtn = createIconButton("/images/delete.png", "Delete", "red");
                    private final Button activitiesBtn = new Button("Activities");
                    private final HBox pane = new HBox(8, editBtn, deleteBtn, activitiesBtn);

                    {
                        editBtn.setOnAction(event -> {
                            Destination dest = getTableView().getItems().get(getIndex());
                            editDestination(dest);
                        });
                        deleteBtn.setOnAction(event -> {
                            Destination dest = getTableView().getItems().get(getIndex());
                            deleteDestination(dest);
                        });
                        
                        // Style for "View Activities" button
                        activitiesBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8; -fx-cursor: hand;");
                        activitiesBtn.setOnAction(event -> {
                            Destination dest = getTableView().getItems().get(getIndex());
                            ActivityManagementController.setPreSelectedDestinationId(dest.getDestinationId());
                            navigate("/fxml/admin/activity_management.fxml");
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        });
    }

    // Helper to create nice icon buttons (simulated if image missing)
    private Button createIconButton(String iconPath, String fallbackText, String color) {
        Button btn = new Button();
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            icon.setFitHeight(18);
            icon.setFitWidth(18);
            btn.setGraphic(icon);
        } catch (Exception e) {
            btn.setText(fallbackText.substring(0, 1)); // E or D
        }
        btn.setTooltip(new Tooltip(fallbackText));
        
        // Inline style for simplicity
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-border-color: #ddd; -fx-border-radius: 3;");
        
        // Hover effect
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #f0f0f0; -fx-cursor: hand; -fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-border-color: #bbb; -fx-border-radius: 3;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-border-color: #ddd; -fx-border-radius: 3;"));
        
        return btn;
    }

    private void updateThemeButtonText() {
        if (themeBtn != null) {
            themeBtn.setText(tn.esprit.utils.ThemeManager.isDarkMode() ? "☀" : "🌙");
            themeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-text-fill: " + 
                              (tn.esprit.utils.ThemeManager.isDarkMode() ? "#f1c40f" : "#2c3e50") + ";");
        }
    }

    private void setupActions() {
        // Search & Filter
        searchBtn.setOnAction(e -> handleSearch());
        refreshBtn.setOnAction(e -> refreshTable());
        typeFilterCombo.setOnAction(e -> handleFilter());

        // Real-time search (optional but nice)
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.trim().isEmpty()) refreshTable();
        });

        // Form Actions
        addNewBtn.setOnAction(e -> clearForm()); // Switching to 'Add Mode'
        saveBtn.setOnAction(e -> handleSave());
        cancelBtn.setOnAction(e -> clearForm());
        clearBtn.setOnAction(e -> clearForm());
        
        // Navigation
        userViewBtn.setOnAction(e -> navigate("/fxml/user/user_destinations.fxml"));
        activityBtn.setOnAction(e -> navigate("/fxml/admin/activity_management.fxml"));
        if (bookingBtn != null) bookingBtn.setOnAction(e -> navigate("/fxml/admin/booking_management.fxml"));
        
        // Theme Toggle
        if (themeBtn != null) {
             themeBtn.setOnAction(e -> {
                 tn.esprit.utils.ThemeManager.toggleTheme(themeBtn.getScene());
                 updateThemeButtonText();
             });
        }

        // Table Selection
        destinationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) populateForm(newV);
        });
    }

    private void setupValidationListeners() {
        // Remove error styling when user interacts
        removeErrorOnType(nameField);
        removeErrorOnType(typeCombo);
        removeErrorOnType(countryField);
        removeErrorOnType(seasonCombo);
        removeErrorOnType(ratingField);
    }

    private void removeErrorOnType(Control control) {
        if (control instanceof TextInputControl) {
            ((TextInputControl) control).textProperty().addListener((obs, o, n) -> control.getStyleClass().remove("error"));
        } else if (control instanceof ComboBox) {
            ((ComboBox<?>) control).valueProperty().addListener((obs, o, n) -> control.getStyleClass().remove("error"));
        }
    }

    // --- CRUD Operations ---

    public void loadDestinations() {
        try {
            List<Destination> list = destinationService.getAllDestinations();
            destinationList.setAll(list);
            destinationTable.setItems(destinationList);
            updateStats();
            statusLabel.setText("✅ Data loaded successfully.");
        } catch (Exception e) {
            showAlert("Error", "Failed to load destinations: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            refreshTable();
            return;
        }
        List<Destination> results = destinationService.searchDestinations(query);
        destinationList.setAll(results);
        statusLabel.setText("🔍 Found " + results.size() + " matches.");
    }

    private void handleFilter() {
        Destination.DestinationType type = typeFilterCombo.getValue();
        if (type == null) {
            refreshTable();
            return;
        }
        List<Destination> results = destinationService.getDestinationsByType(type);
        destinationList.setAll(results);
        statusLabel.setText("filter applied: " + type);
    }

    private void refreshTable() {
        loadDestinations();
        searchField.clear();
        typeFilterCombo.setValue(null);
    }

    private void handleSave() {
        if (!validateInput()) return;

        try {
            Destination d = (selectedDestination == null) ? new Destination() : selectedDestination;

            // Bind Form to Object
            d.setName(nameField.getText().trim());
            d.setType(typeCombo.getValue());
            d.setCountry(countryField.getText().trim());
            d.setCity(cityField.getText().trim());
            d.setBestSeason(seasonCombo.getValue());
            d.setDescription(descriptionArea.getText().trim());
            d.setTimezone(timezoneField.getText().trim());

            // Handle Review (optional, default 0)
            String ratingTxt = ratingField.getText().trim();
            d.setAverageRating(ratingTxt.isEmpty() ? 0.0 : Double.parseDouble(ratingTxt));

            boolean success;
            if (selectedDestination == null) {
                success = destinationService.addDestination(d);
                if (success) statusLabel.setText("✅ Inserted: " + d.getName());
            } else {
                success = destinationService.updateDestination(d);
                if (success) statusLabel.setText("✅ Updated: " + d.getName());
            }

            if (success) {
                loadDestinations();
                clearForm();
                showAlert("Success", "Operation completed successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Failure", "Database operation failed.", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            showAlert("Error", "An unexpected error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validateInput() {
        boolean valid = true;
        StringBuilder msg = new StringBuilder("Please correct the following:\n");

        if (nameField.getText().trim().isEmpty()) {
            markError(nameField);
            msg.append("- Name is required.\n");
            valid = false;
        }
        if (typeCombo.getValue() == null) {
            markError(typeCombo);
            msg.append("- Type is required.\n");
            valid = false;
        }
        if (countryField.getText().trim().isEmpty()) {
            markError(countryField);
            msg.append("- Country is required.\n");
            valid = false;
        }
        if (seasonCombo.getValue() == null) {
            markError(seasonCombo);
            msg.append("- Best Season is required.\n");
            valid = false;
        }
        
        // Validate Rating
        String r = ratingField.getText().trim();
        if (!r.isEmpty()) {
            try {
                double val = Double.parseDouble(r);
                if (val < 0 || val > 5) {
                    markError(ratingField);
                    msg.append("- Rating must be between 0 and 5.\n");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(ratingField);
                msg.append("- Rating must be a number.\n");
                valid = false;
            }
        }

        if (!valid) {
            showAlert("Invalid Input", msg.toString(), Alert.AlertType.WARNING);
        }
        return valid;
    }

    private void markError(Control node) {
        if (!node.getStyleClass().contains("error")) {
            node.getStyleClass().add("error");
        }
    }

    private void deleteDestination(Destination d) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Confirmation");
        alert.setHeaderText("Delete " + d.getName() + "?");
        alert.setContentText("Are you sure? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = destinationService.deleteDestination(d.getDestinationId());
            if (deleted) {
                statusLabel.setText("🗑 Deleted: " + d.getName());
                loadDestinations();
                clearForm();
            } else {
                showAlert("Error", "Could not delete. Check if it has related activities.", Alert.AlertType.ERROR);
            }
        }
    }

    private void editDestination(Destination d) {
        selectedDestination = d;
        populateForm(d);
        statusLabel.setText("✏ Editing: " + d.getName());
    }

    private void populateForm(Destination d) {
        nameField.setText(d.getName());
        typeCombo.setValue(d.getType());
        countryField.setText(d.getCountry());
        cityField.setText(d.getCity());
        seasonCombo.setValue(d.getBestSeason());
        descriptionArea.setText(d.getDescription());
        timezoneField.setText(d.getTimezone());
        ratingField.setText(String.valueOf(d.getAverageRating()));
        
        saveBtn.setText("Update");
        saveBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;"); // Orange for update
    }

    private void clearForm() {
        selectedDestination = null;
        nameField.clear();
        typeCombo.setValue(null);
        countryField.clear();
        cityField.clear();
        seasonCombo.setValue(null);
        descriptionArea.clear();
        timezoneField.clear();
        ratingField.clear();
        
        // Reset styles
        nameField.getStyleClass().remove("error");
        typeCombo.getStyleClass().remove("error");
        countryField.getStyleClass().remove("error");
        seasonCombo.getStyleClass().remove("error");
        ratingField.getStyleClass().remove("error");

        saveBtn.setText("Save");
        saveBtn.setStyle(""); // Reset style
        destinationTable.getSelectionModel().clearSelection();
        statusLabel.setText("Ready to add new destination.");
    }

    private void updateStats() {
        totalDestinationsLabel.setText("Total: " + destinationList.size());
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void navigate(String fxmlPath) {
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource(fxmlPath));
            userViewBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load " + fxmlPath + "\n\nReason: " + e.getCause(), Alert.AlertType.ERROR);
        }
    }
}