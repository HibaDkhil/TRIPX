package tn.esprit.utils;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {
    // IMPORTANT: Make sure this matches your database name (tripx_db)
    private final String URL = "jdbc:mysql://127.0.0.1:3306/tripx_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private final String USERNAME = "root";  // XAMPP default
    private final String PWD = "";           // XAMPP default (empty)

    private static MyDB instance;
    private Connection conx;

    private MyDB() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            System.out.println("🔄 Attempting to connect to database...");
            conx = DriverManager.getConnection(URL, USERNAME, PWD);
            System.out.println("✅ Connected to database successfully!");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found!");
            System.err.println("Make sure mysql-connector-j is in your pom.xml");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            System.err.println("URL: " + URL);
            System.err.println("Username: " + USERNAME);
            System.err.println("Error: " + e.getMessage());

            if (e.getMessage().contains("Unknown database")) {
                System.err.println("\n💡 TIP: Database 'tripx_db' doesn't exist!");
                System.err.println("   Create it in phpMyAdmin first.");
            } else if (e.getMessage().contains("Access denied")) {
                System.err.println("\n💡 TIP: Check your username/password");
                System.err.println("   Default XAMPP: root / (empty password)");
            }
            e.printStackTrace();
        }
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    public Connection getConx() {
        return conx;
    }

    public void closeConnection() {
        try {
            if (conx != null && !conx.isClosed()) {
                conx.close();
                System.out.println("✅ Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing connection: " + e.getMessage());
        }
    }
}