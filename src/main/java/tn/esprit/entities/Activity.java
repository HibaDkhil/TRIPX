package tn.esprit.entities;

public class Activity {
    private Long activityId;
    private Long destinationId;
    private String destinationName; // For display purposes
    private String name;
    private String description;
    private Double price;
    private int capacity;
    private ActivityCategory category;

    public enum ActivityCategory {
        Adventure, Relax, Culture, Food, Nightlife, Sports, Other
    }

    public Activity() {}

    public Activity(Long destinationId, String name, String description, Double price, int capacity, ActivityCategory category) {
        this.destinationId = destinationId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.capacity = capacity;
        this.category = category;
    }

    // Getters and Setters
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public ActivityCategory getCategory() { return category; }
    public void setCategory(ActivityCategory category) { this.category = category; }

    @Override
    public String toString() {
        return name + " (" + category + ")";
    }
}
