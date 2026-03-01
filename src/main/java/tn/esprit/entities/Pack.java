package tn.esprit.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Pack {

    public Pack(int idPack, String title, String description, int destinationId, int accommodationId, int activityId, int transportId, int categoryId, int durationDays, BigDecimal basePrice, Status status, LocalDateTime localDateTime) {
        this.idPack = idPack;
        this.title = title;
        this.description = description;
        this.destinationId = (long) destinationId;
        this.accommodationId = accommodationId;
        this.activityId = (long) activityId;
        this.transportId = transportId;
        this.categoryId = categoryId;
        this.durationDays = durationDays;
        this.basePrice = basePrice;
        this.status = status;
    }

    public enum Status {
        ACTIVE, INACTIVE
    }
    
    private int idPack;
    private String title;
    private String description;
    private Long destinationId;        // Changed to Long (bigint in DB)
    private Integer accommodationId;   // Kept as Integer
    private Long activityId;          // Changed to Long (bigint in DB)
    private Integer transportId;       // Kept as Integer
    private Integer categoryId;
    private int durationDays;
    private BigDecimal basePrice;
    private Status status;
    private Timestamp createdAt;

    // Constructor for reading from DB (with ID)
    public Pack(int idPack, String title, String description, Long destinationId,
                Integer accommodationId, Long activityId, Integer transportId,
                Integer categoryId, int durationDays, BigDecimal basePrice,
                Status status, Timestamp createdAt) {
        this.idPack = idPack;
        this.title = title;
        this.description = description;
        this.destinationId = destinationId;
        this.accommodationId = accommodationId;
        this.activityId = activityId;
        this.transportId = transportId;
        this.categoryId = categoryId;
        this.durationDays = durationDays;
        this.basePrice = basePrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Constructor for creating new pack (without ID)
    public Pack(String title, String description, Long destinationId,
                Integer accommodationId, Long activityId, Integer transportId,
                Integer categoryId, int durationDays, BigDecimal basePrice) {
        this.title = title;
        this.description = description;
        this.destinationId = destinationId;
        this.accommodationId = accommodationId;
        this.activityId = activityId;
        this.transportId = transportId;
        this.categoryId = categoryId;
        this.durationDays = durationDays;
        this.basePrice = basePrice;
        this.status = Status.ACTIVE;
    }

    // Getters and Setters
    public int getIdPack() {
        return idPack;
    }

    public void setIdPack(int idPack) {
        this.idPack = idPack;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Integer getAccommodationId() {
        return accommodationId;
    }

    public void setAccommodationId(Integer accommodationId) {
        this.accommodationId = accommodationId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Integer getTransportId() {
        return transportId;
    }

    public void setTransportId(Integer transportId) {
        this.transportId = transportId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Pack{" +
                "idPack=" + idPack +
                ", title='" + title + '\'' +
                ", destinationId=" + destinationId +
                ", basePrice=" + basePrice +
                ", status=" + status +
                '}';
    }
}
