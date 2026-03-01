package tn.esprit.controllers.user;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import netscape.javascript.JSObject;
import tn.esprit.entities.Accommodation;
import tn.esprit.entities.User;
import tn.esprit.services.AccommodationService;
import tn.esprit.services.AccommodationCompareService;
import tn.esprit.services.UserService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javafx.collections.transformation.FilteredList;
import javafx.util.Duration;

public class AccommodationsController {

    // Top / center
    @FXML private TextField searchField;
    @FXML private Label resultsCountLabel;
    @FXML private Label avatarInitials;
    @FXML private Label userNameLabel;
    @FXML private GridPane accommodationsGrid;

    // Filters
    @FXML private Slider priceSlider;
    @FXML private Label priceRangeLabel;
    @FXML private ComboBox<String> minRatingCombo;
    @FXML private CheckBox typeHotelCheck;
    @FXML private CheckBox typeApartmentCheck;
    @FXML private CheckBox typeVillaCheck;
    @FXML private CheckBox typeResortCheck;
    @FXML private CheckBox typeHostelCheck;
    @FXML private CheckBox amenityWifiCheck;
    @FXML private CheckBox amenityPoolCheck;
    @FXML private CheckBox amenityParkingCheck;
    @FXML private CheckBox amenitySpaCheck;
    @FXML private DatePicker checkInDatePicker;
    @FXML private DatePicker checkOutDatePicker;

    // Right map panel
    @FXML private VBox mapPanel;
    @FXML private Button mapToggleButton;
    @FXML private WebView mapWebView;

    // Bottom compare bar
    @FXML private javafx.scene.layout.HBox compareBar;
    @FXML private Label compareCountLabel;
    @FXML private Button compareNowButton;

    private final AccommodationService accommodationService = new AccommodationService();
    private final AccommodationCompareService compareService = new AccommodationCompareService();
    private final UserService userService = new UserService();
    private final ObservableList<Accommodation> allAccommodations = FXCollections.observableArrayList();
    private FilteredList<Accommodation> filteredAccommodations;
    private User currentUser;

    private final List<Accommodation> selectedForCompare = new ArrayList<>();
    private WebEngine mapEngine;
    private boolean mapReady = false;
    private boolean mapCollapsed = false;
    private boolean compareBarVisible = false;

    @FXML
    public void initialize() {
        configureFilters();
        configureCompareBar();
        configureMap();
        loadAccommodations();
        ensureCurrentUserLoaded();
    }

