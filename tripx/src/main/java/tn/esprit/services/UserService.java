package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class UserService {

    private Connection conx;

    public UserService() {
        conx = MyDB.getInstance().getConx();
    }

    // ✅ Create new user (SIGNUP)
    public boolean createUser(User user) {
        String sql = "INSERT INTO user (first_name, last_name, email, password) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword()); // plain text for now
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    // ✅ Find user by email (for login)
    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User(
                        rs.getInt("user_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("gender"),
                        rs.getString("birth_year"),
                        rs.getTimestamp("created_at")
                );
                try {
                    u.setPhoneNumber(rs.getString("phone_number"));
                } catch (SQLException e) {
                    // Column might not exist yet
                }
                // Get role
                try {
                    u.setRole(rs.getString("role"));
                } catch (SQLException e) {
                    u.setRole("USER");
                }
                return u;
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return null;
    }

    // ✅ Login check
    public boolean login(String email, String password) {
        User user = findByEmail(email);
        if (user != null && user.getPassword().equals(password)) { // plain text comparison
            return true;
        }
        return false;
    }

    // In UserService.java
    public String getRoleByEmail(String email) {
        String sql = "SELECT role FROM user WHERE email = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            System.err.println("Error getting role: " + e.getMessage());
        }
        return null;
    }

    // Add this method to UserService.java
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY user_id DESC";

        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("gender"),
                        rs.getString("birth_year"),
                        rs.getTimestamp("created_at")
                );
                // Also get role if it exists in your table
                try {
                    user.setRole(rs.getString("role"));
                } catch (SQLException e) {
                    user.setRole("USER"); // Default role if column doesn't exist
                }
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }
        return users;
    }

    // DELETE user
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM user WHERE user_id = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    // UPDATE user
    public boolean updateUser(User user) {
        String sql = "UPDATE user SET first_name = ?, last_name = ?, email = ?, phone_number = ?, gender = ?, birth_year = ? WHERE user_id = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhoneNumber());
            ps.setString(5, user.getGender());
            ps.setString(6, user.getBirthYear());
            ps.setInt(7, user.getUserId());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    // Update User Password
    public boolean updateUserPassword(User user) {
        String sql = "UPDATE user SET password = ? WHERE user_id = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, user.getPassword());
            ps.setInt(2, user.getUserId());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    // Add this method to UserService.java
    public User findById(int userId) {
        String sql = "SELECT * FROM user WHERE user_id = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User(
                        rs.getInt("user_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getString("gender"),
                        rs.getString("birth_year"),
                        rs.getTimestamp("created_at")
                );
                // Get role
                try {
                    u.setRole(rs.getString("role"));
                } catch (SQLException e) {
                    u.setRole("USER");
                }
                return u;
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
        }
        return null;
    }

    // Update User Demographics (Gender, Birth Year)
    public boolean updateUserDemographics(User user) {
        String sql = "UPDATE user SET gender = ?, birth_year = ? WHERE user_id = ?";
        try {
            PreparedStatement ps = conx.prepareStatement(sql);
            ps.setString(1, user.getGender());
            ps.setString(2, user.getBirthYear());
            ps.setInt(3, user.getUserId());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg.contains("Unknown column")) {
                System.err.println("CRITICAL DB ERROR: Missing columns in 'user' table.");
                System.err.println("Please run: ALTER TABLE user ADD COLUMN gender VARCHAR(50), ADD COLUMN birth_year VARCHAR(50);");
            }
            System.err.println("Error updating user demographics: " + msg);
            return false;
        }
    }

}
