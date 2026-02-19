package tn.esprit.services;

import tn.esprit.entities.Destination;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DestinationService {
    private Connection conx;

    public DestinationService() {
        conx = MyDB.getInstance().getConx();
    }

    // CREATE - Add new destination
    public boolean addDestination(Destination destination) {
        String sql = "INSERT INTO destinations (name, type, country, city, best_season, description, timezone, average_rating) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, destination.getName());
            ps.setString(2, destination.getType().name());  // enum to string
            ps.setString(3, destination.getCountry());
            ps.setString(4, destination.getCity());
            ps.setString(5, destination.getBestSeason().name());
            ps.setString(6, destination.getDescription());
            ps.setString(7, destination.getTimezone());
            ps.setDouble(8, destination.getAverageRating() != null ? destination.getAverageRating() : 0.0);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        destination.setDestinationId(generatedKeys.getLong(1));
                    }
                }
                System.out.println("✅ Destination added successfully: " + destination.getName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error adding destination: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // READ - Get all destinations
    public List<Destination> getAllDestinations() {
        List<Destination> destinations = new ArrayList<>();
        String sql = "SELECT * FROM destinations ORDER BY created_at DESC";

        try (Statement stmt = conx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                destinations.add(mapResultSetToDestination(rs));
            }
            System.out.println("📋 Retrieved " + destinations.size() + " destinations");

        } catch (SQLException e) {
            System.err.println("❌ Error getting destinations: " + e.getMessage());
            e.printStackTrace();
        }
        return destinations;
    }

    // READ - Get destination by ID
    public Destination getDestinationById(Long id) {
        String sql = "SELECT * FROM destinations WHERE destination_id = ?";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToDestination(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting destination by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // READ - Search destinations
    public List<Destination> searchDestinations(String keyword) {
        List<Destination> results = new ArrayList<>();
        String sql = "SELECT * FROM destinations WHERE name LIKE ? OR country LIKE ? OR city LIKE ? ORDER BY average_rating DESC";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(mapResultSetToDestination(rs));
            }

            System.out.println("🔍 Found " + results.size() + " destinations for: '" + keyword + "'");

        } catch (SQLException e) {
            System.err.println("❌ Error searching destinations: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    // READ - Get destinations by type
    public List<Destination> getDestinationsByType(Destination.DestinationType type) {
        List<Destination> results = new ArrayList<>();
        String sql = "SELECT * FROM destinations WHERE type = ? ORDER BY average_rating DESC";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToDestination(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting destinations by type: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    // UPDATE - Update destination
    public boolean updateDestination(Destination destination) {
        String sql = "UPDATE destinations SET name=?, type=?, country=?, city=?, best_season=?, " +
                "description=?, timezone=?, average_rating=? WHERE destination_id=?";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1, destination.getName());
            ps.setString(2, destination.getType().name());
            ps.setString(3, destination.getCountry());
            ps.setString(4, destination.getCity());
            ps.setString(5, destination.getBestSeason().name());
            ps.setString(6, destination.getDescription());
            ps.setString(7, destination.getTimezone());
            ps.setDouble(8, destination.getAverageRating() != null ? destination.getAverageRating() : 0.0);
            ps.setLong(9, destination.getDestinationId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Destination updated successfully: " + destination.getName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error updating destination: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // DELETE - Delete destination
    public boolean deleteDestination(Long id) {
        // First check if there are any activities linked to this destination
        String checkSql = "SELECT COUNT(*) FROM activities WHERE destination_id = ?";
        String deleteSql = "DELETE FROM destinations WHERE destination_id = ?";

        try (PreparedStatement checkPs = conx.prepareStatement(checkSql)) {
            checkPs.setLong(1, id);
            ResultSet rs = checkPs.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.err.println("❌ Cannot delete: Destination has " + rs.getInt(1) + " activities linked");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking activities: " + e.getMessage());
        }

        // If no activities, proceed with deletion
        try (PreparedStatement ps = conx.prepareStatement(deleteSql)) {
            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Destination deleted successfully. ID: " + id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error deleting destination: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Helper method to map ResultSet to Destination object
    private Destination mapResultSetToDestination(ResultSet rs) throws SQLException {
        Destination d = new Destination();

        d.setDestinationId(rs.getLong("destination_id"));
        d.setName(rs.getString("name"));

        // Handle enum conversion safely
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            d.setType(Destination.DestinationType.valueOf(typeStr));
        }

        d.setCountry(rs.getString("country"));
        d.setCity(rs.getString("city"));

        String seasonStr = rs.getString("best_season");
        if (seasonStr != null) {
            d.setBestSeason(Destination.Season.valueOf(seasonStr));
        }

        d.setDescription(rs.getString("description"));
        d.setTimezone(rs.getString("timezone"));
        d.setAverageRating(rs.getDouble("average_rating"));

        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            d.setCreatedAt(timestamp.toLocalDateTime());
        }

        return d;
    }

    // Statistics methods
    public int getTotalDestinations() {
        String sql = "SELECT COUNT(*) FROM destinations";
        try (Statement stmt = conx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error counting destinations: " + e.getMessage());
        }
        return 0;
    }

    public List<Destination> getTopRatedDestinations(int limit) {
        List<Destination> topDestinations = new ArrayList<>();
        String sql = "SELECT * FROM destinations ORDER BY average_rating DESC LIMIT ?";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                topDestinations.add(mapResultSetToDestination(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top destinations: " + e.getMessage());
        }
        return topDestinations;
    }

    // Get destinations by country
    public List<Destination> getDestinationsByCountry(String country) {
        List<Destination> results = new ArrayList<>();
        String sql = "SELECT * FROM destinations WHERE country LIKE ? ORDER BY name";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1, "%" + country + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToDestination(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting destinations by country: " + e.getMessage());
        }
        return results;
    }
}