    private void configureFilters() {
        minRatingCombo.getItems().setAll("Any", "1+", "2+", "3+", "4+", "5");
        minRatingCombo.setValue("Any");

        priceRangeLabel.setText("Up to $" + (int) priceSlider.getValue() + "/night");
        priceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            priceRangeLabel.setText("Up to $" + newVal.intValue() + "/night");
            applyFilters();
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        minRatingCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        checkInDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        checkOutDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        typeHotelCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeApartmentCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeVillaCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeResortCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        typeHostelCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        amenityWifiCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        amenityPoolCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        amenityParkingCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        amenitySpaCheck.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void configureCompareBar() {
        compareNowButton.setDisable(true);
        compareNowButton.setOnAction(event -> handleCompareWithAI());
        updateCompareBar();
    }

    private void configureMap() {
        mapEngine = mapWebView.getEngine();
        mapEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapReady = true;
                JSObject window = (JSObject) mapEngine.executeScript("window");
                window.setMember("javaBridge", new MapBridge());
                invalidateMapSize();
                refreshMapMarkers();
            }
        });

        URL mapUrl = getClass().getResource("/map.html");
        if (mapUrl != null) {
            mapEngine.load(mapUrl.toExternalForm());
        }
    }

    private void loadAccommodations() {
        allAccommodations.setAll(accommodationService.getAll());
        filteredAccommodations = new FilteredList<>(allAccommodations, a -> true);
        applyFilters();
    }

    private void applyFilters() {
        if (filteredAccommodations == null) return;

        final String search = normalize(searchField.getText());
        final int minStars = parseMinStars(minRatingCombo.getValue());
        final double maxEstimatedPrice = priceSlider.getValue();

        final boolean anyTypeChecked =
                typeHotelCheck.isSelected() || typeApartmentCheck.isSelected() ||
                typeVillaCheck.isSelected() || typeResortCheck.isSelected() ||
                typeHostelCheck.isSelected();

        final boolean wifi = amenityWifiCheck.isSelected();
        final boolean pool = amenityPoolCheck.isSelected();
        final boolean parking = amenityParkingCheck.isSelected();
        final boolean spa = amenitySpaCheck.isSelected();

        final boolean invalidDateRange =
                checkInDatePicker.getValue() != null &&
                checkOutDatePicker.getValue() != null &&
                checkOutDatePicker.getValue().isBefore(checkInDatePicker.getValue());

        filteredAccommodations.setPredicate(a -> {
            if (a == null || invalidDateRange) return false;

            boolean matchesSearch = search.isEmpty() ||
                    normalize(a.getName()).contains(search) ||
                    normalize(a.getCity()).contains(search) ||
                    normalize(a.getCountry()).contains(search) ||
                    normalize(a.getType()).contains(search) ||
                    normalize(a.getDescription()).contains(search);

            boolean matchesStars = a.getStars() >= minStars;
            boolean matchesEstimatedPrice = estimateNightlyPrice(a) <= maxEstimatedPrice;

            boolean matchesType = true;
            if (anyTypeChecked) {
                String type = normalize(a.getType());
                matchesType =
                        (typeHotelCheck.isSelected() && type.contains("hotel")) ||
                        (typeApartmentCheck.isSelected() && type.contains("apartment")) ||
                        (typeVillaCheck.isSelected() && type.contains("villa")) ||
                        (typeResortCheck.isSelected() && type.contains("resort")) ||
                        (typeHostelCheck.isSelected() && type.contains("hostel"));
            }

            boolean matchesAmenities =
                    (!wifi || hasAmenity(a, "wifi")) &&
                    (!pool || hasAmenity(a, "pool")) &&
                    (!parking || hasAmenity(a, "parking")) &&
                    (!spa || hasAmenity(a, "spa"));

            return matchesSearch && matchesStars && matchesEstimatedPrice && matchesType && matchesAmenities;
        });

        renderAccommodationCards();
        refreshMapMarkers();
        updateResultsLabel();
    }

    private void renderAccommodationCards() {
        accommodationsGrid.getChildren().clear();
        int col = 0;
        int row = 0;
        int index = 0;

        for (Accommodation a : filteredAccommodations) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        Objects.requireNonNull(getClass().getResource("/fxml/user/AccommodationCard.fxml"))
                );
                Node cardNode = loader.load();
                AccommodationCardController cardController = loader.getController();
                cardController.setParentController(this);
                cardController.setAccommodation(a);
                cardController.setCompareSelected(isSelectedForCompare(a));
                addCardHoverEffects(cardNode);

                GridPane.setHgrow(cardNode, Priority.ALWAYS);
                accommodationsGrid.add(cardNode, col, row);
                animateCardEntry(cardNode, index++);

                col++;
                if (col == 2) {
                    col = 0;
                    row++;
                }
            } catch (IOException ignored) {
                // Skip malformed card and continue rendering the rest.
            }
        }
    }

    public boolean toggleCompareAccommodation(Accommodation accommodation, boolean select) {
        if (accommodation == null) return false;

        if (select) {
            if (isSelectedForCompare(accommodation)) return true;
            if (selectedForCompare.size() >= 3) {
                showWarning("Compare limit reached", "You can select up to 3 accommodations.");
                return false;
            }
            selectedForCompare.add(accommodation);
        } else {
            selectedForCompare.removeIf(a -> a.getId() == accommodation.getId());
        }

        updateCompareBar();
        return true;
    }

    @FXML
    private void toggleMapPanel() {
        mapCollapsed = !mapCollapsed;
        double currentWidth = mapPanel.getWidth() > 0 ? mapPanel.getWidth() : mapPanel.getPrefWidth();
        double targetWidth = mapCollapsed ? 62 : 380;

        Timeline widthAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(mapPanel.prefWidthProperty(), currentWidth)),
                new KeyFrame(Duration.millis(220), new KeyValue(mapPanel.prefWidthProperty(), targetWidth))
        );

        if (mapCollapsed) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), mapWebView);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                mapWebView.setVisible(false);
                mapWebView.setManaged(false);
            });
            fadeOut.play();

            mapToggleButton.setText("⟩");
            widthAnimation.play();
        } else {
            mapWebView.setManaged(true);
            mapWebView.setVisible(true);
            mapWebView.setOpacity(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), mapWebView);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            mapToggleButton.setText("⟨");
            widthAnimation.play();
            Platform.runLater(this::invalidateMapSize);
        }
    }

    private void refreshMapMarkers() {
        if (!mapReady || mapEngine == null) return;
        String markersJson = buildMarkersJson(new ArrayList<>(filteredAccommodations));
        mapEngine.executeScript("setMarkers(" + markersJson + ");");
        invalidateMapSize();
    }

    private void invalidateMapSize() {
        if (!mapReady || mapEngine == null) return;
        try {
            mapEngine.executeScript("if (typeof invalidateMapSize === 'function') { invalidateMapSize(); }");
        } catch (Exception ignored) {
            // Map might still be initializing in WebView; next render pass will recover.
        }
    }

    private String buildMarkersJson(List<Accommodation> data) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (Accommodation a : data) {
            if (a.getLatitude() == null || a.getLongitude() == null) continue;
            if (!first) sb.append(',');
            first = false;

            sb.append('{')
                    .append("\"id\":").append(a.getId()).append(',')
                    .append("\"name\":\"").append(escapeJs(a.getName())).append("\",")
                    .append("\"city\":\"").append(escapeJs(a.getCity())).append("\",")
                    .append("\"country\":\"").append(escapeJs(a.getCountry())).append("\",")
                    .append("\"latitude\":").append(a.getLatitude()).append(',')
                    .append("\"longitude\":").append(a.getLongitude())
                    .append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private boolean hasAmenity(Accommodation a, String amenity) {
        String needle = normalize(amenity);
        String fromString = normalize(a.getAccommodationAmenities());
        String fromList = normalize(String.join(",", a.getAmenities() == null ? List.of() : a.getAmenities()));
        return fromString.contains(needle) || fromList.contains(needle);
    }

    private int parseMinStars(String value) {
        if (value == null || value.equalsIgnoreCase("Any")) return 0;
        return switch (value) {
            case "1+" -> 1;
            case "2+" -> 2;
            case "3+" -> 3;
            case "4+" -> 4;
            case "5+" -> 5;
            default -> 0;
        };
    }

    private double estimateNightlyPrice(Accommodation a) {
        // The entity has no public nightly price yet, so we use a deterministic estimate
        // to keep the range filter functional until price fields are introduced.
        return 60 + (a.getStars() * 90);
    }

    private boolean isSelectedForCompare(Accommodation accommodation) {
        return selectedForCompare.stream().anyMatch(a -> a.getId() == accommodation.getId());
    }

    private void updateCompareBar() {
        int count = selectedForCompare.size();
        boolean shouldShow = count > 0;

        if (shouldShow != compareBarVisible) {
            animateCompareBar(shouldShow);
            compareBarVisible = shouldShow;
        }

        compareCountLabel.setText(count + " / 3 selected");
        compareNowButton.setDisable(count < 2);
    }

    private void animateCompareBar(boolean show) {
        if (show) {
            compareBar.setManaged(true);
            compareBar.setVisible(true);
            compareBar.setOpacity(0);
            compareBar.setTranslateY(20);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), compareBar);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            Timeline slideIn = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(compareBar.translateYProperty(), 20)),
                    new KeyFrame(Duration.millis(220), new KeyValue(compareBar.translateYProperty(), 0))
            );

            fadeIn.play();
            slideIn.play();
        } else {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), compareBar);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            Timeline slideOut = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(compareBar.translateYProperty(), 0)),
                    new KeyFrame(Duration.millis(180), new KeyValue(compareBar.translateYProperty(), 14))
            );

            fadeOut.setOnFinished(e -> {
                compareBar.setVisible(false);
                compareBar.setManaged(false);
            });

            fadeOut.play();
            slideOut.play();
        }
    }

    private void animateCardEntry(Node cardNode, int index) {
        cardNode.setOpacity(0);
        cardNode.setTranslateY(12);

        FadeTransition fade = new FadeTransition(Duration.millis(220), cardNode);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(Math.min(index * 35L, 210)));

        Timeline slide = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(cardNode.translateYProperty(), 12)),
                new KeyFrame(Duration.millis(220), new KeyValue(cardNode.translateYProperty(), 0))
        );
        slide.setDelay(Duration.millis(Math.min(index * 35L, 210)));

        fade.play();
        slide.play();
    }

    private void addCardHoverEffects(Node cardNode) {
        cardNode.setOnMouseEntered(event -> {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(140), cardNode);
            scaleUp.setToX(1.015);
            scaleUp.setToY(1.015);
            scaleUp.play();
        });

        cardNode.setOnMouseExited(event -> {
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(140), cardNode);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });
    }

    private void updateResultsLabel() {
        resultsCountLabel.setText(filteredAccommodations.size() + " accommodations found");
    }

    private void handleCompareWithAI() {
        if (selectedForCompare.size() < 2) {
            showWarning("Need at least 2 selections", "Select at least 2 accommodations to compare.");
            return;
        }

        compareNowButton.setDisable(true);
        compareNowButton.setText("Comparing...");

        List<Accommodation> snapshot = new ArrayList<>(selectedForCompare);
        CompletableFuture
                .supplyAsync(() -> compareService.compareAccommodations(snapshot))
                .thenAccept(result -> Platform.runLater(() -> {
                    showCompareResult(result);
                    compareNowButton.setText("Compare Selected");
                    compareNowButton.setDisable(selectedForCompare.size() < 2);
                }));
    }

    private void showCompareResult(AccommodationCompareService.CompareResult result) {
        if (result == null) {
            showWarning("AI Compare Error", "No response from compare service.");
            return;
        }

        if (!result.success) {
            String msg = result.errorMessage == null || result.errorMessage.isBlank()
                    ? "AI comparison failed."
                    : result.errorMessage;
            if (result.rawResponse != null && !result.rawResponse.isBlank()) {
                msg += "\n\n" + result.rawResponse;
            }
            showWarning("AI Compare Error", msg);
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("AI Accommodation Comparison");
        dialog.setHeaderText("Comparison Insights");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: linear-gradient(to bottom right, #F8FAFC, #EFF6FF);");

        content.getChildren().add(createSectionCard(
                "Quick Summary",
                List.of(result.quickSummary == null || result.quickSummary.isBlank()
                        ? "No summary generated."
                        : result.quickSummary)
        ));

        if (!result.ranking.isEmpty()) {
            VBox rankingBox = new VBox(8);
            for (AccommodationCompareService.RankingEntry entry : result.ranking) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(8, 10, 8, 10));
                row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #DBEAFE; -fx-border-radius: 10;");

                Label rank = new Label("#" + entry.rank);
                rank.setStyle("-fx-background-color: linear-gradient(to right, #1E3A8A, #2563EB); -fx-text-fill: white; -fx-padding: 4 9; -fx-background-radius: 999; -fx-font-weight: 800;");

                Label nameReason = new Label((entry.name == null ? "Accommodation" : entry.name) +
                        " — " +
                        (entry.reason == null ? "" : entry.reason));
                nameReason.setWrapText(true);
                nameReason.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 13; -fx-font-weight: 600;");

                row.getChildren().addAll(rank, nameReason);
                rankingBox.getChildren().add(row);
            }

            VBox rankingCard = new VBox(10);
            rankingCard.getChildren().addAll(sectionTitle("Ranking"), rankingBox);
            rankingCard.setPadding(new Insets(12));
            rankingCard.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFFFFF, #ECFEFF); -fx-background-radius: 14; -fx-border-color: #BAE6FD; -fx-border-radius: 14;");
            content.getChildren().add(rankingCard);
        }

        if (!result.accommodationInsights.isEmpty()) {
            VBox insightsCard = new VBox(10);
            insightsCard.getChildren().add(sectionTitle("Strengths & Weaknesses"));

            for (AccommodationCompareService.AccommodationInsight insight : result.accommodationInsights) {
                VBox card = new VBox(6);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #DBEAFE; -fx-border-radius: 12;");

                Label name = new Label(insight.name == null ? "Accommodation" : insight.name);
                name.setStyle("-fx-text-fill: #1E3A8A; -fx-font-size: 14; -fx-font-weight: 800;");

                List<String> strengths = insight.strengths.isEmpty() ? List.of("No strengths listed.") : insight.strengths;
                List<String> weaknesses = insight.weaknesses.isEmpty() ? List.of("No weaknesses listed.") : insight.weaknesses;
                List<String> bestFor = insight.bestFor.isEmpty() ? List.of("General travelers") : insight.bestFor;

                card.getChildren().add(name);
                card.getChildren().add(sectionLine("Strengths", strengths, "#0F766E"));
                card.getChildren().add(sectionLine("Weaknesses", weaknesses, "#B91C1C"));
                card.getChildren().add(sectionLine("Best For", bestFor, "#1E3A8A"));
                insightsCard.getChildren().add(card);
            }

            insightsCard.setPadding(new Insets(12));
            insightsCard.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFFFFF, #EFF6FF); -fx-background-radius: 14; -fx-border-color: #BFDBFE; -fx-border-radius: 14;");
            content.getChildren().add(insightsCard);
        }

        if (!result.bestForCategories.isEmpty()) {
            VBox bestForCard = new VBox(8);
            bestForCard.getChildren().add(sectionTitle("Best For Categories"));
            for (AccommodationCompareService.BestForCategory category : result.bestForCategories) {
                Label line = new Label(
                        "• " + safeText(category.category, "Category") +
                                ": " + safeText(category.recommended, "N/A") +
                                " — " + safeText(category.why, "")
                );
                line.setWrapText(true);
                line.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 13; -fx-font-weight: 600;");
                bestForCard.getChildren().add(line);
            }
            bestForCard.setPadding(new Insets(12));
            bestForCard.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFFFFF, #F0FDFA); -fx-background-radius: 14; -fx-border-color: #99F6E4; -fx-border-radius: 14;");
            content.getChildren().add(bestForCard);
        }

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(920);
        scrollPane.setPrefViewportHeight(640);
        scrollPane.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefSize(980, 720);
        dialog.showAndWait();
    }

    private Label sectionTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #1E3A8A; -fx-font-size: 16; -fx-font-weight: 900;");
        return label;
    }

    private VBox createSectionCard(String title, List<String> lines) {
        VBox card = new VBox(8);
        card.getChildren().add(sectionTitle(title));
        for (String line : lines) {
            Label text = new Label(line);
            text.setWrapText(true);
            text.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 13; -fx-font-weight: 600;");
            card.getChildren().add(text);
        }
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFFFFF, #EFF6FF); -fx-background-radius: 14; -fx-border-color: #BFDBFE; -fx-border-radius: 14;");
        return card;
    }

    private VBox sectionLine(String label, List<String> values, String color) {
        VBox box = new VBox(2);
        Label title = new Label(label + ":");
        title.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12; -fx-font-weight: 800;");
        box.getChildren().add(title);
        for (String value : values) {
            Label item = new Label("• " + value);
            item.setWrapText(true);
            item.setStyle("-fx-text-fill: #334155; -fx-font-size: 12;");
            box.getChildren().add(item);
        }
        return box;
    }

    private String safeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String escapeJs(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    public class MapBridge {
        public void onMarkerClicked(String accommodationId) {
            Platform.runLater(() -> {
                try {
                    int id = Integer.parseInt(accommodationId);
                    allAccommodations.stream()
                            .filter(a -> a.getId() == id)
                            .findFirst()
                            .ifPresent(a -> searchField.setText(a.getName()));
                } catch (NumberFormatException ignored) {
                    // No-op for malformed IDs from JS.
                }
            });
        }
    }

    public void openAccommodationDetails(Accommodation accommodation) {
        if (accommodation == null || searchField == null || searchField.getScene() == null) return;
        try {
            ensureCurrentUserLoaded();
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/user/AccommodationDetailsView.fxml"))
            );
            Parent root = loader.load();
            AccommodationDetailsController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setAccommodation(accommodation);
            searchField.getScene().setRoot(root);
        } catch (Exception e) {
            showWarning("Navigation error", "Unable to open accommodation details.");
        }
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        if (currentUser != null && currentUser.getUserId() > 0) {
            SessionManager.setCurrentUserId(currentUser.getUserId());
        }
        if (avatarInitials != null) {
            if (currentUser == null) {
                avatarInitials.setText("");
                avatarInitials.setGraphic(null);
            } else {
                String first = currentUser.getFirstName() != null && !currentUser.getFirstName().isBlank()
                        ? currentUser.getFirstName().substring(0, 1).toUpperCase() : "";
                String last = currentUser.getLastName() != null && !currentUser.getLastName().isBlank()
                        ? currentUser.getLastName().substring(0, 1).toUpperCase() : "";
                String initials = (first + last).isBlank() ? "U" : (first + last);
                avatarInitials.setText(initials);
                applyAvatarGraphic();
            }
        }
        if (userNameLabel != null) {
            userNameLabel.setText(currentUser == null ? "" : (safeName(currentUser.getFirstName()) + " " + safeName(currentUser.getLastName())).trim());
        }
    }

    @FXML
    private void handleHomeNav(MouseEvent event) {
        navigate("/fxml/user/home.fxml");
    }

    @FXML
    private void handleDestinationsNav(ActionEvent event) {
        navigate("/fxml/user/user_destinations.fxml");
    }

    @FXML
    private void handleDestinationsNav(MouseEvent event) {
        navigate("/fxml/user/user_destinations.fxml");
    }

    @FXML
    private void handleActivitiesNav(MouseEvent event) {
        navigate("/fxml/user/user_activities.fxml");
    }

    @FXML
    private void handleProfile(MouseEvent event) {
        navigate("/fxml/user/profile.fxml");
    }

    @FXML
    private void handleTransportNav(MouseEvent event) {
        navigate("/fxml/user/TransportUserInterface.fxml");
    }

    @FXML
    private void handleBlogNav(MouseEvent event) {
        showWarning("Coming soon", "Blog module navigation is not available yet.");
    }

    @FXML
    private void handleTransportNav(ActionEvent event) {
        showWarning("Coming soon", "Transport module navigation is not available yet.");
    }

    @FXML
    private void handlePacksOffersNav(MouseEvent event) {
        navigate("/fxml/user/UserPacksOffersView.fxml");
    }

    @FXML
    private void handleOffersNav(ActionEvent event) {
        navigate("/fxml/user/UserPacksOffersView.fxml");
    }

    @FXML
    private void handleBlogNav(ActionEvent event) {
        showWarning("Coming soon", "Blog module navigation is not available yet.");
    }

    @FXML
    private void handleMyBookings(ActionEvent event) {
        navigate("/fxml/user/my_bookings.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.setCurrentUserId(-1);
        currentUser = null;
        navigate("/fxml/user/login.fxml");
    }

    private void navigate(String fxmlPath) {
        if (searchField == null || searchField.getScene() == null) return;
        try {
            ensureCurrentUserLoaded();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object targetController = loader.getController();

            if (currentUser != null) {
                if (targetController instanceof UserDestinationsController destinationsController) {
                    destinationsController.setCurrentUser(currentUser);
                } else if (targetController instanceof UserActivitiesController activitiesController) {
                    activitiesController.setCurrentUser(currentUser);
                } else if (targetController instanceof UserBookingsController bookingsController) {
                    bookingsController.setCurrentUser(currentUser);
                } else if (targetController instanceof HomeController homeController) {
                    homeController.setUser(currentUser);
                } else if (targetController instanceof AccommodationsController accommodationsController) {
                    accommodationsController.setCurrentUser(currentUser);
                } else if (targetController instanceof ProfileController profileController) {
                    profileController.setUser(currentUser);
                } else if (targetController instanceof TransportUserInterfaceController transportController) {
                    transportController.setCurrentUser(currentUser);
                } else if (targetController instanceof UserPacksOffersController packsOffersController) {
                    packsOffersController.setCurrentUser(currentUser);
                }
            }

            Parent currentRoot = searchField.getScene().getRoot();
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(Duration.millis(140), currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                searchField.getScene().setRoot(root);
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.millis(170), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (Exception e) {
            showWarning("Navigation error", "Unable to open " + fxmlPath + ".");
        }
    }

    private String safeName(String text) {
        return text == null ? "" : text.trim();
    }

    private void applyAvatarGraphic() {
        if (avatarInitials == null || currentUser == null) return;
        String avatarId = currentUser.getAvatarId();
        if (avatarId == null || !avatarId.contains(":")) {
            avatarInitials.setGraphic(null);
            return;
        }
        String[] parts = avatarId.split(":");
        if (parts.length < 2) {
            avatarInitials.setGraphic(null);
            return;
        }
        String url = "https://api.dicebear.com/9.x/" + parts[0] + "/png?seed=" + parts[1] + "&size=40";
        Image image = new Image(url, 36, 36, true, true, true);
        ImageView view = new ImageView(image);
        view.setFitWidth(36);
        view.setFitHeight(36);
        view.setPreserveRatio(true);
        avatarInitials.setText("");
        avatarInitials.setGraphic(view);
    }

    private void ensureCurrentUserLoaded() {
        if (currentUser != null) {
            return;
        }
        int userId = SessionManager.getCurrentUserId();
        if (userId > 0) {
            User sessionUser = userService.findById(userId);
            if (sessionUser != null) {
                setCurrentUser(sessionUser);
            }
        }
    }
}
