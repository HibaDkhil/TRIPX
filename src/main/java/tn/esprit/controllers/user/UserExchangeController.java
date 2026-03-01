package tn.esprit.controllers.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserExchangeController {

    @FXML private TextField txtAmount;
    @FXML private ComboBox<String> currencyPicker;
    @FXML private Label lblResult, lblRate, lblError;
    @FXML private VBox resultBox;
    @FXML private Button btnConvert;

    // Currency display names → ISO codes
    private static final Map<String, String> CURRENCIES = new LinkedHashMap<>();
    static {
        CURRENCIES.put("🇺🇸 US Dollar (USD)",        "USD");
        CURRENCIES.put("🇪🇺 Euro (EUR)",              "EUR");
        CURRENCIES.put("🇬🇧 British Pound (GBP)",     "GBP");
        CURRENCIES.put("🇸🇦 Saudi Riyal (SAR)",       "SAR");
        CURRENCIES.put("🇦🇪 UAE Dirham (AED)",        "AED");
        CURRENCIES.put("🇲🇦 Moroccan Dirham (MAD)",   "MAD");
        CURRENCIES.put("🇩🇿 Algerian Dinar (DZD)",    "DZD");
        CURRENCIES.put("🇹🇷 Turkish Lira (TRY)",      "TRY");
        CURRENCIES.put("🇯🇵 Japanese Yen (JPY)",      "JPY");
        CURRENCIES.put("🇨🇦 Canadian Dollar (CAD)",   "CAD");
    }

    private static final String API_KEY;
    private static final String BASE_URL = "https://openexchangerates.org/api/latest.json";
    // TND is not a free base currency, so we fetch in USD and cross-convert
    // 1 TND ≈ 0.323 USD (we'll fetch live USD rate for TND to cross-convert)

    static {
        String key = null;
        try {
            key = Dotenv.load().get("EXCHANGE_API_KEY");
        } catch (Exception ignored) {
            key = System.getenv("EXCHANGE_API_KEY");
        }
        API_KEY = (key != null && !key.isBlank()) ? key : "058c8b689b954819a8826f9bdc20083b";
    }

    public void initialize() {
        currencyPicker.setItems(FXCollections.observableArrayList(CURRENCIES.keySet()));
        currencyPicker.setValue("🇺🇸 US Dollar (USD)");

        // Allow pressing Enter in amount field
        txtAmount.setOnAction(e -> handleConvert());
    }

    @FXML
    private void handleConvert() {
        // Reset UI
        hideResult();
        hideError();

        // Validate input
        String amountText = txtAmount.getText().trim();
        if (amountText.isEmpty()) {
            showError("Please enter an amount.");
            return;
        }

        double amountTND;
        try {
            amountTND = Double.parseDouble(amountText);
            if (amountTND <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showError("Please enter a valid positive number.");
            return;
        }

        String selectedLabel = currencyPicker.getValue();
        String targetCode = CURRENCIES.get(selectedLabel);

        // Disable button while loading
        btnConvert.setText("Converting...");
        btnConvert.setDisable(true);

        // Run API call on background thread so UI doesn't freeze
        Thread thread = new Thread(() -> {
            try {
                String url = BASE_URL + "?app_id=" + API_KEY;

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    Platform.runLater(() -> showError("API error: " + response.statusCode()
                            + ". Check your API key."));
                    return;
                }

                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject rates = json.getAsJsonObject("rates");

                // Rates are relative to USD (free plan)
                // TND/USD rate
                double tndPerUsd = rates.get("TND").getAsDouble();
                // Target/USD rate
                double targetPerUsd = rates.get(targetCode).getAsDouble();

                // Convert: amountTND → USD → targetCurrency
                double amountUsd = amountTND / tndPerUsd;
                double converted = amountUsd * targetPerUsd;

                // Rate: 1 TND = X targetCurrency
                double rateOneTnd = targetPerUsd / tndPerUsd;

                Platform.runLater(() -> showResult(converted, rateOneTnd, targetCode));

            } catch (Exception ex) {
                Platform.runLater(() -> showError("Connection failed: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    btnConvert.setText("Convert Now");
                    btnConvert.setDisable(false);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void showResult(double converted, double rate, String targetCode) {
        lblResult.setText(String.format("%.2f %s", converted, targetCode));
        lblRate.setText(String.format("1 TND = %.4f %s", rate, targetCode));
        resultBox.setVisible(true);
        resultBox.setManaged(true);
    }

    private void hideResult() {
        resultBox.setVisible(false);
        resultBox.setManaged(false);
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
