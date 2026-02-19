package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import tn.esprit.entities.Bookingtrans;
import tn.esprit.entities.Schedule;
import tn.esprit.entities.Transport;
import tn.esprit.services.BookingtransService;
import tn.esprit.services.ScheduleService;
import tn.esprit.services.TransportService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TransportUserInterfaceController {

    /* ── FXML Nodes ── */
    @FXML private StackPane contentArea;
    @FXML private Button    btnBrowse;
    @FXML private Button    btnSchedules;
    @FXML private Button    btnMyBookings;
    @FXML private Label     welcomeLabel;

    /* ── Services ── */
    private final TransportService    transportService = new TransportService();
    private final ScheduleService     scheduleService  = new ScheduleService();
    private final BookingtransService bookingService   = new BookingtransService();

    /* ── Session ── */
    // Set this before loading the FXML, e.g. controller.setUserId(loggedInUser.getId())
    private int currentUserId = 1; // default; replace with real session user ID

    /* ── Style Constants ── */
    private static final String NAV_ACTIVE =
            "-fx-background-color: #4FB3B5; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-font-size: 13px; " +
                    "-fx-padding: 10 15 10 15; -fx-cursor: hand; -fx-background-radius: 6;";

    private static final String NAV_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #CCCCCC; " +
                    "-fx-font-size: 13px; -fx-padding: 10 15 10 15; " +
                    "-fx-cursor: hand; -fx-background-radius: 6;";

    private static final String BTN_TEAL =
            "-fx-background-color: #4FB3B5; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 8 18 8 18; " +
                    "-fx-cursor: hand; -fx-background-radius: 5;";

    private static final String BTN_ORANGE =
            "-fx-background-color: #F06E32; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 8 18 8 18; " +
                    "-fx-cursor: hand; -fx-background-radius: 5;";

    private static final String BTN_DARK =
            "-fx-background-color: #1F294C; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 8 18 8 18; " +
                    "-fx-cursor: hand; -fx-background-radius: 5;";

    private static final String BTN_RED =
            "-fx-background-color: #c0392b; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 8 18 8 18; " +
                    "-fx-cursor: hand; -fx-background-radius: 5;";

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /* ══════════════════════════════════════════════════════
       INIT
       ══════════════════════════════════════════════════════ */

    @FXML
    public void initialize() {
        showBrowseTab();
    }

    /** Call this right after FXMLLoader to inject the logged-in user's ID. */
    public void setUserId(int userId) {
        this.currentUserId = userId;
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome, User #" + userId);
    }

    /* ══════════════════════════════════════════════════════
       NAV HIGHLIGHTING
       ══════════════════════════════════════════════════════ */

    private void setActiveNav(Button active) {
        btnBrowse    .setStyle(NAV_INACTIVE);
        btnSchedules .setStyle(NAV_INACTIVE);
        btnMyBookings.setStyle(NAV_INACTIVE);
        active.setStyle(NAV_ACTIVE);
    }

    /* ══════════════════════════════════════════════════════
       LOGOUT
       ══════════════════════════════════════════════════════ */

    @FXML
    public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Logout");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.close();
            }
        });
    }

    /* ══════════════════════════════════════════════════════
       ── BROWSE TRANSPORTS TAB ──
       ══════════════════════════════════════════════════════ */

    @FXML
    public void showBrowseTab() {
        setActiveNav(btnBrowse);
        buildBrowseView();
    }

    @SuppressWarnings("unchecked")
    private void buildBrowseView() {
        TableView<Transport> table = new TableView<>();
        styleTable(table);

        TableColumn<Transport, Integer> idCol    = col("ID",         "transportId",        55);
        TableColumn<Transport, String>  typeCol  = col("Type",       "transportType",      90);
        TableColumn<Transport, String>  provCol  = col("Provider",   "providerName",       150);
        TableColumn<Transport, String>  modelCol = col("Model",      "vehicleModel",       150);
        TableColumn<Transport, Double>  priceCol = col("Base Price", "basePrice",          100);
        TableColumn<Transport, Integer> capCol   = col("Capacity",   "capacity",           80);
        TableColumn<Transport, Integer> unitCol  = col("Available",  "availableUnits",     80);
        TableColumn<Transport, Double>  ecoCol   = col("Eco ★",      "sustainabilityRating", 75);
        TableColumn<Transport, String>  amenCol  = col("Amenities",  "amenities",          180);

        table.getColumns().addAll(idCol, typeCol, provCol, modelCol,
                priceCol, capCol, unitCol, ecoCol, amenCol);

        List<Transport> transports = transportService.getAllTransports();
        table.setItems(FXCollections.observableArrayList(transports));

        // ── Search bar ──
        TextField searchField = new TextField();
        searchField.setPromptText("Search by provider or type...");
        searchField.setStyle("-fx-padding: 7 12 7 12; -fx-background-radius: 5; " +
                "-fx-border-color: #4FB3B5; -fx-border-radius: 5;");
        searchField.setPrefWidth(300);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String lower = newVal.toLowerCase();
            table.setItems(FXCollections.observableArrayList(
                    transports.stream()
                            .filter(t -> t.getProviderName().toLowerCase().contains(lower)
                                    || t.getTransportType().toLowerCase().contains(lower)
                                    || t.getVehicleModel().toLowerCase().contains(lower))
                            .toList()
            ));
        });

        Button bookBtn = btn("✈  Book This Transport", BTN_TEAL);
        bookBtn.setOnAction(e -> {
            Transport sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openBookingForm(sel, null);
            else showAlert("Please select a transport first.");
        });

        HBox topBar = new HBox(12, searchField, new Region(), bookBtn);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = sectionTitle("✈   Available Transports");
        VBox layout = new VBox(14, title, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        contentArea.getChildren().setAll(layout);
    }

    /* ══════════════════════════════════════════════════════
       ── VIEW SCHEDULES TAB ──
       ══════════════════════════════════════════════════════ */

    @FXML
    public void showSchedulesTab() {
        setActiveNav(btnSchedules);
        buildSchedulesView();
    }

    @SuppressWarnings("unchecked")
    private void buildSchedulesView() {
        TableView<Schedule> table = new TableView<>();
        styleTable(table);

        TableColumn<Schedule, Integer> idCol     = col("ID",         "scheduleId",       55);
        TableColumn<Schedule, Integer> transCol  = col("Transport",  "transportId",      90);
        TableColumn<Schedule, String>  classCol  = col("Class",      "travelClass",      90);
        TableColumn<Schedule, String>  statusCol = col("Status",     "status",           100);
        TableColumn<Schedule, Double>  multCol   = col("Price ×",    "priceMultiplier",  80);
        TableColumn<Schedule, Integer> delayCol  = col("Delay(min)", "delayMinutes",     90);
        TableColumn<Schedule, Double>  demandCol = col("Demand",     "aiDemandScore",    80);

        /* Departure column formatted */
        TableColumn<Schedule, LocalDateTime> depCol = new TableColumn<>("Departure");
        depCol.setCellValueFactory(new PropertyValueFactory<>("departureDatetime"));
        depCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(DT_FMT));
            }
        });
        depCol.setPrefWidth(130);

        /* Arrival column formatted */
        TableColumn<Schedule, LocalDateTime> arrCol = new TableColumn<>("Arrival");
        arrCol.setCellValueFactory(new PropertyValueFactory<>("arrivalDatetime"));
        arrCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(DT_FMT));
            }
        });
        arrCol.setPrefWidth(130);

        /* Status color-coded */
        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "ON_TIME"   -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "DELAYED"   -> setStyle("-fx-text-fill: #F06E32; -fx-font-weight: bold;");
                    case "CANCELLED" -> setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    default          -> setStyle("");
                }
            }
        });

        table.getColumns().addAll(idCol, transCol, depCol, arrCol,
                classCol, statusCol, multCol, delayCol, demandCol);

        List<Schedule> schedules = scheduleService.getAllSchedules();
        table.setItems(FXCollections.observableArrayList(schedules));

        /* Filter: only ON_TIME */
        CheckBox onlyAvailable = new CheckBox("Show only ON TIME schedules");
        onlyAvailable.setStyle("-fx-text-fill: #1F294C; -fx-font-weight: bold;");
        onlyAvailable.selectedProperty().addListener((obs, old, checked) -> {
            if (checked) {
                table.setItems(FXCollections.observableArrayList(
                        schedules.stream().filter(s -> "ON_TIME".equals(s.getStatus())).toList()
                ));
            } else {
                table.setItems(FXCollections.observableArrayList(schedules));
            }
        });

        Button bookBtn = btn("📋  Book Selected Schedule", BTN_TEAL);
        bookBtn.setOnAction(e -> {
            Schedule sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Please select a schedule first."); return; }
            if ("CANCELLED".equals(sel.getStatus())) {
                showAlert("This schedule has been cancelled and cannot be booked.");
                return;
            }
            openBookingForm(null, sel);
        });

        HBox topBar = new HBox(12, onlyAvailable, new Region(), bookBtn);
        HBox.setHgrow(topBar.getChildren().get(1), Priority.ALWAYS);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = sectionTitle("🗓   Available Schedules");
        VBox layout = new VBox(14, title, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        contentArea.getChildren().setAll(layout);
    }

    /* ══════════════════════════════════════════════════════
       ── BOOKING FORM POPUP ──
       Called from both Browse and Schedules tabs.
       ══════════════════════════════════════════════════════ */

    /**
     * Class multipliers relative to ECONOMY base price.
     * ECONOMY = 1.0x  (the base price the admin entered)
     * PREMIUM  = 1.5x
     * BUSINESS = 2.2x
     * FIRST    = 3.0x
     *
     * If the admin set the transport's base price for PREMIUM (e.g. €150),
     * that is treated as the ECONOMY anchor and all classes scale from it.
     * The schedule's priceMultiplier (demand/delay factor) is applied on top.
     */
    private double classMultiplier(String travelClass) {
        return switch (travelClass == null ? "ECONOMY" : travelClass) {
            case "PREMIUM"  -> 1.5;
            case "BUSINESS" -> 2.2;
            case "FIRST"    -> 3.0;
            default         -> 1.0; // ECONOMY
        };
    }

    private void openBookingForm(Transport transport, Schedule schedule) {
        Stage popup = popupStage("Book Your Trip");

        /* Pre-fill known IDs */
        int preTransportId = (transport != null) ? transport.getTransportId()
                : (schedule != null)  ? schedule.getTransportId() : 0;
        int preScheduleId  = (schedule != null)  ? schedule.getScheduleId() : 0;

        /* Resolve base price once */
        final double basePrice;
        if (transport != null) {
            basePrice = transport.getBasePrice();
        } else if (schedule != null) {
            basePrice = transportService.getAllTransports().stream()
                    .filter(t -> t.getTransportId() == schedule.getTransportId())
                    .mapToDouble(Transport::getBasePrice)
                    .findFirst().orElse(0);
        } else {
            basePrice = 0;
        }

        /* Schedule price multiplier (demand/delay factor set by admin) */
        final double scheduleMultiplier = (schedule != null) ? schedule.getPriceMultiplier() : 1.0;

        /* Fields */
        TextField transportIdField = new TextField(String.valueOf(preTransportId));
        transportIdField.setEditable(false);
        transportIdField.setStyle("-fx-background-color: #EEEEEE;");

        TextField scheduleIdField = new TextField(preScheduleId > 0 ? String.valueOf(preScheduleId) : "—");
        scheduleIdField.setEditable(false);
        scheduleIdField.setStyle("-fx-background-color: #EEEEEE;");

        Spinner<Integer> adultsSpinner   = new Spinner<>(1, 20, 1);
        Spinner<Integer> childrenSpinner = new Spinner<>(0, 20, 0);
        adultsSpinner.setEditable(true);
        childrenSpinner.setEditable(true);

        CheckBox insuranceCheck = new CheckBox("Add travel insurance (+€25/seat)");
        insuranceCheck.setStyle("-fx-text-fill: #1F294C;");

        ComboBox<String> classBox = new ComboBox<>(
                FXCollections.observableArrayList("ECONOMY", "PREMIUM", "BUSINESS", "FIRST"));
        // Default to the schedule's class if available, otherwise ECONOMY
        if (schedule != null && schedule.getTravelClass() != null)
            classBox.setValue(schedule.getTravelClass());
        else
            classBox.setValue("ECONOMY");

        /* Live price preview — updates whenever class, adults, children or insurance changes */
        Label pricePreview = new Label();
        pricePreview.setStyle("-fx-text-fill: #1F294C; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-background-color: #FFFFFF; -fx-padding: 6 12 6 12; -fx-background-radius: 5;");

        Runnable updatePrice = () -> {
            int seats = adultsSpinner.getValue() + childrenSpinner.getValue();
            double classMult    = classMultiplier(classBox.getValue());
            double insurance    = insuranceCheck.isSelected() ? 25.0 * seats : 0;
            double total        = (basePrice * scheduleMultiplier * classMult * seats) + insurance;
            pricePreview.setText(String.format("Estimated Total: €%.2f  (%s × %.1fx class × %d seat%s%s)",
                    total,
                    String.format("€%.2f", basePrice * scheduleMultiplier),
                    classMult,
                    seats,
                    seats > 1 ? "s" : "",
                    insuranceCheck.isSelected() ? " + insurance" : ""));
        };

        classBox.valueProperty().addListener((o, old, nv) -> updatePrice.run());
        adultsSpinner.valueProperty().addListener((o, old, nv) -> updatePrice.run());
        childrenSpinner.valueProperty().addListener((o, old, nv) -> updatePrice.run());
        insuranceCheck.selectedProperty().addListener((o, old, nv) -> updatePrice.run());
        updatePrice.run(); // set initial value

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, formLabel("Transport ID"),  transportIdField);
        grid.addRow(r++, formLabel("Schedule ID"),   scheduleIdField);
        grid.addRow(r++, formLabel("Travel Class"),  classBox);
        grid.addRow(r++, formLabel("Adults"),        adultsSpinner);
        grid.addRow(r++, formLabel("Children"),      childrenSpinner);
        grid.addRow(r++, formLabel("Insurance"),     insuranceCheck);
        grid.addRow(r++, formLabel("Price"),         pricePreview);

        Button confirmBtn = btn("✅  Confirm Booking", BTN_TEAL);
        Button cancelBtn  = btn("✖  Cancel", BTN_DARK);
        cancelBtn.setOnAction(e -> popup.close());

        confirmBtn.setOnAction(e -> {
            try {
                int tId        = preTransportId;
                int sId        = preScheduleId;
                int adults     = adultsSpinner.getValue();
                int children   = childrenSpinner.getValue();
                int totalSeats = adults + children;

                if (tId <= 0) { showAlert("Invalid transport ID."); return; }

                double classMult   = classMultiplier(classBox.getValue());
                double insurance   = insuranceCheck.isSelected() ? 25.0 * totalSeats : 0;
                double total       = (basePrice * scheduleMultiplier * classMult * totalSeats) + insurance;

                Bookingtrans booking = new Bookingtrans();
                booking.setUserId(currentUserId);
                booking.setTransportId(tId);
                booking.setScheduleId(sId);
                booking.setAdultsCount(adults);
                booking.setChildrenCount(children);
                booking.setTotalSeats(totalSeats);
                booking.setBookingStatus("PENDING");
                booking.setPaymentStatus("UNPAID");
                booking.setInsuranceIncluded(insuranceCheck.isSelected());
                booking.setBookingDate(LocalDateTime.now());
                booking.setTotalPrice(total);

                bookingService.addBookingtrans(booking);
                popup.close();
                showSuccess(String.format(
                        "Booking confirmed!\nClass: %s\nTotal: €%.2f\nStatus: PENDING (awaiting admin confirmation)",
                        classBox.getValue(), total));
                showMyBookingsTab();

            } catch (Exception ex) {
                showAlert("Booking failed: " + ex.getMessage());
            }
        });

        VBox root = popupRoot("Book Your Trip", grid, confirmBtn, cancelBtn);
        popup.setScene(new Scene(root, 560, 460));
        popup.showAndWait();
    }

    /* ══════════════════════════════════════════════════════
       ── MY BOOKINGS TAB ──
       ══════════════════════════════════════════════════════ */

    @FXML
    public void showMyBookingsTab() {
        setActiveNav(btnMyBookings);
        buildMyBookingsView();
    }

    @SuppressWarnings("unchecked")
    private void buildMyBookingsView() {
        TableView<Bookingtrans> table = new TableView<>();
        styleTable(table);

        TableColumn<Bookingtrans, Integer> idCol      = col("ID",          "bookingId",       55);
        TableColumn<Bookingtrans, Integer> transCol   = col("Transport",   "transportId",     90);
        TableColumn<Bookingtrans, Integer> schedCol   = col("Schedule",    "scheduleId",      80);
        TableColumn<Bookingtrans, Integer> seatsCol   = col("Seats",       "totalSeats",      60);
        TableColumn<Bookingtrans, Double>  priceCol   = col("Total (€)",   "totalPrice",      90);
        TableColumn<Bookingtrans, String>  statusCol  = col("Status",      "bookingStatus",   110);
        TableColumn<Bookingtrans, String>  payCol     = col("Payment",     "paymentStatus",   100);
        TableColumn<Bookingtrans, Boolean> insCol     = col("Insurance",   "insuranceIncluded", 80);

        /* Booking date column */
        TableColumn<Bookingtrans, LocalDateTime> dateCol = new TableColumn<>("Booked On");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        dateCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(DT_FMT));
            }
        });
        dateCol.setPrefWidth(130);

        /* Status colour */
        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "CONFIRMED"  -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "CANCELLED"  -> setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    default           -> setStyle("-fx-text-fill: #F06E32; -fx-font-weight: bold;");
                }
            }
        });

        table.getColumns().addAll(idCol, transCol, schedCol, dateCol,
                seatsCol, priceCol, statusCol, payCol, insCol);

        // Use getBookingsByUserId to avoid the INNER JOIN on schedule_id
        // that getAllBookings() uses — this ensures transport-only bookings
        // (schedule_id = 0 / NULL) are also included.
        List<Bookingtrans> myBookings = bookingService.getBookingsByUserId(currentUserId);

        table.setItems(FXCollections.observableArrayList(myBookings));

        /* Buttons */
        Button cancelBtn  = btn("❌  Cancel Booking",  BTN_RED);
        Button detailBtn  = btn("🔍  View Details",    BTN_TEAL);
        Button refreshBtn = btn("🔄  Refresh",         BTN_DARK);

        cancelBtn.setOnAction(e -> {
            Bookingtrans sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a booking first."); return; }
            if ("CANCELLED".equals(sel.getBookingStatus())) {
                showAlert("This booking is already cancelled.");
                return;
            }
            if ("CONFIRMED".equals(sel.getBookingStatus())) {
                showAlert("Confirmed bookings cannot be self-cancelled.\nPlease contact support.");
                return;
            }
            if (confirm("Cancel your booking #" + sel.getBookingId() + "?\nThis cannot be undone.")) {
                sel.setBookingStatus("CANCELLED");
                sel.setCancellationReason("Cancelled by user");
                bookingService.updateBookingtrans(sel);
                showSuccess("Booking #" + sel.getBookingId() + " cancelled.");
                buildMyBookingsView();
            }
        });

        detailBtn.setOnAction(e -> {
            Bookingtrans sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) viewBookingDetail(sel);
            else showAlert("Select a booking first.");
        });

        refreshBtn.setOnAction(e -> buildMyBookingsView());

        HBox toolbar = toolbar(cancelBtn, detailBtn, refreshBtn);

        /* Summary bar */
        long total     = myBookings.size();
        long pending   = myBookings.stream().filter(b -> "PENDING".equals(b.getBookingStatus())).count();
        long confirmed = myBookings.stream().filter(b -> "CONFIRMED".equals(b.getBookingStatus())).count();
        double spent   = myBookings.stream()
                .filter(b -> !"CANCELLED".equals(b.getBookingStatus()))
                .mapToDouble(Bookingtrans::getTotalPrice).sum();

        Label summary = new Label(String.format(
                "Total Bookings: %d   |   Confirmed: %d   |   Pending: %d   |   Total Spent: €%.2f",
                total, confirmed, pending, spent));
        summary.setStyle("-fx-text-fill: #1F294C; -fx-font-weight: bold; " +
                "-fx-background-color: #FFFFFF; -fx-padding: 8 14 8 14; " +
                "-fx-background-radius: 6; -fx-font-size: 12px;");

        Label title = sectionTitle("📋   My Bookings");
        VBox layout = new VBox(14, title, summary, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        contentArea.getChildren().setAll(layout);
    }

    /* ── Booking Detail Popup (read-only) ── */
    private void viewBookingDetail(Bookingtrans b) {
        Stage popup = popupStage("Booking Details – #" + b.getBookingId());

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, formLabel("Booking ID"),    new Label(String.valueOf(b.getBookingId())));
        grid.addRow(r++, formLabel("Transport ID"),  new Label(String.valueOf(b.getTransportId())));
        grid.addRow(r++, formLabel("Schedule ID"),   new Label(String.valueOf(b.getScheduleId())));
        grid.addRow(r++, formLabel("Booked On"),     new Label(b.getBookingDate() == null ? "N/A" : b.getBookingDate().format(DT_FMT)));
        grid.addRow(r++, formLabel("Adults"),        new Label(String.valueOf(b.getAdultsCount())));
        grid.addRow(r++, formLabel("Children"),      new Label(String.valueOf(b.getChildrenCount())));
        grid.addRow(r++, formLabel("Total Seats"),   new Label(String.valueOf(b.getTotalSeats())));
        grid.addRow(r++, formLabel("Total Price"),   new Label(String.format("€ %.2f", b.getTotalPrice())));
        grid.addRow(r++, formLabel("Status"),        new Label(b.getBookingStatus()));
        grid.addRow(r++, formLabel("Payment"),       new Label(b.getPaymentStatus()));
        grid.addRow(r++, formLabel("Insurance"),     new Label(b.isInsuranceIncluded() ? "Yes (+€25/seat)" : "No"));
        grid.addRow(r++, formLabel("Cancel Reason"), new Label(b.getCancellationReason() == null ? "—" : b.getCancellationReason()));
        if (b.getQrCode() != null)
            grid.addRow(r++, formLabel("QR Code"),   new Label(b.getQrCode()));
        if (b.getVoucherPath() != null)
            grid.addRow(r++, formLabel("Voucher"),   new Label(b.getVoucherPath()));

        Button closeBtn = btn("✖  Close", BTN_DARK);
        closeBtn.setOnAction(e -> popup.close());

        VBox root = popupRoot("Booking Details", grid, closeBtn);
        popup.setScene(new Scene(root, 480, 520));
        popup.showAndWait();
    }

    /* ══════════════════════════════════════════════════════
       ── SHARED UI HELPERS ──
       ══════════════════════════════════════════════════════ */

    private <T> void styleTable(TableView<T> tv) {
        tv.setStyle(
                "-fx-background-color: #FFFFFF; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);"
        );
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private <T, V> TableColumn<T, V> col(String title, String prop, double width) {
        TableColumn<T, V> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(width);
        return c;
    }

    private Button btn(String text, String style) {
        Button b = new Button(text);
        b.setStyle(style);
        return b;
    }

    private HBox toolbar(Button... buttons) {
        HBox box = new HBox(10, buttons);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1F294C;");
        return l;
    }

    private Stage popupStage(String title) {
        Stage s = new Stage();
        s.setTitle(title);
        s.initModality(Modality.APPLICATION_MODAL);
        s.setResizable(false);
        return s;
    }

    private GridPane formGrid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(160);
        ColumnConstraints c2 = new ColumnConstraints(280);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    private Label formLabel(String text) {
        Label l = new Label(text + ":");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #1F294C;");
        l.setAlignment(Pos.CENTER_RIGHT);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private VBox popupRoot(String headerText, GridPane grid, Button... actionBtns) {
        Label header = new Label(headerText);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-text-fill: white; -fx-padding: 0 0 0 5;");

        HBox headerBar = new HBox(header);
        headerBar.setStyle("-fx-background-color: #1F294C; -fx-padding: 14 18 14 18;");
        headerBar.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox btnRow = new HBox(10, actionBtns);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));

        VBox content = new VBox(12, scroll, btnRow);
        content.setPadding(new Insets(18));
        content.setStyle("-fx-background-color: #F1EAE7;");
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox root = new VBox(headerBar, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        return root;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}