package tn.esprit.controllers.admin;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.Duration;
import tn.esprit.entities.AccommodationBooking;
import tn.esprit.services.AccommodationBookingService;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AdminAccommodationBookingController {

    // Sidebar and topbar shell
    @FXML private VBox sidebar;
    @FXML private Button sidebarToggle;
    @FXML private Button sidebarOpenButton;
    @FXML private javafx.scene.control.ToggleButton darkModeToggle;
    @FXML private ComboBox<String> languageSelector;
    @FXML private Button dashboardToggle;
    @FXML private Button usersToggle;
    @FXML private Button accommodationsToggle;
    @FXML private Button destinationsToggle;
    @FXML private Button offersToggle;
    @FXML private Button transportToggle;
    @FXML private VBox dashboardMenu;
    @FXML private VBox usersMenu;
    @FXML private VBox accommodationsMenu;
    @FXML private VBox destinationsMenu;
    @FXML private VBox offersMenu;
    @FXML private VBox transportMenu;
    @FXML private MenuButton profileDropdown;
    @FXML private Button notificationBtn;

    @FXML
    private TableView<AccommodationBooking> bookingsTable;
    @FXML
    private TableColumn<AccommodationBooking, Integer> idColumn;
    @FXML
    private TableColumn<AccommodationBooking, Integer> userIdColumn;
    @FXML
    private TableColumn<AccommodationBooking, Integer> roomIdColumn;
    @FXML
    private TableColumn<AccommodationBooking, java.sql.Date> checkInColumn;
    @FXML
    private TableColumn<AccommodationBooking, java.sql.Date> checkOutColumn;
    @FXML
    private TableColumn<AccommodationBooking, Double> totalPriceColumn;
    @FXML
    private TableColumn<AccommodationBooking, String> statusColumn;
    @FXML
    private TableColumn<AccommodationBooking, java.sql.Timestamp> createdAtColumn;

    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Label selectedBookingLabel;
    @FXML
    private Button confirmButton;
    @FXML
    private Button rejectButton;
    @FXML
    private Button cancelButton;
    @FXML
    private TextField bookingSearchField;
    @FXML
    private ComboBox<String> roomIdFilter;
    @FXML
    private DatePicker checkInFromFilter;
    @FXML
    private DatePicker checkOutToFilter;
    @FXML
    private TextField minTotalFilterField;
    @FXML
    private TextField maxTotalFilterField;
    @FXML
    private Label detailBookingIdValue;
    @FXML
    private Label detailUserIdValue;
    @FXML
    private Label detailRoomIdValue;
    @FXML
    private Label detailStatusValue;
    @FXML
    private Label detailCheckInValue;
    @FXML
    private Label detailCheckOutValue;
    @FXML
    private Label detailTotalPriceValue;
    @FXML
    private Label detailCreatedAtValue;
    @FXML
    private Label detailGuestsValue;
    @FXML
    private Label detailPhoneValue;
    @FXML
    private Label detailArrivalTimeValue;
    @FXML
    private Label detailSpecialRequestsValue;
    @FXML
    private Label detailRejectReasonValue;
    @FXML
    private Label detailCancelReasonValue;
    @FXML
    private Label detailCancelledAtValue;

    private final AccommodationBookingService accommodationBookingService = new AccommodationBookingService();
    private final ObservableList<AccommodationBooking> bookingItems = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    private boolean isSidebarCollapsed = false;
    private final List<AccommodationBooking> allBookings = new ArrayList<>();

    @FXML
    public void initialize() {
        setupSidebarNavigation();
        setupTableColumns();
        setupStatusFilter();
        setupSearchFilter();
        setupSelectionListener();
        clearBookingDetails();
        loadBookings();
    }

    private void setupSidebarNavigation() {
        setupSidebarToggle();
        setupMenuToggles();
        setupThemeToggle();
        setupLanguageSelector();
        setupProfileDropdown();
        setupNotificationButton();
    }

    private void setupSidebarToggle() {
        if (sidebarToggle != null) {
            sidebarToggle.setOnAction(event -> collapseSidebar());
        }

        if (sidebarOpenButton != null) {
            sidebarOpenButton.setOnAction(event -> expandSidebar());
            sidebarOpenButton.setVisible(false);
            sidebarOpenButton.setManaged(false);
        }
    }

    private void collapseSidebar() {
        isSidebarCollapsed = true;
        if (sidebar != null) {
            animateSidebarHide();
        }
        if (sidebarOpenButton != null) {
            sidebarOpenButton.setVisible(true);
            sidebarOpenButton.setManaged(true);
        }
    }

    private void expandSidebar() {
        isSidebarCollapsed = false;
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
            animateSidebarShow();
        }
        if (sidebarOpenButton != null) {
            sidebarOpenButton.setVisible(false);
            sidebarOpenButton.setManaged(false);
        }
    }

    private void animateSidebarHide() {
        if (sidebar == null) return;
        double width = sidebar.getWidth() > 0 ? sidebar.getWidth() : 280;
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(220), sidebar);
        slideOut.setFromX(0);
        slideOut.setToX(-width);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), sidebar);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ParallelTransition hideTransition = new ParallelTransition(slideOut, fadeOut);
        hideTransition.setOnFinished(e -> {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
            sidebar.setTranslateX(0);
            sidebar.setOpacity(1.0);
        });
        hideTransition.play();
    }

    private void animateSidebarShow() {
        if (sidebar == null) return;
        double width = sidebar.getWidth() > 0 ? sidebar.getWidth() : 280;
        sidebar.setTranslateX(-width);
        sidebar.setOpacity(0.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), sidebar);
        slideIn.setFromX(-width);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), sidebar);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        new ParallelTransition(slideIn, fadeIn).play();
    }

    private void setupMenuToggles() {
        if (dashboardToggle != null && dashboardMenu != null) {
            dashboardToggle.setOnAction(event -> toggleMenu(dashboardMenu, dashboardToggle));
            dashboardMenu.setVisible(false);
            dashboardMenu.setManaged(false);
        }
        if (usersToggle != null && usersMenu != null) {
            usersToggle.setOnAction(event -> toggleMenu(usersMenu, usersToggle));
            usersMenu.setVisible(false);
            usersMenu.setManaged(false);
        }
        if (accommodationsToggle != null && accommodationsMenu != null) {
            accommodationsToggle.setOnAction(event -> toggleMenu(accommodationsMenu, accommodationsToggle));
            accommodationsMenu.setVisible(true);
            accommodationsMenu.setManaged(true);
        }
        if (destinationsToggle != null && destinationsMenu != null) {
            destinationsToggle.setOnAction(event -> toggleMenu(destinationsMenu, destinationsToggle));
            destinationsMenu.setVisible(false);
            destinationsMenu.setManaged(false);
        }
        if (offersToggle != null && offersMenu != null) {
            offersToggle.setOnAction(event -> toggleMenu(offersMenu, offersToggle));
            offersMenu.setVisible(false);
            offersMenu.setManaged(false);
        }
        if (transportToggle != null && transportMenu != null) {
            transportToggle.setOnAction(event -> toggleMenu(transportMenu, transportToggle));
            transportMenu.setVisible(false);
            transportMenu.setManaged(false);
        }
    }

    private void toggleMenu(VBox menu, Button toggleButton) {
        boolean isVisible = menu.isVisible();
        toggleButton.setText(isVisible ? "▶" : "▼");

        if (isVisible) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), menu);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), menu);
            scaleOut.setFromY(1);
            scaleOut.setToY(0);
            ParallelTransition hide = new ParallelTransition(fadeOut, scaleOut);
            hide.setOnFinished(e -> {
                menu.setVisible(false);
                menu.setManaged(false);
                menu.setOpacity(1);
                menu.setScaleY(1);
            });
            hide.play();
        } else {
            menu.setManaged(true);
            menu.setVisible(true);
            menu.setOpacity(0);
            menu.setScaleY(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), menu);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(180), menu);
            scaleIn.setFromY(0);
            scaleIn.setToY(1);
            new ParallelTransition(fadeIn, scaleIn).play();
        }
    }

    private void setupThemeToggle() {
        if (darkModeToggle != null) {
            darkModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) enableDarkMode();
                else enableLightMode();
            });
        }
    }

    private void enableDarkMode() {
        if (sidebar == null || sidebar.getScene() == null) return;
        StackPane root = (StackPane) sidebar.getScene().getRoot();
        if (!root.getStyleClass().contains("dark-mode")) {
            root.getStyleClass().add("dark-mode");
        }
    }

    private void enableLightMode() {
        if (sidebar == null || sidebar.getScene() == null) return;
        StackPane root = (StackPane) sidebar.getScene().getRoot();
        root.getStyleClass().remove("dark-mode");
    }

    private void setupLanguageSelector() {
        if (languageSelector != null) {
            languageSelector.setValue("English");
        }
    }

    private void setupProfileDropdown() {
        if (profileDropdown == null) return;
        var items = profileDropdown.getItems();
        if (items.size() > 0) items.get(0).setOnAction(e -> showToast("Opening user profile...", "info"));
        if (items.size() > 1) items.get(1).setOnAction(e -> showToast("Opening settings...", "info"));
        if (items.size() > 3) items.get(3).setOnAction(e -> showToast("Logging out...", "info"));
    }

    private void setupNotificationButton() {
        if (notificationBtn != null) {
            notificationBtn.setOnAction(e -> showToast("Showing notifications...", "info"));
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        roomIdColumn.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        checkInColumn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        checkOutColumn.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        totalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        totalPriceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(item));
                }
            }
        });

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setStyle("");
                    return;
                }

                String normalized = item.toUpperCase();
                setText(normalized);
                setStyle("-fx-font-weight: 800; -fx-font-size: 11.5px; -fx-padding: 4 10; -fx-background-radius: 999;");

                switch (normalized) {
                    case "PENDING":
                        setStyle(getStyle() + "-fx-text-fill: #92400E; -fx-background-color: #FEF3C7;");
                        break;
                    case "CONFIRMED":
                        setStyle(getStyle() + "-fx-text-fill: #166534; -fx-background-color: #DCFCE7;");
                        break;
                    case "REJECTED":
                        setStyle(getStyle() + "-fx-text-fill: #991B1B; -fx-background-color: #FEE2E2;");
                        break;
                    case "CANCELLED":
                        setStyle(getStyle() + "-fx-text-fill: #334155; -fx-background-color: #E2E8F0;");
                        break;
                    default:
                        setStyle(getStyle() + "-fx-text-fill: #1E3A8A; -fx-background-color: #DBEAFE;");
                        break;
                }
            }
        });

        bookingsTable.setItems(bookingItems);
    }

    private void setupStatusFilter() {
        statusFilter.getItems().setAll("ALL", "PENDING", "CONFIRMED", "REJECTED", "CANCELLED");
        statusFilter.setValue("ALL");
        statusFilter.setOnAction(event -> applyFilters());
    }

    private void setupSearchFilter() {
        if (bookingSearchField != null) {
            bookingSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (roomIdFilter != null) {
            roomIdFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (checkInFromFilter != null) {
            checkInFromFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (checkOutToFilter != null) {
            checkOutToFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (minTotalFilterField != null) {
            minTotalFilterField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (maxTotalFilterField != null) {
            maxTotalFilterField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
    }

    private void setupSelectionListener() {
        bookingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                selectedBookingLabel.setText("No booking selected");
                clearBookingDetails();
            } else {
                selectedBookingLabel.setText("Selected Booking ID: " + newValue.getId() + " (" + newValue.getStatus() + ")");
                updateBookingDetails(newValue);
            }

            updateActionButtonsBySelection(newValue);
        });

        updateActionButtonsBySelection(null);
    }

    private void loadBookings() {
        List<AccommodationBooking> list = accommodationBookingService.getAllAccommodationBookings();
        allBookings.clear();
        allBookings.addAll(list);
        bookingItems.setAll(list);
        setupRoomIdFilterOptions();
        bookingsTable.getSelectionModel().clearSelection();
        clearBookingDetails();
        updateActionButtonsBySelection(null);
    }

    @FXML
    private void handleApplyFilter() {
        applyFilters();
    }

    @FXML
    private void handleResetFilter() {
        statusFilter.setValue("ALL");
        if (bookingSearchField != null) {
            bookingSearchField.clear();
        }
        if (roomIdFilter != null) {
            roomIdFilter.setValue("ALL");
        }
        if (checkInFromFilter != null) {
            checkInFromFilter.setValue(null);
        }
        if (checkOutToFilter != null) {
            checkOutToFilter.setValue(null);
        }
        if (minTotalFilterField != null) {
            minTotalFilterField.clear();
        }
        if (maxTotalFilterField != null) {
            maxTotalFilterField.clear();
        }
        loadBookings();
    }

    @FXML
    private void handleRefresh() {
        loadBookings();
        applyFilters();
    }

    @FXML
    private void handleConfirmSelected() {
        updateSelectedBookingStatus("CONFIRMED");
    }

    @FXML
    private void handleRejectSelected() {
        String reason = promptReason("Reject booking", "Please enter the rejection reason (required):", true);
        if (reason == null) {
            return;
        }
        updateSelectedBookingStatus("REJECTED", null, reason);
    }

    @FXML
    private void handleCancelSelected() {
        String reason = promptReason("Cancel booking", "Enter cancellation reason (optional):", false);
        if (reason == null) {
            return;
        }
        updateSelectedBookingStatus("CANCELLED", reason, null);
    }

    private void updateSelectedBookingStatus(String targetStatus) {
        updateSelectedBookingStatus(targetStatus, null, null);
    }

    private void updateSelectedBookingStatus(String targetStatus, String cancelReason, String rejectionReason) {
        AccommodationBooking selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a booking first.");
            return;
        }

        boolean success;
        switch (targetStatus) {
            case "CONFIRMED":
                success = accommodationBookingService.confirmAccommodationBooking(selected.getId());
                break;
            case "REJECTED":
                success = accommodationBookingService.rejectAccommodationBooking(selected.getId(), rejectionReason);
                break;
            case "CANCELLED":
                success = accommodationBookingService.cancelAccommodationBooking(selected.getId(), cancelReason);
                break;
            default:
                success = false;
                break;
        }

        if (success) {
            showInfo("Booking status updated to " + targetStatus + ".");
            loadBookings();
            applyFilters();
        } else {
            showError("Unable to update booking status. Transition may be invalid.");
        }
    }

    @FXML
    private void handleBackToAccommodationAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/accommodation-admin-dashboard.fxml")
            );
            Parent root = loader.load();
            if (bookingsTable != null && bookingsTable.getScene() != null) {
                bookingsTable.getScene().setRoot(root);
            }
        } catch (IOException e) {
            showError("Unable to open accommodation admin dashboard.");
        }
    }

    @FXML
    private void showAdminAccommodationBookingPage() {
        // Already on booking page: intentionally no-op.
    }

    private void showToast(String message, String type) {
        if (sidebar == null || sidebar.getScene() == null) return;

        HBox toast = new HBox(10);
        toast.setStyle(
                "-fx-background-color: " + (type.equals("error") ? "#EF5350" : "#2F9D94") + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);"
        );

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 500;");
        toast.getChildren().add(messageLabel);
        toast.setOpacity(0);
        toast.setMaxWidth(Region.USE_PREF_SIZE);

        StackPane root = (StackPane) sidebar.getScene().getRoot();
        StackPane.setAlignment(toast, javafx.geometry.Pos.TOP_CENTER);
        toast.setTranslateY(-40);
        root.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
        slideIn.setFromY(-40);
        slideIn.setToY(20);

        ParallelTransition show = new ParallelTransition(fadeIn, slideIn);
        PauseTransition pause = new PauseTransition(Duration.seconds(2.8));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(260), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(260), toast);
        slideOut.setFromY(20);
        slideOut.setToY(-40);

        ParallelTransition hide = new ParallelTransition(fadeOut, slideOut);
        hide.setOnFinished(e -> root.getChildren().remove(toast));
        new SequentialTransition(show, pause, hide).play();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateBookingDetails(AccommodationBooking booking) {
        detailBookingIdValue.setText(String.valueOf(booking.getId()));
        detailUserIdValue.setText(String.valueOf(booking.getUserId()));
        detailRoomIdValue.setText(String.valueOf(booking.getRoomId()));
        detailStatusValue.setText(booking.getStatus() == null ? "-" : booking.getStatus());
        detailCheckInValue.setText(booking.getCheckIn() == null ? "-" : booking.getCheckIn().toString());
        detailCheckOutValue.setText(booking.getCheckOut() == null ? "-" : booking.getCheckOut().toString());
        detailTotalPriceValue.setText(currencyFormat.format(booking.getTotalPrice()));
        detailCreatedAtValue.setText(booking.getCreatedAt() == null ? "-" : booking.getCreatedAt().toString());
        detailGuestsValue.setText(String.valueOf(booking.getNumberOfGuests()));
        detailPhoneValue.setText(safeText(booking.getPhoneNumber()));
        detailArrivalTimeValue.setText(safeText(booking.getEstimatedArrivalTime()));
        detailSpecialRequestsValue.setText(safeText(booking.getSpecialRequests()));
        detailRejectReasonValue.setText(safeText(booking.getRejectionReason()));
        detailCancelReasonValue.setText(safeText(booking.getCancelReason()));
        detailCancelledAtValue.setText(booking.getCancelledAt() == null ? "-" : booking.getCancelledAt().toString());
    }

    private void clearBookingDetails() {
        detailBookingIdValue.setText("-");
        detailUserIdValue.setText("-");
        detailRoomIdValue.setText("-");
        detailStatusValue.setText("-");
        detailCheckInValue.setText("-");
        detailCheckOutValue.setText("-");
        detailTotalPriceValue.setText("-");
        detailCreatedAtValue.setText("-");
        detailGuestsValue.setText("-");
        detailPhoneValue.setText("-");
        detailArrivalTimeValue.setText("-");
        detailSpecialRequestsValue.setText("-");
        detailRejectReasonValue.setText("-");
        detailCancelReasonValue.setText("-");
        detailCancelledAtValue.setText("-");
    }

    private void updateActionButtonsBySelection(AccommodationBooking booking) {
        if (booking == null) {
            confirmButton.setDisable(true);
            rejectButton.setDisable(true);
            cancelButton.setDisable(true);
            return;
        }
        // Keep actions clickable when a row is selected.
        // Service layer remains the source of truth for allowed transitions.
        confirmButton.setDisable(false);
        rejectButton.setDisable(false);
        cancelButton.setDisable(false);
    }

    private void applyFilters() {
        String selectedStatus = statusFilter == null ? "ALL" : statusFilter.getValue();
        String selectedRoom = roomIdFilter == null ? "ALL" : roomIdFilter.getValue();
        String search = bookingSearchField == null || bookingSearchField.getText() == null
                ? ""
                : bookingSearchField.getText().trim().toLowerCase();
        java.time.LocalDate checkInFrom = checkInFromFilter == null ? null : checkInFromFilter.getValue();
        java.time.LocalDate checkOutTo = checkOutToFilter == null ? null : checkOutToFilter.getValue();
        Double minTotal = parseDoubleOrNull(minTotalFilterField == null ? null : minTotalFilterField.getText());
        Double maxTotal = parseDoubleOrNull(maxTotalFilterField == null ? null : maxTotalFilterField.getText());

        List<AccommodationBooking> filtered = new ArrayList<>();
        for (AccommodationBooking booking : allBookings) {
            if (!matchesStatusFilter(booking, selectedStatus)) {
                continue;
            }
            if (!matchesRoomFilter(booking, selectedRoom)) {
                continue;
            }
            if (!matchesDateFilter(booking, checkInFrom, checkOutTo)) {
                continue;
            }
            if (!matchesTotalFilter(booking, minTotal, maxTotal)) {
                continue;
            }
            if (!matchesSearchFilter(booking, search)) {
                continue;
            }
            filtered.add(booking);
        }

        bookingItems.setAll(filtered);
        bookingsTable.getSelectionModel().clearSelection();
        clearBookingDetails();
        updateActionButtonsBySelection(null);
    }

    private boolean matchesStatusFilter(AccommodationBooking booking, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equalsIgnoreCase("ALL")) {
            return true;
        }
        String status = booking.getStatus() == null ? "" : booking.getStatus().toUpperCase();
        return status.equals(selectedStatus.toUpperCase());
    }

    private boolean matchesRoomFilter(AccommodationBooking booking, String selectedRoom) {
        if (selectedRoom == null || selectedRoom.equalsIgnoreCase("ALL")) {
            return true;
        }
        return String.valueOf(booking.getRoomId()).equals(selectedRoom);
    }

    private boolean matchesDateFilter(AccommodationBooking booking, java.time.LocalDate checkInFrom, java.time.LocalDate checkOutTo) {
        if (checkInFrom != null) {
            if (booking.getCheckIn() == null || booking.getCheckIn().toLocalDate().isBefore(checkInFrom)) {
                return false;
            }
        }
        if (checkOutTo != null) {
            if (booking.getCheckOut() == null || booking.getCheckOut().toLocalDate().isAfter(checkOutTo)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesTotalFilter(AccommodationBooking booking, Double minTotal, Double maxTotal) {
        double total = booking.getTotalPrice();
        if (minTotal != null && total < minTotal) {
            return false;
        }
        if (maxTotal != null && total > maxTotal) {
            return false;
        }
        return true;
    }

    private boolean matchesSearchFilter(AccommodationBooking booking, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        return String.valueOf(booking.getId()).contains(search)
                || String.valueOf(booking.getUserId()).contains(search)
                || String.valueOf(booking.getRoomId()).contains(search)
                || safeText(booking.getPhoneNumber()).toLowerCase().contains(search)
                || safeText(booking.getStatus()).toLowerCase().contains(search);
    }

    private String promptReason(String title, String header, boolean required) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Reason:");
        if (bookingsTable != null && bookingsTable.getScene() != null && bookingsTable.getScene().getWindow() != null) {
            dialog.initOwner(bookingsTable.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return null;
        }

        String reason = result.get().trim();
        if (required && reason.isBlank()) {
            showWarning("Reason is required for this action.");
            return null;
        }
        return reason.isBlank() ? null : reason;
    }

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private void setupRoomIdFilterOptions() {
        if (roomIdFilter == null) {
            return;
        }
        java.util.Set<String> roomIds = new java.util.TreeSet<>();
        for (AccommodationBooking booking : allBookings) {
            roomIds.add(String.valueOf(booking.getRoomId()));
        }
        List<String> options = new ArrayList<>();
        options.add("ALL");
        options.addAll(roomIds);
        roomIdFilter.getItems().setAll(options);
        if (roomIdFilter.getValue() == null || !options.contains(roomIdFilter.getValue())) {
            roomIdFilter.setValue("ALL");
        }
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
