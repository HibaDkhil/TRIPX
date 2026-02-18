package tn.esprit.controllers.adminn;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.geometry.Insets;
import netscape.javascript.JSObject;
import tn.esprit.entities.Accommodation;
import tn.esprit.entities.Room;
import tn.esprit.services.AccommodationService;
import tn.esprit.services.RoomService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Controller for Add/Edit Accommodation Modal
 * Handles form input, validation, saving, and IMAGE UPLOAD
 */
public class AccommodationModalController {

    // ============ FXML Components ============

    // Modal Header
    @FXML private Label modalTitle;
    @FXML private Button closeModalBtn;
    @FXML private TabPane accommodationTabs;

    // Tab 1: Basic Info
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> starRatingCombo;
    @FXML private TextArea descriptionArea;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private ComboBox<String> countryCombo;
    @FXML private TextField postalCodeField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField websiteField;
    @FXML private FlowPane imagesPreviewPane;

    // Tab 2: Rooms
    @FXML private Button addRoomBtn;
    @FXML private VBox roomsContainer;

    // Tab 3: Amenities
    @FXML private CheckBox wifiCheck;
    @FXML private CheckBox parkingCheck;
    @FXML private CheckBox airConditioningCheck;
    @FXML private CheckBox heatingCheck;
    @FXML private CheckBox elevatorCheck;
    @FXML private CheckBox wheelchairCheck;
    @FXML private CheckBox petFriendlyCheck;
    @FXML private CheckBox smokingCheck;
    @FXML private CheckBox poolCheck;
    @FXML private CheckBox gymCheck;
    @FXML private CheckBox spaCheck;
    @FXML private CheckBox restaurantCheck;
    @FXML private CheckBox barCheck;
    @FXML private CheckBox conferenceCheck;
    @FXML private CheckBox businessCenterCheck;
    @FXML private CheckBox laundryCheck;
    @FXML private CheckBox reception24Check;
    @FXML private CheckBox conciergeCheck;
    @FXML private CheckBox roomServiceCheck;
    @FXML private CheckBox airportShuttleCheck;
    @FXML private CheckBox breakfastCheck;
    @FXML private CheckBox vipServicesCheck;
    @FXML private CheckBox babyCheck;
    @FXML private CheckBox tourDeskCheck;
    @FXML private CheckBox securityCheck;
    @FXML private CheckBox cctvCheck;
    @FXML private CheckBox safeCheck;
    @FXML private CheckBox fireExtinguisherCheck;
    @FXML private CheckBox smokeDetectorCheck;
    @FXML private CheckBox firstAidCheck;
    @FXML private TextField customAmenityField;
    @FXML private FlowPane customAmenitiesPane;

    // Footer Buttons
    @FXML private Button saveDraftBtn;
    @FXML private Button saveBtn;

    // ============ State Variables ============
    private AccommodationAdminController parentController;
    private AccommodationService accommodationService;
    private String mode; // "ADD" or "EDIT"
    private Accommodation currentAccommodation;
    private List<Room> rooms;
    private List<String> customAmenities;
    private List<File> selectedImages;
    private RoomService roomService; // 🔥 ADDED for database operations
    private List<Room> roomsToDelete; // 🔥 ADDED to track rooms to delete
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    // Logger
    private static final Logger logger = Logger.getLogger(AccommodationModalController.class.getName());

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.log(Level.INFO, "Initializing Modal Controller...");

        accommodationService = new AccommodationService();
        roomService = new RoomService(); // 🔥 ADDED
        rooms = new ArrayList<>();
        roomsToDelete = new ArrayList<>(); // 🔥 ADDED
        customAmenities = new ArrayList<>();
        selectedImages = new ArrayList<>();

        // POPULATE COMBOBOXES FIRST
        populateComboBoxes();

        setupFormValidation();
        populateCountries();

