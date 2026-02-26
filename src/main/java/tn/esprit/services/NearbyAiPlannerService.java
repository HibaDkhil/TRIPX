package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.entities.Accommodation;
import tn.esprit.utils.EnvConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class NearbyAiPlannerService {
    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final Gson gson = new Gson();

    public NearbyResult generateNearbyPlan(Accommodation accommodation) {
        NearbyResult result = new NearbyResult();
        if (accommodation == null) {
            result.success = false;
            result.errorMessage = "Accommodation data is missing.";
            return result;
        }

        String apiKey = EnvConfig.get("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            result.success = false;
            result.errorMessage = "Missing GROQ_API_KEY. Set it in the project .env file.";
            return result;
        }

        try {
            String requestJson = buildRequestJson(buildPrompt(accommodation));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_ENDPOINT))
                    .timeout(Duration.ofSeconds(40))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                result.success = false;
                result.errorMessage = "AI request failed (" + response.statusCode() + ").";
                result.rawResponse = response.body();
                return result;
            }

            JsonObject root = gson.fromJson(response.body(), JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                result.success = false;
                result.errorMessage = "No AI suggestions were returned.";
                return result;
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            String content = message == null ? "" : getString(message, "content");
            result.rawResponse = content;
            parseResult(content, result);
            return result;
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = "AI nearby generation failed: " + e.getMessage();
            return result;
        }
    }

    private String buildRequestJson(String prompt) {
        JsonObject root = new JsonObject();
        root.addProperty("model", MODEL);
        root.addProperty("temperature", 0.35);
        root.addProperty("max_tokens", 900);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "You are a travel concierge assistant. Output valid JSON only.");
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

    private String buildPrompt(Accommodation a) {
        return "Given this accommodation context, generate practical nearby suggestions.\n" +
                "Return JSON with this exact shape:\n" +
                "{\n" +
                "  \"summary\": \"short summary\",\n" +
                "  \"sections\": [\n" +
                "    {\"title\":\"Top Attractions\", \"items\":[\"...\", \"...\"]},\n" +
                "    {\"title\":\"Restaurants & Cafes\", \"items\":[\"...\", \"...\"]},\n" +
                "    {\"title\":\"Activities\", \"items\":[\"...\", \"...\"]},\n" +
                "    {\"title\":\"Transport Tips\", \"items\":[\"...\", \"...\"]}\n" +
                "  ]\n" +
                "}\n" +
                "No markdown.\n\n" +
                "Accommodation:\n" +
                "- Name: " + safe(a.getName()) + "\n" +
                "- Type: " + safe(a.getType()) + "\n" +
                "- City: " + safe(a.getCity()) + "\n" +
                "- Country: " + safe(a.getCountry()) + "\n" +
                "- Address: " + safe(a.getAddress()) + "\n" +
                "- Latitude: " + (a.getLatitude() == null ? "unknown" : a.getLatitude()) + "\n" +
                "- Longitude: " + (a.getLongitude() == null ? "unknown" : a.getLongitude()) + "\n" +
                "- Amenities: " + safe(a.getAccommodationAmenities()) + "\n";
    }

    private void parseResult(String content, NearbyResult result) {
        try {
            String json = extractJson(content);
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null) {
                result.success = false;
                result.errorMessage = "AI returned empty JSON.";
                return;
            }

            result.summary = getString(root, "summary");
            JsonArray sections = root.getAsJsonArray("sections");
            if (sections != null) {
                for (int i = 0; i < sections.size(); i++) {
                    JsonObject sectionObj = sections.get(i).getAsJsonObject();
                    NearbySection section = new NearbySection();
                    section.title = getString(sectionObj, "title");
                    section.items = toStringList(sectionObj.getAsJsonArray("items"));
                    result.sections.add(section);
                }
            }
            result.success = true;
        } catch (Exception e) {
            result.success = false;
            result.errorMessage = "AI response could not be parsed.";
        }
    }

    private String extractJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed;
        String cleaned = trimmed.replace("```json", "").replace("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) return cleaned.substring(start, end + 1);
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

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    public static class NearbyResult {
        public boolean success;
        public String errorMessage = "";
        public String summary = "";
        public String rawResponse = "";
        public List<NearbySection> sections = new ArrayList<>();
    }

    public static class NearbySection {
        public String title = "";
        public List<String> items = new ArrayList<>();
    }
}
