package tn.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class WeatherService {

    private static final String API_KEY = tn.esprit.utils.Config.getWeatherKey();
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    public WeatherInfo getWeatherForCity(String city) {
        try {
            // Encode the city name for URL (handles spaces and special characters)
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());

            // Build URL with city name and API key
            String urlString = BASE_URL + "?q=" + encodedCity + "&appid=" + API_KEY + "&units=metric";
            System.out.println("🌤️ Fetching weather for: " + city + " (URL: " + urlString + ")");

            // Create HTTP client
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("User-Agent", "TRIPX-App")
                    .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📡 Weather API Response Code: " + response.statusCode());

            if (response.statusCode() == 200) {
                return parseWeatherResponse(response.body());
            } else {
                System.err.println("❌ Weather API error for " + city + ": " + response.statusCode());
                System.err.println("Response body: " + response.body());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Error fetching weather for " + city + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private WeatherInfo parseWeatherResponse(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

        // Extract temperature
        double temp = jsonObject.getAsJsonObject("main").get("temp").getAsDouble();

        // Extract feels like temperature
        double feelsLike = jsonObject.getAsJsonObject("main").get("feels_like").getAsDouble();

        // Extract weather condition
        String condition = jsonObject.getAsJsonArray("weather")
                .get(0).getAsJsonObject()
                .get("description").getAsString();

        // Extract weather main (clear, clouds, rain, etc.)
        String mainCondition = jsonObject.getAsJsonArray("weather")
                .get(0).getAsJsonObject()
                .get("main").getAsString();

        // Extract humidity
        int humidity = jsonObject.getAsJsonObject("main").get("humidity").getAsInt();

        // Extract wind speed
        double windSpeed = jsonObject.getAsJsonObject("wind").get("speed").getAsDouble();

        // Extract city name (API returns the name it found)
        String cityName = jsonObject.get("name").getAsString();

        return new WeatherInfo(cityName, temp, feelsLike, condition, mainCondition, humidity, windSpeed);
    }

    // Inner class to hold weather data
    public static class WeatherInfo {
        private String city;
        private double temperature;
        private double feelsLike;
        private String condition;
        private String mainCondition;
        private int humidity;
        private double windSpeed;

        public WeatherInfo(String city, double temperature, double feelsLike, String condition,
                           String mainCondition, int humidity, double windSpeed) {
            this.city = city;
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.condition = condition;
            this.mainCondition = mainCondition;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
        }

        public String getCity() { return city; }
        public double getTemperature() { return temperature; }
        public double getFeelsLike() { return feelsLike; }
        public String getCondition() { return condition; }
        public String getMainCondition() { return mainCondition; }
        public int getHumidity() { return humidity; }
        public double getWindSpeed() { return windSpeed; }

        public String getWeatherEmoji() {
            if (mainCondition.contains("Clear")) return "☀️";
            if (mainCondition.contains("Clouds")) return "☁️";
            if (mainCondition.contains("Rain")) return "🌧️";
            if (mainCondition.contains("Snow")) return "❄️";
            if (mainCondition.contains("Thunderstorm")) return "⛈️";
            if (mainCondition.contains("Drizzle")) return "🌦️";
            if (mainCondition.contains("Mist") || mainCondition.contains("Fog")) return "🌫️";
            return "🌤️";
        }

        @Override
        public String toString() {
            return String.format("%s %.1f°C (feels like %.1f°C) - %s, 💧%d%% 🌬️%.1fm/s",
                    getWeatherEmoji(), temperature, feelsLike, condition, humidity, windSpeed);
        }

        // Short version for display
        public String toShortString() {
            return String.format("%s %.1f°C - %s", getWeatherEmoji(), temperature, condition);
        }
    }
    // In WeatherService, you can add a method to test specific cities
    public void testWeatherAPI() {
        String[] testCities = {"London", "Paris", "Tokyo", "New York", "Sydney", "Bali"};

        for (String city : testCities) {
            System.out.println("\n🔍 Testing: " + city);
            WeatherInfo info = getWeatherForCity(city);
            if (info != null) {
                System.out.println("✅ Success: " + info);
            } else {
                System.out.println("❌ Failed: " + city);
            }
        }
    }
}