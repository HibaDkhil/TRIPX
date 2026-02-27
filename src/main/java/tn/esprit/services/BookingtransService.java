package tn.esprit.services;

import tn.esprit.entities.Bookingtrans;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingtransService {

    private Connection conx;

    public BookingtransService() {
        conx = MyDB.getInstance().getConx();
        ensurePickupDropoffColumns();
    }

    private void ensurePickupDropoffColumns() {
        String[] ddl = new String[]{
                "ALTER TABLE bookingtrans ADD COLUMN IF NOT EXISTS pickup_latitude DOUBLE NULL",
                "ALTER TABLE bookingtrans ADD COLUMN IF NOT EXISTS pickup_longitude DOUBLE NULL",
                "ALTER TABLE bookingtrans ADD COLUMN IF NOT EXISTS pickup_address VARCHAR(255) NULL",
                "ALTER TABLE bookingtrans ADD COLUMN IF NOT EXISTS dropoff_latitude DOUBLE NULL",
                "ALTER TABLE bookingtrans ADD COLUMN IF NOT EXISTS dropoff_longitude DOUBLE NULL",
                "ALTER TABLE bookingtrans ADD COLUMN IF NOT EXISTS dropoff_address VARCHAR(255) NULL"
        };
        try (Statement st = conx.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            System.out.println("Schema migration warning: " + e.getMessage());
        }
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) ps.setNull(index, Types.DOUBLE);
        else ps.setDouble(index, value);
    }

    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) return null;
        return ((Number) value).doubleValue();
    }

    // Create
    public String addBookingtrans(Bookingtrans b) {
        String sql = "INSERT INTO bookingtrans (user_id, transport_id, schedule_id, booking_date, adults_count, children_count, total_seats, total_price, booking_status, payment_status, insurance_included, qr_code, voucher_path, ai_price_prediction, comparison_score, cancellation_reason, pickup_latitude, pickup_longitude, pickup_address, dropoff_latitude, dropoff_longitude, dropoff_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, b.getUserId());
            ps.setInt(2, b.getTransportId());
            if (b.getScheduleId() == 0) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, b.getScheduleId());
            ps.setTimestamp(4, Timestamp.valueOf(b.getBookingDate()));
            ps.setInt(5, b.getAdultsCount());
            ps.setInt(6, b.getChildrenCount());
            ps.setInt(7, b.getTotalSeats());
            ps.setDouble(8, b.getTotalPrice());
            ps.setString(9, b.getBookingStatus());
            ps.setString(10, b.getPaymentStatus());
            ps.setBoolean(11, b.isInsuranceIncluded());
            ps.setString(12, b.getQrCode());
            ps.setString(13, b.getVoucherPath());
            ps.setDouble(14, b.getAiPricePrediction());
            ps.setDouble(15, b.getComparisonScore());
            ps.setString(16, b.getCancellationReason());
            setNullableDouble(ps, 17, b.getPickupLatitude());
            setNullableDouble(ps, 18, b.getPickupLongitude());
            ps.setString(19, b.getPickupAddress());
            setNullableDouble(ps, 20, b.getDropoffLatitude());
            setNullableDouble(ps, 21, b.getDropoffLongitude());
            ps.setString(22, b.getDropoffAddress());
            ps.executeUpdate();
            System.out.println("Booking added successfully!");
        } catch (SQLException e) {
            return e.getMessage(); // return the actual error
        }
        return sql;
    }

    // Read all
    public List<Bookingtrans> getAllBookings() {
        List<Bookingtrans> list = new ArrayList<>();
        String sql = "SELECT * FROM bookingtrans";
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Bookingtrans b = new Bookingtrans();
                b.setBookingId(rs.getInt("booking_id"));
                b.setUserId(rs.getInt("user_id"));
                b.setTransportId(rs.getInt("transport_id"));
                b.setScheduleId(rs.getInt("schedule_id"));
                b.setBookingDate(rs.getTimestamp("booking_date").toLocalDateTime());
                b.setAdultsCount(rs.getInt("adults_count"));
                b.setChildrenCount(rs.getInt("children_count"));
                b.setTotalSeats(rs.getInt("total_seats"));
                b.setTotalPrice(rs.getDouble("total_price"));
                b.setBookingStatus(rs.getString("booking_status"));
                b.setPaymentStatus(rs.getString("payment_status"));
                b.setInsuranceIncluded(rs.getBoolean("insurance_included"));
                b.setQrCode(rs.getString("qr_code"));
                b.setVoucherPath(rs.getString("voucher_path"));
                b.setAiPricePrediction(rs.getDouble("ai_price_prediction"));
                b.setComparisonScore(rs.getDouble("comparison_score"));
                b.setCancellationReason(rs.getString("cancellation_reason"));
                b.setPickupLatitude(getNullableDouble(rs, "pickup_latitude"));
                b.setPickupLongitude(getNullableDouble(rs, "pickup_longitude"));
                b.setPickupAddress(rs.getString("pickup_address"));
                b.setDropoffLatitude(getNullableDouble(rs, "dropoff_latitude"));
                b.setDropoffLongitude(getNullableDouble(rs, "dropoff_longitude"));
                b.setDropoffAddress(rs.getString("dropoff_address"));
                list.add(b);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    // Update
    public void updateBookingtrans(Bookingtrans b) {
        String sql = "UPDATE bookingtrans SET user_id=?, transport_id=?, schedule_id=?, booking_date=?, adults_count=?, children_count=?, total_seats=?, total_price=?, booking_status=?, payment_status=?, insurance_included=?, qr_code=?, voucher_path=?, ai_price_prediction=?, comparison_score=?, cancellation_reason=?, pickup_latitude=?, pickup_longitude=?, pickup_address=?, dropoff_latitude=?, dropoff_longitude=?, dropoff_address=? WHERE booking_id=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, b.getUserId());
            ps.setInt(2, b.getTransportId());
            if (b.getScheduleId() == 0) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, b.getScheduleId());
            ps.setTimestamp(4, Timestamp.valueOf(b.getBookingDate()));
            ps.setInt(5, b.getAdultsCount());
            ps.setInt(6, b.getChildrenCount());
            ps.setInt(7, b.getTotalSeats());
            ps.setDouble(8, b.getTotalPrice());
            ps.setString(9, b.getBookingStatus());
            ps.setString(10, b.getPaymentStatus());
            ps.setBoolean(11, b.isInsuranceIncluded());
            ps.setString(12, b.getQrCode());
            ps.setString(13, b.getVoucherPath());
            ps.setDouble(14, b.getAiPricePrediction());
            ps.setDouble(15, b.getComparisonScore());
            ps.setString(16, b.getCancellationReason());
            setNullableDouble(ps, 17, b.getPickupLatitude());
            setNullableDouble(ps, 18, b.getPickupLongitude());
            ps.setString(19, b.getPickupAddress());
            setNullableDouble(ps, 20, b.getDropoffLatitude());
            setNullableDouble(ps, 21, b.getDropoffLongitude());
            ps.setString(22, b.getDropoffAddress());
            ps.setInt(23, b.getBookingId());
            ps.executeUpdate();
            System.out.println("Booking updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Delete
    public void deleteBookingtrans(int id) {
        String sql = "DELETE FROM bookingtrans WHERE booking_id=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Booking deleted successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public List<Bookingtrans> getBookingsByUserId(int userId) {
        List<Bookingtrans> list = new ArrayList<>();
        String sql = "SELECT * FROM bookingtrans WHERE user_id = ?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Bookingtrans b = new Bookingtrans();
                b.setBookingId(rs.getInt("booking_id"));
                b.setUserId(rs.getInt("user_id"));
                b.setTransportId(rs.getInt("transport_id"));
                b.setScheduleId(rs.getInt("schedule_id"));           // 0 if NULL in DB
                b.setAdultsCount(rs.getInt("adults_count"));
                b.setChildrenCount(rs.getInt("children_count"));
                b.setTotalSeats(rs.getInt("total_seats"));
                b.setTotalPrice(rs.getDouble("total_price"));
                b.setBookingStatus(rs.getString("booking_status"));
                b.setPaymentStatus(rs.getString("payment_status"));
                b.setInsuranceIncluded(rs.getBoolean("insurance_included"));
                b.setCancellationReason(rs.getString("cancellation_reason"));
                b.setQrCode(rs.getString("qr_code"));
                b.setVoucherPath(rs.getString("voucher_path"));
                b.setAiPricePrediction(rs.getDouble("ai_price_prediction"));
                b.setComparisonScore(rs.getDouble("comparison_score"));
                b.setPickupLatitude(getNullableDouble(rs, "pickup_latitude"));
                b.setPickupLongitude(getNullableDouble(rs, "pickup_longitude"));
                b.setPickupAddress(rs.getString("pickup_address"));
                b.setDropoffLatitude(getNullableDouble(rs, "dropoff_latitude"));
                b.setDropoffLongitude(getNullableDouble(rs, "dropoff_longitude"));
                b.setDropoffAddress(rs.getString("dropoff_address"));

                Timestamp bookingDate = rs.getTimestamp("booking_date");
                if (bookingDate != null)
                    b.setBookingDate(bookingDate.toLocalDateTime());

                list.add(b);
            }
        } catch (SQLException e) {
            System.out.println("getBookingsByUserId error: " + e.getMessage());
        }
        return list;
    }

}
