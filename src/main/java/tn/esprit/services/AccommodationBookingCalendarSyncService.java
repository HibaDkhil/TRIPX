package tn.esprit.services;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AccommodationBookingCalendarSyncService {

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String SYNC_NOT_SYNCED = "NOT_SYNCED";
    private static final String SYNC_SYNCED = "SYNCED";
    private static final String SYNC_FAILED = "FAILED";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Tunis");

    private final Connection cnx;
    private final AccommodationGoogleCalendarClientService calendarClientService;

    public AccommodationBookingCalendarSyncService() {
        this.cnx = MyDB.getConnection();
        this.calendarClientService = new AccommodationGoogleCalendarClientService();
    }

    public void syncAfterStatusChange(int bookingId, String targetStatus) {
        if (STATUS_CONFIRMED.equalsIgnoreCase(targetStatus)) {
            createEventForConfirmedBooking(bookingId);
            return;
        }

        if (STATUS_CANCELLED.equalsIgnoreCase(targetStatus)) {
            deleteEventForCancelledBooking(bookingId);
        }
    }

    private void createEventForConfirmedBooking(int bookingId) {
        BookingCalendarContext context = loadContext(bookingId);
        if (context == null) {
            return;
        }

        try {
            Calendar service = calendarClientService.getCalendarService();
            Event event = buildEvent(context);
            Event created = service.events().insert("primary", event).execute();

            updateSyncState(bookingId, created.getId(), SYNC_SYNCED, null);
            System.out.println("✅ Google Calendar event created for booking #" + bookingId);
        } catch (Exception e) {
            updateSyncState(bookingId, null, SYNC_FAILED, safeError(e.getMessage()));
            System.err.println("❌ Google Calendar sync failed for booking #" + bookingId + ": " + e.getMessage());
        }
    }

    private void deleteEventForCancelledBooking(int bookingId) {
        String eventId = fetchCalendarEventId(bookingId);
        if (eventId == null || eventId.isBlank()) {
            updateSyncState(bookingId, null, SYNC_NOT_SYNCED, null);
            return;
        }

        try {
            Calendar service = calendarClientService.getCalendarService();
            service.events().delete("primary", eventId).execute();
            updateSyncState(bookingId, null, SYNC_NOT_SYNCED, null);
            System.out.println("✅ Google Calendar event deleted for cancelled booking #" + bookingId);
        } catch (Exception e) {
            updateSyncState(bookingId, eventId, SYNC_FAILED, safeError(e.getMessage()));
            System.err.println("❌ Failed to delete calendar event for booking #" + bookingId + ": " + e.getMessage());
        }
    }

    private Event buildEvent(BookingCalendarContext context) {
        Event event = new Event();
        event.setSummary("TripX Booking #" + context.bookingId + " - " + context.accommodationName);
        event.setDescription(buildDescription(context));

        DateTime startDateTime = toDateTime(context.checkIn, 14, 0);
        DateTime endDateTime = toDateTime(context.checkOut, 11, 0);

        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(BUSINESS_ZONE.getId());
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(BUSINESS_ZONE.getId());

        event.setStart(start);
        event.setEnd(end);
        return event;
    }

    private DateTime toDateTime(Date sqlDate, int hour, int minute) {
        LocalDate localDate = sqlDate.toLocalDate();
        ZonedDateTime zoned = localDate.atTime(hour, minute).atZone(BUSINESS_ZONE);
        return new DateTime(zoned.toInstant().toEpochMilli());
    }

    private String buildDescription(BookingCalendarContext context) {
        StringBuilder description = new StringBuilder();
        description.append("Accommodation: ").append(context.accommodationName).append('\n');
        description.append("Room: ").append(context.roomName).append('\n');
        description.append("Guests: ").append(context.numberOfGuests).append('\n');
        description.append("Phone: ").append(valueOrDash(context.phoneNumber)).append('\n');
        description.append("Special Requests: ").append(valueOrDash(context.specialRequests)).append('\n');
        description.append("Booking Status: ").append(context.status);
        return description.toString();
    }

    private BookingCalendarContext loadContext(int bookingId) {
        String sql = "SELECT b.id, b.check_in, b.check_out, b.number_of_guests, b.phone_number, b.special_requests, b.status, " +
                "r.room_name, a.name AS accommodation_name " +
                "FROM bookingacc b " +
                "JOIN room r ON b.room_id = r.id " +
                "JOIN accommodation a ON r.accommodation_id = a.id " +
                "WHERE b.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                BookingCalendarContext context = new BookingCalendarContext();
                context.bookingId = rs.getInt("id");
                context.checkIn = rs.getDate("check_in");
                context.checkOut = rs.getDate("check_out");
                context.numberOfGuests = rs.getInt("number_of_guests");
                context.phoneNumber = rs.getString("phone_number");
                context.specialRequests = rs.getString("special_requests");
                context.status = rs.getString("status");
                context.roomName = rs.getString("room_name");
                context.accommodationName = rs.getString("accommodation_name");
                return context;
            }
        } catch (SQLException e) {
            System.err.println("❌ Could not load booking context for calendar sync: " + e.getMessage());
            return null;
        }
    }

    private String fetchCalendarEventId(int bookingId) {
        String sql = "SELECT google_calendar_event_id FROM bookingacc WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("google_calendar_event_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠ Calendar columns not ready or unavailable: " + e.getMessage());
        }
        return null;
    }

    private void updateSyncState(int bookingId, String eventId, String syncStatus, String lastError) {
        String sql = "UPDATE bookingacc " +
                "SET google_calendar_event_id = ?, " +
                "calendar_sync_status = ?, " +
                "calendar_last_error = ?, " +
                "calendar_synced_at = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, trimToNull(eventId));
            ps.setString(2, syncStatus);
            ps.setString(3, trimToNull(lastError));
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setInt(5, bookingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠ Unable to persist calendar sync state: " + e.getMessage());
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeError(String message) {
        if (message == null) {
            return "Unknown calendar sync error";
        }
        String compact = message.replace('\n', ' ').replace('\r', ' ').trim();
        return compact.length() > 500 ? compact.substring(0, 500) : compact;
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private static class BookingCalendarContext {
        int bookingId;
        Date checkIn;
        Date checkOut;
        int numberOfGuests;
        String phoneNumber;
        String specialRequests;
        String status;
        String roomName;
        String accommodationName;
    }
}
