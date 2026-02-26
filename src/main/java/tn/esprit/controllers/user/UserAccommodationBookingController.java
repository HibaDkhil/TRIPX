package tn.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Accommodation;
import tn.esprit.entities.AccommodationBooking;
import tn.esprit.entities.Room;
import tn.esprit.services.AccommodationBookingService;

import java.sql.Date;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class UserAccommodationBookingController {

    @FXML
    private Label accommodationNameLabel;
    @FXML
    private Label roomNameLabel;
    @FXML
    private Label pricePerNightLabel;
    @FXML
    private DatePicker checkInDatePicker;
    @FXML
    private DatePicker checkOutDatePicker;
    @FXML
    private Label nightsLabel;
    @FXML
    private Label totalPriceLabel;
    @FXML
    private Label availabilityStatusLabel;
    @FXML
    private Button checkAvailabilityButton;
    @FXML
    private Button bookNowButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ComboBox<Integer> numberOfGuestsCombo;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextArea specialRequestsArea;
    @FXML
    private ComboBox<String> estimatedArrivalTimeCombo;
    @FXML
    private Label capacityHintLabel;

    private final AccommodationBookingService accommodationBookingService = new AccommodationBookingService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    private Accommodation accommodation;
    private Room room;
    private int userId;
    private boolean latestAvailability = false;
    private Runnable onBookingCreated;

    @FXML
    public void initialize() {
        checkInDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            updateTotals();
            resetAvailabilityStatus();
        });
        checkOutDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            updateTotals();
            resetAvailabilityStatus();
        });

        if (estimatedArrivalTimeCombo != null) {
            estimatedArrivalTimeCombo.getItems().setAll(
                    "Not sure yet",
                    "06:00 - 09:00",
                    "09:00 - 12:00",
                    "12:00 - 15:00",
                    "15:00 - 18:00",
                    "18:00 - 21:00",
                    "21:00 - 00:00",
                    "After 00:00"
            );
            estimatedArrivalTimeCombo.setValue("Not sure yet");
        }
    }

    public void setContext(Accommodation accommodation, Room room, int userId) {
        this.accommodation = accommodation;
        this.room = room;
        this.userId = userId;

        accommodationNameLabel.setText(accommodation != null ? safe(accommodation.getName(), "Accommodation") : "Accommodation");
        roomNameLabel.setText(room != null ? safe(room.getRoomName(), "Room") : "Room");
        pricePerNightLabel.setText(room != null ? currencyFormat.format(room.getPricePerNight()) + " / night" : "-");
        if (room != null && numberOfGuestsCombo != null) {
            int maxGuests = Math.max(1, room.getCapacity());
            numberOfGuestsCombo.getItems().clear();
            for (int i = 1; i <= maxGuests; i++) {
                numberOfGuestsCombo.getItems().add(i);
            }
            numberOfGuestsCombo.setValue(Math.min(2, maxGuests));
            if (capacityHintLabel != null) {
                capacityHintLabel.setText("Room capacity: up to " + maxGuests + " guest" + (maxGuests > 1 ? "s" : ""));
            }
        }

        updateTotals();
        resetAvailabilityStatus();
    }

    public void setOnBookingCreated(Runnable onBookingCreated) {
        this.onBookingCreated = onBookingCreated;
    }

    @FXML
    private void handleCheckAvailability() {
        if (!validateInputDates(true)) {
            return;
        }

        Date checkIn = Date.valueOf(checkInDatePicker.getValue());
        Date checkOut = Date.valueOf(checkOutDatePicker.getValue());
        latestAvailability = accommodationBookingService.isAccommodationRoomAvailable(room.getId(), checkIn, checkOut);

        if (latestAvailability) {
            availabilityStatusLabel.setText("Room is available for selected dates.");
            availabilityStatusLabel.getStyleClass().setAll("booking-dialog-status-success");
        } else {
            availabilityStatusLabel.setText("Room is not available for selected dates.");
            availabilityStatusLabel.getStyleClass().setAll("booking-dialog-status-error");
        }
    }

    @FXML
    private void handleBookNow() {
        if (!validateInputDates(true) || !validateAdvancedInputs(true)) {
            return;
        }

        bookNowButton.setDisable(true);
        checkAvailabilityButton.setDisable(true);

        Date checkIn = Date.valueOf(checkInDatePicker.getValue());
        Date checkOut = Date.valueOf(checkOutDatePicker.getValue());

        boolean availableNow = accommodationBookingService.isAccommodationRoomAvailable(room.getId(), checkIn, checkOut);
        if (!availableNow) {
            latestAvailability = false;
            availabilityStatusLabel.setText("Room just became unavailable. Please try different dates.");
            availabilityStatusLabel.getStyleClass().setAll("booking-dialog-status-error");
            bookNowButton.setDisable(false);
            checkAvailabilityButton.setDisable(false);
            return;
        }

        long nights = ChronoUnit.DAYS.between(checkInDatePicker.getValue(), checkOutDatePicker.getValue());
        double totalPrice = nights * room.getPricePerNight();

        AccommodationBooking booking = new AccommodationBooking();
        booking.setUserId(userId);
        booking.setRoomId(room.getId());
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setTotalPrice(totalPrice);
        booking.setNumberOfGuests(numberOfGuestsCombo.getValue());
        booking.setPhoneNumber(phoneNumberField.getText());
        booking.setSpecialRequests(specialRequestsArea.getText());
        booking.setEstimatedArrivalTime(estimatedArrivalTimeCombo.getValue());
        booking.setStatus("PENDING");

        boolean added = accommodationBookingService.addAccommodationBooking(booking);
        if (added) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Booking created");
            alert.setHeaderText(null);
            alert.setContentText("Your accommodation booking request was created with status PENDING.");
            alert.showAndWait();
            if (onBookingCreated != null) {
                onBookingCreated.run();
            }
            closeDialog();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Booking failed");
            alert.setHeaderText(null);
            alert.setContentText("Unable to create booking. Please verify dates and try again.");
            alert.showAndWait();
            bookNowButton.setDisable(false);
            checkAvailabilityButton.setDisable(false);
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void updateTotals() {
        if (!validateInputDates(false)) {
            nightsLabel.setText("-");
            totalPriceLabel.setText("-");
            return;
        }

        long nights = ChronoUnit.DAYS.between(checkInDatePicker.getValue(), checkOutDatePicker.getValue());
        double total = nights * (room != null ? room.getPricePerNight() : 0);
        nightsLabel.setText(String.valueOf(nights));
        totalPriceLabel.setText(currencyFormat.format(total));
    }

    private boolean validateInputDates(boolean showErrors) {
        LocalDate checkIn = checkInDatePicker.getValue();
        LocalDate checkOut = checkOutDatePicker.getValue();

        if (checkIn == null || checkOut == null) {
            if (showErrors) {
                showWarning("Please select both check-in and check-out dates.");
            }
            return false;
        }

        if (!checkIn.isBefore(checkOut)) {
            if (showErrors) {
                showWarning("Check-out date must be after check-in date.");
            }
            return false;
        }

        if (checkIn.isBefore(LocalDate.now())) {
            if (showErrors) {
                showWarning("Check-in date cannot be in the past.");
            }
            return false;
        }

        return true;
    }

    private boolean validateAdvancedInputs(boolean showErrors) {
        if (numberOfGuestsCombo.getValue() == null || numberOfGuestsCombo.getValue() <= 0) {
            if (showErrors) {
                showWarning("Please select number of guests.");
            }
            return false;
        }

        if (room != null && numberOfGuestsCombo.getValue() > room.getCapacity()) {
            if (showErrors) {
                showWarning("Selected guests exceed room capacity.");
            }
            return false;
        }

        String phone = phoneNumberField.getText() == null ? "" : phoneNumberField.getText().trim();
        if (phone.isEmpty()) {
            if (showErrors) {
                showWarning("Phone number is required.");
            }
            return false;
        }

        if (!phone.matches("^[+0-9()\\-\\s]{8,20}$")) {
            if (showErrors) {
                showWarning("Phone number format is invalid.");
            }
            return false;
        }

        return true;
    }

    private void resetAvailabilityStatus() {
        latestAvailability = false;
        availabilityStatusLabel.setText("Availability not checked yet.");
        availabilityStatusLabel.getStyleClass().setAll("booking-dialog-status-neutral");
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
