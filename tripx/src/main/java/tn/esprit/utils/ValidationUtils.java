package tn.esprit.utils;

import java.util.regex.Pattern;

/**
 * Utility class for input validation across the application
 */
public class ValidationUtils {
    
    // Regex patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "^[A-Za-z\\s'-]+$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[0-9+\\s()-]+$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    );
    
    /**
     * Validates that a field is not empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validates name fields (letters, spaces, hyphens, apostrophes only)
     */
    public static boolean isValidName(String name) {
        if (!isNotEmpty(name)) {
            return false;
        }
        return NAME_PATTERN.matcher(name.trim()).matches();
    }
    
    /**
     * Validates email format
     */
    public static boolean isValidEmail(String email) {
        if (!isNotEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validates phone number (optional field)
     * Returns true if empty or valid format
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Phone is optional
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }
    
    /**
     * Validates password strength
     * Must be at least 8 characters with uppercase, lowercase, and number
     */
    public static boolean isValidPassword(String password) {
        if (!isNotEmpty(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * Validates that two passwords match
     */
    public static boolean passwordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }
    
    /**
     * Get validation error message for name
     */
    public static String getNameError(String fieldName) {
        return fieldName + " must contain only letters, spaces, hyphens, or apostrophes";
    }
    
    /**
     * Get validation error message for email
     */
    public static String getEmailError() {
        return "Please enter a valid email address (e.g., user@example.com)";
    }
    
    /**
     * Get validation error message for phone
     */
    public static String getPhoneError() {
        return "Phone number must contain only numbers, +, -, (, ), or spaces";
    }
    
    /**
     * Get validation error message for password
     */
    public static String getPasswordError() {
        return "Password must be at least 8 characters with uppercase, lowercase, and number";
    }
    
    /**
     * Get validation error message for password mismatch
     */
    public static String getPasswordMismatchError() {
        return "Passwords do not match";
    }
    
    /**
     * Get validation error message for required field
     */
    public static String getRequiredFieldError(String fieldName) {
        return fieldName + " is required";
    }
}
