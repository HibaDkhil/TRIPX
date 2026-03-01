package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Offer {
    
    public enum DiscountType {
        PERCENTAGE, FIXED
    }
    
    private int idOffer;
    private String title;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer packId;
    private Long destinationId;      // Long for bigint
    private Integer accommodationId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActive;

    // Constructor for reading from DB (with ID) - THE CORRECT ONE
    public Offer(int idOffer, String title, String description, DiscountType discountType,
                 BigDecimal discountValue, Integer packId, Long destinationId,
                 Integer accommodationId, LocalDate startDate, LocalDate endDate, boolean isActive) {
        this.idOffer = idOffer;
        this.title = title;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.packId = packId;
        this.destinationId = destinationId;  // Direct assignment, no conversion needed
        this.accommodationId = accommodationId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    // Constructor for creating new offer (without ID)
    public Offer(String title, String description, DiscountType discountType,
                 BigDecimal discountValue, Integer packId, Long destinationId,
                 Integer accommodationId, LocalDate startDate, LocalDate endDate) {
        this.title = title;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.packId = packId;
        this.destinationId = destinationId;
        this.accommodationId = accommodationId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
    }

    // Getters and Setters
    public int getIdOffer() {
        return idOffer;
    }

    public void setIdOffer(int idOffer) {
        this.idOffer = idOffer;
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

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public Integer getPackId() {
        return packId;
    }

    public void setPackId(Integer packId) {
        this.packId = packId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Offer{" +
                "idOffer=" + idOffer +
                ", title='" + title + '\'' +
                ", discountType=" + discountType +
                ", discountValue=" + discountValue +
                ", isActive=" + isActive +
                '}';
    }
}
