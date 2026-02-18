package tn.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.entities.Accommodation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AccommodationCardController {
    private static final Logger LOGGER = Logger.getLogger(AccommodationCardController.class.getName());
    private static final String PROJECT_ROOT = System.getProperty("user.dir");


    @FXML private ImageView accommodationImageView;
    @FXML private Label imageFallbackLabel;
    @FXML private Label nameLabel;
    @FXML private Label starsLabel;
    @FXML private Label typeCityLabel;
    @FXML private Label addressLabel;
    @FXML private Label amenitiesLabel;
    @FXML private Label statusLabel;
    @FXML private ToggleButton compareToggleButton;

    private Accommodation accommodation;
    private AccommodationsController parentController;

    public void setParentController(AccommodationsController parentController) {
        this.parentController = parentController;
    }

    public void setAccommodation(Accommodation accommodation) {
        this.accommodation = accommodation;
        bindData();
    }

    public void setCompareSelected(boolean selected) {
        if (compareToggleButton != null) {
            compareToggleButton.setSelected(selected);
            compareToggleButton.setText(selected ? "Selected" : "Compare");
            compareToggleButton.setStyle(selected
                    ? "-fx-background-color: linear-gradient(to right, #0EA5A4, #14B8A6); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 10; -fx-cursor: hand;"
                    : "-fx-background-color: linear-gradient(to right, #EEF2FF, #DBEAFE); -fx-text-fill: #1E3A8A; -fx-font-weight: 800; -fx-background-radius: 10; -fx-cursor: hand;");
        }
    }

    @FXML
    private void onCompareToggle() {
        if (accommodation == null || parentController == null) {
            return;
        }

        boolean requested = compareToggleButton.isSelected();
        boolean accepted = parentController.toggleCompareAccommodation(accommodation, requested);
        setCompareSelected(accepted && requested);
    }

    @FXML
    private void onDetails() {
        if (accommodation == null || parentController == null) {
            return;
        }
        parentController.openAccommodationDetails(accommodation);
    }

    private void bindData() {
        if (accommodation == null) return;

        nameLabel.setText(safe(accommodation.getName(), "Unnamed"));
        typeCityLabel.setText(
                safe(accommodation.getType(), "Accommodation") + " • " +
                safe(accommodation.getCity(), "Unknown city") + ", " +
                safe(accommodation.getCountry(), "Unknown country")
        );
        addressLabel.setText(safe(accommodation.getAddress(), "No address available"));
        statusLabel.setText(safe(accommodation.getStatus(), "Unknown"));

        starsLabel.setText(buildStars(Math.max(0, accommodation.getStars())));

        String amenities = accommodation.getAccommodationAmenities();
        if (amenities == null || amenities.isBlank()) {
            amenitiesLabel.setText("Amenities: Not specified");
        } else {
            amenitiesLabel.setText("Amenities: " + amenities);
        }

        loadImage(accommodation.getImagePath());
    }

    private void loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            showImageFallback(true);
            return;
        }

        try {
            Image image;
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                image = new Image(imagePath);
            } else {
                image = resolveLocalImage(imagePath);
                if (image == null) {
                    showImageFallback(true);
                    return;
                }
            }
            accommodationImageView.setImage(image);
            showImageFallback(false);
        } catch (Exception ignored) {
            showImageFallback(true);
        }
    }

    private Image resolveLocalImage(String rawPath) {
        String cleanPath = rawPath.trim();
        LOGGER.info("Loading user-card image: " + cleanPath);

        try {
            // 1) Absolute file path
            File absoluteFile = new File(cleanPath);
            if (absoluteFile.exists() && absoluteFile.isFile()) {
                return new Image(new FileInputStream(absoluteFile));
            }

            // 2) Relative to project root
            Path projectRelativePath = Paths.get(PROJECT_ROOT, cleanPath);
            if (Files.exists(projectRelativePath) && Files.isRegularFile(projectRelativePath)) {
                return new Image(new FileInputStream(projectRelativePath.toFile()));
            }

            // 3) Leading slash variant
            if (cleanPath.startsWith("/")) {
                String withoutLeadingSlash = cleanPath.substring(1);
                Path noSlashPath = Paths.get(PROJECT_ROOT, withoutLeadingSlash);
                if (Files.exists(noSlashPath) && Files.isRegularFile(noSlashPath)) {
                    return new Image(new FileInputStream(noSlashPath.toFile()));
                }
            }

            // 4) Platform separator variant
            String platformPath = cleanPath.replace("/", File.separator).replace("\\", File.separator);
            Path platformSpecificPath = Paths.get(PROJECT_ROOT, platformPath);
            if (Files.exists(platformSpecificPath) && Files.isRegularFile(platformSpecificPath)) {
                return new Image(new FileInputStream(platformSpecificPath.toFile()));
            }

            // 5) Search by filename in uploads/ recursively (same idea as admin)
            String filename = Paths.get(cleanPath).getFileName().toString();
            Path uploadsDir = Paths.get(PROJECT_ROOT, "uploads");
            if (Files.exists(uploadsDir)) {
                Path foundPath = findFileInDirectory(uploadsDir, filename);
                if (foundPath != null) {
                    return new Image(new FileInputStream(foundPath.toFile()));
                }
            }

            // 6) Classpath resource
            List<String> classpathCandidates = new ArrayList<>();
            classpathCandidates.add(cleanPath);
            classpathCandidates.add("/" + cleanPath);
            classpathCandidates.add("/uploads/images/" + filename);
            classpathCandidates.add("/images/" + filename);
            for (String candidate : classpathCandidates) {
                InputStream resourceStream = getClass().getResourceAsStream(candidate);
                if (resourceStream != null) {
                    return new Image(resourceStream);
                }
                URL resourceUrl = getClass().getResource(candidate);
                if (resourceUrl != null) {
                    return new Image(resourceUrl.toExternalForm());
                }
            }

            LOGGER.warning("User-card image not found: " + cleanPath);
        } catch (Exception e) {
            LOGGER.warning("Error loading user-card image '" + rawPath + "': " + e.getMessage());
        }
        return null;
    }

    private Path findFileInDirectory(Path directory, String filename) {
        try {
            return Files.walk(directory, 4)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.warning("Error searching uploads dir: " + e.getMessage());
            return null;
        }
    }

    private void showImageFallback(boolean visible) {
        imageFallbackLabel.setVisible(visible);
        imageFallbackLabel.setManaged(visible);
    }

    private String buildStars(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) sb.append("★");
        for (int i = stars; i < 5; i++) sb.append("☆");
        return sb.toString();
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
