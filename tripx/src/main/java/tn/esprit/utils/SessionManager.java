package tn.esprit.utils;

public class SessionManager {
    // Default to -1 (none)
    private static int currentUserId = -1;

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }
}
