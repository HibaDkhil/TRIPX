package admin.shell.kit;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Reusable Admin Shell behavior controller.
 * Teammates can keep this class as-is and only inject module content into centerContentHost.
 */
public class AdminShellController {

    @FXML private VBox sidebar;
    @FXML private Button sidebarToggle;
    @FXML private Button sidebarOpenButton;

    @FXML private Button dashboardToggle;
    @FXML private Button usersToggle;
    @FXML private Button accommodationsToggle;
    @FXML private Button destinationsToggle;

    @FXML private VBox dashboardMenu;
    @FXML private VBox usersMenu;
    @FXML private VBox accommodationsMenu;
    @FXML private VBox destinationsMenu;

    @FXML private ToggleButton darkModeToggle;
    @FXML private ComboBox<String> languageSelector;
    @FXML private MenuButton profileDropdown;

    @FXML private StackPane centerContentHost;

    private boolean isSidebarCollapsed = false;

    @FXML
    public void initialize() {
        setupSidebarToggle();
        setupMenuToggles();
        setupThemeToggle();
        setupLanguageSelector();
        setupProfileDropdown();
    }

    public void setCenterContent(Node node) {
        if (centerContentHost == null || node == null) {
            return;
        }
        centerContentHost.getChildren().setAll(node);
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
            animateSidebarHide();
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
            animateSidebarShow();
        }
        if (sidebarOpenButton != null) {
            sidebarOpenButton.setVisible(false);
            sidebarOpenButton.setManaged(false);
        }
    }

    private void animateSidebarHide() {
        if (sidebar == null) return;

        double width = sidebar.getWidth() > 0 ? sidebar.getWidth() : 280;
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(220), sidebar);
        slideOut.setFromX(0);
        slideOut.setToX(-width);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), sidebar);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ParallelTransition hideTransition = new ParallelTransition(slideOut, fadeOut);
        hideTransition.setOnFinished(e -> {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
            sidebar.setTranslateX(0);
            sidebar.setOpacity(1.0);
        });
        hideTransition.play();
    }

    private void animateSidebarShow() {
        if (sidebar == null) return;

        double width = sidebar.getWidth() > 0 ? sidebar.getWidth() : 280;
        sidebar.setTranslateX(-width);
        sidebar.setOpacity(0.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), sidebar);
        slideIn.setFromX(-width);
        slideIn.setToX(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), sidebar);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        new ParallelTransition(slideIn, fadeIn).play();
    }

    private void setupMenuToggles() {
        wireMenuToggle(dashboardToggle, dashboardMenu, true);
        wireMenuToggle(usersToggle, usersMenu, false);
        wireMenuToggle(accommodationsToggle, accommodationsMenu, true);
        wireMenuToggle(destinationsToggle, destinationsMenu, false);
    }

    private void wireMenuToggle(Button toggleButton, VBox menu, boolean visibleInitially) {
        if (toggleButton == null || menu == null) return;

        menu.setVisible(visibleInitially);
        menu.setManaged(visibleInitially);
        toggleButton.setText(visibleInitially ? "▼" : "▶");
        toggleButton.setOnAction(event -> toggleMenu(menu, toggleButton));
    }

    private void toggleMenu(VBox menu, Button toggleButton) {
        boolean isVisible = menu.isVisible();
        toggleButton.setText(isVisible ? "▶" : "▼");

        if (isVisible) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), menu);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), menu);
            scaleOut.setFromY(1);
            scaleOut.setToY(0);

            ParallelTransition hide = new ParallelTransition(fadeOut, scaleOut);
            hide.setOnFinished(e -> {
                menu.setVisible(false);
                menu.setManaged(false);
                menu.setOpacity(1);
                menu.setScaleY(1);
            });
            hide.play();
        } else {
            menu.setManaged(true);
            menu.setVisible(true);
            menu.setOpacity(0);
            menu.setScaleY(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), menu);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(180), menu);
            scaleIn.setFromY(0);
            scaleIn.setToY(1);

            new ParallelTransition(fadeIn, scaleIn).play();
        }
    }

    private void setupThemeToggle() {
        if (darkModeToggle == null) return;
        darkModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (sidebar == null || sidebar.getScene() == null) return;
            if (!(sidebar.getScene().getRoot() instanceof StackPane root)) return;

            if (newVal) {
                if (!root.getStyleClass().contains("dark-mode")) {
                    root.getStyleClass().add("dark-mode");
                }
            } else {
                root.getStyleClass().remove("dark-mode");
            }
        });
    }

    private void setupLanguageSelector() {
        if (languageSelector == null) return;
        if (languageSelector.getValue() == null) {
            languageSelector.setValue("English");
        }
    }

    private void setupProfileDropdown() {
        if (profileDropdown == null || profileDropdown.getItems().isEmpty()) return;
        profileDropdown.getItems().forEach(item -> item.setOnAction(event -> {
            // Hook point: teammates can route profile actions here.
        }));
    }

    public boolean isSidebarCollapsed() {
        return isSidebarCollapsed;
    }
}
