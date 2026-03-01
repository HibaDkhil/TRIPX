package tn.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.*;
import tn.esprit.services.*;
import tn.esprit.utils.SessionManager;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

public class UserPacksBookingDialogController {

    @FXML private Label lblPackName, lblDestination, lblDuration;
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private Spinner<Integer> spinTravelers;
    @FXML private TextArea txtNotes;
    @FXML private Label lblBasePrice, lblOfferDiscount, lblLoyaltyDiscount, lblNumTravelers, lblTotalPrice;
    @FXML private HBox offerRow;
    @FXML private Button btnCancel, btnConfirmBooking;

    private Pack currentPack;
    private Offer currentOffer;
    private LoyaltyPoints userLoyalty;
    
    private PackService packService;
    private OfferService offerService;
    private LoyaltyPointsService loyaltyService;
    private LookupService lookupService;
    private PackBookingService packBookingService;
    
    private boolean bookingConfirmed = false;

    public void initialize() {
        packService = new PackService();
        offerService = new OfferService();
        loyaltyService = new LoyaltyPointsService();
        lookupService = new LookupService();
        packBookingService = new PackBookingService();
        
        // Setup spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1);
        spinTravelers.setValueFactory(valueFactory);
        
        // Listen to changes
        spinTravelers.valueProperty().addListener((obs, old, newVal) -> updatePriceSummary());
        
        // Setup buttons
        btnCancel.setOnAction(e -> closeDialog());
        btnConfirmBooking.setOnAction(e -> handleConfirmBooking());
        
        // Set minimum dates (can't book in the past)
        dpStartDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
        
        dpEndDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate startDate = dpStartDate.getValue();
                setDisable(empty || date.isBefore(LocalDate.now()) || 
                          (startDate != null && date.isBefore(startDate)));
            }
        });
    }

    public void setPackData(Pack pack) {
        this.currentPack = pack;
        
        try {
            // Load pack info
            lblPackName.setText(pack.getTitle());
            lblDuration.setText("📅 " + pack.getDurationDays() + " days");
            
            Destination dest = lookupService.getDestinationById(pack.getDestinationId());
            lblDestination.setText("📍 " + (dest != null ? dest.getName() : "N/A"));
            
            // Check for active offer
            currentOffer = offerService.getActiveOfferByPackId(pack.getIdPack());
            
            // Get user loyalty
            int userId = SessionManager.getCurrentUserId();
            userLoyalty = loyaltyService.getByUserId(userId);
            if (userLoyalty == null) {
                userLoyalty = new LoyaltyPoints(userId);
                loyaltyService.add(userLoyalty);
            }
            
            // Set suggested dates (start = tomorrow, end = tomorrow + pack duration)
            dpStartDate.setValue(LocalDate.now().plusDays(1));
            dpEndDate.setValue(LocalDate.now().plusDays(1 + pack.getDurationDays()));
            
            // Update price summary
            updatePriceSummary();
            
        } catch (SQLException e) {
            showError("Failed to load pack data: " + e.getMessage());
        }
    }

    private void updatePriceSummary() {
        if (currentPack == null) return;
        
        double basePrice = currentPack.getBasePrice().doubleValue();
        lblBasePrice.setText(String.format("%.2f TND", basePrice));
        
        // Offer discount
        double offerDiscountPercent = 0;
        if (currentOffer != null && currentOffer.getDiscountType() == Offer.DiscountType.PERCENTAGE) {
            offerDiscountPercent = currentOffer.getDiscountValue().doubleValue();
            lblOfferDiscount.setText("-" + offerDiscountPercent + "%");
            offerRow.setVisible(true);
            offerRow.setManaged(true);
        } else {
            offerRow.setVisible(false);
            offerRow.setManaged(false);
        }
        
        // Loyalty discount
        double loyaltyDiscountPercent = userLoyalty != null ? userLoyalty.getLoyaltyDiscountPercent() : 0;
        lblLoyaltyDiscount.setText("-" + loyaltyDiscountPercent + "%");
        
        // Number of travelers
        int numTravelers = spinTravelers.getValue();
        lblNumTravelers.setText("× " + numTravelers);
        
        // Calculate total
        try {
            double pricePerPerson = loyaltyService.calculateFinalPrice(basePrice,
                SessionManager.getCurrentUserId(), offerDiscountPercent);
            double totalPrice = pricePerPerson * numTravelers;
            
            lblTotalPrice.setText(String.format("%.2f TND", totalPrice));
        } catch (SQLException e) {
            lblTotalPrice.setText("Error");
        }
    }

    private void handleConfirmBooking() {
        // Validation
        if (dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            showError("Please select travel dates");
            return;
        }
        
        if (dpEndDate.getValue().isBefore(dpStartDate.getValue())) {
            showError("End date must be after start date");
            return;
        }
        
        try {
            int userId = SessionManager.getCurrentUserId();
            double basePrice = currentPack.getBasePrice().doubleValue();
            
            // Calculate discount
            double offerDiscountPercent = 0;
            if (currentOffer != null && currentOffer.getDiscountType() == Offer.DiscountType.PERCENTAGE) {
                offerDiscountPercent = currentOffer.getDiscountValue().doubleValue();
            }
            
            double loyaltyDiscountPercent = userLoyalty != null ? userLoyalty.getLoyaltyDiscountPercent() : 0;
            double totalDiscountPercent = offerDiscountPercent + loyaltyDiscountPercent;
            
            // Calculate prices
            double pricePerPerson = loyaltyService.calculateFinalPrice(basePrice, userId, offerDiscountPercent);
            int numTravelers = spinTravelers.getValue();
            double finalPrice = pricePerPerson * numTravelers;
            
            // Create booking
            PacksBooking booking = new PacksBooking(
                userId,
                currentPack.getIdPack(),
                Date.valueOf(dpStartDate.getValue()),
                Date.valueOf(dpEndDate.getValue()),
                numTravelers,
                new BigDecimal(basePrice * numTravelers),
                new BigDecimal(totalDiscountPercent),
                new BigDecimal(finalPrice),
                txtNotes.getText()
            );
            
            packBookingService.add(booking);
            
            // Add loyalty points (50 points per trip)
            loyaltyService.addTripPoints(userId);
            
            bookingConfirmed = true;
            
            // Show success
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Booking Confirmed");
            success.setHeaderText("🎉 Your trip is booked!");
            success.setContentText(String.format(
                "Booking Details:\n\n" +
                "Pack: %s\n" +
                "Dates: %s to %s\n" +
                "Travelers: %d\n" +
                "Total Price: %.2f TND\n\n" +
                "You earned 50 loyalty points!",
                currentPack.getTitle(),
                dpStartDate.getValue(),
                dpEndDate.getValue(),
                numTravelers,
                finalPrice
            ));
            success.showAndWait();
            
            closeDialog();
            
        } catch (SQLException e) {
            showError("Failed to create booking: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public boolean isBookingConfirmed() {
        return bookingConfirmed;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
