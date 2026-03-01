package tn.esprit.controllers.user;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Activity;
import tn.esprit.entities.Destination;
import tn.esprit.entities.User;
import tn.esprit.entities.UserActivity;
import tn.esprit.entities.UserPreferences;
import tn.esprit.entities.Accommodation;
import tn.esprit.services.*;
import tn.esprit.utils.SessionManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class HomeController {

    private User currentUser;
    @FXML private HBox recommendationsContainer;
    @FXML private VBox recommendationsSection;

    // Services
    private UserPreferencesService preferencesService;
    private WeatherPreferenceMatcher weatherMatcher;
    private DestinationService destinationService;
    private ActivityService activityService;
    private UserActivityService activityLogService;
    private AccommodationService accommodationService;

    // Search / Combo fields
    @FXML private ComboBox<String> destinationCombo;
    @FXML private ComboBox<String> activityCombo;
    @FXML private TextField accommodationSearchField;
    @FXML private DatePicker datePicker;

    // Cards containers
    @FXML private HBox destinationsContainer;
    @FXML private HBox activitiesContainer;

    // AI Chat
    @FXML private VBox chatPanel;
    @FXML private VBox chatMessageContainer;
    @FXML private TextField chatInputField;
    @FXML private ScrollPane chatScrollPane;
    @FXML private Button aiAssistantBtn;
    @FXML private Label userNameLabel;
    @FXML private ImageView userAvatarView;

    private GeminiService geminiService;
    //weather
    @FXML private VBox weatherResultsContainer;
    @FXML private Button matchWeatherBtn;

    // AVATAR
    @FXML private Label avatarInitials;

    // ========== NEW TRIP PLANNER FIELDS ==========
    @FXML private ComboBox<String> plannerDestinationCombo;
    @FXML private TextField plannerDaysField;
    @FXML private TextField plannerStyleField;
    @FXML private Label plannerDestinationError;
    @FXML private Label plannerDaysError;
    @FXML private Button generatePlanBtn;
    @FXML private Button bookPlanBtn;
    @FXML private VBox planResultsContainer;
    @FXML private VBox planDetailsContainer;
    @FXML private ProgressIndicator plannerProgress;

    // PDF Generation
    @FXML private Button downloadPdfBtn;
    @FXML private Label planDestinationIcon;
    @FXML private Label planDestinationName;
    @FXML private Label planDestinationDesc;
    private String currentGeneratedPlan;
    private Destination currentPlanDestination;
// ============================================

    // ========== FUTURE MODULE CONTAINERS (COMMENTED) ==========
    @FXML private HBox accommodationsContainer;
/*
@FXML private HBox offersContainer;
@FXML private HBox transportContainer;
@FXML private HBox blogContainer;
*/
// ============================================

    // ─── Initialisation ───────────────────────────────────────────────────────

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null && user.getUserId() > 0) {
            SessionManager.setCurrentUserId(user.getUserId());
        }
        this.preferencesService   = new UserPreferencesService();
        this.weatherMatcher       = new WeatherPreferenceMatcher();
        this.geminiService        = new GeminiService();
        this.destinationService   = new DestinationService();
        this.activityService      = new ActivityService();
        this.activityLogService   = new UserActivityService();
        this.accommodationService = new AccommodationService();

        // Track page visit
        tn.esprit.utils.ActivityLogger.logVisit(currentUser, "HOME");

        // AI welcome
        addBotMessage("Hi " + (currentUser != null ? currentUser.getFirstName() : "") +
                "! \ud83e\udd16 I'm your TripX Assistant. How can I help you plan your dream trip today?");

        updateUserDisplay();
        initializeTripPlanner();

        loadFeaturedDestinations();
        loadFeaturedActivities();
        loadRecommendations();
        loadFeaturedAccommodations();
        populateSearchCombos();
    }

    private void loadRecommendations() {
        if (currentUser == null || activityLogService == null || recommendationsContainer == null) return;

        List<Long> recommendedIds = activityLogService.getRecommendationsByClicks(currentUser.getUserId(), 6);

        if (recommendedIds.isEmpty()) {
            recommendationsSection.setVisible(false);
            recommendationsSection.setManaged(false);
            return;
        }

        recommendationsSection.setVisible(true);
        recommendationsSection.setManaged(true);
        recommendationsContainer.getChildren().clear();

        for (Long id : recommendedIds) {
            Destination d = destinationService.getDestinationById(id);
            if (d != null) {
                recommendationsContainer.getChildren().add(buildDestinationCard(d));
            }
        }
    }

    private void updateUserDisplay() {
        if (currentUser == null) return;

        if (userNameLabel != null) {
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
        }

        // Set initials
        String initials = "";
        if (currentUser.getFirstName() != null && !currentUser.getFirstName().isEmpty()) {
            initials += currentUser.getFirstName().substring(0, 1).toUpperCase();
        }
        if (currentUser.getLastName() != null && !currentUser.getLastName().isEmpty()) {
            initials += currentUser.getLastName().substring(0, 1).toUpperCase();
        }

        if (initials.isEmpty()) {
            initials = "U";
        }

        final String finalInitials = initials;

        // Get final references for lambda
        final Label finalAvatarInitials = this.avatarInitials;
        final ImageView finalUserAvatarView = this.userAvatarView;

        // Hide ImageView initially, show initials
        if (finalUserAvatarView != null) {
            finalUserAvatarView.setVisible(false);
            finalUserAvatarView.setManaged(false);
        }

        if (finalAvatarInitials != null) {
            finalAvatarInitials.setText(finalInitials);
            finalAvatarInitials.setVisible(true);
            finalAvatarInitials.setManaged(true);
        }

        // Try to load avatar from DiceBear
        String avatarId = currentUser.getAvatarId();
        if (avatarId != null && avatarId.contains(":")) {
            String[] parts = avatarId.split(":");
            String style = parts[0];
            String seed = parts[1];
            String avatarUrl = "https://api.dicebear.com/9.x/" + style + "/png?seed=" + seed + "&size=40&backgroundColor=4cccad";

            Thread loadThread = new Thread(() -> {
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(avatarUrl, 40, 40, true, true, true);
                    Platform.runLater(() -> {
                        if (finalUserAvatarView != null && !img.isError()) {
                            finalUserAvatarView.setImage(img);

                            // Create circular clip
                            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);
                            finalUserAvatarView.setClip(clip);

                            // Show ImageView, hide initials
                            finalUserAvatarView.setVisible(true);
                            finalUserAvatarView.setManaged(true);

                            if (finalAvatarInitials != null) {
                                finalAvatarInitials.setVisible(false);
                                finalAvatarInitials.setManaged(false);
                            }
                        }
                    });
                } catch (Exception e) {
                    // Keep showing initials, that's fine
                    System.err.println("Could not load avatar: " + e.getMessage());
                }
            });
            loadThread.setDaemon(true);
            loadThread.start();
        }
    }

    @FXML
    public void initialize() {}


    // ─── Populate ComboBoxes ──────────────────────────────────────────────────

    private void populateSearchCombos() {
        // Destinations
        if (destinationCombo != null) {
            List<Destination> dests = destinationService.getAllDestinations();
            List<String> names = dests.stream().map(Destination::getName).toList();
            destinationCombo.setItems(FXCollections.observableArrayList(names));
        }
        // Activities
        if (activityCombo != null) {
            List<Activity> acts = activityService.getAllActivities();
            List<String> names = acts.stream().map(Activity::getName).toList();
            activityCombo.setItems(FXCollections.observableArrayList(names));
        }
    }

    // ─── Load Featured Cards ──────────────────────────────────────────────────

    private void loadFeaturedDestinations() {
        if (destinationsContainer == null) return;
        destinationsContainer.getChildren().clear();

        List<Destination> top = destinationService.getTopRatedDestinations(6);
        if (top.isEmpty()) {
            top = destinationService.getAllDestinations();
        }

        for (Destination d : top) {
            destinationsContainer.getChildren().add(buildDestinationCard(d));
        }

        if (top.isEmpty()) {
            Label lbl = new Label("No destinations yet. Visit the admin to add some!");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 30;");
            destinationsContainer.getChildren().add(lbl);
        }
    }

    private void loadFeaturedActivities() {
        if (activitiesContainer == null) return;
        activitiesContainer.getChildren().clear();

        List<Activity> all = activityService.getAllActivities();
        List<Activity> top = all.size() > 6 ? all.subList(0, 6) : all;

        for (Activity a : top) {
            activitiesContainer.getChildren().add(buildActivityCard(a));
        }

        if (top.isEmpty()) {
            Label lbl = new Label("No activities yet.");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 30;");
            activitiesContainer.getChildren().add(lbl);
        }
    }

    private void loadFeaturedAccommodations() {
        if (accommodationsContainer == null) return;
        accommodationsContainer.getChildren().clear();

        List<Accommodation> all = accommodationService.getAll();
        // Limit to top 6
        List<Accommodation> top = all.size() > 6 ? all.subList(0, 6) : all;

        for (Accommodation a : top) {
            accommodationsContainer.getChildren().add(buildAccommodationCard(a));
        }

        if (top.isEmpty()) {
            Label lbl = new Label("No accommodations yet.");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 30;");
            accommodationsContainer.getChildren().add(lbl);
        }
    }

    // ─── Card Builders ────────────────────────────────────────────────────────

    private VBox buildDestinationCard(Destination d) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-padding: 0;" +
                        "-fx-min-width: 240; -fx-max-width: 240;" +
                        "-fx-cursor: hand;"
        );

        // Image / colour banner
        StackPane banner = new StackPane();
        banner.setPrefHeight(140);
        banner.setPrefWidth(240);

        if (d.getImageUrl() != null && !d.getImageUrl().isEmpty()) {
            try {
                ImageView iv = new ImageView(new Image(d.getImageUrl(), true));
                iv.setFitWidth(240);
                iv.setFitHeight(140);
                iv.setPreserveRatio(false);
                banner.getChildren().add(iv);
            } catch (Exception ex) {
                banner.setStyle(getDestBannerColor(d.getType()) + "-fx-background-radius: 14 14 0 0;");
                Label icon = new Label(getDestIcon(d.getType()));
                icon.setStyle("-fx-font-size: 44px;");
                banner.getChildren().add(icon);
            }
        } else {
            banner.setStyle(getDestBannerColor(d.getType()) + "-fx-background-radius: 14 14 0 0;");
            Label icon = new Label(getDestIcon(d.getType()));
            icon.setStyle("-fx-font-size: 44px;");
            banner.getChildren().add(icon);
        }

        // Info area
        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 12 14 8 14;");

        Label name = new Label(d.getName());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label loc = new Label("📍 " + d.getFullLocation());
        loc.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = new Label(d.getType() != null ? d.getType().toString() : "");
        typeBadge.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        Label rating = new Label("⭐ " + String.format("%.1f", d.getAverageRating()));
        rating.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
        badges.getChildren().addAll(typeBadge, rating);

        Button bookBtn = new Button("🎫 Book Now");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #1a73e8, #0d47a1);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 9 14; -fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        bookBtn.setOnAction(e -> openBookingDialog(d));

        info.getChildren().addAll(name, loc, badges, bookBtn);
        card.getChildren().addAll(banner, info);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
                "-fx-effect: dropshadow(gaussian, rgba(26,115,232,0.4), 18, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-padding: 0;" +
                        "-fx-min-width: 240; -fx-max-width: 240;" +
                        "-fx-cursor: hand;"
        ));

        card.setOnMouseClicked(e -> {
            if (currentUser != null && activityLogService != null) {
                activityLogService.logActivity(new UserActivity(currentUser.getUserId(), "CLICK", d.getDestinationId(), "DESTINATION"));
            }
            navigateToDestinations(null);
        });

        return card;
    }

    private VBox buildActivityCard(Activity a) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-padding: 0;" +
                        "-fx-min-width: 220; -fx-max-width: 220;" +
                        "-fx-cursor: hand;"
        );

        StackPane banner = new StackPane();
        banner.setPrefHeight(120);
        banner.setStyle(getActivityBannerColor(a.getCategory()) +
                "-fx-background-radius: 14 14 0 0;");
        Label icon = new Label(getActivityIcon(a.getCategory()));
        icon.setStyle("-fx-font-size: 40px;");
        banner.getChildren().add(icon);

        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 10 12 8 12;");

        Label name = new Label(a.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label dest = new Label("📍 " + (a.getDestinationName() != null ? a.getDestinationName() : ""));
        dest.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(a.getCategory() != null ? a.getCategory().toString() : "");
        catBadge.setStyle("-fx-background-color: #fce4ec; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c2185b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label price = new Label("$" + String.format("%.0f", a.getPrice()));
        price.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        priceRow.getChildren().addAll(catBadge, spacer, price);

        Button bookBtn = new Button("🎫 Book");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #27ae60, #1e8449);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        bookBtn.setOnAction(e -> openActivityBooking(a));

        info.getChildren().addAll(name, dest, priceRow, bookBtn);
        card.getChildren().addAll(banner, info);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
                "-fx-effect: dropshadow(gaussian, rgba(39,174,96,0.4), 18, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-padding: 0;" +
                        "-fx-min-width: 220; -fx-max-width: 220;" +
                        "-fx-cursor: hand;"
        ));

        card.setOnMouseClicked(e -> {
            if (currentUser != null && activityLogService != null) {
                activityLogService.logActivity(new UserActivity(currentUser.getUserId(), "CLICK", a.getActivityId(), "ACTIVITY"));
            }
            navigateToActivities();
        });

        return card;
    }

    private VBox buildAccommodationCard(Accommodation a) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-padding: 0;" +
                        "-fx-min-width: 220; -fx-max-width: 220;" +
                        "-fx-cursor: hand;"
        );

        StackPane banner = new StackPane();
        banner.setPrefHeight(120);

        // Simple color based on type
        String type = a.getType() != null ? a.getType().toLowerCase() : "";
        String color = "-fx-background-color: linear-gradient(to bottom, #34495e, #2c3e50);"; // dark default
        String iconEmoji = "🏨";

        if (type.contains("hotel")) { color = "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);"; iconEmoji = "🏨"; }
        else if (type.contains("apartment")) { color = "-fx-background-color: linear-gradient(to bottom, #9b59b6, #8e44ad);"; iconEmoji = "🏢"; }
        else if (type.contains("villa")) { color = "-fx-background-color: linear-gradient(to bottom, #f1c40f, #f39c12);"; iconEmoji = "🏡"; }
        else if (type.contains("resort")) { color = "-fx-background-color: linear-gradient(to bottom, #1abc9c, #16a085);"; iconEmoji = "🏝️"; }

        banner.setStyle(color + "-fx-background-radius: 14 14 0 0;");
        Label icon = new Label(iconEmoji);
        icon.setStyle("-fx-font-size: 40px;");
        banner.getChildren().add(icon);

        VBox info = new VBox(5);
        info.setStyle("-fx-padding: 10 12 8 12;");

        Label name = new Label(a.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label loc = new Label("📍 " + a.getCity() + ", " + a.getCountry());
        loc.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        HBox starsRow = new HBox(8);
        starsRow.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = new Label(a.getType());
        typeBadge.setStyle("-fx-background-color: #f3e5f5; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7b1fa2;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StringBuilder starStr = new StringBuilder();
        for(int i=0; i<a.getStars(); i++) starStr.append("★");
        Label stars = new Label(starStr.toString());
        stars.setStyle("-fx-font-size: 12px; -fx-text-fill: #f1c40f;");

        starsRow.getChildren().addAll(typeBadge, spacer, stars);

        Button viewBtn = new Button("🔍 Details");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #34495e, #2c3e50);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );
        viewBtn.setOnAction(e -> navigateToAccommodations());

        info.getChildren().addAll(name, loc, starsRow, viewBtn);
        card.getChildren().addAll(banner, info);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() +
                "-fx-effect: dropshadow(gaussian, rgba(52,73,94,0.4), 18, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-padding: 0;" +
                        "-fx-min-width: 220; -fx-max-width: 220;" +
                        "-fx-cursor: hand;"
        ));

        card.setOnMouseClicked(e -> {
            navigateToAccommodations();
        });

        return card;
    }

    // ─── Booking Handlers ─────────────────────────────────────────────────────

    private void openBookingDialog(Destination dest) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/booking_dialog.fxml"));
            Parent root = loader.load();
            BookingDialogController ctrl = loader.getController();
            ctrl.setDestination(dest);
            Stage stage = new Stage();
            stage.setTitle("Book: " + dest.getName());
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Could not open booking dialog.");
        }
    }

    private void openActivityBooking(Activity activity) {
        // Navigate to user_destinations filtered so the booking dialog opens for the activity's destination
        try {
            Destination dest = destinationService.getDestinationById(activity.getDestinationId());
            if (dest != null) {
                // Pre-select the activity, then open booking dialog
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/booking_dialog.fxml"));
                Parent root = loader.load();
                BookingDialogController ctrl = loader.getController();
                ctrl.setDestination(dest);
                // This method needs to be added to BookingDialogController
                // ctrl.preSelectActivity(activity);
                Stage stage = new Stage();
                stage.setTitle("Book Activity: " + activity.getName());
                stage.setScene(new Scene(root));
                stage.showAndWait();
            } else {
                showAlert("Destination not found for this activity.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Could not open booking dialog.");
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @FXML
    private void handleSearch(ActionEvent event) {
        String destQuery = destinationCombo != null ? destinationCombo.getValue() : null;
        String actQuery  = activityCombo  != null ? activityCombo.getValue()  : null;
        String accQuery  = accommodationSearchField != null ? accommodationSearchField.getText() : null;

        if (destQuery != null && !destQuery.trim().isEmpty()) {
            tn.esprit.utils.ActivityLogger.logSearch(currentUser, destQuery.trim());
            navigateToDestinations(destQuery.trim());
        } else if (actQuery != null && !actQuery.trim().isEmpty()) {
            tn.esprit.utils.ActivityLogger.logSearch(currentUser, actQuery.trim());
            navigateToActivities();
        } else if (accQuery != null && !accQuery.trim().isEmpty()) {
            tn.esprit.utils.ActivityLogger.logSearch(currentUser, accQuery.trim());
            navigateToAccommodations();
        } else {
            // weather fallback
            WeatherPreferenceMatcher.DestinationMatch match = null;
            if (destQuery != null && !destQuery.isEmpty()) {
                match = weatherMatcher.searchDestinationWithWeather(destQuery);
            }
            if (match != null) {
                showWeatherResult(match);
            } else {
                navigateToDestinations(null);
            }
        }
    }

    @FXML
    private void handleBrowseDestinations(ActionEvent event) {
        navigateToDestinations(null);
    }

    @FXML
    private void handleBrowseActivities(ActionEvent event) {
        navigateToActivities();
    }

    // Top-bar navigation: Destinations
    @FXML
    private void handleNavDestinations() {
        navigateToDestinations(null);
    }

    // Top-bar navigation: Activities
    @FXML
    private void handleNavActivities() {
        navigateToActivities();
    }

    // Top-bar navigation: Transport
    @FXML
    private void handleNavTransport() {
        navigateToTransport();
    }

    // Top-bar navigation: Blog
    @FXML
    private void handleNavBlog() {
        showAlert("Blog page coming soon!");
    }

    @FXML
    private void handleMyBookings(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/my_bookings.fxml"));
            Parent root = loader.load();

            UserBookingsController ctrl = loader.getController();
            if (ctrl != null && currentUser != null) {
                ctrl.setCurrentUser(currentUser);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            stage.setScene(new Scene(root, width, height));

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not load bookings: " + e.getMessage());
        }
    }

    private Stage getStage() {
        // Primary lookup from a stable node on Home
        if (destinationsContainer != null && destinationsContainer.getScene() != null) {
            return (Stage) destinationsContainer.getScene().getWindow();
        }
        // Fallbacks to reduce risk of null stage in some contexts
        if (activitiesContainer != null && activitiesContainer.getScene() != null) {
            return (Stage) activitiesContainer.getScene().getWindow();
        }
        if (chatPanel != null && chatPanel.getScene() != null) {
            return (Stage) chatPanel.getScene().getWindow();
        }
        return null;
    }

    // ─── AI Chat ──────────────────────────────────────────────────────────────

    @FXML
    private void toggleChat() {
        boolean isVisible = chatPanel.isVisible();
        chatPanel.setVisible(!isVisible);
        if (!isVisible) chatInputField.requestFocus();
    }

    @FXML
    private void handleSendMessage() {
        String message = chatInputField.getText().trim();
        if (message.isEmpty()) return;

        addUserMessage(message);
        chatInputField.clear();

        // Track AI Chat use
        tn.esprit.utils.ActivityLogger.logFeatureUse(currentUser, "AI_CHAT");

        Label loadingLabel = new Label("TripX AI is thinking...");
        loadingLabel.getStyleClass().add("ai-bubble-bot");
        loadingLabel.setStyle("-fx-opacity: 0.6; -fx-font-style: italic;");
        chatMessageContainer.getChildren().add(loadingLabel);
        scrollToBottom();

        UserPreferences prefs = (currentUser != null) ?
                preferencesService.getPreferencesByUserId(currentUser.getUserId()) : null;

        geminiService.getRecommendation(message, prefs)
                .thenAccept(response -> Platform.runLater(() -> {
                    chatMessageContainer.getChildren().remove(loadingLabel);
                    addBotMessage(response);
                }));
    }

    private void addUserMessage(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("ai-bubble-user");
        label.setWrapText(true);
        HBox container = new HBox(label);
        container.setAlignment(Pos.CENTER_RIGHT);
        chatMessageContainer.getChildren().add(container);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("ai-bubble-bot");
        label.setWrapText(true);
        HBox container = new HBox(label);
        container.setAlignment(Pos.CENTER_LEFT);
        chatMessageContainer.getChildren().add(container);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (chatScrollPane != null) chatScrollPane.setVvalue(1.0);
    }

    // ─── Weather Handler ──────────────────────────────────────────────────────

    @FXML
    private void handleWeatherMatch(ActionEvent event) {
        if (currentUser == null) { showAlert("Please log in to use this feature"); return; }
        UserPreferences prefs = preferencesService.getPreferencesByUserId(currentUser.getUserId());
        if (prefs == null || prefs.getPreferredClimate() == null) {
            showAlert("Please set your climate preferences in your profile first");
            return;
        }
        new Thread(() -> {
            // Track Climate Match use
            tn.esprit.utils.ActivityLogger.logFeatureUse(currentUser, "CLIMATE_MATCH");

            List<WeatherPreferenceMatcher.DestinationMatch> matches =
                    weatherMatcher.getDestinationsMatchingClimate(prefs);
            Platform.runLater(() -> displayWeatherMatches(matches));
        }).start();
    }

    private void displayWeatherMatches(List<WeatherPreferenceMatcher.DestinationMatch> matches) {
        if (weatherResultsContainer == null) return;
        weatherResultsContainer.getChildren().clear();

        if (matches.isEmpty()) {
            Label noMatch = new Label("No perfect climate matches found, but here are some popular options!");
            noMatch.setStyle("-fx-text-fill: #7f8c8d; -fx-italic: true; -fx-padding: 10;");
            weatherResultsContainer.getChildren().add(noMatch);
            return;
        }

        HBox resultsScrollContainer = new HBox(20);
        resultsScrollContainer.setAlignment(Pos.CENTER_LEFT);
        resultsScrollContainer.setStyle("-fx-padding: 10;");

        for (WeatherPreferenceMatcher.DestinationMatch match : matches) {
            resultsScrollContainer.getChildren().add(buildWeatherMatchCard(match));
        }

        ScrollPane scrollPane = new ScrollPane(resultsScrollContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(400); // Increased for better visibility and adaptability
        scrollPane.setMinHeight(200);
        scrollPane.setMaxHeight(600);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        weatherResultsContainer.setVisible(true);
        weatherResultsContainer.setManaged(true);
        weatherResultsContainer.getChildren().add(scrollPane);
        // Ensure parent container expands
        weatherResultsContainer.setMinHeight(Region.USE_COMPUTED_SIZE);
    }

    private VBox buildWeatherMatchCard(WeatherPreferenceMatcher.DestinationMatch match) {
        Destination d = match.getDestination();
        WeatherServiceUser.WeatherInfo w = match.getWeather();

        VBox card = new VBox(10);
        card.getStyleClass().add("weather-match-card");
        card.setPrefWidth(280);
        card.setMinWidth(280);

        // Header with Match Score
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label matchBadge = new Label(match.getMatchScore() + "% Match");
        matchBadge.getStyleClass().add("match-score-badge");
        if (match.getMatchScore() >= 90) matchBadge.setStyle(matchBadge.getStyle() + "-fx-background-color: #27ae60;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label typeIcon = new Label(getDestIcon(d.getType()));
        typeIcon.setStyle("-fx-font-size: 20px;");

        header.getChildren().addAll(matchBadge, spacer, typeIcon);

        // Destination Info
        VBox info = new VBox(2);
        Label name = new Label(d.getName());
        name.getStyleClass().add("weather-card-name");

        Label location = new Label(d.getCity() + ", " + d.getCountry());
        location.getStyleClass().add("weather-card-location");

        info.getChildren().addAll(name, location);

        // Weather Info (if available)
        HBox weatherBox = new HBox(12);
        weatherBox.setAlignment(Pos.CENTER_LEFT);
        weatherBox.getStyleClass().add("weather-info-container");

        if (w != null) {
            Label temp = new Label(w.getWeatherEmoji() + " " + String.format("%.1f°C", w.getTemperature()));
            temp.getStyleClass().add("weather-card-temp");

            Label cond = new Label(w.getCondition());
            cond.getStyleClass().add("weather-card-condition");

            weatherBox.getChildren().addAll(temp, cond);
        } else {
            Label unavailable = new Label("☁️ Weather currently unavailable");
            unavailable.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px; -fx-font-style: italic;");
            weatherBox.getChildren().add(unavailable);
        }

        // Action Button
        Button viewBtn = new Button("View Destination");
        viewBtn.getStyleClass().add("weather-card-button");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> navigateToDestinations(d.getName()));

        card.getChildren().addAll(header, info, weatherBox, viewBtn);

        return card;
    }


    private void showWeatherResult(WeatherPreferenceMatcher.DestinationMatch match) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Destination Found");
        alert.setHeaderText(match.getDestination().getName() + ", " + match.getDestination().getCountry());
        String weatherInfo = (match.getWeather() != null) ? match.getWeather().toString() : "Weather data unavailable";
        alert.setContentText("📍 " + match.getDestination().getFullLocation() +
                "\n🌡️ " + weatherInfo + "\n📝 " + match.getDestination().getDescription());
        alert.showAndWait();
    }

    // ─── Profile / Logout ─────────────────────────────────────────────────────

    @FXML
    private void handleProfile(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/profile.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.setUser(currentUser);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();

            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not open profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            SessionManager.setCurrentUserId(-1);
            double width = stage.getWidth();
            double height = stage.getHeight();
            stage.setScene(new Scene(root, width, height));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ─── Icon / Color helpers ─────────────────────────────────────────────────

    private String getDestBannerColor(Destination.DestinationType type) {
        if (type == null) return "-fx-background-color: linear-gradient(to bottom, #9B59B6, #8E44AD);";
        return switch (type) {
            case beach      -> "-fx-background-color: linear-gradient(to bottom, #FFD700, #FFA500);";
            case mountain   -> "-fx-background-color: linear-gradient(to bottom, #95A5A6, #7F8C8D);";
            case city       -> "-fx-background-color: linear-gradient(to bottom, #3498DB, #2980B9);";
            case desert     -> "-fx-background-color: linear-gradient(to bottom, #F4D03F, #D4AC0D);";
            case island     -> "-fx-background-color: linear-gradient(to bottom, #1ABC9C, #16A085);";
            case forest     -> "-fx-background-color: linear-gradient(to bottom, #27AE60, #229954);";
            case countryside-> "-fx-background-color: linear-gradient(to bottom, #2ECC71, #27AE60);";
            default         -> "-fx-background-color: linear-gradient(to bottom, #9B59B6, #8E44AD);";
        };
    }

    private String getDestIcon(Destination.DestinationType type) {
        if (type == null) return "🌍";
        return switch (type) {
            case beach      -> "🏖️";
            case mountain   -> "⛰️";
            case city       -> "🏙️";
            case desert     -> "🏜️";
            case island     -> "🏝️";
            case forest     -> "🌲";
            case countryside-> "🌾";
            case cruise     -> "🚢";
            default         -> "🌍";
        };
    }

    private String getActivityBannerColor(Activity.ActivityCategory cat) {
        if (cat == null) return "-fx-background-color: linear-gradient(to bottom, #95a5a6, #7f8c8d);";
        return switch (cat) {
            case Adventure  -> "-fx-background-color: linear-gradient(to bottom, #e67e22, #d35400);";
            case Culture    -> "-fx-background-color: linear-gradient(to bottom, #9b59b6, #8e44ad);";
            case Relax      -> "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);";
            case Food       -> "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b);";
            case Sports     -> "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60);";
            case Nightlife  -> "-fx-background-color: linear-gradient(to bottom, #2c3e50, #1a252f);";
            default         -> "-fx-background-color: linear-gradient(to bottom, #95a5a6, #7f8c8d);";
        };
    }

    private String getActivityIcon(Activity.ActivityCategory cat) {
        if (cat == null) return "✨";
        return switch (cat) {
            case Adventure  -> "🧗";
            case Culture    -> "🏛️";
            case Relax      -> "🧘";
            case Food       -> "🍱";
            case Sports     -> "⚽";
            case Nightlife  -> "🎉";
            default         -> "✨";
        };
    }

    // ========== TRIP PLANNER METHODS ==========

    /**
     * Initialize the trip planner with user preferences and destinations
     */
    private void initializeTripPlanner() {
        if (plannerDestinationCombo == null) return;

        // Load destinations into combo box
        List<Destination> destinations = destinationService.getAllDestinations();
        List<String> destNames = destinations.stream()
                .map(d -> d.getName() + ", " + d.getCountry())
                .toList();
        plannerDestinationCombo.setItems(FXCollections.observableArrayList(destNames));

        // Set travel style from user preferences
        if (currentUser != null) {
            UserPreferences prefs = preferencesService.getPreferencesByUserId(currentUser.getUserId());
            if (prefs != null) {
                String style = "";
                if (prefs.getTravelPace() != null) style += prefs.getTravelPace() + " pace";
                if (prefs.getPreferredClimate() != null) {
                    if (!style.isEmpty()) style += " • ";
                    style += prefs.getPreferredClimate() + " climate";
                }
                if (prefs.getGroupType() != null) {
                    if (!style.isEmpty()) style += " • ";
                    style += prefs.getGroupType();
                }
                plannerStyleField.setText(style.isEmpty() ? "Not set - using defaults" : style);
            } else {
                plannerStyleField.setText("No preferences set - using defaults");
            }
        }

        // Setup generate button
        generatePlanBtn.setOnAction(e -> generateTripPlan());

        // Setup book plan button
        bookPlanBtn.setOnAction(e -> bookEntirePlan());
    }

    /**
     * Generate a trip plan using AI
     */
    @FXML
    private void generateTripPlan() {
        // Validate inputs
        if (!validatePlannerInputs()) return;

        // Get selected destination
        String selected = plannerDestinationCombo.getValue();
        String destinationName = selected.split(",")[0].trim();
        Destination destination = destinationService.searchDestinations(destinationName)
                .stream().findFirst().orElse(null);

        if (destination == null) {
            showAlert("Destination not found in database");
            return;
        }

        int days = Integer.parseInt(plannerDaysField.getText().trim());

        // Get user preferences
        UserPreferences prefs = currentUser != null ?
                preferencesService.getPreferencesByUserId(currentUser.getUserId()) : null;

        // Get activities for this destination
        List<Activity> activities = activityService.getActivitiesByDestination(destination.getDestinationId());

        // Show loading
        plannerProgress.setVisible(true);
        generatePlanBtn.setDisable(true);

        // Track Trip Planner use
        tn.esprit.utils.ActivityLogger.logFeatureUse(currentUser, "TRIP_PLANNER");

        // Build prompt for AI
        String prompt = buildTripPlanPrompt(destination, days, activities, prefs);

        // Call Gemini
        geminiService.getRecommendation(prompt, prefs)
                .thenAccept(response -> Platform.runLater(() -> {
                    plannerProgress.setVisible(false);
                    generatePlanBtn.setDisable(false);

                    // DEBUG: Print the response
                    System.out.println("===== AI RESPONSE =====");
                    System.out.println(response);
                    System.out.println("======================");

                    if (response == null || response.trim().isEmpty() || response.startsWith("Error:")) {
                        // Show error in UI
                        Label errorLabel = new Label("❌ " + response);
                        errorLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        planDetailsContainer.getChildren().clear();
                        planDetailsContainer.getChildren().add(errorLabel);
                        planResultsContainer.setVisible(true);
                    } else {
                        displayTripPlan(response, destination, activities, days);
                    }
                }));
    }

    /**
     * Validate planner inputs
     */
    private boolean validatePlannerInputs() {
        boolean valid = true;

        // Reset errors
        plannerDestinationError.setVisible(false);
        plannerDestinationError.setManaged(false);
        plannerDaysError.setVisible(false);
        plannerDaysError.setManaged(false);

        // Validate destination
        if (plannerDestinationCombo.getValue() == null || plannerDestinationCombo.getValue().isEmpty()) {
            plannerDestinationError.setText("Please select a destination");
            plannerDestinationError.setVisible(true);
            plannerDestinationError.setManaged(true);
            valid = false;
        }

        // Validate days
        String daysText = plannerDaysField.getText().trim();
        if (daysText.isEmpty()) {
            plannerDaysError.setText("Please enter number of days");
            plannerDaysError.setVisible(true);
            plannerDaysError.setManaged(true);
            valid = false;
        } else {
            try {
                int days = Integer.parseInt(daysText);
                if (days < 1 || days > 30) {
                    plannerDaysError.setText("Days must be between 1 and 30");
                    plannerDaysError.setVisible(true);
                    plannerDaysError.setManaged(true);
                    valid = false;
                }
            } catch (NumberFormatException e) {
                plannerDaysError.setText("Please enter a valid number");
                plannerDaysError.setVisible(true);
                plannerDaysError.setManaged(true);
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Build prompt for AI trip plan
     */
    private String buildTripPlanPrompt(Destination dest, int days, List<Activity> activities, UserPreferences prefs) {
        StringBuilder sb = new StringBuilder();

        sb.append("Create a detailed ").append(days).append("-day travel itinerary for ");
        sb.append(dest.getName()).append(", ").append(dest.getCountry()).append(". ");

        if (!activities.isEmpty()) {
            sb.append("\n\nAvailable activities in this destination:\n");
            for (Activity a : activities) {
                sb.append("- ").append(a.getName()).append(" ($").append(a.getPrice()).append(")");
                if (a.getCategory() != null) sb.append(" [").append(a.getCategory()).append("]");
                sb.append("\n");
            }
        }

        if (prefs != null) {
            sb.append("\n\nUser preferences:\n");
            if (prefs.getTravelPace() != null) sb.append("- Travel pace: ").append(prefs.getTravelPace()).append("\n");
            if (prefs.getPreferredClimate() != null) sb.append("- Climate preference: ").append(prefs.getPreferredClimate()).append("\n");
            if (prefs.getGroupType() != null) sb.append("- Traveling with: ").append(prefs.getGroupType()).append("\n");
            if (prefs.getBudgetMinPerNight() != null) {
                sb.append("- Budget per night: $").append(prefs.getBudgetMinPerNight())
                        .append(" - $").append(prefs.getBudgetMaxPerNight()).append("\n");
            }
        }

        sb.append("\n\nPlease make the description detailed and immersive. ");
        sb.append("For each day, describe the activities with more details just a little more - what makes them special, ");
        sb.append("what to expect, approximate duration, and any tips. ");
        sb.append("Include specific names of beaches, restaurants, or attractions when possible. ");
        sb.append("Make it feel like a real travel guide!");

        return sb.toString();
    }

    /**
     * Book the entire plan (destination + selected activities)
     */
    @FXML
    private void bookEntirePlan() {
        if (currentUser == null) {
            showAlert("Please log in to book");
            return;
        }

        String selected = plannerDestinationCombo.getValue();
        if (selected == null) return;

        String destinationName = selected.split(",")[0].trim();
        Destination destination = destinationService.searchDestinations(destinationName)
                .stream().findFirst().orElse(null);

        if (destination == null) return;

        // Open booking dialog for the destination
        openBookingDialog(destination);

        // Success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Started");
        alert.setHeaderText("Your trip planning has begun!");
        alert.setContentText("You can now book activities individually from the list above.");
        alert.showAndWait();
    }

    private void generatePDF(String plan, Destination destination) {
        try {
            // Create file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Trip Plan");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            fileChooser.setInitialFileName(destination.getName() + "_Trip_Plan.pdf");

            java.io.File file = fileChooser.showSaveDialog(plannerDestinationCombo.getScene().getWindow());
            if (file == null) return;

            // Create PDF document
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("Trip Plan: " + destination.getName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add destination info
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);
            Paragraph destInfo = new Paragraph(destination.getDescription(), infoFont);
            destInfo.setSpacingAfter(20);
            document.add(destInfo);

            // Add the plan
            Font planFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);
            Paragraph planParagraph = new Paragraph(plan, planFont);
            document.add(planParagraph);

            // Close document
            document.close();

            showAlert("✅ PDF Generated", "Trip plan saved to:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("❌ Error", "Could not generate PDF: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayTripPlan(String plan, Destination destination, List<Activity> activities, int days) {
        // Store for PDF generation
        this.currentGeneratedPlan = plan;
        this.currentPlanDestination = destination;

        planDetailsContainer.getChildren().clear();

        // Set destination info
        planDestinationIcon.setText(getDestIcon(destination.getType()));
        planDestinationName.setText(destination.getName() + ", " + destination.getCountry());
        planDestinationDesc.setText(destination.getDescription());

        // Format the plan nicely - better spacing and formatting
        String[] lines = plan.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Label lineLabel = new Label(line);
            lineLabel.setWrapText(true);

            // Different styles for different types of lines using CSS classes
            if (line.matches("(?i).*day\\s*\\d+.*") || line.matches("(?i)day\\s*\\d+.*")) {
                lineLabel.getStyleClass().add("ai-plan-day-header");
            }
            else if (line.matches("(?i).*morning:.*") || line.matches("(?i).*afternoon:.*") || line.matches("(?i).*evening:.*")) {
                lineLabel.getStyleClass().add("ai-plan-time-of-day");
            }
            else if (line.matches("(?i).*breakfast.*") || line.matches("(?i).*lunch.*") || line.matches("(?i).*dinner.*")) {
                lineLabel.getStyleClass().add("ai-plan-meal");
            }
            else if (line.matches("(?i).*total.*cost.*") || line.matches("(?i).*estimated.*budget.*")) {
                lineLabel.getStyleClass().add("ai-plan-cost");
            }
            else if (line.matches(".*[0-9]+\\.\\s.*") || line.matches(".*\\*\\s.*")) {
                lineLabel.getStyleClass().add("ai-plan-bullet");
            }
            else {
                lineLabel.getStyleClass().add("ai-plan-text");
            }

            planDetailsContainer.getChildren().add(lineLabel);
        }

        // Add a separator after the plan
        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10 0;");
        planDetailsContainer.getChildren().add(separator);

        // Available Activities Section
        if (!activities.isEmpty()) {
            Label activitiesTitle = new Label("🎯 Available Activities in " + destination.getName());
            activitiesTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Poppins'; -fx-padding: 15 0 15 0; -fx-text-fill: #0a1a2f;");
            planDetailsContainer.getChildren().add(activitiesTitle);

            for (Activity act : activities) {
                VBox actCard = new VBox(8);
                actCard.setStyle("-fx-padding: 15; -fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");

                HBox actHeader = new HBox(15);
                actHeader.setAlignment(Pos.CENTER_LEFT);

                Label actIcon = new Label(getActivityIcon(act.getCategory()));
                actIcon.setStyle("-fx-font-size: 24px; -fx-min-width: 40;");

                VBox actInfo = new VBox(3);
                Label actName = new Label(act.getName());
                actName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label actDesc = new Label(act.getDescription());
                actDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
                actDesc.setWrapText(true);

                actInfo.getChildren().addAll(actName, actDesc);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label actPrice = new Label("$" + String.format("%.0f", act.getPrice()));
                actPrice.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-padding: 0 10;");

                actHeader.getChildren().addAll(actIcon, actInfo, spacer, actPrice);

                Button addBtn = new Button("➕ Add to My Booking");
                addBtn.setStyle("-fx-background-color: #4cccad; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
                addBtn.setMaxWidth(200);

                actCard.getChildren().addAll(actHeader, addBtn);
                planDetailsContainer.getChildren().add(actCard);
            }
        }

        // Show results and enable buttons
        planResultsContainer.setVisible(true);
        planResultsContainer.setManaged(true);
        downloadPdfBtn.setVisible(true);
        downloadPdfBtn.setManaged(true);
        bookPlanBtn.setDisable(false);

        // Setup PDF download button
        downloadPdfBtn.setOnAction(e -> generatePDF(plan, destination));
    }

    private String getLineStyle(String line) {
        if (line.startsWith("Day ") || line.contains("Day ")) {
            return "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0a1a2f; -fx-padding: 10 0 5 0;";
        } else if (line.contains("Total") || line.contains("Cost")) {
            return "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-padding: 10 0 0 0;";
        } else if (line.trim().isEmpty()) {
            return "-fx-padding: 5 0;";
        } else {
            return "-fx-font-size: 13px; -fx-padding: 2 0 2 20;";
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Partie mta navigation mta les modules fel bar fel home

    private void navigateToDestinations(String search) {
        try {
            System.out.println("🔍 Attempting to load: /fxml/user/user_destinations.fxml");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/user_destinations.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("❌ ERROR: Could not find user_destinations.fxml");
                showAlert("Error: Destinations page not found!");
                return;
            }

            Parent root = loader.load();
            System.out.println("✅ Successfully loaded user_destinations.fxml");

            UserDestinationsController ctrl = loader.getController();
            if (ctrl == null) {
                System.err.println("❌ ERROR: Controller is null");
            } else {
                System.out.println("✅ Controller loaded: " + ctrl.getClass().getSimpleName());
                if (currentUser != null) {
                    ctrl.setCurrentUser(currentUser);
                    System.out.println("✅ User set in controller: " + currentUser.getEmail());
                }
            }

            Stage stage = getStage();
            if (stage != null) {
                double w = stage.getWidth();
                double h = stage.getHeight();
                stage.setScene(new Scene(root, w, h));
            } else if (chatPanel != null && chatPanel.getScene() != null) {
                chatPanel.getScene().setRoot(root);
            }

        } catch (IOException e) {
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
            showAlert("Could not load destinations: " + e.getMessage());
        }
    }

    private void navigateToActivities() {
        try {
            String path = "/fxml/user/user_activities.fxml";
            System.out.println("🔍 Loading activities from: " + path);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));

            if (loader.getLocation() == null) {
                System.err.println("❌ ERROR: File not found at " + path);
                showAlert("Activities page not found!");
                return;
            }

            Parent root = loader.load();
            System.out.println("✅ Successfully loaded activities");

            UserActivitiesController ctrl = loader.getController();
            if (ctrl != null && currentUser != null) {
                ctrl.setCurrentUser(currentUser);
                System.out.println("✅ User set in activities controller: " + currentUser.getEmail());
            }

            Stage stage = getStage();
            if (stage != null) {
                double w = stage.getWidth();
                double h = stage.getHeight();
                stage.setScene(new Scene(root, w, h));
            } else if (chatPanel != null && chatPanel.getScene() != null) {
                chatPanel.getScene().setRoot(root);
            }

        } catch (IOException e) {
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
            showAlert("Could not load activities: " + e.getMessage());
        }
    }


    // ========== NAVIGATION METHODS FOR OTHER MODULES ==========

    /**
     * Navigate to Accommodations page
     */
    @FXML
    private void navigateToAccommodations() {
        try {
            String path = "/fxml/user/AccommodationsView.fxml";
            System.out.println("🔍 Loading accommodations from: " + path);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));

            if (loader.getLocation() == null) {
                System.err.println("❌ ERROR: File not found at " + path);
                showAlert("Accommodations page not found!");
                return;
            }

            Parent root = loader.load();
            System.out.println("✅ Successfully loaded accommodations");

            AccommodationsController ctrl = loader.getController();
            if (ctrl != null && currentUser != null) {
                ctrl.setCurrentUser(currentUser);
                System.out.println("✅ User set in accommodations controller: " + currentUser.getEmail());
            }

            Stage stage = getStage();
            if (stage != null) {
                double w = stage.getWidth();
                double h = stage.getHeight();
                stage.setScene(new Scene(root, w, h));
            } else if (chatPanel != null && chatPanel.getScene() != null) {
                chatPanel.getScene().setRoot(root);
            }

        } catch (IOException e) {
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
            showAlert("Could not load accommodations: " + e.getMessage());
        }
    }

    /**
     * Navigate to Transport page
     */
    private void navigateToTransport() {
        try {
            String path = "/fxml/user/TransportUserInterface.fxml";
            System.out.println("🔍 Loading transport from: " + path);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));

            if (loader.getLocation() == null) {
                System.err.println("❌ ERROR: File not found at " + path);
                showAlert("Transport page not found!");
                return;
            }

            Parent root = loader.load();
            System.out.println("✅ Successfully loaded transport");

            TransportUserInterfaceController controller = loader.getController();
            if (controller != null && currentUser != null) {
                controller.setCurrentUser(currentUser);
                System.out.println("✅ User set in transport controller: " + currentUser.getEmail());
            }

            Stage stage = getStage();
            if (stage != null) {
                double w = stage.getWidth();
                double h = stage.getHeight();
                stage.setScene(new Scene(root, w, h));
                System.out.println("✅ Navigated to transport page");
            } else if (chatPanel != null && chatPanel.getScene() != null) {
                chatPanel.getScene().setRoot(root);
            }

        } catch (IOException e) {
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
            showAlert("Could not load transport: " + e.getMessage());
        }
    }

    private void navigateToPacksOffers() {
        try {
            String path = "/fxml/user/UserPacksOffersView.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            if (loader.getLocation() == null) {
                showAlert("Packs & Offers page not found!");
                return;
            }
            Parent root = loader.load();
            UserPacksOffersController ctrl = loader.getController();
            if (ctrl != null && currentUser != null) {
                ctrl.setCurrentUser(currentUser);
            }
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            } else if (chatPanel != null && chatPanel.getScene() != null) {
                chatPanel.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not load Packs & Offers: " + e.getMessage());
        }
    }

    /**
     * Navigate to Blog page
     */
    /*
    @FXML
    private void navigateToBlog() {
        try {
            String path = "/fxml/user/user_blog.fxml";
            System.out.println("🔍 Loading blog from: " + path);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));

            if (loader.getLocation() == null) {
                System.err.println("❌ ERROR: File not found at " + path);
                showAlert("Blog page not found!");
                return;
            }

            Parent root = loader.load();
            System.out.println("✅ Successfully loaded blog");

            // Get controller and set user if needed
            Object ctrl = loader.getController();
            // TODO: Set user in blog controller when implemented

            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root, w, h));
                System.out.println("✅ Navigated to blog page");
            } else {
                System.err.println("❌ ERROR: Stage is null");
            }

        } catch (IOException e) {
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
            showAlert("Could not load blog: " + e.getMessage());
        }
    }
    */


    @FXML
    private void handleHomeNav(MouseEvent mouseEvent) {
        System.out.println("Home clicked");
        // Already on home page, do nothing or refresh
    }

    @FXML
    private void handleDestinationsNav(MouseEvent mouseEvent) {
        navigateToDestinations(null);
    }

    @FXML
    private void handleAccommodationsNav(MouseEvent mouseEvent) {
        navigateToAccommodations();
    }

    @FXML
    private void handleActivitiesNav(MouseEvent mouseEvent) {
        navigateToActivities();
    }

    @FXML
    private void handleTransportNav(MouseEvent mouseEvent) {
        navigateToTransport();
    }

    @FXML
    private void handlePacksOffersNav(MouseEvent mouseEvent) {
        navigateToPacksOffers();
    }

    @FXML
    private void handleBlogNav(MouseEvent mouseEvent) {
        showAlert("Blog page coming soon!");
    }

    @FXML
    public void handleBrowseAccommodations(ActionEvent Event) {
        navigateToAccommodations();
    }

    // ========== FUTURE NAVIGATION METHODS (COMMENTED) ==========
/*


@FXML
private void handleBrowseOffers(ActionEvent event) {
    // Will navigate to offers page when implemented
    System.out.println("Browse offers clicked");
    showAlert("Offers page coming soon!");
}

@FXML
private void handleBrowseTransport(ActionEvent event) {
    // Will navigate to transport page when implemented
    System.out.println("Browse transport clicked");
    showAlert("Transport page coming soon!");
}

@FXML
private void handleBrowseBlog(ActionEvent event) {
    // Will navigate to blog page when implemented
    System.out.println("Browse blog clicked");
    showAlert("Blog page coming soon!");
}
*/
}