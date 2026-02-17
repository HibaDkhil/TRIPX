package tn.esprit.controllers.user;

import tn.esprit.entities.User;
import tn.esprit.entities.UserPreferences;
import tn.esprit.services.UserPreferencesService;
import tn.esprit.services.UserService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OnboardingController {

    @FXML private StackPane mainContainer;

    @FXML private VBox step1, step2, step3, step4, step5, step6, step7, step8, step9, step10, step11, step12;

    // Step 1: Gender
    @FXML private ToggleButton maleBtn;
    @FXML private ToggleButton femaleBtn;
    @FXML private ToggleGroup genderGroup;

    // Step 2: Age
    @FXML private ComboBox<String> birthDateRangeCombo;

    // Step 3: Priorities
    @FXML private FlowPane prioritiesContainer;
    
    // Step 4: Climate
    @FXML private FlowPane climateContainer;

    // Step 5: Budget
    @FXML private Slider minBudgetSlider;
    @FXML private Slider maxBudgetSlider;
    @FXML private Label budgetLabel;

    // Step 6: Location
    @FXML private FlowPane locationContainer;

    // Step 7: Accommodation
    @FXML private FlowPane accommodationContainer;

    // Step 8: Style
    @FXML private FlowPane styleContainer;

    // Step 9: Dietary
    @FXML private FlowPane dietaryContainer;

    // Step 10: Travel Pace
    @FXML private FlowPane travelPaceContainer;

    // Step 11: Group Type
    @FXML private FlowPane groupTypeContainer;

    // Step 12: Accessibility
    @FXML private ToggleButton accessibilityBtn;

    // Navigation
    @FXML private Button nextBtn;
    @FXML private Button backBtn;

    private int currentStep = 1;
    private final int MAX_STEPS = 12;

    // Data Storage
    private List<String> selectedPriorities = new ArrayList<>();
    private List<String> selectedClimate = new ArrayList<>();
    private List<String> selectedLocations = new ArrayList<>();
    private List<String> selectedAccommodation = new ArrayList<>();
    private List<String> selectedStyles = new ArrayList<>();
    private List<String> selectedDietary = new ArrayList<>();
    private List<String> selectedPace = new ArrayList<>(); // Only 1 allowed, but using list for common logic
    private List<String> selectedGroup = new ArrayList<>(); // Only 1 allowed

    private UserService userService;
    private UserPreferencesService userPreferencesService;
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void initialize() {
        userService = new UserService();
        userPreferencesService = new UserPreferencesService();

        // 1. Priorities (Exact DB values)
        // DB: Activities, Wellness, Food and Drinks, Family Friendly, Parking, Amenities, Pet-Friendly, Business Facilities, I dont care
        String[] priorities = {"Activities", "Wellness", "Food and Drinks", "Family Friendly", "Parking", "Amenities", "Pet-Friendly", "Business Facilities", "I dont care"};
        populateFlowPane(prioritiesContainer, priorities, selectedPriorities, 4, false);

        // 2. Climate (Exact DB values)
        // DB: Temperate, Tropical, Desert, Cold/Arctic, Oceanic, Mountain
        String[] climates = {"Temperate", "Tropical", "Desert", "Cold/Arctic", "Oceanic", "Mountain"};
        populateFlowPane(climateContainer, climates, selectedClimate, 4, false);

        // 3. Location (Exact DB values)
        // DB: City Center, Beachfront, Mountain View, Countryside
        String[] locations = {"City Center", "Beachfront", "Mountain View", "Countryside"};
        populateFlowPane(locationContainer, locations, selectedLocations, 2, false);

        // 4. Accommodation (Exact DB values)
        // DB: Hotel, Resort, Villa, Hostel, Vacation Rental, Camping, Cabin, Boat/Yacht
        String[] accommodations = {"Hotel", "Resort", "Villa", "Hostel", "Vacation Rental", "Camping", "Cabin", "Boat/Yacht"};
        populateFlowPane(accommodationContainer, accommodations, selectedAccommodation, 99, false);

        // 5. Style (Exact DB values)
        // DB: Minimalist, Urban, Luxury, Bohemian, Rustic, Traditional, Tropical, Mediterranean, Vintage, Classic
        String[] styles = {"Minimalist", "Urban", "Luxury", "Bohemian", "Rustic", "Traditional", "Tropical", "Mediterranean", "Vintage", "Classic"};
        populateFlowPane(styleContainer, styles, selectedStyles, 4, false);

        // 6. Dietary (Exact DB values)
        // DB: None, Vegetarian, Vegan, Gluten-Free, Dairy-Free, Halal, Nut Allergies, Seafood Allergies, Raw Food, Sugar-Free, Low-Sodium
        String[] diets = {"None", "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free", "Halal", "Nut Allergies", "Seafood Allergies", "Raw Food", "Sugar-Free", "Low-Sodium"};
        populateFlowPane(dietaryContainer, diets, selectedDietary, 99, false);
        
        // 7. Travel Pace (Exact DB values)
        // DB: Relaxed, Moderate, Fast-paced
        String[] paces = {"Relaxed", "Moderate", "Fast-paced"};
        populateFlowPane(travelPaceContainer, paces, selectedPace, 1, false); // false for WHITE theme

        // 8. Group Type (Exact DB values)
        // DB: Solo, Couple, Family, Friends, Business
        String[] groups = {"Solo", "Couple", "Family", "Friends", "Business"};
        populateFlowPane(groupTypeContainer, groups, selectedGroup, 1, false);

        // Age Ranges (Removed 1950)
        birthDateRangeCombo.getItems().addAll(
            "1960-1969", "1970-1979", "1980-1989", "1990-1999", "2000-2010", "2010-Present"
        );

        // Budget Slider Logic
        updateBudgetLabel();
        minBudgetSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateBudgetLabel());
        maxBudgetSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateBudgetLabel());

        // Initial View
        showStep(1);
    }

    private void updateBudgetLabel() {
        budgetLabel.setText(String.format("Min: $%.0f - Max: $%.0f", minBudgetSlider.getValue(), maxBudgetSlider.getValue()));
    }

    private void populateFlowPane(FlowPane container, String[] items, List<String> targetList, int maxSelection, boolean isBlueTheme) {
        // ToggleGroup for single selection logic (Pace/Group)
        ToggleGroup group = (maxSelection == 1) ? new ToggleGroup() : null;

        for (String item : items) {
            ToggleButton btn = new ToggleButton(item);
            
            // Apply correct style class
            if (isBlueTheme) {
                btn.getStyleClass().add("option-card-blue");
            } else {
                btn.getStyleClass().add("option-card");
            }

            if (maxSelection == 1) {
                btn.setToggleGroup(group);
            }

            btn.setOnAction(e -> {
                if (maxSelection == 1) {
                    targetList.clear();
                    if (btn.isSelected()) {
                        targetList.add(item);
                    }
                } else {
                    if (btn.isSelected()) {
                        if (maxSelection != 99 && targetList.size() >= maxSelection) {
                            btn.setSelected(false);
                            return; // Limit reached
                        }
                        targetList.add(item);
                    } else {
                        targetList.remove(item);
                    }
                }
            });
            container.getChildren().add(btn);
        }
    }

    @FXML
    private void handleNext() {
        if (currentStep < 11) { // MAX_STEPS is now effectively 11 since we remove Accessibility
            currentStep++;
            showStep(currentStep);
        } else {
            saveAndFinish();
        }
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) {
            currentStep--;
            showStep(currentStep);
        }
    }

    private void showStep(int step) {
        // Hide all steps first
        step1.setVisible(false);
        step2.setVisible(false);
        step3.setVisible(false);
        step4.setVisible(false);
        step5.setVisible(false);
        step6.setVisible(false);
        step7.setVisible(false);
        step8.setVisible(false);
        step9.setVisible(false);
        step10.setVisible(false);
        step11.setVisible(false);
        // Step 12 removed

        // Show current step
        VBox currentNode = null;
        switch (step) {
            case 1: currentNode = step1; break;
            case 2: currentNode = step2; break;
            case 3: currentNode = step3; break;
            case 4: currentNode = step4; break;
            case 5: currentNode = step5; break;
            case 6: currentNode = step6; break;
            case 7: currentNode = step7; break;
            case 8: currentNode = step8; break;
            case 9: currentNode = step9; break;
            case 10: currentNode = step10; break;
            case 11: currentNode = step11; break;
            // Case 12 removed
        }

        if (currentNode != null) {
            currentNode.setVisible(true);
            applyFadeInfo(currentNode);
        }

        // Button Visibility
        backBtn.setVisible(step > 1);
        nextBtn.setText(step == 11 ? "Finish" : "Next");
    }

    private void applyFadeInfo(VBox node) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void saveAndFinish() {
        if (currentUser == null) {
            // Defensive Check: If currentUser is null, we can't save.
            // This might happen if testing directly without login, or if signup failed silently.
            System.err.println("CRITICAL ERROR: currentUser is NULL in saveAndFinish!");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("User Data Missing");
            alert.setContentText("Cannot save preferences because user data is missing. Please restart the application.");
            alert.showAndWait();
            return;
        }

        // 1. Update User (Gender & Birth Date)
        if (maleBtn.isSelected()) currentUser.setGender("Male");
        if (femaleBtn.isSelected()) currentUser.setGender("Female");
        currentUser.setBirthYear(birthDateRangeCombo.getValue());
        
        System.out.println("Updating user demographics for: " + currentUser.getEmail());
        userService.updateUserDemographics(currentUser);

        // 2. Save Preferences
        UserPreferences prefs = new UserPreferences();
        prefs.setUserId(currentUser.getUserId());

        String prioritiesStr = String.join(",", selectedPriorities);
        System.out.println("DEBUG: Saving Priorities: [" + prioritiesStr + "]");
        prefs.setPriorities(prioritiesStr);
        prefs.setPreferredClimate(String.join(",", selectedClimate));
        prefs.setBudgetMinPerNight(java.math.BigDecimal.valueOf(minBudgetSlider.getValue()));
        prefs.setBudgetMaxPerNight(java.math.BigDecimal.valueOf(maxBudgetSlider.getValue()));
        prefs.setLocationPreferences(String.join(",", selectedLocations));
        prefs.setAccommodationTypes(String.join(",", selectedAccommodation));
        prefs.setStylePreferences(String.join(",", selectedStyles));
        prefs.setDietaryRestrictions(String.join(",", selectedDietary));
        
        // New logic for Pace/Group
        if (!selectedPace.isEmpty()) prefs.setTravelPace(selectedPace.get(0));
        if (!selectedGroup.isEmpty()) prefs.setGroupType(selectedGroup.get(0));
        
        prefs.setAccessibilityNeeds(false); // Default to false since question removed

        userPreferencesService.addPreferences(prefs);

        // 3. Redirect to Home
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user/home.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) nextBtn.getScene().getWindow();
            // Set standard size for Home
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();
            
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
