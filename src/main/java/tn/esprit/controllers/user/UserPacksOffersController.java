package tn.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.utils.SessionManager;

import java.io.IOException;

public class UserPacksOffersController {

    // Top bar
    @FXML private Label avatarInitials;
    @FXML private ImageView userAvatarView;
    @FXML private Label userNameLabel;

    // Sub-tab buttons
    @FXML private Button btnBrowsePacks;
    @FXML private Button btnOffers;
    @FXML private Button btnLoyalty;
    @FXML private Button btnExchange;

    // Content area
    @FXML private StackPane contentArea;

    private User currentUser;

    private static final String TAB_ACTIVE =
            "-fx-background-color: linear-gradient(to right, #6D83F2, #4CCCAD); -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-font-size: 13px; -fx-padding: 12 32 12 32; -fx-cursor: hand; -fx-background-radius: 0;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: transparent; -fx-text-fill: #0A153A; -fx-font-size: 13px; " +
            "-fx-padding: 12 32 12 32; -fx-cursor: hand; -fx-background-radius: 0;";

    @FXML
    public void initialize() {
        showBrowsePacksTab();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null && currentUser.getUserId() > 0) {
            SessionManager.setCurrentUserId(currentUser.getUserId());
        }
        if (currentUser != null) {
            if (userNameLabel != null) {
                String name = safeName(currentUser.getFirstName()) + " " + safeName(currentUser.getLastName());
                userNameLabel.setText(name.trim());
            }
            if (avatarInitials != null) {
                String first = currentUser.getFirstName() != null && !currentUser.getFirstName().isBlank()
                        ? currentUser.getFirstName().substring(0, 1).toUpperCase() : "";
                String last = currentUser.getLastName() != null && !currentUser.getLastName().isBlank()
                        ? currentUser.getLastName().substring(0, 1).toUpperCase() : "";
                avatarInitials.setText((first + last).isBlank() ? "U" : (first + last));
            }
            applyAvatarGraphic();
        }
    }

    private void applyAvatarGraphic() {
        if (avatarInitials == null || currentUser == null) return;
        String avatarId = currentUser.getAvatarId();
        if (avatarId != null && avatarId.contains(":")) {
            String[] parts = avatarId.split(":");
            if (parts.length == 2 && "emoji".equals(parts[0])) {
                avatarInitials.setText(parts[1]);
                if (userAvatarView != null) {
                    userAvatarView.setVisible(false);
                    userAvatarView.setManaged(false);
                }
            } else if (parts.length == 2 && "url".equals(parts[0])) {
                try {
                    if (userAvatarView != null) {
                        userAvatarView.setImage(new Image(parts[1], true));
                        userAvatarView.setVisible(true);
                        userAvatarView.setManaged(true);
                        avatarInitials.setVisible(false);
                        avatarInitials.setManaged(false);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private String safeName(String s) {
        return s == null ? "" : s;
    }

    /* ─── Sub-tab handlers ─── */

    @FXML
    public void showBrowsePacksTab() {
        setActiveTab(btnBrowsePacks);
        loadSubView("/fxml/user/UserBrowsePacks.fxml");
    }

    @FXML
    public void showOffersTab() {
        setActiveTab(btnOffers);
        loadSubView("/fxml/user/UserOffers.fxml");
    }

    @FXML
    public void showLoyaltyTab() {
        setActiveTab(btnLoyalty);
        loadSubView("/fxml/user/UserLoyalty.fxml");
    }

    @FXML
    public void showExchangeTab() {
        setActiveTab(btnExchange);
        loadSubView("/fxml/user/UserExchange.fxml");
    }

    private void setActiveTab(Button active) {
        if (btnBrowsePacks != null) btnBrowsePacks.setStyle(TAB_INACTIVE);
        if (btnOffers != null)       btnOffers.setStyle(TAB_INACTIVE);
        if (btnLoyalty != null)      btnLoyalty.setStyle(TAB_INACTIVE);
        if (btnExchange != null)     btnExchange.setStyle(TAB_INACTIVE);
        if (active != null)          active.setStyle(TAB_ACTIVE);
    }

    private void loadSubView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Could not load view: " + e.getMessage());
        }
    }

    /* ─── Top-bar navigation handlers ─── */

    @FXML
    private void handleHomeNav(MouseEvent event) {
        navigateTo("/fxml/user/home.fxml");
    }

    @FXML
    private void handleDestinationsNav(MouseEvent event) {
        navigateTo("/fxml/user/user_destinations.fxml");
    }

    @FXML
    private void handleAccommodationsNav(MouseEvent event) {
        navigateTo("/fxml/user/AccommodationsView.fxml");
    }

    @FXML
    private void handleActivitiesNav(MouseEvent event) {
        navigateTo("/fxml/user/user_activities.fxml");
    }

    @FXML
    private void handleTransportNav(MouseEvent event) {
        navigateTo("/fxml/user/TransportUserInterface.fxml");
    }

    @FXML
    private void handleBlogNav(MouseEvent event) {
        showAlert("Blog page coming soon!");
    }

    @FXML
    private void handleProfile(MouseEvent event) {
        navigateTo("/fxml/user/profile.fxml");
    }

    @FXML
    private void handleMyBookingsNav(javafx.event.ActionEvent event) {
        navigateTo("/fxml/user/my_bookings.fxml");
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                SessionManager.setCurrentUserId(-1);
                navigateTo("/fxml/user/login.fxml");
            }
        });
    }

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();

            if (currentUser != null) {
                if (controller instanceof HomeController c) {
                    c.setUser(currentUser);
                } else if (controller instanceof UserDestinationsController c) {
                    c.setCurrentUser(currentUser);
                } else if (controller instanceof UserActivitiesController c) {
                    c.setCurrentUser(currentUser);
                } else if (controller instanceof AccommodationsController c) {
                    c.setCurrentUser(currentUser);
                } else if (controller instanceof TransportUserInterfaceController c) {
                    c.setCurrentUser(currentUser);
                } else if (controller instanceof UserBookingsController c) {
                    c.setCurrentUser(currentUser);
                } else if (controller instanceof ProfileController c) {
                    c.setUser(currentUser);
                }
            }

            Stage stage = (Stage) contentArea.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            stage.setScene(new Scene(root, width, height));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation error: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }
}
