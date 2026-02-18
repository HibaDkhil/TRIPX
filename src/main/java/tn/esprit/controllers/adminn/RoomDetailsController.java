package tn.esprit.controllers.adminn;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.esprit.entities.Accommodation;
import tn.esprit.entities.Room;
import tn.esprit.entities.RoomImage;
import tn.esprit.services.RoomImageService;
import tn.esprit.services.RoomService;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoomDetailsController {
    private static final String PROJECT_ROOT = System.getProperty("user.dir");

    @FXML private Label pageTitleLabel;
    @FXML private Label breadcrumbsLabel;
    @FXML private Label roomNameLabel;
    @FXML private Label roomTypeLabel;
    @FXML private Label capacityLabel;
    @FXML private Label sizeLabel;
    @FXML private Label priceLabel;
    @FXML private Label statusLabel;
    @FXML private Label roomDescriptionLabel;
    @FXML private Label imageMetaLabel;
    @FXML private Label galleryCountLabel;
    @FXML private ImageView mainImageView;
    @FXML private HBox thumbnailsContainer;
    @FXML private VBox amenitiesContainer;
    @FXML private Button setPrimaryButton;
    @FXML private Button moveLeftButton;
    @FXML private Button moveRightButton;
    @FXML private Button deleteImageButton;

    private final RoomImageService roomImageService = new RoomImageService();
    private final RoomService roomService = new RoomService();

    private Accommodation accommodation;
    private Room room;
    private AccommodationAdminController parentController;
    private List<RoomImage> images = new ArrayList<>();
    private RoomImage selectedImage;
    private int selectedIndex = -1;
    private int pendingSelectedImageId = -1;

    @FXML
    public void initialize() {
        setMainImagePlaceholder();
        updateImageActionButtons();
    }

    public void setParentController(AccommodationAdminController parentController) {
        this.parentController = parentController;
    }

    public void setContext(Accommodation accommodation, Room room) {
        this.accommodation = accommodation;
        this.room = roomService.getRoomById(room.getId());
        if (this.room == null) {
            this.room = room;
        }
        renderRoomInfo();
        refreshImages();
    }

    private void renderRoomInfo() {
        if (room == null) return;

        pageTitleLabel.setText("Room Details");
        String accommodationName = accommodation != null ? safe(accommodation.getName(), "Accommodation") : "Accommodation";
        breadcrumbsLabel.setText("Dashboard  >  Accommodations  >  " + accommodationName + "  >  " + safe(room.getRoomName(), "Room"));

        roomNameLabel.setText(safe(room.getRoomName(), "Room"));
        roomTypeLabel.setText("Type: " + safe(room.getRoomType(), "N/A"));
        capacityLabel.setText("Capacity: " + room.getCapacity() + " person(s)");
        sizeLabel.setText("Size: " + String.format(Locale.US, "%.1f", room.getSize()) + " m²");
        priceLabel.setText("Price: " + NumberFormat.getCurrencyInstance(Locale.FRANCE).format(room.getPricePerNight()) + " / night");
        statusLabel.setText(room.isAvailable() ? "Available" : "Unavailable");
        statusLabel.setStyle(room.isAvailable()
                ? "-fx-background-color: rgba(39,174,96,0.15); -fx-text-fill: #27ae60; -fx-background-radius: 999; -fx-padding: 4 12; -fx-font-weight: 700;"
                : "-fx-background-color: rgba(231,76,60,0.15); -fx-text-fill: #e74c3c; -fx-background-radius: 999; -fx-padding: 4 12; -fx-font-weight: 700;");

        roomDescriptionLabel.setText(safe(room.getDescription(), "No description available."));
        roomDescriptionLabel.setWrapText(true);

        renderAmenities();
    }

    private void renderAmenities() {
        amenitiesContainer.getChildren().clear();
        String rawAmenities = room != null ? room.getAmenities() : "";
        if (rawAmenities == null || rawAmenities.isBlank()) {
            Label empty = new Label("No amenities listed.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13;");
            amenitiesContainer.getChildren().add(empty);
            return;
        }

        String[] items = rawAmenities.split(",");
        for (String item : items) {
            String trimmed = item.trim();
            if (trimmed.isBlank()) continue;
            Label line = new Label("• " + trimmed);
            line.setStyle("-fx-text-fill: #334155; -fx-font-size: 13;");
            amenitiesContainer.getChildren().add(line);
        }
    }

    private void refreshImages() {
        if (room == null) return;
        images = roomImageService.getByRoomId(room.getId());
        thumbnailsContainer.getChildren().clear();

        galleryCountLabel.setText(images.size() + " image(s)");

        if (images.isEmpty()) {
            selectedImage = null;
            selectedIndex = -1;
            setMainImagePlaceholder();
            imageMetaLabel.setText("No image selected");
            updateImageActionButtons();
            return;
        }

        for (int i = 0; i < images.size(); i++) {
            RoomImage image = images.get(i);
            VBox thumbCard = buildThumbnailCard(image, i);
            thumbnailsContainer.getChildren().add(thumbCard);
        }

        // Preserve selection if possible
        int nextIndex = 0;
        if (pendingSelectedImageId > 0) {
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i).getId() == pendingSelectedImageId) {
                    nextIndex = i;
                    break;
                }
            }
            pendingSelectedImageId = -1;
        } else if (selectedImage != null) {
            for (int i = 0; i < images.size(); i++) {
                if (images.get(i).getId() == selectedImage.getId()) {
                    nextIndex = i;
                    break;
                }
            }
        } else {
            int primaryIndex = findPrimaryIndex();
            if (primaryIndex >= 0) nextIndex = primaryIndex;
        }
        selectImage(nextIndex);
    }

    private VBox buildThumbnailCard(RoomImage image, int index) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(6));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #CBD5E1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        ImageView thumb = new ImageView(loadImageByPath(image.getFilePath()));
        thumb.setFitWidth(120);
        thumb.setFitHeight(78);
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);

        Label flag = new Label(image.isPrimary() ? "Primary" : "Image");
        flag.setStyle(image.isPrimary()
                ? "-fx-text-fill: #0F766E; -fx-font-size: 11; -fx-font-weight: 700;"
                : "-fx-text-fill: #64748B; -fx-font-size: 11;");

        card.getChildren().addAll(thumb, flag);
        card.setOnMouseClicked(event -> selectImage(index));
        card.setUserData(image.getId());

        card.setOnDragDetected(event -> {
            if (images.size() <= 1) return;
            Dragboard dragboard = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(image.getId()));
            dragboard.setContent(content);
            event.consume();
        });

        card.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (event.getGestureSource() != card && dragboard.hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        card.setOnDragEntered(event -> {
            Dragboard dragboard = event.getDragboard();
            if (event.getGestureSource() != card && dragboard.hasString()) {
                card.setStyle(
                        "-fx-background-color: #DBEAFE;" +
                                "-fx-border-color: #2563EB;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 10;" +
                                "-fx-background-radius: 10;"
                );
            }
        });

        card.setOnDragExited(event -> {
            boolean isSelected = selectedImage != null && selectedImage.getId() == image.getId();
            card.setStyle(isSelected
                    ? "-fx-background-color: #EFF6FF; -fx-border-color: #2563EB; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;"
                    : "-fx-background-color: white; -fx-border-color: #CBD5E1; -fx-border-radius: 10; -fx-background-radius: 10;");
        });

        card.setOnDragDropped(event -> {
            boolean success = false;
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString()) {
                int draggedImageId = parseIntSafe(dragboard.getString());
                int targetImageId = image.getId();
                if (draggedImageId > 0 && targetImageId > 0 && draggedImageId != targetImageId) {
                    reorderByDragAndDrop(draggedImageId, targetImageId);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        return card;
    }

    private void selectImage(int index) {
        if (index < 0 || index >= images.size()) return;
        selectedIndex = index;
        selectedImage = images.get(index);

        for (int i = 0; i < thumbnailsContainer.getChildren().size(); i++) {
            var node = thumbnailsContainer.getChildren().get(i);
            if (node instanceof VBox box) {
                box.setStyle(i == index
                        ? "-fx-background-color: #EFF6FF; -fx-border-color: #2563EB; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;"
                        : "-fx-background-color: white; -fx-border-color: #CBD5E1; -fx-border-radius: 10; -fx-background-radius: 10;");
            }
        }

        Image image = loadImageByPath(selectedImage.getFilePath());
        mainImageView.setImage(image);
        mainImageView.setStyle("");
        imageMetaLabel.setText(
                safe(selectedImage.getFileName(), "image") +
                        " • order " + selectedImage.getDisplayOrder() +
                        (selectedImage.isPrimary() ? " • primary" : "")
        );
        updateImageActionButtons();
    }

    private int findPrimaryIndex() {
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).isPrimary()) return i;
        }
        return -1;
    }

    private void setMainImagePlaceholder() {
        mainImageView.setImage(null);
        mainImageView.setStyle(
                "-fx-background-color: linear-gradient(to right, #E2E8F0, #CBD5E1);" +
                        "-fx-background-radius: 14;"
        );
    }

    private void updateImageActionButtons() {
        boolean hasSelection = selectedImage != null;
        setPrimaryButton.setDisable(!hasSelection || selectedImage.isPrimary());
        deleteImageButton.setDisable(!hasSelection);
        moveLeftButton.setDisable(!hasSelection || selectedIndex <= 0);
        moveRightButton.setDisable(!hasSelection || selectedIndex < 0 || selectedIndex >= images.size() - 1);
    }

    @FXML
    private void handleUploadImages() {
        if (room == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload Room Images");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.gif", "*.bmp")
        );

        List<File> files = chooser.showOpenMultipleDialog(mainImageView.getScene().getWindow());
        if (files == null || files.isEmpty()) return;

        int uploaded = 0;
        for (File file : files) {
            try {
                roomImageService.addImage(room.getId(), file);
                uploaded++;
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Upload failed", "Could not upload: " + file.getName() + "\n" + e.getMessage());
            }
        }

        refreshImages();
        if (uploaded > 0) {
            showAlert(Alert.AlertType.INFORMATION, "Upload complete", uploaded + " image(s) uploaded successfully.");
        }
    }

    @FXML
    private void handleSetPrimary() {
        if (room == null || selectedImage == null) return;
        try {
            roomImageService.setPrimaryImage(room.getId(), selectedImage.getId());
            refreshImages();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Primary image", "Could not set primary image.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleMoveLeft() {
        moveSelected(true);
    }

    @FXML
    private void handleMoveRight() {
        moveSelected(false);
    }

    private void moveSelected(boolean left) {
        if (room == null || selectedImage == null) return;
        try {
            roomImageService.moveImage(room.getId(), selectedImage.getId(), left);
            pendingSelectedImageId = selectedImage.getId();
            refreshImages();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Reorder image", "Could not reorder image.\n" + e.getMessage());
        }
    }

    private void reorderByDragAndDrop(int draggedImageId, int targetImageId) {
        if (room == null) return;

        List<Integer> orderedIds = new ArrayList<>();
        for (RoomImage image : images) {
            orderedIds.add(image.getId());
        }

        int fromIndex = orderedIds.indexOf(draggedImageId);
        int targetIndex = orderedIds.indexOf(targetImageId);
        if (fromIndex < 0 || targetIndex < 0) return;

        orderedIds.remove(fromIndex);
        if (fromIndex < targetIndex) {
            targetIndex--;
        }
        orderedIds.add(targetIndex, draggedImageId);

        try {
            roomImageService.reorderImages(room.getId(), orderedIds);
            pendingSelectedImageId = draggedImageId;
            refreshImages();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Reorder image", "Could not reorder image.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteImage() {
        if (room == null || selectedImage == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete image");
        confirm.setHeaderText("Delete selected image?");
        confirm.setContentText(safe(selectedImage.getFileName(), "selected image"));
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    roomImageService.deleteImage(selectedImage.getId(), room.getId());
                    selectedImage = null;
                    selectedIndex = -1;
                    refreshImages();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Delete image", "Could not delete image.\n" + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBackToAccommodation() {
        if (parentController != null && accommodation != null) {
            parentController.showAccommodationDetailsPage(accommodation);
        }
    }

    @FXML
    private void handleClose() {
        if (parentController != null) {
            parentController.closeModal();
        }
    }

    @FXML
    private void handleEditRoom() {
        if (room == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Update room details");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(12));

        TextField nameField = new TextField(safe(room.getRoomName(), ""));
        TextField typeField = new TextField(safe(room.getRoomType(), ""));
        TextField capacityField = new TextField(String.valueOf(room.getCapacity()));
        TextField sizeField = new TextField(String.valueOf(room.getSize()));
        TextField priceField = new TextField(String.valueOf(room.getPricePerNight()));
        CheckBox availableCheck = new CheckBox("Available");
        availableCheck.setSelected(room.isAvailable());
        TextArea amenitiesArea = new TextArea(safe(room.getAmenities(), ""));
        amenitiesArea.setPrefRowCount(4);

        form.add(new Label("Room Name"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("Room Type"), 0, 1);
        form.add(typeField, 1, 1);
        form.add(new Label("Capacity"), 0, 2);
        form.add(capacityField, 1, 2);
        form.add(new Label("Size (m²)"), 0, 3);
        form.add(sizeField, 1, 3);
        form.add(new Label("Price / night"), 0, 4);
        form.add(priceField, 1, 4);
        form.add(availableCheck, 1, 5);
        form.add(new Label("Amenities (comma separated)"), 0, 6);
        form.add(amenitiesArea, 1, 6);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            try {
                room.setRoomName(nameField.getText().trim());
                room.setRoomType(typeField.getText().trim());
                room.setCapacity(Integer.parseInt(capacityField.getText().trim()));
                room.setSize(Double.parseDouble(sizeField.getText().trim()));
                room.setPricePerNight(Double.parseDouble(priceField.getText().trim()));
                room.setAvailable(availableCheck.isSelected());
                room.setAmenities(amenitiesArea.getText() == null ? "" : amenitiesArea.getText().trim());

                roomService.updateRoom(room);
                renderRoomInfo();
                showAlert(Alert.AlertType.INFORMATION, "Room updated", "Room details updated successfully.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Update failed", "Could not update room.\n" + e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteRoom() {
        if (room == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room");
        confirm.setHeaderText("Delete this room?");
        confirm.setContentText(safe(room.getRoomName(), "Selected room"));
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    roomService.deleteRoom(room.getId());
                    if (parentController != null && accommodation != null) {
                        parentController.showAccommodationDetailsPage(accommodation);
                    }
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Delete failed", "Could not delete room.\n" + e.getMessage());
                }
            }
        });
    }

    private Image loadImageByPath(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            String cleanPath = imagePath.trim();

            File absoluteFile = new File(cleanPath);
            if (absoluteFile.exists() && absoluteFile.isFile()) {
                return new Image(new FileInputStream(absoluteFile));
            }

            Path projectRelativePath = Paths.get(PROJECT_ROOT, cleanPath);
            if (Files.exists(projectRelativePath) && Files.isRegularFile(projectRelativePath)) {
                return new Image(new FileInputStream(projectRelativePath.toFile()));
            }

            if (cleanPath.startsWith("/")) {
                String withoutSlash = cleanPath.substring(1);
                Path noSlashPath = Paths.get(PROJECT_ROOT, withoutSlash);
                if (Files.exists(noSlashPath) && Files.isRegularFile(noSlashPath)) {
                    return new Image(new FileInputStream(noSlashPath.toFile()));
                }
            }

            String platformPath = cleanPath.replace("/", File.separator).replace("\\", File.separator);
            Path platformSpecific = Paths.get(PROJECT_ROOT, platformPath);
            if (Files.exists(platformSpecific) && Files.isRegularFile(platformSpecific)) {
                return new Image(new FileInputStream(platformSpecific.toFile()));
            }

            var resource = getClass().getResource(cleanPath.startsWith("/") ? cleanPath : "/" + cleanPath);
            if (resource != null) {
                return new Image(resource.toExternalForm());
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int parseIntSafe(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return -1;
        }
    }
}
