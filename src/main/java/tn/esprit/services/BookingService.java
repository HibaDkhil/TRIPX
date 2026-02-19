package tn.esprit.services;

import tn.esprit.entities.Booking;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookingService {

    private Connection cnx;

    public BookingService() {
        cnx = MyDB.getInstance().getConx();
    }

    public boolean createBooking(Booking booking) {
        String sql = "INSERT INTO bookingdes (booking_reference, user_id, destination_id, activity_id, start_at, end_at, num_guests, status, payment_status, total_amount, currency, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            // Generate Unique Reference if not provided
            if (booking.getBookingReference() == null || booking.getBookingReference().isEmpty()) {
                booking.setBookingReference("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }

            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, booking.getBookingReference());
            ps.setInt(2, booking.getUserId());
            ps.setLong(3, booking.getDestinationId());
            
            if (booking.getActivityId() != null) {
                ps.setLong(4, booking.getActivityId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }

            ps.setTimestamp(5, booking.getStartAt());
            ps.setTimestamp(6, booking.getEndAt());
            ps.setInt(7, booking.getNumGuests());
            ps.setString(8, booking.getStatus().toString());
            ps.setString(9, booking.getPaymentStatus().toString());
            ps.setDouble(10, booking.getTotalAmount());
            ps.setString(11, booking.getCurrency());
            ps.setString(12, booking.getNotes());

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        // Join with destinations and activities to get names
        String sql = "SELECT b.*, d.name as dest_name, a.name as act_name " +
                     "FROM bookingdes b " +
                     "LEFT JOIN destinations d ON b.destination_id = d.destination_id " +
                     "LEFT JOIN activities a ON b.activity_id = a.activity_id " +
                     "ORDER BY b.created_at DESC";
        
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                bookings.add(mapResultSetToBooking(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    public List<Booking> getBookingsByUser(int userId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT b.*, d.name as dest_name, a.name as act_name " +
                     "FROM bookingdes b " +
                     "LEFT JOIN destinations d ON b.destination_id = d.destination_id " +
                     "LEFT JOIN activities a ON b.activity_id = a.activity_id " +
                     "WHERE b.user_id = ? " +
                     "ORDER BY b.created_at DESC";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bookings.add(mapResultSetToBooking(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    public boolean updateStatus(long bookingId, Booking.BookingStatus status) {
        String sql = "UPDATE bookingdes SET status = ? WHERE booking_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, status.toString());
            ps.setLong(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBooking(long bookingId) {
        String sql = "DELETE FROM bookingdes WHERE booking_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setLong(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBooking(Booking booking) {
        String sql = "UPDATE bookingdes SET start_at=?, end_at=?, num_guests=?, activity_id=?, total_amount=?, notes=?, status=?, payment_status=? WHERE booking_id=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setTimestamp(1, booking.getStartAt());
            ps.setTimestamp(2, booking.getEndAt());
            ps.setInt(3, booking.getNumGuests());
            
            if (booking.getActivityId() != null) {
                ps.setLong(4, booking.getActivityId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            
            ps.setDouble(5, booking.getTotalAmount());
            ps.setString(6, booking.getNotes());
            ps.setString(7, booking.getStatus().toString());
            ps.setString(8, booking.getPaymentStatus().toString());
            ps.setLong(9, booking.getBookingId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePaymentStatus(long bookingId, Booking.PaymentStatus status) {
        String sql = "UPDATE bookingdes SET payment_status = ? WHERE booking_id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, status.toString());
            ps.setLong(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getLong("booking_id"));
        b.setBookingReference(rs.getString("booking_reference"));
        b.setUserId(rs.getInt("user_id"));
        b.setDestinationId(rs.getLong("destination_id"));
        
        long actId = rs.getLong("activity_id");
        if (!rs.wasNull()) {
            b.setActivityId(actId);
        }

        b.setStartAt(rs.getTimestamp("start_at"));
        b.setEndAt(rs.getTimestamp("end_at"));
        b.setNumGuests(rs.getInt("num_guests"));
        
        try {
            String statusStr = rs.getString("status");
            if (statusStr != null) {
                b.setStatus(Booking.BookingStatus.valueOf(statusStr.trim().toUpperCase()));
            } else {
                 b.setStatus(Booking.BookingStatus.PENDING);
            }
        } catch (Exception e) {
            b.setStatus(Booking.BookingStatus.PENDING); 
        }

        try {
            b.setPaymentStatus(Booking.PaymentStatus.valueOf(rs.getString("payment_status")));
        } catch (Exception e) { b.setPaymentStatus(Booking.PaymentStatus.UNPAID); }

        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setCurrency(rs.getString("currency"));
        b.setNotes(rs.getString("notes"));
        b.setCreatedAt(rs.getTimestamp("created_at"));

        // Transient fields
        b.setDestinationName(rs.getString("dest_name"));
        b.setActivityName(rs.getString("act_name"));

        return b;
    }
}
