package tn.esprit.entities;

import java.math.BigDecimal;
import java.util.Date;

public class UserPreferences {
    private int preferenceId;
    private int userId;
    private BigDecimal budgetMinPerNight;
    private BigDecimal budgetMaxPerNight;
    private String priorities; // stored as JSON or CSV
    private String locationPreferences;
    private String accommodationTypes;
    private String stylePreferences;
    private String dietaryRestrictions;
    private String preferredClimate;
    private String travelPace; // Enum stored as String: 'Relaxed', 'Moderate', 'Fast-paced'
    private String groupType; // Enum stored as String: 'Solo', 'Couple', 'Family', 'Friends', 'Business'
    private boolean accessibilityNeeds;
    private Date createdAt;
    private Date updatedAt;

    public UserPreferences() {
    }

    public UserPreferences(int userId, BigDecimal budgetMinPerNight, BigDecimal budgetMaxPerNight, String priorities,
                           String locationPreferences, String accommodationTypes, String stylePreferences,
                           String dietaryRestrictions, String preferredClimate, String travelPace, String groupType,
                           boolean accessibilityNeeds) {
        this.userId = userId;
        this.budgetMinPerNight = budgetMinPerNight;
        this.budgetMaxPerNight = budgetMaxPerNight;
        this.priorities = priorities;
        this.locationPreferences = locationPreferences;
        this.accommodationTypes = accommodationTypes;
        this.stylePreferences = stylePreferences;
        this.dietaryRestrictions = dietaryRestrictions;
        this.preferredClimate = preferredClimate;
        this.travelPace = travelPace;
        this.groupType = groupType;
        this.accessibilityNeeds = accessibilityNeeds;
    }

    public int getPreferenceId() { return preferenceId; }
    public void setPreferenceId(int preferenceId) { this.preferenceId = preferenceId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public BigDecimal getBudgetMinPerNight() { return budgetMinPerNight; }
    public void setBudgetMinPerNight(BigDecimal budgetMinPerNight) { this.budgetMinPerNight = budgetMinPerNight; }

    public BigDecimal getBudgetMaxPerNight() { return budgetMaxPerNight; }
    public void setBudgetMaxPerNight(BigDecimal budgetMaxPerNight) { this.budgetMaxPerNight = budgetMaxPerNight; }

    public String getPriorities() { return priorities; }
    public void setPriorities(String priorities) { this.priorities = priorities; }

    public String getLocationPreferences() { return locationPreferences; }
    public void setLocationPreferences(String locationPreferences) { this.locationPreferences = locationPreferences; }

    public String getAccommodationTypes() { return accommodationTypes; }
    public void setAccommodationTypes(String accommodationTypes) { this.accommodationTypes = accommodationTypes; }

    public String getStylePreferences() { return stylePreferences; }
    public void setStylePreferences(String stylePreferences) { this.stylePreferences = stylePreferences; }

    public String getDietaryRestrictions() { return dietaryRestrictions; }
    public void setDietaryRestrictions(String dietaryRestrictions) { this.dietaryRestrictions = dietaryRestrictions; }

    public String getPreferredClimate() { return preferredClimate; }
    public void setPreferredClimate(String preferredClimate) { this.preferredClimate = preferredClimate; }

    public String getTravelPace() { return travelPace; }
    public void setTravelPace(String travelPace) { this.travelPace = travelPace; }

    public String getGroupType() { return groupType; }
    public void setGroupType(String groupType) { this.groupType = groupType; }

    public boolean isAccessibilityNeeds() { return accessibilityNeeds; }
    public void setAccessibilityNeeds(boolean accessibilityNeeds) { this.accessibilityNeeds = accessibilityNeeds; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "UserPreferences{" +
                "preferenceId=" + preferenceId +
                ", userId=" + userId +
                ", priorities='" + priorities + '\'' +
                ", preferredClimate='" + preferredClimate + '\'' +
                '}';
    }
}
