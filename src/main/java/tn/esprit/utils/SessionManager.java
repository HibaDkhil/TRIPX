package tn.esprit.utils;

public class SessionManager {
    // For now, we simulate a logged-in user with ID 1
    // In a real app, this would be set after login
    private static int currentUserId = 1;

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }
}
