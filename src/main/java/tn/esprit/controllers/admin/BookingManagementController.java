package tn.esprit.controllers.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Booking;
import tn.esprit.entities.Booking.BookingStatus;
import tn.esprit.services.BookingService;
import tn.esprit.utils.ThemeManager;

import java.net.URL;
import java.util.ResourceBundle;

public class BookingManagementController implements Initializable {

    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> colRef;
    @FXML private TableColumn<Booking, String> colDest;
    @FXML private TableColumn<Booking, String> colUser;
    @FXML private TableColumn<Booking, String> colDate;
    @FXML private TableColumn<Booking, String> colStatus;
    @FXML private TableColumn<Booking, Double> colAmount;
    @FXML private TableColumn<Booking, Void> colActions;

    @FXML private TextField searchField;
    @FXML private Button refreshBtn;
    @FXML private Button backBtn;
    @FXML private Button themeBtn;
    @FXML private Label statusLabel;

    private BookingService bookingService;
    private tn.esprit.services.DestinationService destinationService;
    private ObservableList<Booking> bookingList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bookingService = new BookingService();
        destinationService = new tn.esprit.services.DestinationService();
        bookingList = FXCollections.observableArrayList();

        setupTable();
        setupActions();
        loadData();

        // Theme
        javafx.application.Platform.runLater(() -> {
            if (themeBtn != null && themeBtn.getScene() != null) {
                ThemeManager.applyTheme(themeBtn.getScene());
                updateThemeButtonText();
            }
        });
    }

    private void setupTable() {
        // Use lambdas for safer type conversion and display
        colRef.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBookingReference()));
        colDest.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDestinationName()));
        colUser.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getUserId())));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStartAt().toString()));
        
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().toString()));

        colAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        // Status coloring
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        BookingStatus status = BookingStatus.valueOf(item);
                        switch (status) {
                            case CONFIRMED: setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;"); break;
                            case PENDING: setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); break;
                            case CANCELLED: setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); break;
                            default: setStyle("-fx-text-fill: #2c3e50;");
                        }
                    } catch (IllegalArgumentException e) {
                        setStyle("-fx-text-fill: #2c3e50;");
                    }
                }
            }
        });

        // Actions: Edit / Confirm / Cancel / Delete
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("✏");
            private final Button confirmBtn = new Button("✅");
            private final Button cancelBtn = new Button("❌");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(5, editBtn, confirmBtn, cancelBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-font-size: 14px;");
                confirmBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #27ae60; -fx-cursor: hand; -fx-font-size: 14px;");
                cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-size: 14px;");
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-cursor: hand; -fx-font-size: 14px;");
                
                editBtn.setTooltip(new Tooltip("Edit Details"));
                confirmBtn.setTooltip(new Tooltip("Confirm Booking"));
                cancelBtn.setTooltip(new Tooltip("Cancel Booking"));
                deleteBtn.setTooltip(new Tooltip("Delete Booking"));

                editBtn.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                confirmBtn.setOnAction(e -> updateStatus(getTableView().getItems().get(getIndex()), BookingStatus.CONFIRMED));
                cancelBtn.setOnAction(e -> updateStatus(getTableView().getItems().get(getIndex()), BookingStatus.CANCELLED));
                deleteBtn.setOnAction(e -> deleteBooking(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void handleEdit(Booking b) {
        try {
            tn.esprit.entities.Destination dest = destinationService.getDestinationById(b.getDestinationId());
            if (dest == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Could not find destination info.");
                alert.showAndWait();
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/user/booking_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            tn.esprit.controllers.user.BookingDialogController controller = loader.getController();
            
            // Admin has full access (not restricted)
            controller.setBooking(b, dest, false);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Booking (Admin): " + b.getBookingReference());
            stage.setScene(new javafx.scene.Scene(root));
            
            if (ThemeManager.isDarkMode()) {
                ThemeManager.applyTheme(stage.getScene());
            }

            stage.showAndWait();
            loadData();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Could not open edit dialog: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void setupActions() {
        refreshBtn.setOnAction(e -> loadData());
        backBtn.setOnAction(e -> navigateBack());
        
        if (themeBtn != null) {
            themeBtn.setOnAction(e -> {
                ThemeManager.toggleTheme(themeBtn.getScene());
                updateThemeButtonText();
            });
        }
        
        searchField.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isEmpty()) {
                bookingTable.setItems(bookingList);
            } else {
                ObservableList<Booking> filtered = bookingList.filtered(b -> 
                    b.getBookingReference().toLowerCase().contains(n.toLowerCase()) ||
                    (b.getDestinationName() != null && b.getDestinationName().toLowerCase().contains(n.toLowerCase()))
                );
                bookingTable.setItems(filtered);
            }
        });
    }

    private void loadData() {
        bookingList.setAll(bookingService.getAllBookings());
        bookingTable.setItems(bookingList);
        statusLabel.setText("Loaded " + bookingList.size() + " bookings.");
    }

    private void updateStatus(Booking b, BookingStatus status) {
        if (bookingService.updateStatus(b.getBookingId(), status)) {
            // Reload data to ensure UI sync
            loadData();
            statusLabel.setText("Updated booking " + b.getBookingReference() + " to " + status);
        } else {
            statusLabel.setText("Failed to update status.");
        }
    }

    private void deleteBooking(Booking b) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Booking");
        alert.setHeaderText("Delete " + b.getBookingReference() + "?");
        alert.setContentText("Are you sure? This cannot be undone.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (bookingService.deleteBooking(b.getBookingId())) {
                loadData();
                statusLabel.setText("Deleted booking " + b.getBookingReference());
            } else {
                statusLabel.setText("Failed to delete booking.");
            }
        }
    }

    private void navigateBack() {
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/admin/destination_management.fxml"));
            backBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateThemeButtonText() {
        if (themeBtn != null) {
            themeBtn.setText(ThemeManager.isDarkMode() ? "☀" : "🌙");
            themeBtn.getStyleClass().removeAll("action-button", "action-button-secondary");
            if (!themeBtn.getStyleClass().contains("theme-toggle-btn")) {
                themeBtn.getStyleClass().add("theme-toggle-btn");
            }
        }
    }
}
