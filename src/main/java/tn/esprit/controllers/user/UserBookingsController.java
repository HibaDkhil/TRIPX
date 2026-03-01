package tn.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.entities.*;
import tn.esprit.services.*;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class UserBookingsController {

    // Top bar
    @FXML private Label avatarInitials;
    @FXML private Label userNameLabel;

    // Count / refresh
    @FXML private Label lblTotalCount;

    // Section grids
    @FXML private FlowPane gridAccommodations;
    @FXML private FlowPane gridDestinations;
    @FXML private FlowPane gridTransport;
    @FXML private FlowPane gridPacks;

    // Empty labels
    @FXML private Label lblNoAccommodations;
    @FXML private Label lblNoDestinations;
    @FXML private Label lblNoTransport;
    @FXML private Label lblNoPacks;

    // Services
    private final AccommodationBookingService accomService = new AccommodationBookingService();
    private final BookingService destService = new BookingService();
    private final BookingtransService transService = new BookingtransService();
    private final PackBookingService packService = new PackBookingService();
    private final RoomService roomService = new RoomService();
    private final LookupService lookupService = new LookupService();

    private User currentUser;

    // ── Style constants (packs & offers palette) ───────────────────────────────
    private static final String CARD_STYLE =
            "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; " +
            "-fx-border-radius: 16; -fx-min-width: 300; -fx-max-width: 300; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 3);";

    private static final String HEADER_ACCOM =
            "-fx-background-color: linear-gradient(to right, #0A4174, #2F9D94); " +
            "-fx-background-radius: 10 10 0 0; -fx-padding: 14 16;";
    private static final String HEADER_DEST =
            "-fx-background-color: linear-gradient(to right, #1565C0, #42A5F5); " +
            "-fx-background-radius: 10 10 0 0; -fx-padding: 14 16;";
    private static final String HEADER_TRANS =
            "-fx-background-color: linear-gradient(to right, #6D83F2, #4CCCAD); " +
            "-fx-background-radius: 10 10 0 0; -fx-padding: 14 16;";
    private static final String HEADER_PACKS =
            "-fx-background-color: linear-gradient(to right, #E65100, #FF8F00); " +
            "-fx-background-radius: 10 10 0 0; -fx-padding: 14 16;";

    private static final String BADGE_CONFIRMED =
            "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; " +
            "-fx-font-size: 11px; -fx-background-radius: 20; -fx-padding: 4 12;";
    private static final String ROW_LABEL =
            "-fx-font-size: 12px; -fx-text-fill: #64748B;";
    private static final String ROW_VALUE =
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #063154;";
    private static final String PRICE_STYLE =
            "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0A4174;";

    // ───────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadAllBookings();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && user.getUserId() > 0) {
            SessionManager.setCurrentUserId(user.getUserId());
        }
        if (user != null) {
            if (userNameLabel != null) {
                userNameLabel.setText(safe(user.getFirstName()) + " " + safe(user.getLastName()));
            }
            if (avatarInitials != null) {
                String f = user.getFirstName() != null && !user.getFirstName().isBlank()
                        ? user.getFirstName().substring(0, 1).toUpperCase() : "";
                String l = user.getLastName() != null && !user.getLastName().isBlank()
                        ? user.getLastName().substring(0, 1).toUpperCase() : "";
                avatarInitials.setText((f + l).isBlank() ? "U" : (f + l));
            }
        }
        loadAllBookings();
    }

    @FXML
    private void handleRefresh() {
        loadAllBookings();
    }

    // ── Load & render all sections ─────────────────────────────────────────────

    private void loadAllBookings() {
        int uid = SessionManager.getCurrentUserId();
        int total = 0;

        // ── 1. Accommodations (shown first) ──
        gridAccommodations.getChildren().clear();
        List<AccommodationBooking> accomList = accomService.getAccommodationBookingsByUserId(uid)
                .stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .toList();
        for (AccommodationBooking b : accomList) {
            gridAccommodations.getChildren().add(buildAccomCard(b));
        }
        boolean noAccom = accomList.isEmpty();
        lblNoAccommodations.setVisible(noAccom);
        lblNoAccommodations.setManaged(noAccom);
        total += accomList.size();

        // ── 2. Destinations ──
        gridDestinations.getChildren().clear();
        List<Booking> destList = destService.getBookingsByUser(uid)
                .stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .toList();
        for (Booking b : destList) {
            gridDestinations.getChildren().add(buildDestCard(b));
        }
        boolean noDest = destList.isEmpty();
        lblNoDestinations.setVisible(noDest);
        lblNoDestinations.setManaged(noDest);
        total += destList.size();

        // ── 3. Transport ──
        gridTransport.getChildren().clear();
        List<Bookingtrans> transList = transService.getBookingsByUserId(uid)
                .stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getBookingStatus()))
                .toList();
        for (Bookingtrans b : transList) {
            gridTransport.getChildren().add(buildTransCard(b));
        }
        boolean noTrans = transList.isEmpty();
        lblNoTransport.setVisible(noTrans);
        lblNoTransport.setManaged(noTrans);
        total += transList.size();

        // ── 4. Packs ──
        gridPacks.getChildren().clear();
        try {
            List<PacksBooking> packList = packService.getByUserId(uid)
                    .stream()
                    .filter(b -> b.getStatus() == PacksBooking.Status.CONFIRMED)
                    .toList();
            for (PacksBooking b : packList) {
                gridPacks.getChildren().add(buildPackCard(b));
            }
            boolean noPacks = packList.isEmpty();
            lblNoPacks.setVisible(noPacks);
            lblNoPacks.setManaged(noPacks);
            total += packList.size();
        } catch (SQLException e) {
            lblNoPacks.setVisible(true);
            lblNoPacks.setManaged(true);
        }

        lblTotalCount.setText(total + " confirmed booking" + (total != 1 ? "s" : ""));
    }

    // ── Card builders ──────────────────────────────────────────────────────────

    private VBox buildAccomCard(AccommodationBooking b) {
        String accomName = "Accommodation";
        String roomType  = "Room";
        try {
            Room room = roomService.getRoomById(b.getRoomId());
            if (room != null) {
                roomType = room.getRoomType();
                Accommodation acc = lookupService.getAccommodationById(room.getAccommodationId());
                if (acc != null) accomName = acc.getName();
            }
        } catch (Exception ignored) {}

        VBox header = headerBox(HEADER_ACCOM, "🏨", accomName, roomType + " Room");
        VBox body = bodyBox(
            row("Check-in",  fmt(b.getCheckIn())),
            row("Check-out", fmt(b.getCheckOut())),
            row("Guests",    b.getNumberOfGuests() > 0 ? b.getNumberOfGuests() + " guest(s)" : "—"),
            row("Total",     String.format("%.2f TND", b.getTotalPrice()))
        );
        Label price = new Label(String.format("%.2f TND", b.getTotalPrice()));
        price.setStyle(PRICE_STYLE);

        return card(header, body, price);
    }

    private VBox buildDestCard(Booking b) {
        VBox header = headerBox(HEADER_DEST, "✈️", b.getDestinationName(), "Destination Booking");
        VBox body = bodyBox(
            row("Ref",    b.getBookingReference()),
            row("Date",   b.getStartAt() != null ? b.getStartAt().toString().substring(0, 10) : "—"),
            row("Total",  String.format("$%.2f %s", b.getTotalAmount(), b.getCurrency()))
        );
        Label price = new Label(String.format("$%.2f", b.getTotalAmount()));
        price.setStyle(PRICE_STYLE);

        return card(header, body, price);
    }

    private VBox buildTransCard(Bookingtrans b) {
        String transportName = "Transport #" + b.getTransportId();
        try {
            Transport t = lookupService.getTransportById(b.getTransportId());
            if (t != null) transportName = t.getProviderName() + " (" + t.getTransportType() + ")";
        } catch (Exception ignored) {}

        VBox header = headerBox(HEADER_TRANS, "🚀", transportName, "Transport Booking");
        VBox body = bodyBox(
            row("Seats",   b.getTotalSeats() + " seat(s)"),
            row("Date",    b.getBookingDate() != null ? b.getBookingDate().toString().substring(0, 10) : "—"),
            row("Payment", b.getPaymentStatus()),
            row("Total",   String.format("%.2f TND", b.getTotalPrice()))
        );
        Label price = new Label(String.format("%.2f TND", b.getTotalPrice()));
        price.setStyle(PRICE_STYLE);

        return card(header, body, price);
    }

    private VBox buildPackCard(PacksBooking b) {
        String packName = "Pack #" + b.getPackId();
        try {
            Pack p = new PackService().getById(b.getPackId());
            if (p != null) packName = p.getTitle();
        } catch (Exception ignored) {}

        VBox header = headerBox(HEADER_PACKS, "🎒", packName, "Pack Booking");
        VBox body = bodyBox(
            row("Travel",    fmt(b.getTravelStartDate()) + " → " + fmt(b.getTravelEndDate())),
            row("Travelers", b.getNumTravelers() + " person(s)"),
            row("Discount",  b.getDiscountApplied() != null ? b.getDiscountApplied() + "%" : "0%"),
            row("Final",     String.format("%.2f TND", b.getFinalPrice().doubleValue()))
        );
        Label price = new Label(String.format("%.2f TND", b.getFinalPrice().doubleValue()));
        price.setStyle(PRICE_STYLE);

        return card(header, body, price);
    }

    // ── UI helpers ─────────────────────────────────────────────────────────────

    private VBox headerBox(String headerStyle, String icon, String title, String subtitle) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        titleLbl.setWrapText(true);

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.80);");

        VBox texts = new VBox(2, titleLbl, subLbl);
        HBox hb = new HBox(10, iconLbl, texts);
        hb.setAlignment(Pos.CENTER_LEFT);
        hb.setStyle(headerStyle);

        return new VBox(hb);
    }

    private VBox bodyBox(HBox... rows) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #F5F7FA; -fx-background-radius: 8; -fx-padding: 12;");
        box.getChildren().addAll(rows);
        return box;
    }

    private HBox row(String label, String value) {
        Label k = new Label(label + ":");
        k.setStyle(ROW_LABEL);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle(ROW_VALUE);
        HBox hb = new HBox(k, sp, v);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }

    private VBox card(VBox header, VBox body, Label price) {
        Label badge = new Label("✅  CONFIRMED");
        badge.setStyle(BADGE_CONFIRMED);

        HBox footer = new HBox(badge, new Region(), price);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(0, header.getChildren().get(0), body, footer);
        card.setSpacing(12);
        card.setStyle(CARD_STYLE);
        return card;
    }

    // ── Navigation handlers ────────────────────────────────────────────────────

    @FXML private void handleHomeNav(MouseEvent e)          { navigateTo("/fxml/user/home.fxml"); }
    @FXML private void handleDestinationsNav(MouseEvent e)  { navigateTo("/fxml/user/user_destinations.fxml"); }
    @FXML private void handleAccommodationsNav(MouseEvent e){ navigateTo("/fxml/user/AccommodationsView.fxml"); }
    @FXML private void handleActivitiesNav(MouseEvent e)    { navigateTo("/fxml/user/user_activities.fxml"); }
    @FXML private void handleTransportNav(MouseEvent e)     { navigateTo("/fxml/user/TransportUserInterface.fxml"); }
    @FXML private void handlePacksOffersNav(MouseEvent e)   { navigateTo("/fxml/user/UserPacksOffersView.fxml"); }
    @FXML private void handleBlogNav(MouseEvent e)          { showAlert("Blog page coming soon!"); }
    @FXML private void handleProfile(MouseEvent e)          { navigateTo("/fxml/user/profile.fxml"); }

    @FXML
    private void handleLogout(javafx.event.ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                SessionManager.setCurrentUserId(-1);
                navigateTo("/fxml/user/login.fxml");
            }
        });
    }

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            if (currentUser != null) {
                if (ctrl instanceof HomeController c)                    c.setUser(currentUser);
                else if (ctrl instanceof UserDestinationsController c)   c.setCurrentUser(currentUser);
                else if (ctrl instanceof UserActivitiesController c)     c.setCurrentUser(currentUser);
                else if (ctrl instanceof AccommodationsController c)     c.setCurrentUser(currentUser);
                else if (ctrl instanceof TransportUserInterfaceController c) c.setCurrentUser(currentUser);
                else if (ctrl instanceof UserPacksOffersController c)    c.setCurrentUser(currentUser);
                else if (ctrl instanceof ProfileController c)            c.setUser(currentUser);
            }
            Stage stage = (Stage) lblTotalCount.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert("Navigation error: " + ex.getMessage());
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private String fmt(java.sql.Date d) {
        return d != null ? d.toString() : "—";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).show();
    }
}
