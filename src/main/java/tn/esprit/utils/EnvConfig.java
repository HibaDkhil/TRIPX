package tn.esprit.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads local .env values and provides simple key lookup.
 * Lookup source: local .env file.
 */
public final class EnvConfig {

    private static final String ENV_FILE = ".env";
    private static final String GROQ_PLACEHOLDER = "replace_with_your_groq_api_key";
    private static volatile Map<String, String> cachedValues;

    private EnvConfig() {
    }

    public static String get(String key) {
        String fromDotEnv = loadDotEnv().get(key);
        if (isUsableValue(key, fromDotEnv)) {
            return fromDotEnv;
        }
        return null;
    }

    private static boolean isUsableValue(String key, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if ("GROQ_API_KEY".equals(key) && GROQ_PLACEHOLDER.equals(value.trim())) {
            return false;
        }
        return true;
    }

    private static Map<String, String> loadDotEnv() {
        if (cachedValues != null) {
            return cachedValues;
        }
        synchronized (EnvConfig.class) {
            if (cachedValues == null) {
                cachedValues = parseDotEnv();
            }
        }
        return cachedValues;
    }

    private static Map<String, String> parseDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path envPath = Path.of(ENV_FILE);
        if (!Files.exists(envPath)) {
            return values;
        }

        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String rawLine : lines) {
                if (rawLine == null) {
                    continue;
                }
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String name = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(name, value);
            }
        } catch (IOException ignored) {
            // Ignore .env parsing errors and return empty map.
        }
        return values;
    }
}
