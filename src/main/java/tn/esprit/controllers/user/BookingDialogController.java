package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Activity;
import tn.esprit.entities.Booking;
import tn.esprit.entities.Destination;
import tn.esprit.services.ActivityService;
import tn.esprit.services.BookingService;
import tn.esprit.utils.SessionManager;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

public class BookingDialogController {

    @FXML private Label destinationLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker; // Added field
    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private ComboBox<Activity> activityCombo;
    @FXML private Label totalLabel;
    @FXML private TextArea notesArea;
    @FXML private Button confirmBtn;
    @FXML private Button cancelBtn;

    private Booking existingBooking;
    private boolean isEditMode = false;
    private Destination destination; // Restored
    private BookingService bookingService; // Restored
    private ActivityService activityService; // Restored

    public void setDestination(Destination destination) {
        this.destination = destination;
        this.bookingService = new BookingService();
        this.activityService = new ActivityService();

        destinationLabel.setText("Booking for: " + destination.getName());
        setupUI();
    }

    public void setBooking(Booking booking, Destination destination, boolean restrictedMode) {
        this.existingBooking = booking;
        this.destination = destination;
        this.isEditMode = true;
        this.bookingService = new BookingService();
        this.activityService = new ActivityService();

        destinationLabel.setText("Edit Booking: " + booking.getBookingReference());
        confirmBtn.setText("Update Booking");

        setupUI();
        prefillData();
        
        if (restrictedMode) {
            applyRestrictions();
        }
    }

    private void setupUI() {
        guestsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        
        // Load activities for this destination
        List<Activity> activities = activityService.getActivitiesByDestination(destination.getDestinationId());
        activityCombo.setItems(FXCollections.observableArrayList(activities));
        
        // Listeners for price calculation
        guestsSpinner.valueProperty().addListener((obs, o, n) -> calculateTotal());
        activityCombo.valueProperty().addListener((obs, o, n) -> calculateTotal());
        startDatePicker.valueProperty().addListener((obs, o, n) -> calculateDateDiff());
        endDatePicker.valueProperty().addListener((obs, o, n) -> calculateDateDiff());
        
        confirmBtn.setOnAction(e -> handleConfirm());
        cancelBtn.setOnAction(e -> closeDialog());
    }

    private void prefillData() {
        if (existingBooking == null) return;

        startDatePicker.setValue(existingBooking.getStartAt().toLocalDateTime().toLocalDate());
        endDatePicker.setValue(existingBooking.getEndAt().toLocalDateTime().toLocalDate());
        guestsSpinner.getValueFactory().setValue(existingBooking.getNumGuests());
        notesArea.setText(existingBooking.getNotes());

        if (existingBooking.getActivityId() != null) {
            activityCombo.getItems().stream()
                .filter(a -> a.getActivityId() == existingBooking.getActivityId())
                .findFirst()
                .ifPresent(activityCombo::setValue);
        }
        
        calculateTotal();
    }

    private void applyRestrictions() {
        startDatePicker.setDisable(true);
        endDatePicker.setDisable(true);
        guestsSpinner.setDisable(true);
        activityCombo.setDisable(true);
        // Notes remain editable
    }

    private void calculateDateDiff() {
        // Optional: Implement logic to scale price by days if needed
        calculateTotal();
    }

    private void calculateTotal() {
        double basePrice = 0.0; // Assume 0 base price for destination only
        double activityPrice = 0.0;
        
        if (activityCombo.getValue() != null) {
            activityPrice = activityCombo.getValue().getPrice();
        }
        
        int guests = guestsSpinner.getValue();
        double total = (basePrice + activityPrice) * guests;
        
        totalLabel.setText(String.format("$%.2f", total));
    }

    private void handleConfirm() {
        if (!isEditMode) {
             // ... Creation Logic (Date Validations) ...
            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                showAlert("Date Required", "Please select start and end dates.");
                return;
            }
            if (startDatePicker.getValue().isBefore(LocalDate.now())) {
                showAlert("Invalid Date", "Start date cannot be in the past.");
                return;
            }
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                showAlert("Invalid Date", "End date must be after start date.");
                return;
            }
        }

        // Prepare Booking object
        Booking booking = isEditMode ? existingBooking : new Booking();
        
        if (!isEditMode) {
            booking.setUserId(SessionManager.getCurrentUserId());
            booking.setDestinationId(destination.getDestinationId());
             // New booking defaults
        }

        // Update fields (if enabled)
        if (!startDatePicker.isDisabled()) {
             booking.setStartAt(Timestamp.valueOf(startDatePicker.getValue().atStartOfDay()));
             booking.setEndAt(Timestamp.valueOf(endDatePicker.getValue().atTime(23, 59, 59)));
        }
        
        if (!guestsSpinner.isDisabled()) {
            booking.setNumGuests(guestsSpinner.getValue());
        }

        if (!activityCombo.isDisabled()) {
            if (activityCombo.getValue() != null) {
                booking.setActivityId(activityCombo.getValue().getActivityId());
            } else {
                booking.setActivityId(null);
            }
        }
        
        booking.setNotes(notesArea.getText());

        // Recalculate total if key fields changed
        String priceText = totalLabel.getText().replace("$", "").replace(",", ".");
        booking.setTotalAmount(Double.parseDouble(priceText));

        boolean success;
        if (isEditMode) {
            success = bookingService.updateBooking(booking);
        } else {
            success = bookingService.createBooking(booking);
        }

        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(isEditMode ? "Booking Updated!" : "Booking Confirmed!");
            alert.setContentText(isEditMode ? "Your changes have been saved." : "Reference: " + booking.getBookingReference());
            alert.showAndWait();
            closeDialog();
        } else {
            showAlert("Error", "Operation failed.");
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) confirmBtn.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
