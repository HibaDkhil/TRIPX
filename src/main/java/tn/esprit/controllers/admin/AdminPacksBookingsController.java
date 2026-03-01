package tn.esprit.controllers.admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.PacksBooking;
import tn.esprit.entities.Pack;
import tn.esprit.services.PackBookingService;
import tn.esprit.services.PackService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminPacksBookingsController {

    @FXML private FlowPane bookingsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button btnClearFilters;
    @FXML private Label lblTotalBookings, lblPendingBookings, lblConfirmedBookings, lblTotalRevenue;

    private PackBookingService packBookingService;
    private PackService packService;
    
    private List<PacksBooking> allBookings;
    private tn.esprit.entities.User currentUser;
    private String userRole;

    public void initialize() {
        packBookingService = new PackBookingService();
        packService = new PackService();
        
        setupFilters();
        loadBookings();
        updateStats();
        
        btnClearFilters.setOnAction(e -> clearFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> filterBookings());
    }

    public void setCurrentUser(tn.esprit.entities.User user) {
        this.currentUser = user;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList("All", "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"));
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> filterBookings());
    }

    private void loadBookings() {
        try {
            allBookings = packBookingService.afficherList();
            displayBookings(allBookings);
        } catch (SQLException e) {
            showError("Failed to load bookings: " + e.getMessage());
        }
    }

    private void displayBookings(List<PacksBooking> bookings) {
        bookingsGrid.getChildren().clear();
        
        for (PacksBooking booking : bookings) {
            bookingsGrid.getChildren().add(createBookingCard(booking));
        }
    }

    private VBox createBookingCard(PacksBooking booking) {
        VBox card = new VBox(16);
        card.setPrefWidth(360);
        
        // Card color based on status
        String bgColor = switch (booking.getStatus()) {
            case CONFIRMED -> "linear-gradient(to bottom right, #43e97b, #38f9d7)";
            case CANCELLED -> "linear-gradient(to bottom right, #ff6b6b, #ee5a6f)";
            case COMPLETED -> "linear-gradient(to bottom right, #4facfe, #00f2fe)";
            default -> "linear-gradient(to bottom right, #FFA500, #FF6347)";
        };
        
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16; -fx-padding: 24; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4); -fx-cursor: hand;");
        
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-translate-y: -4; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 8);");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-translate-y: -4;", ""));
        });
        
        try {
            // Header
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);
            
            Label icon = new Label("🎫");
            icon.setStyle("-fx-font-size: 28px;");
            
            VBox titleBox = new VBox(4);
            Label bookingId = new Label("Booking #" + booking.getIdBooking());
            bookingId.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            Pack pack = packService.getById(booking.getPackId());
            Label packName = new Label(pack != null ? pack.getTitle() : "Pack #" + booking.getPackId());
            packName.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.9);");
            packName.setWrapText(true);
            
            titleBox.getChildren().addAll(bookingId, packName);
            
            header.getChildren().addAll(icon, titleBox);
            
            // Info box
            VBox infoBox = new VBox(8);
            infoBox.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10; -fx-padding: 12;");
            
            Label userInfo = new Label("👤 User ID: " + booking.getUserId());
            userInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: white; -fx-font-weight: 600;");
            
            Label dates = new Label("📅 " + booking.getTravelStartDate() + " → " + booking.getTravelEndDate());
            dates.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.95);");
            
            Label travelers = new Label("👥 " + booking.getNumTravelers() + " traveler" + (booking.getNumTravelers() > 1 ? "s" : ""));
            travelers.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.95);");
            
            Label bookingDate = new Label("🕐 Booked: " + booking.getBookingDate().toString().substring(0, 16));
            bookingDate.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.85);");
            
            infoBox.getChildren().addAll(userInfo, dates, travelers, bookingDate);
            
            // Price
            HBox priceBox = new HBox(8);
            priceBox.setAlignment(Pos.CENTER_LEFT);
            priceBox.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10; -fx-padding: 12;");
            
            Label priceLabel = new Label("💰 Total: " + String.format("%.2f TND", booking.getFinalPrice()));
            priceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
            
            if (booking.getDiscountApplied().doubleValue() > 0) {
                Label discountLabel = new Label("(-" + booking.getDiscountApplied() + "%)");
                discountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.85);");
                priceBox.getChildren().addAll(priceLabel, discountLabel);
            } else {
                priceBox.getChildren().add(priceLabel);
            }
            
            // Status badge
            Label statusBadge = new Label(booking.getStatus().name());
            statusBadge.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; " +
                               "-fx-padding: 6 16; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: 700;");
            statusBadge.setMaxWidth(Double.MAX_VALUE);
            statusBadge.setAlignment(Pos.CENTER);
            
            // Notes (if any)
            if (booking.getNotes() != null && !booking.getNotes().trim().isEmpty()) {
                VBox notesBox = new VBox(4);
                notesBox.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10; -fx-padding: 12;");
                
                Label notesLabel = new Label("📝 Notes:");
                notesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-weight: 600;");
                
                Label notesText = new Label(booking.getNotes());
                notesText.setWrapText(true);
                notesText.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.9);");
                
                notesBox.getChildren().addAll(notesLabel, notesText);
                card.getChildren().addAll(header, infoBox, priceBox, statusBadge, notesBox);
            } else {
                card.getChildren().addAll(header, infoBox, priceBox, statusBadge);
            }
            
            // Actions (only for PENDING bookings)
            if (booking.getStatus() == PacksBooking.Status.PENDING) {
                HBox actions = new HBox(10);
                actions.setAlignment(Pos.CENTER);
                
                Button confirmBtn = new Button("✅ Confirm");
                confirmBtn.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-weight: 600; " +
                                  "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
                confirmBtn.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(confirmBtn, Priority.ALWAYS);
                confirmBtn.setOnAction(e -> handleConfirmBooking(booking));
                
                Button rejectBtn = new Button("❌ Reject");
                rejectBtn.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-weight: 600; " +
                                 "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
                rejectBtn.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(rejectBtn, Priority.ALWAYS);
                rejectBtn.setOnAction(e -> handleRejectBooking(booking));
                
                actions.getChildren().addAll(confirmBtn, rejectBtn);
                card.getChildren().add(actions);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return card;
    }

    private void updateStats() {
        if (allBookings == null || allBookings.isEmpty()) {
            lblTotalBookings.setText("0");
            lblPendingBookings.setText("0");
            lblConfirmedBookings.setText("0");
            lblTotalRevenue.setText("0 TND");
            return;
        }
        
        lblTotalBookings.setText(String.valueOf(allBookings.size()));
        
        long pending = allBookings.stream().filter(b -> b.getStatus() == PacksBooking.Status.PENDING).count();
        lblPendingBookings.setText(String.valueOf(pending));
        
        long confirmed = allBookings.stream().filter(b -> b.getStatus() == PacksBooking.Status.CONFIRMED).count();
        lblConfirmedBookings.setText(String.valueOf(confirmed));
        
        double revenue = allBookings.stream()
            .filter(b -> b.getStatus() == PacksBooking.Status.CONFIRMED || b.getStatus() == PacksBooking.Status.COMPLETED)
            .mapToDouble(b -> b.getFinalPrice().doubleValue())
            .sum();
        lblTotalRevenue.setText(String.format("%.2f TND", revenue));
    }

    private void filterBookings() {
        if (allBookings == null) return;

        List<PacksBooking> filtered = allBookings.stream().filter(booking -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                boolean matchesUserId = String.valueOf(booking.getUserId()).contains(searchText);
                boolean matchesPackId = String.valueOf(booking.getPackId()).contains(searchText);
                boolean matchesBookingId = String.valueOf(booking.getIdBooking()).contains(searchText);
                if (!matchesUserId && !matchesPackId && !matchesBookingId) return false;
            }

            if (!statusFilter.getValue().equals("All")) {
                if (!booking.getStatus().name().equals(statusFilter.getValue())) return false;
            }

            return true;
        }).toList();

        displayBookings(filtered);
    }

    private void clearFilters() {
        searchField.clear();
        statusFilter.setValue("All");
        loadBookings();
    }

    private void handleConfirmBooking(PacksBooking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Booking");
        confirm.setHeaderText("Confirm Booking #" + booking.getIdBooking() + "?");
        confirm.setContentText("This will notify the customer that their booking is confirmed.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                packBookingService.updateStatus(booking.getIdBooking(), PacksBooking.Status.CONFIRMED);
                loadBookings();
                updateStats();
                showInfo("Booking confirmed successfully!");
            } catch (SQLException e) {
                showError("Failed to confirm booking: " + e.getMessage());
            }
        }
    }

    private void handleRejectBooking(PacksBooking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reject Booking");
        confirm.setHeaderText("Reject Booking #" + booking.getIdBooking() + "?");
        confirm.setContentText("This will cancel the booking and notify the customer.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                packBookingService.updateStatus(booking.getIdBooking(), PacksBooking.Status.CANCELLED);
                loadBookings();
                updateStats();
                showInfo("Booking rejected successfully!");
            } catch (SQLException e) {
                showError("Failed to reject booking: " + e.getMessage());
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}
