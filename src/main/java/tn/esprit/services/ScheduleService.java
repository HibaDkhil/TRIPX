package tn.esprit.services;

import tn.esprit.entities.Schedule;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService {

    private Connection conx;

    public ScheduleService() {
        conx = MyDB.getInstance().getConx();
    }

    // ── helper: null-safe Timestamp setter ──
    private void setNullableTimestamp(PreparedStatement ps, int idx, LocalDateTime val) throws SQLException {
        if (val != null) ps.setTimestamp(idx, Timestamp.valueOf(val));
        else             ps.setNull(idx, Types.TIMESTAMP);
    }

    // Create
    public void addSchedule(Schedule s) {
        String sql = "INSERT INTO schedule (transport_id, departure_destination_id, arrival_destination_id, " +
                "departure_datetime, arrival_datetime, rental_start, rental_end, " +
                "travel_class, price_multiplier, status, delay_minutes, ai_demand_score) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, s.getTransportId());
            ps.setLong(2, s.getDepartureDestinationId());
            ps.setLong(3, s.getArrivalDestinationId());
            setNullableTimestamp(ps, 4, s.getDepartureDatetime());   // FIX: null-safe
            setNullableTimestamp(ps, 5, s.getArrivalDatetime());     // FIX: null-safe
            setNullableTimestamp(ps, 6, s.getRentalStart());
            setNullableTimestamp(ps, 7, s.getRentalEnd());
            ps.setString(8, s.getTravelClass());
            ps.setDouble(9, s.getPriceMultiplier());
            ps.setString(10, s.getStatus());
            ps.setInt(11, s.getDelayMinutes());
            ps.setDouble(12, s.getAiDemandScore());
            ps.executeUpdate();
            System.out.println("Schedule added successfully!");
        } catch (SQLException e) {
            System.err.println("[addSchedule ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Read all
    public List<Schedule> getAllSchedules() {
        List<Schedule> list = new ArrayList<>();
        String sql = "SELECT * FROM schedule";
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Schedule s = new Schedule();
                s.setScheduleId(rs.getInt("schedule_id"));
                s.setTransportId(rs.getInt("transport_id"));
                s.setDepartureDestinationId(rs.getLong("departure_destination_id"));
                s.setArrivalDestinationId(rs.getLong("arrival_destination_id"));

                // FIX: null-safe reads — vehicle schedules have null departure/arrival
                Timestamp depDt = rs.getTimestamp("departure_datetime");
                Timestamp arrDt = rs.getTimestamp("arrival_datetime");
                Timestamp rsStart = rs.getTimestamp("rental_start");
                Timestamp rsEnd   = rs.getTimestamp("rental_end");

                s.setDepartureDatetime(depDt   != null ? depDt.toLocalDateTime()   : null);
                s.setArrivalDatetime  (arrDt   != null ? arrDt.toLocalDateTime()   : null);
                s.setRentalStart      (rsStart != null ? rsStart.toLocalDateTime() : null);
                s.setRentalEnd        (rsEnd   != null ? rsEnd.toLocalDateTime()   : null);

                s.setTravelClass(rs.getString("travel_class"));
                s.setPriceMultiplier(rs.getDouble("price_multiplier"));
                s.setStatus(rs.getString("status"));
                s.setDelayMinutes(rs.getInt("delay_minutes"));
                s.setAiDemandScore(rs.getDouble("ai_demand_score"));
                list.add(s);
            }
        } catch (SQLException e) {
            System.err.println("[getAllSchedules ERROR] " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // Update
    public void updateSchedule(Schedule s) {
        String sql = "UPDATE schedule SET transport_id=?, departure_destination_id=?, arrival_destination_id=?, " +
                "departure_datetime=?, arrival_datetime=?, rental_start=?, rental_end=?, " +
                "travel_class=?, price_multiplier=?, status=?, delay_minutes=?, ai_demand_score=? " +
                "WHERE schedule_id=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, s.getTransportId());
            ps.setLong(2, s.getDepartureDestinationId());
            ps.setLong(3, s.getArrivalDestinationId());
            setNullableTimestamp(ps, 4, s.getDepartureDatetime());   // FIX: null-safe
            setNullableTimestamp(ps, 5, s.getArrivalDatetime());     // FIX: null-safe
            setNullableTimestamp(ps, 6, s.getRentalStart());
            setNullableTimestamp(ps, 7, s.getRentalEnd());
            ps.setString(8, s.getTravelClass());
            ps.setDouble(9, s.getPriceMultiplier());
            ps.setString(10, s.getStatus());
            ps.setInt(11, s.getDelayMinutes());
            ps.setDouble(12, s.getAiDemandScore());
            ps.setInt(13, s.getScheduleId());
            ps.executeUpdate();
            System.out.println("Schedule updated successfully!");
        } catch (SQLException e) {
            System.err.println("[updateSchedule ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Delete
    public void deleteSchedule(int id) {
        String sql = "DELETE FROM schedule WHERE schedule_id=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Schedule deleted successfully!");
        } catch (SQLException e) {
            System.err.println("[deleteSchedule ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }
}