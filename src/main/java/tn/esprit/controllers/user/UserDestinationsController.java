package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Destination;
import tn.esprit.entities.User;
import tn.esprit.services.*;
import tn.esprit.utils.ThemeManager;
import tn.esprit.utils.ImageHelper;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserDestinationsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<Destination.DestinationType> typeFilter;
    @FXML private ComboBox<String> countryFilter;
    @FXML private Button searchBtn;
    @FXML private Button resetBtn;

    @FXML private Button themeBtn; // Added missing button

    @FXML private FlowPane destinationsContainer;
    @FXML private Label resultsCountLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label avatarInitials;
    @FXML private Label userNameLabel;
    @FXML private ImageView userAvatarView;

    // Added missing fields
    private DestinationService destinationService;
    private ActivityService activityService;
    private WeatherServiceUser weatherService;  // FIXED: Changed to WeatherServiceUser
    private CountryService countryService;
    private ObservableList<Destination> destinations;
    private List<Destination> allDestinations;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        destinationService = new DestinationService();
        activityService = new ActivityService();
        weatherService = new WeatherServiceUser();  // FIXED: Changed to WeatherServiceUser
        countryService = new CountryService();
        destinations = FXCollections.observableArrayList();

        setupFilters();
        setupActions();
        
        // Load data in background to prevent black screen/freeze
        javafx.application.Platform.runLater(this::loadDestinations);

        // Defer theme apply
        javafx.application.Platform.runLater(() -> {
            if (themeBtn != null && themeBtn.getScene() != null) {
                ThemeManager.applyTheme(themeBtn.getScene());
                updateThemeButtonText();
            }
        });
    }

    // Add this field with the other fields
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null && this.currentUser.getUserId() > 0) {
            tn.esprit.utils.SessionManager.setCurrentUserId(this.currentUser.getUserId());
        }
        // Fallback: if user is null, try to recover from SessionManager
        if (this.currentUser == null) {
            int userId = tn.esprit.utils.SessionManager.getCurrentUserId();
            if (userId > 0) {
                tn.esprit.services.UserService us = new tn.esprit.services.UserService();
                this.currentUser = us.findById(userId);
            }
        }
        if (this.currentUser != null) {
            if (userNameLabel != null) userNameLabel.setText(this.currentUser.getFirstName() + " " + this.currentUser.getLastName());
            if (avatarInitials != null) {
                String initials = (this.currentUser.getFirstName().substring(0, 1) + this.currentUser.getLastName().substring(0, 1)).toUpperCase();
                avatarInitials.setText(initials);
            }
            applyTopBarAvatar();
        }
        System.out.println("✅ User set in DestinationsController: " +
                (this.currentUser != null ? this.currentUser.getEmail() : "null"));
    }

    @FXML
    private void handleHomeNav(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/home.fxml"));
            Parent root = loader.load();
            HomeController controller = loader.getController();
            controller.setUser(currentUser);
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load home view.");
        }
    }

    @FXML
    private void handleDestinationsNav(javafx.scene.input.MouseEvent event) {
        // Already here
    }

    @FXML
    private void handleAccommodationsNav(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/AccommodationsView.fxml"));
            Parent root = loader.load();
            AccommodationsController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load accommodations view.");
        }
    }

    @FXML
    private void handleActivitiesNav(javafx.scene.input.MouseEvent event) {
        navigateToActivities();
    }

    private void navigateToActivities() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/user_activities.fxml"));
            Parent root = loader.load();
            UserActivitiesController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load activities view.");
        }
    }

    @FXML
    private void handleTransportNav(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/TransportUserInterface.fxml"));
            Parent root = loader.load();
            TransportUserInterfaceController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load transport view.");
        }
    }
    @FXML

    private void handlePacksOffersNav(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/UserPacksOffersView.fxml"));
            Parent root = loader.load();
            UserPacksOffersController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load Packs & Offers view.");
        }
    }

    @FXML
    private void handleBlogNav(javafx.scene.input.MouseEvent event) {
        showError("Blog page coming soon!");
    }

    @FXML
    private void handleProfile(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/profile.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.setUser(currentUser);
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load profile view.");
        }
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user/login.fxml"));
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not perform logout.");
        }
    }

    @FXML
    private void handleMyBookings(javafx.event.ActionEvent event) {
        navigateToMyBookings();
    }

    private void updateThemeButtonText() {
        if (themeBtn != null) {
            themeBtn.setText(ThemeManager.isDarkMode() ? "☀" : "🌙");
            themeBtn.getStyleClass().removeAll("action-button", "action-button-secondary");
            if (!themeBtn.getStyleClass().contains("theme-toggle-btn")) {
                themeBtn.getStyleClass().add("theme-toggle-btn");
            }
        }
    }

    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList(Destination.DestinationType.values()));
        typeFilter.setPromptText("All Types");
        countryFilter.setPromptText("All Countries");
    }

    @FXML private Button myBookingsBtn;

    @FXML private Button myReviewsBtn; // New button binding
    @FXML private Button showMapBtn;
    @FXML private Button recommendBtn;

    private void setupActions() {
        searchBtn.setOnAction(e -> filterDestinations());
        resetBtn.setOnAction(e -> resetFilters());


        if (myBookingsBtn != null) {
            myBookingsBtn.setOnAction(e -> navigateToMyBookings());
        }


        if (myReviewsBtn != null) {
            myReviewsBtn.setOnAction(e -> showMyReviews());
        }

        if (showMapBtn != null) {
            showMapBtn.setOnAction(e -> showMap());
        }

        if (recommendBtn != null) {
            recommendBtn.setOnAction(e -> showRecommendations());
        }

        if (themeBtn != null) {
            themeBtn.setOnAction(e -> {
                ThemeManager.toggleTheme(themeBtn.getScene());
                updateThemeButtonText();
            });
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() >= 2 || newVal.isEmpty()) {
                filterDestinations();
            }
        });

        typeFilter.setOnAction(e -> filterDestinations());
        countryFilter.setOnAction(e -> filterDestinations());
    }

    private void loadDestinations() {
        loadingIndicator.setVisible(true);
        destinationsContainer.getChildren().clear();

        try {
            allDestinations = destinationService.getAllDestinations();
            destinations.setAll(allDestinations);
            populateCountryFilter();
            displayDestinations(destinations);
            resultsCountLabel.setText("Found " + destinations.size() + " destinations");
        } catch (Exception e) {
            showError("Error loading destinations: " + e.getMessage());
            e.printStackTrace();
        } finally {
            loadingIndicator.setVisible(false);
        }
    }

    private void populateCountryFilter() {
        ObservableList<String> countries = FXCollections.observableArrayList();
        countries.add("All Countries");
        for (Destination d : allDestinations) {
            if (!countries.contains(d.getCountry())) {
                countries.add(d.getCountry());
            }
        }
        countryFilter.setItems(countries);
        countryFilter.setValue("All Countries");
    }

    private void filterDestinations() {
        loadingIndicator.setVisible(true);
        destinationsContainer.getChildren().clear();

        try {
            String keyword = searchField.getText().trim().toLowerCase();
            Destination.DestinationType selectedType = typeFilter.getValue();
            String selectedCountry = countryFilter.getValue();

            List<Destination> filtered = allDestinations.stream()
                    .filter(d -> {
                        if (!keyword.isEmpty()) {
                            return d.getName().toLowerCase().contains(keyword) ||
                                    d.getCountry().toLowerCase().contains(keyword) ||
                                    (d.getCity() != null && d.getCity().toLowerCase().contains(keyword));
                        }
                        return true;
                    })
                    .filter(d -> selectedType == null || d.getType() == selectedType)
                    .filter(d -> selectedCountry == null || selectedCountry.equals("All Countries") || d.getCountry().equals(selectedCountry))
                    .toList();

            destinations.setAll(filtered);
            displayDestinations(destinations);
            resultsCountLabel.setText("Found " + destinations.size() + " destinations");
        } catch (Exception e) {
            showError("Error filtering destinations: " + e.getMessage());
            e.printStackTrace();
        } finally {
            loadingIndicator.setVisible(false);
        }
    }

    private void resetFilters() {
        searchField.clear();
        typeFilter.setValue(null);
        countryFilter.setValue("All Countries");
        filterDestinations();
    }

    private void displayDestinations(ObservableList<Destination> destinations) {
        destinationsContainer.getChildren().clear();
        for (Destination d : destinations) {
            try {
                destinationsContainer.getChildren().add(createDestinationCard(d));
            } catch (Exception e) {
                System.err.println("❌ Error creating card for " + d.getName() + ": " + e.getMessage());
            }
        }
        if (destinations.isEmpty()) {
            Label noResults = new Label("No destinations found matching your criteria.");
            noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 50;");
            destinationsContainer.getChildren().add(noResults);
        }
    }

    private VBox createDestinationCard(Destination destination) {
        VBox card = new VBox(10);
        boolean dark = ThemeManager.isDarkMode();
        String cardBg = dark ? "#2a2a3d" : "white";
        String shadow = dark ? "rgba(0,0,0,0.5)" : "rgba(0,0,0,0.1)";
        String cardStyle = "-fx-background-color: " + cardBg + "; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, " + shadow + ", 10, 0, 0, 2); " +
                "-fx-padding: 15; -fx-min-width: 280; -fx-max-width: 280; -fx-cursor: hand;";
        card.setStyle(cardStyle);

        String hoverShadow = dark ? "rgba(124,58,237,0.4)" : "rgba(102,126,234,0.4)";
        card.setOnMouseEntered(e -> card.setStyle(cardStyle + "-fx-effect: dropshadow(gaussian, " + hoverShadow + ", 18, 0, 0, 5); -fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
        card.setOnMouseExited(e -> card.setStyle(cardStyle));

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(150);
        imageContainer.setPrefWidth(250);

        Image image = ImageHelper.loadImage("destinations", destination.getName());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(150);
            imageView.setFitWidth(250);
            imageView.setPreserveRatio(false); // Fill the container
            // Use clipping for rounded corners on image
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(250, 150);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imageView.setClip(clip);
            imageContainer.getChildren().add(imageView);
        } else {
            imageContainer.setStyle(getBackgroundColorForType(destination.getType()));
            Label typeIcon = new Label(getIconForType(destination.getType()));
            typeIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
            imageContainer.getChildren().add(typeIcon);
        }

        String textPrimary = dark ? "#e0e0e0" : "#2c3e50";
        String textSecondary = dark ? "#a0a0b8" : "#7f8c8d";
        String textTertiary = dark ? "#c4c4dc" : "#34495e";

        Label nameLabel = new Label(destination.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");
        nameLabel.setWrapText(true);

        Label locationLabel = new Label("📍 " + destination.getFullLocation());
        locationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + textSecondary + ";");

        HBox infoBox = new HBox(10);
        infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label typeLabel = new Label(getTypeDisplayName(destination.getType()));
        String typeBg = dark ? "#312e81" : "#e3f2fd";
        String typeColor = dark ? "#a5b4fc" : "#1976d2";
        typeLabel.setStyle("-fx-background-color: " + typeBg + "; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + typeColor + ";");
        Label ratingLabel = new Label("⭐ " + String.format("%.1f", destination.getAverageRating()));
        ratingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
        infoBox.getChildren().addAll(typeLabel, ratingLabel);

        Label seasonLabel = new Label("📅 Best: " + getSeasonDisplayName(destination.getBestSeason()));
        seasonLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textTertiary + ";");

        // Weather badge (fetched async)
        String weatherBg = dark ? "#1a2744" : "#e8f4fd";
        String weatherColor = dark ? "#60a5fa" : "#2980b9";
        Label weatherBadge = new Label("🌤️ Loading...");
        weatherBadge.setStyle("-fx-background-color: " + weatherBg + "; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 12px; -fx-text-fill: " + weatherColor + "; -fx-font-weight: bold;");

        // Fetch weather asynchronously using WeatherServiceUser
        new Thread(() -> {
            WeatherServiceUser.WeatherInfo weather = weatherService.getWeatherForCity(
                    destination.getCity() != null ? destination.getCity() : destination.getName()
            );
            javafx.application.Platform.runLater(() -> {
                if (weather != null) {
                    weatherBadge.setText(weather.getWeatherEmoji() + " " + String.format("%.1f°C", weather.getTemperature()));
                    Tooltip tip = new Tooltip(weather.getCondition() + ", feels like " + String.format("%.1f°C", weather.getFeelsLike()));
                    weatherBadge.setTooltip(tip);
                } else {
                    weatherBadge.setText("");
                    weatherBadge.setVisible(false);
                }
            });
        }).start();

        // Country flag badge
        Label flagBadge = new Label("");
        flagBadge.setStyle("-fx-font-size: 16px;");
        new Thread(() -> {
            CountryService.CountryData countryData = countryService.getCountryByName(destination.getCountry());
            javafx.application.Platform.runLater(() -> {
                if (countryData != null && countryData.flagEmoji != null) {
                    flagBadge.setText(countryData.flagEmoji);
                }
            });
        }).start();

        HBox weatherAndFlagBox = new HBox(8, flagBadge, weatherBadge);
        weatherAndFlagBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String desc = destination.getDescription();
        if (desc != null && desc.length() > 100) desc = desc.substring(0, 97) + "...";
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textTertiary + "; -fx-line-spacing: 2;");

        Button bookBtn = new Button("Book Now");
        bookBtn.setStyle("-fx-background-color: linear-gradient(to right, #27ae60, #2ecc71); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15; -fx-cursor: hand; -fx-font-size: 14px;");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> handleBooking(destination));

        card.getChildren().addAll(imageContainer, nameLabel, locationLabel, infoBox, seasonLabel, weatherAndFlagBox, descLabel, bookBtn);
        card.setOnMouseClicked(e -> showDestinationDetails(destination));

        return card;
    }

    private void showDestinationDetails(Destination destination) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(destination.getName());
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/user-destinations.css").toExternalForm());
        if (ThemeManager.isDarkMode()) {
            dialogPane.getStylesheets().add(getClass().getResource("/css/dark-mode.css").toExternalForm());
        }
        dialogPane.getStyleClass().add("details-dialog");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        boolean dark = ThemeManager.isDarkMode();
        String textPrimary = dark ? "#e0e0e0" : "#2c3e50";
        String textSecondary = dark ? "#a0a0b8" : "#7f8c8d";
        String textTertiary = dark ? "#c4c4dc" : "#555";
        String sectionStyle = "-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 5 0; -fx-text-fill: " + textPrimary + ";";

        Label nameLabel = new Label(destination.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        String gridLabelStyle = "-fx-text-fill: " + textTertiary + "; -fx-font-weight: bold;";
        String gridValueStyle = "-fx-text-fill: " + textPrimary + ";";
        int row = 0;

        grid.addRow(row++, new Label("Type:"), new Label(getTypeDisplayName(destination.getType())));
        grid.addRow(row++, new Label("Country:"), new Label(destination.getCountry()));

        if (destination.getCity() != null) {
            Label cityKey = new Label("City:"); Label cityVal = new Label(destination.getCity());
            cityKey.setStyle(gridLabelStyle); cityVal.setStyle(gridValueStyle);
            grid.addRow(row++, cityKey, cityVal);
        }

        Label seasonKey = new Label("Season:");
        Label seasonVal = new Label(getSeasonDisplayName(destination.getBestSeason()));
        seasonKey.setStyle(gridLabelStyle);
        seasonVal.setStyle(gridValueStyle);
        grid.addRow(row++, seasonKey, seasonVal);

        if (destination.getTimezone() != null) {
            Label tzKey = new Label("Timezone:");
            Label tzVal = new Label(destination.getTimezone());
            tzKey.setStyle(gridLabelStyle);
            tzVal.setStyle(gridValueStyle);
            grid.addRow(row++, tzKey, tzVal);
        }

        Label ratingKey = new Label("Rating:");
        Label ratingVal = new Label("⭐ " + destination.getAverageRating());
        ratingKey.setStyle(gridLabelStyle);
        ratingVal.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        grid.addRow(row++, ratingKey, ratingVal);

        // Apply styles to grid labels
        for (javafx.scene.Node node : grid.getChildren()) {
            if (node instanceof Label) {
                Label lbl = (Label) node;
                if (grid.getColumnIndex(node) == 0) {
                    lbl.setStyle(gridLabelStyle);
                } else {
                    lbl.setStyle(gridValueStyle);
                }
            }
        }

        Label descTitle = new Label("About");
        descTitle.setStyle(sectionStyle);
        TextArea descArea = new TextArea(destination.getDescription());
        descArea.setWrapText(true);
        descArea.setEditable(false);
        descArea.setPrefRowCount(3);
        descArea.getStyleClass().add("form-textarea");

        Label actLabel = new Label("Activities & Experiences");
        actLabel.setStyle(sectionStyle);

        VBox activitiesBox = new VBox(10);
        List<tn.esprit.entities.Activity> activities = activityService.getActivitiesByDestination(destination.getDestinationId());

        if (activities.isEmpty()) {
            Label noAct = new Label("No specific activities listed yet.");
            noAct.setStyle("-fx-text-fill: " + textSecondary + "; -fx-font-style: italic;");
            activitiesBox.getChildren().add(noAct);
        } else {
            String actRowBg = dark ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.05)";
            for (tn.esprit.entities.Activity a : activities) {
                HBox actRow = new HBox(10);
                actRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                actRow.setStyle("-fx-background-color: " + actRowBg + "; -fx-padding: 8; -fx-background-radius: 5;");

                Label aIcon = new Label("✨");
                Label aName = new Label(a.getName());
                aName.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");
                Label aPrice = new Label(String.format("$%.0f", a.getPrice()));
                aPrice.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                actRow.getChildren().addAll(aIcon, aName, spacer, aPrice);
                activitiesBox.getChildren().add(actRow);
            }
        }

        ScrollPane actScroll = new ScrollPane(activitiesBox);
        actScroll.setFitToWidth(true);
        actScroll.setPrefHeight(150);
        actScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- Weather Section ---
        Label weatherTitle = new Label("Current Weather");
        weatherTitle.setStyle(sectionStyle);

        VBox weatherBox = new VBox(5);
        Label weatherLoading = new Label("🌤️ Fetching weather data...");
        weatherLoading.setStyle("-fx-text-fill: " + textSecondary + "; -fx-font-style: italic;");
        weatherBox.getChildren().add(weatherLoading);

        // Fetch weather async using WeatherServiceUser
        new Thread(() -> {
            WeatherServiceUser.WeatherInfo weather = weatherService.getWeatherForCity(
                    destination.getCity() != null ? destination.getCity() : destination.getName()
            );
            javafx.application.Platform.runLater(() -> {
                weatherBox.getChildren().clear();
                if (weather != null) {
                    VBox weatherCard = new VBox(4);
                    String wcBg = dark ? "#1a332e" : "#e8f8f5";
                    weatherCard.setStyle("-fx-background-color: " + wcBg + "; -fx-padding: 12; -fx-background-radius: 8;");

                    Label tempLbl = new Label(weather.getWeatherEmoji() + " " + String.format("%.1f°C", weather.getTemperature()) +
                            " — " + weather.getCondition());
                    tempLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

                    Label feelsLbl = new Label("🌡️ Feels like " + String.format("%.1f°C", weather.getFeelsLike()));
                    Label humidLbl = new Label("💧 Humidity: " + weather.getHumidity() + "%");
                    Label windLbl = new Label("💨 Wind: " + String.format("%.1f m/s", weather.getWindSpeed()));

                    feelsLbl.setStyle("-fx-text-fill: " + textTertiary + ";");
                    humidLbl.setStyle("-fx-text-fill: " + textTertiary + ";");
                    windLbl.setStyle("-fx-text-fill: " + textTertiary + ";");

                    weatherCard.getChildren().addAll(tempLbl, feelsLbl, humidLbl, windLbl);
                    weatherBox.getChildren().add(weatherCard);
                } else {
                    Label noWeather = new Label("Could not fetch weather data.");
                    noWeather.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                    weatherBox.getChildren().add(noWeather);
                }
            });
        }).start();

        // --- Country Info Section ---
        Label countryTitle = new Label("Country Information");
        countryTitle.setStyle(sectionStyle);

        VBox countryBox = new VBox(5);
        Label countryLoading = new Label("🌍 Fetching country info...");
        countryLoading.setStyle("-fx-text-fill: " + textSecondary + "; -fx-font-style: italic;");
        countryBox.getChildren().add(countryLoading);

        // Fetch country info async
        new Thread(() -> {
            CountryService.CountryData countryData = countryService.getCountryByName(destination.getCountry());
            javafx.application.Platform.runLater(() -> {
                countryBox.getChildren().clear();
                if (countryData != null) {
                    VBox countryCard = new VBox(4);
                    String ccBg = dark ? "#2c2a1a" : "#fef9e7";
                    countryCard.setStyle("-fx-background-color: " + ccBg + "; -fx-padding: 12; -fx-background-radius: 8;");

                    Label flagLbl = new Label(countryData.flagEmoji + " " + countryData.officialName);
                    flagLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

                    Label capitalLbl = new Label("🏛️ Capital: " + countryData.capital);
                    Label regionLbl = new Label("🌍 Region: " + countryData.region + (countryData.subregion.isEmpty() ? "" : " (" + countryData.subregion + ")"));
                    Label popLbl = new Label("👥 Population: " + countryData.getPopulationString());
                    Label langLbl = new Label("🗣️ Languages: " + countryData.getLanguagesString());
                    Label currLbl = new Label("💰 Currency: " + countryData.getCurrenciesString());

                    String detailStyle = "-fx-text-fill: " + textTertiary + ";";
                    capitalLbl.setStyle(detailStyle);
                    regionLbl.setStyle(detailStyle);
                    popLbl.setStyle(detailStyle);
                    langLbl.setStyle(detailStyle);
                    currLbl.setStyle(detailStyle);

                    countryCard.getChildren().addAll(flagLbl, capitalLbl, regionLbl, popLbl, langLbl, currLbl);
                    countryBox.getChildren().add(countryCard);
                } else {
                    Label noCountry = new Label("Could not fetch country info.");
                    noCountry.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                    countryBox.getChildren().add(noCountry);
                }
            });
        }).start();

        // --- Reviews Button ---
        Button reviewsBtn = new Button("⭐ Reviews & Ratings");
        reviewsBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-size: 14px;");
        reviewsBtn.setMaxWidth(Double.MAX_VALUE);
        reviewsBtn.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader reviewLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/user/reviews_dialog.fxml"));
                javafx.scene.Parent reviewRoot = reviewLoader.load();
                ReviewDialogController reviewController = reviewLoader.getController();
                reviewController.setTarget(tn.esprit.entities.Review.TargetType.DESTINATION, destination.getDestinationId(), destination.getName());

                Stage reviewStage = new Stage();
                reviewStage.setTitle("Reviews: " + destination.getName());
                reviewStage.setScene(new javafx.scene.Scene(reviewRoot));
                if (ThemeManager.isDarkMode()) {
                    ThemeManager.applyTheme(reviewStage.getScene());
                }
                reviewStage.showAndWait();

                // Refresh the destination rating after reviews dialog closes
                loadDestinations();
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Could not open reviews dialog.");
            }
        });

        content.getChildren().addAll(nameLabel, grid, weatherTitle, weatherBox, countryTitle, countryBox, descTitle, descArea, actLabel, actScroll, reviewsBtn);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dialogPane.setContent(scrollPane);
        dialogPane.setPrefWidth(550);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleBooking(Destination destination) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/booking_dialog.fxml"));
            Parent root = loader.load();

            BookingDialogController controller = loader.getController();
            controller.setDestination(destination);

            Stage stage = new Stage();
            stage.setTitle("Book: " + destination.getName());
            stage.setScene(new javafx.scene.Scene(root));

            // Apply theme
            if (ThemeManager.isDarkMode()) {
                ThemeManager.applyTheme(stage.getScene());
            }

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open booking dialog.");
        }
    }

    private void navigateToMyBookings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/my_bookings.fxml"));
            Parent root = loader.load();
            UserBookingsController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load bookings view.");
        }
    }

    public void directBookDestination(Long destinationId) {
        try {
            Destination destination = destinationService.getDestinationById(destinationId);
            if (destination != null) {
                handleBooking(destination);
            } else {
                showError("Destination not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error during redirection to booking.");
        }
    }

    private void navigateToAdmin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin/destination_management.fxml"));
            searchBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load admin view.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (ThemeManager.isDarkMode()) {
            ThemeManager.applyTheme(alert.getDialogPane().getScene());
        }
        alert.showAndWait();
    }

    private String getBackgroundColorForType(Destination.DestinationType type) {
        if (type == null) return "-fx-background-color: #9B59B6; -fx-background-radius: 10 10 0 0;";
        switch (type) {
            case beach: return "-fx-background-color: linear-gradient(to bottom, #FFD700, #FFA500); -fx-background-radius: 10 10 0 0;";
            case mountain: return "-fx-background-color: linear-gradient(to bottom, #95A5A6, #7F8C8D); -fx-background-radius: 10 10 0 0;";
            case city: return "-fx-background-color: linear-gradient(to bottom, #3498DB, #2980B9); -fx-background-radius: 10 10 0 0;";
            case desert: return "-fx-background-color: linear-gradient(to bottom, #F4D03F, #D4AC0D); -fx-background-radius: 10 10 0 0;";
            case island: return "-fx-background-color: linear-gradient(to bottom, #1ABC9C, #16A085); -fx-background-radius: 10 10 0 0;";
            case forest: return "-fx-background-color: linear-gradient(to bottom, #27AE60, #229954); -fx-background-radius: 10 10 0 0;";
            case countryside: return "-fx-background-color: linear-gradient(to bottom, #2ECC71, #27AE60); -fx-background-radius: 10 10 0 0;";
            default: return "-fx-background-color: linear-gradient(to bottom, #9B59B6, #8E44AD); -fx-background-radius: 10 10 0 0;";
        }
    }

    private String getIconForType(Destination.DestinationType type) {
        if (type == null) return "🌍";
        switch (type) {
            case beach: return "🏖️";
            case mountain: return "⛰️";
            case city: return "🏙️";
            case desert: return "🏜️";
            case island: return "🏝️";
            case forest: return "🌲";
            case countryside: return "🌾";
            case cruise: return "🚢";
            default: return "🌍";
        }
    }

    private String getTypeDisplayName(Destination.DestinationType type) {
        if (type == null) return "General";
        switch (type) {
            case beach: return "Beach";
            case mountain: return "Mountain";
            case city: return "City";
            case desert: return "Desert";
            case island: return "Island";
            case forest: return "Forest";
            case countryside: return "Countryside";
            case cruise: return "Cruise";
            case other: return "Other";
            default: return type.toString();
        }
    }

    private String getSeasonDisplayName(Destination.Season season) {
        switch (season) {
            case all_year: return "All Year";
            case spring: return "Spring";
            case summer: return "Summer";
            case autumn: return "Autumn";
            case winter: return "Winter";
            default: return season.toString();
        }
    }

    private void showMyReviews() {
        try {
            URL fxmlLocation = getClass().getResource("/fxml/user/my_reviews_dialog.fxml");
            if (fxmlLocation == null) {
                showError("FXML file not found: /fxml/user/my_reviews_dialog.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("My Reviews & Ratings");
            stage.setScene(new javafx.scene.Scene(root));

            // Apply base stylesheet
            URL cssUrl = getClass().getResource("/css/user-destinations.css");
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }

            // Apply theme
            ThemeManager.applyTheme(stage.getScene());

            stage.showAndWait();
            loadDestinations();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open My Reviews dialog: " + e.getMessage());
        }
    }

    private void showMap() {
        try {
            Stage mapStage = new Stage();
            mapStage.setTitle("🗺️ Destinations Map");

            javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
            javafx.scene.web.WebEngine webEngine = webView.getEngine();

            // Build JSON array from all loaded destinations
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            List<Destination> source = (allDestinations != null) ? allDestinations : destinations;
            for (Destination d : source) {
                // Note: You need to add latitude/longitude to your Destination entity if not present
                if (d.getLatitude() == null || d.getLongitude() == null) continue;
                if (!first) json.append(",");
                first = false;
                json.append("{")
                        .append("\"id\":").append(d.getDestinationId() != null ? d.getDestinationId() : 0).append(",")
                        .append("\"name\":\"").append(escapeJson(d.getName())).append("\",")
                        .append("\"latitude\":").append(d.getLatitude()).append(",")
                        .append("\"longitude\":").append(d.getLongitude()).append(",")
                        .append("\"country\":\"").append(escapeJson(d.getCountry())).append("\",")
                        .append("\"city\":\"").append(escapeJson(d.getCity() != null ? d.getCity() : "")).append("\"")
                        .append("}");
            }
            json.append("]");
            String destinationsJson = json.toString();

            // Load the HTML page and inject data once loaded
            URL htmlUrl = getClass().getResource("/map.html");
            if (htmlUrl == null) {
                showError("Map HTML file not found: /map.html");
                return;
            }

            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    webEngine.executeScript("setMarkers(" + destinationsJson + ");");
                    webEngine.executeScript("if(typeof invalidateMapSize==='function'){invalidateMapSize();}");
                }
            });

            webEngine.load(htmlUrl.toExternalForm());

            javafx.scene.Scene scene = new javafx.scene.Scene(webView, 900, 600);
            mapStage.setScene(scene);
            mapStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    webEngine.executeScript("if(typeof invalidateMapSize==='function'){invalidateMapSize();}");
                } catch (Exception ignored) {}
            });
            mapStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    webEngine.executeScript("if(typeof invalidateMapSize==='function'){invalidateMapSize();}");
                } catch (Exception ignored) {}
            });
            mapStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open map: " + e.getMessage());
        }
    }

    private void applyTopBarAvatar() {
        if (currentUser == null || userAvatarView == null || avatarInitials == null) {
            return;
        }
        String avatarId = currentUser.getAvatarId();
        if (avatarId == null || !avatarId.contains(":")) {
            userAvatarView.setVisible(false);
            userAvatarView.setManaged(false);
            avatarInitials.setVisible(true);
            avatarInitials.setManaged(true);
            return;
        }
        String[] parts = avatarId.split(":");
        if (parts.length < 2) {
            return;
        }
        String avatarUrl = "https://api.dicebear.com/9.x/" + parts[0] + "/png?seed=" + parts[1] + "&size=40";
        Image image = new Image(avatarUrl, 40, 40, true, true, true);
        userAvatarView.setImage(image);
        userAvatarView.setVisible(true);
        userAvatarView.setManaged(true);
        avatarInitials.setVisible(false);
        avatarInitials.setManaged(false);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void showRecommendations() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🤖 AI Destination Recommendations");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/user-destinations.css").toExternalForm());
        if (ThemeManager.isDarkMode()) {
            dialogPane.getStylesheets().add(getClass().getResource("/css/dark-mode.css").toExternalForm());
        }
        dialogPane.getStyleClass().add("details-dialog");

        boolean dark = ThemeManager.isDarkMode();
        String textPrimary = dark ? "#e0e0e0" : "#1a1a2e";
        String textSecondary = dark ? "#a0a0b8" : "#555";
        String cardBg = dark ? "#2a2a3d" : "#f8f9fa";
        String accentGradient = dark ? "linear-gradient(to right, #7c3aed, #a855f7)" : "linear-gradient(to right, #667eea, #764ba2)";

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(550);

        // Title
        Label title = new Label("🤖 Smart Recommendations");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

        Label subtitle = new Label("Powered by content-based filtering with weighted scoring");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + textSecondary + "; -fx-font-style: italic;");

        // Preference inputs
        Label catLabel = new Label("🏷️ Preferred Category");
        catLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

        ComboBox<Destination.DestinationType> categoryCombo = new ComboBox<>();
        categoryCombo.setItems(javafx.collections.FXCollections.observableArrayList(Destination.DestinationType.values()));
        categoryCombo.setPromptText("Select a category...");
        categoryCombo.setPrefWidth(300);
        categoryCombo.getStyleClass().add("filter-combo");

        Label budgetLabel = new Label("💰 Maximum Budget (per person, $)");
        budgetLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

        TextField budgetField = new TextField();
        budgetField.setPromptText("e.g. 200");
        budgetField.setPrefWidth(300);
        budgetField.getStyleClass().add("search-field");

        // Results container
        VBox resultsBox = new VBox(12);
        resultsBox.setPadding(new Insets(10, 0, 0, 0));

        Button getRecsBtn = new Button("✨ Get Recommendations");
        getRecsBtn.setStyle("-fx-background-color: " + accentGradient + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 12 24; -fx-cursor: hand; -fx-font-size: 14px;");
        getRecsBtn.setMaxWidth(Double.MAX_VALUE);

        getRecsBtn.setOnAction(e -> {
            resultsBox.getChildren().clear();

            Destination.DestinationType selectedType = categoryCombo.getValue();
            double maxBudget = 0;
            try {
                String budgetText = budgetField.getText().trim();
                if (!budgetText.isEmpty()) {
                    maxBudget = Double.parseDouble(budgetText);
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid budget number.");
                return;
            }

            // Show loading
            Label loadingLabel = new Label("🔄 Analyzing destinations...");
            loadingLabel.setStyle("-fx-text-fill: " + textSecondary + "; -fx-font-style: italic;");
            resultsBox.getChildren().add(loadingLabel);

            final double budget = maxBudget;
            new Thread(() -> {
                RecommendationService recService = new RecommendationService();
                java.util.List<RecommendationService.ScoredDestination> results = recService.getRecommendations(selectedType, budget);

                javafx.application.Platform.runLater(() -> {
                    resultsBox.getChildren().clear();

                    if (results.isEmpty()) {
                        Label noResults = new Label("No destinations found. Try different preferences.");
                        noResults.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                        resultsBox.getChildren().add(noResults);
                        return;
                    }

                    Label resultsTitle = new Label("🏆 Top " + results.size() + " Recommendations");
                    resultsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");
                    resultsBox.getChildren().add(resultsTitle);

                    int rank = 1;
                    for (RecommendationService.ScoredDestination sd : results) {
                        Destination d = sd.getDestination();

                        VBox card = new VBox(6);
                        card.setStyle("-fx-background-color: " + cardBg + "; -fx-padding: 14; -fx-background-radius: 10; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");

                        // Rank + Name row
                        HBox headerRow = new HBox(10);
                        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        String medalEmoji = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank;
                        Label rankLabel = new Label(medalEmoji);
                        rankLabel.setStyle("-fx-font-size: 20px;");

                        Label nameLabel = new Label(d.getName());
                        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

                        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                        // Score badge
                        Label scoreBadge = new Label(sd.getScorePercent());
                        String badgeColor = sd.getScore() >= 0.7 ? "#10b981" : sd.getScore() >= 0.4 ? "#f59e0b" : "#ef4444";
                        scoreBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; " +
                                "-fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 13px;");

                        headerRow.getChildren().addAll(rankLabel, nameLabel, spacer, scoreBadge);

                        // Details row
                        Label detailsLabel = new Label("📍 " + d.getFullLocation() + "  •  " +
                                getTypeDisplayName(d.getType()) + "  •  ⭐ " + String.format("%.1f", d.getAverageRating()));
                        detailsLabel.setStyle("-fx-text-fill: " + textSecondary + "; -fx-font-size: 12.5px;");

                        // Budget & popularity info
                        String budgetInfo = d.getEstimatedBudget() != null && d.getEstimatedBudget() > 0
                                ? String.format("$%.0f/person", d.getEstimatedBudget()) : "N/A";
                        String popInfo = d.getPopularity() != null && d.getPopularity() > 0
                                ? d.getPopularity() + " bookings" : "New";

                        Label metricsLabel = new Label("💰 " + budgetInfo + "  •  📊 " + popInfo + "  •  " + sd.getMatchLabel());
                        metricsLabel.setStyle("-fx-text-fill: " + textSecondary + "; -fx-font-size: 12px;");

                        // Score bar
                        javafx.scene.layout.StackPane barBg = new javafx.scene.layout.StackPane();
                        barBg.setStyle("-fx-background-color: " + (dark ? "#3a3a4d" : "#e5e7eb") + "; -fx-background-radius: 4; -fx-pref-height: 6;");
                        barBg.setMaxWidth(Double.MAX_VALUE);

                        javafx.scene.layout.StackPane barFill = new javafx.scene.layout.StackPane();
                        barFill.setStyle("-fx-background-color: " + badgeColor + "; -fx-background-radius: 4; -fx-pref-height: 6;");
                        barFill.setMaxWidth(500 * sd.getScore());
                        barFill.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        javafx.scene.layout.StackPane barContainer = new javafx.scene.layout.StackPane(barBg, barFill);
                        javafx.scene.layout.StackPane.setAlignment(barFill, javafx.geometry.Pos.CENTER_LEFT);

                        card.getChildren().addAll(headerRow, detailsLabel, metricsLabel, barContainer);
                        resultsBox.getChildren().add(card);
                        rank++;
                    }
                });
            }).start();
        });

        content.getChildren().addAll(title, subtitle,
                new javafx.scene.control.Separator(),
                catLabel, categoryCombo, budgetLabel, budgetField, getRecsBtn,
                new javafx.scene.control.Separator(),
                resultsBox);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dialogPane.setContent(scrollPane);
        dialogPane.setPrefWidth(600);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

}