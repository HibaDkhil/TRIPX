package tn.esprit.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class EnvConfig {
    private static final Map<String, String> CACHE = new HashMap<>();
    private static boolean loaded = false;

    private EnvConfig() {}

    public static String get(String key) {
        if (!loaded) load();
        String fromSystem = System.getenv(key);
        if (fromSystem != null && !fromSystem.isBlank()) return fromSystem;
        return CACHE.get(key);
    }

    private static synchronized void load() {
        if (loaded) return;
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try (BufferedReader br = Files.newBufferedReader(envPath)) {
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int idx = trimmed.indexOf('=');
                    if (idx <= 0) continue;
                    String k = trimmed.substring(0, idx).trim();
                    String v = trimmed.substring(idx + 1).trim();
                    if (!k.isEmpty()) CACHE.put(k, v);
                }
            } catch (IOException ignored) {
                // Keep startup resilient when .env is missing or unreadable.
            }
        }
        loaded = true;
    }
}
