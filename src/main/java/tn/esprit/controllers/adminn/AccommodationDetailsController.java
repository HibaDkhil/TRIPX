package tn.esprit.controllers.adminn;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Accommodation;
import tn.esprit.entities.Room;
import tn.esprit.services.RoomService;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class AccommodationDetailsController implements Initializable {
    private static final String PROJECT_ROOT = System.getProperty("user.dir");

    @FXML private Label titleLabel;
    @FXML private Label nameLabel;
    @FXML private Label locationLabel;
    @FXML private Label typeLabel;
    @FXML private Label starsLabel;
    @FXML private Label ratingLabel;
    @FXML private Label statusLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label amenitiesLabel;
    @FXML private Label contactLabel;
    @FXML private ImageView mainImageView;
    @FXML private VBox roomsContainer;
    @FXML private javafx.scene.control.Button closeButton;

    private Accommodation accommodation;
    private AccommodationAdminController parentController;
    private RoomService roomService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roomService = new RoomService();
        closeButton.setOnAction(e -> handleClose());
    }

    public void setAccommodation(Accommodation accommodation) {
        this.accommodation = accommodation;
        loadAccommodationDetails();
    }

    public void setParentController(AccommodationAdminController parentController) {
        this.parentController = parentController;
    }

    private void loadAccommodationDetails() {
        if (accommodation == null) return;

        // Set basic information
        titleLabel.setText(accommodation.getName());
        nameLabel.setText(accommodation.getName());

        // Location
        String location = "📍 " + accommodation.getCity();
        if (accommodation.getCountry() != null && !accommodation.getCountry().isEmpty()) {
            location += ", " + accommodation.getCountry();
        }
        if (accommodation.getAddress() != null && !accommodation.getAddress().isEmpty()) {
            location += "\n" + accommodation.getAddress();
        }
        locationLabel.setText(location);

        // Type and Stars
        typeLabel.setText("🏠 " + accommodation.getType());
        starsLabel.setText(getStarsText(accommodation.getStars()));

        // Rating
        if (accommodation.getRating() > 0) {
            ratingLabel.setText("★ " + String.format("%.1f", accommodation.getRating()) + " / 5.0");
            ratingLabel.setVisible(true);
        } else {
            ratingLabel.setText("No ratings yet");
        }

        // Status with color coding
        statusLabel.setText(accommodation.getStatus());
        String statusColor = getStatusColor(accommodation.getStatus());
        statusLabel.setStyle(
                "-fx-background-color: " + statusColor + "20;" +
                        "-fx-text-fill: " + statusColor + ";" +
                        "-fx-padding: 5 15;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-weight: bold;"
        );

        // Description
        if (accommodation.getDescription() != null && !accommodation.getDescription().isEmpty()) {
            descriptionLabel.setText(accommodation.getDescription());
        } else {
            descriptionLabel.setText("No description available.");
        }

        // Contact Information
        StringBuilder contactInfo = new StringBuilder();
        if (accommodation.getPhone() != null && !accommodation.getPhone().isEmpty()) {
            contactInfo.append("📞 ").append(accommodation.getPhone()).append("\n");
        }
        if (accommodation.getEmail() != null && !accommodation.getEmail().isEmpty()) {
            contactInfo.append("✉️ ").append(accommodation.getEmail()).append("\n");
        }
        if (accommodation.getWebsite() != null && !accommodation.getWebsite().isEmpty()) {
            contactInfo.append("🌐 ").append(accommodation.getWebsite()).append("\n");
        }
        contactLabel.setText(contactInfo.toString());

        // Load image
        loadAccommodationImage();

        // Load rooms
        loadRooms();

        // Set amenities
        setAmenities();
    }

    private String getStarsText(int stars) {
        if (stars <= 0) return "☆☆☆☆☆";

        int starCount = Math.min(stars, 5);
        return "★".repeat(starCount) + "☆".repeat(5 - starCount) + " (" + stars + " Stars)";
    }

    private String getStatusColor(String status) {
        if (status == null) return "#95a5a6";

        return switch (status.toLowerCase()) {
            case "active" -> "#27ae60";
            case "inactive" -> "#e74c3c";
            case "pending" -> "#f39c12";
            case "maintenance" -> "#3498db";
            default -> "#95a5a6";
        };
    }

    /**
     * IMPROVED IMAGE LOADING - matches AccommodationCard logic
     */
    private void loadAccommodationImage() {
        String imagePath = accommodation.getImagePath();

        if (imagePath == null || imagePath.trim().isEmpty()) {
            System.out.println("No image path for accommodation: " + accommodation.getName());
            setDefaultImage();
            return;
        }

        System.out.println("Loading image for details view: " + imagePath);

        try {
            String cleanPath = imagePath.trim();
            Image image = null;

            // Strategy 1: Try as absolute file path
            File absoluteFile = new File(cleanPath);
            if (absoluteFile.exists() && absoluteFile.isFile()) {
                System.out.println("✓ Loaded from absolute path");
                image = new Image(new FileInputStream(absoluteFile));
            }

            // Strategy 2: Try relative to project root
            if (image == null) {
                Path projectRelativePath = Paths.get(PROJECT_ROOT, cleanPath);
                if (Files.exists(projectRelativePath) && Files.isRegularFile(projectRelativePath)) {
                    System.out.println("✓ Loaded from project-relative path: " + projectRelativePath);
                    image = new Image(new FileInputStream(projectRelativePath.toFile()));
                }
            }

            // Strategy 3: Try without leading slash
            if (image == null && cleanPath.startsWith("/")) {
                String withoutLeadingSlash = cleanPath.substring(1);
                Path noSlashPath = Paths.get(PROJECT_ROOT, withoutLeadingSlash);
                if (Files.exists(noSlashPath) && Files.isRegularFile(noSlashPath)) {
                    System.out.println("✓ Loaded after removing leading slash");
                    image = new Image(new FileInputStream(noSlashPath.toFile()));
                }
            }

            // Strategy 4: Try with platform separators
            if (image == null) {
                String platformPath = cleanPath.replace("/", File.separator).replace("\\", File.separator);
                Path platformSpecificPath = Paths.get(PROJECT_ROOT, platformPath);
                if (Files.exists(platformSpecificPath) && Files.isRegularFile(platformSpecificPath)) {
                    System.out.println("✓ Loaded with platform separators");
                    image = new Image(new FileInputStream(platformSpecificPath.toFile()));
                }
            }

            // Strategy 5: Search by filename in uploads directory
            if (image == null) {
                String filename = Paths.get(cleanPath).getFileName().toString();
                System.out.println("Searching for filename: " + filename);

                Path uploadsDir = Paths.get(PROJECT_ROOT, "uploads");
                if (Files.exists(uploadsDir)) {
                    Path foundPath = findFileInDirectory(uploadsDir, filename);
                    if (foundPath != null) {
                        System.out.println("✓ Found file by searching uploads: " + foundPath);
                        image = new Image(new FileInputStream(foundPath.toFile()));
                    }
                }
            }

            // Strategy 6: Try as classpath resource
            if (image == null) {
                InputStream resourceStream = getClass().getResourceAsStream("/" + cleanPath);
                if (resourceStream != null) {
                    System.out.println("✓ Loaded from classpath");
                    image = new Image(resourceStream);
                }
            }

            // If image was loaded successfully, display it
            if (image != null && !image.isError()) {
                mainImageView.setImage(image);
                mainImageView.setStyle(""); // Clear any background style
                return;
            }

            System.err.println("✗ Could not load image after all strategies: " + imagePath);

        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            e.printStackTrace();
        }

        setDefaultImage();
    }

    /**
     * Search for a file recursively in a directory
     */
    private Path findFileInDirectory(Path directory, String filename) {
        try {
            return Files.walk(directory, 3)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Error searching directory: " + e.getMessage());
            return null;
        }
    }

    private void setDefaultImage() {
        String gradient = getTypeGradient(accommodation.getType());
        mainImageView.setStyle(
                "-fx-background-color: " + gradient + ";" +
                        "-fx-background-radius: 0;"
        );

        mainImageView.setImage(null);
    }

    private String getTypeGradient(String type) {
        if (type == null) return "linear-gradient(to right, #3498db, #2980b9)";

        return switch (type.toLowerCase()) {
            case "hotel" -> "linear-gradient(to right, #3498db, #2980b9)";
            case "apartment" -> "linear-gradient(to right, #9b59b6, #8e44ad)";
            case "villa" -> "linear-gradient(to right, #2ecc71, #27ae60)";
            case "resort" -> "linear-gradient(to right, #e74c3c, #c0392b)";
            case "hostel" -> "linear-gradient(to right, #f39c12, #d35400)";
            default -> "linear-gradient(to right, #95a5a6, #7f8c8d)";
        };
    }

    private void loadRooms() {
        roomsContainer.getChildren().clear();

        try {
            List<Room> rooms;
            if (accommodation.getRooms() != null && !accommodation.getRooms().isEmpty()) {
                rooms = accommodation.getRooms();
            } else {
                rooms = roomService.getRoomsByAccommodationId(accommodation.getId());
            }

            if (rooms.isEmpty()) {
                Label noRoomsLabel = new Label("No rooms available for this accommodation.");
                noRoomsLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
                roomsContainer.getChildren().add(noRoomsLabel);
                return;
            }

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE);

            for (Room room : rooms) {
                VBox roomCard = createRoomCard(room, currencyFormat);
                roomsContainer.getChildren().add(roomCard);
            }

        } catch (Exception e) {
            System.err.println("Error loading rooms: " + e.getMessage());
            Label errorLabel = new Label("Error loading rooms information.");
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px; -fx-padding: 20;");
            roomsContainer.getChildren().add(errorLabel);
        }
    }

    private VBox createRoomCard(Room room, NumberFormat currencyFormat) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 15;"
        );

        // Room name and type
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label roomName = new Label(room.getRoomType() + " - " + room.getRoomName());
        roomName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label availabilityLabel = createAvailabilityLabel(room.isAvailable());

        headerBox.getChildren().addAll(roomName, availabilityLabel);

        // Room details
        VBox detailsBox = new VBox(5);

        // Capacity
        HBox capacityBox = new HBox(5);
        capacityBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label capacityIcon = new Label("👥");
        Label capacityLabel = new Label("Capacity: " + room.getCapacity() + " person(s)");
        capacityLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        capacityBox.getChildren().addAll(capacityIcon, capacityLabel);

        // Beds
        HBox bedsBox = new HBox(5);
        bedsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label bedsIcon = new Label("🛏️");
        Label bedsLabel = new Label(room.getBedType() + " bed(s)");
        bedsLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
        bedsBox.getChildren().addAll(bedsIcon, bedsLabel);

        // Price
        HBox priceBox = new HBox(5);
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label priceIcon = new Label("💰");
        Label priceLabel = new Label("Price: " + currencyFormat.format(room.getPricePerNight()) + " / night");
        priceLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 14px; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(priceIcon, priceLabel);

        // Description
        if (room.getDescription() != null && !room.getDescription().isEmpty()) {
            Label descriptionLabel = new Label(room.getDescription());
            descriptionLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
            descriptionLabel.setWrapText(true);
            detailsBox.getChildren().add(descriptionLabel);
        }

        detailsBox.getChildren().addAll(capacityBox, bedsBox, priceBox);

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button viewRoomDetailsBtn = new Button("View Room Details");
        viewRoomDetailsBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #0A4174, #4E8EA2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 7 14;" +
                        "-fx-cursor: hand;"
        );
        viewRoomDetailsBtn.setOnAction(e -> {
            if (parentController != null && accommodation != null) {
                parentController.showRoomDetailsPage(accommodation, room);
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        actions.getChildren().addAll(viewRoomDetailsBtn, spacer);

        card.getChildren().addAll(headerBox, detailsBox, actions);

        return card;
    }

    private Label createAvailabilityLabel(boolean isAvailable) {
        String availability = isAvailable ? "Available" : "Occupied";
        String availabilityColor = isAvailable ? "#27ae60" : "#e74c3c";
        Label availabilityLabel = new Label(availability);
        availabilityLabel.setStyle(
                "-fx-background-color: " + availabilityColor + "20;" +
                        "-fx-text-fill: " + availabilityColor + ";" +
                        "-fx-padding: 3 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-font-size: 12px;"
        );
        return availabilityLabel;
    }

    private void setAmenities() {
        StringBuilder amenities = new StringBuilder();

        // Add basic amenities
        amenities.append("✓ Free WiFi\n");
        amenities.append("✓ Air Conditioning\n");

        // Add star-based amenities
        int stars = accommodation.getStars();
        if (stars >= 4) {
            amenities.append("✓ Swimming Pool\n");
            amenities.append("✓ Spa & Wellness\n");
            amenities.append("✓ Restaurant\n");
        } else if (stars >= 3) {
            amenities.append("✓ Fitness Center\n");
            amenities.append("✓ Room Service\n");
        }

        // Add type-specific amenities
        String type = accommodation.getType().toLowerCase();
        if (type.contains("hotel")) {
            amenities.append("✓ 24/7 Front Desk\n");
            amenities.append("✓ Daily Housekeeping\n");
        } else if (type.contains("apartment")) {
            amenities.append("✓ Fully Equipped Kitchen\n");
            amenities.append("✓ Laundry Facilities\n");
        } else if (type.contains("villa")) {
            amenities.append("✓ Private Garden\n");
            amenities.append("✓ Parking Space\n");
        }

        amenitiesLabel.setText(amenities.toString());
    }

    @FXML
    private void handleClose() {
        if (parentController != null) {
            parentController.closeModal();
        }
    }
}
