package tn.esprit.utils;

public class DefaultCurrentUserProvider implements CurrentUserProvider {

    private static Integer cachedUserId;

    @Override
    public Integer getCurrentUserId() {
        if (cachedUserId != null && cachedUserId > 0) {
            return cachedUserId;
        }

        Integer fromProperty = parsePositiveInt(System.getProperty("tripx.user.id"));
        if (fromProperty != null) {
            cachedUserId = fromProperty;
            return fromProperty;
        }

        Integer fromEnv = parsePositiveInt(System.getenv("TRIPX_USER_ID"));
        if (fromEnv != null) {
            cachedUserId = fromEnv;
            return fromEnv;
        }

        return null;
    }

    public static void setCurrentUserId(Integer userId) {
        if (userId != null && userId > 0) {
            cachedUserId = userId;
        }
    }

    private Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
