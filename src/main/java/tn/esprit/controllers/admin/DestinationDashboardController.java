package tn.esprit.controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class DestinationDashboardController {

    // Sidebar components
    @FXML private VBox sidebar;
    @FXML private Button sidebarToggle;
    @FXML private Button sidebarOpenButton;

    // Menu headers
    @FXML private HBox dashboardHeader;
    @FXML private HBox destinationsHeader;

    // Menu toggles
    @FXML private Button dashboardToggle;
    @FXML private Button destinationsToggle;

    // Menu containers
    @FXML private VBox dashboardMenu;
    @FXML private VBox destinationsMenu;

    // Navigation buttons
    @FXML private Button overviewBtn;
    @FXML private Button manageDestinationsBtn;
    @FXML private Button manageActivitiesBtn;
    @FXML private Button manageBookingsBtn;

    // Header components
    @FXML private Button headerSettingsBtn;
    @FXML private MenuButton profileDropdown;
    @FXML private MenuItem logoutMenuItem;
    @FXML private Label avatarText;
    @FXML private Button logoutBtn;

    // Theme and language
    @FXML private Label themeIcon;
    @FXML private ToggleButton darkModeToggle;
    @FXML private ComboBox<String> languageSelector;

    // Breadcrumb
    @FXML private Label breadcrumb1;
    @FXML private Label breadcrumb2;

    // Main content area
    @FXML private StackPane mainContent;

    private boolean isDarkMode = false;
    private boolean isSidebarCollapsed = false;

    @FXML
    public void initialize() {
        setupSidebarToggle();
        setupNavigation();
        setupHeaderActions();
        setupTheme();
        setupLanguageSelector();

        // Load overview by default
        showOverview();
    }

    private void setupSidebarToggle() {
        if (sidebarToggle != null) {
            sidebarToggle.setOnAction(event -> collapseSidebar());
        }
        if (sidebarOpenButton != null) {
            sidebarOpenButton.setOnAction(event -> expandSidebar());
            sidebarOpenButton.setVisible(false);
            sidebarOpenButton.setManaged(false);
        }
    }

    private void collapseSidebar() {
        isSidebarCollapsed = true;
        if (sidebar != null) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
        }
        if (sidebarOpenButton != null) {
            sidebarOpenButton.setVisible(true);
            sidebarOpenButton.setManaged(true);
        }
    }

    private void expandSidebar() {
        isSidebarCollapsed = false;
        if (sidebar != null) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }
        if (sidebarOpenButton != null) {
            sidebarOpenButton.setVisible(false);
            sidebarOpenButton.setManaged(false);
        }
    }

    // Menu toggle methods
    @FXML
    private void toggleDashboardMenu(MouseEvent event) {
        toggleMenu(dashboardMenu, dashboardToggle);
    }

    @FXML
    private void toggleDestinationsMenu(MouseEvent event) {
        toggleMenu(destinationsMenu, destinationsToggle);
    }

    private void toggleMenu(VBox menu, Button toggleButton) {
        if (menu == null || toggleButton == null) return;

        boolean isVisible = menu.isVisible();
        menu.setVisible(!isVisible);
        menu.setManaged(!isVisible);
        toggleButton.setText(!isVisible ? "▼" : "▶");
    }

    private void setupNavigation() {
        // Overview uses the SAME overview.fxml from admin folder
        overviewBtn.setOnAction(e -> showOverview());

        // Destination module pages
        manageDestinationsBtn.setOnAction(e -> showDestinationManagement());
        manageActivitiesBtn.setOnAction(e -> showActivityManagement());
        manageBookingsBtn.setOnAction(e -> showBookingManagement());
    }

    private void setupHeaderActions() {
        // Settings button does nothing
        if (headerSettingsBtn != null) {
            headerSettingsBtn.setOnAction(e -> {
                System.out.println("Settings clicked - to be implemented by team member");
            });
        }

        // Profile dropdown items do nothing (except logout)
        if (logoutMenuItem != null) {
            logoutMenuItem.setOnAction(e -> handleLogout());
        }

        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> handleLogout());
        }
    }

    private void setActiveButton(Button button) {
        resetAllMenuItems();
        if (button != null) {
            button.getStyleClass().add("active");
        }
        updateBreadcrumb(button);
    }

    private void resetAllMenuItems() {
        Button[] allButtons = {overviewBtn, manageDestinationsBtn, manageActivitiesBtn, manageBookingsBtn};
        for (Button btn : allButtons) {
            btn.getStyleClass().remove("active");
        }
    }

    private void updateBreadcrumb(Button button) {
        if (button == overviewBtn) {
            breadcrumb1.setText("Dashboard");
            breadcrumb2.setText("Overview");
        } else if (button == manageDestinationsBtn) {
            breadcrumb1.setText("Destinations");
            breadcrumb2.setText("Manage Destinations");
        } else if (button == manageActivitiesBtn) {
            breadcrumb1.setText("Destinations");
            breadcrumb2.setText("Manage Activities");
        } else if (button == manageBookingsBtn) {
            breadcrumb1.setText("Destinations");
            breadcrumb2.setText("Manage Bookings");
        }
    }

    // IMPORTANT: This loads the SAME overview.fxml from admin folder
    private void showOverview() {
        setActiveButton(overviewBtn);
        try {
            // Load the shared overview.fxml from admin folder
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/overview.fxml"));
            Node overviewView = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(overviewView);
        } catch (IOException e) {
            e.printStackTrace();
            showPlaceholder("Overview Module - Stats from all modules will appear here");
        }
    }

    private void showDestinationManagement() {
        setActiveButton(manageDestinationsBtn);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/destination/destination_management.fxml"));
            Node view = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            showPlaceholder("Destination Management Module\n\nError loading: " + e.getMessage());
        }
    }

    private void showActivityManagement() {
        setActiveButton(manageActivitiesBtn);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/destination/activity_management.fxml"));
            Node view = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            showPlaceholder("Activity Management Module\n\nYour CRUD goes here");
        }
    }

    private void showBookingManagement() {
        setActiveButton(manageBookingsBtn);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/destination/booking_management.fxml"));
            Node view = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
            showPlaceholder("Booking Management Module\n\nYour CRUD goes here");
        }
    }

    private void showPlaceholder(String text) {
        mainContent.getChildren().clear();
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Poppins'; -fx-text-alignment: center;");
        label.setWrapText(true);
        StackPane.setAlignment(label, javafx.geometry.Pos.CENTER);
        mainContent.getChildren().add(label);
    }

    private void setupTheme() {
        if (darkModeToggle == null) return;

        darkModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isDarkMode = newVal;
            if (sidebar != null && sidebar.getScene() != null) {
                if (sidebar.getScene().getRoot() instanceof StackPane root) {
                    if (isDarkMode) {
                        root.getStyleClass().add("dark-mode");
                        themeIcon.setText("🌙");
                    } else {
                        root.getStyleClass().remove("dark-mode");
                        themeIcon.setText("☀️");
                    }
                }
            }
        });
    }

    private void setupLanguageSelector() {
        if (languageSelector == null) return;
        if (languageSelector.getValue() == null) {
            languageSelector.setValue("English");
        }
    }

    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) (profileDropdown != null ?
                    profileDropdown.getScene().getWindow() :
                    sidebar.getScene().getWindow());

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            stage.setScene(scene);
            stage.setWidth(1320);
            stage.setHeight(820);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}