package tn.esprit.controllers.admin;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class OverviewController {

    @FXML private VBox contentBox;

    // Style constants (packs & offers palette)
    private static final String STAT_LBL =
            "-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-family: 'Poppins','Segoe UI';";
    private static final String SEC_TITLE =
            "-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #0A4174; " +
            "-fx-font-family: 'Poppins','Segoe UI';";
    private static final String SECTION_CARD =
            "-fx-background-color: white; -fx-background-radius: 18; " +
            "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.10), 18, 0, 0, 5); -fx-padding: 26;";
    private static final String GRADIENT_LINE =
            "-fx-background-color: linear-gradient(to right, #6D83F2, #4CCCAD); " +
            "-fx-pref-height: 3; -fx-background-radius: 2; -fx-min-height: 3; -fx-max-height: 3;";

    @FXML
    public void initialize() {
        if (contentBox == null) return;
        contentBox.getChildren().clear();
        contentBox.getChildren().addAll(
                buildPageHeader(),
                buildUsersSection(),
                buildAccommodationsSection(),
                buildDestinationsSection(),
                buildTransportSection(),
                buildPacksSection()
        );
    }

    // ═══════════════════════════════════════════════
    // PAGE HEADER
    // ═══════════════════════════════════════════════
    private VBox buildPageHeader() {
        Label title = new Label("Dashboard Overview");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0A4174; " +
                       "-fx-font-family: 'Poppins','Segoe UI';");
        Label sub = new Label("Real-time statistics and AI insights across all TripX modules");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8; -fx-font-family: 'Poppins','Segoe UI';");
        VBox box = new VBox(5, title, sub);
        box.setPadding(new Insets(0, 0, 10, 0));
        return box;
    }

    // ═══════════════════════════════════════════════
    // USERS SECTION
    // ═══════════════════════════════════════════════
    private VBox buildUsersSection() {
        List<User> users;
        try {
            users = new UserService().getAllUsers();
        } catch (Exception e) {
            users = List.of();
        }

        long total = users.size();
        long admins = users.stream()
                .filter(u -> u.getRole() != null && u.getRole().toLowerCase().contains("admin"))
                .count();
        long regular = total - admins;

        HBox row = new HBox(16,
                statCard("👥", String.valueOf(total), "Total Users", "#6D83F2", "#EEF2FF"),
                statCard("🛡️", String.valueOf(admins), "Admins", "#0D9488", "#E0F7F4"),
                statCard("🙋", String.valueOf(regular), "Regular Users", "#F59E0B", "#FEF3C7")
        );
        row.setPadding(new Insets(14, 0, 0, 0));
        return buildSection("👤   Users", row, false);
    }

    // ═══════════════════════════════════════════════
    // ACCOMMODATIONS (MOST ADVANCED + AI)
    // ═══════════════════════════════════════════════
    private VBox buildAccommodationsSection() {
        AccommodationService accomSvc = new AccommodationService();
        RoomService roomSvc = new RoomService();
        AccommodationBookingService bkSvc = new AccommodationBookingService();

        List<Accommodation> accommodations = accomSvc.getAllAccommodations();
        List<Room> rooms;
        try { rooms = roomSvc.getAll(); } catch (Exception e) { rooms = List.of(); }

        List<AccommodationBooking> allBk = bkSvc.getAllAccommodationBookings();
        long confirmed = allBk.stream().filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus())).count();
        long pending   = allBk.stream().filter(b -> "PENDING".equalsIgnoreCase(b.getStatus())).count();
        long cancelled = allBk.stream().filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus())).count();
        double revenue = allBk.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .mapToDouble(AccommodationBooking::getTotalPrice).sum();

        int totalAccom = accommodations.size();
        int totalRooms = rooms.size();

        // Top stats row
        HBox statsRow = new HBox(14,
                statCard("🏨", String.valueOf(totalAccom), "Properties", "#6D83F2", "#EEF2FF"),
                statCard("🛏️", String.valueOf(totalRooms), "Total Rooms", "#0D9488", "#E0F7F4"),
                statCard("✅", String.valueOf(confirmed), "Confirmed Bookings", "#10B981", "#D1FAE5"),
                statCard("⏳", String.valueOf(pending), "Pending Approval", "#F59E0B", "#FEF3C7"),
                statCard("❌", String.valueOf(cancelled), "Cancelled", "#EF4444", "#FEE2E2"),
                statCard("💰", String.format("%.0f TND", revenue), "Revenue", "#8B5CF6", "#EDE9FE")
        );
        statsRow.setPadding(new Insets(14, 0, 12, 0));

        // Occupancy progress bar
        double occupancy = totalRooms > 0 ? Math.min(100, (confirmed * 100.0 / totalRooms)) : 0;
        VBox occupancyBar = buildOccupancyBar(occupancy);

        // AI panel
        VBox aiPanel = buildAiPanel(totalAccom, totalRooms, (int) confirmed, (int) pending, revenue);

        VBox inner = new VBox(0, statsRow, occupancyBar, aiPanel);

        return buildSection("🏨   Accommodations", inner, true);
    }

    private VBox buildOccupancyBar(double pct) {
        Label lbl = new Label(String.format("Booking Occupancy Rate:  %.1f%%", pct));
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0A4174;");

        // Background track
        StackPane track = new StackPane();
        track.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 8; -fx-pref-height: 14;");
        track.setMinHeight(14);
        track.setMaxHeight(14);

        // Fill
        Region fill = new Region();
        fill.setStyle("-fx-background-color: linear-gradient(to right, #6D83F2, #4CCCAD); " +
                      "-fx-background-radius: 8;");
        fill.setMinHeight(14);
        fill.setMaxHeight(14);
        fill.prefWidthProperty().bind(track.widthProperty().multiply(pct / 100.0));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        HBox row = new HBox(14, lbl, track);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(track, Priority.ALWAYS);
        row.setPadding(new Insets(0, 0, 14, 0));
        return new VBox(0, row);
    }

    private VBox buildAiPanel(int properties, int rooms, int confirmed, int pending, double revenue) {
        VBox panel = new VBox(14);
        panel.setStyle("-fx-background-color: linear-gradient(to bottom right, #F0F4FF, #E8FAF8); " +
                "-fx-background-radius: 14; -fx-padding: 22; " +
                "-fx-border-color: rgba(109,131,242,0.22); -fx-border-radius: 14; -fx-border-width: 1;");

        Label heading = new Label("🤖   AI Accommodation Insights   —   Powered by Groq");
        heading.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0A4174; " +
                         "-fx-font-family: 'Poppins','Segoe UI';");

        Label hint = new Label(
                "Click 'Analyze' to get AI-powered analysis of your accommodation performance and personalised recommendations.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-family: 'Poppins','Segoe UI';");
        hint.setWrapText(true);

        TextArea resultArea = new TextArea("Click 'Analyze' to receive insights...");
        resultArea.setWrapText(true);
        resultArea.setEditable(false);
        resultArea.setPrefHeight(140);
        resultArea.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: rgba(109,131,242,0.18); -fx-border-radius: 10; " +
                "-fx-font-size: 13px; -fx-text-fill: #374151; -fx-font-family: 'Poppins','Segoe UI';");

        Button analyzeBtn = new Button("✨   Analyze with AI");
        analyzeBtn.setStyle("-fx-background-color: linear-gradient(to right, #6D83F2, #4CCCAD); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                "-fx-background-radius: 22; -fx-padding: 10 26; -fx-cursor: hand; " +
                "-fx-font-family: 'Poppins','Segoe UI';");

        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        analyzeBtn.setOnAction(e -> {
            analyzeBtn.setDisable(true);
            analyzeBtn.setText("⏳   Analyzing...");
            statusLbl.setText("Connecting to Groq AI...");
            resultArea.setText("Generating insights, please wait...");

            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return callGroqApi(properties, rooms, confirmed, pending, revenue);
                }
            };
            task.setOnSucceeded(ev -> {
                resultArea.setText(task.getValue());
                analyzeBtn.setDisable(false);
                analyzeBtn.setText("✨   Analyze with AI");
                statusLbl.setText("Analysis complete. Updated just now.");
            });
            task.setOnFailed(ev -> {
                Throwable ex = task.getException();
                resultArea.setText("Could not reach Groq API.\n" +
                        "• Make sure GROQ_API_KEY is set in your .env file.\n" +
                        "• Error: " + (ex != null ? ex.getMessage() : "unknown"));
                analyzeBtn.setDisable(false);
                analyzeBtn.setText("↺   Retry");
                statusLbl.setText("Analysis failed.");
            });
            new Thread(task, "groq-overview-thread").start();
        });

        HBox btnRow = new HBox(14, analyzeBtn, statusLbl);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().addAll(heading, hint, resultArea, btnRow);
        return panel;
    }

    private String callGroqApi(int properties, int rooms, int confirmed, int pending, double revenue)
            throws Exception {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            try { apiKey = Dotenv.load().get("GROQ_API_KEY"); } catch (Exception ignored) {}
        }
        if (apiKey == null || apiKey.isBlank()) {
            return "GROQ_API_KEY not configured.\nAdd it to your .env file: GROQ_API_KEY=your_key_here";
        }

        double occupancy = rooms > 0 ? Math.min(100, (confirmed * 100.0 / rooms)) : 0;
        String prompt = String.format(
                "You are a hospitality business analyst for TripX, a travel platform. " +
                "Analyze the following accommodation KPIs and provide 4-5 concise, actionable insights " +
                "and specific recommendations to improve performance:\n\n" +
                "• Total properties listed: %d\n" +
                "• Total rooms across all properties: %d\n" +
                "• Confirmed bookings: %d\n" +
                "• Pending approval bookings: %d\n" +
                "• Revenue from confirmed bookings: %.2f TND\n" +
                "• Booking-to-room ratio (occupancy proxy): %.1f%%\n\n" +
                "Be concise, use bullet points, and focus on revenue and occupancy improvement strategies.",
                properties, rooms, confirmed, pending, revenue, occupancy);

        String requestBody = new Gson().toJson(Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 450
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return "Groq API returned error " + response.statusCode() + ".\n" + response.body();
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.getAsJsonArray("choices")
                   .get(0).getAsJsonObject()
                   .getAsJsonObject("message")
                   .get("content").getAsString().trim();
    }

    // ═══════════════════════════════════════════════
    // DESTINATIONS & ACTIVITIES SECTION
    // ═══════════════════════════════════════════════
    private VBox buildDestinationsSection() {
        DestinationService destSvc = new DestinationService();
        ActivityService actSvc = new ActivityService();
        BookingService bkSvc = new BookingService();

        List<Destination> destinations = destSvc.getAllDestinations();
        List<Activity> activities = actSvc.getAllActivities();
        List<Booking> bookings = bkSvc.getAllBookings();

        long confirmed = bookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED).count();
        long countries = destinations.stream()
                .map(Destination::getCountry)
                .filter(c -> c != null && !c.isBlank())
                .distinct().count();

        HBox row = new HBox(16,
                statCard("🌍", String.valueOf(destinations.size()), "Destinations", "#6D83F2", "#EEF2FF"),
                statCard("🏳", String.valueOf(countries), "Countries Covered", "#0D9488", "#E0F7F4"),
                statCard("🎭", String.valueOf(activities.size()), "Activities", "#F59E0B", "#FEF3C7"),
                statCard("✅", String.valueOf(confirmed), "Confirmed Bookings", "#10B981", "#D1FAE5")
        );
        row.setPadding(new Insets(14, 0, 0, 0));
        return buildSection("🌍   Destinations & Activities", row, false);
    }

    // ═══════════════════════════════════════════════
    // TRANSPORT SECTION
    // ═══════════════════════════════════════════════
    private VBox buildTransportSection() {
        TransportService tSvc = new TransportService();
        BookingtransService btSvc = new BookingtransService();

        List<Transport> transports = tSvc.getAllTransports();
        List<Bookingtrans> bookings = btSvc.getAllBookings();

        long confirmed  = bookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getBookingStatus())).count();
        long pending    = bookings.stream()
                .filter(b -> "PENDING".equalsIgnoreCase(b.getBookingStatus())).count();
        long cancelled  = bookings.stream()
                .filter(b -> "CANCELLED".equalsIgnoreCase(b.getBookingStatus())).count();
        double revenue  = bookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getBookingStatus()))
                .mapToDouble(Bookingtrans::getTotalPrice).sum();

        HBox row = new HBox(16,
                statCard("🚗", String.valueOf(transports.size()), "Vehicles", "#6D83F2", "#EEF2FF"),
                statCard("📋", String.valueOf(bookings.size()), "Total Bookings", "#F59E0B", "#FEF3C7"),
                statCard("✅", String.valueOf(confirmed), "Confirmed", "#10B981", "#D1FAE5"),
                statCard("⏳", String.valueOf(pending), "Pending", "#F97316", "#FEF0E7"),
                statCard("❌", String.valueOf(cancelled), "Cancelled", "#EF4444", "#FEE2E2"),
                statCard("💰", String.format("%.0f TND", revenue), "Revenue", "#8B5CF6", "#EDE9FE")
        );
        row.setPadding(new Insets(14, 0, 12, 0));

        VBox aiPanel = buildTransportAiPanel(
                transports.size(), (int) bookings.size(),
                (int) confirmed, (int) pending, (int) cancelled, revenue);

        VBox inner = new VBox(0, row, aiPanel);
        return buildSection("🚗   Transport", inner, true);
    }

    private VBox buildTransportAiPanel(int vehicles, int total, int confirmed,
                                       int pending, int cancelled, double revenue) {
        VBox panel = new VBox(14);
        panel.setStyle("-fx-background-color: linear-gradient(to bottom right, #F0F4FF, #FFF7ED); " +
                "-fx-background-radius: 14; -fx-padding: 22; " +
                "-fx-border-color: rgba(109,131,242,0.22); -fx-border-radius: 14; -fx-border-width: 1;");

        Label heading = new Label("🤖   AI Transport Insights   —   Powered by Groq");
        heading.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0A4174; " +
                         "-fx-font-family: 'Poppins','Segoe UI';");

        Label hint = new Label(
                "Click 'Analyze' to get AI-powered analysis of your transport fleet performance and booking trends.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-family: 'Poppins','Segoe UI';");
        hint.setWrapText(true);

        TextArea resultArea = new TextArea("Click 'Analyze' to receive insights...");
        resultArea.setWrapText(true);
        resultArea.setEditable(false);
        resultArea.setPrefHeight(140);
        resultArea.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: rgba(109,131,242,0.18); -fx-border-radius: 10; " +
                "-fx-font-size: 13px; -fx-text-fill: #374151; -fx-font-family: 'Poppins','Segoe UI';");

        Button analyzeBtn = new Button("✨   Analyze with AI");
        analyzeBtn.setStyle("-fx-background-color: linear-gradient(to right, #F97316, #6D83F2); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                "-fx-background-radius: 22; -fx-padding: 10 26; -fx-cursor: hand; " +
                "-fx-font-family: 'Poppins','Segoe UI';");

        Label statusLbl = new Label("");
        statusLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        analyzeBtn.setOnAction(e -> {
            analyzeBtn.setDisable(true);
            analyzeBtn.setText("⏳   Analyzing...");
            statusLbl.setText("Connecting to Groq AI...");
            resultArea.setText("Generating insights, please wait...");

            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return callGroqTransportApi(vehicles, total, confirmed, pending, cancelled, revenue);
                }
            };
            task.setOnSucceeded(ev -> {
                resultArea.setText(task.getValue());
                analyzeBtn.setDisable(false);
                analyzeBtn.setText("✨   Analyze with AI");
                statusLbl.setText("Analysis complete. Updated just now.");
            });
            task.setOnFailed(ev -> {
                Throwable ex = task.getException();
                resultArea.setText("Could not reach Groq API.\n" +
                        "• Make sure GROQ_API_KEY is set in your .env file.\n" +
                        "• Error: " + (ex != null ? ex.getMessage() : "unknown"));
                analyzeBtn.setDisable(false);
                analyzeBtn.setText("↺   Retry");
                statusLbl.setText("Analysis failed.");
            });
            new Thread(task, "groq-transport-thread").start();
        });

        HBox btnRow = new HBox(14, analyzeBtn, statusLbl);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().addAll(heading, hint, resultArea, btnRow);
        return panel;
    }

    private String callGroqTransportApi(int vehicles, int total, int confirmed,
                                        int pending, int cancelled, double revenue) throws Exception {
        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            try { apiKey = Dotenv.load().get("GROQ_API_KEY"); } catch (Exception ignored) {}
        }
        if (apiKey == null || apiKey.isBlank()) {
            return "GROQ_API_KEY not configured.\nAdd it to your .env file: GROQ_API_KEY=your_key_here";
        }

        double confirmationRate = total > 0 ? (confirmed * 100.0 / total) : 0;
        double cancellationRate = total > 0 ? (cancelled * 100.0 / total) : 0;
        double avgRevenuePerBooking = confirmed > 0 ? (revenue / confirmed) : 0;

        String prompt = String.format(
                "You are a transport logistics analyst for TripX, a travel platform. " +
                "Analyze the following transport KPIs and provide 4-5 concise, actionable insights " +
                "and specific recommendations to improve fleet utilization and revenue:\n\n" +
                "• Total vehicles in fleet: %d\n" +
                "• Total bookings received: %d\n" +
                "• Confirmed bookings: %d (%.1f%% confirmation rate)\n" +
                "• Pending bookings: %d\n" +
                "• Cancelled bookings: %d (%.1f%% cancellation rate)\n" +
                "• Total revenue from confirmed bookings: %.2f TND\n" +
                "• Average revenue per confirmed booking: %.2f TND\n\n" +
                "Be concise, use bullet points, and focus on reducing cancellations, improving " +
                "confirmation rate, and maximising fleet revenue.",
                vehicles, total, confirmed, confirmationRate,
                pending, cancelled, cancellationRate, revenue, avgRevenuePerBooking);

        String requestBody = new Gson().toJson(Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 450
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return "Groq API returned error " + response.statusCode() + ".\n" + response.body();
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.getAsJsonArray("choices")
                   .get(0).getAsJsonObject()
                   .getAsJsonObject("message")
                   .get("content").getAsString().trim();
    }

    // ═══════════════════════════════════════════════
    // PACKS & OFFERS SECTION
    // ═══════════════════════════════════════════════
    private VBox buildPacksSection() {
        long activePacks = 0, activeOffers = 0, confirmedBk = 0, totalBk = 0;
        double revenue = 0;
        try {
            PackService pSvc = new PackService();
            OfferService oSvc = new OfferService();
            PackBookingService pbSvc = new PackBookingService();

            activePacks  = pSvc.getActivePacks().size();
            activeOffers = oSvc.getActiveOffers().size();
            List<PacksBooking> bookings = pbSvc.afficherList();
            totalBk      = bookings.size();
            confirmedBk  = bookings.stream()
                    .filter(b -> b.getStatus() == PacksBooking.Status.CONFIRMED).count();
            revenue = bookings.stream()
                    .filter(b -> b.getStatus() == PacksBooking.Status.CONFIRMED)
                    .mapToDouble(b -> b.getFinalPrice() != null ? b.getFinalPrice().doubleValue() : 0)
                    .sum();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        HBox row = new HBox(16,
                statCard("🎒", String.valueOf(activePacks), "Active Packs", "#6D83F2", "#EEF2FF"),
                statCard("🎉", String.valueOf(activeOffers), "Active Offers", "#F59E0B", "#FEF3C7"),
                statCard("📦", String.valueOf(totalBk), "Total Bookings", "#0D9488", "#E0F7F4"),
                statCard("✅", String.valueOf(confirmedBk), "Confirmed", "#10B981", "#D1FAE5"),
                statCard("💰", String.format("%.0f TND", revenue), "Revenue", "#8B5CF6", "#EDE9FE")
        );
        row.setPadding(new Insets(14, 0, 0, 0));
        return buildSection("📦   Packs & Offers", row, false);
    }

    // ═══════════════════════════════════════════════
    // SHARED HELPERS
    // ═══════════════════════════════════════════════

    /**
     * Wraps content in a styled white section card with a gradient underline.
     * @param isAccommodation pass true to add a teal left accent bar
     */
    private VBox buildSection(String title, javafx.scene.Node content, boolean isAccommodation) {
        Label lbl = new Label(title);
        lbl.setStyle(SEC_TITLE + (isAccommodation ? " -fx-font-size: 20px;" : ""));

        Region gradLine = new Region();
        gradLine.setStyle(isAccommodation
                ? "-fx-background-color: linear-gradient(to right, #0D9488, #6D83F2, #4CCCAD); " +
                  "-fx-pref-height: 3; -fx-background-radius: 2; -fx-min-height: 3; -fx-max-height: 3;"
                : GRADIENT_LINE);
        VBox.setMargin(gradLine, new Insets(5, 0, 0, 0));

        VBox section = new VBox(0, lbl, gradLine, content);
        section.setStyle(isAccommodation
                ? "-fx-background-color: white; -fx-background-radius: 18; " +
                  "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.14), 22, 0, 0, 6); -fx-padding: 26; " +
                  "-fx-border-color: rgba(13,148,136,0.12); -fx-border-radius: 18; -fx-border-width: 1;"
                : SECTION_CARD);
        return section;
    }

    /**
     * Builds a single KPI stat card with icon, value, and label.
     */
    private VBox statCard(String icon, String value, String label, String accentColor, String bgColor) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 24px;");

        StackPane iconCircle = new StackPane(iconLbl);
        iconCircle.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 14; " +
                "-fx-min-width: 50; -fx-min-height: 50; -fx-max-width: 50; -fx-max-height: 50;");
        iconCircle.setAlignment(Pos.CENTER);

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + "; " +
                        "-fx-font-family: 'Poppins','Segoe UI';");

        Label nameLbl = new Label(label);
        nameLbl.setStyle(STAT_LBL);
        nameLbl.setWrapText(true);

        VBox card = new VBox(8, iconCircle, valLbl, nameLbl);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.09), 10, 0, 0, 3); " +
                "-fx-padding: 18; -fx-cursor: default;");
        card.setPrefWidth(155);
        card.setMinWidth(140);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }
}
