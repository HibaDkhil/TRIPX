package tn.esprit.services;

import tn.esprit.entities.Transport;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransportService {

    private Connection conx;

    public TransportService() {
        conx = MyDB.getInstance().getConx();
    }

    // Create
    public void addTransport(Transport t) {
        String sql = "INSERT INTO transport (transport_type, provider_name, vehicle_model, base_price, capacity, available_units, sustainability_rating, amenities, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1, t.getTransportType());
            ps.setString(2, t.getProviderName());
            ps.setString(3, t.getVehicleModel());
            ps.setDouble(4, t.getBasePrice());
            ps.setInt(5, t.getCapacity());
            ps.setInt(6, t.getAvailableUnits());
            ps.setDouble(7, t.getSustainabilityRating());
            ps.setString(8, t.getAmenities());
            ps.setString(9, t.getImageUrl());
            ps.executeUpdate();
            System.out.println("Transport added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Read all
    public List<Transport> getAllTransports() {
        List<Transport> list = new ArrayList<>();
        String sql = "SELECT * FROM transport";
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Transport t = new Transport();
                t.setTransportId(rs.getInt("transport_id"));
                t.setTransportType(rs.getString("transport_type"));
                t.setProviderName(rs.getString("provider_name"));
                t.setVehicleModel(rs.getString("vehicle_model"));
                t.setBasePrice(rs.getDouble("base_price"));
                t.setCapacity(rs.getInt("capacity"));
                t.setAvailableUnits(rs.getInt("available_units"));
                t.setSustainabilityRating(rs.getDouble("sustainability_rating"));
                t.setAmenities(rs.getString("amenities"));
                t.setImageUrl(rs.getString("image_url"));
                list.add(t);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    // Update
    public void updateTransport(Transport t) {
        String sql = "UPDATE transport SET transport_type=?, provider_name=?, vehicle_model=?, base_price=?, capacity=?, available_units=?, sustainability_rating=?, amenities=?, image_url=? WHERE transport_id=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1, t.getTransportType());
            ps.setString(2, t.getProviderName());
            ps.setString(3, t.getVehicleModel());
            ps.setDouble(4, t.getBasePrice());
            ps.setInt(5, t.getCapacity());
            ps.setInt(6, t.getAvailableUnits());
            ps.setDouble(7, t.getSustainabilityRating());
            ps.setString(8, t.getAmenities());
            ps.setString(9, t.getImageUrl());
            ps.setInt(10, t.getTransportId());
            ps.executeUpdate();
            System.out.println("Transport updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Delete
    public void deleteTransport(int id) {
        String sql = "DELETE FROM transport WHERE transport_id=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Transport deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
