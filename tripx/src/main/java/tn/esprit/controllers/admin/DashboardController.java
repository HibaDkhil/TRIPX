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
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.io.IOException;
import java.util.prefs.Preferences;

public class DashboardController {

    // Sidebar components
    @FXML private VBox sidebar;
    @FXML private Button sidebarToggle;
    @FXML private Button sidebarOpenButton;

    // Menu headers (for click events)
    @FXML private HBox dashboardHeader;
    @FXML private HBox usersHeader;
    @FXML private HBox accommodationsHeader;
    @FXML private HBox destinationsHeader;
    @FXML private HBox transportHeader;
    @FXML private HBox offersHeader;
    @FXML private HBox blogHeader;

    // Menu toggles (triangles)
    @FXML private Button dashboardToggle;
    @FXML private Button usersToggle;
    @FXML private Button accommodationsToggle;
    @FXML private Button destinationsToggle;
    @FXML private Button transportToggle;
    @FXML private Button offersToggle;
    @FXML private Button blogToggle;

    // Menu containers
    @FXML private VBox dashboardMenu;
    @FXML private VBox usersMenu;
    @FXML private VBox accommodationsMenu;
    @FXML private VBox destinationsMenu;
    @FXML private VBox transportMenu;
    @FXML private VBox offersMenu;
    @FXML private VBox blogMenu;

    // Menu buttons (for navigation)
    @FXML private Button overviewBtn;
    @FXML private Button usersBtn;
    @FXML private Button destinationsBtn;
    @FXML private Button accommodationsBtn;
    @FXML private Button transportBtn;
    @FXML private Button offersBtn;
    @FXML private Button blogBtn;

    // Header components
    @FXML private Button headerSettingsBtn;
    @FXML private MenuButton profileDropdown;
    @FXML private MenuItem profileMenuItem;
    @FXML private MenuItem settingsMenuItem;
    @FXML private MenuItem logoutMenuItem;
    @FXML private Label avatarText;

    // Theme and language
    @FXML private Label themeIcon;
    @FXML private ToggleButton darkModeToggle;
    @FXML private ComboBox<String> languageSelector;

    // Breadcrumb
    @FXML private Label breadcrumb1;
    @FXML private Label breadcrumb2;

    // Main content area
    @FXML private StackPane mainContent;

    // Logout button in sidebar
    @FXML private Button logoutBtn;

    private String role;
    private User currentUser;
    private boolean isDarkMode = false;
    private boolean isSidebarCollapsed = false;

    public void setRole(String role) {
        this.role = role;
        configureSidebar();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateProfileInfo();
    }

    private void updateProfileInfo() {
        if (currentUser != null && avatarText != null) {
            String initials = currentUser.getFirstName().substring(0, 1) +
                    currentUser.getLastName().substring(0, 1);
            avatarText.setText(initials.toUpperCase());
            profileDropdown.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
        }
    }

    private void configureSidebar() {
        // For now, show ALL buttons
        usersBtn.setVisible(true);
        destinationsBtn.setVisible(true);
        accommodationsBtn.setVisible(true);
        transportBtn.setVisible(true);
        offersBtn.setVisible(true);
        blogBtn.setVisible(true);
    }

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

    // ============ MENU TOGGLE METHODS - CLICK ON WHOLE HEADER ============
    @FXML
    private void toggleDashboardMenu(MouseEvent event) {
        toggleMenu(dashboardMenu, dashboardToggle);
    }

    @FXML
    private void toggleUsersMenu(MouseEvent event) {
        toggleMenu(usersMenu, usersToggle);
    }

    @FXML
    private void toggleAccommodationsMenu(MouseEvent event) {
        toggleMenu(accommodationsMenu, accommodationsToggle);
    }

    @FXML
    private void toggleDestinationsMenu(MouseEvent event) {
        toggleMenu(destinationsMenu, destinationsToggle);
    }

    @FXML
    private void toggleTransportMenu(MouseEvent event) {
        toggleMenu(transportMenu, transportToggle);
    }

