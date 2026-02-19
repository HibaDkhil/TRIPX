package tn.esprit.controllers.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.format.DateTimeParseException;

public class TransportAdminDashboardController {

    /* ── FXML Nodes ── */
    @FXML private StackPane contentArea;
    @FXML private Button btnTransport;
    @FXML private Button btnSchedule;
    @FXML private Button btnBooking;

    /* ── Services ── */
    private final TransportService    transportService = new TransportService();
    private final ScheduleService     scheduleService  = new ScheduleService();
    private final BookingtransService bookingService   = new BookingtransService();

    /*
     * NO shared TableView fields — each build method creates a fresh local
     * TableView to avoid the JavaFX "node already has a parent" bug that
     * causes the wrong tab content to appear when switching tabs.
     */

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

    private static final String BTN_GREEN =
            "-fx-background-color: #27ae60; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 8 18 8 18; " +
                    "-fx-cursor: hand; -fx-background-radius: 5;";

    private static final String BTN_RED =
            "-fx-background-color: #c0392b; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-padding: 8 18 8 18; " +
                    "-fx-cursor: hand; -fx-background-radius: 5;";

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /* ════════════════════════════════════════
       INIT
       ════════════════════════════════════════ */

    @FXML
    public void initialize() {
        showTransportTab();
    }

    /* ════════════════════════════════════════
       NAV BUTTON HIGHLIGHTING
       ════════════════════════════════════════ */

    private void setActiveNav(Button active) {
        btnTransport.setStyle(NAV_INACTIVE);
        btnSchedule .setStyle(NAV_INACTIVE);
        btnBooking  .setStyle(NAV_INACTIVE);
        active.setStyle(NAV_ACTIVE);
    }

    /* ════════════════════════════════════════
       LOGOUT
       ════════════════════════════════════════ */

