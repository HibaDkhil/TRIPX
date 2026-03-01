package tn.esprit.entities;

import java.time.LocalDateTime;

public class LoyaltyPoints {

    public enum Level { BRONZE, SILVER, GOLD }

    private int id;
    private int userId;
    private int totalPoints;
    private Level level;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- Constructor with id (reading from DB) ----
    public LoyaltyPoints(int id, int userId, int totalPoints, Level level,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id          = id;
        this.userId      = userId;
        this.totalPoints = totalPoints;
        this.level       = level;
        this.createdAt   = createdAt;
        this.updatedAt   = updatedAt;
    }

    // ---- Constructor without id (inserting) ----
    public LoyaltyPoints(int userId) {
        this.userId      = userId;
        this.totalPoints = 0;
        this.level       = Level.BRONZE;
    }

    // ---- Computed: recalculate level from total points ----
    // 0-199 = BRONZE, 200-399 = SILVER, 400+ = GOLD
    public Level computeLevel() {
        if (totalPoints >= 400) return Level.GOLD;
        if (totalPoints >= 200) return Level.SILVER;
        return Level.BRONZE;
    }

    // ---- Computed: loyalty discount % based on level ----
    public double getLoyaltyDiscountPercent() {
        switch (computeLevel()) {
            case GOLD:   return 15.0;
            case SILVER: return 9.0;
            default:     return 4.0; // BRONZE
        }
    }

    // ---- Getters & Setters ----
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public int getUserId()                      { return userId; }
    public void setUserId(int userId)           { this.userId = userId; }

    public int getTotalPoints()                 { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public Level getLevel()                     { return level; }
    public void setLevel(Level level)           { this.level = level; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void setCreatedAt(LocalDateTime t)   { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)   { this.updatedAt = t; }

    @Override
    public String toString() {
        return "LoyaltyPoints{" +
                "id=" + id +
                ", userId=" + userId +
                ", totalPoints=" + totalPoints +
                ", level=" + computeLevel() +
                ", discount=" + getLoyaltyDiscountPercent() + "%" +
                '}';
    }
}