    @FXML
    private void toggleOffersMenu(MouseEvent event) {
        toggleMenu(offersMenu, offersToggle);
    }

    @FXML
    private void toggleBlogMenu(MouseEvent event) {
        toggleMenu(blogMenu, blogToggle);
    }

    private void toggleMenu(VBox menu, Button toggleButton) {
        if (menu == null || toggleButton == null) return;

        boolean isVisible = menu.isVisible();
        menu.setVisible(!isVisible);
        menu.setManaged(!isVisible);
        toggleButton.setText(!isVisible ? "▼" : "▶");
    }

    private void setupNavigation() {
        overviewBtn.setOnAction(e -> showOverview());
        usersBtn.setOnAction(e -> showUsers());
        destinationsBtn.setOnAction(e -> showDestinations());
        accommodationsBtn.setOnAction(e -> showAccommodations());
        transportBtn.setOnAction(e -> showTransport());
        offersBtn.setOnAction(e -> showOffers());
        blogBtn.setOnAction(e -> showBlog());
    }

    private void setupHeaderActions() {
        // Settings button click (opens Settings with My Claims tab - index 0)
        if (headerSettingsBtn != null) {
            headerSettingsBtn.setOnAction(e -> showSettings(0));
        }

        // Profile dropdown items - CHANGED: profile now opens ProfileAdmin
        if (profileMenuItem != null) {
            profileMenuItem.setOnAction(e -> showProfile()); // ← CHANGED
        }

        if (settingsMenuItem != null) {
            settingsMenuItem.setOnAction(e -> showSettings(0)); // Settings tab - index 0
        }

        // Logout from dropdown
        if (logoutMenuItem != null) {
            logoutMenuItem.setOnAction(e -> handleLogout());
        }

        // Logout from sidebar
        if (logoutBtn != null) {
            logoutBtn.setOnAction(e -> handleLogout());
        }
    }

    private void setActiveButton(Button button) {
        // Reset all menu items
        resetAllMenuItems();

        // Set active class on the clicked button
        if (button != null) {
            button.getStyleClass().add("active");
        }

        // Update breadcrumb
        updateBreadcrumb(button);
    }

    private void resetAllMenuItems() {
        Button[] allButtons = {overviewBtn, usersBtn, destinationsBtn, accommodationsBtn,
                transportBtn, offersBtn, blogBtn};
        for (Button btn : allButtons) {
            btn.getStyleClass().remove("active");
        }
    }

    private void updateBreadcrumb(Button button) {
        if (button == overviewBtn) {
            breadcrumb1.setText("Dashboard");
            breadcrumb2.setText("Overview");
        } else if (button == usersBtn) {
            breadcrumb1.setText("Users");
            breadcrumb2.setText("User Management");
        } else if (button == destinationsBtn) {
            breadcrumb1.setText("Destinations");
            breadcrumb2.setText("All Destinations");
        } else if (button == accommodationsBtn) {
            breadcrumb1.setText("Accommodations");
            breadcrumb2.setText("Manage Accommodations");
        } else if (button == transportBtn) {
            breadcrumb1.setText("Transport");
            breadcrumb2.setText("Manage Transport");
        } else if (button == offersBtn) {
            breadcrumb1.setText("Offers");
            breadcrumb2.setText("Manage Offers");
        } else if (button == blogBtn) {
            breadcrumb1.setText("Blog");
            breadcrumb2.setText("Blog & Community");
        }
    }

