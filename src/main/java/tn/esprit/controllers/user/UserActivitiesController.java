package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Activity;
import tn.esprit.entities.Destination;
import tn.esprit.services.ActivityService;
import tn.esprit.services.DestinationService;
import tn.esprit.utils.ThemeManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UserActivitiesController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<Activity.ActivityCategory> categoryFilter;
    @FXML private Button searchBtn;
    @FXML private Button resetBtn;
    @FXML private Button backToDestinationsBtn;
    @FXML private Button themeBtn;

    @FXML private FlowPane activitiesContainer;
    @FXML private Label resultsCountLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private ActivityService activityService;
    private DestinationService destinationService;
    private ObservableList<Activity> activities;
    private List<Activity> allActivities;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activityService = new ActivityService();
        destinationService = new DestinationService();
        activities = FXCollections.observableArrayList();

        setupFilters();
        setupActions();
        loadActivities();

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
        }
    }

    private void setupFilters() {
        categoryFilter.setItems(FXCollections.observableArrayList(Activity.ActivityCategory.values()));
        categoryFilter.setPromptText("All Categories");
    }

    private void setupActions() {
        searchBtn.setOnAction(e -> filterActivities());
        resetBtn.setOnAction(e -> resetFilters());
        backToDestinationsBtn.setOnAction(e -> navigateToDestinations());
        
        if (themeBtn != null) {
            themeBtn.setOnAction(e -> {
                ThemeManager.toggleTheme(themeBtn.getScene());
                updateThemeButtonText();
            });
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() >= 2 || newVal.isEmpty()) {
                filterActivities();
            }
        });

        categoryFilter.setOnAction(e -> filterActivities());
    }

    private void loadActivities() {
        loadingIndicator.setVisible(true);
        activitiesContainer.getChildren().clear();

        try {
            allActivities = activityService.getAllActivities();
            activities.setAll(allActivities);
            displayActivities(activities);
            resultsCountLabel.setText("Found " + activities.size() + " activities");
        } catch (Exception e) {
            showError("Error loading activities: " + e.getMessage());
            e.printStackTrace();
        } finally {
            loadingIndicator.setVisible(false);
        }
    }

    private void filterActivities() {
        String keyword = searchField.getText().trim().toLowerCase();
        Activity.ActivityCategory selectedCategory = categoryFilter.getValue();

        List<Activity> filtered = allActivities.stream()
                .filter(a -> {
                    if (!keyword.isEmpty()) {
                        return a.getName().toLowerCase().contains(keyword) ||
                                (a.getDescription() != null && a.getDescription().toLowerCase().contains(keyword)) ||
                                (a.getDestinationName() != null && a.getDestinationName().toLowerCase().contains(keyword));
                    }
                    return true;
                })
                .filter(a -> selectedCategory == null || a.getCategory() == selectedCategory)
                .toList();

        activities.setAll(filtered);
        displayActivities(activities);
        resultsCountLabel.setText("Found " + activities.size() + " activities");
    }

    private void resetFilters() {
        searchField.clear();
        categoryFilter.setValue(null);
        filterActivities();
    }

    private void displayActivities(ObservableList<Activity> activities) {
        activitiesContainer.getChildren().clear();
        for (Activity a : activities) {
            activitiesContainer.getChildren().add(createActivityCard(a));
        }
        if (activities.isEmpty()) {
            Label noResults = new Label("No activities found");
            noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 50;");
            activitiesContainer.getChildren().add(noResults);
        }
    }

    private VBox createActivityCard(Activity activity) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-padding: 15; -fx-min-width: 280; -fx-max-width: 280; -fx-cursor: hand;");

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(150);
        imageContainer.setPrefWidth(250);
        imageContainer.setStyle(getCategoryColor(activity.getCategory()));

        Label categoryIcon = new Label(getCategoryIcon(activity.getCategory()));
        categoryIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
        imageContainer.getChildren().add(categoryIcon);

        Label nameLabel = new Label(activity.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        Label destLabel = new Label("📍 " + activity.getDestinationName());
        destLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");

        HBox priceBox = new HBox(10);
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label categoryLabel = new Label(activity.getCategory().toString());
        categoryLabel.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976d2;");
        Label priceLabel = new Label("$" + String.format("%.2f", activity.getPrice()));
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        priceBox.getChildren().addAll(categoryLabel, priceLabel);

        Button bookBtn = new Button("Book & View Destination");
        bookBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 10 15; -fx-cursor: hand; -fx-font-size: 14px;");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> handleRedirection(activity));

        card.getChildren().addAll(imageContainer, nameLabel, destLabel, priceBox, bookBtn);
        card.setOnMouseClicked(e -> showActivityDetails(activity));

        return card;
    }

    private void showActivityDetails(Activity activity) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(activity.getName());
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

        Label nameLabel = new Label(activity.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        if (ThemeManager.isDarkMode()) nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Label destLabel = new Label("📍 Located in: " + activity.getDestinationName());
        destLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #34495e; -fx-font-weight: bold;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.addRow(0, new Label("Category:"), new Label(activity.getCategory().name()));
        grid.addRow(1, new Label("Price:"), new Label("$" + String.format("%.2f", activity.getPrice())));
        grid.addRow(2, new Label("Capacity:"), new Label(activity.getCapacity() + " people"));

        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 5 0;");
        TextArea descArea = new TextArea(activity.getDescription());
        descArea.setWrapText(true);
        descArea.setEditable(false);
        descArea.setPrefRowCount(5);
        descArea.setStyle("-fx-background-color: transparent;");

        Button bookNowBtn = new Button("Book Now (Go to Destination)");
        bookNowBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        bookNowBtn.setMaxWidth(Double.MAX_VALUE);
        bookNowBtn.setOnAction(e -> {
            dialog.close();
            handleRedirection(activity);
        });

        content.getChildren().addAll(nameLabel, destLabel, grid, descTitle, descArea, bookNowBtn);
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleRedirection(Activity activity) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/user_destinations.fxml"));
            Parent root = loader.load();
            
            UserDestinationsController controller = loader.getController();
            
            // Switch scene first
            searchBtn.getScene().setRoot(root);
            
            // Then trigger booking dialog
            controller.directBookDestination(activity.getDestinationId());
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Redirection failed.");
        }
    }

    private void navigateToDestinations() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user/user_destinations.fxml"));
            backToDestinationsBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load destinations view.");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getCategoryColor(Activity.ActivityCategory category) {
        switch (category) {
            case Adventure: return "-fx-background-color: linear-gradient(to bottom, #e67e22, #d35400); -fx-background-radius: 10 10 0 0;";
            case Culture: return "-fx-background-color: linear-gradient(to bottom, #9b59b6, #8e44ad); -fx-background-radius: 10 10 0 0;";
            case Relax: return "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9); -fx-background-radius: 10 10 0 0;";
            case Food: return "-fx-background-color: linear-gradient(to bottom, #e74c3c, #c0392b); -fx-background-radius: 10 10 0 0;";
            case Sports: return "-fx-background-color: linear-gradient(to bottom, #2ecc71, #27ae60); -fx-background-radius: 10 10 0 0;";
            default: return "-fx-background-color: linear-gradient(to bottom, #95a5a6, #7f8c8d); -fx-background-radius: 10 10 0 0;";
        }
    }

    private String getCategoryIcon(Activity.ActivityCategory category) {
        switch (category) {
            case Adventure: return "🧗";
            case Culture: return "🏛️";
            case Relax: return "🧘";
            case Food: return "🍱";
            case Sports: return "⚽";
            default: return "✨";
        }
    }
}
