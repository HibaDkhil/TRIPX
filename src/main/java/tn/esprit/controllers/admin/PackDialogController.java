package tn.esprit.controllers.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

public class PackDialogController {

    @FXML private Label dialogTitle;
    @FXML private TextField txtTitle, txtPrice;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<PackCategory> cmbCategory;
    @FXML private ComboBox<Destination> cmbDestination;
    @FXML private ComboBox<Accommodation> cmbAccommodation;
    @FXML private ComboBox<Activity> cmbActivity;
    @FXML private ComboBox<Transport> cmbTransport;
    @FXML private ComboBox<Pack.Status> cmbStatus;
    @FXML private Spinner<Integer> spinDuration;
    @FXML private Button btnSave, btnCancel;

    // Country info card elements
    @FXML private VBox countryCard;
    @FXML private Label lblLoading;
    @FXML private HBox countryDetails;
    @FXML private Label lblFlag, lblCountryName;
    @FXML private Label lblCapital, lblCurrency, lblLanguage, lblPopulation, lblRegion;
    @FXML private Label lblCountryError;

    private PackService packService;
    private PackCategoryService categoryService;
    private LookupService lookupService;

    private Pack packToEdit;
    private boolean saveClicked = false;

    // RestCountries API — completely free, no key needed
    private static final String COUNTRIES_API = "https://restcountries.com/v3.1/name/";

    public void initialize() {
        packService = new PackService();
        categoryService = new PackCategoryService();
        lookupService = new LookupService();

        loadComboBoxes();
        setupButtons();
        setupDestinationListener(); // ← NEW: watch for destination changes
    }

    // ─── RestCountries API ───────────────────────────────────────────────────

