package tn.esprit.entities;

import java.time.LocalDateTime;

public class Transport {
    private int transportId;
    private String transportType; // FLIGHT or VEHICLE
    private String providerName;
    private String vehicleModel;
    private double basePrice;
    private int capacity;
    private int availableUnits;
    private double sustainabilityRating;
    private String amenities;
    private String imageUrl;
    private double dynamicPriceFactor;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public Transport() {
        this.dynamicPriceFactor = 1.0;
        this.isActive = true;
    }

    // Full constructor
    public Transport(String transportType, String providerName, String vehicleModel,
                     double basePrice, int capacity, int availableUnits,
                     double sustainabilityRating, String amenities, String imageUrl) {
        this.transportType = transportType;
        this.providerName = providerName;
        this.vehicleModel = vehicleModel;
        this.basePrice = basePrice;
        this.capacity = capacity;
        this.availableUnits = availableUnits;
        this.sustainabilityRating = sustainabilityRating;
        this.amenities = amenities;
        this.imageUrl = imageUrl;
        this.dynamicPriceFactor = 1.0;
        this.isActive = true;
    }

    // Getters and Setters
    public int getTransportId() {
        return transportId;
    }

    public void setTransportId(int transportId) {
        this.transportId = transportId;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(int availableUnits) {
        this.availableUnits = availableUnits;
    }

    public double getSustainabilityRating() {
        return sustainabilityRating;
    }

    public void setSustainabilityRating(double sustainabilityRating) {
        this.sustainabilityRating = sustainabilityRating;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getDynamicPriceFactor() {
        return dynamicPriceFactor;
    }

    public void setDynamicPriceFactor(double dynamicPriceFactor) {
        this.dynamicPriceFactor = dynamicPriceFactor;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public boolean getIsActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return "Transport{" +
                "transportId=" + transportId +
                ", transportType='" + transportType + '\'' +
                ", providerName='" + providerName + '\'' +
                ", vehicleModel='" + vehicleModel + '\'' +
                ", basePrice=" + basePrice +
                ", capacity=" + capacity +
                ", availableUnits=" + availableUnits +
                ", sustainabilityRating=" + sustainabilityRating +
                ", amenities='" + amenities + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", dynamicPriceFactor=" + dynamicPriceFactor +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
