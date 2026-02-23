package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.entities.UserPreferences;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GeminiService {

    // https://aistudio.google.com/app/apikey
    private static final String API_KEY = "";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public CompletableFuture<String> getRecommendation(String userPrompt, UserPreferences preferences) {
        if ("YOUR_API_KEY_HERE".equals(API_KEY)) {
            return CompletableFuture.completedFuture(getMockResponse(userPrompt));
        }

        String systemContext = buildSystemContext(preferences);
        String fullPrompt = systemContext + "\n\nUser Question: " + userPrompt;

        // Build Payload using Gson - FIXED: changed put() to add()
        JsonObject partsObj = new JsonObject();
        partsObj.addProperty("text", fullPrompt);

        JsonArray partsArray = new JsonArray();
        partsArray.add(partsObj);

        JsonObject contentObj = new JsonObject();
        contentObj.add("parts", partsArray);  // ← CHANGED from put() to add()

        JsonArray contentsArray = new JsonArray();
        contentsArray.add(contentObj);

        JsonObject requestBody = new JsonObject();
        requestBody.add("contents", contentsArray);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                        return jsonResponse.getAsJsonArray("candidates")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();
                    } else {
                        return "Error: " + response.statusCode() + " - " + response.body();
                    }
                })
                .exceptionally(ex -> "I'm sorry, I'm having trouble connecting to my brain right now. Please try again later! (" + ex.getMessage() + ")");
    }

    private String buildSystemContext(UserPreferences prefs) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are 'TripX AI', a friendly and professional travel assistant. ");
        sb.append("Provide personalized travel recommendations conversationally.");

        if (prefs != null) {
            sb.append("\n\nUser Profile:");
            if (prefs.getBudgetMinPerNight() != null)
                sb.append("\n- Budget: ").append(prefs.getBudgetMinPerNight()).append("-").append(prefs.getBudgetMaxPerNight());
            if (prefs.getPreferredClimate() != null)
                sb.append("\n- Climate: ").append(prefs.getPreferredClimate());
            if (prefs.getTravelPace() != null)
                sb.append("\n- Pace: ").append(prefs.getTravelPace());
        }

        sb.append("\n\nInstructions: Be enthusiastic and concise. Use markdown for formatting.");
        return sb.toString();
    }

    private String getMockResponse(String prompt) {
        try { Thread.sleep(800); } catch (InterruptedException e) {}
        String lower = prompt.toLowerCase();
        if (lower.contains("hello") || lower.contains("hi")) {
            return "Hello! I'm your **TripX AI Assistant**. I'm currently in **Test Mode**. Once you add your API key, I'll be fully powered by Gemini!";
        } else if (lower.contains("recommend") || lower.contains("where")) {
            return "Based on your preferences, I'd suggest **Kyoto, Japan** for a blend of culture and serenity. (This is a mock response - add an API key for real intelligence!)";
        }
        return "That's interesting! I'll be able to help you better with '" + prompt + "' once my Gemini brain is connected via an API key!";
    }
}