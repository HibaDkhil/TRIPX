package tn.esprit.controllers.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Offer;
import tn.esprit.services.OfferService;
import tn.esprit.services.PackService;
import tn.esprit.services.LookupService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminOffersController {

    @FXML private TableView<Offer> offersTable;
    @FXML private TableColumn<Offer, String> colId, colTitle, colType, colValue, colAppliesTo, colStartDate, colEndDate, colActive, colActions;
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter, activeFilter;
    @FXML private Button btnAddOffer, btnClearFilters;

    private OfferService offerService;
    private PackService packService;
    private LookupService lookupService;
    
    private ObservableList<Offer> offersList;
    private List<Offer> allOffers;
    private tn.esprit.entities.User currentUser;
    private String userRole;

    public void initialize() {
        offerService = new OfferService();
        packService = new PackService();
        lookupService = new LookupService();
        
        offersList = FXCollections.observableArrayList();
        
        setupTable();
        setupFilters();
        loadOffers();
        
        btnAddOffer.setOnAction(e -> handleAddOffer());
        btnClearFilters.setOnAction(e -> clearFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> filterOffers());
    }

    public void setCurrentUser(tn.esprit.entities.User user) {
        this.currentUser = user;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    private void setupTable() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getIdOffer())));
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDiscountType().name()));
        
        colValue.setCellValueFactory(data -> {
            Offer offer = data.getValue();
            String value = offer.getDiscountValue().toString();
            if (offer.getDiscountType() == Offer.DiscountType.PERCENTAGE) {
                value += "%";
            } else {
                value += " TND";
            }
            return new SimpleStringProperty(value);
        });
        
        colAppliesTo.setCellValueFactory(data -> {
            Offer offer = data.getValue();
            String appliesTo = "";
            
            try {
                if (offer.getPackId() != null) {
                    var pack = packService.getById(offer.getPackId());
                    appliesTo = "Pack: " + (pack != null ? pack.getTitle() : "N/A");
                } else if (offer.getDestinationId() != null) {
                    var dest = lookupService.getDestinationById(offer.getDestinationId());
                    appliesTo = "Destination: " + (dest != null ? dest.getName() : "N/A");
                } else if (offer.getAccommodationId() != null) {
                    var acc = lookupService.getAccommodationById(offer.getAccommodationId());
                    appliesTo = "Accommodation: " + (acc != null ? acc.getName() : "N/A");
                }
            } catch (SQLException e) {
                appliesTo = "Error";
            }
            
            return new SimpleStringProperty(appliesTo);
        });
        
        colStartDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStartDate().toString()));
        colEndDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEndDate().toString()));
        colActive.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isActive() ? "✅ Yes" : "❌ No"));
        
        // Actions column
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏ Edit");
            private final Button deleteBtn = new Button("🗑 Delete");
            private final HBox box = new HBox(8, editBtn, deleteBtn);
            
            {
                box.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #4E8EA2; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #EF5350; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
                
                editBtn.setOnAction(e -> handleEditOffer(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDeleteOffer(getTableRow().getItem()));
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        
        offersTable.setItems(offersList);
    }

    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList("All", "PERCENTAGE", "FIXED"));
        typeFilter.setValue("All");
        typeFilter.setOnAction(e -> filterOffers());
        
        activeFilter.setItems(FXCollections.observableArrayList("All", "Active", "Inactive"));
        activeFilter.setValue("All");
        activeFilter.setOnAction(e -> filterOffers());
    }

    private void loadOffers() {
        try {
            allOffers = offerService.afficherList();
            offersList.clear();
            offersList.addAll(allOffers);
        } catch (SQLException e) {
            showError("Failed to load offers: " + e.getMessage());
        }
    }

    private void filterOffers() {
        if (allOffers == null) return;

        List<Offer> filtered = allOffers.stream().filter(offer -> {
            // Search filter
            String searchText = searchField.getText().toLowerCase().trim();
            if (!searchText.isEmpty()) {
                boolean matchesTitle = offer.getTitle().toLowerCase().contains(searchText);
                boolean matchesDesc = offer.getDescription() != null && 
                                     offer.getDescription().toLowerCase().contains(searchText);
                if (!matchesTitle && !matchesDesc) {
                    return false;
                }
            }

            // Type filter
            if (!typeFilter.getValue().equals("All")) {
                if (!offer.getDiscountType().name().equals(typeFilter.getValue())) {
                    return false;
                }
            }

            // Active filter
            if (!activeFilter.getValue().equals("All")) {
                boolean shouldBeActive = activeFilter.getValue().equals("Active");
                if (offer.isActive() != shouldBeActive) {
                    return false;
                }
            }

            return true;
        }).toList();

        offersList.clear();
        offersList.addAll(filtered);
    }

    private void clearFilters() {
        searchField.clear();
        typeFilter.setValue("All");
        activeFilter.setValue("All");
        loadOffers();
    }

    private void handleAddOffer() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/admin/dialogs/OfferDialog.fxml")
            );
            javafx.scene.layout.VBox dialogContent = loader.load();
            OfferDialogController controller = loader.getController();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Add New Offer");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(dialogContent));
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadOffers();
                showInfo("Offer added successfully!");
            }
        } catch (Exception e) {
            showError("Failed to open dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleEditOffer(Offer offer) {
        if (offer == null) return;

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/admin/dialogs/OfferDialog.fxml")
            );
            javafx.scene.layout.VBox dialogContent = loader.load();
            OfferDialogController controller = loader.getController();
            controller.setOfferToEdit(offer);

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Edit Offer");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.setScene(new javafx.scene.Scene(dialogContent));
            dialogStage.showAndWait();

            if (controller.isSaveClicked()) {
                loadOffers();
                showInfo("Offer updated successfully!");
            }
        } catch (Exception e) {
            showError("Failed to open dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteOffer(Offer offer) {
        if (offer == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Offer");
        confirm.setHeaderText("Delete " + offer.getTitle() + "?");
        confirm.setContentText("This action cannot be undone.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                offerService.delete(offer);
                loadOffers();
                showInfo("Offer deleted successfully!");
            } catch (SQLException e) {
                showError("Failed to delete offer: " + e.getMessage());
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
