package tn.esprit.controllers.admin;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import tn.esprit.entities.Accommodation;
import tn.esprit.entities.Room;
import tn.esprit.services.AccommodationDashboardAnalyticsService;
import tn.esprit.services.AccommodationDashboardExportService;
import tn.esprit.services.AccommodationMlInsightsService;
import tn.esprit.services.AccommodationService;
import tn.esprit.services.RoomService;
import tn.esprit.utils.AccommodationCard;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Controller for Admin Dashboard - Accommodation Management
 * Handles CRUD operations, analytics, modal interactions, and sidebar navigation
 *
 * ✅ UPDATED: Complete with sidebar navigation, collapsible menus, and user controls
 */
public class AccommodationAdminController {

    // ============ FXML Components ============

    // Header & Navigation
    @FXML private Button addAccommodationBtn;

    // KPI Labels
    @FXML private Label totalAccommodationsLabel;
    @FXML private Label activeBookingsLabel;
    @FXML private Label revenueLabel;
    @FXML private Label avgBookingValueLabel;
    @FXML private Label cancellationRateLabel;
    @FXML private Label topCityLabel;
    @FXML private Label topTypeLabel;
    @FXML private Label insightSummaryLabel;
    @FXML private Label forecastOccupancyLabel;
    @FXML private Label suggestedPriceActionLabel;
    @FXML private Label modelConfidenceLabel;
    @FXML private Label mlDecisionSummaryLabel;

    // Charts
    @FXML private LineChart<String, Number> bookingsChart;
    @FXML private PieChart revenueChart;

    // Search & Filters
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> cityFilter;
    @FXML private ComboBox<String> sortBy;
    @FXML private ToggleButton gridViewToggle;
    @FXML private ToggleButton listViewToggle;
    @FXML private Label resultsLabel;

    // Accommodations Grid
    @FXML private FlowPane accommodationsGrid;

    // Modal Components
    @FXML private StackPane modalOverlay;
    @FXML private VBox accommodationModal;

    // ============ SIDEBAR COMPONENTS ============
    @FXML private VBox sidebar;
    @FXML private Button sidebarToggle;
    @FXML private Button sidebarOpenButton;
    @FXML private ToggleButton darkModeToggle;
    @FXML private ComboBox<String> languageSelector;

    // Menu Toggles
    @FXML private Button dashboardToggle;
    @FXML private Button usersToggle;
    @FXML private Button accommodationsToggle;
    @FXML private Button destinationsToggle;
    @FXML private Button offersToggle;
    @FXML private Button transportToggle;

    // Menu Panels
    @FXML private VBox dashboardMenu;
    @FXML private VBox usersMenu;
    @FXML private VBox accommodationsMenu;
    @FXML private VBox destinationsMenu;
    @FXML private VBox offersMenu;
    @FXML private VBox transportMenu;

    // Header Components
    @FXML private MenuButton profileDropdown;
    @FXML private Button notificationBtn;

    // ============ Services ============
    private AccommodationService accommodationService;
    private RoomService roomService;
    private AccommodationDashboardAnalyticsService dashboardAnalyticsService;
    private AccommodationMlInsightsService accommodationMlInsightsService;
    private AccommodationDashboardExportService dashboardExportService;

    // ============ State Variables ============
    private List<Accommodation> allAccommodations;
    private List<Accommodation> filteredAccommodations;
    private boolean isGridView = true;
    private boolean isSidebarCollapsed = false;

    // Logger
    private static final Logger logger = Logger.getLogger(AccommodationAdminController.class.getName());

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.log(Level.INFO, "===== Initializing Admin Dashboard =====");

        // Initialize services
        accommodationService = new AccommodationService();
        roomService = new RoomService();
        dashboardAnalyticsService = new AccommodationDashboardAnalyticsService();
        accommodationMlInsightsService = new AccommodationMlInsightsService();
        dashboardExportService = new AccommodationDashboardExportService(dashboardAnalyticsService, accommodationMlInsightsService);

        // Initialize UI components
        initializeUI();

        // Setup sidebar and navigation
        setupSidebarNavigation();

        // Load data
        loadDashboardData();
        loadCharts();
        loadAccommodations();

