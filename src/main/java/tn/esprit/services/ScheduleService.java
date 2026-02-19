package tn.esprit.services;

import tn.esprit.entities.Schedule;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleService {

    private Connection conx;

    public ScheduleService() {
        conx = MyDatabase.getInstance().getConx();
    }

    // Create
    public void addSchedule(Schedule s) {
        String sql = "INSERT INTO schedule (transport_id, departure_destination_id, arrival_destination_id, departure_datetime, arrival_datetime, rental_start, rental_end, travel_class, price_multiplier, status, delay_minutes, ai_demand_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, s.getTransportId());
            ps.setLong(2, s.getDepartureDestinationId());
            ps.setLong(3, s.getArrivalDestinationId());
            ps.setTimestamp(4, Timestamp.valueOf(s.getDepartureDatetime()));
            ps.setTimestamp(5, Timestamp.valueOf(s.getArrivalDatetime()));
            ps.setTimestamp(6, s.getRentalStart() != null ? Timestamp.valueOf(s.getRentalStart()) : null);
            ps.setTimestamp(7, s.getRentalEnd()   != null ? Timestamp.valueOf(s.getRentalEnd())   : null);
            ps.setString(8, s.getTravelClass());
            ps.setDouble(9, s.getPriceMultiplier());
            ps.setString(10, s.getStatus());
            ps.setInt(11, s.getDelayMinutes());
            ps.setDouble(12, s.getAiDemandScore());
            ps.executeUpdate();
            System.out.println("Schedule added successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
                s.setDepartureDatetime(rs.getTimestamp("departure_datetime").toLocalDateTime());
                s.setArrivalDatetime(rs.getTimestamp("arrival_datetime").toLocalDateTime());
                Timestamp rsStart = rs.getTimestamp("rental_start");
                Timestamp rsEnd   = rs.getTimestamp("rental_end");
                s.setRentalStart(rsStart != null ? rsStart.toLocalDateTime() : null);
                s.setRentalEnd  (rsEnd   != null ? rsEnd.toLocalDateTime()   : null);
                s.setTravelClass(rs.getString("travel_class"));
                s.setPriceMultiplier(rs.getDouble("price_multiplier"));
                s.setStatus(rs.getString("status"));
                s.setDelayMinutes(rs.getInt("delay_minutes"));
                s.setAiDemandScore(rs.getDouble("ai_demand_score"));
                list.add(s);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    // Update
    public void updateSchedule(Schedule s) {
        String sql = "UPDATE schedule SET transport_id=?, departure_destination_id=?, arrival_destination_id=?, departure_datetime=?, arrival_datetime=?, rental_start=?, rental_end=?, travel_class=?, price_multiplier=?, status=?, delay_minutes=?, ai_demand_score=? WHERE schedule_id=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, s.getTransportId());
            ps.setLong(2, s.getDepartureDestinationId());
            ps.setLong(3, s.getArrivalDestinationId());
            ps.setTimestamp(4, Timestamp.valueOf(s.getDepartureDatetime()));
            ps.setTimestamp(5, Timestamp.valueOf(s.getArrivalDatetime()));
            ps.setTimestamp(6, s.getRentalStart() != null ? Timestamp.valueOf(s.getRentalStart()) : null);
            ps.setTimestamp(7, s.getRentalEnd()   != null ? Timestamp.valueOf(s.getRentalEnd())   : null);
            ps.setString(8, s.getTravelClass());
            ps.setDouble(9, s.getPriceMultiplier());
            ps.setString(10, s.getStatus());
            ps.setInt(11, s.getDelayMinutes());
            ps.setDouble(12, s.getAiDemandScore());
            ps.setInt(13, s.getScheduleId());
            ps.executeUpdate();
            System.out.println("Schedule updated successfully!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
        }
    }
}
