package tn.esprit.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {

    public static void main(String[] args) {
        System.out.println("🔍 Testing database connection...\n");

        // Get database connection
        MyDB db = MyDB.getInstance();
        Connection conn = db.getConx();

        if (conn != null) {
            System.out.println("✅ Connection successful!");

            // Test query: Show all tables in database
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW TABLES");

                System.out.println("\n📋 Tables in database:");
                int count = 0;
                while (rs.next()) {
                    System.out.println("  - " + rs.getString(1));
                    count++;
                }
                System.out.println("\n📊 Total tables: " + count);

                rs.close();
                stmt.close();

            } catch (Exception e) {
                System.err.println("❌ Error querying database: " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            System.err.println("❌ Connection failed!");
        }
    }
}