package tn.esprit.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {

    private static final Map<String, String> envVars = new HashMap<>();

    static {
        loadEnvFile();
    }

    private static void loadEnvFile() {
        try {
            Path path = Paths.get(".env");
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        envVars.put(parts[0].trim(), parts[1].trim());
                        System.out.println("✅ Loaded: " + parts[0].trim());
                    }
                }
            } else {
                System.out.println("⚠️ No .env file found. Using system environment variables.");
            }
        } catch (IOException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
        }
    }

    public static String get(String key) {
        // 1. Try .env file first
        String value = envVars.get(key);
        if (value != null && !value.isEmpty()) return value;

        // 2. Try system environment variables
        value = System.getenv(key);
        if (value != null && !value.isEmpty()) return value;

        // 3. Try system properties (for IDE configurations)
        value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;

        System.err.println("⚠️ Warning: " + key + " not found in any source!");
        return "";
    }

    // Convenience methods
    public static String getGeminiKey() {
        return get("GEMINI_API_KEY");
    }

    public static String getWeatherKey() {
        return get("WEATHER_API_KEY");
    }

    public static String getEmailKey() {
        return get("EMAIL_API_KEY");
    }
}