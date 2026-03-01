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
import tn.esprit.entities.User;
import tn.esprit.services.ActivityService;
import tn.esprit.services.DestinationService;
import tn.esprit.utils.ThemeManager;
import tn.esprit.utils.ImageHelper;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

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
    @FXML private Label avatarInitials;
    @FXML private Label userNameLabel;
    @FXML private ImageView userAvatarView;

    private ActivityService activityService;
    private DestinationService destinationService;
    private ObservableList<Activity> activities;
    private List<Activity> allActivities;
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        activityService = new ActivityService();
        destinationService = new DestinationService();
        activities = FXCollections.observableArrayList();

        setupFilters();
        setupActions();

        // Load data in background to prevent freeze
        javafx.application.Platform.runLater(this::loadActivities);

        javafx.application.Platform.runLater(() -> {
            if (themeBtn != null && themeBtn.getScene() != null) {
                ThemeManager.applyTheme(themeBtn.getScene());
                updateThemeButtonText();
            }
        });
    }

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
        System.out.println("✅ User set in ActivitiesController: " +
                (this.currentUser != null ? this.currentUser.getEmail() : "null"));
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

    @FXML
    private void handleHomeNav(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/home.fxml"));
            Parent root = loader.load();
            HomeController controller = loader.getController();
            controller.setUser(currentUser);
            searchBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load home view.");
        }
    }

    @FXML
    private void handleDestinationsNav(javafx.scene.input.MouseEvent event) {
        navigateToDestinations();
    }

    @FXML
    private void handleAccommodationsNav(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/AccommodationsView.fxml"));
            Parent root = loader.load();
            AccommodationsController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            searchBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load accommodations view.");
        }
    }

    @FXML
    private void handleActivitiesNav(javafx.scene.input.MouseEvent event) {
        // Already here
    }

    @FXML
    private void handleTransportNav(javafx.scene.input.MouseEvent event) {
        navigateToTransport();
    }

    @FXML
    private void handlePacksOffersNav(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/UserPacksOffersView.fxml"));
            Parent root = loader.load();
            UserPacksOffersController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            searchBtn.getScene().setRoot(root);
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
            searchBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load profile view.");
        }
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user/login.fxml"));
            searchBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not perform logout.");
        }
    }

    @FXML
    private void handleMyBookings(javafx.event.ActionEvent event) {
        navigateToDestinations();
    }

    private void navigateToTransport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/TransportUserInterface.fxml"));
            Parent root = loader.load();
            TransportUserInterfaceController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            searchBtn.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not load transport view.");
        }
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
            try {
                activitiesContainer.getChildren().add(createActivityCard(a));
            } catch (Exception e) {
                System.err.println("❌ Error creating activity card for " + a.getName() + ": " + e.getMessage());
            }
        }
        if (activities.isEmpty()) {
            Label noResults = new Label("No activities found matching your filters.");
            noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 50;");
            activitiesContainer.getChildren().add(noResults);
        }
    }

    private VBox createActivityCard(Activity activity) {
        VBox card = new VBox(10);
        boolean dark = ThemeManager.isDarkMode();
        String cardBg = dark ? "#2a2a3d" : "white";
        String shadow = dark ? "rgba(0,0,0,0.5)" : "rgba(0,0,0,0.1)";
        String cardStyle = "-fx-background-color: " + cardBg + "; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, " + shadow + ", 10, 0, 0, 2); " +
                "-fx-padding: 15; -fx-min-width: 280; -fx-max-width: 280; -fx-cursor: hand;";
        card.setStyle(cardStyle);

        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(150);
        imageContainer.setPrefWidth(250);

        Image image = ImageHelper.loadImage("activities", activity.getName());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(150);
            imageView.setFitWidth(250);
            imageView.setPreserveRatio(false);
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(250, 150);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imageView.setClip(clip);
            imageContainer.getChildren().add(imageView);
        } else {
            imageContainer.setStyle(getCategoryColor(activity.getCategory()));
            Label categoryIcon = new Label(getCategoryIcon(activity.getCategory()));
            categoryIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: white;");
            imageContainer.getChildren().add(categoryIcon);
        }

        String textP = dark ? "#e0e0e0" : "#2c3e50";
        String textS = dark ? "#a0a0b8" : "#7f8c8d";

        Label nameLabel = new Label(activity.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + textP + ";");
        nameLabel.setWrapText(true);

        Label destLabel = new Label("📍 " + activity.getDestinationName());
        destLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + textS + "; -fx-font-weight: bold;");

        HBox priceBox = new HBox(10);
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label categoryLabel = new Label(activity.getCategory().toString());
        String catBg = dark ? "#312e81" : "#e3f2fd";
        String catColor = dark ? "#a5b4fc" : "#1976d2";
        categoryLabel.setStyle("-fx-background-color: " + catBg + "; -fx-padding: 5 10; -fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + catColor + ";");
        Label priceLabel = new Label("$" + String.format("%.2f", activity.getPrice()));
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        priceBox.getChildren().addAll(categoryLabel, priceLabel);

        Button bookBtn = new Button("Book & View Destination");
        bookBtn.setStyle("-fx-background-color: linear-gradient(to right, #27ae60, #2ecc71); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 15; -fx-cursor: hand; -fx-font-size: 14px;");
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

        boolean dark = ThemeManager.isDarkMode();
        String textPrimary = dark ? "#e0e0e0" : "#2c3e50";
        String textSecondary = dark ? "#a0a0b8" : "#34495e";
        String textTertiary = dark ? "#c4c4dc" : "#555";

        Label nameLabel = new Label(activity.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + textPrimary + ";");

        Label destLabel = new Label("📍 Located in: " + activity.getDestinationName());
        destLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + textSecondary + "; -fx-font-weight: bold;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        String gls = "-fx-text-fill: " + textTertiary + "; -fx-font-weight: bold;";
        String gvs = "-fx-text-fill: " + textPrimary + ";";
        Label k1 = new Label("Category:"); Label v1 = new Label(activity.getCategory().name());
        Label k2 = new Label("Price:"); Label v2 = new Label("$" + String.format("%.2f", activity.getPrice()));
        Label k3 = new Label("Capacity:"); Label v3 = new Label(activity.getCapacity() + " people");
        k1.setStyle(gls); v1.setStyle(gvs);
        k2.setStyle(gls); v2.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        k3.setStyle(gls); v3.setStyle(gvs);
        grid.addRow(0, k1, v1);
        grid.addRow(1, k2, v2);
        grid.addRow(2, k3, v3);

        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 0 5 0; -fx-text-fill: " + textPrimary + ";");
        TextArea descArea = new TextArea(activity.getDescription());
        descArea.setWrapText(true);
        descArea.setEditable(false);
        descArea.setPrefRowCount(5);
        descArea.getStyleClass().add("form-textarea");

        Button bookNowBtn = new Button("Book Now (Go to Destination)");
        bookNowBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        bookNowBtn.setMaxWidth(Double.MAX_VALUE);
        bookNowBtn.setOnAction(e -> {
            dialog.close();
            handleRedirection(activity);
        });

        // Reviews button
        Button reviewsBtn = new Button("⭐ Reviews & Ratings");
        reviewsBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-size: 14px;");
        reviewsBtn.setMaxWidth(Double.MAX_VALUE);
        reviewsBtn.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader reviewLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/user/reviews_dialog.fxml"));
                javafx.scene.Parent reviewRoot = reviewLoader.load();
                ReviewDialogController reviewController = reviewLoader.getController();
                reviewController.setTarget(tn.esprit.entities.Review.TargetType.ACTIVITY, activity.getActivityId(), activity.getName());

                javafx.stage.Stage reviewStage = new javafx.stage.Stage();
                reviewStage.setTitle("Reviews: " + activity.getName());
                reviewStage.setScene(new javafx.scene.Scene(reviewRoot));
                if (ThemeManager.isDarkMode()) {
                    ThemeManager.applyTheme(reviewStage.getScene());
                }
                reviewStage.showAndWait();

                // Refresh activities after reviews dialog closes
                loadActivities();
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Could not open reviews dialog.");
            }
        });

        content.getChildren().addAll(nameLabel, destLabel, grid, descTitle, descArea, bookNowBtn, reviewsBtn);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dialogPane.setContent(scrollPane);
        dialogPane.setPrefWidth(550);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/user_destinations.fxml"));
            Parent root = loader.load();
            UserDestinationsController controller = loader.getController();
            controller.setCurrentUser(currentUser);
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