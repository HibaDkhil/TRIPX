package tn.esprit.services;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class AccommodationDashboardExportService {

    private final AccommodationDashboardAnalyticsService analyticsService;
    private final AccommodationMlInsightsService mlInsightsService;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public AccommodationDashboardExportService(AccommodationDashboardAnalyticsService analyticsService,
                                               AccommodationMlInsightsService mlInsightsService) {
        this.analyticsService = analyticsService;
        this.mlInsightsService = mlInsightsService;
    }

    public void exportDashboardExcel(Path targetPath) throws IOException {
        AccommodationDashboardAnalyticsService.DashboardKpis kpis = analyticsService.getDashboardKpis();
        AccommodationMlInsightsService.MlInsightSnapshot mlSnapshot = mlInsightsService.computeGlobalSnapshot();
        Map<String, Integer> bookingTrend = analyticsService.getBookingsTrendLast6Months();
        List<AccommodationDashboardAnalyticsService.RevenueByType> revenueByType = analyticsService.getRevenueByAccommodationType();
        List<String> highlights = analyticsService.getInsightHighlights();

        try (Workbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = (XSSFSheet) workbook.createSheet("Dashboard Statistics");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle generatedAtStyle = createGeneratedAtStyle(workbook);
            CellStyle sectionHeaderStyle = createSectionHeaderStyle(workbook);
            CellStyle tableHeaderStyle = createTableHeaderStyle(workbook);
            CellStyle metricNameStyle = createMetricNameStyle(workbook);
            CellStyle metricValueStyle = createMetricValueStyle(workbook);
            CellStyle wrapValueStyle = createWrapValueStyle(workbook);

            int rowIdx = 0;
            rowIdx = addMergedTitleRow(sheet, rowIdx, "TripX Accommodation Dashboard Export", titleStyle);
            rowIdx = addMergedTitleRow(
                    sheet,
                    rowIdx,
                    "Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    generatedAtStyle
            );
            rowIdx++;

            rowIdx = addSectionHeader(sheet, rowIdx, "Core Metrics", sectionHeaderStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Total Accommodations", String.valueOf(kpis.totalAccommodations), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Active Bookings", String.valueOf(kpis.activeBookings), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Monthly Revenue", formatMoney(kpis.confirmedRevenue), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Average Booking Value", formatMoney(kpis.averageBookingValue), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Cancellation Rate", formatPercent(kpis.cancellationRatePercent), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Top City", safeText(kpis.topCity), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Top Accommodation Type", safeText(kpis.topType), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Forecast Occupancy (Next 30 days)", formatPercent(mlSnapshot.forecastOccupancyPercent), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Suggested Price Action", formatSignedPercent(mlSnapshot.suggestedPriceAdjustmentPercent), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Model Confidence", formatPercent(mlSnapshot.modelConfidencePercent), metricNameStyle, metricValueStyle);
            rowIdx = addMetricRow(sheet, rowIdx, "Model Decision Summary", safeText(mlSnapshot.decisionSummary), metricNameStyle, wrapValueStyle);
            rowIdx++;

            rowIdx = addSectionHeader(sheet, rowIdx, "Bookings Trend (Last 6 Months)", sectionHeaderStyle);
            rowIdx = addTableHeader(sheet, rowIdx, "Month", "Bookings", tableHeaderStyle);
            for (Map.Entry<String, Integer> entry : bookingTrend.entrySet()) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, entry.getKey(), metricValueStyle);
                createCell(row, 1, String.valueOf(entry.getValue()), metricValueStyle);
            }
            rowIdx++;

            rowIdx = addSectionHeader(sheet, rowIdx, "Revenue By Accommodation Type", sectionHeaderStyle);
            rowIdx = addTableHeader(sheet, rowIdx, "Accommodation Type", "Revenue", tableHeaderStyle);
            for (AccommodationDashboardAnalyticsService.RevenueByType item : revenueByType) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, item.type, metricValueStyle);
                createCell(row, 1, formatMoney(item.revenue), metricValueStyle);
            }
            rowIdx++;

            rowIdx = addSectionHeader(sheet, rowIdx, "Insight Highlights", sectionHeaderStyle);
            rowIdx = addTableHeader(sheet, rowIdx, "No.", "Insight", tableHeaderStyle);
            for (int i = 0; i < highlights.size(); i++) {
                Row row = sheet.createRow(rowIdx++);
                createCell(row, 0, String.valueOf(i + 1), metricValueStyle);
                createCell(row, 1, highlights.get(i), wrapValueStyle);
            }

            sheet.setColumnWidth(0, 9000);
            sheet.setColumnWidth(1, 18000);
            sheet.setColumnWidth(2, 1000);

            try (var outputStream = java.nio.file.Files.newOutputStream(targetPath)) {
                workbook.write(outputStream);
            }
        }
    }

    private String formatMoney(double value) {
        return "EUR " + decimalFormat.format(value);
    }

    private String formatPercent(double value) {
        return decimalFormat.format(value) + "%";
    }

    private String formatSignedPercent(double value) {
        return (value >= 0 ? "+" : "") + decimalFormat.format(value) + "%";
    }

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

    private int addMergedTitleRow(XSSFSheet sheet, int rowIdx, String text, CellStyle style) {
        Row row = sheet.createRow(rowIdx++);
        row.setHeightInPoints(24);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 1));
        return rowIdx;
    }

    private int addSectionHeader(XSSFSheet sheet, int rowIdx, String title, CellStyle style) {
        Row row = sheet.createRow(rowIdx++);
        row.setHeightInPoints(20);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 1));
        return rowIdx;
    }

    private int addTableHeader(XSSFSheet sheet, int rowIdx, String col1, String col2, CellStyle style) {
        Row row = sheet.createRow(rowIdx++);
        createCell(row, 0, col1, style);
        createCell(row, 1, col2, style);
        return rowIdx;
    }

    private int addMetricRow(XSSFSheet sheet, int rowIdx, String metric, String value, CellStyle metricStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIdx++);
        createCell(row, 0, metric, metricStyle);
        createCell(row, 1, value, valueStyle);
        return rowIdx;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createGeneratedAtStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createSectionHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createTableHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createMetricNameStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createMetricValueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createWrapValueStyle(Workbook workbook) {
        CellStyle style = createMetricValueStyle(workbook);
        style.setWrapText(true);
        return style;
    }
}
