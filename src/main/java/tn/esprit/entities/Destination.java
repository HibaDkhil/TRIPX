package tn.esprit.entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class Destination {
    private Long destinationId;  // matches destination_id in DB
    private String name;
    private DestinationType type;  // enum for the type field
    private String country;
    private String city;
    private Season bestSeason;  // enum for best_season
    private String description;
    private String timezone;
    private Double averageRating;  // matches average_rating
    private LocalDateTime createdAt;  // matches created_at
    private boolean isActive;  // we'll add this for UI purposes (not in DB)

    // Enum for destination type (matches your DB enum)
    public enum DestinationType {
        city, beach, mountain, countryside, desert, island, forest, cruise, other;

        @Override
        public String toString() {
            return name().substring(0, 1).toUpperCase() + name().substring(1);
        }
    }

    // Enum for best season (matches your DB enum)
    public enum Season {
        spring, summer, autumn, winter, all_year;

        @Override
        public String toString() {
            if (this == all_year) return "All Year";
            return name().substring(0, 1).toUpperCase() + name().substring(1);
        }
    }

    // Constructors
    public Destination() {
        this.isActive = true;
        this.averageRating = 0.0;
    }

    public Destination(String name, DestinationType type, String country, String city,
                       Season bestSeason, String description) {
        this();
        this.name = name;
        this.type = type;
        this.country = country;
        this.city = city;
        this.bestSeason = bestSeason;
        this.description = description;
    }

    // Getters and Setters
    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DestinationType getType() {
        return type;
    }

    public void setType(DestinationType type) {
        this.type = type;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Season getBestSeason() {
        return bestSeason;
    }

    public void setBestSeason(Season bestSeason) {
        this.bestSeason = bestSeason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Helper methods
    public String getFullLocation() {
        if (city != null && !city.isEmpty()) {
            return city + ", " + country;
        }
        return country;
    }

    @Override
    public String toString() {
        return String.format("Destination{id=%d, name='%s', country='%s', type=%s, rating=%.1f}",
                destinationId, name, country, type, averageRating);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return Objects.equals(destinationId, that.destinationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationId);
    }
}
