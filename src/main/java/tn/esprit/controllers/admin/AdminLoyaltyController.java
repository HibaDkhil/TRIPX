package tn.esprit.controllers.admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.LoyaltyPoints;
import tn.esprit.services.LoyaltyPointsService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminLoyaltyController {

    @FXML private FlowPane loyaltyGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> levelFilter;
    @FXML private Button btnClearFilters;
    @FXML private Label bronzeCount, silverCount, goldCount;

    private LoyaltyPointsService loyaltyService;
    
    private List<LoyaltyPoints> allLoyalty;
    private tn.esprit.entities.User currentUser;
    private String userRole;

    public void initialize() {
        loyaltyService = new LoyaltyPointsService();
        
        setupFilters();
        loadLoyaltyData();
        updateStats();
        
        btnClearFilters.setOnAction(e -> clearFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> filterLoyalty());
    }

    public void setCurrentUser(tn.esprit.entities.User user) {
        this.currentUser = user;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    private void setupFilters() {
        levelFilter.setItems(FXCollections.observableArrayList("All", "BRONZE", "SILVER", "GOLD"));
        levelFilter.setValue("All");
        levelFilter.setOnAction(e -> filterLoyalty());
    }

    private void loadLoyaltyData() {
        try {
            allLoyalty = loyaltyService.afficherList();
            displayLoyalty(allLoyalty);
        } catch (SQLException e) {
            showError("Failed to load loyalty data: " + e.getMessage());
        }
    }

    private void displayLoyalty(List<LoyaltyPoints> loyaltyList) {
        loyaltyGrid.getChildren().clear();
        
        for (LoyaltyPoints lp : loyaltyList) {
            loyaltyGrid.getChildren().add(createLoyaltyCard(lp));
        }
    }

    private VBox createLoyaltyCard(LoyaltyPoints lp) {
        VBox card = new VBox(16);
        card.setPrefWidth(300);
        
        LoyaltyPoints.Level level = lp.computeLevel();
        
        String bgColor = switch (level) {
            case GOLD -> "linear-gradient(to bottom right, #FFD700, #FFA500)";
            case SILVER -> "linear-gradient(to bottom right, #C0C0C0, #A8A8A8)";
            default -> "linear-gradient(to bottom right, #CD7F32, #B87333)";
        };
        
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16; -fx-padding: 24; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4); -fx-cursor: hand;");
        
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-translate-y: -4; -fx-effect: dropshadow(gaussian, rgba(255,215,0,0.4), 20, 0, 0, 8);");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-translate-y: -4;", ""));
        });
        
        // Medal + User ID
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        String medal = switch (level) {
            case GOLD -> "🥇";
            case SILVER -> "🥈";
            default -> "🥉";
        };
        
        Label medalLabel = new Label(medal);
        medalLabel.setStyle("-fx-font-size: 36px;");
        
        VBox userInfo = new VBox(4);
        Label userId = new Label("User #" + lp.getUserId());
        userId.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label levelLabel = new Label(level.name());
        levelLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.9); -fx-font-weight: 600;");
        
        userInfo.getChildren().addAll(userId, levelLabel);
        
        header.getChildren().addAll(medalLabel, userInfo);
        
        // Points
        HBox pointsBox = new HBox(8);
        pointsBox.setAlignment(Pos.CENTER_LEFT);
        pointsBox.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10; -fx-padding: 12;");
        
        Label pointsLabel = new Label(lp.getTotalPoints() + " points");
        pointsLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        pointsBox.getChildren().add(pointsLabel);

//        // Discount
//        Label discountLabel = new Label("💰 " + lp.getLoyaltyDiscountPercent() + "% discount");
//        discountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.95); -fx-font-weight: 600;");

//        // Progress to next level
//        VBox progressBox = new VBox(6);
//        progressBox.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10; -fx-padding: 12;");
//
//        if (level == LoyaltyPoints.Level.GOLD) {
//            Label maxLevel = new Label("✨ Max Level Reached");
//            maxLevel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.9);");
//            progressBox.getChildren().add(maxLevel);
//        } else {
//            int currentLevelPoints = lp.getTotalPoints() % 200;
//            Label progress = new Label("Next level: " + currentLevelPoints + " / 200 pts");
//            progress.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.9);");
//
//            ProgressBar bar = new ProgressBar((double) currentLevelPoints / 200);
//            bar.setMaxWidth(Double.MAX_VALUE);
//            bar.setStyle("-fx-accent: white;");
//
//            progressBox.getChildren().addAll(progress, bar);
//        }
//
        // Actions
        Button adjustBtn = new Button("✏ Adjust Points");
        adjustBtn.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-weight: 600; " +
                         "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        adjustBtn.setMaxWidth(Double.MAX_VALUE);
        adjustBtn.setOnAction(e -> handleAdjustPoints(lp));
        
        card.getChildren().addAll(header, pointsBox, adjustBtn);
        
        return card;
    }

    private void updateStats() {
        if (allLoyalty == null || allLoyalty.isEmpty()) {
            bronzeCount.setText("0");
            silverCount.setText("0");
            goldCount.setText("0");
            return;
        }
        
        int bronze = 0, silver = 0, gold = 0;
        
        for (LoyaltyPoints lp : allLoyalty) {
            switch (lp.computeLevel()) {
                case BRONZE -> bronze++;
                case SILVER -> silver++;
                case GOLD -> gold++;
            }
        }
        
        bronzeCount.setText(String.valueOf(bronze));
        silverCount.setText(String.valueOf(silver));
        goldCount.setText(String.valueOf(gold));
    }

    private void filterLoyalty() {
        if (allLoyalty == null) return;

        List<LoyaltyPoints> filtered = allLoyalty.stream().filter(lp -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                if (!String.valueOf(lp.getUserId()).contains(searchText)) {
                    return false;
                }
            }

            if (!levelFilter.getValue().equals("All")) {
                if (!lp.computeLevel().name().equals(levelFilter.getValue())) {
                    return false;
                }
            }

            return true;
        }).toList();

        displayLoyalty(filtered);
    }

    private void clearFilters() {
        searchField.clear();
        levelFilter.setValue("All");
        loadLoyaltyData();
    }

    private void handleAdjustPoints(LoyaltyPoints lp) {
        if (lp == null) return;
        
        TextInputDialog dialog = new TextInputDialog(String.valueOf(lp.getTotalPoints()));
        dialog.setTitle("Adjust Points");
        dialog.setHeaderText("Adjust Points for User #" + lp.getUserId());
        dialog.setContentText("New Total Points:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(pointsStr -> {
            try {
                int newPoints = Integer.parseInt(pointsStr);
                if (newPoints >= 0) {
                    lp.setTotalPoints(newPoints);
                    loyaltyService.modifier(lp);
                    loadLoyaltyData();
                    updateStats();
                    showInfo("Points adjusted successfully!");
                } else {
                    showError("Points must be >= 0");
                }
            } catch (NumberFormatException e) {
                showError("Invalid number format");
            } catch (SQLException e) {
                showError("Failed to adjust points: " + e.getMessage());
            }
        });
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}
