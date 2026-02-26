package tn.esprit.entities;

import java.sql.Date;
import java.sql.Timestamp;

public class AccommodationBooking {

    private int id;
    private int userId;
    private int roomId;
    private Date checkIn;
    private Date checkOut;
    private double totalPrice;
    private int numberOfGuests;
    private String phoneNumber;
    private String specialRequests;
    private String estimatedArrivalTime;
    private String status;
    private String cancelReason;
    private String rejectionReason;
    private java.sql.Timestamp cancelledAt;
    private java.sql.Timestamp rejectedAt;
    private Timestamp createdAt;

    // Empty constructor
    public AccommodationBooking() {}

    // Constructor without id (for insert)
    public AccommodationBooking(int userId, int roomId, Date checkIn, Date checkOut, double totalPrice, String status) {
        this.userId = userId;
        this.roomId = roomId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    // Full constructor with id
    public AccommodationBooking(int id, int userId, int roomId, Date checkIn, Date checkOut, double totalPrice, String status, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.roomId = roomId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public Date getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(Date checkIn) {
        this.checkIn = checkIn;
    }

    public Date getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(Date checkOut) {
        this.checkOut = checkOut;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

    public void setEstimatedArrivalTime(String estimatedArrivalTime) {
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public java.sql.Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(java.sql.Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public java.sql.Timestamp getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(java.sql.Timestamp rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AccommodationBooking{" +
                "id=" + id +
                ", userId=" + userId +
                ", roomId=" + roomId +
                ", checkIn=" + checkIn +
                ", checkOut=" + checkOut +
                ", totalPrice=" + totalPrice +
                ", numberOfGuests=" + numberOfGuests +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", estimatedArrivalTime='" + estimatedArrivalTime + '\'' +
                ", status='" + status + '\'' +
                ", cancelReason='" + cancelReason + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", cancelledAt=" + cancelledAt +
                ", rejectedAt=" + rejectedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
