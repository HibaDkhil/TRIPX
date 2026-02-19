package tn.esprit.utils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Validation {

    private static final List<String> VALID_CLASSES =
            Arrays.asList("ECONOMY", "PREMIUM", "BUSINESS", "FIRST");

    private static final List<String> VALID_BOOKING_STATUS =
            Arrays.asList("PENDING", "CONFIRMED", "CANCELLED");

    private static final List<String> VALID_PAYMENT_STATUS =
            Arrays.asList("UNPAID", "PAID", "REFUNDED");

    // 1️⃣ Positive number validation
    public static boolean isPositive(double value) {
        return value > 0;
    }

    // 2️⃣ Seats validation
    public static boolean isValidSeats(int seats) {
        return seats > 0;
    }

    // 3️⃣ Date validation
    public static boolean isFutureDate(LocalDateTime date) {
        return date != null && date.isAfter(LocalDateTime.now());
    }

    // 4️⃣ Return after departure
    public static boolean isValidReturnDate(LocalDateTime departure, LocalDateTime arrival) {
        return departure != null && arrival != null && arrival.isAfter(departure);
    }

    // 5️⃣ Travel class validation
    public static boolean isValidTravelClass(String travelClass) {
        return VALID_CLASSES.contains(travelClass);
    }

    // 6️⃣ Booking status validation
    public static boolean isValidBookingStatus(String status) {
        return VALID_BOOKING_STATUS.contains(status);
    }

    // 7️⃣ Payment status validation
    public static boolean isValidPaymentStatus(String status) {
        return VALID_PAYMENT_STATUS.contains(status);
    }
}
