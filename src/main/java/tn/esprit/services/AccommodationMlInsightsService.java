package tn.esprit.services;

import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccommodationMlInsightsService {

    private final Connection cnx;
    private final AccommodationPriceRegressionService priceRegressionService;

    public AccommodationMlInsightsService() {
        this.cnx = MyDB.getConnection();
        this.priceRegressionService = new AccommodationPriceRegressionService();
    }

    public MlInsightSnapshot computeGlobalSnapshot() {
        MlInsightSnapshot snapshot = new MlInsightSnapshot();

        int totalRooms = getTotalRoomsCount();
        if (totalRooms <= 0) {
            snapshot.forecastOccupancyPercent = 0;
            snapshot.suggestedPriceAdjustmentPercent = 0;
            snapshot.modelConfidencePercent = 20;
            snapshot.decisionSummary = "No rooms found. Add room inventory to start ML forecasting.";
            return snapshot;
        }

        double occupancyLast30 = getHistoricOccupancyPercent(30, 0, totalRooms);
        double occupancyPrev30 = getHistoricOccupancyPercent(30, 30, totalRooms);

        int confirmedLast14 = getConfirmedBookingsCount(14, 0);
        int confirmedPrev14 = getConfirmedBookingsCount(14, 14);
        double bookingMomentum = computeMomentum(confirmedLast14, confirmedPrev14);

        double forecastOccupancy = clamp(
                occupancyLast30 * 0.65 + occupancyPrev30 * 0.25 + (bookingMomentum * 100.0) * 0.10,
                0,
                100
        );

        AccommodationPriceRegressionService.RegressionSnapshot regression = priceRegressionService.computeGlobalPriceSnapshot();
        double priceAdjustment = regression.modelTrained
                ? regression.suggestedAdjustmentPercent
                : clamp(((forecastOccupancy / 100.0) - 0.55) * 35.0, -12.0, 12.0);

        int confirmedLast90 = getConfirmedBookingsCount(90, 0);
        double fallbackConfidence = clamp(30 + (confirmedLast90 * 65.0 / 120.0), 30, 92);
        double confidence = regression.modelTrained ? regression.confidencePercent : fallbackConfidence;

        snapshot.forecastOccupancyPercent = forecastOccupancy;
        snapshot.suggestedPriceAdjustmentPercent = priceAdjustment;
        snapshot.modelConfidencePercent = confidence;
        snapshot.decisionSummary = buildDecisionSummary(
                forecastOccupancy,
                priceAdjustment,
                bookingMomentum,
                confidence,
                regression
        );
        return snapshot;
    }

    private int getTotalRoomsCount() {
        String sql = "SELECT COUNT(*) FROM room";
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getHistoricOccupancyPercent(int windowDays, int offsetDays, int totalRooms) {
        if (totalRooms <= 0 || windowDays <= 0) {
            return 0;
        }

        String sql = "SELECT COALESCE(SUM(DATEDIFF(LEAST(check_out, DATE_SUB(CURDATE(), INTERVAL ? DAY)), " +
                "GREATEST(check_in, DATE_SUB(CURDATE(), INTERVAL ? DAY)))), 0) AS booked_nights " +
                "FROM bookingacc " +
                "WHERE status = 'CONFIRMED' " +
                "AND check_in < DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "AND check_out > DATE_SUB(CURDATE(), INTERVAL ? DAY)";

        int endOffset = offsetDays;
        int startOffset = offsetDays + windowDays;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, endOffset);
            ps.setInt(2, startOffset);
            ps.setInt(3, endOffset);
            ps.setInt(4, startOffset);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double bookedNights = rs.getDouble("booked_nights");
                    double capacityNights = totalRooms * (double) windowDays;
                    if (capacityNights <= 0) {
                        return 0;
                    }
                    return clamp((bookedNights * 100.0) / capacityNights, 0, 100);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getConfirmedBookingsCount(int windowDays, int offsetDays) {
        String sql = "SELECT COUNT(*) FROM bookingacc " +
                "WHERE status = 'CONFIRMED' " +
                "AND created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "AND created_at < DATE_SUB(CURDATE(), INTERVAL ? DAY)";
        int startOffset = offsetDays + windowDays;
        int endOffset = offsetDays;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, startOffset);
            ps.setInt(2, endOffset);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getCancellationRatePercent() {
        String sql = "SELECT COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_count, " +
                "COUNT(*) AS total_count FROM bookingacc";
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

    private double computeMomentum(int recent, int previous) {
        if (recent <= 0 && previous <= 0) {
            return 0.5;
        }
        if (previous <= 0) {
            return 0.8;
        }
        double growth = (recent - previous) / (double) previous;
        return clamp(0.5 + growth * 0.3, 0, 1);
    }

    private String buildDecisionSummary(double forecastOccupancy,
                                        double adjustment,
                                        double momentum,
                                        double confidence,
                                        AccommodationPriceRegressionService.RegressionSnapshot regression) {
        String direction;
        if (adjustment > 1) {
            direction = "increase";
        } else if (adjustment < -1) {
            direction = "decrease";
        } else {
            direction = "hold";
        }
        if (regression != null && regression.modelTrained) {
            return "True ML regression suggests to " + direction +
                    " prices (" + formatSigned(adjustment) + "%). " +
                    "Forecast occupancy: " + round1(forecastOccupancy) + "%, " +
                    "R2: " + round3(regression.r2) + ", samples: " + regression.sampleSize +
                    ", confidence: " + round1(confidence) + "%.";
        }

        return "ML fallback suggests to " + direction +
                " prices (" + formatSigned(adjustment) + "%) until enough confirmed bookings exist. " +
                "Forecast occupancy: " + round1(forecastOccupancy) + "%, " +
                "demand momentum score: " + round1(momentum * 100.0) + "%, " +
                "confidence: " + round1(confidence) + "%.";
    }

    private String formatSigned(double value) {
        if (value >= 0) {
            return "+" + round1(value);
        }
        return String.valueOf(round1(value));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class MlInsightSnapshot {
        public double forecastOccupancyPercent;
        public double suggestedPriceAdjustmentPercent;
        public double modelConfidencePercent;
        public String decisionSummary;
    }
}
