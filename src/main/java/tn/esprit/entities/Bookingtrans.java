package tn.esprit.entities;

import java.time.LocalDateTime;

public class Bookingtrans {
    private int bookingId;
    private int userId;
    private int transportId;
    private int scheduleId;
    private LocalDateTime bookingDate;
    private int adultsCount;
    private int childrenCount;
    private int totalSeats;
    private double totalPrice;
    private String bookingStatus; // PENDING, CONFIRMED, CANCELLED
    private String paymentStatus; // UNPAID, PAID, REFUNDED
    private boolean insuranceIncluded;
    private String qrCode;
    private String voucherPath;
    private double aiPricePrediction;
    private double comparisonScore;
    private String cancellationReason;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;
    private Double dropoffLatitude;
    private Double dropoffLongitude;
    private String dropoffAddress;

    // Default constructor
    public Bookingtrans() {
        this.bookingStatus = "PENDING";
        this.paymentStatus = "UNPAID";
        this.insuranceIncluded = false;
        this.bookingDate = LocalDateTime.now();
    }

    // Full constructor
    public Bookingtrans(int userId, int transportId, int scheduleId,
                        int totalSeats, double totalPrice) {
        this.userId = userId;
        this.transportId = transportId;
        this.scheduleId = scheduleId;
        this.totalSeats = totalSeats;
        this.totalPrice = totalPrice;
        this.bookingDate = LocalDateTime.now();
        this.bookingStatus = "PENDING";
        this.paymentStatus = "UNPAID";
        this.insuranceIncluded = false;
    }

    // Getters and Setters
    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTransportId() {
        return transportId;
    }

    public void setTransportId(int transportId) {
        this.transportId = transportId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public int getAdultsCount() {
        return adultsCount;
    }

    public void setAdultsCount(int adultsCount) {
        this.adultsCount = adultsCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public boolean isInsuranceIncluded() {
        return insuranceIncluded;
    }

    public void setInsuranceIncluded(boolean insuranceIncluded) {
        this.insuranceIncluded = insuranceIncluded;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getVoucherPath() {
        return voucherPath;
    }

    public void setVoucherPath(String voucherPath) {
        this.voucherPath = voucherPath;
    }

    public double getAiPricePrediction() {
        return aiPricePrediction;
    }

    public void setAiPricePrediction(double aiPricePrediction) {
        this.aiPricePrediction = aiPricePrediction;
    }

    public double getComparisonScore() {
        return comparisonScore;
    }

    public void setComparisonScore(double comparisonScore) {
        this.comparisonScore = comparisonScore;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(Double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(Double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Double getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(Double dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public Double getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(Double dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", userId=" + userId +
                ", transportId=" + transportId +
                ", scheduleId=" + scheduleId +
                ", bookingDate=" + bookingDate +
                ", adultsCount=" + adultsCount +
                ", childrenCount=" + childrenCount +
                ", totalSeats=" + totalSeats +
                ", totalPrice=" + totalPrice +
                ", bookingStatus='" + bookingStatus + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", insuranceIncluded=" + insuranceIncluded +
                ", qrCode='" + qrCode + '\'' +
                ", voucherPath='" + voucherPath + '\'' +
                ", aiPricePrediction=" + aiPricePrediction +
                ", comparisonScore=" + comparisonScore +
                ", cancellationReason='" + cancellationReason + '\'' +
                ", pickupLatitude=" + pickupLatitude +
                ", pickupLongitude=" + pickupLongitude +
                ", pickupAddress='" + pickupAddress + '\'' +
                ", dropoffLatitude=" + dropoffLatitude +
                ", dropoffLongitude=" + dropoffLongitude +
                ", dropoffAddress='" + dropoffAddress + '\'' +
                '}';
    }
}
