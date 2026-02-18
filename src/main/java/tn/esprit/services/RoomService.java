package tn.esprit.services;

import tn.esprit.entities.Room;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomService {

    Connection cnx;

    public RoomService() {
        cnx = MyDB.getConnection();
    }

    // 🔹 CREATE with all fields
    public void addRoom(Room r) {
        String sql = "INSERT INTO room " +
                "(accommodation_id, room_name, room_type, price_per_night, capacity, size, amenities, is_available) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, r.getAccommodationId());
            ps.setString(2, r.getRoomName() != null ? r.getRoomName() : (r.getRoomType() + " Room"));
            ps.setString(3, r.getRoomType());
            ps.setDouble(4, r.getPricePerNight());
            ps.setInt(5, r.getCapacity());
            ps.setDouble(6, r.getSize());
            ps.setString(7, r.getAmenities());
            ps.setBoolean(8, r.isAvailable());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    r.setId(rs.getInt(1));
                }
            }

            System.out.println("✅ Room added successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Error adding room:");
            e.printStackTrace();
        }
    }

    // 🔹 READ ALL
    public List<Room> getAll() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM room ORDER BY accommodation_id, room_type";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Room r = extractRoomFromResultSet(rs);
                list.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 READ BY ACCOMMODATION
    public List<Room> getRoomsByAccommodation(int accommodationId) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM room WHERE accommodation_id=? ORDER BY room_type";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, accommodationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Room r = extractRoomFromResultSet(rs);
                list.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 Alias for compatibility
    public List<Room> getRoomsByAccommodationId(int accommodationId) {
        return getRoomsByAccommodation(accommodationId);
    }

    // 🔹 GET BY ID
    public Room getRoomById(int id) {
        String sql = "SELECT * FROM room WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return extractRoomFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 🔹 UPDATE with all fields
    public void updateRoom(Room r) {
        String sql = "UPDATE room SET accommodation_id=?, room_name=?, room_type=?, " +
                "price_per_night=?, capacity=?, size=?, amenities=?, is_available=? " +
                "WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setInt(1, r.getAccommodationId());
            ps.setString(2, r.getRoomName());
            ps.setString(3, r.getRoomType());
            ps.setDouble(4, r.getPricePerNight());
            ps.setInt(5, r.getCapacity());
            ps.setDouble(6, r.getSize());
            ps.setString(7, r.getAmenities());
            ps.setBoolean(8, r.isAvailable());
            ps.setInt(9, r.getId());

            ps.executeUpdate();
            System.out.println("✅ Room updated successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Error updating room:");
            e.printStackTrace();
        }
    }

    // 🔹 DELETE
    public void deleteRoom(int id) {
        String sql = "DELETE FROM room WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);

            ps.executeUpdate();
            System.out.println("✅ Room deleted successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Error deleting room:");
            e.printStackTrace();
        }
    }

    // 🔹 DELETE ALL ROOMS FOR ACCOMMODATION
    public void deleteRoomsByAccommodationId(int accommodationId) {
        String sql = "DELETE FROM room WHERE accommodation_id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, accommodationId);

            int deleted = ps.executeUpdate();
            System.out.println("✅ Deleted " + deleted + " rooms for accommodation " + accommodationId);

        } catch (SQLException e) {
            System.err.println("❌ Error deleting rooms:");
            e.printStackTrace();
        }
    }

    // 🔹 GET MINIMUM PRICE FOR ACCOMMODATION
    public double getMinimumPriceForAccommodation(int accommodationId) {
        String sql = "SELECT MIN(price_per_night) as min_price FROM room WHERE accommodation_id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, accommodationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("min_price");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // 🔹 GET AVAILABLE ROOMS FOR ACCOMMODATION
    public List<Room> getAvailableRoomsByAccommodationId(int accommodationId) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM room WHERE accommodation_id=? AND is_available=1 ORDER BY price_per_night";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, accommodationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Room r = extractRoomFromResultSet(rs);
                list.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 COUNT ROOMS FOR ACCOMMODATION
    public int countRoomsByAccommodationId(int accommodationId) {
        String sql = "SELECT COUNT(*) as count FROM room WHERE accommodation_id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, accommodationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 🔹 HELPER: Extract Room from ResultSet
    private Room extractRoomFromResultSet(ResultSet rs) throws SQLException {
        Room r = new Room();

        r.setId(rs.getInt("id"));
        r.setAccommodationId(rs.getInt("accommodation_id"));
        r.setRoomName(rs.getString("room_name"));
        r.setRoomType(rs.getString("room_type"));
        r.setPricePerNight(rs.getDouble("price_per_night"));
        r.setCapacity(rs.getInt("capacity"));

        double size = rs.getDouble("size");
        if (!rs.wasNull()) {
            r.setSize(size);
        }

        r.setAmenities(rs.getString("amenities"));
        r.setAvailable(rs.getBoolean("is_available"));

        return r;
    }
    // 🔹 ADD THIS METHOD TO YOUR RoomService.java class

    /**
     * Get features/amenities for an accommodation from its rooms
     */
    public List<String> getFeaturesForAccommodation(int accommodationId) {
        List<String> features = new ArrayList<>();
        String sql = "SELECT amenities FROM room WHERE accommodation_id = ? AND amenities IS NOT NULL LIMIT 1";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, accommodationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String amenities = rs.getString("amenities");
                if (amenities != null && !amenities.trim().isEmpty()) {
                    // Split amenities by comma
                    String[] amenityArray = amenities.split(",");
                    for (String amenity : amenityArray) {
                        String cleaned = formatFeatureName(amenity.trim());
                        if (!cleaned.isEmpty()) {
                            features.add(cleaned);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Add default features if none found
        if (features.isEmpty()) {
            features.add("Free WiFi");
            features.add("Air Conditioning");
            features.add("Hotel Service");
        }

        return features;
    }

    /**
     * Format feature names to be user-friendly
     * Converts "wifi" to "Wifi", "sea_view" to "Sea View", etc.
     */
    private String formatFeatureName(String feature) {
        if (feature == null || feature.isEmpty()) {
            return "";
        }

        // Replace underscores with spaces
        String formatted = feature.replace("_", " ");

        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return result.toString().trim();
    }
}