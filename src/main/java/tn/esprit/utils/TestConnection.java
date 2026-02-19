package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("🔌 TESTING DATABASE CONNECTION");
        System.out.println("=================================");

        try {
            // Get database connection
            MyDB db = MyDB.getInstance();
            Connection conn = db.getConx();

            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ SUCCESS: Connected to database!");

                // Get database info
                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("📊 Database: " + conn.getCatalog());
                System.out.println("🔧 MySQL Version: " + metaData.getDatabaseProductVersion());
                System.out.println("🔌 Driver: " + metaData.getDriverName());

                // Check if our tables exist
                System.out.println("\n📋 Checking tables...");
                String[] tables = {"destinations", "activities", "bookingdes", "user"};

                for (String table : tables) {
                    try (ResultSet rs = metaData.getTables(null, null, table, null)) {
                        if (rs.next()) {
                            System.out.println("   ✅ Table '" + table + "' exists");
                        } else {
                            System.out.println("   ❌ Table '" + table + "' does NOT exist");
                        }
                    }
                }

                // Test a simple query on destinations
                System.out.println("\n🔍 Testing query on destinations...");
                try (var stmt = conn.createStatement();
                     var rs = stmt.executeQuery("SELECT COUNT(*) as count FROM destinations")) {
                    if (rs.next()) {
                        System.out.println("   📊 Total destinations: " + rs.getInt("count"));
                    }
                } catch (SQLException e) {
                    System.out.println("   ❌ Error querying destinations: " + e.getMessage());
                }

            } else {
                System.out.println("❌ FAILED: Connection is null or closed");
            }

        } catch (SQLException e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=================================");
    }
}