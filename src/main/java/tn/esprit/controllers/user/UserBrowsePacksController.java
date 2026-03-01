package tn.esprit.controllers.user;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.*;
import tn.esprit.services.*;
import tn.esprit.utils.SessionManager;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class UserBrowsePacksController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter, destinationFilter;
    @FXML private CheckBox chkHasOffer;
    @FXML private Slider priceSlider;
    @FXML private Label lblMaxPrice, lblPackCount;
    @FXML private Button btnClearFilters;
    @FXML private FlowPane packsGrid;

    private PackService packService;
    private LookupService lookupService;
    private PackCategoryService categoryService;
    private OfferService offerService;
    
    private List<Pack> allPacks;
    
    private int getCurrentUserId() {
        return SessionManager.getCurrentUserId();
    }

    public void initialize() {
        packService = new PackService();
        lookupService = new LookupService();
        categoryService = new PackCategoryService();
        offerService = new OfferService();
        
        setupFilters();
        loadPacks();
        
        btnClearFilters.setOnAction(e -> clearFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> filterPacks());
        chkHasOffer.selectedProperty().addListener((obs, old, newVal) -> filterPacks());
        priceSlider.valueProperty().addListener((obs, old, newVal) -> {
            lblMaxPrice.setText(String.format("%.0f TND", newVal.doubleValue()));
            filterPacks();
        });
    }

    private void setupFilters() {
        try {
            // Category filter
            var categories = FXCollections.observableArrayList("All");
            categoryService.afficherList().forEach(cat -> categories.add(cat.getName()));
            categoryFilter.setItems(categories);
            categoryFilter.setValue("All");
            categoryFilter.setOnAction(e -> filterPacks());
            
            // Destination filter
            var destinations = FXCollections.observableArrayList("All");
            lookupService.getAllDestinations().forEach(dest -> destinations.add(dest.getName()));
            destinationFilter.setItems(destinations);
            destinationFilter.setValue("All");
            destinationFilter.setOnAction(e -> filterPacks());
            
        } catch (SQLException e) {
            showError("Failed to load filters: " + e.getMessage());
        }
    }

    private void loadPacks() {
        try {
            allPacks = packService.getActivePacks();
            displayPacks(allPacks);
        } catch (SQLException e) {
            showError("Failed to load packs: " + e.getMessage());
        }
    }

    private void filterPacks() {
        if (allPacks == null) return;
        
        List<Pack> filtered = allPacks.stream()
            .filter(p -> {
                // Search
                if (!searchField.getText().isEmpty() && 
                    !p.getTitle().toLowerCase().contains(searchField.getText().toLowerCase())) {
                    return false;
                }
                
                // Category
                if (!categoryFilter.getValue().equals("All")) {
                    try {
                        PackCategory cat = categoryService.getById(p.getCategoryId());
                        if (cat == null || !cat.getName().equals(categoryFilter.getValue())) {
                            return false;
                        }
                    } catch (SQLException e) {
                        return false;
                    }
                }
                
                // Destination
                if (!destinationFilter.getValue().equals("All")) {
                    try {
                        Destination dest = lookupService.getDestinationById(p.getDestinationId());
                        if (dest == null || !dest.getName().equals(destinationFilter.getValue())) {
                            return false;
                        }
                    } catch (SQLException e) {
                        return false;
                    }
                }
                
                // Price
                if (p.getBasePrice().doubleValue() > priceSlider.getValue()) {
                    return false;
                }
                
                // Has Offer filter
                if (chkHasOffer.isSelected()) {
                    try {
                        Offer activeOffer = offerService.getActiveOfferByPackId(p.getIdPack());
                        if (activeOffer == null) {
                            return false;
                        }
                    } catch (SQLException e) {
                        return false;
                    }
                }
                
                return true;
            })
            .toList();
        
        displayPacks(filtered);
    }

    private void displayPacks(List<Pack> packs) {
        packsGrid.getChildren().clear();
        lblPackCount.setText(packs.size() + " pack" + (packs.size() != 1 ? "s" : "") + " found");
        
        for (Pack pack : packs) {
            packsGrid.getChildren().add(createPackCard(pack));
        }
    }

    private VBox createPackCard(Pack pack) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        
        // Check if pack has active offer
        Offer activeOffer = null;
        double offerDiscount = 0;
        try {
            activeOffer = offerService.getActiveOfferByPackId(pack.getIdPack());
            if (activeOffer != null && activeOffer.getDiscountType() == Offer.DiscountType.PERCENTAGE) {
                offerDiscount = activeOffer.getDiscountValue().doubleValue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Special styling if has offer
        String cardStyle = "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; " +
                          "-fx-border-radius: 16; -fx-cursor: hand; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);";
        
        if (activeOffer != null) {
            // Gold border for packs with offers
            cardStyle = "-fx-background-color: linear-gradient(to bottom, #FFF9E6, white); " +
                       "-fx-background-radius: 16; -fx-padding: 20; " +
                       "-fx-border-color: #FFD700; -fx-border-width: 2; -fx-border-radius: 16; " +
                       "-fx-cursor: hand; " +
                       "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.4), 15, 0, 0, 4);";
        }
        
        card.setStyle(cardStyle);
        
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-scale-y: 1.02; -fx-scale-x: 1.02;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle()));
        
        try {
            // Offer badge (if has offer)
            VBox topSection = new VBox(8);
            
            if (activeOffer != null) {
                HBox offerBadge = new HBox(8);
                offerBadge.setAlignment(javafx.geometry.Pos.CENTER);
                offerBadge.setStyle("-fx-background-color: linear-gradient(to right, #FFD700, #FFA500); " +
                                   "-fx-background-radius: 20; -fx-padding: 6 16;");
                
                Label offerIcon = new Label("🎉");
                offerIcon.setStyle("-fx-font-size: 14px;");
                
                Label offerText = new Label("SPECIAL OFFER: " + offerDiscount + "% OFF");
                offerText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;");
                
                offerBadge.getChildren().addAll(offerIcon, offerText);
                topSection.getChildren().add(offerBadge);
            }
            
            // Title
            Label title = new Label(pack.getTitle());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #063154;");
            title.setWrapText(true);
            
            // Destination & Duration
            HBox info = new HBox(12);
            Destination dest = lookupService.getDestinationById(pack.getDestinationId());
            Label destLabel = new Label("📍 " + (dest != null ? dest.getName() : "N/A"));
            destLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
            Label durLabel = new Label("📅 " + pack.getDurationDays() + " days");
            durLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
            info.getChildren().addAll(destLabel, durLabel);
            
            // Description (truncated)
            Label desc = new Label(pack.getDescription() != null ? 
                (pack.getDescription().length() > 100 ? 
                    pack.getDescription().substring(0, 100) + "..." : 
                    pack.getDescription()) : "");
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
            
            // Pricing
            VBox pricing = new VBox(4);
            pricing.setStyle("-fx-background-color: #F5F7FA; -fx-background-radius: 8; -fx-padding: 12;");
            
            Label basePrice = new Label("Base: " + pack.getBasePrice() + " TND");
            basePrice.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-strikethrough: true;");
            
            // Final price with loyalty (assuming 4% for demo)
            double finalPrice = pack.getBasePrice().doubleValue() * (1 - (offerDiscount + 4) / 100.0);
            
            Label finalPriceLabel = new Label(String.format("%.2f TND", finalPrice));
            finalPriceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0A4174;");
            
            if (offerDiscount > 0) {
                Label offerBadge = new Label("🎉 " + offerDiscount + "% OFF");
                offerBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #2F9D94; -fx-font-weight: 600;");
                pricing.getChildren().addAll(basePrice, offerBadge, finalPriceLabel);
            } else {
                pricing.getChildren().addAll(basePrice, finalPriceLabel);
            }
            
            // Book button
            Button btnBook = new Button("View Details & Book");
            btnBook.setMaxWidth(Double.MAX_VALUE);
            btnBook.setStyle("-fx-background-color: linear-gradient(to right, #0A4174, #2F9D94); " +
                            "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; " +
                            "-fx-padding: 12; -fx-cursor: hand;");
            btnBook.setOnAction(e -> openPackDetails(pack));
            
            if (activeOffer != null) {
                card.getChildren().addAll(topSection, title, info, desc, pricing, btnBook);
            } else {
                card.getChildren().addAll(title, info, desc, pricing, btnBook);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return card;
    }

    private void openPackDetails(Pack pack) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/user/UserPackDetails.fxml")
            );
            javafx.scene.Parent page = loader.load();
            
            // Pass pack ID to details controller
            UserPackDetailsController controller = loader.getController();
            controller.loadPackDetails(pack.getIdPack());
            
            // Find the content area in UserDashboard and replace content
            javafx.scene.Node current = packsGrid.getScene().getRoot();
            if (current instanceof javafx.scene.layout.BorderPane) {
                javafx.scene.layout.BorderPane dashboard = (javafx.scene.layout.BorderPane) current;
                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) dashboard.getCenter();
                contentArea.getChildren().clear();
                contentArea.getChildren().add(page);
            }
            
        } catch (Exception e) {
            showError("Failed to open pack details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearFilters() {
        searchField.clear();
        categoryFilter.setValue("All");
        destinationFilter.setValue("All");
        chkHasOffer.setSelected(false);
        priceSlider.setValue(2000);
        loadPacks();
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
