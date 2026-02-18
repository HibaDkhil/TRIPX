package tn.esprit.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Accommodation {
    private int id;
    private String name;
    private String type;
    private String city;
    private String country;
    private String address;
    private String description;
    private int stars;
    private double rating;
    private String imagePath;

    // New fields for admin interface
    private String status;           // Active, Inactive, Pending, Under Maintenance
    private String phone;
    private String email;
    private String website;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 🔥 NEW: Accommodation-level amenities (property-wide features)
    private String accommodationAmenities;  // Stored as comma-separated in database

    // Related entities
    private List<Room> rooms;
    private List<String> amenities;  // For backward compatibility and parsed amenities

    // Empty constructor
    public Accommodation() {
        this.rooms = new ArrayList<>();
        this.amenities = new ArrayList<>();
        this.status = "Active";
    }

    // Constructor without id (for insert)
    public Accommodation(String name, String type, String city, String country,
                         String address, String description, int stars, double rating,
                         String imagePath) {
        this();
        this.name = name;
        this.type = type;
        this.city = city;
        this.country = country;
        this.address = address;
        this.description = description;
        this.stars = stars;
        this.rating = rating;
        this.imagePath = imagePath;
    }

    // Full constructor with all fields
    public Accommodation(int id, String name, String type, String city, String country,
                         String address, String description, int stars, double rating,
                         String imagePath, String status, String phone, String email,
                         String website, String postalCode, Double latitude, Double longitude,
                         String accommodationAmenities) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
        this.city = city;
        this.country = country;
        this.address = address;
        this.description = description;
        this.stars = stars;
        this.rating = rating;
        this.imagePath = imagePath;
        this.status = status;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accommodationAmenities = accommodationAmenities;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    // New getters/setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // 🔥 NEW: Accommodation amenities getter/setter
    public String getAccommodationAmenities() { return accommodationAmenities; }
    public void setAccommodationAmenities(String accommodationAmenities) {
        this.accommodationAmenities = accommodationAmenities;
        // Parse into list for convenience
        parseAmenitiesFromString();
    }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
        // Convert list to comma-separated string
        if (amenities != null && !amenities.isEmpty()) {
            this.accommodationAmenities = String.join(",", amenities);
        }
    }

    // Helper methods
    public int getStarRating() { return stars; }
    public void setStarRating(int stars) { this.stars = stars; }

    public void addRoom(Room room) {
        if (this.rooms == null) {
            this.rooms = new ArrayList<>();
        }
        this.rooms.add(room);
    }

    public void addAmenity(String amenity) {
        if (this.amenities == null) {
            this.amenities = new ArrayList<>();
        }
        this.amenities.add(amenity);
        // Update the string representation
        this.accommodationAmenities = String.join(",", this.amenities);
    }

    // 🔥 Parse amenities from comma-separated string to List
    private void parseAmenitiesFromString() {
        if (this.accommodationAmenities != null && !this.accommodationAmenities.isEmpty()) {
            this.amenities = new ArrayList<>();
            String[] amenitiesArray = this.accommodationAmenities.split(",");
            for (String amenity : amenitiesArray) {
                String trimmed = amenity.trim();
                if (!trimmed.isEmpty()) {
                    this.amenities.add(trimmed);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Accommodation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", status='" + status + '\'' +
                ", stars=" + stars +
                ", rating=" + rating +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", accommodationAmenities='" + accommodationAmenities + '\'' +
                '}';
    }
}