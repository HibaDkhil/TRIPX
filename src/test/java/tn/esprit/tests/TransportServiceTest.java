package tn.esprit.tests;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Transport;
import tn.esprit.services.TransportService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransportServiceTest {

    static TransportService service;
    static Transport transport;
    static int createdTransportId;

    @BeforeAll
    static void setup() {
        service = new TransportService();

        transport = new Transport(
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
    }

    @Test
    @Order(1)
    void addTransportTest() {
        service.addTransport(transport);

        List<Transport> list = service.getAllTransports();
        assertFalse(list.isEmpty());

        createdTransportId = list.get(list.size() - 1).getTransportId();
    }

    @Test
    @Order(2)
    void updateTransportTest() {
        Transport t = service.getAllTransports()
                .stream()
                .filter(tr -> tr.getTransportId() == createdTransportId)
                .findFirst()
                .orElse(null);

        assertNotNull(t);

        t.setBasePrice(800.0);
        service.updateTransport(t);

        Transport updated = service.getAllTransports()
                .stream()
                .filter(tr -> tr.getTransportId() == createdTransportId)
                .findFirst()
                .orElse(null);

        assertEquals(800.0, updated.getBasePrice());
    }

    @Test
    @Order(3)
    void deleteTransportTest() {
        service.deleteTransport(createdTransportId);

        boolean exists = service.getAllTransports()
                .stream()
                .anyMatch(tr -> tr.getTransportId() == createdTransportId);

        assertFalse(exists);
    }
}
