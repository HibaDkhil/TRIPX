package tn.esprit.entities;

import java.time.LocalDateTime;

public class Schedule {

    private int scheduleId;
    private int transportId;
    private long departureDestinationId;
    private long arrivalDestinationId;
    private LocalDateTime departureDatetime;
    private LocalDateTime arrivalDatetime;
    private LocalDateTime rentalStart;
    private LocalDateTime rentalEnd;
    private String travelClass; // ECONOMY, PREMIUM, BUSINESS, FIRST
    private double priceMultiplier;
    private String status; // ON_TIME, DELAYED, CANCELLED
    private int delayMinutes;
    private double aiDemandScore;
    private LocalDateTime createdAt;

    // --- 1️⃣ No-argument constructor (for reading from DB) ---
    public Schedule() {
        this.priceMultiplier = 1.0;
        this.status = "ON_TIME";
        this.delayMinutes = 0;
    }

    // --- 2️⃣ Constructor for creation with rental times ---
    public Schedule(int transportId, long departureDestinationId, long arrivalDestinationId,
                    LocalDateTime departureDatetime, LocalDateTime arrivalDatetime,
                    LocalDateTime rentalStart, LocalDateTime rentalEnd,
                    String travelClass) {
        this.transportId = transportId;
        this.departureDestinationId = departureDestinationId;
        this.arrivalDestinationId = arrivalDestinationId;
        this.departureDatetime = departureDatetime;
        this.arrivalDatetime = arrivalDatetime;
        this.rentalStart = rentalStart;
        this.rentalEnd = rentalEnd;
        this.travelClass = travelClass;
        this.priceMultiplier = 1.0;
        this.status = "ON_TIME";
        this.delayMinutes = 0;
    }

    // --- 3️⃣ Constructor without rental times ---
    public Schedule(int transportId, long departureDestinationId, long arrivalDestinationId,
                    LocalDateTime departureDatetime, LocalDateTime arrivalDatetime,
                    String travelClass) {
        this(transportId, departureDestinationId, arrivalDestinationId,
                departureDatetime, arrivalDatetime, null, null, travelClass);
    }

    // --- Getters and Setters ---
    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public int getTransportId() { return transportId; }
    public void setTransportId(int transportId) { this.transportId = transportId; }

    public long getDepartureDestinationId() { return departureDestinationId; }
    public void setDepartureDestinationId(long departureDestinationId) { this.departureDestinationId = departureDestinationId; }

    public long getArrivalDestinationId() { return arrivalDestinationId; }
    public void setArrivalDestinationId(long arrivalDestinationId) { this.arrivalDestinationId = arrivalDestinationId; }

    public LocalDateTime getDepartureDatetime() { return departureDatetime; }
    public void setDepartureDatetime(LocalDateTime departureDatetime) { this.departureDatetime = departureDatetime; }

    public LocalDateTime getArrivalDatetime() { return arrivalDatetime; }
    public void setArrivalDatetime(LocalDateTime arrivalDatetime) { this.arrivalDatetime = arrivalDatetime; }

    public LocalDateTime getRentalStart() { return rentalStart; }
    public void setRentalStart(LocalDateTime rentalStart) { this.rentalStart = rentalStart; }

    public LocalDateTime getRentalEnd() { return rentalEnd; }
    public void setRentalEnd(LocalDateTime rentalEnd) { this.rentalEnd = rentalEnd; }

    public String getTravelClass() { return travelClass; }
    public void setTravelClass(String travelClass) { this.travelClass = travelClass; }

    public double getPriceMultiplier() { return priceMultiplier; }
    public void setPriceMultiplier(double priceMultiplier) { this.priceMultiplier = priceMultiplier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }

    public double getAiDemandScore() { return aiDemandScore; }
    public void setAiDemandScore(double aiDemandScore) { this.aiDemandScore = aiDemandScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Schedule{" +
                "scheduleId=" + scheduleId +
                ", transportId=" + transportId +
                ", departureDestinationId=" + departureDestinationId +
                ", arrivalDestinationId=" + arrivalDestinationId +
                ", departureDatetime=" + departureDatetime +
                ", arrivalDatetime=" + arrivalDatetime +
                ", rentalStart=" + rentalStart +
                ", rentalEnd=" + rentalEnd +
                ", travelClass='" + travelClass + '\'' +
                ", priceMultiplier=" + priceMultiplier +
                ", status='" + status + '\'' +
                ", delayMinutes=" + delayMinutes +
                ", aiDemandScore=" + aiDemandScore +
                ", createdAt=" + createdAt +
                '}';
    }
}
