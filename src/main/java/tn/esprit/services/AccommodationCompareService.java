package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.entities.Accommodation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AccommodationCompareService {

    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    private final HttpClient httpClient;
    private final Gson gson;

    public AccommodationCompareService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.gson = new Gson();
    }

    public CompareResult compareAccommodations(List<Accommodation> accommodations) {
        CompareResult result = new CompareResult();

        if (accommodations == null || accommodations.size() < 2) {
            result.success = false;
            result.errorMessage = "Please select at least 2 accommodations for comparison.";
            return result;
        }

        String apiKey = System.getenv("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            result.success = false;
            result.errorMessage = "Missing GROQ_API_KEY environment variable.\n" +
                    "Set it in your shell, restart the app, and try again.";
            return result;
        }

        try {
            String prompt = buildPrompt(accommodations);
            String requestJson = buildRequestJson(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_ENDPOINT))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                result.success = false;
                result.errorMessage = "Groq API request failed (" + response.statusCode() + ").";
                result.rawResponse = response.body();
                return result;
            }

            JsonObject root = gson.fromJson(response.body(), JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                result.success = false;
                result.errorMessage = "Groq returned no choices.";
                return result;
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                result.success = false;
                result.errorMessage = "Groq response missing message content.";
                return result;
            }

            String content = message.get("content").getAsString();
            result.rawResponse = content;
            parseStructuredResponse(content, result);
            return result;

        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            result.success = false;
            result.errorMessage = "Groq compare failed: " + ex.getMessage();
            return result;
        } catch (Exception ex) {
            result.success = false;
            result.errorMessage = "Unexpected error while comparing accommodations: " + ex.getMessage();
            return result;
        }
    }

    private String buildRequestJson(String prompt) {
        JsonObject root = new JsonObject();
        root.addProperty("model", MODEL);
        root.addProperty("temperature", 0.2);
        root.addProperty("max_tokens", 900);

        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
                "You are a travel assistant. Always output valid JSON only, no markdown.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");

        root.add("messages", messages);
        root.add("response_format", responseFormat);
        return gson.toJson(root);
    }

    private String buildPrompt(List<Accommodation> accommodations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Compare these accommodations and return a JSON object with this exact shape:\n");
        sb.append("{\n");
        sb.append("  \"quick_summary\": \"...\",\n");
        sb.append("  \"ranking\": [\n");
        sb.append("    {\"rank\":1, \"name\":\"...\", \"reason\":\"...\"}\n");
        sb.append("  ],\n");
        sb.append("  \"accommodation_insights\": [\n");
        sb.append("    {\n");
        sb.append("      \"name\":\"...\",\n");
        sb.append("      \"strengths\":[\"...\"],\n");
        sb.append("      \"weaknesses\":[\"...\"],\n");
        sb.append("      \"best_for\":[\"...\"]\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"best_for_categories\": [\n");
        sb.append("    {\"category\":\"Budget\", \"recommended\":\"...\", \"why\":\"...\"}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("Return only JSON.\n\n");
        sb.append("Accommodations:\n");

        for (int i = 0; i < accommodations.size(); i++) {
            Accommodation a = accommodations.get(i);
            sb.append("\n#").append(i + 1).append(" ")
                    .append(safe(a.getName(), "Unnamed")).append("\n")
                    .append("- Type: ").append(safe(a.getType(), "N/A")).append("\n")
                    .append("- Location: ").append(safe(a.getCity(), "N/A"))
                    .append(", ").append(safe(a.getCountry(), "N/A")).append("\n")
                    .append("- Address: ").append(safe(a.getAddress(), "N/A")).append("\n")
                    .append("- Stars: ").append(a.getStars()).append("\n")
                    .append("- Status: ").append(safe(a.getStatus(), "N/A")).append("\n")
                    .append("- Amenities: ").append(safe(a.getAccommodationAmenities(), "N/A")).append("\n")
                    .append("- Description: ").append(safe(a.getDescription(), "N/A")).append("\n");
        }

        return sb.toString();
    }

    private void parseStructuredResponse(String content, CompareResult result) {
        try {
            String jsonContent = extractJson(content);
            JsonObject obj = gson.fromJson(jsonContent, JsonObject.class);
            if (obj == null) {
                result.success = false;
                result.errorMessage = "AI returned empty JSON.";
                return;
            }

            result.quickSummary = getString(obj, "quick_summary");

            JsonArray rankingArray = obj.getAsJsonArray("ranking");
            if (rankingArray != null) {
                for (int i = 0; i < rankingArray.size(); i++) {
                    JsonObject item = rankingArray.get(i).getAsJsonObject();
                    RankingEntry entry = new RankingEntry();
                    entry.rank = item.has("rank") ? item.get("rank").getAsInt() : (i + 1);
                    entry.name = getString(item, "name");
                    entry.reason = getString(item, "reason");
                    result.ranking.add(entry);
                }
            }

            JsonArray insightsArray = obj.getAsJsonArray("accommodation_insights");
            if (insightsArray != null) {
                for (int i = 0; i < insightsArray.size(); i++) {
                    JsonObject item = insightsArray.get(i).getAsJsonObject();
                    AccommodationInsight insight = new AccommodationInsight();
                    insight.name = getString(item, "name");
                    insight.strengths = toStringList(item.getAsJsonArray("strengths"));
                    insight.weaknesses = toStringList(item.getAsJsonArray("weaknesses"));
                    insight.bestFor = toStringList(item.getAsJsonArray("best_for"));
                    result.accommodationInsights.add(insight);
                }
            }

            JsonArray categoriesArray = obj.getAsJsonArray("best_for_categories");
            if (categoriesArray != null) {
                for (int i = 0; i < categoriesArray.size(); i++) {
                    JsonObject item = categoriesArray.get(i).getAsJsonObject();
                    BestForCategory category = new BestForCategory();
                    category.category = getString(item, "category");
                    category.recommended = getString(item, "recommended");
                    category.why = getString(item, "why");
                    result.bestForCategories.add(category);
                }
            }

            result.success = true;
        } catch (Exception ex) {
            result.success = false;
            result.errorMessage = "AI response could not be parsed as structured JSON.";
        }
    }

    private String extractJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }
        // Remove markdown fences if present
        String cleaned = trimmed.replace("```json", "").replace("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private List<String> toStringList(JsonArray arr) {
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.size(); i++) {
            out.add(arr.get(i).getAsString());
        }
        return out;
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static class CompareResult {
        public boolean success;
        public String errorMessage = "";
        public String quickSummary = "";
        public List<RankingEntry> ranking = new ArrayList<>();
        public List<AccommodationInsight> accommodationInsights = new ArrayList<>();
        public List<BestForCategory> bestForCategories = new ArrayList<>();
        public String rawResponse = "";
    }

    public static class RankingEntry {
        public int rank;
        public String name;
        public String reason;
    }

    public static class AccommodationInsight {
        public String name;
        public List<String> strengths = new ArrayList<>();
        public List<String> weaknesses = new ArrayList<>();
        public List<String> bestFor = new ArrayList<>();
    }

    public static class BestForCategory {
        public String category;
        public String recommended;
        public String why;
    }
}
