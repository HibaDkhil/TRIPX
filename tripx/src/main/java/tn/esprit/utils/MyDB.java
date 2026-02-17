package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {
    // Database connection details - à modifier si nécessaire
    private final String URL = "jdbc:mysql://127.0.0.1:3306/tripx_db";  // Nom de ta base
    private final String USERNAME = "root";  // Ton username XAMPP
    private final String PWD = "";           // Ton mot de passe XAMPP (vide par défaut)

    // Singleton instance
    private static MyDB instance;
    private Connection conx;

    // Private constructor (Singleton pattern)
    private MyDB() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            conx = DriverManager.getConnection(URL, USERNAME, PWD);
            System.out.println("✅ Connected to database successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get singleton instance
    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        }
        return instance;
    }

    // Get database connection
    public Connection getConx() {
        return conx;
    }

    // Close connection (optional, for cleanup)
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