    private void showOverview() {
        setActiveButton(overviewBtn);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/overview.fxml"));
            Node overviewView = loader.load();
            mainContent.getChildren().clear();
            mainContent.getChildren().add(overviewView);
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to empty content
            mainContent.getChildren().clear();
            Label label = new Label("Overview Module");
            label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            mainContent.getChildren().add(label);
        }
    }

    private void showUsers() {
        setActiveButton(usersBtn);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/user_management.fxml"));
            Node view = loader.load();

            UserManagementController controller = loader.getController();
            if (currentUser != null) {
                controller.setUserData(currentUser, role);
            }

            mainContent.getChildren().clear();
            mainContent.getChildren().add(view);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading User Management");
        }
    }

    private void showDestinations() {
        setActiveButton(destinationsBtn);
        mainContent.getChildren().clear();
        Label label = new Label("Destination Management Module");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
        mainContent.getChildren().add(label);
    }

    private void showAccommodations() {
        setActiveButton(accommodationsBtn);
        mainContent.getChildren().clear();
        Label label = new Label("Accommodation Management Module");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
        mainContent.getChildren().add(label);
    }

    private void showTransport() {
        setActiveButton(transportBtn);
        mainContent.getChildren().clear();
        Label label = new Label("Transport Management Module");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
        mainContent.getChildren().add(label);
    }

    private void showOffers() {
        setActiveButton(offersBtn);
        mainContent.getChildren().clear();
        Label label = new Label("Offers Management Module");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
        mainContent.getChildren().add(label);
    }

    private void showBlog() {
        setActiveButton(blogBtn);
        mainContent.getChildren().clear();
        Label label = new Label("Blog & Community Module");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Poppins';");
        mainContent.getChildren().add(label);
    }

    // NEW METHOD: Show Profile
    private void showProfile() {
        try {
            // Clear active button from sidebar
            resetAllMenuItems();

            // DEBUG: Check if currentUser exists
            System.out.println("=== DEBUG SHOW PROFILE ===");
            System.out.println("currentUser: " + currentUser);

            if (currentUser != null) {
                System.out.println("User ID: " + currentUser.getUserId());
                System.out.println("User Email: " + currentUser.getEmail());
                System.out.println("User Name: " + currentUser.getFirstName() + " " + currentUser.getLastName());
                System.out.println("User Role: " + role);
            } else {
                System.out.println("ERROR: currentUser is NULL!");
                showError("No user data available!");
                return;
            }

            String fxmlPath = "/fxml/admin/profileAdmin.fxml";
            System.out.println("Loading profile from: " + fxmlPath);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            if (loader.getLocation() == null) {
                System.err.println("ERROR: Cannot find profileAdmin.fxml at " + fxmlPath);
                showError("Profile FXML file not found!");
                return;
            }

            Node profileView = loader.load();

            ProfileAdminController controller = loader.getController();

            // PASS THE USER DATA
            controller.setUserData(currentUser, role);
            System.out.println("User data passed to ProfileAdminController");

            // Update breadcrumb
            breadcrumb1.setText("Admin");
            breadcrumb2.setText("My Profile");

            mainContent.getChildren().clear();
            mainContent.getChildren().add(profileView);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading profile: " + e.getMessage());
        }
    }

    private void showSettings(int selectedTab) {
        try {
            // Clear active button from sidebar (settings not in sidebar)
            resetAllMenuItems();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/settings.fxml"));
            Node settingsView = loader.load();

            SettingsController controller = loader.getController();
            if (currentUser != null) {
                controller.setUserData(currentUser, role, selectedTab); // 3 arguments
            }

            // Update breadcrumb - CHANGED: removed "My Profile" option
            breadcrumb1.setText("Settings");
            breadcrumb2.setText("My Claims");

            mainContent.getChildren().clear();
            mainContent.getChildren().add(settingsView);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading settings");
        }
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
            saveThemePreference(isDarkMode ? "dark" : "light");
        });

        // Load saved preference
        loadThemePreference();
    }

    private void saveThemePreference(String theme) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(DashboardController.class);
            prefs.put("theme", theme);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadThemePreference() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(DashboardController.class);
            String theme = prefs.get("theme", "light");
            isDarkMode = theme.equals("dark");
            darkModeToggle.setSelected(isDarkMode);

            // Update theme icon
            if (themeIcon != null) {
                themeIcon.setText(isDarkMode ? "🌙" : "☀️");
            }
        } catch (Exception e) {
            isDarkMode = false;
        }
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
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean isSidebarCollapsed() {
        return isSidebarCollapsed;
    }
}