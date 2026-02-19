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
import tn.esprit.services.DestinationService;
import tn.esprit.services.ActivityService;
import tn.esprit.utils.ThemeManager;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class UserDestinationsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<Destination.DestinationType> typeFilter;
    @FXML private ComboBox<String> countryFilter;
    @FXML private Button searchBtn;
    @FXML private Button resetBtn;
    @FXML private Button backToAdminBtn;
    @FXML private Button themeBtn; // Added missing button

    @FXML private FlowPane destinationsContainer;
    @FXML private Label resultsCountLabel;
    @FXML private ProgressIndicator loadingIndicator;

    // Added missing fields
    private DestinationService destinationService;
    private ActivityService activityService;
    private ObservableList<Destination> destinations;
    private List<Destination> allDestinations;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        destinationService = new DestinationService();
        activityService = new ActivityService();
        destinations = FXCollections.observableArrayList();

        setupFilters();
        setupActions();
        loadDestinations();
        
        // Defer theme apply
        javafx.application.Platform.runLater(() -> {
            if (themeBtn != null && themeBtn.getScene() != null) {
                ThemeManager.applyTheme(themeBtn.getScene());
                updateThemeButtonText();
            }
        });
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
    @FXML private Button browseActivitiesBtn; // New button

    private void setupActions() {
        searchBtn.setOnAction(e -> filterDestinations());
        resetBtn.setOnAction(e -> resetFilters());
        backToAdminBtn.setOnAction(e -> navigateToAdmin());
        
        if (myBookingsBtn != null) {
            myBookingsBtn.setOnAction(e -> navigateToMyBookings());
        }

        if (browseActivitiesBtn != null) {
            browseActivitiesBtn.setOnAction(e -> navigateToActivities());
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
            destinationsContainer.getChildren().add(createDestinationCard(d));
        }
        if (destinations.isEmpty()) {
            Label noResults = new Label("No destinations found");
            noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 50;");
            destinationsContainer.getChildren().add(noResults);
        }
    }

    private VBox createDestinationCard(Destination destination) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-padding: 15; -fx-min-width: 280; -fx-max-width: 280; -fx-cursor: hand;");

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(52,152,219,0.5), 15, 0, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-padding: 15; -fx-min-width: 280; -fx-max-width: 280;"));

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(150);
        imageContainer.setPrefWidth(250);
        imageContainer.setStyle(getBackgroundColorForType(destination.getType()));

        Label typeIcon = new Label(getIconForType(destination.getType()));
        typeIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
        imageContainer.getChildren().add(typeIcon);

        Label nameLabel = new Label(destination.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        Label locationLabel = new Label("📍 " + destination.getFullLocation());
        locationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        HBox infoBox = new HBox(10);
        infoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label typeLabel = new Label(getTypeDisplayName(destination.getType()));
        typeLabel.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        Label ratingLabel = new Label("⭐ " + String.format("%.1f", destination.getAverageRating()));
        ratingLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");
        infoBox.getChildren().addAll(typeLabel, ratingLabel);

        Label seasonLabel = new Label("📅 Best: " + getSeasonDisplayName(destination.getBestSeason()));
        seasonLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");

        String desc = destination.getDescription();
        if (desc != null && desc.length() > 100) desc = desc.substring(0, 97) + "...";
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e; -fx-line-spacing: 2;");

        Button bookBtn = new Button("Book Now");
        bookBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10 15; -fx-cursor: hand; -fx-font-size: 14px;");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> handleBooking(destination));

        card.getChildren().addAll(imageContainer, nameLabel, locationLabel, infoBox, seasonLabel, descLabel, bookBtn);
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

        Label nameLabel = new Label(destination.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        if (ThemeManager.isDarkMode()) nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");


        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        int row = 0;
        grid.addRow(row++, new Label("Type:"), new Label(getTypeDisplayName(destination.getType())));
        grid.addRow(row++, new Label("Country:"), new Label(destination.getCountry()));
        if (destination.getCity() != null) grid.addRow(row++, new Label("City:"), new Label(destination.getCity()));
        grid.addRow(row++, new Label("Season:"), new Label(getSeasonDisplayName(destination.getBestSeason())));
        if (destination.getTimezone() != null) grid.addRow(row++, new Label("Timezone:"), new Label(destination.getTimezone()));
        grid.addRow(row++, new Label("Rating:"), new Label("⭐ " + destination.getAverageRating()));

        Label descTitle = new Label("About");
        descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 5 0;");
        TextArea descArea = new TextArea(destination.getDescription());
        descArea.setWrapText(true);
        descArea.setEditable(false);
        descArea.setPrefRowCount(3);
        descArea.getStyleClass().add("form-textarea");

        Label actLabel = new Label("Activities & Experiences");
        actLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 5 0;");
        
        VBox activitiesBox = new VBox(10);
        List<tn.esprit.entities.Activity> activities = activityService.getActivitiesByDestination(destination.getDestinationId());
        
        if (activities.isEmpty()) {
            Label noAct = new Label("No specific activities listed yet.");
            noAct.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
            activitiesBox.getChildren().add(noAct);
        } else {
            for (tn.esprit.entities.Activity a : activities) {
                HBox actRow = new HBox(10);
                actRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                actRow.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-padding: 8; -fx-background-radius: 5;");
                
                Label aIcon = new Label("✨");
                Label aName = new Label(a.getName());
                aName.setStyle("-fx-font-weight: bold;");
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

        content.getChildren().addAll(nameLabel, grid, descTitle, descArea, actLabel, actScroll);
        dialogPane.setContent(content);
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
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user/my_bookings.fxml"));
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load bookings view.");
        }
    }

    private void navigateToActivities() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user/user_activities.fxml"));
            themeBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load activities view.");
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
            backToAdminBtn.getScene().setRoot(root);
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
        alert.showAndWait();
    }

    private String getBackgroundColorForType(Destination.DestinationType type) {
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
}