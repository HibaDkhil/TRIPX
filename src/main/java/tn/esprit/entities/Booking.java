package tn.esprit.entities;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Booking {

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }

    public enum PaymentStatus {
        UNPAID, PAID, REFUNDED
    }

    private long bookingId;
    private String bookingReference;
    private int userId;
    private long destinationId;
    private Long activityId; // Nullable
    private Timestamp startAt; // Using Timestamp for DB compatibility
    private Timestamp endAt;
    private int numGuests;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private double totalAmount;
    private String currency;
    private String notes;
    private Timestamp createdAt;

    // Transient fields for display purposes (not in DB table directly, joined in Service)
    private String destinationName;
    private String activityName;

    public Booking() {
        this.status = BookingStatus.PENDING;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.currency = "USD";
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public Booking(String bookingReference, int userId, long destinationId, Timestamp startAt, int numGuests, double totalAmount) {
        this();
        this.bookingReference = bookingReference;
        this.userId = userId;
        this.destinationId = destinationId;
        this.startAt = startAt;
        this.numGuests = numGuests;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public long getDestinationId() { return destinationId; }
    public void setDestinationId(long destinationId) { this.destinationId = destinationId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Timestamp getStartAt() { return startAt; }
    public void setStartAt(Timestamp startAt) { this.startAt = startAt; }

    public Timestamp getEndAt() { return endAt; }
    public void setEndAt(Timestamp endAt) { this.endAt = endAt; }

    public int getNumGuests() { return numGuests; }
    public void setNumGuests(int numGuests) { this.numGuests = numGuests; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
}
