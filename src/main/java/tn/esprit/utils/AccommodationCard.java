package tn.esprit.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Accommodation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class AccommodationCard extends VBox {
    private static final Logger LOGGER = Logger.getLogger(AccommodationCard.class.getName());
    private static final String PROJECT_ROOT = System.getProperty("user.dir");

    private final Accommodation accommodation;
    private ImageView imageView;
    private int customHeight;

    // Constructor with just accommodation (basic - for admin list)
    public AccommodationCard(Accommodation accommodation) {
        this(accommodation, 200);
    }

    // Constructor with accommodation and height (for admin with custom height)
    public AccommodationCard(Accommodation accommodation, int height) {
        this.accommodation = accommodation;
        this.customHeight = height;
        setupCard("grid-view-card");
    }

    private void setupCard(String additionalStyleClass) {
        this.setAlignment(Pos.TOP_CENTER);
        this.setSpacing(0);

        // Apply CSS classes
        this.getStyleClass().addAll("accommodation-card", "accommodation-card-container");
        if (additionalStyleClass != null && !additionalStyleClass.isEmpty()) {
            this.getStyleClass().add(additionalStyleClass);
        }

        // ===== IMAGE CONTAINER WITH PROPER CLIPPING =====
        VBox imageContainer = new VBox();
        imageContainer.getStyleClass().add("image-container");
        imageContainer.setMinHeight(customHeight);
        imageContainer.setMaxHeight(customHeight);
        imageContainer.setPrefHeight(customHeight);

        // Create ImageView with proper settings
        imageView = new ImageView();
        imageView.setFitWidth(350);
        imageView.setFitHeight(customHeight);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("accommodation-image");

        // Load and set image
        Image image = loadImage(accommodation.getImagePath());
        imageView.setImage(image);

        // Add image to container
        imageContainer.getChildren().add(imageView);

        // ===== CONTENT CONTAINER =====
        VBox contentBox = new VBox(12);
        contentBox.getStyleClass().add("card-content");
        contentBox.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        contentBox.setStyle("-fx-padding: 16;");

        // Name (bold, larger)
        Label nameLabel = new Label(accommodation.getName());
        nameLabel.getStyleClass().add("accommodation-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(310);

        // Type badge and location in horizontal layout
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(accommodation.getType());
        typeBadge.getStyleClass().add("type-badge");
        typeBadge.setStyle("-fx-background-color: " + getTypeColor(accommodation.getType()) + ";");

        // Location (with icon)
        HBox locationBox = new HBox(6);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label locationIcon = new Label("📍");
        locationIcon.getStyleClass().add("location-icon");

        String location = accommodation.getCity() + ", " + accommodation.getCountry();
        Label locationLabel = new Label(location);
        locationLabel.getStyleClass().add("location-label");
        locationLabel.setWrapText(true);
        locationLabel.setMaxWidth(200);
        locationBox.getChildren().addAll(locationIcon, locationLabel);

        topRow.getChildren().addAll(typeBadge, locationBox);

        // Add to content
        contentBox.getChildren().addAll(nameLabel, topRow);

        // Add stars display
        addStarsDisplay(contentBox);

        // Description (shortened)
        if (accommodation.getDescription() != null && !accommodation.getDescription().isEmpty()) {
            String shortDesc = accommodation.getDescription();
            if (shortDesc.length() > 80) {
                shortDesc = shortDesc.substring(0, 77) + "...";
            }
            Label descLabel = new Label(shortDesc);
            descLabel.getStyleClass().add("description-label");
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(310);
            contentBox.getChildren().add(descLabel);
        }

        // ===== BOTTOM SECTION WITH STATUS =====
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.getStyleClass().add("card-bottom");

        // Status badge
        Label statusLabel = new Label(accommodation.getStatus());
        statusLabel.getStyleClass().add("status-badge");
        statusLabel.getStyleClass().add(accommodation.getStatus().equalsIgnoreCase("Active") ?
                "status-badge-active" : "status-badge-inactive");

        bottomRow.getChildren().add(statusLabel);

        contentBox.getChildren().addAll(spacer, bottomRow);

        // Add image and content to card
        this.getChildren().addAll(imageContainer, contentBox);
    }

    private void addStarsDisplay(VBox contentBox) {
        // Display STARS instead of rating
        int stars = accommodation.getStars();
        if (stars > 0) {
            HBox starsBox = new HBox(4);
            starsBox.setAlignment(Pos.CENTER_LEFT);
            starsBox.getStyleClass().add("stars-container");

            // Display star icons based on star rating
            for (int i = 0; i < 5; i++) {
                Label star = new Label(i < stars ? "★" : "☆");
                star.getStyleClass().add(i < stars ? "star-filled" : "star-empty");
                star.setStyle(i < stars ?
                        "-fx-text-fill: #FFB300; -fx-font-size: 16;" :
                        "-fx-text-fill: #E0E0E0; -fx-font-size: 16;");
                starsBox.getChildren().add(star);
            }

            // Add star count label
            Label starsLabel = new Label(stars + " stars");
            starsLabel.getStyleClass().add("stars-text");
            starsLabel.setStyle("-fx-text-fill: #FF8F00; -fx-font-weight: bold; -fx-font-size: 14;");

            starsBox.getChildren().add(starsLabel);
            contentBox.getChildren().add(starsBox);
        } else {
            Label noStars = new Label("No star rating");
            noStars.getStyleClass().add("no-stars");
            noStars.setStyle("-fx-text-fill: #BDBDBD; -fx-font-style: italic;");
            contentBox.getChildren().add(noStars);
        }
    }

    private String getTypeColor(String type) {
        if (type == null) return "#90CAF9";

        String lowerType = type.toLowerCase();
        switch (lowerType) {
            case "hotel":
                return "#90CAF9"; // Pastel Blue
            case "villa":
                return "#A5D6A7"; // Pastel Green
            case "apartment":
                return "#FFCC80"; // Pastel Orange
            case "resort":
                return "#CE93D8"; // Pastel Purple
            case "hostel":
                return "#80DEEA"; // Pastel Turquoise
            case "airbnb":
                return "#EF9A9A"; // Pastel Red
            case "guest house":
                return "#FFE082"; // Pastel Yellow
            case "bed & breakfast":
                return "#FFAB91"; // Pastel Coral
            default:
                return "#B0BEC5"; // Pastel Gray
        }
    }

    /**
     * IMPROVED IMAGE LOADING - handles all path formats correctly
     */
    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            LOGGER.warning("No image path provided, using placeholder");
            return getPlaceholderImage();
        }

        LOGGER.info("Loading image: " + imagePath);

        try {
            // Clean the path - remove any leading/trailing spaces
            String cleanPath = imagePath.trim();

            // Strategy 1: Try as absolute file path first
            File absoluteFile = new File(cleanPath);
            if (absoluteFile.exists() && absoluteFile.isFile()) {
                LOGGER.info("✓ Loaded from absolute path: " + absoluteFile.getAbsolutePath());
                return new Image(new FileInputStream(absoluteFile));
            }

            // Strategy 2: Try relative to project root (most common for uploaded files)
            // This handles paths like: uploads/images/hotels/filename.jpg
            Path projectRelativePath = Paths.get(PROJECT_ROOT, cleanPath);
            if (Files.exists(projectRelativePath) && Files.isRegularFile(projectRelativePath)) {
                LOGGER.info("✓ Loaded from project-relative path: " + projectRelativePath);
                return new Image(new FileInputStream(projectRelativePath.toFile()));
            }

            // Strategy 3: If path starts with forward slash, try without it
            if (cleanPath.startsWith("/")) {
                String withoutLeadingSlash = cleanPath.substring(1);
                Path noSlashPath = Paths.get(PROJECT_ROOT, withoutLeadingSlash);
                if (Files.exists(noSlashPath) && Files.isRegularFile(noSlashPath)) {
                    LOGGER.info("✓ Loaded after removing leading slash: " + noSlashPath);
                    return new Image(new FileInputStream(noSlashPath.toFile()));
                }
            }

            // Strategy 4: Try with platform-specific separators
            String platformPath = cleanPath.replace("/", File.separator).replace("\\", File.separator);
            Path platformSpecificPath = Paths.get(PROJECT_ROOT, platformPath);
            if (Files.exists(platformSpecificPath) && Files.isRegularFile(platformSpecificPath)) {
                LOGGER.info("✓ Loaded with platform separators: " + platformSpecificPath);
                return new Image(new FileInputStream(platformSpecificPath.toFile()));
            }

            // Strategy 5: Search by filename in uploads directory
            String filename = Paths.get(cleanPath).getFileName().toString();
            LOGGER.info("Searching for filename: " + filename);

            Path uploadsDir = Paths.get(PROJECT_ROOT, "uploads");
            if (Files.exists(uploadsDir)) {
                Path foundPath = findFileInDirectory(uploadsDir, filename);
                if (foundPath != null) {
                    LOGGER.info("✓ Found file by searching uploads: " + foundPath);
                    return new Image(new FileInputStream(foundPath.toFile()));
                }
            }

            // Strategy 6: Try as classpath resource
            InputStream resourceStream = getClass().getResourceAsStream("/" + cleanPath);
            if (resourceStream != null) {
                LOGGER.info("✓ Loaded from classpath: " + cleanPath);
                return new Image(resourceStream);
            }

            // If we got here, image wasn't found
            LOGGER.warning("✗ Image not found after trying all strategies: " + imagePath);

        } catch (Exception e) {
            LOGGER.warning("Error loading image '" + imagePath + "': " + e.getMessage());
            e.printStackTrace();
        }

        return getPlaceholderImage();
    }

    /**
     * Recursively search for a file in a directory
     */
    private Path findFileInDirectory(Path directory, String filename) {
        try {
            return Files.walk(directory, 3)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.warning("Error searching directory: " + e.getMessage());
            return null;
        }
    }

    private Image getPlaceholderImage() {
        // Try to load placeholder from resources first
        try {
            InputStream placeholder = getClass().getResourceAsStream("/images/placeholder.png");
            if (placeholder != null) {
                return new Image(placeholder);
            }
        } catch (Exception ignored) {
        }

        // Generate SVG placeholder with pastel gradient
        String gradientColor = getTypeColor(accommodation.getType());
        String darkerColor = darkenColor(gradientColor, 0.15);

        String svg = String.format(
                "<svg width='350' height='%d' xmlns='http://www.w3.org/2000/svg'>" +
                        "<defs>" +
                        "<linearGradient id='grad' x1='0%%' y1='0%%' x2='100%%' y2='100%%'>" +
                        "<stop offset='0%%' style='stop-color:%s;stop-opacity:0.95' />" +
                        "<stop offset='100%%' style='stop-color:%s;stop-opacity:0.95' />" +
                        "</linearGradient>" +
                        "</defs>" +
                        "<rect width='350' height='%d' fill='url(#grad)'/>" +
                        "<text x='50%%' y='50%%' font-family='Arial' font-size='18' font-weight='600' " +
                        "fill='white' text-anchor='middle' dominant-baseline='middle' opacity='0.9'>%s</text>" +
                        "</svg>",
                customHeight, gradientColor, darkerColor, customHeight,
                accommodation.getType().toUpperCase()
        );

        String base64 = java.util.Base64.getEncoder().encodeToString(svg.getBytes());
        return new Image("data:image/svg+xml;base64," + base64);
    }

    private String darkenColor(String hexColor, double factor) {
        try {
            hexColor = hexColor.replace("#", "");
            int r = Integer.parseInt(hexColor.substring(0, 2), 16);
            int g = Integer.parseInt(hexColor.substring(2, 4), 16);
            int b = Integer.parseInt(hexColor.substring(4, 6), 16);

            r = Math.max(0, (int) (r * (1 - factor)));
            g = Math.max(0, (int) (g * (1 - factor)));
            b = Math.max(0, (int) (b * (1 - factor)));

            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return "#78909C";
        }
    }

    public Accommodation getAccommodation() {
        return accommodation;
    }

    public ImageView getImageView() {
        return imageView;
    }

    /**
     * Add action buttons to the card (for admin view)
     */
    public void addActionButtons(Runnable onView, Runnable onEdit, Runnable onDelete) {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getStyleClass().add("action-buttons-container");
        buttonBox.setStyle("-fx-padding: 12 20 16 20;");

        // View Button
        javafx.scene.control.Button viewBtn = new javafx.scene.control.Button("👁 View");
        viewBtn.getStyleClass().addAll("button", "action-btn-view");
        viewBtn.setOnAction(e -> onView.run());

        // Edit Button
        javafx.scene.control.Button editBtn = new javafx.scene.control.Button("✏ Edit");
        editBtn.getStyleClass().addAll("button", "action-btn-edit");
        editBtn.setOnAction(e -> onEdit.run());

        // Delete Button
        javafx.scene.control.Button deleteBtn = new javafx.scene.control.Button("🗑 Delete");
        deleteBtn.getStyleClass().addAll("button", "action-btn-delete");
        deleteBtn.setOnAction(e -> onDelete.run());

        buttonBox.getChildren().addAll(viewBtn, editBtn, deleteBtn);
        this.getChildren().add(buttonBox);
    }
}