        // Setup search and filters
        setupFilters();

        // Setup modal overlay click handler
        setupModalOverlay();

        logger.log(Level.INFO, "Admin Dashboard initialized successfully");
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Setup view toggles
        setupViewToggles();

        // Initialize filter dropdowns
        initializeFilters();

        // Setup FlowPane for responsive layout
        accommodationsGrid.prefWrapLengthProperty().bind(
                accommodationsGrid.widthProperty().subtract(40)
        );

        // Set initial view
        accommodationsGrid.getChildren().clear();
    }

    /**
     * Setup sidebar navigation and controls
     */
    private void setupSidebarNavigation() {
        // Setup sidebar toggle
        setupSidebarToggle();

        // Setup menu toggles
        setupMenuToggles();

        // Setup theme toggle
        setupThemeToggle();

        // Setup language selector
        setupLanguageSelector();

        // Setup profile dropdown
        setupProfileDropdown();

        // Setup notification button
        setupNotificationButton();
    }

    /**
     * Setup sidebar collapse/expand toggle
     */
    private void setupSidebarToggle() {
        if (sidebarToggle != null) {
            // Collapse sidebar completely when the arrow is clicked
            sidebarToggle.setOnAction(event -> collapseSidebar());
        }

        // Header button (☰) reopens the sidebar when hidden
        if (sidebarOpenButton != null) {
            sidebarOpenButton.setOnAction(event -> expandSidebar());
            // Initially hidden because sidebar starts visible
            sidebarOpenButton.setVisible(false);
            sidebarOpenButton.setManaged(false);
        }
    }

    /**
     * Collapse sidebar completely and show the header open button
     */
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

    /**
     * Expand sidebar back to full width and hide the header open button
     */
    private void expandSidebar() {
        isSidebarCollapsed = false;
        if (sidebar != null) {
            // Prepare sidebar just off-screen to the left, then slide it in
            sidebar.setVisible(true);
            sidebar.setManaged(true);
            animateSidebarShow();
        }

        if (sidebarOpenButton != null) {
            sidebarOpenButton.setVisible(false);
            sidebarOpenButton.setManaged(false);
        }
    }

    /**
     * Helper: slide sidebar out and hide it
     */
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

    /**
     * Helper: show sidebar and slide it in
     */
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

        ParallelTransition showTransition = new ParallelTransition(slideIn, fadeIn);
        showTransition.play();
    }

    /**
     * Setup menu section toggles (expand/collapse)
     */
    private void setupMenuToggles() {
        // Dashboard menu toggle
        if (dashboardToggle != null && dashboardMenu != null) {
            dashboardToggle.setOnAction(event -> toggleMenu(dashboardMenu, dashboardToggle));
        }

        // Users menu toggle
        if (usersToggle != null && usersMenu != null) {
            usersToggle.setOnAction(event -> toggleMenu(usersMenu, usersToggle));
            // Initially collapse users menu
            usersMenu.setVisible(false);
            usersMenu.setManaged(false);
        }

        // Accommodations menu toggle (initially expanded)
        if (accommodationsToggle != null && accommodationsMenu != null) {
            accommodationsToggle.setOnAction(event -> toggleMenu(accommodationsMenu, accommodationsToggle));
            // Initially expanded
            accommodationsMenu.setVisible(true);
            accommodationsMenu.setManaged(true);
        }

        // Destinations menu toggle
        if (destinationsToggle != null && destinationsMenu != null) {
            destinationsToggle.setOnAction(event -> toggleMenu(destinationsMenu, destinationsToggle));
            // Initially collapse destinations menu
            destinationsMenu.setVisible(false);
            destinationsMenu.setManaged(false);
        }

        // Offers menu toggle
        if (offersToggle != null && offersMenu != null) {
            offersToggle.setOnAction(event -> toggleMenu(offersMenu, offersToggle));
            // Initially collapse offers menu
            offersMenu.setVisible(false);
            offersMenu.setManaged(false);
        }

        // Transport menu toggle
        if (transportToggle != null && transportMenu != null) {
            transportToggle.setOnAction(event -> toggleMenu(transportMenu, transportToggle));
            // Initially collapse transport menu
            transportMenu.setVisible(false);
            transportMenu.setManaged(false);
        }
    }

    /**
     * Toggle menu visibility
     */
    private void toggleMenu(VBox menu, Button toggleButton) {
        boolean isVisible = menu.isVisible();
        toggleButton.setText(isVisible ? "▶" : "▼");

        if (isVisible) {
            // Smooth collapse: fade + scale, then hide from layout
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
            // Smooth expand: show in layout, then fade + scale in
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

    /**
     * Setup dark/light mode toggle
     */
    private void setupThemeToggle() {
        if (darkModeToggle != null) {
            darkModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    enableDarkMode();
                } else {
                    enableLightMode();
                }
            });
        }
    }

    /**
     * Enable dark mode
     */
    private void enableDarkMode() {
        // Add dark mode CSS class to root
        StackPane root = (StackPane) sidebar.getScene().getRoot();
        root.getStyleClass().add("dark-mode");
    }

    /**
     * Enable light mode
     */
    private void enableLightMode() {
        // Remove dark mode CSS class from root
        StackPane root = (StackPane) sidebar.getScene().getRoot();
        root.getStyleClass().remove("dark-mode");
    }

    /**
     * Setup language selector
     */
    private void setupLanguageSelector() {
        if (languageSelector != null) {
            // Set default language
            languageSelector.setValue("English");

            // Handle language change
            languageSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    changeLanguage(newVal);
                }
            });
        }
    }

    /**
     * Change application language
     */
    private void changeLanguage(String language) {
        switch (language) {
            case "English":
                // TODO: Implement English localization
                break;
            case "Français":
                // TODO: Implement French localization
                break;
            case "العربية":
                // TODO: Implement Arabic localization
                break;
        }
    }

    /**
     * Setup profile dropdown
     */
    private void setupProfileDropdown() {
        if (profileDropdown != null) {
            // Get menu items
            var items = profileDropdown.getItems();

            // Setup My Profile action
            if (items.size() > 0) {
                items.get(0).setOnAction(event -> showUserProfile());
            }

            // Setup Settings action
            if (items.size() > 1) {
                items.get(1).setOnAction(event -> showSettings());
            }

            // Setup Logout action
            if (items.size() > 3) {
                items.get(3).setOnAction(event -> handleLogout());
            }
        }
    }

    /**
     * Show user profile modal
     */
    private void showUserProfile() {
        showToast("Opening user profile...", "info");
        // TODO: Implement user profile modal
    }

    /**
     * Show settings modal
     */
    private void showSettings() {
        showToast("Opening settings...", "info");
        // TODO: Implement settings modal
    }

    /**
     * Handle logout
     */
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be redirected to the login page.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showToast("Logging out...", "info");
                // TODO: Implement logout logic and redirect to login
            }
        });
    }

    /**
     * Setup notification button
     */
    private void setupNotificationButton() {
        if (notificationBtn != null) {
            notificationBtn.setOnAction(event -> showNotifications());
        }
    }

    /**
     * Show notifications panel
     */
    private void showNotifications() {
        showToast("Showing notifications...", "info");
        // TODO: Implement notifications panel
    }

    /**
     * Show toast notification
     */
    private void showToast(String message, String type) {
        // Create toast notification
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

        // Position toast
        StackPane root = (StackPane) sidebar.getScene().getRoot();
        StackPane.setAlignment(toast, javafx.geometry.Pos.TOP_CENTER);
        toast.setTranslateY(-40);
        root.getChildren().add(toast);

        // Animate toast
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
        slideIn.setFromY(-40);
        slideIn.setToY(20);

        ParallelTransition show = new ParallelTransition(fadeIn, slideIn);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), toast);
        slideOut.setFromY(20);
        slideOut.setToY(-50);

        ParallelTransition hide = new ParallelTransition(fadeOut, slideOut);
        hide.setOnFinished(e -> root.getChildren().remove(toast));

        SequentialTransition sequence = new SequentialTransition(show, pause, hide);
        sequence.play();
    }

    /**
     * Initialize filter dropdowns with data
     */
    private void initializeFilters() {
        // Get unique cities and types from database
        try {
            List<String> cities = accommodationService.getAllCities();
            List<String> types = accommodationService.getAllTypes();

            // Add "All" options
            cities.add(0, "All Cities");
            types.add(0, "All Types");

            // Set cities to combo box
            cityFilter.getItems().setAll(cities);

            // Status filter options
            if (statusFilter.getItems().isEmpty()) {
                statusFilter.getItems().addAll("All Status", "Active", "Inactive", "Pending", "Under Maintenance");
                statusFilter.setValue("All Status");
            }

            // Type filter options
            if (typeFilter.getItems().isEmpty()) {
                typeFilter.getItems().addAll("All Types", "Hotel", "Apartment", "Villa", "Resort",
                        "Hostel", "Guesthouse", "Boutique Hotel", "Motel", "Bed & Breakfast");
                typeFilter.setValue("All Types");
            }

            // Sort options - REMOVED price options, added star options
            if (sortBy.getItems().isEmpty()) {
                sortBy.getItems().addAll(
                        "Name (A-Z)",
                        "Name (Z-A)",
                        "Stars (High-Low)",
                        "Stars (Low-High)",
                        "Newest First",
                        "Oldest First"
                );
                sortBy.setValue("Name (A-Z)");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading filter data: " + e.getMessage(), e);
        }
    }

    /**
     * Setup modal overlay to close when clicking outside
     */
    private void setupModalOverlay() {
        if (modalOverlay != null) {
            modalOverlay.setOnMouseClicked(event -> {
                // Only close if clicking directly on the overlay, not the modal content
                if (event.getTarget() == modalOverlay) {
                    closeModal();
                }
            });
        }
    }

    /**
     * Setup view toggle buttons
     */
    private void setupViewToggles() {
        ToggleGroup viewGroup = new ToggleGroup();
        gridViewToggle.setToggleGroup(viewGroup);
        listViewToggle.setToggleGroup(viewGroup);

        gridViewToggle.setSelected(true);

        gridViewToggle.selectedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                isGridView = true;
                refreshAccommodationsView();
            }
        });

        listViewToggle.selectedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                isGridView = false;
                refreshAccommodationsView();
            }
        });
    }

    /**
     * Load dashboard KPIs and statistics
     */
    private void loadDashboardData() {
        try {
            AccommodationDashboardAnalyticsService.DashboardKpis kpis = dashboardAnalyticsService.getDashboardKpis();
            totalAccommodationsLabel.setText(String.valueOf(kpis.totalAccommodations));
            activeBookingsLabel.setText(formatInteger(kpis.activeBookings));
            revenueLabel.setText("€" + formatCurrency(kpis.confirmedRevenue));
            avgBookingValueLabel.setText("€" + formatCurrency(kpis.averageBookingValue));
            cancellationRateLabel.setText(formatPercent(kpis.cancellationRatePercent));
            topCityLabel.setText(safeText(kpis.topCity));
            topTypeLabel.setText(safeText(kpis.topType));
            insightSummaryLabel.setText(String.join("  |  ", dashboardAnalyticsService.getInsightHighlights()));

            AccommodationMlInsightsService.MlInsightSnapshot mlSnapshot = accommodationMlInsightsService.computeGlobalSnapshot();
            forecastOccupancyLabel.setText(formatPercent(mlSnapshot.forecastOccupancyPercent));
            suggestedPriceActionLabel.setText(formatSignedPercent(mlSnapshot.suggestedPriceAdjustmentPercent));
            modelConfidenceLabel.setText(formatPercent(mlSnapshot.modelConfidencePercent));
            mlDecisionSummaryLabel.setText(safeText(mlSnapshot.decisionSummary));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading dashboard data: " + e.getMessage(), e);
            // Set default values
            totalAccommodationsLabel.setText("0");
            activeBookingsLabel.setText("0");
            revenueLabel.setText("€0");
            avgBookingValueLabel.setText("€0");
            cancellationRateLabel.setText("0%");
            topCityLabel.setText("N/A");
            topTypeLabel.setText("N/A");
            insightSummaryLabel.setText("Analytics insights will appear once booking data is available.");
            forecastOccupancyLabel.setText("0%");
            suggestedPriceActionLabel.setText("0.0%");
            modelConfidenceLabel.setText("0%");
            mlDecisionSummaryLabel.setText("ML baseline will appear once enough booking history is available.");
        }
    }

    /**
     * Load and populate charts
     */
    private void loadCharts() {
        try {
            bookingsChart.getData().clear();
            revenueChart.getData().clear();

            // Bookings Chart
            XYChart.Series<String, Number> bookingsSeries = new XYChart.Series<>();
            bookingsSeries.setName("Bookings");

            dashboardAnalyticsService.getBookingsTrendLast6Months()
                    .forEach((month, count) -> bookingsSeries.getData().add(new XYChart.Data<>(month, count)));

            bookingsChart.getData().add(bookingsSeries);

            // Revenue Chart
            for (AccommodationDashboardAnalyticsService.RevenueByType row : dashboardAnalyticsService.getRevenueByAccommodationType()) {
                revenueChart.getData().add(new PieChart.Data(row.type, row.revenue));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading charts: " + e.getMessage(), e);
        }
    }

    /**
     * Load accommodations from database
     */
    private void loadAccommodations() {
        logger.log(Level.INFO, "Loading accommodations from database...");
        try {
            allAccommodations = accommodationService.getAll();
            logger.log(Level.INFO, "Loaded " + allAccommodations.size() + " accommodations");

            // Apply any existing filters
            applyFilters();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading accommodations: " + e.getMessage(), e);
            allAccommodations = new ArrayList<>();
            filteredAccommodations = new ArrayList<>();
            updateResultsLabel();
        }
    }

    /**
     * Refresh the accommodations view (grid or list)
     * ✅ UPDATED: No price display
     */
    private void refreshAccommodationsView() {
        logger.log(Level.INFO, "Refreshing view (isGridView=" + isGridView + ")...");

        // Clear existing content
        accommodationsGrid.getChildren().clear();

        // Check if there are accommodations to display
        if (filteredAccommodations == null || filteredAccommodations.isEmpty()) {
            showEmptyState();
            return;
        }

        logger.log(Level.INFO, "Creating cards for " + filteredAccommodations.size() + " accommodation(s)");

        for (int i = 0; i < filteredAccommodations.size(); i++) {
            Accommodation accommodation = filteredAccommodations.get(i);
            logger.log(Level.FINE, "  [" + (i+1) + "] Creating card for: " + accommodation.getName());

            try {
                // Create a container for the card
                VBox cardContainer = new VBox(0); // No spacing - buttons are part of card
                cardContainer.getStyleClass().add("card-wrapper");

                // ⭐ Create basic card without price
                AccommodationCard accommodationCard = new AccommodationCard(accommodation, 200);
                logger.log(Level.FINE, "    ✅ Created card for " + accommodation.getName());

                // ⭐ Add action buttons directly to the card
                accommodationCard.addActionButtons(
                        () -> showAccommodationDetailsModal(accommodation),  // View
                        () -> showEditAccommodationModal(accommodation),     // Edit
                        () -> handleDeleteAccommodation(accommodation)       // Delete
                );

                // Set appropriate width based on view mode
                if (isGridView) {
                    // Grid view: fixed width
                    cardContainer.setPrefWidth(350);
                    cardContainer.setMinWidth(350);
                    cardContainer.setMaxWidth(350);
                } else {
                    // List view: responsive width
                    cardContainer.setPrefWidth(1200);
                    cardContainer.setMinWidth(1200);
                    cardContainer.setMaxWidth(1200);
                }

                // Add card to container
                cardContainer.getChildren().add(accommodationCard);

                // Add container to grid
                accommodationsGrid.getChildren().add(cardContainer);
                logger.log(Level.FINE, "    Card added successfully");

            } catch (Exception e) {
                logger.log(Level.SEVERE, "    Error creating card for " + accommodation.getName() + ": " + e.getMessage(), e);
                // Add a basic card as fallback
                accommodationsGrid.getChildren().add(createFallbackCard(accommodation));
            }
        }

        logger.log(Level.INFO, "View refresh complete. Total cards: " + accommodationsGrid.getChildren().size());
    }

    /**
     * Show empty state when no accommodations
     */
    private void showEmptyState() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
        emptyBox.setPadding(new javafx.geometry.Insets(50));
        emptyBox.setPrefWidth(accommodationsGrid.getWidth() - 40);
        emptyBox.getStyleClass().add("empty-state-container");

        Label emptyIcon = new Label("🏨");
        emptyIcon.setStyle("-fx-font-size: 64px;");

        Label emptyLabel = new Label("No accommodations found");
        emptyLabel.setStyle("-fx-text-fill: #37474F; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label emptySubLabel = new Label("Try adjusting your filters or add a new accommodation");
        emptySubLabel.setStyle("-fx-text-fill: #90A4AE; -fx-font-size: 14px;");

        Button addButton = new Button("➕ Add Accommodation");
        addButton.getStyleClass().addAll("button", "btn-add-primary");
        addButton.setOnAction(e -> showAddAccommodationModal());

        emptyBox.getChildren().addAll(emptyIcon, emptyLabel, emptySubLabel, addButton);
        accommodationsGrid.getChildren().add(emptyBox);
    }

    /**
     * Create fallback card if custom card fails
     */
    private VBox createFallbackCard(Accommodation accommodation) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #E8EAF6; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-padding: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        if (isGridView) {
            card.setPrefWidth(350);
            card.setMaxWidth(350);
            card.setPrefHeight(200);
        } else {
            card.setPrefWidth(1200);
            card.setMaxWidth(1200);
            card.setPrefHeight(150);
        }

        // Basic content
        Label nameLabel = new Label(accommodation.getName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #37474F;");
        nameLabel.setWrapText(true);

        Label locationLabel = new Label("📍 " + accommodation.getCity() + ", " + accommodation.getCountry());
        locationLabel.setStyle("-fx-text-fill: #78909C; -fx-font-size: 14px;");

        Label typeLabel = new Label("🏠 " + accommodation.getType());
        typeLabel.setStyle("-fx-text-fill: #90CAF9; -fx-font-size: 14px; -fx-font-weight: 600;");

        // Status indicator
        String statusColor = accommodation.getStatus().equals("Active") ? "#C8E6C9" : "#FFCCBC";
        String statusText = accommodation.getStatus().equals("Active") ? "#2E7D32" : "#D84315";
        Label statusLabel = new Label(accommodation.getStatus());
        statusLabel.setStyle(
                "-fx-background-color: " + statusColor + "; " +
                        "-fx-text-fill: " + statusText + "; " +
                        "-fx-padding: 6 14; " +
                        "-fx-background-radius: 12; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: 600;"
        );

        card.getChildren().addAll(nameLabel, locationLabel, typeLabel, statusLabel);

        return card;
    }

    /**
     * Setup filters and search functionality
     */
    private void setupFilters() {
        // Search field listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Filter combo boxes listeners
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        cityFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        sortBy.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    /**
     * Apply filters and sort to accommodations list
     */
    private void applyFilters() {
        if (allAccommodations == null) {
            filteredAccommodations = new ArrayList<>();
            updateResultsLabel();
            return;
        }

        logger.log(Level.FINE, "Applying filters...");

        // Filter accommodations
        filteredAccommodations = allAccommodations.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesStatus)
                .filter(this::matchesType)
                .filter(this::matchesCity)
                .collect(java.util.stream.Collectors.toList());

        // Apply sorting
        applySorting();

        // Update UI
        updateResultsLabel();
        refreshAccommodationsView();

        logger.log(Level.INFO, "Filters applied. Showing " + filteredAccommodations.size() + " accommodations");
    }

    /**
     * Apply sorting to filtered list - UPDATED: No price sorting, stars instead of rating
     */
    private void applySorting() {
        if (sortBy.getValue() == null || filteredAccommodations == null) return;

        String sortOption = sortBy.getValue();

        switch (sortOption) {
            case "Name (A-Z)":
                filteredAccommodations.sort((a, b) ->
                        a.getName().compareToIgnoreCase(b.getName()));
                break;
            case "Name (Z-A)":
                filteredAccommodations.sort((a, b) ->
                        b.getName().compareToIgnoreCase(a.getName()));
                break;
            case "Stars (High-Low)":
                filteredAccommodations.sort((a, b) ->
                        Integer.compare(b.getStars(), a.getStars()));
                break;
            case "Stars (Low-High)":
                filteredAccommodations.sort((a, b) ->
                        Integer.compare(a.getStars(), b.getStars()));
                break;
            case "Newest First":
                filteredAccommodations.sort((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                break;
            case "Oldest First":
                filteredAccommodations.sort((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                });
                break;
        }
    }

    private boolean matchesSearch(Accommodation a) {
        if (searchField.getText() == null || searchField.getText().isEmpty()) return true;
        String search = searchField.getText().toLowerCase();
        return (a.getName() != null && a.getName().toLowerCase().contains(search)) ||
                (a.getCity() != null && a.getCity().toLowerCase().contains(search)) ||
                (a.getDescription() != null && a.getDescription().toLowerCase().contains(search));
    }

    private boolean matchesStatus(Accommodation a) {
        if (statusFilter.getValue() == null || statusFilter.getValue().equals("All Status")) return true;
        return a.getStatus() != null && a.getStatus().equals(statusFilter.getValue());
    }

    private boolean matchesType(Accommodation a) {
        if (typeFilter.getValue() == null || typeFilter.getValue().equals("All Types")) return true;
        return a.getType() != null && a.getType().equals(typeFilter.getValue());
    }

    private boolean matchesCity(Accommodation a) {
        if (cityFilter.getValue() == null || cityFilter.getValue().isEmpty() || cityFilter.getValue().equals("All Cities")) return true;
        return a.getCity() != null && a.getCity().equals(cityFilter.getValue());
    }

    /**
     * Update results label
     */
    private void updateResultsLabel() {
        int count = filteredAccommodations != null ? filteredAccommodations.size() : 0;
        resultsLabel.setText("Showing " + count + " accommodation" + (count != 1 ? "s" : ""));
    }

    /**
     * Show Add Accommodation Modal
     */
    @FXML
    private void showAddAccommodationModal() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/accommodation-modal.fxml")
            );
            VBox modalContent = loader.load();

            // Get controller and set mode to ADD
            AccommodationModalController modalController = loader.getController();
            modalController.setParentController(this);
            modalController.setMode("ADD");

            showModal(modalContent);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading modal: " + e.getMessage(), e);
            showError("Error loading modal: " + e.getMessage());
        }
    }

    /**
     * Show Edit Accommodation Modal
     */
    public void showEditAccommodationModal(Accommodation accommodation) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/accommodation-modal.fxml")
            );
            VBox modalContent = loader.load();

            // Get controller and set mode to EDIT
            AccommodationModalController modalController = loader.getController();
            modalController.setParentController(this);
            modalController.setMode("EDIT");
            modalController.loadAccommodation(accommodation);

            showModal(modalContent);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading modal: " + e.getMessage(), e);
            showError("Error loading modal: " + e.getMessage());
        }
    }

    /**
     * Show Accommodation Details Modal
     */
    private void showAccommodationDetailsModal(Accommodation accommodation) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/accommodation-details-modal.fxml")
            );
            VBox modalContent = loader.load();

            // Get controller and set accommodation
            AccommodationDetailsController detailsController = loader.getController();
            detailsController.setAccommodation(accommodation);
            detailsController.setParentController(this);

            showModal(modalContent);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading details modal: " + e.getMessage(), e);
            showError("Error loading accommodation details: " + e.getMessage());
        }
    }

    /**
     * Public wrapper used by nested pages/controllers to open accommodation details.
     */
    public void showAccommodationDetailsPage(Accommodation accommodation) {
        showAccommodationDetailsModal(accommodation);
    }

    /**
     * Open dedicated Room Details page (inside admin overlay shell).
     */
    public void showRoomDetailsPage(Accommodation accommodation, Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/room-details-page.fxml")
            );
            VBox pageContent = loader.load();

            RoomDetailsController controller = loader.getController();
            controller.setParentController(this);
            controller.setContext(accommodation, room);

            showModal(pageContent);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading room details page: " + e.getMessage(), e);
            showError("Error loading room details: " + e.getMessage());
        }
    }

    /**
     * Show modal with fade-in animation
     */
    private void showModal(VBox content) {
        // Clear previous content and add new
        accommodationModal.getChildren().clear();
        accommodationModal.getChildren().add(content);

        // Make overlay visible and bring to front
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
        modalOverlay.toFront();

        // Reset opacity for animation
        modalOverlay.setOpacity(0);
        accommodationModal.setScaleX(0.8);
        accommodationModal.setScaleY(0.8);

        // Fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), modalOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), accommodationModal);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        ParallelTransition parallel = new ParallelTransition(fadeIn, scaleIn);
        parallel.play();
    }

    /**
     * Close modal (called from modal controller)
     */
    public void closeModal() {
        if (modalOverlay == null || !modalOverlay.isVisible()) {
            return;
        }

        // Fade-out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), modalOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), accommodationModal);
        scaleOut.setFromX(1.0);
        scaleOut.setFromY(1.0);
        scaleOut.setToX(0.8);
        scaleOut.setToY(0.8);

        ParallelTransition parallel = new ParallelTransition(fadeOut, scaleOut);

        parallel.setOnFinished(e -> {
            modalOverlay.setVisible(false);
            modalOverlay.setManaged(false);
            accommodationModal.getChildren().clear();
        });

        parallel.play();
    }

    /**
     * Refresh accommodations list after CRUD operation
     */
    public void refreshAfterSave() {
        logger.log(Level.INFO, "Refreshing after save...");
        loadAccommodations();
        loadDashboardData();
        loadCharts();
        closeModal();
        showSuccess("Accommodation saved successfully!");
    }

    /**
     * Handle delete accommodation
     */
    private void handleDeleteAccommodation(Accommodation accommodation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Accommodation");
        alert.setHeaderText("Are you sure you want to delete this accommodation?");
        alert.setContentText(accommodation.getName());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    accommodationService.deleteAccommodation(accommodation.getId());
                    showSuccess("Accommodation deleted successfully!");
                    loadAccommodations(); // Reload the list
                    loadDashboardData();
                    loadCharts();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error deleting accommodation: " + e.getMessage(), e);
                    showError("Error deleting accommodation: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Handle reset filters
     */
    @FXML
    private void handleResetFilters() {
        searchField.clear();
        statusFilter.setValue("All Status");
        typeFilter.setValue("All Types");
        cityFilter.setValue("All Cities");
        sortBy.setValue(null);
        applyFilters();
    }

    @FXML
    private void handleExportDashboardStats() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Accommodation Dashboard Statistics");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
            chooser.setInitialFileName("accommodation-dashboard-stats.xlsx");

            Window owner = addAccommodationBtn != null && addAccommodationBtn.getScene() != null
                    ? addAccommodationBtn.getScene().getWindow()
                    : null;
            File destination = chooser.showSaveDialog(owner);
            if (destination == null) {
                return;
            }

            dashboardExportService.exportDashboardExcel(destination.toPath());
            showSuccess("Dashboard statistics exported successfully:\n" + destination.getAbsolutePath());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error exporting dashboard statistics: " + e.getMessage(), e);
            showError("Unable to export dashboard statistics: " + e.getMessage());
        }
    }

    /**
     * Navigation method for accommodations menu item
     */
    @FXML
    private void navigateToAccommodations() {
        // This is already the accommodations page
        // You can add any specific initialization here if needed
        showToast("Already on Accommodations page", "info");
    }

    /**
     * Open dedicated Accommodation Booking management page.
     */
    @FXML
    private void showAdminAccommodationBookingPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/accommodation-booking-management.fxml")
            );
            Parent root = loader.load();
            if (sidebar != null && sidebar.getScene() != null) {
                sidebar.getScene().setRoot(root);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading booking management page: " + e.getMessage(), e);
            showError("Error loading booking management page: " + e.getMessage());
        }
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatInteger(int value) {
        return String.format("%,d", value);
    }

    private String formatCurrency(double value) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(value);
    }

    private String formatPercent(double value) {
        DecimalFormat df = new DecimalFormat("0.0");
        return df.format(value) + "%";
    }

    private String formatSignedPercent(double value) {
        DecimalFormat df = new DecimalFormat("0.0");
        return (value >= 0 ? "+" : "") + df.format(value) + "%";
    }

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }
}