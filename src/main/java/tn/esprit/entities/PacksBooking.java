package tn.esprit.entities;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class PacksBooking {
    
    public enum Status {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }
    
    private int idBooking;
    private int userId;
    private int packId;
    private Timestamp bookingDate;
    private Date travelStartDate;
    private Date travelEndDate;
    private int numTravelers;
    private BigDecimal totalPrice;
    private BigDecimal discountApplied;
    private BigDecimal finalPrice;
    private Status status;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructor for reading from DB (with ID)
    public PacksBooking(int idBooking, int userId, int packId, Timestamp bookingDate,
                        Date travelStartDate, Date travelEndDate, int numTravelers,
                        BigDecimal totalPrice, BigDecimal discountApplied, BigDecimal finalPrice,
                        Status status, String notes, Timestamp createdAt, Timestamp updatedAt) {
        this.idBooking = idBooking;
        this.userId = userId;
        this.packId = packId;
        this.bookingDate = bookingDate;
        this.travelStartDate = travelStartDate;
        this.travelEndDate = travelEndDate;
        this.numTravelers = numTravelers;
        this.totalPrice = totalPrice;
        this.discountApplied = discountApplied;
        this.finalPrice = finalPrice;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Constructor for creating new booking (without ID)
    public PacksBooking(int userId, int packId, Date travelStartDate, Date travelEndDate,
                        int numTravelers, BigDecimal totalPrice, BigDecimal discountApplied,
                        BigDecimal finalPrice, String notes) {
        this.userId = userId;
        this.packId = packId;
        this.travelStartDate = travelStartDate;
        this.travelEndDate = travelEndDate;
        this.numTravelers = numTravelers;
        this.totalPrice = totalPrice;
        this.discountApplied = discountApplied;
        this.finalPrice = finalPrice;
        this.status = Status.PENDING;
        this.notes = notes;
    }

    // Getters and Setters
    public int getIdBooking() {
        return idBooking;
    }

    public void setIdBooking(int idBooking) {
        this.idBooking = idBooking;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getPackId() {
        return packId;
    }

    public void setPackId(int packId) {
        this.packId = packId;
    }

    public Timestamp getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(Timestamp bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Date getTravelStartDate() {
        return travelStartDate;
    }

    public void setTravelStartDate(Date travelStartDate) {
        this.travelStartDate = travelStartDate;
    }

    public Date getTravelEndDate() {
        return travelEndDate;
    }

    public void setTravelEndDate(Date travelEndDate) {
        this.travelEndDate = travelEndDate;
    }

    public int getNumTravelers() {
        return numTravelers;
    }

    public void setNumTravelers(int numTravelers) {
        this.numTravelers = numTravelers;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(BigDecimal discountApplied) {
        this.discountApplied = discountApplied;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "idBooking=" + idBooking +
                ", userId=" + userId +
                ", packId=" + packId +
                ", travelStartDate=" + travelStartDate +
                ", travelEndDate=" + travelEndDate +
                ", numTravelers=" + numTravelers +
                ", finalPrice=" + finalPrice +
                ", status=" + status +
                '}';
    }
}
