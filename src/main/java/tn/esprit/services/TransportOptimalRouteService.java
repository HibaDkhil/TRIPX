package tn.esprit.services;

import tn.esprit.utils.EnvConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransportOptimalRouteService {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Pattern ROUTE_OBJ_PATTERN =
            Pattern.compile("\"routes\"\\s*:\\s*\\[\\s*\\{(.*?)\\}\\s*\\]", Pattern.DOTALL);
    private static final Pattern DISTANCE_PATTERN =
            Pattern.compile("\"distance\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern DURATION_PATTERN =
            Pattern.compile("\"duration\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern AI_CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);

    public String generateTransportOptimalRouteReport(
            double pickupLat, double pickupLon, double dropoffLat, double dropoffLon,
            String pickupAddress, String dropoffAddress
    ) {
        RouteMetrics metrics;
        try {
            metrics = fetchRouteMetrics(pickupLat, pickupLon, dropoffLat, dropoffLon);
        } catch (Exception ex) {
            return "Could not fetch route from routing engine.\nReason: " + ex.getMessage();
        }

        String plainSummary = buildPlainSummary(metrics, pickupAddress, dropoffAddress);
        String apiKey = EnvConfig.get("transportai");
        if (apiKey == null || apiKey.isBlank()) {
            return plainSummary + "\n\nAI note: Missing key 'transportai' in .env.";
        }

        String aiText = askGroq(apiKey, metrics, pickupAddress, dropoffAddress);
        if (aiText == null || aiText.isBlank()) {
            return plainSummary + "\n\nAI note: Groq response unavailable. Showing routing engine result.";
        }
        return aiText + "\n\n---\n" + plainSummary;
    }

    private RouteMetrics fetchRouteMetrics(double pickupLat, double pickupLon, double dropoffLat, double dropoffLon) throws Exception {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + pickupLon + "," + pickupLat + ";" + dropoffLon + "," + dropoffLat
                + "?overview=false&alternatives=false&steps=false";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Routing API HTTP " + response.statusCode());
        }

        String body = response.body();
        Matcher routeMatcher = ROUTE_OBJ_PATTERN.matcher(body);
        if (!routeMatcher.find()) throw new IllegalStateException("No route data found in response.");
        String firstRoute = routeMatcher.group(1);

        Matcher distanceMatcher = DISTANCE_PATTERN.matcher(firstRoute);
        Matcher durationMatcher = DURATION_PATTERN.matcher(firstRoute);
        if (!distanceMatcher.find() || !durationMatcher.find()) {
            throw new IllegalStateException("Route distance/duration missing in response.");
        }

        double distanceMeters = Double.parseDouble(distanceMatcher.group(1));
        double durationSeconds = Double.parseDouble(durationMatcher.group(1));
        return new RouteMetrics(distanceMeters / 1000.0, durationSeconds / 60.0);
    }

    private String askGroq(String apiKey, RouteMetrics metrics, String pickupAddress, String dropoffAddress) {
        try {
            String userPrompt = "Pickup: " + safeText(pickupAddress) + "\n"
                    + "Drop-off: " + safeText(dropoffAddress) + "\n"
                    + "Estimated distance: " + String.format("%.2f km", metrics.distanceKm()) + "\n"
                    + "Estimated duration: " + String.format("%.0f minutes", metrics.durationMinutes()) + "\n\n"
                    + "Give a concise travel recommendation: best route choice, expected travel time, and one practical tip.";

            String body = "{"
                    + "\"model\":\"llama-3.1-8b-instant\","
                    + "\"temperature\":0.2,"
                    + "\"max_tokens\":220,"
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"You are a travel route assistant. Be concise and practical.\"},"
                    + "{\"role\":\"user\",\"content\":\"" + escapeJson(userPrompt) + "\"}"
                    + "]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(18))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;

            Matcher m = AI_CONTENT_PATTERN.matcher(response.body());
            if (!m.find()) return null;
            return unescapeJson(m.group(1)).trim();
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildPlainSummary(RouteMetrics metrics, String pickupAddress, String dropoffAddress) {
        return "Route engine summary\n"
                + "From: " + safeText(pickupAddress) + "\n"
                + "To: " + safeText(dropoffAddress) + "\n"
                + "Distance: " + String.format("%.2f km", metrics.distanceKm()) + "\n"
                + "Fastest time (est.): " + String.format("%.0f minutes", metrics.durationMinutes());
    }

    private String safeText(String value) {
        if (value == null || value.isBlank() || "Address not resolved yet.".equals(value)) return "N/A";
        return value;
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescapeJson(String raw) {
        return raw.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private record RouteMetrics(double distanceKm, double durationMinutes) {}
}