    @FXML
    public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Logout");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                stage.close();
            }
        });
    }

    /* ════════════════════════════════════════
       ── TRANSPORT TAB ──
       ════════════════════════════════════════ */

    @FXML
    public void showTransportTab() {
        setActiveNav(btnTransport);
        buildTransportView();
    }

    @SuppressWarnings("unchecked")
    private void buildTransportView() {
        // Fresh local table every call — no parent conflict
        TableView<Transport> transportTable = new TableView<>();
        styleTable(transportTable);

        TableColumn<Transport, Integer> idCol    = col("ID",         "transportId",         60);
        TableColumn<Transport, String>  typeCol  = col("Type",       "transportType",        90);
        TableColumn<Transport, String>  provCol  = col("Provider",   "providerName",        140);
        TableColumn<Transport, String>  modelCol = col("Model",      "vehicleModel",        140);
        TableColumn<Transport, Double>  priceCol = col("Base Price", "basePrice",           100);
        TableColumn<Transport, Integer> capCol   = col("Capacity",   "capacity",             90);
        TableColumn<Transport, Integer> unitsCol = col("Units",      "availableUnits",       80);
        TableColumn<Transport, Double>  ecoCol   = col("Eco ★",     "sustainabilityRating", 80);
        TableColumn<Transport, Boolean> activeCol= col("Active",     "isActive",             70);

        transportTable.getColumns().addAll(
                idCol, typeCol, provCol, modelCol,
                priceCol, capCol, unitsCol, ecoCol, activeCol);

        transportTable.setItems(
                FXCollections.observableArrayList(transportService.getAllTransports()));

        Button addBtn    = btn("➕  Add",          BTN_TEAL);
        Button updateBtn = btn("✏️  Update",       BTN_ORANGE);
        Button deleteBtn = btn("🗑  Delete",        BTN_DARK);
        Button toggleBtn = btn("⏻  Toggle Active", BTN_GREEN);

        addBtn.setOnAction(e -> {
            openTransportForm(null);
            transportTable.setItems(
                    FXCollections.observableArrayList(transportService.getAllTransports()));
        });
        updateBtn.setOnAction(e -> {
            Transport sel = transportTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                openTransportForm(sel);
                transportTable.setItems(
                        FXCollections.observableArrayList(transportService.getAllTransports()));
            } else showAlert("Select a transport row first.");
        });
        deleteBtn.setOnAction(e -> {
            Transport sel = transportTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a transport row first."); return; }
            if (confirm("Delete transport: " + sel.getProviderName() + "?")) {
                transportService.deleteTransport(sel.getTransportId());
                transportTable.setItems(
                        FXCollections.observableArrayList(transportService.getAllTransports()));
                showSuccess("Transport deleted.");
            }
        });
        toggleBtn.setOnAction(e -> {
            Transport sel = transportTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a transport row first."); return; }
            sel.setActive(!sel.isActive());
            transportService.updateTransport(sel);
            transportTable.setItems(
                    FXCollections.observableArrayList(transportService.getAllTransports()));
            showSuccess("Transport status toggled.");
        });

        HBox toolbar = toolbar(addBtn, updateBtn, deleteBtn, toggleBtn);
        Label title  = sectionTitle("✈   Transport Management");
        VBox layout  = new VBox(14, title, toolbar, transportTable);
        VBox.setVgrow(transportTable, Priority.ALWAYS);
        contentArea.getChildren().setAll(layout);
    }

    /* ── Transport Add/Edit Popup ── */

    private void openTransportForm(Transport existing) {
        boolean isEdit = (existing != null);
        Stage popup = popupStage(isEdit ? "Update Transport" : "Add Transport");

        ComboBox<String> typeBox = new ComboBox<>(
                FXCollections.observableArrayList("FLIGHT", "VEHICLE"));
        TextField providerField = new TextField();
        TextField modelField    = new TextField();
        TextField priceField    = new TextField();
        TextField capField      = new TextField();
        TextField unitsField    = new TextField();
        TextField ecoField      = new TextField();
        TextArea  amenitiesArea = new TextArea();
        amenitiesArea.setPrefRowCount(2);
        TextField imageField    = new TextField();

        placeholders(providerField, "e.g. Air France");
        placeholders(modelField,    "e.g. Boeing 737 / Toyota Corolla");
        placeholders(priceField,    "e.g. 250.00");
        placeholders(capField,      "e.g. 180");
        placeholders(unitsField,    "e.g. 180");
        placeholders(ecoField,      "e.g. 4.5  (0-5)");
        placeholders(imageField,    "URL or file path");

        if (isEdit) {
            typeBox.setValue(existing.getTransportType());
            providerField.setText(existing.getProviderName());
            modelField.setText(existing.getVehicleModel());
            priceField.setText(String.valueOf(existing.getBasePrice()));
            capField.setText(String.valueOf(existing.getCapacity()));
            unitsField.setText(String.valueOf(existing.getAvailableUnits()));
            ecoField.setText(String.valueOf(existing.getSustainabilityRating()));
            amenitiesArea.setText(existing.getAmenities());
            imageField.setText(existing.getImageUrl());
        } else {
            typeBox.setValue("FLIGHT");
        }

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, formLabel("Type"),            typeBox);
        grid.addRow(r++, formLabel("Provider Name"),   providerField);
        grid.addRow(r++, formLabel("Model"),           modelField);
        grid.addRow(r++, formLabel("Base Price (€)"),  priceField);
        grid.addRow(r++, formLabel("Capacity"),        capField);
        grid.addRow(r++, formLabel("Available Units"), unitsField);
        grid.addRow(r++, formLabel("Eco Rating"),      ecoField);
        grid.addRow(r++, formLabel("Amenities"),       amenitiesArea);
        grid.addRow(r++, formLabel("Image URL"),       imageField);

        Button saveBtn   = btn(isEdit ? "💾  Save Changes" : "➕  Add Transport", BTN_TEAL);
        Button cancelBtn = btn("✖  Cancel", BTN_DARK);
        cancelBtn.setOnAction(e -> popup.close());

        saveBtn.setOnAction(e -> {
            try {
                Transport t = isEdit ? existing : new Transport();
                t.setTransportType(typeBox.getValue());
                t.setProviderName(providerField.getText().trim());
                t.setVehicleModel(modelField.getText().trim());
                t.setBasePrice(Double.parseDouble(priceField.getText().trim()));
                t.setCapacity(Integer.parseInt(capField.getText().trim()));
                t.setAvailableUnits(Integer.parseInt(unitsField.getText().trim()));
                t.setSustainabilityRating(Double.parseDouble(ecoField.getText().trim()));
                t.setAmenities(amenitiesArea.getText().trim());
                t.setImageUrl(imageField.getText().trim());

                if (isEdit) transportService.updateTransport(t);
                else        transportService.addTransport(t);

                popup.close();
                showSuccess(isEdit ? "Transport updated!" : "Transport added!");
            } catch (NumberFormatException ex) {
                showAlert("Please enter valid numbers for price, capacity, units and eco rating.");
            }
        });

        VBox root = popupRoot(isEdit ? "Update Transport" : "Add Transport",
                grid, saveBtn, cancelBtn);
        popup.setScene(new Scene(root, 520, 560));
        popup.showAndWait();
    }

    /* ════════════════════════════════════════
       ── SCHEDULE TAB ──
       ════════════════════════════════════════ */

    @FXML
    public void showScheduleTab() {
        setActiveNav(btnSchedule);
        buildScheduleView();
    }

    @SuppressWarnings("unchecked")
    private void buildScheduleView() {
        // Fresh local table every call — no parent conflict
        TableView<Schedule> scheduleTable = new TableView<>();
        styleTable(scheduleTable);

        TableColumn<Schedule, Integer> idCol     = col("ID",         "scheduleId",      55);
        TableColumn<Schedule, Integer> transCol  = col("Transport",  "transportId",     90);
        TableColumn<Schedule, String>  classCol  = col("Class",      "travelClass",     90);
        TableColumn<Schedule, String>  statusCol = col("Status",     "status",         100);
        TableColumn<Schedule, Integer> delayCol  = col("Delay(min)", "delayMinutes",    95);
        TableColumn<Schedule, Double>  multCol   = col("Price×",     "priceMultiplier", 75);
        TableColumn<Schedule, Double>  demandCol = col("AI Demand",  "aiDemandScore",   90);

        TableColumn<Schedule, LocalDateTime> depCol = new TableColumn<>("Departure");
        depCol.setCellValueFactory(new PropertyValueFactory<>("departureDatetime"));
        depCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(DT_FMT));
            }
        });
        depCol.setPrefWidth(130);

        scheduleTable.getColumns().addAll(
                idCol, transCol, depCol, classCol, statusCol, delayCol, multCol, demandCol);

        scheduleTable.setItems(
                FXCollections.observableArrayList(scheduleService.getAllSchedules()));

        Button addBtn    = btn("➕  Add",            BTN_TEAL);
        Button updateBtn = btn("✏️  Update",         BTN_ORANGE);
        Button deleteBtn = btn("🗑  Delete",          BTN_DARK);
        Button delayBtn  = btn("⏱  Mark Delayed",    BTN_ORANGE);
        Button cancelBtn = btn("✖  Cancel Schedule", BTN_RED);

        addBtn.setOnAction(e -> {
            openScheduleForm(null);
            scheduleTable.setItems(
                    FXCollections.observableArrayList(scheduleService.getAllSchedules()));
        });
        updateBtn.setOnAction(e -> {
            Schedule sel = scheduleTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                openScheduleForm(sel);
                scheduleTable.setItems(
                        FXCollections.observableArrayList(scheduleService.getAllSchedules()));
            } else showAlert("Select a schedule row first.");
        });
        deleteBtn.setOnAction(e -> {
            Schedule sel = scheduleTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a schedule row first."); return; }
            if (confirm("Delete schedule #" + sel.getScheduleId() + "?")) {
                scheduleService.deleteSchedule(sel.getScheduleId());
                scheduleTable.setItems(
                        FXCollections.observableArrayList(scheduleService.getAllSchedules()));
                showSuccess("Schedule deleted.");
            }
        });
        delayBtn.setOnAction(e -> {
            Schedule sel = scheduleTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a schedule row first."); return; }
            TextInputDialog dlg = new TextInputDialog("30");
            dlg.setTitle("Mark Delayed");
            dlg.setHeaderText("Enter delay in minutes:");
            dlg.setContentText("Minutes:");
            dlg.showAndWait().ifPresent(val -> {
                try {
                    sel.setDelayMinutes(Integer.parseInt(val.trim()));
                    sel.setStatus("DELAYED");
                    scheduleService.updateSchedule(sel);
                    scheduleTable.setItems(
                            FXCollections.observableArrayList(scheduleService.getAllSchedules()));
                    showSuccess("Schedule marked as DELAYED.");
                } catch (NumberFormatException ex) {
                    showAlert("Invalid number.");
                }
            });
        });
        cancelBtn.setOnAction(e -> {
            Schedule sel = scheduleTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a schedule row first."); return; }
            if (confirm("Cancel schedule #" + sel.getScheduleId() + "?")) {
                sel.setStatus("CANCELLED");
                scheduleService.updateSchedule(sel);
                scheduleTable.setItems(
                        FXCollections.observableArrayList(scheduleService.getAllSchedules()));
                showSuccess("Schedule cancelled.");
            }
        });

        HBox toolbar = toolbar(addBtn, updateBtn, deleteBtn, delayBtn, cancelBtn);
        Label title  = sectionTitle("🗓   Schedule Management");
        VBox layout  = new VBox(14, title, toolbar, scheduleTable);
        VBox.setVgrow(scheduleTable, Priority.ALWAYS);
        contentArea.getChildren().setAll(layout);
    }

    /* ── Schedule Add/Edit Popup ── */

    private void openScheduleForm(Schedule existing) {
        boolean isEdit = (existing != null);
        Stage popup = popupStage(isEdit ? "Update Schedule" : "Add Schedule");

        TextField transIdField = new TextField();
        TextField depDestField = new TextField();
        TextField arrDestField = new TextField();
        TextField depDateField = new TextField();
        TextField arrDateField = new TextField();
        TextField rentalSField = new TextField();
        TextField rentalEField = new TextField();
        TextField multField    = new TextField();
        TextField demandField  = new TextField();

        ComboBox<String> classBox  = new ComboBox<>(
                FXCollections.observableArrayList("ECONOMY","PREMIUM","BUSINESS","FIRST"));
        ComboBox<String> statusBox = new ComboBox<>(
                FXCollections.observableArrayList("ON_TIME","DELAYED","CANCELLED"));

        placeholders(transIdField, "Transport ID");
        placeholders(depDestField, "Departure Destination ID");
        placeholders(arrDestField, "Arrival Destination ID");
        placeholders(depDateField, "yyyy-MM-dd HH:mm");
        placeholders(arrDateField, "yyyy-MM-dd HH:mm");
        placeholders(rentalSField, "yyyy-MM-dd HH:mm  (vehicle only — leave empty for flights)");
        placeholders(rentalEField, "yyyy-MM-dd HH:mm  (vehicle only — leave empty for flights)");
        placeholders(multField,    "e.g. 1.0");
        placeholders(demandField,  "e.g. 0.75");

        if (isEdit) {
            transIdField.setText(String.valueOf(existing.getTransportId()));
            depDestField.setText(String.valueOf(existing.getDepartureDestinationId()));
            arrDestField.setText(String.valueOf(existing.getArrivalDestinationId()));
            if (existing.getDepartureDatetime() != null)
                depDateField.setText(existing.getDepartureDatetime().format(DT_FMT));
            if (existing.getArrivalDatetime() != null)
                arrDateField.setText(existing.getArrivalDatetime().format(DT_FMT));
            if (existing.getRentalStart() != null)
                rentalSField.setText(existing.getRentalStart().format(DT_FMT));
            if (existing.getRentalEnd() != null)
                rentalEField.setText(existing.getRentalEnd().format(DT_FMT));
            classBox .setValue(existing.getTravelClass());
            statusBox.setValue(existing.getStatus());
            multField.setText(String.valueOf(existing.getPriceMultiplier()));
            demandField.setText(String.valueOf(existing.getAiDemandScore()));
        } else {
            classBox .setValue("ECONOMY");
            statusBox.setValue("ON_TIME");
            multField.setText("1.0");
            demandField.setText("0.0");
        }

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, formLabel("Transport ID"),        transIdField);
        grid.addRow(r++, formLabel("Dep. Destination ID"), depDestField);
        grid.addRow(r++, formLabel("Arr. Destination ID"), arrDestField);
        grid.addRow(r++, formLabel("Departure DateTime"),  depDateField);
        grid.addRow(r++, formLabel("Arrival DateTime"),    arrDateField);
        grid.addRow(r++, formLabel("Rental Start"),        rentalSField);
        grid.addRow(r++, formLabel("Rental End"),          rentalEField);
        grid.addRow(r++, formLabel("Travel Class"),        classBox);
        grid.addRow(r++, formLabel("Status"),              statusBox);
        grid.addRow(r++, formLabel("Price Multiplier"),    multField);
        grid.addRow(r++, formLabel("AI Demand Score"),     demandField);

        Button saveBtn   = btn(isEdit ? "💾  Save Changes" : "➕  Add Schedule", BTN_TEAL);
        Button cancelBtn = btn("✖  Cancel", BTN_DARK);
        cancelBtn.setOnAction(e -> popup.close());

        saveBtn.setOnAction(e -> {
            try {
                Schedule s = isEdit ? existing : new Schedule();
                s.setTransportId(Integer.parseInt(transIdField.getText().trim()));
                s.setDepartureDestinationId(Long.parseLong(depDestField.getText().trim()));
                s.setArrivalDestinationId(Long.parseLong(arrDestField.getText().trim()));
                s.setDepartureDatetime(LocalDateTime.parse(depDateField.getText().trim(), DT_FMT));
                s.setArrivalDatetime(LocalDateTime.parse(arrDateField.getText().trim(), DT_FMT));

                String rs = rentalSField.getText().trim();
                String re = rentalEField.getText().trim();
                s.setRentalStart(rs.isEmpty() ? null : LocalDateTime.parse(rs, DT_FMT));
                s.setRentalEnd  (re.isEmpty() ? null : LocalDateTime.parse(re, DT_FMT));

                s.setTravelClass(classBox.getValue());
                s.setStatus(statusBox.getValue());
                s.setPriceMultiplier(Double.parseDouble(multField.getText().trim()));
                s.setAiDemandScore(Double.parseDouble(demandField.getText().trim()));

                if (isEdit) scheduleService.updateSchedule(s);
                else        scheduleService.addSchedule(s);

                popup.close();
                showSuccess(isEdit ? "Schedule updated!" : "Schedule added!");
            } catch (NumberFormatException ex) {
                showAlert("Invalid number in one of the numeric fields.");
            } catch (DateTimeParseException ex) {
                showAlert("Date format must be yyyy-MM-dd HH:mm  (e.g. 2025-08-01 14:30)");
            } catch (Exception ex) {
                showAlert("Unexpected error: " + ex.getMessage());
            }
        });

        VBox root = popupRoot(isEdit ? "Update Schedule" : "Add Schedule",
                grid, saveBtn, cancelBtn);
        popup.setScene(new Scene(root, 540, 640));
        popup.showAndWait();
    }

    /* ════════════════════════════════════════
       ── BOOKING TAB ──
       ════════════════════════════════════════ */

    @FXML
    public void showBookingTab() {
        setActiveNav(btnBooking);
        buildBookingView();
    }

    @SuppressWarnings("unchecked")
    private void buildBookingView() {
        // Fresh local table every call — no parent conflict
        TableView<Bookingtrans> bookingTable = new TableView<>();
        styleTable(bookingTable);

        TableColumn<Bookingtrans, Integer> idCol     = col("ID",        "bookingId",         55);
        TableColumn<Bookingtrans, Integer> userCol   = col("User",      "userId",            65);
        TableColumn<Bookingtrans, Integer> transCol  = col("Transport", "transportId",       90);
        TableColumn<Bookingtrans, Integer> schedCol  = col("Schedule",  "scheduleId",        80);
        TableColumn<Bookingtrans, Integer> seatsCol  = col("Seats",     "totalSeats",        65);
        TableColumn<Bookingtrans, Double>  priceCol  = col("Total (€)", "totalPrice",        90);
        TableColumn<Bookingtrans, String>  statusCol = col("Status",    "bookingStatus",    110);
        TableColumn<Bookingtrans, String>  payCol    = col("Payment",   "paymentStatus",    100);
        TableColumn<Bookingtrans, Boolean> insCol    = col("Insurance", "insuranceIncluded", 90);

        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "CONFIRMED" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "CANCELLED" -> setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    default          -> setStyle("-fx-text-fill: #F06E32; -fx-font-weight: bold;");
                }
            }
        });

        bookingTable.getColumns().addAll(
                idCol, userCol, transCol, schedCol, seatsCol,
                priceCol, statusCol, payCol, insCol);

        bookingTable.setItems(
                FXCollections.observableArrayList(bookingService.getAllBookings()));

        Button confirmBtn = btn("✅  Confirm",        BTN_GREEN);
        Button cancelBtn2 = btn("❌  Cancel Booking", BTN_RED);
        Button detailBtn  = btn("🔍  View Details",   BTN_TEAL);
        Button refundBtn  = btn("↩  Mark Refunded",  BTN_DARK);

        confirmBtn.setOnAction(e -> {
            Bookingtrans sel = bookingTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a booking row first."); return; }
            if (confirm("Confirm booking #" + sel.getBookingId() + "?")) {
                sel.setBookingStatus("CONFIRMED");
                sel.setPaymentStatus("PAID");
                bookingService.updateBookingtrans(sel);
                bookingTable.setItems(
                        FXCollections.observableArrayList(bookingService.getAllBookings()));
                showSuccess("Booking confirmed.");
            }
        });
        cancelBtn2.setOnAction(e -> {
            Bookingtrans sel = bookingTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a booking row first."); return; }
            if (confirm("Cancel booking #" + sel.getBookingId() + "?")) {
                sel.setBookingStatus("CANCELLED");
                TextInputDialog reasonDlg = new TextInputDialog();
                reasonDlg.setTitle("Cancellation Reason");
                reasonDlg.setHeaderText("Enter cancellation reason (optional):");
                reasonDlg.showAndWait().ifPresent(sel::setCancellationReason);
                bookingService.updateBookingtrans(sel);
                bookingTable.setItems(
                        FXCollections.observableArrayList(bookingService.getAllBookings()));
                showSuccess("Booking cancelled.");
            }
        });
        refundBtn.setOnAction(e -> {
            Bookingtrans sel = bookingTable.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("Select a booking row first."); return; }
            if (confirm("Mark booking #" + sel.getBookingId() + " as refunded?")) {
                sel.setPaymentStatus("REFUNDED");
                bookingService.updateBookingtrans(sel);
                bookingTable.setItems(
                        FXCollections.observableArrayList(bookingService.getAllBookings()));
                showSuccess("Payment marked as REFUNDED.");
            }
        });
        detailBtn.setOnAction(e -> {
            Bookingtrans sel = bookingTable.getSelectionModel().getSelectedItem();
            if (sel != null) viewBookingDetails(sel);
            else showAlert("Select a booking row first.");
        });

        HBox toolbar = toolbar(confirmBtn, cancelBtn2, refundBtn, detailBtn);
        Label title  = sectionTitle("📋   Booking Management");
        VBox layout  = new VBox(14, title, toolbar, bookingTable);
        VBox.setVgrow(bookingTable, Priority.ALWAYS);
        contentArea.getChildren().setAll(layout);
    }

    private void viewBookingDetails(Bookingtrans sel) {
        Stage popup = popupStage("Booking Details – #" + sel.getBookingId());

        GridPane grid = formGrid();
        int r = 0;
        grid.addRow(r++, formLabel("Booking ID"),     new Label(String.valueOf(sel.getBookingId())));
        grid.addRow(r++, formLabel("User ID"),        new Label(String.valueOf(sel.getUserId())));
        grid.addRow(r++, formLabel("Transport ID"),   new Label(String.valueOf(sel.getTransportId())));
        grid.addRow(r++, formLabel("Schedule ID"),    new Label(String.valueOf(sel.getScheduleId())));
        grid.addRow(r++, formLabel("Booking Date"),   new Label(sel.getBookingDate() == null ? "N/A" : sel.getBookingDate().format(DT_FMT)));
        grid.addRow(r++, formLabel("Adults"),         new Label(String.valueOf(sel.getAdultsCount())));
        grid.addRow(r++, formLabel("Children"),       new Label(String.valueOf(sel.getChildrenCount())));
        grid.addRow(r++, formLabel("Total Seats"),    new Label(String.valueOf(sel.getTotalSeats())));
        grid.addRow(r++, formLabel("Total Price"),    new Label(String.format("€ %.2f", sel.getTotalPrice())));
        grid.addRow(r++, formLabel("Status"),         new Label(sel.getBookingStatus()));
        grid.addRow(r++, formLabel("Payment"),        new Label(sel.getPaymentStatus()));
        grid.addRow(r++, formLabel("Insurance"),      new Label(sel.isInsuranceIncluded() ? "Yes" : "No"));
        grid.addRow(r++, formLabel("AI Price Pred."), new Label(String.valueOf(sel.getAiPricePrediction())));
        grid.addRow(r++, formLabel("Comp. Score"),    new Label(String.valueOf(sel.getComparisonScore())));
        grid.addRow(r++, formLabel("Cancel Reason"),  new Label(sel.getCancellationReason() == null ? "—" : sel.getCancellationReason()));
        grid.addRow(r++, formLabel("QR Code"),        new Label(sel.getQrCode()      == null ? "—" : sel.getQrCode()));
        grid.addRow(r++, formLabel("Voucher"),        new Label(sel.getVoucherPath() == null ? "—" : sel.getVoucherPath()));

        Button closeBtn = btn("✖  Close", BTN_DARK);
        closeBtn.setOnAction(e -> popup.close());

        VBox root = popupRoot("Booking Details", grid, closeBtn);
        popup.setScene(new Scene(root, 480, 600));
        popup.showAndWait();
    }

    /* ════════════════════════════════════════
       ── SHARED UI HELPERS ──
       ════════════════════════════════════════ */

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

    private void placeholders(TextField tf, String hint) {
        tf.setPromptText(hint);
        tf.setMaxWidth(Double.MAX_VALUE);
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