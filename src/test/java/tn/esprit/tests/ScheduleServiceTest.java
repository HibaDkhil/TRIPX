package tn.esprit.tests;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Schedule;
import tn.esprit.entities.Transport;
import tn.esprit.services.ScheduleService;
import tn.esprit.services.TransportService;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScheduleServiceTest {

    static ScheduleService scheduleService;
    static TransportService transportService;
    static Connection conx;

    static int transportId;
    static long departureId;
    static long arrivalId;
    static int scheduleId;

    @BeforeAll
    static void setup() throws SQLException {

        scheduleService = new ScheduleService();
        transportService = new TransportService();
        conx = MyDatabase.getInstance().getConx();

        // 1️⃣ Create transport
        Transport t = new Transport(
                "FLIGHT",
                "Test Airline",
                "Boeing",
                500,
                100,
                10,
                4.5,
                "WiFi",
                "img.jpg"
        );

        transportService.addTransport(t);
        transportId = transportService.getAllTransports()
                .get(transportService.getAllTransports().size() - 1)
                .getTransportId();

        // 2️⃣ Create departure destination
        String destSql = "INSERT INTO destinations (name, type, country, city, best_season, description, timezone, average_rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conx.prepareStatement(destSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "Test Departure");
            ps.setString(2, "CITY");
            ps.setString(3, "CountryA");
            ps.setString(4, "CityA");
            ps.setString(5, "Summer");
            ps.setString(6, "Desc");
            ps.setString(7, "UTC");
            ps.setDouble(8, 4.5);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) departureId = rs.getLong(1);
        }

        // 3️⃣ Create arrival destination
        try (PreparedStatement ps = conx.prepareStatement(destSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "Test Arrival");
            ps.setString(2, "CITY");
            ps.setString(3, "CountryB");
            ps.setString(4, "CityB");
            ps.setString(5, "Winter");
            ps.setString(6, "Desc");
            ps.setString(7, "UTC");
            ps.setDouble(8, 4.0);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) arrivalId = rs.getLong(1);
        }
    }

    @Test
    @Order(1)
    void addScheduleTest() {

        Schedule s = new Schedule(
                transportId,
                departureId,
                arrivalId,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                "ECONOMY"
        );

        scheduleService.addSchedule(s);

        List<Schedule> list = scheduleService.getAllSchedules();
        assertFalse(list.isEmpty());

        scheduleId = list.get(list.size() - 1).getScheduleId();
    }

    @Test
    @Order(2)
    void updateScheduleTest() {

        Schedule s = scheduleService.getAllSchedules()
                .stream()
                .filter(sc -> sc.getScheduleId() == scheduleId)
                .findFirst()
                .orElse(null);

        assertNotNull(s);

        s.setStatus("DELAYED");
        s.setDelayMinutes(30);
        scheduleService.updateSchedule(s);

        Schedule updated = scheduleService.getAllSchedules()
                .stream()
                .filter(sc -> sc.getScheduleId() == scheduleId)
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertEquals("DELAYED", updated.getStatus());
        assertEquals(30, updated.getDelayMinutes());
    }

    @Test
    @Order(3)
    void deleteScheduleTest() {

        scheduleService.deleteSchedule(scheduleId);

        boolean exists = scheduleService.getAllSchedules()
                .stream()
                .anyMatch(sc -> sc.getScheduleId() == scheduleId);

        assertFalse(exists);
    }

    @AfterAll
    static void cleanup() throws SQLException {

        transportService.deleteTransport(transportId);

        try (PreparedStatement ps = conx.prepareStatement("DELETE FROM destinations WHERE destination_id IN (?, ?)")) {
            ps.setLong(1, departureId);
            ps.setLong(2, arrivalId);
            ps.executeUpdate();
        }
    }
}
