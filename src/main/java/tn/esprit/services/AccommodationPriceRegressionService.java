package tn.esprit.services;

import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * True ML price model (ridge linear regression) trained from confirmed bookings.
 */
public class AccommodationPriceRegressionService {

    private static final int FEATURE_COUNT = 8; // includes intercept
    private static final double RIDGE_LAMBDA = 0.12;
    private static final double DEFAULT_WEEKEND_RATIO = 2.0 / 7.0;

    private final Connection cnx;

    public AccommodationPriceRegressionService() {
        this.cnx = MyDB.getConnection();
    }

    public RegressionSnapshot computeGlobalPriceSnapshot() {
        List<TrainingRow> rows = loadTrainingRows();
        RegressionSnapshot snapshot = new RegressionSnapshot();
        snapshot.sampleSize = rows.size();

        if (rows.size() < 10) {
            snapshot.modelTrained = false;
            snapshot.confidencePercent = 20;
            snapshot.narrative = "Not enough confirmed bookings for regression training yet (need at least 10 samples).";
            return snapshot;
        }

        Standardization standardization = new Standardization(FEATURE_COUNT);
        double[][] x = buildFeatureMatrix(rows, standardization);
        double[] y = buildTargetVector(rows);

        double[] weights = trainRidgeLinearRegression(x, y);
        if (weights == null) {
            snapshot.modelTrained = false;
            snapshot.confidencePercent = 15;
            snapshot.narrative = "Price model training failed due to unstable historical data matrix.";
            return snapshot;
        }

        EvaluationStats eval = evaluateModel(x, y, weights);

        List<PredictionRow> predictionRows = loadCurrentRoomRows();
        if (predictionRows.isEmpty()) {
            snapshot.modelTrained = true;
            snapshot.r2 = eval.r2;
            snapshot.rmse = eval.rmse;
            snapshot.confidencePercent = computeConfidence(eval.r2, rows.size());
            snapshot.narrative = "Regression trained, but no room inventory found for current price suggestion.";
            return snapshot;
        }

        double predictedSum = 0;
        double currentSum = 0;
        int validPredictions = 0;

        for (PredictionRow row : predictionRows) {
            double[] features = buildPredictionFeatures(row);
            double[] standardizedFeatures = standardization.apply(features);
            double predicted = predict(standardizedFeatures, weights);
            if (Double.isFinite(predicted) && predicted > 0) {
                predictedSum += predicted;
                currentSum += row.basePrice;
                validPredictions++;
            }
        }

        if (validPredictions == 0 || currentSum <= 0) {
            snapshot.modelTrained = true;
            snapshot.r2 = eval.r2;
            snapshot.rmse = eval.rmse;
            snapshot.confidencePercent = computeConfidence(eval.r2, rows.size());
            snapshot.narrative = "Regression trained, but prediction rows are not sufficient for a global recommendation.";
            return snapshot;
        }

        snapshot.modelTrained = true;
        snapshot.r2 = eval.r2;
        snapshot.rmse = eval.rmse;
        snapshot.currentAvgNightly = currentSum / validPredictions;
        snapshot.predictedAvgNightly = predictedSum / validPredictions;
        snapshot.suggestedAdjustmentPercent = clamp(
                ((snapshot.predictedAvgNightly - snapshot.currentAvgNightly) / snapshot.currentAvgNightly) * 100.0,
                -15.0,
                15.0
        );
        snapshot.confidencePercent = computeConfidence(eval.r2, rows.size());
        snapshot.narrative = buildNarrative(snapshot);

        return snapshot;
    }

