package tn.esprit.entities;

public class Room {

    private int id;
    private int accommodationId;
    private String roomName;         // New field
    private String roomType;
    private double pricePerNight;
    private int capacity;
    private double size;             // New field - size in m²
    private String amenities;
    private boolean isAvailable;

    // Empty constructor
    public Room() {}

    // Constructor without id (for insert)
    public Room(int accommodationId, String roomName, String roomType,
                double pricePerNight, int capacity, double size,
                String amenities, boolean isAvailable) {
        this.accommodationId = accommodationId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.size = size;
        this.amenities = amenities;
        this.isAvailable = isAvailable;
    }

    // Full constructor with id
    public Room(int id, int accommodationId, String roomName, String roomType,
                double pricePerNight, int capacity, double size,
                String amenities, boolean isAvailable) {
        this.id = id;
        this.accommodationId = accommodationId;
        this.roomName = roomName;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.size = size;
        this.amenities = amenities;
        this.isAvailable = isAvailable;
    }

    // Backward compatibility constructor (without room_name and size)
    public Room(int accommodationId, String roomType,
                double pricePerNight, int capacity,
                String amenities, boolean isAvailable) {
        this.accommodationId = accommodationId;
        this.roomName = roomType + " Room";  // Auto-generate name
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.size = 0.0;  // Default size
        this.amenities = amenities;
        this.isAvailable = isAvailable;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAccommodationId() { return accommodationId; }
    public void setAccommodationId(int accommodationId) { this.accommodationId = accommodationId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public double getSize() { return size; }
    public void setSize(double size) { this.size = size; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    @Override
    public String toString() {
        return "Room{" +
                "id=" + id +
                ", accommodationId=" + accommodationId +
                ", roomName='" + roomName + '\'' +
                ", roomType='" + roomType + '\'' +
                ", pricePerNight=" + pricePerNight +
                ", capacity=" + capacity +
                ", size=" + size + "m²" +
                ", isAvailable=" + isAvailable +
                '}';
    }
    // Add these methods to your existing Room class:

    // Alias for getRoomType() - to fix line 255 error
    public String getType() {
        return this.roomType;
    }

    // Alias for getRoomName() - to fix line 255 error
    public String getName() {
        return this.roomName;
    }

    // Add getBedType() method - to fix line 287 error
    public String getBedType() {
        // You can implement this based on your room type or amenities
        if (this.amenities != null && this.amenities.toLowerCase().contains("king")) {
            return "King";
        } else if (this.amenities != null && this.amenities.toLowerCase().contains("queen")) {
            return "Queen";
        } else if (this.amenities != null && this.amenities.toLowerCase().contains("double")) {
            return "Double";
        } else if (this.capacity >= 2) {
            return "Double";
        } else {
            return "Single";
        }
    }

    // Add getDescription() method - to fix lines 300, 301 error
    public String getDescription() {
        if (this.amenities != null && !this.amenities.isEmpty()) {
            return amenities;
        }
        return this.roomType + " room with " + this.capacity + " person capacity";
    }
}