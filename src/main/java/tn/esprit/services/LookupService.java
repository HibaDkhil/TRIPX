package tn.esprit.services;

import tn.esprit.entities.Accommodation;
import tn.esprit.entities.Activity;
import tn.esprit.entities.Destination;
import tn.esprit.entities.Transport;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for read-only lookup tables (managed by other modules)
 */
public class LookupService {

    private Connection conx;

    public LookupService() {
        conx = MyDB.getInstance().getConx();
    }

    // ============ DESTINATIONS ============
    
    public List<Destination> getAllDestinations() throws SQLException {
        List<Destination> destinations = new ArrayList<>();
        String query = "SELECT destination_id, name FROM destinations";  // Updated column name
        
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                Destination destination = new Destination();
                destination.setDestinationId(rs.getLong("destination_id"));
                destination.setName(rs.getString("name"));
                destinations.add(destination);
            }
        }
        
        return destinations;
    }

    public Destination getDestinationById(long id) throws SQLException {
        String query = "SELECT destination_id, name FROM destinations WHERE destination_id = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setLong(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Destination destination = new Destination();
                    destination.setDestinationId(rs.getLong("destination_id"));
                    destination.setName(rs.getString("name"));
                    return destination;
                }
            }
        }
        
        return null;
    }

    // ============ ACTIVITIES ============
    
    public List<Activity> getAllActivities() throws SQLException {
        List<Activity> activities = new ArrayList<>();
        String query = "SELECT activity_id, name FROM activities";  // Updated column name
        
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                Activity activity = new Activity();
                activity.setActivityId(rs.getLong("activity_id"));
                activity.setName(rs.getString("name"));
                activities.add(activity);
            }
        }
        
        return activities;
    }

    public Activity getActivityById(long id) throws SQLException {
        String query = "SELECT activity_id, name FROM activities WHERE activity_id = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setLong(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Activity activity = new Activity();
                    activity.setActivityId(rs.getLong("activity_id"));
                    activity.setName(rs.getString("name"));
                    return activity;
                }
            }
        }
        
        return null;
    }

    // ============ ACCOMMODATIONS ============
    
    public List<Accommodation> getAllAccommodations() throws SQLException {
        List<Accommodation> accommodations = new ArrayList<>();
        String query = "SELECT id, name FROM accommodation";  // Updated table and column names
        
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                Accommodation accommodation = new Accommodation();
                accommodation.setId(rs.getInt("id"));
                accommodation.setName(rs.getString("name"));
                accommodations.add(accommodation);
            }
        }
        
        return accommodations;
    }

    public Accommodation getAccommodationById(int id) throws SQLException {
        String query = "SELECT id, name FROM accommodation WHERE id = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Accommodation accommodation = new Accommodation();
                    accommodation.setId(rs.getInt("id"));
                    accommodation.setName(rs.getString("name"));
                    return accommodation;
                }
            }
        }
        
        return null;
    }

    // ============ TRANSPORT ============
    
    public List<Transport> getAllTransport() throws SQLException {
        List<Transport> transports = new ArrayList<>();
        String query = "SELECT transport_id, transport_type FROM transport";  // Updated column name
        
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                Transport transport = new Transport();
                transport.setTransportId(rs.getInt("transport_id"));
                transport.setTransportType(rs.getString("transport_type"));
                transports.add(transport);
            }
        }
        
        return transports;
    }

    public Transport getTransportById(int id) throws SQLException {
        String query = "SELECT transport_id, transport_type FROM transport WHERE transport_id = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Transport transport = new Transport();
                    transport.setTransportId(rs.getInt("transport_id"));
                    transport.setTransportType(rs.getString("transport_type"));
                    return transport;
                }
            }
        }
        
        return null;
    }
}
