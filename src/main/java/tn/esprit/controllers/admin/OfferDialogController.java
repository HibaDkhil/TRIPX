package tn.esprit.controllers.admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.*;
import tn.esprit.services.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class OfferDialogController {

    @FXML private Label dialogTitle;
    @FXML private TextField txtTitle, txtDiscountValue;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<Offer.DiscountType> cmbDiscountType;
    @FXML private RadioButton rbPack, rbDestination, rbAccommodation;
    @FXML private ComboBox<Object> cmbSelection; // Can hold Pack, Destination, or Accommodation
    @FXML private DatePicker dpStartDate, dpEndDate;
    @FXML private CheckBox chkActive;
    @FXML private Button btnSave, btnCancel;

    private OfferService offerService;
    private PackService packService;
    private LookupService lookupService;

    private Offer offerToEdit;
    private boolean saveClicked = false;

    public void initialize() {
        offerService = new OfferService();
        packService = new PackService();
        lookupService = new LookupService();

        setupComboBoxes();
        setupRadioButtons();
        setupButtons();
    }

    private void setupComboBoxes() {
        // Discount type
        cmbDiscountType.setItems(FXCollections.observableArrayList(Offer.DiscountType.values()));
        cmbDiscountType.setValue(Offer.DiscountType.PERCENTAGE);
    }

    private void setupRadioButtons() {
        rbPack.setSelected(true);
        updateSelectionComboBox();

        rbPack.setOnAction(e -> updateSelectionComboBox());
        rbDestination.setOnAction(e -> updateSelectionComboBox());
        rbAccommodation.setOnAction(e -> updateSelectionComboBox());
    }

    private void updateSelectionComboBox() {
        cmbSelection.getItems().clear();

        try {
            if (rbPack.isSelected()) {
                var packs = packService.getActivePacks();
                cmbSelection.setItems(FXCollections.observableArrayList(packs));
                cmbSelection.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(((Pack) item).getTitle());
                        }
                    }
                });
                cmbSelection.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(((Pack) item).getTitle());
                        }
                    }
                });

            } else if (rbDestination.isSelected()) {
                var destinations = lookupService.getAllDestinations();
                cmbSelection.setItems(FXCollections.observableArrayList(destinations));
                cmbSelection.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(((Destination) item).getName());
                        }
                    }
                });
                cmbSelection.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(((Destination) item).getName());
                        }
                    }
                });

            } else if (rbAccommodation.isSelected()) {
                var accommodations = lookupService.getAllAccommodations();
                cmbSelection.setItems(FXCollections.observableArrayList(accommodations));
                cmbSelection.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(((Accommodation) item).getName());
                        }
                    }
                });
                cmbSelection.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(((Accommodation) item).getName());
                        }
                    }
                });
            }
        } catch (SQLException e) {
            showError("Failed to load selection data: " + e.getMessage());
        }
    }

    private void setupButtons() {
        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> handleCancel());
    }

    public void setOfferToEdit(Offer offer) {
        this.offerToEdit = offer;
        dialogTitle.setText("Edit Offer");

        if (offer != null) {
            txtTitle.setText(offer.getTitle());
            txtDescription.setText(offer.getDescription());
            cmbDiscountType.setValue(offer.getDiscountType());
            txtDiscountValue.setText(offer.getDiscountValue().toString());
            dpStartDate.setValue(offer.getStartDate());
            dpEndDate.setValue(offer.getEndDate());
            chkActive.setSelected(offer.isActive());

            // Set radio button and selection based on what the offer applies to
            if (offer.getPackId() != null) {
                rbPack.setSelected(true);
                updateSelectionComboBox();
                try {
                    Pack pack = packService.getById(offer.getPackId());
                    if (pack != null) {
                        cmbSelection.setValue(pack);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else if (offer.getDestinationId() != null) {
                rbDestination.setSelected(true);
                updateSelectionComboBox();
                try {
                    Destination dest = lookupService.getDestinationById(offer.getDestinationId());
                    if (dest != null) {
                        cmbSelection.setValue(dest);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else if (offer.getAccommodationId() != null) {
                rbAccommodation.setSelected(true);
                updateSelectionComboBox();
                try {
                    Accommodation acc = lookupService.getAccommodationById(offer.getAccommodationId());
                    if (acc != null) {
                        cmbSelection.setValue(acc);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleSave() {
        // Validation
        if (txtTitle.getText().trim().isEmpty()) {
            showError("Please enter a title");
            return;
        }
        if (txtDiscountValue.getText().trim().isEmpty()) {
            showError("Please enter a discount value");
            return;
        }
        if (cmbSelection.getValue() == null) {
            showError("Please select what this offer applies to");
            return;
        }
        if (dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
            showError("Please select start and end dates");
            return;
        }
        if (dpEndDate.getValue().isBefore(dpStartDate.getValue())) {
            showError("End date must be after start date");
            return;
        }

        try {
            BigDecimal discountValue = new BigDecimal(txtDiscountValue.getText().trim());
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Discount value must be greater than 0");
                return;
            }

            Integer packId = null, destinationId = null, accommodationId = null;

            Object selected = cmbSelection.getValue();
            if (rbPack.isSelected()) {
                packId = ((Pack) selected).getIdPack();
            } else if (rbDestination.isSelected()) {
                destinationId = Math.toIntExact(((Destination) selected).getDestinationId());
            } else if (rbAccommodation.isSelected()) {
                accommodationId = ((Accommodation) selected).getId();
            }

            if (offerToEdit == null) {
                // Add new offer
                Offer newOffer = new Offer(
                    txtTitle.getText().trim(),
                    txtDescription.getText().trim(),
                    cmbDiscountType.getValue(),
                    discountValue,
                    packId,
                    destinationId != null ? destinationId.longValue() : null,
                    accommodationId,
                    dpStartDate.getValue(),
                    dpEndDate.getValue()
                );
                newOffer.setActive(chkActive.isSelected());
                offerService.add(newOffer);
            } else {
                // Update existing offer
                offerToEdit.setTitle(txtTitle.getText().trim());
                offerToEdit.setDescription(txtDescription.getText().trim());
                offerToEdit.setDiscountType(cmbDiscountType.getValue());
                offerToEdit.setDiscountValue(discountValue);
                offerToEdit.setPackId(packId);
                offerToEdit.setDestinationId(destinationId != null ? destinationId.longValue() : null);
                offerToEdit.setAccommodationId(accommodationId);
                offerToEdit.setStartDate(dpStartDate.getValue());
                offerToEdit.setEndDate(dpEndDate.getValue());
                offerToEdit.setActive(chkActive.isSelected());
                offerService.modifier(offerToEdit);
            }

            saveClicked = true;
            closeDialog();

        } catch (NumberFormatException e) {
            showError("Invalid discount value format");
        } catch (SQLException e) {
            showError("Failed to save offer: " + e.getMessage());
        }
    }

    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