    private void setupDestinationListener() {
        cmbDestination.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                hideCountryCard();
                return;
            }
            fetchCountryInfo(newVal.getName());
        });
    }

    private void fetchCountryInfo(String destinationName) {
        // Show card with loading state
        countryCard.setVisible(true);
        countryCard.setManaged(true);
        lblLoading.setVisible(true);
        lblLoading.setManaged(true);
        countryDetails.setVisible(false);
        countryDetails.setManaged(false);
        lblCountryError.setVisible(false);
        lblCountryError.setManaged(false);

        // Run on background thread
        Thread thread = new Thread(() -> {
            try {
                // Use just the first word of the destination as country name
                // e.g. "Paris, France" → "France" or "Tokyo" → "Tokyo"
                String query = extractCountryName(destinationName);
                String url = COUNTRIES_API + java.net.URLEncoder.encode(query, "UTF-8") + "?fullText=false";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 404) {
                    Platform.runLater(() -> showCountryError("No country info found for: " + destinationName));
                    return;
                }

                JsonArray countries = JsonParser.parseString(response.body()).getAsJsonArray();
                JsonObject country = countries.get(0).getAsJsonObject();

                // Extract data
                String flag = country.has("flag") ? country.get("flag").getAsString() : "🏳";

                String commonName = country.getAsJsonObject("name")
                        .get("common").getAsString();

                String capital = "N/A";
                if (country.has("capital") && country.get("capital").isJsonArray()) {
                    capital = country.getAsJsonArray("capital").get(0).getAsString();
                }

                String currency = "N/A";
                if (country.has("currencies")) {
                    JsonObject currencies = country.getAsJsonObject("currencies");
                    String code = currencies.keySet().iterator().next();
                    JsonObject currObj = currencies.getAsJsonObject(code);
                    String currName = currObj.has("name") ? currObj.get("name").getAsString() : code;
                    String symbol = currObj.has("symbol") ? currObj.get("symbol").getAsString() : "";
                    currency = currName + (symbol.isEmpty() ? "" : " (" + symbol + ")");
                }

                String language = "N/A";
                if (country.has("languages")) {
                    JsonObject langs = country.getAsJsonObject("languages");
                    language = langs.entrySet().iterator().next().getValue().getAsString();
                }

                long population = country.has("population") ?
                        country.get("population").getAsLong() : 0;
                String populationStr = NumberFormat.getNumberInstance(Locale.US).format(population);

                String region = country.has("region") ?
                        country.get("region").getAsString() : "N/A";
                if (country.has("subregion")) {
                    region += " — " + country.get("subregion").getAsString();
                }

                // Update UI on JavaFX thread
                final String fFlag = flag, fName = commonName, fCapital = capital;
                final String fCurrency = currency, fLanguage = language;
                final String fPopulation = populationStr, fRegion = region;

                Platform.runLater(() -> showCountryInfo(
                        fFlag, fName, fCapital, fCurrency, fLanguage, fPopulation, fRegion));

            } catch (Exception ex) {
                Platform.runLater(() -> showCountryError("Could not fetch country info."));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String extractCountryName(String destinationName) {
        // If destination contains a comma like "Paris, France" → take "France"
        if (destinationName.contains(",")) {
            return destinationName.split(",")[1].trim();
        }
        // Otherwise use as-is
        return destinationName.trim();
    }

    private void showCountryInfo(String flag, String name, String capital,
                                  String currency, String language,
                                  String population, String region) {
        lblLoading.setVisible(false);
        lblLoading.setManaged(false);
        lblCountryError.setVisible(false);
        lblCountryError.setManaged(false);

        lblFlag.setText(flag);
        lblCountryName.setText(name);
        lblCapital.setText(capital);
        lblCurrency.setText(currency);
        lblLanguage.setText(language);
        lblPopulation.setText(population);
        lblRegion.setText(region);

        countryDetails.setVisible(true);
        countryDetails.setManaged(true);
    }

    private void showCountryError(String message) {
        lblLoading.setVisible(false);
        lblLoading.setManaged(false);
        countryDetails.setVisible(false);
        countryDetails.setManaged(false);
        lblCountryError.setText("⚠ " + message);
        lblCountryError.setVisible(true);
        lblCountryError.setManaged(true);
    }

    private void hideCountryCard() {
        countryCard.setVisible(false);
        countryCard.setManaged(false);
    }

    // ─── Existing logic (unchanged) ─────────────────────────────────────────

    private void loadComboBoxes() {
        try {
            cmbCategory.setItems(FXCollections.observableArrayList(categoryService.afficherList()));
            cmbCategory.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(PackCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            cmbCategory.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(PackCategory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            cmbDestination.setItems(FXCollections.observableArrayList(lookupService.getAllDestinations()));
            cmbDestination.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Destination item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            cmbDestination.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Destination item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            cmbAccommodation.setItems(FXCollections.observableArrayList(lookupService.getAllAccommodations()));
            cmbAccommodation.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Accommodation item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            cmbAccommodation.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Accommodation item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            cmbActivity.setItems(FXCollections.observableArrayList(lookupService.getAllActivities()));
            cmbActivity.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Activity item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            cmbActivity.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Activity item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });

            cmbTransport.setItems(FXCollections.observableArrayList(lookupService.getAllTransport()));
            cmbTransport.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Transport item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTransportType());
                }
            });
            cmbTransport.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Transport item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getTransportType());
                }
            });

            cmbStatus.setItems(FXCollections.observableArrayList(Pack.Status.values()));

        } catch (SQLException e) {
            showError("Failed to load dropdown data: " + e.getMessage());
        }
    }

    private void setupButtons() {
        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> handleCancel());
    }

    public void setPackToEdit(Pack pack) {
        this.packToEdit = pack;
        dialogTitle.setText("Edit Pack");

        if (pack != null) {
            txtTitle.setText(pack.getTitle());
            txtDescription.setText(pack.getDescription());
            txtPrice.setText(pack.getBasePrice().toString());
            spinDuration.getValueFactory().setValue(pack.getDurationDays());
            cmbStatus.setValue(pack.getStatus());

            try {
                cmbCategory.getItems().stream()
                        .filter(c -> c.getIdCategory() == pack.getCategoryId())
                        .findFirst().ifPresent(cmbCategory::setValue);

                cmbDestination.getItems().stream()
                        .filter(d -> d.getDestinationId() == pack.getDestinationId())
                        .findFirst().ifPresent(cmbDestination::setValue); // ← this also triggers the API

                cmbAccommodation.getItems().stream()
                        .filter(a -> a.getId() == pack.getAccommodationId())
                        .findFirst().ifPresent(cmbAccommodation::setValue);

                cmbActivity.getItems().stream()
                        .filter(a -> a.getActivityId() == pack.getActivityId())
                        .findFirst().ifPresent(cmbActivity::setValue);

                cmbTransport.getItems().stream()
                        .filter(t -> t.getTransportId() == pack.getTransportId())
                        .findFirst().ifPresent(cmbTransport::setValue);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSave() {
        if (txtTitle.getText().trim().isEmpty()) { showError("Please enter a title"); return; }
        if (cmbCategory.getValue() == null)       { showError("Please select a category"); return; }
        if (cmbDestination.getValue() == null)    { showError("Please select a destination"); return; }
        if (cmbAccommodation.getValue() == null)  { showError("Please select an accommodation"); return; }
        if (cmbActivity.getValue() == null)       { showError("Please select an activity"); return; }
        if (cmbTransport.getValue() == null)      { showError("Please select a transport"); return; }
        if (txtPrice.getText().trim().isEmpty())   { showError("Please enter a price"); return; }

        try {
            BigDecimal price = new BigDecimal(txtPrice.getText().trim());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Price must be greater than 0");
                return;
            }

            if (packToEdit == null) {
                Pack newPack = new Pack(
                        txtTitle.getText().trim(),
                        txtDescription.getText().trim(),
                        cmbDestination.getValue().getDestinationId(),
                        cmbAccommodation.getValue().getId(),
                        cmbActivity.getValue().getActivityId(),
                        cmbTransport.getValue().getTransportId(),
                        cmbCategory.getValue().getIdCategory(),
                        spinDuration.getValue(),
                        price
                );
                packService.add(newPack);
            } else {
                packToEdit.setTitle(txtTitle.getText().trim());
                packToEdit.setDescription(txtDescription.getText().trim());
                packToEdit.setCategoryId(cmbCategory.getValue().getIdCategory());
                packToEdit.setDestinationId(cmbDestination.getValue().getDestinationId());
                packToEdit.setAccommodationId(cmbAccommodation.getValue().getId());
                packToEdit.setActivityId(cmbActivity.getValue().getActivityId());
                packToEdit.setTransportId(cmbTransport.getValue().getTransportId());
                packToEdit.setDurationDays(spinDuration.getValue());
                packToEdit.setBasePrice(price);
                packToEdit.setStatus(cmbStatus.getValue());
                packService.modifier(packToEdit);
            }

            saveClicked = true;
            closeDialog();

        } catch (NumberFormatException e) {
            showError("Invalid price format");
        } catch (SQLException e) {
            showError("Failed to save pack: " + e.getMessage());
        }
    }

    private void handleCancel() { closeDialog(); }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public boolean isSaveClicked() { return saveClicked; }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
