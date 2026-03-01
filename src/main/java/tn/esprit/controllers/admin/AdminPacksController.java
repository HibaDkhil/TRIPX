package tn.esprit.controllers.admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminPacksController {

    @FXML private FlowPane packsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter, statusFilter;
    @FXML private Button btnAddPack, btnClearFilters;
    @FXML private Label lblTotalPacks, lblActivePacks, lblAvgPrice;

    private PackService packService;
    private LookupService lookupService;
    private PackCategoryService categoryService;
    
    private List<Pack> allPacks;
    private User currentUser;
    private String userRole;

    public void initialize() {
        packService = new PackService();
        lookupService = new LookupService();
        categoryService = new PackCategoryService();
        
        setupFilters();
        loadPacks();
        updateStats();
        
        btnAddPack.setOnAction(e -> handleAddPack());
        btnClearFilters.setOnAction(e -> clearFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> filterPacks());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    private void setupFilters() {
        try {
            var categories = FXCollections.observableArrayList("All");
            categoryService.afficherList().forEach(cat -> categories.add(cat.getName()));
            categoryFilter.setItems(categories);
            categoryFilter.setValue("All");
            categoryFilter.setOnAction(e -> filterPacks());
            
            statusFilter.setItems(FXCollections.observableArrayList("All", "ACTIVE", "INACTIVE"));
            statusFilter.setValue("All");
            statusFilter.setOnAction(e -> filterPacks());
            
        } catch (SQLException e) {
            showError("Failed to load filters: " + e.getMessage());
        }
    }

    private void loadPacks() {
        try {
            allPacks = packService.afficherList();
            displayPacks(allPacks);
        } catch (SQLException e) {
            showError("Failed to load packs: " + e.getMessage());
        }
    }

    private void displayPacks(List<Pack> packs) {
        packsGrid.getChildren().clear();
        
        for (Pack pack : packs) {
            packsGrid.getChildren().add(createPackCard(pack));
        }
    }

    private VBox createPackCard(Pack pack) {
        VBox card = new VBox(16);
        card.setPrefWidth(340);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 24; " +
                     "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2); -fx-cursor: hand;");
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-translate-y: -4; -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 20, 0, 0, 8);");
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-translate-y: -4;", ""));
        });
        
        try {
            // Header with status badge
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);
            
            Label title = new Label(pack.getTitle());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1A202C;");
            title.setWrapText(true);
            HBox.setHgrow(title, Priority.ALWAYS);
            
            Label statusBadge = new Label(pack.getStatus().name());
            statusBadge.setStyle(pack.getStatus() == Pack.Status.ACTIVE ?
                "-fx-background-color: #C6F6D5; -fx-text-fill: #22543D; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 700;" :
                "-fx-background-color: #FED7D7; -fx-text-fill: #742A2A; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 700;");
            
            header.getChildren().addAll(title, statusBadge);
            
            // Info row
            Destination dest = lookupService.getDestinationById(pack.getDestinationId());
            PackCategory cat = categoryService.getById(pack.getCategoryId());
            
            HBox info = new HBox(16);
            info.setAlignment(Pos.CENTER_LEFT);
            
            Label destLabel = new Label("📍 " + (dest != null ? dest.getName() : "N/A"));
            destLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");
            
            Label catLabel = new Label("🏷 " + (cat != null ? cat.getName() : "N/A"));
            catLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");
            
            Label durLabel = new Label("📅 " + pack.getDurationDays() + " days");
            durLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096;");
            
            info.getChildren().addAll(destLabel, catLabel, durLabel);
            
            // Description
            Label desc = new Label(pack.getDescription() != null && !pack.getDescription().isEmpty() ?
                (pack.getDescription().length() > 120 ? pack.getDescription().substring(0, 120) + "..." : pack.getDescription()) :
                "No description available");
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #A0AEC0; -fx-line-spacing: 1.5em;");
            
            // Price
            HBox priceBox = new HBox(8);
            priceBox.setAlignment(Pos.CENTER_LEFT);
            priceBox.setStyle("-fx-background-color: #F7FAFC; -fx-background-radius: 10; -fx-padding: 12;");
            
            Label priceLabel = new Label("💰");
            priceLabel.setStyle("-fx-font-size: 20px;");
            
            Label price = new Label(String.format("%.2f TND", pack.getBasePrice()));
            price.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #667eea;");
            
            priceBox.getChildren().addAll(priceLabel, price);
            
            // Actions
            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER);
            
            Button editBtn = new Button("✏ Edit");
            editBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: 600; " +
                           "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
            editBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(editBtn, Priority.ALWAYS);
            editBtn.setOnAction(e -> handleEditPack(pack));
            
            Button deleteBtn = new Button("🗑 Delete");
            deleteBtn.setStyle("-fx-background-color: #FED7D7; -fx-text-fill: #C53030; -fx-font-weight: 600; " +
                             "-fx-background-radius: 8; -fx-padding: 10 16; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> handleDeletePack(pack));
            
            actions.getChildren().addAll(editBtn, deleteBtn);
            
            card.getChildren().addAll(header, info, desc, priceBox, actions);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return card;
    }

    private void updateStats() {
        if (allPacks == null || allPacks.isEmpty()) {
            lblTotalPacks.setText("0");
            lblActivePacks.setText("0");
            lblAvgPrice.setText("0 TND");
            return;
        }
        
        lblTotalPacks.setText(String.valueOf(allPacks.size()));
        
        long activeCount = allPacks.stream().filter(p -> p.getStatus() == Pack.Status.ACTIVE).count();
        lblActivePacks.setText(String.valueOf(activeCount));
        
        double avgPrice = allPacks.stream()
            .mapToDouble(p -> p.getBasePrice().doubleValue())
            .average()
            .orElse(0);
        lblAvgPrice.setText(String.format("%.0f TND", avgPrice));
    }

    private void filterPacks() {
        if (allPacks == null) return;

        List<Pack> filtered = allPacks.stream().filter(pack -> {
            String searchText = searchField.getText().toLowerCase().trim();
            if (!searchText.isEmpty()) {
                boolean matchesTitle = pack.getTitle().toLowerCase().contains(searchText);
                boolean matchesDesc = pack.getDescription() != null && 
                                     pack.getDescription().toLowerCase().contains(searchText);
                if (!matchesTitle && !matchesDesc) return false;
            }

            if (!categoryFilter.getValue().equals("All")) {
                try {
                    PackCategory cat = categoryService.getById(pack.getCategoryId());
                    if (cat == null || !cat.getName().equals(categoryFilter.getValue())) return false;
                } catch (SQLException e) {
                    return false;
                }
            }

            if (!statusFilter.getValue().equals("All")) {
                if (!pack.getStatus().name().equals(statusFilter.getValue())) return false;
            }

            return true;
        }).toList();

        displayPacks(filtered);
    }

    private void clearFilters() {
        searchField.clear();
        categoryFilter.setValue("All");
        statusFilter.setValue("All");
        loadPacks();
    }

    private void handleAddPack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/admin/dialogs/PackDialog.fxml")
            );
            javafx.scene.layout.VBox dialogContent = loader.load();
            PackDialogController controller = loader.getController();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Add New Pack");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(dialogContent));
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadPacks();
                updateStats();
                showInfo("Pack added successfully!");
            }
        } catch (Exception e) {
            showError("Failed to open dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEditPack(Pack pack) {
        if (pack == null) return;

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/admin/dialogs/PackDialog.fxml")
            );
            javafx.scene.layout.VBox dialogContent = loader.load();
            PackDialogController controller = loader.getController();
            controller.setPackToEdit(pack);

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Edit Pack");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(dialogContent));
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadPacks();
                updateStats();
                showInfo("Pack updated successfully!");
            }
        } catch (Exception e) {
            showError("Failed to open dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeletePack(Pack pack) {
        if (pack == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Pack");
        confirm.setHeaderText("Delete " + pack.getTitle() + "?");
        confirm.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                packService.delete(pack);
                loadPacks();
                updateStats();
                showInfo("Pack deleted successfully!");
            } catch (SQLException e) {
                showError("Failed to delete pack: " + e.getMessage());
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}
