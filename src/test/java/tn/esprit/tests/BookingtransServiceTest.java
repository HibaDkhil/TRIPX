package tn.esprit.tests;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Bookingtrans;
import tn.esprit.entities.Schedule;
import tn.esprit.entities.Transport;
import tn.esprit.services.BookingtransService;
import tn.esprit.services.ScheduleService;
import tn.esprit.services.TransportService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookingtransServiceTest {

    static ScheduleService scheduleService;
    static TransportService transportService;
    static BookingtransService bookingService;

    static Transport testTransport;
    static int createdTransportId;

    static Schedule testSchedule;
    static int createdScheduleId;

    static Bookingtrans testBooking;
    static int createdBookingId;

    @BeforeAll
    static void setup() {

        scheduleService = new ScheduleService();
        transportService = new TransportService();
        bookingService = new BookingtransService();

        // 1️⃣ Create Transport
        testTransport = new Transport(
                "FLIGHT",
                "Test Airline",
                "Boeing 737",
                500.0,
                180,
                10,
                4.5,
                "WiFi, Meal",
                "image.jpg"
        );

        transportService.addTransport(testTransport);

        List<Transport> transports = transportService.getAllTransports();
        createdTransportId = transports.get(transports.size() - 1).getTransportId();

        // 2️⃣ Create Schedule
        testSchedule = new Schedule(
                createdTransportId,
                1,
                2,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                "ECONOMY"
        );

        testSchedule.setRentalStart(LocalDateTime.now().plusDays(1));
        testSchedule.setRentalEnd(LocalDateTime.now().plusDays(1).plusHours(2));
    }

    // ==========================
    // SCHEDULE CRUD (existing)
    // ==========================

    @Test
    @Order(1)
    void addScheduleTest() {

        scheduleService.addSchedule(testSchedule);

        List<Schedule> list = scheduleService.getAllSchedules();
        assertFalse(list.isEmpty());

        createdScheduleId = list.get(list.size() - 1).getScheduleId();
    }

    @Test
    @Order(2)
    void updateScheduleTest() {

        Schedule s = scheduleService.getAllSchedules()
                .stream()
                .filter(sc -> sc.getScheduleId() == createdScheduleId)
                .findFirst()
                .orElse(null);

        assertNotNull(s);

        s.setStatus("DELAYED");
        s.setDelayMinutes(30);
        scheduleService.updateSchedule(s);

        Schedule updated = scheduleService.getAllSchedules()
                .stream()
                .filter(sc -> sc.getScheduleId() == createdScheduleId)
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertEquals("DELAYED", updated.getStatus());
        assertEquals(30, updated.getDelayMinutes());
    }



    // ==========================
    // BOOKING CRUD (NEW PART)
    // ==========================

    @Test
    @Order(3)
    void addBookingTest() {

        testBooking = new Bookingtrans(
                1,                      // user_id
                createdTransportId,     // transport_id
                createdScheduleId,      // schedule_id
                2,                      // seats
                1000.0                  // price
        );

        testBooking.setAdultsCount(2);
        testBooking.setChildrenCount(0);

        bookingService.addBookingtrans(testBooking);

        List<Bookingtrans> list = bookingService.getAllBookings();
        assertFalse(list.isEmpty());

        createdBookingId = list.get(list.size() - 1).getBookingId();
    }

    @Test
    @Order(4)
    void updateBookingTest() {

        Bookingtrans booking = bookingService.getAllBookings()
                .stream()
                .filter(b -> b.getBookingId() == createdBookingId)
                .findFirst()
                .orElse(null);

        assertNotNull(booking);

        booking.setBookingStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");

        bookingService.updateBookingtrans(booking);

        Bookingtrans updated = bookingService.getAllBookings()
                .stream()
                .filter(b -> b.getBookingId() == createdBookingId)
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertEquals("CONFIRMED", updated.getBookingStatus());
        assertEquals("PAID", updated.getPaymentStatus());
    }

    @Test
    @Order(5)
    void deleteBookingTest() {

        bookingService.deleteBookingtrans(createdBookingId);

        Bookingtrans deleted = bookingService.getAllBookings()
                .stream()
                .filter(b -> b.getBookingId() == createdBookingId)
                .findFirst()
                .orElse(null);

        assertNull(deleted);
    }
    @Test
    @Order(6)
    void deleteScheduleTest() {

        scheduleService.deleteSchedule(createdScheduleId);

        Schedule deleted = scheduleService.getAllSchedules()
                .stream()
                .filter(sc -> sc.getScheduleId() == createdScheduleId)
                .findFirst()
                .orElse(null);

        assertNull(deleted);
    }
    @AfterAll
    static void cleanup() {

        transportService.deleteTransport(createdTransportId);
    }
}
