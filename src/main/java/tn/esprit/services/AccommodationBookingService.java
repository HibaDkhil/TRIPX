package tn.esprit.services;

import tn.esprit.entities.AccommodationBooking;
import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AccommodationBookingService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    Connection cnx;
    private final AccommodationBookingCalendarSyncService calendarSyncService;

    public AccommodationBookingService() {
        cnx = MyDB.getConnection();
        calendarSyncService = new AccommodationBookingCalendarSyncService();
    }

    // CREATE accommodation booking
    public boolean addAccommodationBooking(AccommodationBooking booking) {
        if (!isValidBookingDates(booking.getCheckIn(), booking.getCheckOut())) {
            System.out.println("❌ Invalid booking dates.");
            return false;
        }

        if (booking.getTotalPrice() < 0) {
            System.out.println("❌ Invalid booking total price.");
            return false;
        }

        if (!isAccommodationRoomAvailable(booking.getRoomId(), booking.getCheckIn(), booking.getCheckOut())) {
            System.out.println("❌ Room is not available for selected dates.");
            return false;
        }

        String insertSql = "INSERT INTO bookingacc " +
                "(user_id, room_id, check_in, check_out, total_price, number_of_guests, phone_number, special_requests, estimated_arrival_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, booking.getUserId());
            ps.setInt(2, booking.getRoomId());
            ps.setDate(3, booking.getCheckIn());
            ps.setDate(4, booking.getCheckOut());
            ps.setDouble(5, booking.getTotalPrice());
            ps.setInt(6, booking.getNumberOfGuests() > 0 ? booking.getNumberOfGuests() : 1);
            ps.setString(7, trimToNull(booking.getPhoneNumber()));
            ps.setString(8, trimToNull(booking.getSpecialRequests()));
            ps.setString(9, trimToNull(booking.getEstimatedArrivalTime()));
            ps.setString(10, normalizeStatus(booking.getStatus(), STATUS_PENDING));

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    booking.setId(generatedKeys.getInt(1));
                }
                System.out.println("✅ Accommodation booking added successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error adding accommodation booking:");
            e.printStackTrace();
        }
        return false;
    }

    // READ all accommodation bookings
    public List<AccommodationBooking> getAllAccommodationBookings() {
        List<AccommodationBooking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookingacc ORDER BY created_at DESC";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                list.add(extractAccommodationBookingFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error loading accommodation bookings:");
            e.printStackTrace();
        }

        return list;
    }

    // READ accommodation bookings by user
    public List<AccommodationBooking> getAccommodationBookingsByUserId(int userId) {
        List<AccommodationBooking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookingacc WHERE user_id = ? ORDER BY created_at DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(extractAccommodationBookingFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error loading user accommodation bookings:");
            e.printStackTrace();
        }

        return list;
    }

    // READ accommodation bookings by status
    public List<AccommodationBooking> getAccommodationBookingsByStatus(String status) {
        List<AccommodationBooking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookingacc WHERE status = ? ORDER BY created_at DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, normalizeStatus(status, STATUS_PENDING));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(extractAccommodationBookingFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error loading accommodation bookings by status:");
            e.printStackTrace();
        }

        return list;
    }

    // READ one accommodation booking by id
    public AccommodationBooking getAccommodationBookingById(int bookingId) {
        String sql = "SELECT * FROM bookingacc WHERE id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return extractAccommodationBookingFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error loading accommodation booking by id:");
            e.printStackTrace();
        }

        return null;
    }

    // Availability check for accommodation room
    public boolean isAccommodationRoomAvailable(int roomId, Date checkIn, Date checkOut) {
        if (!isValidBookingDates(checkIn, checkOut)) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM bookingacc " +
                "WHERE room_id = ? " +
                "AND status IN ('PENDING', 'CONFIRMED') " +
                "AND check_in < ? " +
                "AND check_out > ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, roomId);
            ps.setDate(2, checkOut);
            ps.setDate(3, checkIn);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count == 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking room availability:");
            e.printStackTrace();
        }

        return false;
    }

    // ADMIN action: confirm booking
    public boolean confirmAccommodationBooking(int bookingId) {
        return updateAccommodationBookingStatus(bookingId, STATUS_CONFIRMED);
    }

    // ADMIN action: reject booking
    public boolean rejectAccommodationBooking(int bookingId) {
        return rejectAccommodationBooking(bookingId, null);
    }

    // ADMIN action: reject booking with reason
    public boolean rejectAccommodationBooking(int bookingId, String rejectionReason) {
        return updateAccommodationBookingStatus(bookingId, STATUS_REJECTED, null, rejectionReason);
    }

    // USER/ADMIN action: cancel booking
    public boolean cancelAccommodationBooking(int bookingId) {
        return cancelAccommodationBooking(bookingId, null);
    }

    // USER/ADMIN action: cancel booking with reason
    public boolean cancelAccommodationBooking(int bookingId, String cancelReason) {
        return updateAccommodationBookingStatus(bookingId, STATUS_CANCELLED, cancelReason, null);
    }

    // Generic status update with transition checks
    public boolean updateAccommodationBookingStatus(int bookingId, String newStatus) {
        return updateAccommodationBookingStatus(bookingId, newStatus, null, null);
    }

    // Generic status update with transition checks and optional reasons
    public boolean updateAccommodationBookingStatus(int bookingId, String newStatus, String cancelReason, String rejectionReason) {
        String targetStatus = normalizeStatus(newStatus, STATUS_PENDING);
        AccommodationBooking current = getAccommodationBookingById(bookingId);

        if (current == null) {
            System.out.println("❌ Booking not found.");
            return false;
        }

        String currentStatus = normalizeStatus(current.getStatus(), STATUS_PENDING);
        if (!isAllowedTransition(currentStatus, targetStatus)) {
            System.out.println("❌ Invalid status transition: " + currentStatus + " -> " + targetStatus);
            return false;
        }

        String sql = "UPDATE bookingacc " +
                "SET status = ?, " +
                "cancel_reason = CASE WHEN ? = 'CANCELLED' THEN ? ELSE cancel_reason END, " +
                "rejection_reason = CASE WHEN ? = 'REJECTED' THEN ? ELSE rejection_reason END, " +
                "cancelled_at = CASE WHEN ? = 'CANCELLED' THEN ? ELSE cancelled_at END, " +
                "rejected_at = CASE WHEN ? = 'REJECTED' THEN ? ELSE rejected_at END " +
                "WHERE id = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, targetStatus);
            ps.setString(2, targetStatus);
            ps.setString(3, trimToNull(cancelReason));
            ps.setString(4, targetStatus);
            ps.setString(5, trimToNull(rejectionReason));
            ps.setString(6, targetStatus);
            ps.setTimestamp(7, STATUS_CANCELLED.equals(targetStatus) ? new Timestamp(System.currentTimeMillis()) : null);
            ps.setString(8, targetStatus);
            ps.setTimestamp(9, STATUS_REJECTED.equals(targetStatus) ? new Timestamp(System.currentTimeMillis()) : null);
            ps.setInt(10, bookingId);

            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Booking status updated to " + targetStatus + ".");
                syncCalendarAfterStatusChangeSafe(bookingId, targetStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error updating booking status:");
            e.printStackTrace();
        }

        return false;
    }

    private void syncCalendarAfterStatusChangeSafe(int bookingId, String targetStatus) {
        try {
            calendarSyncService.syncAfterStatusChange(bookingId, targetStatus);
        } catch (Exception e) {
            // Calendar sync must never block booking status updates.
            System.err.println("⚠ Calendar sync skipped for booking #" + bookingId + ": " + e.getMessage());
        }
    }

    private boolean isAllowedTransition(String currentStatus, String newStatus) {
        if (currentStatus.equals(newStatus)) {
            return true;
        }

        if (STATUS_PENDING.equals(currentStatus)) {
            return STATUS_CONFIRMED.equals(newStatus)
                    || STATUS_REJECTED.equals(newStatus)
                    || STATUS_CANCELLED.equals(newStatus);
        }

        if (STATUS_CONFIRMED.equals(currentStatus)) {
            return STATUS_CANCELLED.equals(newStatus);
        }

        return false;
    }

    private boolean isValidBookingDates(Date checkIn, Date checkOut) {
        if (checkIn == null || checkOut == null) {
            return false;
        }
        return checkIn.before(checkOut);
    }

    private String normalizeStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }

        String normalized = status.trim().toUpperCase();
        if (STATUS_PENDING.equals(normalized)
                || STATUS_CONFIRMED.equals(normalized)
                || STATUS_REJECTED.equals(normalized)
                || STATUS_CANCELLED.equals(normalized)) {
            return normalized;
        }

        return fallback;
    }

    private AccommodationBooking extractAccommodationBookingFromResultSet(ResultSet rs) throws SQLException {
        AccommodationBooking booking = new AccommodationBooking();
        booking.setId(rs.getInt("id"));
        booking.setUserId(rs.getInt("user_id"));
        booking.setRoomId(rs.getInt("room_id"));
        booking.setCheckIn(rs.getDate("check_in"));
        booking.setCheckOut(rs.getDate("check_out"));
        booking.setTotalPrice(rs.getDouble("total_price"));
        booking.setNumberOfGuests(rs.getInt("number_of_guests"));
        booking.setPhoneNumber(rs.getString("phone_number"));
        booking.setSpecialRequests(rs.getString("special_requests"));
        booking.setEstimatedArrivalTime(rs.getString("estimated_arrival_time"));
        booking.setStatus(rs.getString("status"));
        booking.setCancelReason(rs.getString("cancel_reason"));
        booking.setRejectionReason(rs.getString("rejection_reason"));
        booking.setCancelledAt(rs.getTimestamp("cancelled_at"));
        booking.setRejectedAt(rs.getTimestamp("rejected_at"));
        booking.setCreatedAt(rs.getTimestamp("created_at"));
        return booking;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