        logger.log(Level.INFO, "Modal Controller initialized");
    }

    /**
     * Populate all ComboBoxes with default values
     */
    private void populateComboBoxes() {
        // Type ComboBox
        if (typeCombo != null && typeCombo.getItems().isEmpty()) {
            typeCombo.getItems().addAll(
                    "Hotel", "Apartment", "Villa", "Resort", "Hostel",
                    "Guesthouse", "Boutique Hotel", "Motel", "Bed & Breakfast"
            );
        }

        // Status ComboBox
        if (statusCombo != null && statusCombo.getItems().isEmpty()) {
            statusCombo.getItems().addAll("Active", "Inactive", "Pending", "Under Maintenance");
        }

        // Star Rating ComboBox
        if (starRatingCombo != null && starRatingCombo.getItems().isEmpty()) {
            starRatingCombo.getItems().addAll(
                    "1 Stars", "2 Stars", "3 Stars", "4 Stars", "5 Stars"
            );
        }

        // Country ComboBox - Initialize if not done in FXML
        if (countryCombo != null && countryCombo.getItems().isEmpty()) {
            countryCombo.getItems().addAll(
                    "United Arab Emirates", "France", "USA", "Spain", "Italy",
                    "Greece", "Thailand", "Japan", "Maldives", "Switzerland",
                    "United Kingdom", "Germany", "Portugal", "Turkey", "Egypt",
                    "Tunisia", "Morocco", "Australia", "Canada", "Mexico"
            );
        }
    }

    /**
     * Set the parent controller
     */
    public void setParentController(AccommodationAdminController controller) {
        this.parentController = controller;
    }

    /**
     * Set mode (ADD or EDIT)
     */
    public void setMode(String mode) {
        this.mode = mode;
        if ("ADD".equals(mode)) {
            modalTitle.setText("➕ Add New Accommodation");
        } else if ("EDIT".equals(mode)) {
            modalTitle.setText("✏️ Edit Accommodation");
        }
    }

    /**
     * Load accommodation data for editing
     */
    public void loadAccommodation(Accommodation accommodation) {
        try {
            logger.log(Level.INFO, "===== Loading accommodation for EDIT =====");
            logger.log(Level.INFO, "   Name: " + accommodation.getName());
            logger.log(Level.INFO, "   ID: " + accommodation.getId());
            logger.log(Level.INFO, "   Type: " + accommodation.getType());
            logger.log(Level.INFO, "   Status: " + accommodation.getStatus());
            logger.log(Level.INFO, "   Stars: " + accommodation.getStars());
            logger.log(Level.INFO, "   Image Path: " + accommodation.getImagePath());

            this.currentAccommodation = accommodation;

            // Populate basic info with null checks
            if (nameField != null) {
                nameField.setText(accommodation.getName() != null ? accommodation.getName() : "");
            }

            // SAFE ComboBox setting
            safeSetComboValue(typeCombo, accommodation.getType());
            safeSetComboValue(statusCombo, accommodation.getStatus());

            // Handle star rating safely
            if (starRatingCombo != null && accommodation.getStars() > 0) {
                String starValue = accommodation.getStars() + " Stars";
                safeSetComboValue(starRatingCombo, starValue);
            }

            if (descriptionArea != null) {
                descriptionArea.setText(accommodation.getDescription() != null ? accommodation.getDescription() : "");
            }

            // Populate location
            if (addressField != null) {
                addressField.setText(accommodation.getAddress() != null ? accommodation.getAddress() : "");
            }
            if (cityField != null) {
                cityField.setText(accommodation.getCity() != null ? accommodation.getCity() : "");
            }

            safeSetComboValue(countryCombo, accommodation.getCountry());

            if (postalCodeField != null) {
                postalCodeField.setText(accommodation.getPostalCode() != null ? accommodation.getPostalCode() : "");
            }

            if (latitudeField != null && accommodation.getLatitude() != null && accommodation.getLatitude() != 0.0) {
                latitudeField.setText(String.valueOf(accommodation.getLatitude()));
            }
            if (longitudeField != null && accommodation.getLongitude() != null && accommodation.getLongitude() != 0.0) {
                longitudeField.setText(String.valueOf(accommodation.getLongitude()));
            }

            // Populate contact
            if (phoneField != null) {
                phoneField.setText(accommodation.getPhone() != null ? accommodation.getPhone() : "");
            }
            if (emailField != null) {
                emailField.setText(accommodation.getEmail() != null ? accommodation.getEmail() : "");
            }
            if (websiteField != null) {
                websiteField.setText(accommodation.getWebsite() != null ? accommodation.getWebsite() : "");
            }

            // Load existing image preview
            if (accommodation.getImagePath() != null && !accommodation.getImagePath().isEmpty()) {
                showExistingImagePreview(accommodation.getImagePath());
            }

            // 🔥 FIXED: Load rooms from database
            logger.log(Level.INFO, "📦 Loading rooms from database for accommodation ID: " + accommodation.getId());
            List<Room> dbRooms = roomService.getRoomsByAccommodationId(accommodation.getId());
            logger.log(Level.INFO, "   Found " + dbRooms.size() + " rooms in database");

            rooms.clear();
            rooms.addAll(dbRooms);
            refreshRoomsView();

            // 🔥 UPDATED: Load amenities from accommodation_amenities field
            if (accommodation.getAccommodationAmenities() != null && !accommodation.getAccommodationAmenities().isEmpty()) {
                // Parse comma-separated amenities string
                String[] amenitiesArray = accommodation.getAccommodationAmenities().split(",");
                List<String> amenitiesList = new ArrayList<>();
                for (String amenity : amenitiesArray) {
                    String trimmed = amenity.trim();
                    if (!trimmed.isEmpty()) {
                        amenitiesList.add(trimmed);
                    }
                }
                loadAmenities(amenitiesList);
            }

            logger.log(Level.INFO, "Accommodation loaded successfully into form");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "ERROR loading accommodation: " + e.getMessage(), e);
            showError("Error loading accommodation data: " + e.getMessage());
        }
    }

    /**
     * Show existing image preview for editing
     */
    private void showExistingImagePreview(String imagePath) {
        if (imagesPreviewPane != null) {
            imagesPreviewPane.getChildren().clear();

            VBox imageBox = new VBox(5);
            imageBox.setAlignment(javafx.geometry.Pos.CENTER);
            imageBox.setStyle(
                    "-fx-border-color: #27ae60; -fx-border-width: 2; " +
                            "-fx-border-radius: 5; -fx-padding: 10; -fx-background-color: white;"
            );

            try {
                // Try to load the image from resources
                if (imagePath != null && !imagePath.isEmpty()) {
                    var resource = getClass().getResource(imagePath);
                    if (resource != null) {
                        Image image = new Image(resource.toString(), 150, 100, true, true, true);
                        ImageView imageView = new ImageView(image);
                        imageView.setFitWidth(150);
                        imageView.setFitHeight(100);
                        imageView.setPreserveRatio(true);
                        imageBox.getChildren().add(imageView);
                    } else {
                        throw new Exception("Image not found in resources: " + imagePath);
                    }
                }
            } catch (Exception e) {
                // If image can't be loaded, show placeholder
                Label placeholderLabel = new Label("📷");
                placeholderLabel.setStyle("-fx-font-size: 48px;");
                imageBox.getChildren().add(placeholderLabel);
            }

            if (imagePath != null) {
                Label pathLabel = new Label("Current: " + new File(imagePath).getName());
                pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
                imageBox.getChildren().add(pathLabel);
            }

            Label infoLabel = new Label("(Upload new to replace)");
            infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

            imageBox.getChildren().add(infoLabel);
            imagesPreviewPane.getChildren().add(imageBox);
        }
    }

    /**
     * Safely set ComboBox value
     */
    private void safeSetComboValue(ComboBox<String> combo, String value) {
        if (combo == null) {
            logger.log(Level.WARNING, "ComboBox is null");
            return;
        }

        if (value == null || value.trim().isEmpty()) {
            combo.setValue(null);
            return;
        }

        String trimmedValue = value.trim();

        // Debug logging
        logger.log(Level.FINE, "Setting combo value: '" + trimmedValue + "'");
        logger.log(Level.FINE, "   Available items: " + combo.getItems());

        // Try exact match first
        if (combo.getItems().contains(trimmedValue)) {
            combo.setValue(trimmedValue);
            logger.log(Level.FINE, "✓ Exact match found and set");
            return;
        }

        // Try case-insensitive match
        for (String item : combo.getItems()) {
            if (item != null && item.equalsIgnoreCase(trimmedValue)) {
                combo.setValue(item);
                logger.log(Level.FINE, "✓ Case-insensitive match found: " + item);
                return;
            }
        }

        // Try partial match (for star ratings like "5 Stars" vs "5")
        for (String item : combo.getItems()) {
            if (item != null && item.contains(trimmedValue)) {
                combo.setValue(item);
                logger.log(Level.FINE, "✓ Partial match found: " + item);
                return;
            }
        }

        // Add if not found (for edit mode - preserve existing value)
        if ("EDIT".equals(mode)) {
            logger.log(Level.WARNING, "Value '" + trimmedValue + "' not found, adding to combo");
            combo.getItems().add(trimmedValue);
            combo.setValue(trimmedValue);
            logger.log(Level.FINE, "✓ Added and set value: " + trimmedValue);
        } else {
            combo.setValue(null);
            logger.log(Level.WARNING, "Value '" + trimmedValue + "' not found in combo");
        }
    }

    /**
     * Setup form validation
     */
    private void setupFormValidation() {
        if (nameField != null) {
            nameField.textProperty().addListener((obs, old, newVal) -> validateName());
        }
        if (emailField != null) {
            emailField.textProperty().addListener((obs, old, newVal) -> validateEmail());
        }
        if (phoneField != null) {
            phoneField.textProperty().addListener((obs, old, newVal) -> validatePhone());
        }

        if (descriptionArea != null) {
            descriptionArea.textProperty().addListener((obs, old, newVal) -> {
                int length = newVal != null ? newVal.length() : 0;
                if (length > 500) {
                    descriptionArea.setText(newVal.substring(0, 500));
                }
            });
        }
    }

    /**
     * Populate countries dropdown
     */
    private void populateCountries() {
        if (countryCombo != null && countryCombo.getItems().isEmpty()) {
            countryCombo.getItems().addAll(
                    "United Arab Emirates", "France", "USA", "Spain", "Italy",
                    "Greece", "Thailand", "Japan", "Maldives", "Switzerland",
                    "United Kingdom", "Germany", "Portugal", "Turkey", "Egypt",
                    "Tunisia", "Morocco", "Australia", "Canada", "Mexico"
            );
        }
    }

    /**
     * Validate name field
     */
    private boolean validateName() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            nameField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return false;
        }
        nameField.setStyle("");
        return true;
    }

    /**
     * Validate email field
     */
    private boolean validateEmail() {
        String email = emailField.getText();
        if (email == null || email.trim().isEmpty()) {
            emailField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return false;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            emailField.setStyle("-fx-border-color: orange; -fx-border-width: 2;");
            return false;
        }
        emailField.setStyle("");
        return true;
    }

    /**
     * Validate phone field
     */
    private boolean validatePhone() {
        String phone = phoneField.getText();
        if (phone == null || phone.trim().isEmpty()) {
            phoneField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            return false;
        }
        phoneField.setStyle("");
        return true;
    }

    /**
     * Validate all required fields
     */
    private boolean validateForm() {
        boolean valid = true;

        valid &= validateName();
        valid &= validateEmail();
        valid &= validatePhone();

        if (typeCombo.getValue() == null) {
            typeCombo.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            valid = false;
        } else {
            typeCombo.setStyle("");
        }

        if (statusCombo.getValue() == null) {
            statusCombo.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            valid = false;
        } else {
            statusCombo.setStyle("");
        }

        if (starRatingCombo.getValue() == null) {
            starRatingCombo.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            valid = false;
        } else {
            starRatingCombo.setStyle("");
        }

        if (cityField.getText() == null || cityField.getText().trim().isEmpty()) {
            cityField.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            valid = false;
        } else {
            cityField.setStyle("");
        }

        if (countryCombo.getValue() == null) {
            countryCombo.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            valid = false;
        } else {
            countryCombo.setStyle("");
        }

        if (!valid) {
            showError("Please fill in all required fields correctly.");
            if (accommodationTabs != null) {
                accommodationTabs.getSelectionModel().select(0);
            }
        }

        return valid;
    }

    /**
     * Handle upload images
     */
    @FXML
    private void handleUploadImages() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Accommodation Images");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(nameField.getScene().getWindow());

        if (files != null && !files.isEmpty()) {
            selectedImages.clear(); // Clear previous selection
            selectedImages.addAll(files);
            updateImagesPreview();
            logger.log(Level.INFO, "Selected " + files.size() + " image(s) for upload");
        }
    }

    /**
     * Update images preview
     */
    private void updateImagesPreview() {
        if (imagesPreviewPane != null) {
            imagesPreviewPane.getChildren().clear();

            for (File file : selectedImages) {
                VBox imageBox = new VBox(5);
                imageBox.setAlignment(javafx.geometry.Pos.CENTER);
                imageBox.setStyle(
                        "-fx-border-color: #3498db; -fx-border-width: 2; " +
                                "-fx-border-radius: 5; -fx-padding: 10; -fx-background-color: white;"
                );

                try {
                    // Show image preview
                    ImageView imageView = new ImageView();
                    imageView.setFitWidth(150);
                    imageView.setFitHeight(100);
                    imageView.setPreserveRatio(true);

                    Image image = new Image(file.toURI().toString(), 150, 100, true, true, true);
                    if (!image.isError()) {
                        imageView.setImage(image);
                        imageBox.getChildren().add(imageView);
                    } else {
                        throw new Exception("Image loading error");
                    }

                } catch (Exception e) {
                    Label iconLabel = new Label("🖼️");
                    iconLabel.setStyle("-fx-font-size: 48px;");
                    imageBox.getChildren().add(iconLabel);
                }

                Label nameLabel = new Label(file.getName());
                nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                nameLabel.setWrapText(true);
                nameLabel.setMaxWidth(140);

                Label sizeLabel = new Label(formatFileSize(file.length()));
                sizeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

                Button removeBtn = new Button("Remove");
                removeBtn.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                "-fx-font-size: 10px; -fx-padding: 3 8; -fx-cursor: hand; -fx-background-radius: 3;"
                );
                removeBtn.setOnAction(e -> {
                    selectedImages.remove(file);
                    updateImagesPreview();
                });

                imageBox.getChildren().addAll(nameLabel, sizeLabel, removeBtn);
                imagesPreviewPane.getChildren().add(imageBox);
            }
        }
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String saveUploadedImage(File imageFile, String accommodationType) {
        try {
            String projectRoot = System.getProperty("user.dir");

            String folderName = getFolderNameForType(accommodationType);

            String uploadDirPath =
                    projectRoot + "/uploads/images/" + folderName + "/";

            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalName = imageFile.getName();
            String extension = originalName.substring(originalName.lastIndexOf("."));
            String cleanName = originalName
                    .substring(0, originalName.lastIndexOf("."))
                    .replaceAll("[^a-zA-Z0-9]", "_");

            String newFileName = cleanName + "_" + System.currentTimeMillis() + extension;

            File destination = new File(uploadDirPath + newFileName);
            Files.copy(imageFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 🔥 ONLY this goes to DB
            return "/uploads/images/" + folderName + "/" + newFileName;

        } catch (Exception e) {
            showError("Failed to save image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get folder name based on accommodation type
     */
    private String getFolderNameForType(String type) {
        if (type == null) return "hotels";

        return switch (type.toLowerCase()) {
            case "hotel", "boutique hotel", "resort", "motel" -> "hotels";
            case "villa" -> "villas";
            case "apartment", "guesthouse", "bed & breakfast", "hostel" -> "apartments";
            default -> "hotels";
        };
    }

    /**
     * Build Accommodation object from form
     */
    private Accommodation buildAccommodationFromForm() {
        Accommodation accommodation = new Accommodation();

        // Basic info
        accommodation.setName(nameField.getText().trim());
        accommodation.setType(typeCombo.getValue());
        accommodation.setStatus(statusCombo.getValue());

        // Parse star rating safely
        String starValue = starRatingCombo.getValue();
        if (starValue != null && starValue.contains(" ")) {
            try {
                accommodation.setStars(Integer.parseInt(starValue.split(" ")[0]));
            } catch (NumberFormatException e) {
                accommodation.setStars(3); // Default
            }
        } else {
            accommodation.setStars(3); // Default
        }

        accommodation.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");

        // Location
        accommodation.setAddress(addressField.getText() != null ? addressField.getText().trim() : "");
        accommodation.setCity(cityField.getText().trim());
        accommodation.setCountry(countryCombo.getValue());
        accommodation.setPostalCode(postalCodeField.getText() != null ? postalCodeField.getText().trim() : "");

        // Coordinates
        try {
            if (latitudeField.getText() != null && !latitudeField.getText().trim().isEmpty()) {
                accommodation.setLatitude(Double.parseDouble(latitudeField.getText().trim()));
            } else {
                accommodation.setLatitude(0.0);
            }
        } catch (NumberFormatException e) {
            accommodation.setLatitude(0.0);
        }

        try {
            if (longitudeField.getText() != null && !longitudeField.getText().trim().isEmpty()) {
                accommodation.setLongitude(Double.parseDouble(longitudeField.getText().trim()));
            } else {
                accommodation.setLongitude(0.0);
            }
        } catch (NumberFormatException e) {
            accommodation.setLongitude(0.0);
        }

        // Contact
        accommodation.setPhone(phoneField.getText().trim());
        accommodation.setEmail(emailField.getText().trim());
        accommodation.setWebsite(websiteField.getText() != null ? websiteField.getText().trim() : "");

        // ========== IMAGE PATH HANDLING ==========
        String imagePath;

        if (selectedImages != null && !selectedImages.isEmpty()) {
            // NEW IMAGE UPLOADED - Save it to resources
            imagePath = saveUploadedImage(selectedImages.get(0), typeCombo.getValue());
            logger.log(Level.INFO, "📸 Using UPLOADED image: " + imagePath);
        } else if ("EDIT".equals(mode) && currentAccommodation != null &&
                currentAccommodation.getImagePath() != null &&
                !currentAccommodation.getImagePath().isEmpty() &&
                !currentAccommodation.getImagePath().equalsIgnoreCase("null")) {
            // EDIT MODE - Keep existing image if no new image uploaded
            imagePath = currentAccommodation.getImagePath();
            logger.log(Level.INFO, "📸 Keeping EXISTING image: " + imagePath);
        } else {
            // NO IMAGE - Don't set any image path
            imagePath = null;
            logger.log(Level.INFO, "📸 No image selected, image path will be null");
        }

        accommodation.setImagePath(imagePath);

        // Initialize rating
        if ("ADD".equals(mode)) {
            accommodation.setRating(0.0);
        } else if (currentAccommodation != null) {
            accommodation.setRating(currentAccommodation.getRating());
        }

        // Rooms
        accommodation.setRooms(new ArrayList<>(rooms));

        // 🔥 UPDATED: Amenities - save as comma-separated string in accommodation_amenities
        List<String> amenities = getSelectedAmenities();
        String amenitiesString = String.join(",", amenities);
        accommodation.setAccommodationAmenities(amenitiesString);

        logger.log(Level.INFO, "✅ Built accommodation object:");
        logger.log(Level.INFO, "   Name: " + accommodation.getName());
        logger.log(Level.INFO, "   Type: " + accommodation.getType());
        logger.log(Level.INFO, "   Image Path: " + accommodation.getImagePath());
        logger.log(Level.INFO, "   Stars: " + accommodation.getStars());
        logger.log(Level.INFO, "   Status: " + accommodation.getStatus());
        logger.log(Level.INFO, "   Amenities: " + amenitiesString);

        return accommodation;
    }

    /**
     * Handle save accommodation
     */
    @FXML
    private void handleSave() {
        if (!validateForm()) {
            return;
        }

        try {
            logger.log(Level.INFO, "💾 Saving accommodation...");
            Accommodation accommodation = buildAccommodationFromForm();

            int accommodationId;

            if ("ADD".equals(mode)) {
                logger.log(Level.INFO, "Mode: ADD");
                accommodationService.addAccommodation(accommodation);
                accommodationId = accommodation.getId();
                logger.log(Level.INFO, "✅ Accommodation added with ID: " + accommodationId);

                // 🔥 ADDED: Save all rooms to database
                saveRoomsToDatabase(accommodationId);

            } else if ("EDIT".equals(mode)) {
                logger.log(Level.INFO, "Mode: EDIT - ID: " + currentAccommodation.getId());
                accommodation.setId(currentAccommodation.getId());
                accommodationService.updateAccommodation(accommodation);
                accommodationId = currentAccommodation.getId();
                logger.log(Level.INFO, "✅ Accommodation updated");

                // 🔥 ADDED: Delete removed rooms from database
                deleteRoomsFromDatabase();

                // 🔥 ADDED: Save/update all rooms to database
                saveRoomsToDatabase(accommodationId);
            }

            logger.log(Level.INFO, "✅ Save completed successfully");

            if (parentController != null) {
                parentController.refreshAfterSave();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error saving: " + e.getMessage(), e);
            showError("Error saving accommodation: " + e.getMessage());
        }
    }

    // ========== ROOMS MANAGEMENT ==========

    @FXML
    private void handleAddRoom() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add Room");
        dialog.setHeaderText("Enter room details");

        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(500);

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new javafx.geometry.Insets(20));

        // === SECTION 1: Basic Info ===
        GridPane basicInfoGrid = new GridPane();
        basicInfoGrid.setHgap(10);
        basicInfoGrid.setVgap(10);

        Label basicInfoLabel = new Label("Basic Information");
        basicInfoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("Room Name");

        ComboBox<String> roomTypeCombo = new ComboBox<>();
        roomTypeCombo.getItems().addAll("Standard", "Deluxe", "Suite", "Presidential Suite", "Family Room");
        roomTypeCombo.setPromptText("Room Type");

        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacity (e.g., 2)");

        TextField sizeField = new TextField();
        sizeField.setPromptText("Size (m²)");

        TextField priceField = new TextField();
        priceField.setPromptText("Price per night (e.g., 150.00)");

        CheckBox availableCheck = new CheckBox("Available");
        availableCheck.setSelected(true);

        basicInfoGrid.add(new Label("Room Name:*"), 0, 0);
        basicInfoGrid.add(roomNameField, 1, 0);
        basicInfoGrid.add(new Label("Type:*"), 0, 1);
        basicInfoGrid.add(roomTypeCombo, 1, 1);
        basicInfoGrid.add(new Label("Capacity:*"), 0, 2);
        basicInfoGrid.add(capacityField, 1, 2);
        basicInfoGrid.add(new Label("Size (m²):*"), 0, 3);
        basicInfoGrid.add(sizeField, 1, 3);
        basicInfoGrid.add(new Label("Price:*"), 0, 4);
        basicInfoGrid.add(priceField, 1, 4);
        basicInfoGrid.add(availableCheck, 1, 5);

        // === SECTION 2: Room Amenities Checkboxes ===
        Label amenitiesLabel = new Label("Room Amenities");
        amenitiesLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Create amenities checkboxes organized by category
        VBox amenitiesContainer = new VBox(10);
        amenitiesContainer.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 5;");

        // Bed & Bedroom
        Label bedLabel = new Label("🛏️ Bed & Bedroom");
        bedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane bedPane = new FlowPane(10, 8);
        CheckBox kingBedCheck = new CheckBox("King Bed");
        CheckBox queenBedCheck = new CheckBox("Queen Bed");
        CheckBox twinBedsCheck = new CheckBox("Twin Beds");
        CheckBox sofaBedCheck = new CheckBox("Sofa Bed");
        CheckBox blackoutCurtainsCheck = new CheckBox("Blackout Curtains");
        CheckBox soundproofingCheck = new CheckBox("Soundproofing");
        bedPane.getChildren().addAll(kingBedCheck, queenBedCheck, twinBedsCheck, sofaBedCheck,
                blackoutCurtainsCheck, soundproofingCheck);

        // Bathroom
        Label bathroomLabel = new Label("🚿 Bathroom");
        bathroomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane bathroomPane = new FlowPane(10, 8);
        CheckBox privateBathroomCheck = new CheckBox("Private Bathroom");
        CheckBox bathtubCheck = new CheckBox("Bathtub");
        CheckBox rainShowerCheck = new CheckBox("Rain Shower");
        CheckBox hairdryerCheck = new CheckBox("Hairdryer");
        CheckBox toiletariesCheck = new CheckBox("Free Toiletries");
        CheckBox bathrobesCheck = new CheckBox("Bathrobes & Slippers");
        bathroomPane.getChildren().addAll(privateBathroomCheck, bathtubCheck, rainShowerCheck,
                hairdryerCheck, toiletariesCheck, bathrobesCheck);

        // Entertainment & Technology
        Label techLabel = new Label("📺 Entertainment & Technology");
        techLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane techPane = new FlowPane(10, 8);
        CheckBox tvCheck = new CheckBox("Flat-screen TV");
        CheckBox cableCheck = new CheckBox("Cable/Satellite TV");
        CheckBox netflixCheck = new CheckBox("Netflix");
        CheckBox roomWifiCheck = new CheckBox("WiFi");
        CheckBox phoneCheck = new CheckBox("Telephone");
        techPane.getChildren().addAll(tvCheck, cableCheck, netflixCheck, roomWifiCheck, phoneCheck);

        // Climate Control
        Label climateLabel = new Label("❄️ Climate Control");
        climateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane climatePane = new FlowPane(10, 8);
        CheckBox roomACCheck = new CheckBox("Air Conditioning");
        CheckBox roomHeatingCheck = new CheckBox("Heating");
        CheckBox ceilingFanCheck = new CheckBox("Ceiling Fan");
        climatePane.getChildren().addAll(roomACCheck, roomHeatingCheck, ceilingFanCheck);

        // Room Features
        Label featuresLabel = new Label("✨ Room Features");
        featuresLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane featuresPane = new FlowPane(10, 8);
        CheckBox minibarCheck = new CheckBox("Mini-bar");
        CheckBox safeCheck = new CheckBox("In-room Safe");
        CheckBox deskCheck = new CheckBox("Work Desk");
        CheckBox seatingAreaCheck = new CheckBox("Seating Area");
        CheckBox coffeeCheck = new CheckBox("Coffee/Tea Maker");
        CheckBox fridgeCheck = new CheckBox("Minibar/Fridge");
        CheckBox ironCheck = new CheckBox("Iron & Ironing Board");
        CheckBox wakeUpCheck = new CheckBox("Wake-up Service");
        featuresPane.getChildren().addAll(minibarCheck, safeCheck, deskCheck, seatingAreaCheck,
                coffeeCheck, fridgeCheck, ironCheck, wakeUpCheck);

        // Views & Outdoor
        Label viewsLabel = new Label("🌅 Views & Outdoor");
        viewsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane viewsPane = new FlowPane(10, 8);
        CheckBox balconyCheck = new CheckBox("Balcony/Terrace");
        CheckBox seaViewCheck = new CheckBox("Sea View");
        CheckBox mountainViewCheck = new CheckBox("Mountain View");
        CheckBox cityViewCheck = new CheckBox("City View");
        CheckBox gardenViewCheck = new CheckBox("Garden View");
        CheckBox poolViewCheck = new CheckBox("Pool View");
        viewsPane.getChildren().addAll(balconyCheck, seaViewCheck, mountainViewCheck,
                cityViewCheck, gardenViewCheck, poolViewCheck);

        amenitiesContainer.getChildren().addAll(
                bedLabel, bedPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                bathroomLabel, bathroomPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                techLabel, techPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                climateLabel, climatePane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                featuresLabel, featuresPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                viewsLabel, viewsPane
        );

        mainContainer.getChildren().addAll(basicInfoLabel, basicInfoGrid, amenitiesLabel, amenitiesContainer);
        scrollPane.setContent(mainContainer);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyRoomDialogTheme(dialog, mainContainer);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    if (roomNameField.getText().trim().isEmpty() ||
                            roomTypeCombo.getValue() == null ||
                            capacityField.getText().trim().isEmpty() ||
                            sizeField.getText().trim().isEmpty() ||
                            priceField.getText().trim().isEmpty()) {
                        showError("Please fill in all required fields.");
                        return null;
                    }

                    Room room = new Room();
                    room.setRoomName(roomNameField.getText().trim());
                    room.setRoomType(roomTypeCombo.getValue());
                    room.setCapacity(Integer.parseInt(capacityField.getText().trim()));
                    room.setSize(Double.parseDouble(sizeField.getText().trim()));
                    room.setPricePerNight(Double.parseDouble(priceField.getText().trim()));
                    room.setAvailable(availableCheck.isSelected());

                    // 🔥 NEW: Collect room amenities from checkboxes
                    List<String> roomAmenities = new ArrayList<>();

                    // Bed & Bedroom
                    if (kingBedCheck.isSelected()) roomAmenities.add("King Bed");
                    if (queenBedCheck.isSelected()) roomAmenities.add("Queen Bed");
                    if (twinBedsCheck.isSelected()) roomAmenities.add("Twin Beds");
                    if (sofaBedCheck.isSelected()) roomAmenities.add("Sofa Bed");
                    if (blackoutCurtainsCheck.isSelected()) roomAmenities.add("Blackout Curtains");
                    if (soundproofingCheck.isSelected()) roomAmenities.add("Soundproofing");

                    // Bathroom
                    if (privateBathroomCheck.isSelected()) roomAmenities.add("Private Bathroom");
                    if (bathtubCheck.isSelected()) roomAmenities.add("Bathtub");
                    if (rainShowerCheck.isSelected()) roomAmenities.add("Rain Shower");
                    if (hairdryerCheck.isSelected()) roomAmenities.add("Hairdryer");
                    if (toiletariesCheck.isSelected()) roomAmenities.add("Free Toiletries");
                    if (bathrobesCheck.isSelected()) roomAmenities.add("Bathrobes & Slippers");

                    // Entertainment & Technology
                    if (tvCheck.isSelected()) roomAmenities.add("Flat-screen TV");
                    if (cableCheck.isSelected()) roomAmenities.add("Cable/Satellite TV");
                    if (netflixCheck.isSelected()) roomAmenities.add("Netflix");
                    if (roomWifiCheck.isSelected()) roomAmenities.add("WiFi");
                    if (phoneCheck.isSelected()) roomAmenities.add("Telephone");

                    // Climate Control
                    if (roomACCheck.isSelected()) roomAmenities.add("Air Conditioning");
                    if (roomHeatingCheck.isSelected()) roomAmenities.add("Heating");
                    if (ceilingFanCheck.isSelected()) roomAmenities.add("Ceiling Fan");

                    // Room Features
                    if (minibarCheck.isSelected()) roomAmenities.add("Mini-bar");
                    if (safeCheck.isSelected()) roomAmenities.add("In-room Safe");
                    if (deskCheck.isSelected()) roomAmenities.add("Work Desk");
                    if (seatingAreaCheck.isSelected()) roomAmenities.add("Seating Area");
                    if (coffeeCheck.isSelected()) roomAmenities.add("Coffee/Tea Maker");
                    if (fridgeCheck.isSelected()) roomAmenities.add("Minibar/Fridge");
                    if (ironCheck.isSelected()) roomAmenities.add("Iron & Ironing Board");
                    if (wakeUpCheck.isSelected()) roomAmenities.add("Wake-up Service");

                    // Views & Outdoor
                    if (balconyCheck.isSelected()) roomAmenities.add("Balcony/Terrace");
                    if (seaViewCheck.isSelected()) roomAmenities.add("Sea View");
                    if (mountainViewCheck.isSelected()) roomAmenities.add("Mountain View");
                    if (cityViewCheck.isSelected()) roomAmenities.add("City View");
                    if (gardenViewCheck.isSelected()) roomAmenities.add("Garden View");
                    if (poolViewCheck.isSelected()) roomAmenities.add("Pool View");

                    room.setAmenities(String.join(",", roomAmenities));

                    return room;
                } catch (NumberFormatException e) {
                    showError("Please enter valid numeric values for capacity, size, and price.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(room -> {
            if (room != null) {
                rooms.add(room);
                refreshRoomsView();
            }
        });
    }

    private void refreshRoomsView() {
        roomsContainer.getChildren().clear();

        if (rooms.isEmpty()) {
            Label emptyLabel = new Label("No rooms added yet. Click 'Add Room' to get started.");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 14px; -fx-padding: 20;");
            roomsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Room room : rooms) {
            HBox roomCard = createRoomCard(room);
            roomsContainer.getChildren().add(roomCard);
        }
    }

    private HBox createRoomCard(Room room) {
        HBox card = new HBox(20);
        card.getStyleClass().add("room-card");
        card.setPadding(new javafx.geometry.Insets(15, 20, 15, 20));
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );

        VBox info = new VBox(5);
        Label nameLabel = new Label(room.getRoomName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label detailsLabel = new Label(
                "Type: " + room.getRoomType() + " | Capacity: " + room.getCapacity() +
                        " | Size: " + room.getSize() + "m²"
        );
        detailsLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

        Label priceLabel = new Label("Price: €" + String.format("%.2f", room.getPricePerNight()) + "/night");
        priceLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 14px; -fx-font-weight: bold;");

        info.getChildren().addAll(nameLabel, detailsLabel, priceLabel);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        VBox actions = new VBox(8);
        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle(
                "-fx-background-color: #3498db; -fx-text-fill: white; " +
                        "-fx-padding: 5 15; -fx-background-radius: 5; -fx-cursor: hand;"
        );
        editBtn.setOnAction(e -> handleEditRoom(room)); // 🔥 ADDED action handler

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                        "-fx-padding: 5 15; -fx-background-radius: 5; -fx-cursor: hand;"
        );
        deleteBtn.setOnAction(e -> handleDeleteRoom(room)); // 🔥 FIXED to use new method

        actions.getChildren().addAll(editBtn, deleteBtn);
        card.getChildren().addAll(info, actions);

        return card;
    }

    // ========== AMENITIES MANAGEMENT ==========

    private void loadAmenities(List<String> amenities) {
        if (amenities == null) return;

        for (String amenity : amenities) {
            switch (amenity) {
                case "Wi-Fi" -> wifiCheck.setSelected(true);
                case "Free Parking" -> parkingCheck.setSelected(true);
                case "Air Conditioning" -> airConditioningCheck.setSelected(true);
                case "Heating" -> heatingCheck.setSelected(true);
                case "Elevator" -> elevatorCheck.setSelected(true);
                case "Wheelchair Accessible" -> wheelchairCheck.setSelected(true);
                case "Pet Friendly" -> petFriendlyCheck.setSelected(true);
                case "Smoking Allowed" -> smokingCheck.setSelected(true);
                case "Swimming Pool" -> poolCheck.setSelected(true);
                case "Fitness Center" -> gymCheck.setSelected(true);
                case "Spa & Wellness" -> spaCheck.setSelected(true);
                case "Restaurant" -> restaurantCheck.setSelected(true);
                case "Bar/Lounge" -> barCheck.setSelected(true);
                case "Conference Rooms" -> conferenceCheck.setSelected(true);
                case "Business Center" -> businessCenterCheck.setSelected(true);
                case "Laundry Service" -> laundryCheck.setSelected(true);
                case "24h Reception" -> reception24Check.setSelected(true);
                case "Concierge" -> conciergeCheck.setSelected(true);
                case "Room Service" -> roomServiceCheck.setSelected(true);
                case "Airport Shuttle" -> airportShuttleCheck.setSelected(true);
                case "Breakfast Included" -> breakfastCheck.setSelected(true);
                case "VIP Services" -> vipServicesCheck.setSelected(true);
                case "Babysitting" -> babyCheck.setSelected(true);
                case "Tour Desk" -> tourDeskCheck.setSelected(true);
                case "24h Security" -> securityCheck.setSelected(true);
                case "CCTV" -> cctvCheck.setSelected(true);
                case "In-room Safe" -> safeCheck.setSelected(true);
                case "Fire Extinguishers" -> fireExtinguisherCheck.setSelected(true);
                case "Smoke Detectors" -> smokeDetectorCheck.setSelected(true);
                case "First Aid Kit" -> firstAidCheck.setSelected(true);
                default -> {
                    if (!customAmenities.contains(amenity)) {
                        customAmenities.add(amenity);
                        createCustomAmenityTag(amenity);
                    }
                }
            }
        }
    }

    private List<String> getSelectedAmenities() {
        List<String> amenities = new ArrayList<>();

        if (wifiCheck.isSelected()) amenities.add("Wi-Fi");
        if (parkingCheck.isSelected()) amenities.add("Free Parking");
        if (airConditioningCheck.isSelected()) amenities.add("Air Conditioning");
        if (heatingCheck.isSelected()) amenities.add("Heating");
        if (elevatorCheck.isSelected()) amenities.add("Elevator");
        if (wheelchairCheck.isSelected()) amenities.add("Wheelchair Accessible");
        if (petFriendlyCheck.isSelected()) amenities.add("Pet Friendly");
        if (smokingCheck.isSelected()) amenities.add("Smoking Allowed");
        if (poolCheck.isSelected()) amenities.add("Swimming Pool");
        if (gymCheck.isSelected()) amenities.add("Fitness Center");
        if (spaCheck.isSelected()) amenities.add("Spa & Wellness");
        if (restaurantCheck.isSelected()) amenities.add("Restaurant");
        if (barCheck.isSelected()) amenities.add("Bar/Lounge");
        if (conferenceCheck.isSelected()) amenities.add("Conference Rooms");
        if (businessCenterCheck.isSelected()) amenities.add("Business Center");
        if (laundryCheck.isSelected()) amenities.add("Laundry Service");
        if (reception24Check.isSelected()) amenities.add("24h Reception");
        if (conciergeCheck.isSelected()) amenities.add("Concierge");
        if (roomServiceCheck.isSelected()) amenities.add("Room Service");
        if (airportShuttleCheck.isSelected()) amenities.add("Airport Shuttle");
        if (breakfastCheck.isSelected()) amenities.add("Breakfast Included");
        if (vipServicesCheck.isSelected()) amenities.add("VIP Services");
        if (babyCheck.isSelected()) amenities.add("Babysitting");
        if (tourDeskCheck.isSelected()) amenities.add("Tour Desk");
        if (securityCheck.isSelected()) amenities.add("24h Security");
        if (cctvCheck.isSelected()) amenities.add("CCTV");
        if (safeCheck.isSelected()) amenities.add("In-room Safe");
        if (fireExtinguisherCheck.isSelected()) amenities.add("Fire Extinguishers");
        if (smokeDetectorCheck.isSelected()) amenities.add("Smoke Detectors");
        if (firstAidCheck.isSelected()) amenities.add("First Aid Kit");

        amenities.addAll(customAmenities);

        return amenities;
    }

    @FXML
    private void handleAddCustomAmenity() {
        String amenity = customAmenityField.getText();
        if (amenity != null && !amenity.trim().isEmpty()) {
            String trimmedAmenity = amenity.trim();
            if (!customAmenities.contains(trimmedAmenity)) {
                customAmenities.add(trimmedAmenity);
                createCustomAmenityTag(trimmedAmenity);
                customAmenityField.clear();
            } else {
                showInfo("This amenity has already been added.");
            }
        }
    }

    private void createCustomAmenityTag(String amenity) {
        HBox tag = new HBox(8);
        tag.setStyle(
                "-fx-background-color: #3498db; -fx-padding: 8 12; " +
                        "-fx-background-radius: 15; -fx-alignment: center-left;"
        );

        Label label = new Label(amenity);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        Button removeBtn = new Button("✕");
        removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; " +
                        "-fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-padding: 0 5;"
        );
        removeBtn.setOnAction(e -> {
            customAmenities.remove(amenity);
            customAmenitiesPane.getChildren().remove(tag);
        });

        tag.getChildren().addAll(label, removeBtn);
        customAmenitiesPane.getChildren().add(tag);
    }

    // ========== UTILITY METHODS ==========

    @FXML
    private void handleSaveDraft() {
        showInfo("Draft functionality will be implemented in a future update.");
    }

    @FXML
    private void closeModal() {
        if (parentController != null) {
            parentController.closeModal();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void applyRoomDialogTheme(Dialog<?> dialog, VBox contentRoot) {
        DialogPane dialogPane = dialog.getDialogPane();
        var cssUrl = getClass().getResource("/css/admin-style.css");
        if (cssUrl != null) {
            String cssPath = cssUrl.toExternalForm();
            if (!dialogPane.getStylesheets().contains(cssPath)) {
                dialogPane.getStylesheets().add(cssPath);
            }
        }

        dialogPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #FFFFFF, #F8FAFC);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #E2E8F0;" +
                        "-fx-border-radius: 18;" +
                        "-fx-border-width: 1.2;"
        );

        if (contentRoot != null) {
            contentRoot.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #FFFFFF, #F8FAFC);" +
                            "-fx-background-radius: 14;"
            );
            styleDialogNodesRecursively(contentRoot);
        }

        Node okNode = dialogPane.lookupButton(ButtonType.OK);
        if (okNode instanceof Button okButton) {
            okButton.setText("Save Room");
            if (!okButton.getStyleClass().contains("btn-save")) {
                okButton.getStyleClass().add("btn-save");
            }
            okButton.setStyle("-fx-font-weight: 700; -fx-padding: 8 16;");
        }

        Node cancelNode = dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelNode instanceof Button cancelButton) {
            if (!cancelButton.getStyleClass().contains("btn-cancel")) {
                cancelButton.getStyleClass().add("btn-cancel");
            }
            cancelButton.setStyle("-fx-font-weight: 600; -fx-padding: 8 16;");
        }
    }

    private void styleDialogNodesRecursively(Node node) {
        if (node instanceof TextField textField) {
            if (!textField.getStyleClass().contains("form-input")) {
                textField.getStyleClass().add("form-input");
            }
        } else if (node instanceof TextArea textArea) {
            if (!textArea.getStyleClass().contains("form-input")) {
                textArea.getStyleClass().add("form-input");
            }
        } else if (node instanceof ComboBox<?> comboBox) {
            if (!comboBox.getStyleClass().contains("form-input")) {
                comboBox.getStyleClass().add("form-input");
            }
        } else if (node instanceof CheckBox checkBox) {
            if (!checkBox.getStyleClass().contains("amenity-checkbox")) {
                checkBox.getStyleClass().add("amenity-checkbox");
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                styleDialogNodesRecursively(child);
            }
        }
    }

    @FXML
    private void handleGetFromMap() {
        openMapPickerDialog();
    }

    private void openMapPickerDialog() {
        Dialog<Void> mapDialog = new Dialog<>();
        mapDialog.initModality(Modality.APPLICATION_MODAL);
        mapDialog.setTitle("Pick Location");
        mapDialog.setHeaderText("Click on the map to select coordinates");
        mapDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        WebView webView = new WebView();
        webView.setPrefSize(860, 560);

        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8));
        wrapper.getChildren().add(webView);
        mapDialog.getDialogPane().setContent(wrapper);
        mapDialog.getDialogPane().setPrefSize(900, 650);

        WebEngine engine = webView.getEngine();
        double initialLat = parseCoordinate(latitudeField != null ? latitudeField.getText() : null, 36.8065);
        double initialLng = parseCoordinate(longitudeField != null ? longitudeField.getText() : null, 10.1815);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new MapPickerBridge(mapDialog));
                Platform.runLater(() -> {
                    try {
                        engine.executeScript("if(window.initMapPicker){window.initMapPicker(" + initialLat + "," + initialLng + ");}");
                        engine.executeScript("if(window.fixMapSize){window.fixMapSize();}");
                    } catch (Exception ignored) {
                    }
                });
            }
        });

        webView.widthProperty().addListener((obs, oldVal, newVal) -> {
            try {
                engine.executeScript("if(window.fixMapSize){window.fixMapSize();}");
            } catch (Exception ignored) {
            }
        });
        webView.heightProperty().addListener((obs, oldVal, newVal) -> {
            try {
                engine.executeScript("if(window.fixMapSize){window.fixMapSize();}");
            } catch (Exception ignored) {
            }
        });

        mapDialog.setOnShown(event -> Platform.runLater(() -> {
            try {
                engine.executeScript("if(window.fixMapSize){window.fixMapSize();}");
            } catch (Exception ignored) {
            }
        }));

        var mapPickerUrl = getClass().getResource("/map-picker.html");
        if (mapPickerUrl != null) {
            engine.load(mapPickerUrl.toExternalForm());
        } else {
            showError("Map resource not found: /map-picker.html");
            return;
        }

        mapDialog.showAndWait();
    }

    private double parseCoordinate(String text, double fallback) {
        try {
            if (text == null || text.trim().isEmpty()) return fallback;
            return Double.parseDouble(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void reverseGeocodeAndFill(double latitude, double longitude) {
        CompletableFuture
                .supplyAsync(() -> fetchAddressFromNominatim(latitude, longitude))
                .thenAccept(address -> Platform.runLater(() -> {
                    if (address == null) return;

                    if (cityField != null && (cityField.getText() == null || cityField.getText().isBlank())) {
                        cityField.setText(address.city);
                    } else if (cityField != null && address.city != null && !address.city.isBlank()) {
                        cityField.setText(address.city);
                    }

                    if (postalCodeField != null && address.postalCode != null && !address.postalCode.isBlank()) {
                        postalCodeField.setText(address.postalCode);
                    }

                    if (countryCombo != null && address.country != null && !address.country.isBlank()) {
                        if (!countryCombo.getItems().contains(address.country)) {
                            countryCombo.getItems().add(address.country);
                        }
                        countryCombo.setValue(address.country);
                    }
                }));
    }

    private ReverseAddress fetchAddressFromNominatim(double latitude, double longitude) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2" +
                    "&lat=" + URLEncoder.encode(String.valueOf(latitude), StandardCharsets.UTF_8) +
                    "&lon=" + URLEncoder.encode(String.valueOf(longitude), StandardCharsets.UTF_8) +
                    "&addressdetails=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "TripX-AdminDashboard/1.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.log(Level.WARNING, "Nominatim reverse geocoding failed with status " + response.statusCode());
                return null;
            }

            JsonObject root = gson.fromJson(response.body(), JsonObject.class);
            if (root == null || !root.has("address")) return null;

            JsonObject address = root.getAsJsonObject("address");
            ReverseAddress out = new ReverseAddress();
            out.city = firstNonEmpty(
                    getString(address, "city"),
                    getString(address, "town"),
                    getString(address, "village"),
                    getString(address, "municipality"),
                    getString(address, "county"),
                    getString(address, "state")
            );
            out.postalCode = getString(address, "postcode");
            out.country = getString(address, "country");
            return out;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error in reverse geocoding: " + e.getMessage());
            return null;
        }
    }

    private String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        return object.get(key).getAsString();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static class ReverseAddress {
        String city = "";
        String postalCode = "";
        String country = "";
    }

    public class MapPickerBridge {
        private final Dialog<Void> mapDialog;

        MapPickerBridge(Dialog<Void> mapDialog) {
            this.mapDialog = mapDialog;
        }

        public void onMapClicked(String lat, String lng) {
            Platform.runLater(() -> {
                latitudeField.setText(lat);
                longitudeField.setText(lng);

                try {
                    double latitude = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lng);
                    reverseGeocodeAndFill(latitude, longitude);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Invalid coordinates received from map: " + lat + ", " + lng);
                }

                mapDialog.close();
            });
        }
    }

    // ========== 🔥 ROOM DATABASE OPERATIONS (ADDED) ==========

    /**
     * Save all rooms to database
     */
    private void saveRoomsToDatabase(int accommodationId) {
        logger.log(Level.INFO, "💾 Saving " + rooms.size() + " rooms to database...");

        for (Room room : rooms) {
            room.setAccommodationId(accommodationId);

            if (room.getId() == 0) {
                // New room - INSERT
                logger.log(Level.INFO, "   ➕ Adding new room: " + room.getRoomName());
                roomService.addRoom(room);
            } else {
                // Existing room - UPDATE
                logger.log(Level.INFO, "   ✏️ Updating room ID " + room.getId() + ": " + room.getRoomName());
                roomService.updateRoom(room);
            }
        }

        logger.log(Level.INFO, "✅ All rooms saved to database");
    }

    /**
     * Delete removed rooms from database
     */
    private void deleteRoomsFromDatabase() {
        if (roomsToDelete.isEmpty()) {
            logger.log(Level.INFO, "No rooms to delete");
            return;
        }

        logger.log(Level.INFO, "🗑️ Deleting " + roomsToDelete.size() + " rooms from database...");

        for (Room room : roomsToDelete) {
            if (room.getId() > 0) {
                logger.log(Level.INFO, "   🗑️ Deleting room ID " + room.getId() + ": " + room.getRoomName());
                roomService.deleteRoom(room.getId());
            }
        }

        roomsToDelete.clear();
        logger.log(Level.INFO, "✅ All rooms deleted from database");
    }

    /**
     * Handle editing a room
     */
    private void handleEditRoom(Room room) {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Edit room details");

        // Create scrollable content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(500);

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new javafx.geometry.Insets(20));

        // === SECTION 1: Basic Info ===
        GridPane basicInfoGrid = new GridPane();
        basicInfoGrid.setHgap(10);
        basicInfoGrid.setVgap(10);

        Label basicInfoLabel = new Label("Basic Information");
        basicInfoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField roomNameField = new TextField(room.getRoomName());
        ComboBox<String> roomTypeCombo = new ComboBox<>();
        roomTypeCombo.getItems().addAll("Standard", "Deluxe", "Suite", "Presidential Suite", "Family Room");
        roomTypeCombo.setValue(room.getRoomType());
        TextField capacityField = new TextField(String.valueOf(room.getCapacity()));
        TextField sizeField = new TextField(String.valueOf(room.getSize()));
        TextField priceField = new TextField(String.valueOf(room.getPricePerNight()));
        CheckBox availableCheck = new CheckBox("Available");
        availableCheck.setSelected(room.isAvailable());

        basicInfoGrid.add(new Label("Room Name:*"), 0, 0);
        basicInfoGrid.add(roomNameField, 1, 0);
        basicInfoGrid.add(new Label("Type:*"), 0, 1);
        basicInfoGrid.add(roomTypeCombo, 1, 1);
        basicInfoGrid.add(new Label("Capacity:*"), 0, 2);
        basicInfoGrid.add(capacityField, 1, 2);
        basicInfoGrid.add(new Label("Size (m²):*"), 0, 3);
        basicInfoGrid.add(sizeField, 1, 3);
        basicInfoGrid.add(new Label("Price:*"), 0, 4);
        basicInfoGrid.add(priceField, 1, 4);
        basicInfoGrid.add(availableCheck, 1, 5);

        // === SECTION 2: Room Amenities Checkboxes ===
        Label amenitiesLabel = new Label("Room Amenities");
        amenitiesLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox amenitiesContainer = new VBox(10);
        amenitiesContainer.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 5;");

        // Bed & Bedroom
        Label bedLabel = new Label("🛏️ Bed & Bedroom");
        bedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane bedPane = new FlowPane(10, 8);
        CheckBox kingBedCheck = new CheckBox("King Bed");
        CheckBox queenBedCheck = new CheckBox("Queen Bed");
        CheckBox twinBedsCheck = new CheckBox("Twin Beds");
        CheckBox sofaBedCheck = new CheckBox("Sofa Bed");
        CheckBox blackoutCurtainsCheck = new CheckBox("Blackout Curtains");
        CheckBox soundproofingCheck = new CheckBox("Soundproofing");
        bedPane.getChildren().addAll(kingBedCheck, queenBedCheck, twinBedsCheck, sofaBedCheck,
                blackoutCurtainsCheck, soundproofingCheck);

        // Bathroom
        Label bathroomLabel = new Label("🚿 Bathroom");
        bathroomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane bathroomPane = new FlowPane(10, 8);
        CheckBox privateBathroomCheck = new CheckBox("Private Bathroom");
        CheckBox bathtubCheck = new CheckBox("Bathtub");
        CheckBox rainShowerCheck = new CheckBox("Rain Shower");
        CheckBox hairdryerCheck = new CheckBox("Hairdryer");
        CheckBox toiletariesCheck = new CheckBox("Free Toiletries");
        CheckBox bathrobesCheck = new CheckBox("Bathrobes & Slippers");
        bathroomPane.getChildren().addAll(privateBathroomCheck, bathtubCheck, rainShowerCheck,
                hairdryerCheck, toiletariesCheck, bathrobesCheck);

        // Entertainment & Technology
        Label techLabel = new Label("📺 Entertainment & Technology");
        techLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane techPane = new FlowPane(10, 8);
        CheckBox tvCheck = new CheckBox("Flat-screen TV");
        CheckBox cableCheck = new CheckBox("Cable/Satellite TV");
        CheckBox netflixCheck = new CheckBox("Netflix");
        CheckBox roomWifiCheck = new CheckBox("WiFi");
        CheckBox phoneCheck = new CheckBox("Telephone");
        techPane.getChildren().addAll(tvCheck, cableCheck, netflixCheck, roomWifiCheck, phoneCheck);

        // Climate Control
        Label climateLabel = new Label("❄️ Climate Control");
        climateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane climatePane = new FlowPane(10, 8);
        CheckBox roomACCheck = new CheckBox("Air Conditioning");
        CheckBox roomHeatingCheck = new CheckBox("Heating");
        CheckBox ceilingFanCheck = new CheckBox("Ceiling Fan");
        climatePane.getChildren().addAll(roomACCheck, roomHeatingCheck, ceilingFanCheck);

        // Room Features
        Label featuresLabel = new Label("✨ Room Features");
        featuresLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane featuresPane = new FlowPane(10, 8);
        CheckBox minibarCheck = new CheckBox("Mini-bar");
        CheckBox safeCheck = new CheckBox("In-room Safe");
        CheckBox deskCheck = new CheckBox("Work Desk");
        CheckBox seatingAreaCheck = new CheckBox("Seating Area");
        CheckBox coffeeCheck = new CheckBox("Coffee/Tea Maker");
        CheckBox fridgeCheck = new CheckBox("Minibar/Fridge");
        CheckBox ironCheck = new CheckBox("Iron & Ironing Board");
        CheckBox wakeUpCheck = new CheckBox("Wake-up Service");
        featuresPane.getChildren().addAll(minibarCheck, safeCheck, deskCheck, seatingAreaCheck,
                coffeeCheck, fridgeCheck, ironCheck, wakeUpCheck);

        // Views & Outdoor
        Label viewsLabel = new Label("🌅 Views & Outdoor");
        viewsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        FlowPane viewsPane = new FlowPane(10, 8);
        CheckBox balconyCheck = new CheckBox("Balcony/Terrace");
        CheckBox seaViewCheck = new CheckBox("Sea View");
        CheckBox mountainViewCheck = new CheckBox("Mountain View");
        CheckBox cityViewCheck = new CheckBox("City View");
        CheckBox gardenViewCheck = new CheckBox("Garden View");
        CheckBox poolViewCheck = new CheckBox("Pool View");
        viewsPane.getChildren().addAll(balconyCheck, seaViewCheck, mountainViewCheck,
                cityViewCheck, gardenViewCheck, poolViewCheck);

        amenitiesContainer.getChildren().addAll(
                bedLabel, bedPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                bathroomLabel, bathroomPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                techLabel, techPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                climateLabel, climatePane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                featuresLabel, featuresPane,
                new javafx.scene.layout.Region() {{ setPrefHeight(5); }},
                viewsLabel, viewsPane
        );

        // 🔥 LOAD existing amenities and check appropriate boxes
        if (room.getAmenities() != null && !room.getAmenities().isEmpty()) {
            String[] existingAmenities = room.getAmenities().split(",");
            for (String amenity : existingAmenities) {
                String trimmed = amenity.trim();

                // Bed & Bedroom
                if (trimmed.equals("King Bed")) kingBedCheck.setSelected(true);
                if (trimmed.equals("Queen Bed")) queenBedCheck.setSelected(true);
                if (trimmed.equals("Twin Beds")) twinBedsCheck.setSelected(true);
                if (trimmed.equals("Sofa Bed")) sofaBedCheck.setSelected(true);
                if (trimmed.equals("Blackout Curtains")) blackoutCurtainsCheck.setSelected(true);
                if (trimmed.equals("Soundproofing")) soundproofingCheck.setSelected(true);

                // Bathroom
                if (trimmed.equals("Private Bathroom")) privateBathroomCheck.setSelected(true);
                if (trimmed.equals("Bathtub")) bathtubCheck.setSelected(true);
                if (trimmed.equals("Rain Shower")) rainShowerCheck.setSelected(true);
                if (trimmed.equals("Hairdryer")) hairdryerCheck.setSelected(true);
                if (trimmed.equals("Free Toiletries")) toiletariesCheck.setSelected(true);
                if (trimmed.equals("Bathrobes & Slippers")) bathrobesCheck.setSelected(true);

                // Entertainment & Technology
                if (trimmed.equals("Flat-screen TV")) tvCheck.setSelected(true);
                if (trimmed.equals("Cable/Satellite TV")) cableCheck.setSelected(true);
                if (trimmed.equals("Netflix")) netflixCheck.setSelected(true);
                if (trimmed.equals("WiFi")) roomWifiCheck.setSelected(true);
                if (trimmed.equals("Telephone")) phoneCheck.setSelected(true);

                // Climate Control
                if (trimmed.equals("Air Conditioning")) roomACCheck.setSelected(true);
                if (trimmed.equals("Heating")) roomHeatingCheck.setSelected(true);
                if (trimmed.equals("Ceiling Fan")) ceilingFanCheck.setSelected(true);

                // Room Features
                if (trimmed.equals("Mini-bar")) minibarCheck.setSelected(true);
                if (trimmed.equals("In-room Safe")) safeCheck.setSelected(true);
                if (trimmed.equals("Work Desk")) deskCheck.setSelected(true);
                if (trimmed.equals("Seating Area")) seatingAreaCheck.setSelected(true);
                if (trimmed.equals("Coffee/Tea Maker")) coffeeCheck.setSelected(true);
                if (trimmed.equals("Minibar/Fridge")) fridgeCheck.setSelected(true);
                if (trimmed.equals("Iron & Ironing Board")) ironCheck.setSelected(true);
                if (trimmed.equals("Wake-up Service")) wakeUpCheck.setSelected(true);

                // Views & Outdoor
                if (trimmed.equals("Balcony/Terrace")) balconyCheck.setSelected(true);
                if (trimmed.equals("Sea View")) seaViewCheck.setSelected(true);
                if (trimmed.equals("Mountain View")) mountainViewCheck.setSelected(true);
                if (trimmed.equals("City View")) cityViewCheck.setSelected(true);
                if (trimmed.equals("Garden View")) gardenViewCheck.setSelected(true);
                if (trimmed.equals("Pool View")) poolViewCheck.setSelected(true);
            }
        }

        mainContainer.getChildren().addAll(basicInfoLabel, basicInfoGrid, amenitiesLabel, amenitiesContainer);
        scrollPane.setContent(mainContainer);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        applyRoomDialogTheme(dialog, mainContainer);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    room.setRoomName(roomNameField.getText().trim());
                    room.setRoomType(roomTypeCombo.getValue());
                    room.setCapacity(Integer.parseInt(capacityField.getText().trim()));
                    room.setSize(Double.parseDouble(sizeField.getText().trim()));
                    room.setPricePerNight(Double.parseDouble(priceField.getText().trim()));
                    room.setAvailable(availableCheck.isSelected());

                    // 🔥 NEW: Collect room amenities from checkboxes
                    List<String> roomAmenities = new ArrayList<>();

                    // Bed & Bedroom
                    if (kingBedCheck.isSelected()) roomAmenities.add("King Bed");
                    if (queenBedCheck.isSelected()) roomAmenities.add("Queen Bed");
                    if (twinBedsCheck.isSelected()) roomAmenities.add("Twin Beds");
                    if (sofaBedCheck.isSelected()) roomAmenities.add("Sofa Bed");
                    if (blackoutCurtainsCheck.isSelected()) roomAmenities.add("Blackout Curtains");
                    if (soundproofingCheck.isSelected()) roomAmenities.add("Soundproofing");

                    // Bathroom
                    if (privateBathroomCheck.isSelected()) roomAmenities.add("Private Bathroom");
                    if (bathtubCheck.isSelected()) roomAmenities.add("Bathtub");
                    if (rainShowerCheck.isSelected()) roomAmenities.add("Rain Shower");
                    if (hairdryerCheck.isSelected()) roomAmenities.add("Hairdryer");
                    if (toiletariesCheck.isSelected()) roomAmenities.add("Free Toiletries");
                    if (bathrobesCheck.isSelected()) roomAmenities.add("Bathrobes & Slippers");

                    // Entertainment & Technology
                    if (tvCheck.isSelected()) roomAmenities.add("Flat-screen TV");
                    if (cableCheck.isSelected()) roomAmenities.add("Cable/Satellite TV");
                    if (netflixCheck.isSelected()) roomAmenities.add("Netflix");
                    if (roomWifiCheck.isSelected()) roomAmenities.add("WiFi");
                    if (phoneCheck.isSelected()) roomAmenities.add("Telephone");

                    // Climate Control
                    if (roomACCheck.isSelected()) roomAmenities.add("Air Conditioning");
                    if (roomHeatingCheck.isSelected()) roomAmenities.add("Heating");
                    if (ceilingFanCheck.isSelected()) roomAmenities.add("Ceiling Fan");

                    // Room Features
                    if (minibarCheck.isSelected()) roomAmenities.add("Mini-bar");
                    if (safeCheck.isSelected()) roomAmenities.add("In-room Safe");
                    if (deskCheck.isSelected()) roomAmenities.add("Work Desk");
                    if (seatingAreaCheck.isSelected()) roomAmenities.add("Seating Area");
                    if (coffeeCheck.isSelected()) roomAmenities.add("Coffee/Tea Maker");
                    if (fridgeCheck.isSelected()) roomAmenities.add("Minibar/Fridge");
                    if (ironCheck.isSelected()) roomAmenities.add("Iron & Ironing Board");
                    if (wakeUpCheck.isSelected()) roomAmenities.add("Wake-up Service");

                    // Views & Outdoor
                    if (balconyCheck.isSelected()) roomAmenities.add("Balcony/Terrace");
                    if (seaViewCheck.isSelected()) roomAmenities.add("Sea View");
                    if (mountainViewCheck.isSelected()) roomAmenities.add("Mountain View");
                    if (cityViewCheck.isSelected()) roomAmenities.add("City View");
                    if (gardenViewCheck.isSelected()) roomAmenities.add("Garden View");
                    if (poolViewCheck.isSelected()) roomAmenities.add("Pool View");

                    room.setAmenities(String.join(",", roomAmenities));

                    return room;
                } catch (Exception e) {
                    showError("Invalid input values");
                    return null;
                }
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();
        if (result.isPresent()) {
            refreshRoomsView();
            logger.log(Level.INFO, "Room updated: " + room.getRoomName());
        }
    }

    /**
     * Handle deleting a room
     */
    private void handleDeleteRoom(Room room) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Room");
        confirmAlert.setHeaderText("Are you sure you want to delete this room?");
        confirmAlert.setContentText(room.getRoomName() + " - " + room.getRoomType());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // If room has an ID, it exists in database - mark for deletion
            if (room.getId() > 0) {
                roomsToDelete.add(room);
                logger.log(Level.INFO, "Room marked for deletion: " + room.getRoomName() + " (ID: " + room.getId() + ")");
            }

            // Remove from UI list
            rooms.remove(room);
            refreshRoomsView();
            logger.log(Level.INFO, "Room removed from list: " + room.getRoomName());
        }
    }

}