    private List<TrainingRow> loadTrainingRows() {
        List<TrainingRow> list = new ArrayList<>();
        String sql = "SELECT r.price_per_night, r.capacity, a.stars, a.rating, " +
                "DAYOFWEEK(b.check_in) AS day_of_week, MONTH(b.check_in) AS month_num, " +
                "DATEDIFF(b.check_out, b.check_in) AS nights, b.total_price " +
                "FROM bookingacc b " +
                "JOIN room r ON b.room_id = r.id " +
                "JOIN accommodation a ON r.accommodation_id = a.id " +
                "WHERE b.status = 'CONFIRMED' " +
                "AND b.total_price > 0 " +
                "AND r.price_per_night > 0 " +
                "AND DATEDIFF(b.check_out, b.check_in) > 0";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TrainingRow row = new TrainingRow();
                row.basePrice = rs.getDouble("price_per_night");
                row.capacity = rs.getInt("capacity");
                row.stars = rs.getInt("stars");
                row.rating = rs.getDouble("rating");
                int dayOfWeek = rs.getInt("day_of_week");
                row.isWeekend = (dayOfWeek == 1 || dayOfWeek == 7) ? 1.0 : 0.0;
                int month = rs.getInt("month_num");
                row.monthSin = Math.sin(2.0 * Math.PI * month / 12.0);
                row.monthCos = Math.cos(2.0 * Math.PI * month / 12.0);
                int nights = rs.getInt("nights");
                row.targetNightlyPrice = rs.getDouble("total_price") / nights;
                if (Double.isFinite(row.targetNightlyPrice) && row.targetNightlyPrice > 0) {
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<PredictionRow> loadCurrentRoomRows() {
        List<PredictionRow> list = new ArrayList<>();
        String sql = "SELECT r.price_per_night, r.capacity, a.stars, a.rating " +
                "FROM room r JOIN accommodation a ON r.accommodation_id = a.id " +
                "WHERE r.price_per_night > 0 AND r.is_available = 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PredictionRow row = new PredictionRow();
                row.basePrice = rs.getDouble("price_per_night");
                row.capacity = rs.getInt("capacity");
                row.stars = rs.getInt("stars");
                row.rating = rs.getDouble("rating");
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private double[][] buildFeatureMatrix(List<TrainingRow> rows, Standardization standardization) {
        double[][] x = new double[rows.size()][FEATURE_COUNT];
        for (int i = 0; i < rows.size(); i++) {
            TrainingRow row = rows.get(i);
            x[i][0] = 1.0;
            x[i][1] = row.basePrice;
            x[i][2] = row.capacity;
            x[i][3] = row.stars;
            x[i][4] = row.rating;
            x[i][5] = row.isWeekend;
            x[i][6] = row.monthSin;
            x[i][7] = row.monthCos;
        }
        standardization.fit(x);
        return standardization.transform(x);
    }

    private double[] buildTargetVector(List<TrainingRow> rows) {
        double[] y = new double[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            y[i] = rows.get(i).targetNightlyPrice;
        }
        return y;
    }

    private double[] buildPredictionFeatures(PredictionRow row) {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        return new double[]{
                1.0,
                row.basePrice,
                row.capacity,
                row.stars,
                row.rating,
                DEFAULT_WEEKEND_RATIO,
                Math.sin(2.0 * Math.PI * month / 12.0),
                Math.cos(2.0 * Math.PI * month / 12.0)
        };
    }

    private double[] trainRidgeLinearRegression(double[][] x, double[] y) {
        int n = x.length;
        int m = x[0].length;

        double[][] a = new double[m][m];
        double[] b = new double[m];

        for (int row = 0; row < n; row++) {
            for (int i = 0; i < m; i++) {
                b[i] += x[row][i] * y[row];
                for (int j = 0; j < m; j++) {
                    a[i][j] += x[row][i] * x[row][j];
                }
            }
        }

        for (int i = 0; i < m; i++) {
            if (i == 0) {
                a[i][i] += 1e-8; // tiny stabilization for intercept only
            } else {
                a[i][i] += RIDGE_LAMBDA;
            }
        }

        return solveLinearSystem(a, b);
    }

    private EvaluationStats evaluateModel(double[][] x, double[] y, double[] weights) {
        EvaluationStats stats = new EvaluationStats();
        int n = y.length;
        double yMean = 0;
        for (double value : y) {
            yMean += value;
        }
        yMean /= n;

        double sse = 0;
        double sst = 0;
        for (int i = 0; i < n; i++) {
            double pred = predict(x[i], weights);
            double err = y[i] - pred;
            sse += err * err;

            double centered = y[i] - yMean;
            sst += centered * centered;
        }

        stats.rmse = Math.sqrt(sse / n);
        if (sst <= 0) {
            stats.r2 = 0;
        } else {
            stats.r2 = clamp(1.0 - (sse / sst), -1.0, 1.0);
        }
        return stats;
    }

    private double predict(double[] features, double[] weights) {
        double sum = 0;
        for (int i = 0; i < features.length; i++) {
            sum += features[i] * weights[i];
        }
        return sum;
    }

    private double[] solveLinearSystem(double[][] matrix, double[] vector) {
        int n = vector.length;
        double[][] augmented = new double[n][n + 1];

        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, augmented[i], 0, n);
            augmented[i][n] = vector[i];
        }

        for (int pivot = 0; pivot < n; pivot++) {
            int maxRow = pivot;
            for (int row = pivot + 1; row < n; row++) {
                if (Math.abs(augmented[row][pivot]) > Math.abs(augmented[maxRow][pivot])) {
                    maxRow = row;
                }
            }

            if (Math.abs(augmented[maxRow][pivot]) < 1e-12) {
                return null;
            }

            if (maxRow != pivot) {
                double[] temp = augmented[pivot];
                augmented[pivot] = augmented[maxRow];
                augmented[maxRow] = temp;
            }

            double pivotValue = augmented[pivot][pivot];
            for (int col = pivot; col <= n; col++) {
                augmented[pivot][col] /= pivotValue;
            }

            for (int row = 0; row < n; row++) {
                if (row == pivot) {
                    continue;
                }
                double factor = augmented[row][pivot];
                for (int col = pivot; col <= n; col++) {
                    augmented[row][col] -= factor * augmented[pivot][col];
                }
            }
        }

        double[] solution = new double[n];
        for (int i = 0; i < n; i++) {
            solution[i] = augmented[i][n];
        }
        return solution;
    }

    private double computeConfidence(double r2, int sampleSize) {
        double quality = clamp((r2 + 0.2) * 100.0 / 1.2, 0, 100);
        double volume = clamp(sampleSize * 100.0 / 250.0, 0, 100);
        return clamp(quality * 0.65 + volume * 0.35, 10, 95);
    }

    private String buildNarrative(RegressionSnapshot snapshot) {
        String direction;
        if (snapshot.suggestedAdjustmentPercent > 1.0) {
            direction = "increase";
        } else if (snapshot.suggestedAdjustmentPercent < -1.0) {
            direction = "decrease";
        } else {
            direction = "hold";
        }

        return "True ML regression recommends to " + direction + " prices (" +
                formatSigned(snapshot.suggestedAdjustmentPercent) + "%). " +
                "Predicted avg nightly: " + round1(snapshot.predictedAvgNightly) +
                " vs current " + round1(snapshot.currentAvgNightly) +
                " (R2: " + round3(snapshot.r2) + ", samples: " + snapshot.sampleSize + ").";
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

    private static class TrainingRow {
        double basePrice;
        int capacity;
        int stars;
        double rating;
        double isWeekend;
        double monthSin;
        double monthCos;
        double targetNightlyPrice;
    }

    private static class PredictionRow {
        double basePrice;
        int capacity;
        int stars;
        double rating;
    }

    private static class EvaluationStats {
        double r2;
        double rmse;
    }

    private static class Standardization {
        private final double[] mean;
        private final double[] std;

        Standardization(int featureCount) {
            this.mean = new double[featureCount];
            this.std = new double[featureCount];
        }

        void fit(double[][] x) {
            int n = x.length;
            int m = x[0].length;

            for (int j = 1; j < m; j++) {
                double sum = 0;
                for (double[] doubles : x) {
                    sum += doubles[j];
                }
                mean[j] = sum / n;
            }

            for (int j = 1; j < m; j++) {
                double variance = 0;
                for (double[] doubles : x) {
                    double diff = doubles[j] - mean[j];
                    variance += diff * diff;
                }
                std[j] = Math.sqrt(variance / Math.max(1, n - 1));
                if (std[j] < 1e-9) {
                    std[j] = 1.0;
                }
            }
        }

        double[][] transform(double[][] x) {
            int n = x.length;
            int m = x[0].length;
            double[][] out = new double[n][m];
            for (int i = 0; i < n; i++) {
                out[i][0] = x[i][0];
                for (int j = 1; j < m; j++) {
                    out[i][j] = (x[i][j] - mean[j]) / std[j];
                }
            }
            return out;
        }

        double[] apply(double[] x) {
            double[] out = new double[x.length];
            out[0] = x[0];
            for (int j = 1; j < x.length; j++) {
                out[j] = (x[j] - mean[j]) / std[j];
            }
            return out;
        }
    }

    public static class RegressionSnapshot {
        public boolean modelTrained;
        public int sampleSize;
        public double r2;
        public double rmse;
        public double predictedAvgNightly;
        public double currentAvgNightly;
        public double suggestedAdjustmentPercent;
        public double confidencePercent;
        public String narrative;
    }
}
