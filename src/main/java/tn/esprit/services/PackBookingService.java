package tn.esprit.services;

import tn.esprit.entities.PacksBooking;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PackBookingService implements ICRUD<PacksBooking> {

    private Connection conx;

    public PackBookingService() {
        conx = MyDB.getInstance().getConx();
    }

    @Override
    public void add(PacksBooking booking) throws SQLException {
        String query = "INSERT INTO packs_bookings (user_id, pack_id, travel_start_date, travel_end_date, " +
                      "num_travelers, total_price, discount_applied, final_price, status, notes) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setInt(1, booking.getUserId());
            pst.setInt(2, booking.getPackId());
            pst.setDate(3, booking.getTravelStartDate());
            pst.setDate(4, booking.getTravelEndDate());
            pst.setInt(5, booking.getNumTravelers());
            pst.setBigDecimal(6, booking.getTotalPrice());
            pst.setBigDecimal(7, booking.getDiscountApplied());
            pst.setBigDecimal(8, booking.getFinalPrice());
            pst.setString(9, booking.getStatus().name());
            pst.setString(10, booking.getNotes());
            
            pst.executeUpdate();
        }
    }
//
//    @Override
//    public void addMeth2(Booking booking) throws SQLException {
//        add(booking);
//    }

    @Override
    public void modifier(PacksBooking booking) throws SQLException {
        String query = "UPDATE packs_bookings SET travel_start_date = ?, travel_end_date = ?, " +
                      "num_travelers = ?, total_price = ?, discount_applied = ?, final_price = ?, " +
                      "status = ?, notes = ? WHERE id_booking = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setDate(1, booking.getTravelStartDate());
            pst.setDate(2, booking.getTravelEndDate());
            pst.setInt(3, booking.getNumTravelers());
            pst.setBigDecimal(4, booking.getTotalPrice());
            pst.setBigDecimal(5, booking.getDiscountApplied());
            pst.setBigDecimal(6, booking.getFinalPrice());
            pst.setString(7, booking.getStatus().name());
            pst.setString(8, booking.getNotes());
            pst.setInt(9, booking.getIdBooking());
            
            pst.executeUpdate();
        }
    }

    @Override
    public void delete(PacksBooking booking) throws SQLException {
        String query = "DELETE FROM packs_bookings WHERE id_booking = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setInt(1, booking.getIdBooking());
            pst.executeUpdate();
        }
    }

    @Override
    public List<PacksBooking> afficherList() throws SQLException {
        List<PacksBooking> bookings = new ArrayList<>();
        String query = "SELECT * FROM packs_bookings ORDER BY booking_date DESC";
        
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                bookings.add(mapResultSetToBooking(rs));
            }
        }
        
        return bookings;
    }

    public List<PacksBooking> getByUserId(int userId) throws SQLException {
        List<PacksBooking> bookings = new ArrayList<>();
        String query = "SELECT * FROM packs_bookings WHERE user_id = ? ORDER BY booking_date DESC";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapResultSetToBooking(rs));
                }
            }
        }
        
        return bookings;
    }

    public List<PacksBooking> getByPackId(int packId) throws SQLException {
        List<PacksBooking> bookings = new ArrayList<>();
        String query = "SELECT * FROM packs_bookings WHERE pack_id = ? ORDER BY booking_date DESC";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setInt(1, packId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapResultSetToBooking(rs));
                }
            }
        }
        
        return bookings;
    }

    public PacksBooking getById(int bookingId) throws SQLException {
        String query = "SELECT * FROM packs_bookings WHERE id_booking = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setInt(1, bookingId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBooking(rs);
                }
            }
        }
        
        return null;
    }

    public void updateStatus(int bookingId, PacksBooking.Status status) throws SQLException {
        String query = "UPDATE packs_bookings SET status = ? WHERE id_booking = ?";
        
        try (PreparedStatement pst = conx.prepareStatement(query)) {
            pst.setString(1, status.name());
            pst.setInt(2, bookingId);
            pst.executeUpdate();
        }
    }

    private PacksBooking mapResultSetToBooking(ResultSet rs) throws SQLException {
        return new PacksBooking(
            rs.getInt("id_booking"),
            rs.getInt("user_id"),
            rs.getInt("pack_id"),
            rs.getTimestamp("booking_date"),
            rs.getDate("travel_start_date"),
            rs.getDate("travel_end_date"),
            rs.getInt("num_travelers"),
            rs.getBigDecimal("total_price"),
            rs.getBigDecimal("discount_applied"),
            rs.getBigDecimal("final_price"),
            PacksBooking.Status.valueOf(rs.getString("status")),
            rs.getString("notes"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }
}
