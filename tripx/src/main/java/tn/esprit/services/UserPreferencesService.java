package tn.esprit.services;

import tn.esprit.entities.UserPreferences;
import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserPreferencesService {

    private Connection conx;

    public UserPreferencesService() {
        conx = MyDB.getInstance().getConx();
    }

    public void addPreferences(UserPreferences prefs) {
        // Check if preferences already exist for this user
        if (getPreferencesByUserId(prefs.getUserId()) != null) {
            System.out.println("Preferences already exist for user " + prefs.getUserId());
            return; 
        }

        String sql = "INSERT INTO userpreferences (user_id, budget_min_per_night, budget_max_per_night, priorities, " +
                     "location_preferences, accommodation_types, style_preferences, dietary_restrictions, " +
                     "preferred_climate, travel_pace, group_type, accessibility_needs, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        
        try (PreparedStatement startPs = conx.prepareStatement(sql)) {
            startPs.setInt(1, prefs.getUserId());
            startPs.setBigDecimal(2, prefs.getBudgetMinPerNight());
            startPs.setBigDecimal(3, prefs.getBudgetMaxPerNight());
            startPs.setString(4, prefs.getPriorities());
            startPs.setString(5, prefs.getLocationPreferences());
            startPs.setString(6, prefs.getAccommodationTypes());
            startPs.setString(7, prefs.getStylePreferences());
            startPs.setString(8, prefs.getDietaryRestrictions());
            startPs.setString(9, prefs.getPreferredClimate());
            startPs.setString(10, prefs.getTravelPace());
            startPs.setString(11, prefs.getGroupType());
            startPs.setBoolean(12, prefs.isAccessibilityNeeds()); // Even if we don't ask, we can save default/false

            startPs.executeUpdate();
            System.out.println("Preferences added successfully!");
        } catch (SQLException e) {
            System.err.println("Error adding preferences: " + e.getMessage());
        }
    }

    public UserPreferences getPreferencesByUserId(int userId) {
        String sql = "SELECT * FROM userpreferences WHERE user_id = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                UserPreferences prefs = new UserPreferences();
                prefs.setPreferenceId(rs.getInt("preference_id"));
                prefs.setUserId(rs.getInt("user_id"));
                prefs.setBudgetMinPerNight(rs.getBigDecimal("budget_min_per_night"));
                prefs.setBudgetMaxPerNight(rs.getBigDecimal("budget_max_per_night"));
                prefs.setPriorities(rs.getString("priorities"));
                prefs.setLocationPreferences(rs.getString("location_preferences"));
                prefs.setAccommodationTypes(rs.getString("accommodation_types"));
                prefs.setStylePreferences(rs.getString("style_preferences"));
                prefs.setDietaryRestrictions(rs.getString("dietary_restrictions"));
                prefs.setPreferredClimate(rs.getString("preferred_climate"));
                prefs.setTravelPace(rs.getString("travel_pace"));
                prefs.setGroupType(rs.getString("group_type"));
                prefs.setAccessibilityNeeds(rs.getBoolean("accessibility_needs"));
                return prefs;
            }
        } catch (SQLException e) {
            System.err.println("Error getting preferences: " + e.getMessage());
        }
        return null;
    }
    public boolean updatePreferences(UserPreferences prefs) {
        String sql = "UPDATE userpreferences SET budget_min_per_night = ?, budget_max_per_night = ?, priorities = ?, " +
                     "location_preferences = ?, accommodation_types = ?, style_preferences = ?, dietary_restrictions = ?, " +
                     "preferred_climate = ?, travel_pace = ?, group_type = ?, accessibility_needs = ?, updated_at = NOW() " +
                     "WHERE user_id = ?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setBigDecimal(1, prefs.getBudgetMinPerNight());
            ps.setBigDecimal(2, prefs.getBudgetMaxPerNight());
            ps.setString(3, prefs.getPriorities());
            ps.setString(4, prefs.getLocationPreferences());
            ps.setString(5, prefs.getAccommodationTypes());
            ps.setString(6, prefs.getStylePreferences());
            ps.setString(7, prefs.getDietaryRestrictions());
            ps.setString(8, prefs.getPreferredClimate());
            ps.setString(9, prefs.getTravelPace());
            ps.setString(10, prefs.getGroupType());
            ps.setBoolean(11, prefs.isAccessibilityNeeds());
            ps.setInt(12, prefs.getUserId());

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating preferences: " + e.getMessage());
            return false;
        }
    }
    public boolean deletePreferencesByUserId(int userId) {
        String sql = "DELETE FROM userpreferences WHERE user_id = ?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting preferences: " + e.getMessage());
            return false;
        }
    }
}
