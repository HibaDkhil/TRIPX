package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ButtonType; // Correct import
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import tn.esprit.entities.Booking;
import tn.esprit.services.BookingService;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.ThemeManager;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserBookingsController implements Initializable {

    @FXML private ListView<Booking> bookingsList;
    @FXML private Button backBtn;
    @FXML private Button refreshBtn; // Added
    @FXML private Button themeBtn;
    @FXML private Label statusLabel;

    private BookingService bookingService;
    private tn.esprit.services.DestinationService destinationService; // Added
    private ObservableList<Booking> myBookings;
    private tn.esprit.entities.User currentUser;

    public void setCurrentUser(tn.esprit.entities.User user) {
        this.currentUser = user;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        bookingService = new BookingService();
        destinationService = new tn.esprit.services.DestinationService(); // Init
        myBookings = FXCollections.observableArrayList();

        setupList();
        
        // Load data initially using SessionManager
        loadData();
        
        if (backBtn != null) {
            backBtn.setOnAction(e -> navigateBack());
        }
    }

    private void setupList() {
        bookingsList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // ... (Card creation code same as before) ...
                    // --- Card Container ---
                    VBox card = new VBox(8);
                    card.setStyle("-fx-padding: 15; -fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1); -fx-border-color: #eee; -fx-border-radius: 8;");
                    
                    if (ThemeManager.isDarkMode()) {
                        card.setStyle("-fx-padding: 15; -fx-background-color: #363636; -fx-background-radius: 8; -fx-border-color: #444; -fx-border-radius: 8;");
                    }

                    // --- Header: Destination & Cost ---
                    HBox header = new HBox(10);
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    Label destLbl = new Label("✈ " + item.getDestinationName());
                    destLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
                    if (ThemeManager.isDarkMode()) destLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #ecf0f1;");
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Label amountLbl = new Label(String.format("$%.2f", item.getTotalAmount()));
                    amountLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #27ae60;");

                    header.getChildren().addAll(destLbl, spacer, amountLbl);

                    // --- Details Row ---
                    HBox details = new HBox(15);
                    Label refLbl = new Label("Ref: " + item.getBookingReference());
                    refLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
                    
                    Label dateLbl = new Label("📅 " + item.getStartAt().toString().substring(0, 10));
                    dateLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
                    
                    details.getChildren().addAll(refLbl, dateLbl);

                    // --- Status & Actions Row ---
                    HBox actions = new HBox(10);
                    actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    Label statusLbl = new Label();
                    statusLbl.setStyle("-fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4;");
                    
                    // Button declarations
                    Button editBtn = new Button("✏");
                    editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498db; -fx-cursor: hand; -fx-font-size: 14px;");
                    editBtn.setTooltip(new Tooltip("Edit Booking Details"));
                    editBtn.setOnAction(e -> handleEdit(item));

                    Button deleteBtn = new Button("🗑");
                    deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #95a5a6; -fx-cursor: hand; -fx-font-size: 14px;");
                    deleteBtn.setTooltip(new Tooltip("Delete Booking History"));
                    deleteBtn.setOnAction(e -> handleDelete(item));

                    // Status Logic
                    switch (item.getStatus()) {
                        case CONFIRMED:
                            statusLbl.setText("✅ CONFIRMED");
                            statusLbl.setStyle(statusLbl.getStyle() + "-fx-background-color: #d5f5e3; -fx-text-fill: #2ecc71;");
                            if (ThemeManager.isDarkMode()) statusLbl.setStyle("-fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-background-color: #1e3a2a; -fx-text-fill: #2ecc71;");
                            
                            Button payBtn = new Button("💳 Proceed to Payment");
                            payBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                            payBtn.setOnAction(e -> handlePayment(item));
                            actions.getChildren().add(payBtn);
                            break;
                            
                        case CANCELLED:
                            statusLbl.setText("❌ CANCELLED");
                            statusLbl.setStyle(statusLbl.getStyle() + "-fx-background-color: #fadbd8; -fx-text-fill: #e74c3c;");
                            if (ThemeManager.isDarkMode()) statusLbl.setStyle("-fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-background-color: #4a2323; -fx-text-fill: #e74c3c;");
                            editBtn.setDisable(true); // Cannot edit cancelled
                            break;
                            
                        case COMPLETED:
                            statusLbl.setText("🏁 COMPLETED");
                             // Style...
                             editBtn.setDisable(true); // Cannot edit completed
                            break;

                        default: // PENDING
                            statusLbl.setText("⏳ PENDING");
                            statusLbl.setStyle(statusLbl.getStyle() + "-fx-background-color: #fce8d2; -fx-text-fill: #e67e22;");
                            if (ThemeManager.isDarkMode()) statusLbl.setStyle("-fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-background-color: #4a3820; -fx-text-fill: #e67e22;");
                            break;
                    }
                    
                    // Spacer
                    Region actionSpacer = new Region();
                    HBox.setHgrow(actionSpacer, Priority.ALWAYS);
                    
                    actions.getChildren().add(0, statusLbl); 
                    actions.getChildren().addAll(actionSpacer, editBtn, deleteBtn); 

                    card.getChildren().addAll(header, details, new Separator(), actions);
                    setGraphic(card);
                }
            }
        });
    }
    
    private void handleDelete(Booking b) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Booking");
        alert.setHeaderText("Remove " + b.getDestinationName() + "?");
        alert.setContentText("Are you sure you want to remove this booking from your history?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (bookingService.deleteBooking(b.getBookingId())) {
                loadData();
            } else {
                statusLabel.setText("Failed to delete booking.");
            }
        }
    }

    private void handlePayment(Booking b) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payment");
        alert.setHeaderText("Payment for " + b.getBookingReference());
        alert.setContentText("This feature is coming soon! Total to pay: $" + b.getTotalAmount());
        alert.showAndWait();
    }

    private void handleEdit(Booking b) {
        try {
            // Fetch full destination for context
            tn.esprit.entities.Destination dest = destinationService.getDestinationById(b.getDestinationId()); // Fixed cast
            if (dest == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Could not find destination info.");
                alert.showAndWait();
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/user/booking_dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            BookingDialogController controller = loader.getController();
            
            // Determine restriction mode
            boolean restricted = (b.getStatus() == Booking.BookingStatus.CONFIRMED);
            
            controller.setBooking(b, dest, restricted);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Edit Booking: " + b.getBookingReference());
            stage.setScene(new javafx.scene.Scene(root));
            
            if (ThemeManager.isDarkMode()) {
                ThemeManager.applyTheme(stage.getScene());
            }

            stage.showAndWait();
            loadData(); // Refresh after edit
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Could not open edit dialog: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadData() {
        myBookings.setAll(bookingService.getBookingsByUser(SessionManager.getCurrentUserId()));
        bookingsList.setItems(myBookings);
        statusLabel.setText("You have " + myBookings.size() + " bookings.");
    }

    private void navigateBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/user/home.fxml"));
            javafx.scene.Parent root = loader.load();
            
            HomeController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }
            
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
