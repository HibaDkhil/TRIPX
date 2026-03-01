package tn.esprit.controllers.user;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Offer;
import tn.esprit.services.OfferService;
import tn.esprit.services.PackService;
import tn.esprit.services.LookupService;

import java.sql.SQLException;
import java.util.List;

public class UserOffersController {

    @FXML private FlowPane offersGrid;
    @FXML private Label lblOfferCount;

    private OfferService offerService;
    private PackService packService;
    private LookupService lookupService;

    public void initialize() {
        offerService = new OfferService();
        packService = new PackService();
        lookupService = new LookupService();

        loadActiveOffers();
    }

    private void loadActiveOffers() {
        try {
            List<Offer> offers = offerService.getActiveOffers();
            lblOfferCount.setText(offers.size() + " active offer" + (offers.size() != 1 ? "s" : ""));

            offersGrid.getChildren().clear();
            for (Offer offer : offers) {
                offersGrid.getChildren().add(createOfferCard(offer));
            }

        } catch (SQLException e) {
            showError("Failed to load offers: " + e.getMessage());
        }
    }

    private VBox createOfferCard(Offer offer) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #FFD700, #FFA500); " +
                     "-fx-background-radius: 16; -fx-padding: 24; -fx-cursor: hand; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4);");

        try {
            // Title
            Label title = new Label("🎉 " + offer.getTitle());
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
            title.setWrapText(true);

            // Description
            Label desc = new Label(offer.getDescription() != null ? offer.getDescription() : "");
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.9);");

            // Discount value
            String discountText = offer.getDiscountType() == Offer.DiscountType.PERCENTAGE ?
                    offer.getDiscountValue() + "% OFF" :
                    offer.getDiscountValue() + " TND OFF";

            Label discount = new Label(discountText);
            discount.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);");

            // Applies to
            String appliesTo = "";
            if (offer.getPackId() != null) {
                var pack = packService.getById(offer.getPackId());
                appliesTo = "On: " + (pack != null ? pack.getTitle() : "Pack #" + offer.getPackId());
            } else if (offer.getDestinationId() != null) {
                var dest = lookupService.getDestinationById(offer.getDestinationId());
                appliesTo = "On all " + (dest != null ? dest.getName() : "Destination") + " packs";
            } else if (offer.getAccommodationId() != null) {
                var acc = lookupService.getAccommodationById(offer.getAccommodationId());
                appliesTo = "On " + (acc != null ? acc.getName() : "Accommodation") + " stays";
            }

            Label appliesLabel = new Label(appliesTo);
            appliesLabel.setWrapText(true);
            appliesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.85);");

            // Valid until
            Label validity = new Label("Valid until: " + offer.getEndDate().toString());
            validity.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.7);");

            card.getChildren().addAll(title, desc, discount, appliesLabel, validity);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return card;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}
