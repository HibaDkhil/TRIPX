package tn.esprit.tests;

import org.junit.jupiter.api.Test;
import tn.esprit.utils.Validation;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationTest {

    // 1️⃣ Test positive numbers
    @Test
    void testPositiveValue() {
        assertTrue(Validation.isPositive(100));
        assertFalse(Validation.isPositive(-50));
        assertFalse(Validation.isPositive(0));
    }

    // 2️⃣ Test seats
    @Test
    void testValidSeats() {
        assertTrue(Validation.isValidSeats(3));
        assertFalse(Validation.isValidSeats(0));
        assertFalse(Validation.isValidSeats(-1));
    }

    // 3️⃣ Test future date
    @Test
    void testFutureDate() {
        LocalDateTime future = LocalDateTime.now().plusDays(2);
        LocalDateTime past = LocalDateTime.now().minusDays(2);

        assertTrue(Validation.isFutureDate(future));
        assertFalse(Validation.isFutureDate(past));
        assertFalse(Validation.isFutureDate(null));
    }

    // 4️⃣ Test return date
    @Test
    void testReturnDate() {
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        LocalDateTime arrival = LocalDateTime.now().plusDays(2);

        assertTrue(Validation.isValidReturnDate(departure, arrival));
        assertFalse(Validation.isValidReturnDate(arrival, departure));
        assertFalse(Validation.isValidReturnDate(null, arrival));
    }

    // 5️⃣ Test travel class
    @Test
    void testTravelClass() {
        assertTrue(Validation.isValidTravelClass("ECONOMY"));
        assertTrue(Validation.isValidTravelClass("BUSINESS"));
        assertFalse(Validation.isValidTravelClass("VIP"));
    }

    // 6️⃣ Test booking status
    @Test
    void testBookingStatus() {
        assertTrue(Validation.isValidBookingStatus("PENDING"));
        assertFalse(Validation.isValidBookingStatus("DONE"));
    }

    // 7️⃣ Test payment status
    @Test
    void testPaymentStatus() {
        assertTrue(Validation.isValidPaymentStatus("PAID"));
        assertFalse(Validation.isValidPaymentStatus("WAITING"));
    }
}
