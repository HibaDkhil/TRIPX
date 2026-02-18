package tn.esprit.services;

import tn.esprit.entities.Booking;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingService {

    Connection cnx;

    public BookingService() {
        cnx = MyDB.getConnection();
    }

    // CREATE booking
    public boolean addBooking(Booking b) {
        // Check availability before booking
        if (!isRoomAvailable(b.getRoomId(), b.getCheckIn(), b.getCheckOut())) {
            System.out.println("❌ Room is not available for these dates.");
            return false;
        }

        String sql = "INSERT INTO booking (user_id, room_id, check_in, check_out, total_price, status) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, b.getUserId());
            ps.setInt(2, b.getRoomId());
            ps.setDate(3, b.getCheckIn());
            ps.setDate(4, b.getCheckOut());
            ps.setDouble(5, b.getTotalPrice());
            ps.setString(6, b.getStatus());

            ps.executeUpdate();
            System.out.println("✅ Booking added successfully.");
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // READ all bookings
    public List<Booking> getAll() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM booking";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Booking b = new Booking(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("room_id"),
                        rs.getDate("check_in"),
                        rs.getDate("check_out"),
                        rs.getDouble("total_price"),
                        rs.getString("status")
                );
                list.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Check if room is available between dates
    public boolean isRoomAvailable(int roomId, Date checkIn, Date checkOut) {
        String sql = "SELECT COUNT(*) FROM booking WHERE room_id = ? AND NOT (check_out <= ? OR check_in >= ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, roomId);
            ps.setDate(2, checkIn);
            ps.setDate(3, checkOut);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count == 0; // zero overlapping bookings means available
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
