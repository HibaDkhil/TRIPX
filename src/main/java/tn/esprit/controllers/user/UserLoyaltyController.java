package tn.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.LoyaltyPoints;
import tn.esprit.services.LoyaltyPointsService;
import tn.esprit.utils.SessionManager;

import java.sql.SQLException;

public class UserLoyaltyController {

    @FXML private Label lblLevelIcon, lblLevel, lblPoints, lblDiscount;
    @FXML private Label lblProgressText;
    @FXML private ProgressBar progressBar;
    
    private int getCurrentUserId() {
        return SessionManager.getCurrentUserId();
    }

    private LoyaltyPointsService loyaltyService;

    public void initialize() {
        loyaltyService = new LoyaltyPointsService();
        loadLoyaltyStatus();
    }

    private void loadLoyaltyStatus() {
        try {
            LoyaltyPoints loyalty = loyaltyService.getByUserId(getCurrentUserId());

            if (loyalty == null) {
                // First time user - create entry
                loyalty = new LoyaltyPoints(getCurrentUserId());
                loyaltyService.add(loyalty);
            }

            // Update UI
            LoyaltyPoints.Level level = loyalty.computeLevel();
            int points = loyalty.getTotalPoints();

            // Icon
            String icon = level == LoyaltyPoints.Level.GOLD ? "🥇" :
                         level == LoyaltyPoints.Level.SILVER ? "🥈" : "🥉";
            lblLevelIcon.setText(icon);

            // Level name
            lblLevel.setText(level.name());

            // Points
            lblPoints.setText(points + " Points");

            // Discount
            lblDiscount.setText(loyalty.getLoyaltyDiscountPercent() + "%");

            // Progress to next level
            int currentLevelPoints = points % 200; // Points within current level
            int nextLevelThreshold = 200;
            
            if (level == LoyaltyPoints.Level.GOLD) {
                lblProgressText.setText("Max level reached!");
                progressBar.setProgress(1.0);
            } else {
                lblProgressText.setText(currentLevelPoints + " / " + nextLevelThreshold + " points");
                progressBar.setProgress((double) currentLevelPoints / nextLevelThreshold);
            }

        } catch (SQLException e) {
            showError("Failed to load loyalty status: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}
