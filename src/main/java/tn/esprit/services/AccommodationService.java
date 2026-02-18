package tn.esprit.services;

import tn.esprit.entities.Accommodation;
import tn.esprit.entities.Room;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccommodationService {

    Connection cnx;
    RoomService roomService;

    public AccommodationService() {
        cnx = MyDB.getConnection();
        roomService = new RoomService();
    }

    // 🔹 CREATE with all fields
    public void addAccommodation(Accommodation a) {
        String sql = "INSERT INTO accommodation " +
                "(name, type, city, country, address, description, stars, rating, image_path, " +
                "status, phone, email, website, postal_code, latitude, longitude, accommodation_amenities) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, a.getName());
            ps.setString(2, a.getType());
            ps.setString(3, a.getCity());
            ps.setString(4, a.getCountry());
            ps.setString(5, a.getAddress());
            ps.setString(6, a.getDescription());
            ps.setInt(7, a.getStars());
            ps.setDouble(8, a.getRating());
            ps.setString(9, a.getImagePath());
            ps.setString(10, a.getStatus() != null ? a.getStatus() : "Active");
            ps.setString(11, a.getPhone());
            ps.setString(12, a.getEmail());
            ps.setString(13, a.getWebsite());
            ps.setString(14, a.getPostalCode());

            if (a.getLatitude() != null) {
                ps.setDouble(15, a.getLatitude());
            } else {
                ps.setNull(15, Types.DOUBLE);
            }

            if (a.getLongitude() != null) {
                ps.setDouble(16, a.getLongitude());
            } else {
                ps.setNull(16, Types.DOUBLE);
            }

            // 🔥 NEW: Add accommodation_amenities
            ps.setString(17, a.getAccommodationAmenities());

            int affectedRows = ps.executeUpdate();

            // Get generated ID
            if (affectedRows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    a.setId(rs.getInt(1));
                    System.out.println("✅ Accommodation added with ID: " + a.getId());

                    // Add rooms if any
                    if (a.getRooms() != null && !a.getRooms().isEmpty()) {
                        for (Room room : a.getRooms()) {
                            room.setAccommodationId(a.getId());
                            roomService.addRoom(room);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error adding accommodation:");
            e.printStackTrace();
        }
    }

    // 🔹 READ ALL with full details
    public List<Accommodation> getAll() {
        List<Accommodation> list = new ArrayList<>();
        String sql = "SELECT * FROM accommodation ORDER BY created_at DESC";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Accommodation a = extractAccommodationFromResultSet(rs);

                // Load rooms for this accommodation
                List<Room> rooms = roomService.getRoomsByAccommodationId(a.getId());
                a.setRooms(rooms);

                list.add(a);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // 🔹 Alias for getAll()
    public List<Accommodation> getAllAccommodations() {
        return getAll();
    }

    // 🔹 GET BY ID with rooms
    public Accommodation getById(int id) {
        String sql = "SELECT * FROM accommodation WHERE id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Accommodation a = extractAccommodationFromResultSet(rs);

                // Load rooms
                List<Room> rooms = roomService.getRoomsByAccommodationId(id);
                a.setRooms(rooms);

                return a;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 🔹 UPDATE with all fields
    public void updateAccommodation(Accommodation a) {
        String sql = "UPDATE accommodation SET " +
                "name=?, type=?, city=?, country=?, address=?, description=?, " +
                "stars=?, rating=?, image_path=?, status=?, phone=?, email=?, " +
                "website=?, postal_code=?, latitude=?, longitude=?, accommodation_amenities=? " +
                "WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setString(1, a.getName());
            ps.setString(2, a.getType());
            ps.setString(3, a.getCity());
            ps.setString(4, a.getCountry());
            ps.setString(5, a.getAddress());
            ps.setString(6, a.getDescription());
            ps.setInt(7, a.getStars());
            ps.setDouble(8, a.getRating());
            ps.setString(9, a.getImagePath());
            ps.setString(10, a.getStatus());
            ps.setString(11, a.getPhone());
            ps.setString(12, a.getEmail());
            ps.setString(13, a.getWebsite());
            ps.setString(14, a.getPostalCode());

            if (a.getLatitude() != null) {
                ps.setDouble(15, a.getLatitude());
            } else {
                ps.setNull(15, Types.DOUBLE);
            }

            if (a.getLongitude() != null) {
                ps.setDouble(16, a.getLongitude());
            } else {
                ps.setNull(16, Types.DOUBLE);
            }

            // 🔥 NEW: Add accommodation_amenities
            ps.setString(17, a.getAccommodationAmenities());

            ps.setInt(18, a.getId());

            ps.executeUpdate();
            System.out.println("✅ Accommodation updated successfully!");

            // Update rooms if needed
            if (a.getRooms() != null) {
                // Delete old rooms and add new ones
                // Or implement a smarter update logic
                updateRoomsForAccommodation(a);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error updating accommodation:");
            e.printStackTrace();
        }
    }

    // 🔹 DELETE (cascade delete rooms via foreign key)
    public void deleteAccommodation(int id) {
        String sql = "DELETE FROM accommodation WHERE id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Accommodation deleted successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Error deleting accommodation:");
            e.printStackTrace();
        }
    }

    // 🔹 SEARCH BY CITY
    public List<Accommodation> getByCity(String city) {
        List<Accommodation> list = new ArrayList<>();
        String sql = "SELECT * FROM accommodation WHERE city LIKE ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, "%" + city + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Accommodation a = extractAccommodationFromResultSet(rs);
                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 SEARCH BY TYPE
    public List<Accommodation> getByType(String type) {
        List<Accommodation> list = new ArrayList<>();
        String sql = "SELECT * FROM accommodation WHERE type LIKE ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, "%" + type + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Accommodation a = extractAccommodationFromResultSet(rs);
                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 SEARCH BY STATUS
    public List<Accommodation> getByStatus(String status) {
        List<Accommodation> list = new ArrayList<>();
        String sql = "SELECT * FROM accommodation WHERE status = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Accommodation a = extractAccommodationFromResultSet(rs);
                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 🔹 GET DISTINCT CITIES
    public List<String> getAllCities() {
        List<String> cities = new ArrayList<>();
        String sql = "SELECT DISTINCT city FROM accommodation WHERE city IS NOT NULL ORDER BY city";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cities;
    }

    // 🔹 GET DISTINCT TYPES
    public List<String> getAllTypes() {
        List<String> types = new ArrayList<>();
        String sql = "SELECT DISTINCT type FROM accommodation WHERE type IS NOT NULL ORDER BY type";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                types.add(rs.getString("type"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return types;
    }

    // 🔹 FILTER ACCOMMODATIONS (Enhanced)
    public List<Accommodation> filterAccommodations(String city, String type, int minStars, double maxPrice) {
        List<Accommodation> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT DISTINCT a.* FROM accommodation a ");
        sql.append("LEFT JOIN room r ON a.id = r.accommodation_id ");
        sql.append("WHERE 1=1 ");

        List<Object> parameters = new ArrayList<>();

        if (city != null && !city.isEmpty() && !city.equals("All Destinations")) {
            sql.append("AND a.city = ? ");
            parameters.add(city);
        }

        if (type != null && !type.isEmpty() && !type.equals("All Types")) {
            sql.append("AND a.type = ? ");
            parameters.add(type);
        }

        if (minStars > 0) {
            sql.append("AND a.stars >= ? ");
            parameters.add(minStars);
        }

        sql.append("GROUP BY a.id ");

        if (maxPrice > 0) {
            sql.append("HAVING MIN(r.price_per_night) <= ? OR MIN(r.price_per_night) IS NULL ");
            parameters.add(maxPrice);
        }

        try {
            PreparedStatement ps = cnx.prepareStatement(sql.toString());

            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else if (param instanceof Double) {
                    ps.setDouble(i + 1, (Double) param);
                }
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Accommodation a = extractAccommodationFromResultSet(rs);
                list.add(a);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // 🔹 HELPER: Extract Accommodation from ResultSet
    private Accommodation extractAccommodationFromResultSet(ResultSet rs) throws SQLException {
        Accommodation a = new Accommodation();

        a.setId(rs.getInt("id"));
        a.setName(rs.getString("name"));
        a.setType(rs.getString("type"));
        a.setCity(rs.getString("city"));
        a.setCountry(rs.getString("country"));
        a.setAddress(rs.getString("address"));
        a.setDescription(rs.getString("description"));
        a.setStars(rs.getInt("stars"));
        a.setRating(rs.getDouble("rating"));
        a.setImagePath(rs.getString("image_path"));

        // New fields
        a.setStatus(rs.getString("status"));
        a.setPhone(rs.getString("phone"));
        a.setEmail(rs.getString("email"));
        a.setWebsite(rs.getString("website"));
        a.setPostalCode(rs.getString("postal_code"));

        double lat = rs.getDouble("latitude");
        if (!rs.wasNull()) a.setLatitude(lat);

        double lon = rs.getDouble("longitude");
        if (!rs.wasNull()) a.setLongitude(lon);

        // 🔥 NEW: Load accommodation_amenities
        a.setAccommodationAmenities(rs.getString("accommodation_amenities"));

        a.setCreatedAt(rs.getTimestamp("created_at"));
        a.setUpdatedAt(rs.getTimestamp("updated_at"));

        return a;
    }

    // 🔹 HELPER: Update rooms for accommodation
    private void updateRoomsForAccommodation(Accommodation a) {
        // This is a simple implementation - delete all old rooms and add new ones
        // You might want a smarter approach in production

        // For now, just keep existing rooms
        // Rooms should be managed separately through RoomService
    }
    // 🔹 HELPER: Get default image path based on type
    private String getDefaultImageForType(String type) {
        if (type == null) {
            return "images/hotels/hotel1.jpg";
        }

        switch (type.toLowerCase()) {
            case "hotel":
            case "boutique hotel":
                return "images/hotels/hotel1.jpg";
            case "villa":
                return "images/villas/villa1.jpg";
            case "apartment":
            case "guesthouse":
            case "bed & breakfast":
                return "images/apartments/apartment1.jpg";
            case "resort":
                return "images/hotels/hotel2.jpg";
            case "hostel":
            case "motel":
                return "images/hotels/hotel3.jpg";
            default:
                return "images/hotels/hotel1.jpg";
        }
    }
}