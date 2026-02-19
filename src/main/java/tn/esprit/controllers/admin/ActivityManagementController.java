package tn.esprit.controllers.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.esprit.entities.Activity;
import tn.esprit.entities.Destination;
import tn.esprit.services.ActivityService;
import tn.esprit.services.DestinationService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ActivityManagementController implements Initializable {

    @FXML private Label totalActivitiesLabel;
    @FXML private Button backBtn;
    @FXML private TextField searchField;
    @FXML private ComboBox<Destination> destFilterCombo;
    @FXML private Button searchBtn;
    @FXML private Button refreshBtn;
    @FXML private Button addNewBtn;
    @FXML private Label statusLabel;
    @FXML private Button themeBtn;

    @FXML private TableView<Activity> activityTable;
    @FXML private TableColumn<Activity, String> colName;
    @FXML private TableColumn<Activity, String> colDest;
    @FXML private TableColumn<Activity, String> colCategory;
    @FXML private TableColumn<Activity, Double> colPrice;
    @FXML private TableColumn<Activity, Integer> colCapacity;
    @FXML private TableColumn<Activity, Void> colActions;

    @FXML private ComboBox<Destination> destCombo;
    @FXML private TextField nameField;
    @FXML private ComboBox<Activity.ActivityCategory> categoryCombo;
    @FXML private TextField priceField;
    @FXML private TextField capacityField;
    @FXML private TextArea descArea;
    @FXML private Button saveBtn;
    @FXML private Button clearBtn;

    private ActivityService activityService;
    private DestinationService destinationService;
    private ObservableList<Activity> activityList;
    private Activity selectedActivity;

    // To pre-select a destination when navigating from Destination View
    private static Long preSelectedDestinationId = null;

    public static void setPreSelectedDestinationId(Long id) {
        preSelectedDestinationId = id;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            activityService = new ActivityService();
            destinationService = new DestinationService();
            activityList = FXCollections.observableArrayList();

            setupUI();
            setupValidationListeners();
            loadData();

            if (preSelectedDestinationId != null) {
                applyPreSelection();
            }
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN ActivityManagementController.initialize:");
            e.printStackTrace();
            throw e; // Rethrow to let FXMLLoader catch it
        }
    }

    private void setupUI() {
        // Combos
        categoryCombo.setItems(FXCollections.observableArrayList(Activity.ActivityCategory.values()));
        
        List<Destination> destinations = destinationService.getAllDestinations();
        ObservableList<Destination> destList = FXCollections.observableArrayList(destinations);
        destCombo.setItems(destList);
        destFilterCombo.setItems(destList);

        // Destination ComboBox Display
        Callback<ListView<Destination>, ListCell<Destination>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(Destination item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        };
        destCombo.setButtonCell(cellFactory.call(null));
        destCombo.setCellFactory(cellFactory);
        destFilterCombo.setButtonCell(cellFactory.call(null));
        destFilterCombo.setCellFactory(cellFactory);

        // Columns
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDest.setCellValueFactory(new PropertyValueFactory<>("destinationName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));

        setupActionsColumn();

        // Listeners
        backBtn.setOnAction(e -> navigateBack());
        searchBtn.setOnAction(e -> handleSearch());
        refreshBtn.setOnAction(e -> loadData());
        addNewBtn.setOnAction(e -> clearForm());
        saveBtn.setOnAction(e -> handleSave());
        clearBtn.setOnAction(e -> clearForm());
        
        destFilterCombo.setOnAction(e -> handleFilter());

        activityTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) populateForm(n);
        });
        
        // Theme Toggle
        if (themeBtn != null) {
             themeBtn.setOnAction(e -> {
                 tn.esprit.utils.ThemeManager.toggleTheme(themeBtn.getScene());
                 updateThemeButtonText();
             });
        }
        
        // Defer theme apply
        javafx.application.Platform.runLater(() -> {
            if (themeBtn != null && themeBtn.getScene() != null) {
                tn.esprit.utils.ThemeManager.applyTheme(themeBtn.getScene());
                updateThemeButtonText();
            }
        });
    }

    private void updateThemeButtonText() {
        if (themeBtn != null) {
            themeBtn.setText(tn.esprit.utils.ThemeManager.isDarkMode() ? "☀" : "🌙");
            themeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-text-fill: " + 
                              (tn.esprit.utils.ThemeManager.isDarkMode() ? "#f1c40f" : "#2c3e50") + ";");
        }
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏"); // Pencil Icon
            private final Button deleteBtn = new Button("🗑"); // Trash Icon
            private final HBox pane = new HBox(8, editBtn, deleteBtn);

            {
                // Style Edit Button
                editBtn.getStyleClass().add("table-action-button");
                editBtn.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 14px; -fx-background-color: transparent;");
                editBtn.setTooltip(new Tooltip("Edit"));

                // Style Delete Button
                deleteBtn.getStyleClass().add("table-action-button");
                deleteBtn.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-background-color: transparent;");
                editBtn.setTooltip(new Tooltip("Delete"));

                editBtn.setOnAction(e -> {
                    populateForm(getTableView().getItems().get(getIndex()));
                });

                deleteBtn.setOnAction(e -> {
                    Activity a = getTableView().getItems().get(getIndex());
                    deleteActivity(a);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadData() {
        List<Activity> list = activityService.getAllActivities();
        activityList.setAll(list);
        activityTable.setItems(activityList);
        totalActivitiesLabel.setText("Total: " + list.size());
        statusLabel.setText("✅ Loaded activities");
    }

    private void applyPreSelection() {
        for (Destination d : destFilterCombo.getItems()) {
            if (d.getDestinationId().equals(preSelectedDestinationId)) {
                destFilterCombo.setValue(d);
                handleFilter();
                break;
            }
        }
        preSelectedDestinationId = null; // consume
    }

    private void handleSearch() {
        String k = searchField.getText().trim();
        if (k.isEmpty()) {
            loadData();
            return;
        }
        activityList.setAll(activityService.searchActivities(k));
    }

    private void handleFilter() {
        Destination d = destFilterCombo.getValue();
        if (d != null) {
            activityList.setAll(activityService.getActivitiesByDestination(d.getDestinationId()));
        } else {
            loadData();
        }
    }

    private void handleSave() {
        if (!validate()) return;

        try {
            Activity a = (selectedActivity == null) ? new Activity() : selectedActivity;
            
            a.setName(nameField.getText().trim());
            a.setDestinationId(destCombo.getValue().getDestinationId());
            a.setCategory(categoryCombo.getValue());
            a.setPrice(Double.parseDouble(priceField.getText().trim()));
            a.setCapacity(Integer.parseInt(capacityField.getText().trim()));
            a.setDescription(descArea.getText().trim());

            boolean success;
            if (selectedActivity == null) {
                success = activityService.addActivity(a);
            } else {
                success = activityService.updateActivity(a);
            }

            if (success) {
                loadData();
                clearForm();
                statusLabel.setText("✅ Saved successfully");
            } else {
                statusLabel.setText("❌ Save failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌ Error: " + e.getMessage());
        }
    }

    private void setupValidationListeners() {
        removeErrorOnType(nameField);
        removeErrorOnType(priceField);
        removeErrorOnType(capacityField);
        removeErrorOnType(destCombo);
        removeErrorOnType(categoryCombo);
    }

    private void removeErrorOnType(Control control) {
        if (control instanceof TextInputControl) {
            ((TextInputControl) control).textProperty().addListener((obs, o, n) -> control.getStyleClass().remove("error"));
        } else if (control instanceof ComboBox) {
            ((ComboBox<?>) control).valueProperty().addListener((obs, o, n) -> control.getStyleClass().remove("error"));
        }
    }

    private boolean validate() {
        boolean valid = true;
        StringBuilder msg = new StringBuilder("Please fix the following errors:\n");

        if (destCombo.getValue() == null) {
            markError(destCombo);
            msg.append("- Destination is required.\n");
            valid = false;
        }

        if (nameField.getText().trim().isEmpty()) {
            markError(nameField);
            msg.append("- Activity Name is required.\n");
            valid = false;
        }

        if (categoryCombo.getValue() == null) {
            markError(categoryCombo);
            msg.append("- Category is required.\n");
            valid = false;
        }

        try {
            double p = Double.parseDouble(priceField.getText().trim());
            if (p < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            markError(priceField);
            msg.append("- Price must be a positive number.\n");
            valid = false;
        }

        try {
            int c = Integer.parseInt(capacityField.getText().trim());
            if (c <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            markError(capacityField);
            msg.append("- Capacity must be a positive integer.\n");
            valid = false;
        }

        if (!valid) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation Error");
            alert.setHeaderText(null);
            alert.setContentText(msg.toString());
            alert.showAndWait();
        }

        return valid;
    }

    private void populateForm(Activity a) {
        selectedActivity = a;
        nameField.setText(a.getName());
        priceField.setText(String.valueOf(a.getPrice()));
        capacityField.setText(String.valueOf(a.getCapacity()));
        descArea.setText(a.getDescription());
        categoryCombo.setValue(a.getCategory());
        
        // Find destination object
        for (Destination d : destCombo.getItems()) {
            if (d.getDestinationId().equals(a.getDestinationId())) {
                destCombo.setValue(d);
                break;
            }
        }
        
        saveBtn.setText("Update");
    }

    private void markError(Control node) {
        if (!node.getStyleClass().contains("error")) {
            node.getStyleClass().add("error");
        }
    }

    private void clearForm() {
        selectedActivity = null;
        nameField.clear();
        priceField.clear();
        capacityField.clear();
        descArea.clear();
        destCombo.setValue(null);
        categoryCombo.setValue(null);
        saveBtn.setText("Save");
        activityTable.getSelectionModel().clearSelection();
        
        // Clear errors
        nameField.getStyleClass().remove("error");
        priceField.getStyleClass().remove("error");
        capacityField.getStyleClass().remove("error");
        destCombo.getStyleClass().remove("error");
        categoryCombo.getStyleClass().remove("error");
        
        statusLabel.setText("Ready");
    }

    private void deleteActivity(Activity a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Delete " + a.getName() + "?");
        if (alert.showAndWait().get() == ButtonType.OK) {
            if (activityService.deleteActivity(a.getActivityId())) {
                loadData();
                clearForm();
            }
        }
    }

    private void navigateBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin/destination_management.fxml"));
            backBtn.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
