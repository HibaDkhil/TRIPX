package tn.esprit.services;

import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class AccommodationDashboardAnalyticsService {

    private final Connection cnx;

    public AccommodationDashboardAnalyticsService() {
        this.cnx = MyDB.getConnection();
    }

    public DashboardKpis getDashboardKpis() {
        DashboardKpis kpis = new DashboardKpis();
        kpis.totalAccommodations = getTotalAccommodationsCount();
        kpis.activeBookings = getActiveBookingsCount();
        kpis.confirmedRevenue = getConfirmedRevenue();
        kpis.occupancyRatePercent = getCurrentOccupancyRatePercent();
        kpis.averageBookingValue = getAverageBookingValue();
        kpis.cancellationRatePercent = getCancellationRatePercent();
        kpis.topCity = getTopCityByConfirmedBookings();
        kpis.topType = getTopTypeByConfirmedBookings();
        return kpis;
    }

    public List<String> getInsightHighlights() {
        List<String> highlights = new ArrayList<>();
        highlights.add(buildTopCityInsight());
        highlights.add(buildTopTypeInsight());
        highlights.add(buildDemandPressureInsight());
        return highlights;
    }

    public LinkedHashMap<String, Integer> getBookingsTrendLast6Months() {
        LinkedHashMap<String, Integer> trend = initLast6MonthMap();
        String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, COUNT(*) AS total " +
                "FROM bookingacc " +
                "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 5 MONTH) " +
                "GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
                "ORDER BY ym";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String ym = rs.getString("ym");
                int count = rs.getInt("total");
                YearMonth yearMonth = YearMonth.parse(ym);
                String key = yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                if (trend.containsKey(key)) {
                    trend.put(key, count);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trend;
    }

    public List<RevenueByType> getRevenueByAccommodationType() {
        List<RevenueByType> list = new ArrayList<>();
        String sql = "SELECT LOWER(COALESCE(a.type, 'Unknown')) AS type_name, COALESCE(SUM(b.total_price), 0) AS revenue " +
                "FROM bookingacc b " +
                "JOIN room r ON b.room_id = r.id " +
                "JOIN accommodation a ON r.accommodation_id = a.id " +
                "WHERE b.status = 'CONFIRMED' " +
                "GROUP BY LOWER(COALESCE(a.type, 'Unknown')) " +
                "ORDER BY revenue DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("type_name");
                double revenue = rs.getDouble("revenue");
                list.add(new RevenueByType(capitalize(type), revenue));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (list.isEmpty()) {
            list.add(new RevenueByType("No Confirmed Revenue", 1.0));
        }

        return list;
    }

    private int getTotalAccommodationsCount() {
        String sql = "SELECT COUNT(*) FROM accommodation";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getActiveBookingsCount() {
        String sql = "SELECT COUNT(*) FROM bookingacc WHERE status IN ('PENDING', 'CONFIRMED')";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getConfirmedRevenue() {
        String sql = "SELECT COALESCE(SUM(total_price), 0) FROM bookingacc WHERE status = 'CONFIRMED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getCurrentOccupancyRatePercent() {
        int totalRooms = getTotalRoomsCount();
        if (totalRooms == 0) {
            return 0;
        }

        int occupiedRoomsToday = getOccupiedRoomsTodayCount();
        return (occupiedRoomsToday * 100.0) / totalRooms;
    }

    private double getAverageBookingValue() {
        String sql = "SELECT COALESCE(AVG(total_price), 0) FROM bookingacc WHERE status = 'CONFIRMED'";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getCancellationRatePercent() {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_count, " +
                "COUNT(*) AS total_count " +
                "FROM bookingacc";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int cancelled = rs.getInt("cancelled_count");
                int total = rs.getInt("total_count");
                if (total == 0) {
                    return 0;
                }
                return (cancelled * 100.0) / total;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getTopCityByConfirmedBookings() {
        String sql = "SELECT COALESCE(a.city, 'Unknown') AS city_name, COUNT(*) AS total " +
                "FROM bookingacc b " +
                "JOIN room r ON b.room_id = r.id " +
                "JOIN accommodation a ON r.accommodation_id = a.id " +
                "WHERE b.status = 'CONFIRMED' " +
                "GROUP BY COALESCE(a.city, 'Unknown') " +
                "ORDER BY total DESC " +
                "LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("city_name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    private String getTopTypeByConfirmedBookings() {
        String sql = "SELECT COALESCE(a.type, 'Unknown') AS type_name, COUNT(*) AS total " +
                "FROM bookingacc b " +
                "JOIN room r ON b.room_id = r.id " +
                "JOIN accommodation a ON r.accommodation_id = a.id " +
                "WHERE b.status = 'CONFIRMED' " +
                "GROUP BY COALESCE(a.type, 'Unknown') " +
                "ORDER BY total DESC " +
                "LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("type_name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "N/A";
    }

    private int getTotalRoomsCount() {
        String sql = "SELECT COUNT(*) FROM room";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getOccupiedRoomsTodayCount() {
        String sql = "SELECT COUNT(DISTINCT room_id) " +
                "FROM bookingacc " +
                "WHERE status = 'CONFIRMED' " +
                "AND CURDATE() >= check_in " +
                "AND CURDATE() < check_out";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private LinkedHashMap<String, Integer> initLast6MonthMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String key = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            map.put(key, 0);
        }
        return map;
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "Unknown";
        }
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
    }

    private String buildTopCityInsight() {
        String city = getTopCityByConfirmedBookings();
        if ("N/A".equals(city)) {
            return "No confirmed bookings yet to identify demand by city.";
        }
        return "Highest confirmed-demand city right now: " + city + ".";
    }

    private String buildTopTypeInsight() {
        String type = getTopTypeByConfirmedBookings();
        if ("N/A".equals(type)) {
            return "Accommodation type trend will appear after first confirmed bookings.";
        }
        return "Most booked accommodation type: " + type + ".";
    }

    private String buildDemandPressureInsight() {
        double occupancy = getCurrentOccupancyRatePercent();
        if (occupancy >= 80) {
            return "High demand pressure: occupancy is above 80%, consider a smart price increase window.";
        }
        if (occupancy >= 60) {
            return "Healthy demand: occupancy is between 60% and 80%, keep balanced pricing.";
        }
        return "Demand is soft: occupancy is below 60%, promotions can improve conversion.";
    }

    public static class DashboardKpis {
        public int totalAccommodations;
        public int activeBookings;
        public double confirmedRevenue;
        public double occupancyRatePercent;
        public double averageBookingValue;
        public double cancellationRatePercent;
        public String topCity;
        public String topType;
    }

    public static class RevenueByType {
        public final String type;
        public final double revenue;

        public RevenueByType(String type, double revenue) {
            this.type = type;
            this.revenue = revenue;
        }
    }
}
