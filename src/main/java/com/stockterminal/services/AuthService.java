package com.stockterminal.services;

import com.stockterminal.db.DatabaseConfig;
import com.stockterminal.models.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    public User register(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        //prepare statements for parsing the params into the query and also prevent the sql injection
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt= conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return login(username, password);
        } catch (SQLException e) {
            System.out.println("Registration failed. Username might already exist.");
            return null;
        }
    }

    public User login(String username, String password) {
        String sql = "SELECT id, username, bank_balance FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getDouble("bank_balance")
                );
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
        return null;
    }

    public double getBalance(int userId) {
        String sql = "SELECT bank_balance FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("bank_balance");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching balance: " + e.getMessage());
        }
        return 0.0;
    }
}
