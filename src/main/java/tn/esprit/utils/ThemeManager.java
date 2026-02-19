package tn.esprit.utils;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {
    private static final String PREF_THEME = "app_theme";
    private static final String DARK_MODE = "dark";
    private static final String LIGHT_MODE = "light";
    
    private static boolean isDarkMode = false;
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    static {
        // Load saved preference
        String savedTheme = prefs.get(PREF_THEME, LIGHT_MODE);
        isDarkMode = DARK_MODE.equals(savedTheme);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        
        String darkModeCss = ThemeManager.class.getResource("/css/dark-mode.css").toExternalForm();
        
        if (isDarkMode) {
            if (!scene.getStylesheets().contains(darkModeCss)) {
                scene.getStylesheets().add(darkModeCss);
            }
        } else {
            scene.getStylesheets().remove(darkModeCss);
        }
    }

    public static void toggleTheme(Scene scene) {
        isDarkMode = !isDarkMode;
        prefs.put(PREF_THEME, isDarkMode ? DARK_MODE : LIGHT_MODE);
        applyTheme(scene);
    }
    
    public static boolean isDarkMode() {
        return isDarkMode;
    }
}
