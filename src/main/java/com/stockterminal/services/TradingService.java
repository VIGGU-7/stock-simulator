package com.stockterminal.services;

import com.stockterminal.db.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TradingService {
    private final StockApiService stockApiService = new StockApiService();
    
    public void buyStock(int userId, String symbol, int quantity) {
        symbol = symbol.toUpperCase();
        double currentPrice = stockApiService.getLivePrice(symbol);
        
        if (currentPrice <= 0) {
            return; // Error already printed in API service
        }
        
        double totalCost = currentPrice * quantity;
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // start transaction
            
            // Check balance
            String balanceQuery = "SELECT bank_balance FROM users WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(balanceQuery)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    double balance = rs.getDouble("bank_balance");
                    if (balance < totalCost) {
                        System.out.println("Insufficient funds! Required: $" + totalCost + ", Available: $" + balance);
                        conn.rollback();
                        return;
                    }
                }
            }
            
            // Deduct balance
            String updateBalance = "UPDATE users SET bank_balance = bank_balance - ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateBalance)) {
                pstmt.setDouble(1, totalCost);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();
            }
            
            // Update Portfolio
            String checkPortfolio = "SELECT quantity, average_buy_price FROM portfolio WHERE user_id = ? AND stock_symbol = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkPortfolio)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, symbol);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    // Update existing
                    int existingQty = rs.getInt("quantity");
                    double avgPrice = rs.getDouble("average_buy_price");
                    int newQty = existingQty + quantity;
                    double newAvgPrice = ((existingQty * avgPrice) + (quantity * currentPrice)) / newQty;
                    
                    String updatePort = "UPDATE portfolio SET quantity = ?, average_buy_price = ? WHERE user_id = ? AND stock_symbol = ?";
                    try (PreparedStatement pstmtPort = conn.prepareStatement(updatePort)) {
                        pstmtPort.setInt(1, newQty);
                        pstmtPort.setDouble(2, newAvgPrice);
                        pstmtPort.setInt(3, userId);
                        pstmtPort.setString(4, symbol);
                        pstmtPort.executeUpdate();
                    }
                } else {
                    // Insert new
                    String insertPort = "INSERT INTO portfolio (user_id, stock_symbol, quantity, average_buy_price) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmtPort = conn.prepareStatement(insertPort)) {
                        pstmtPort.setInt(1, userId);
                        pstmtPort.setString(2, symbol);
                        pstmtPort.setInt(3, quantity);
                        pstmtPort.setDouble(4, currentPrice);
                        pstmtPort.executeUpdate();
                    }
                }
            }
            
            // Log transaction
            String insertTrans = "INSERT INTO transactions (user_id, stock_symbol, type, quantity, price) VALUES (?, ?, 'BUY', ?, ?)";
            try (PreparedStatement pstmtTrans = conn.prepareStatement(insertTrans)) {
                pstmtTrans.setInt(1, userId);
                pstmtTrans.setString(2, symbol);
                pstmtTrans.setInt(3, quantity);
                pstmtTrans.setDouble(4, currentPrice);
                pstmtTrans.executeUpdate();
            }
            
            conn.commit();
            System.out.printf("Successfully bought %d shares of %s at $%.2f%n", quantity, symbol, currentPrice);
            
        } catch (SQLException e) {
            System.out.println("Error processing buy transaction: " + e.getMessage());
        }
    }
    
    public void sellStock(int userId, String symbol, int quantity) {
        symbol = symbol.toUpperCase();
        double currentPrice = stockApiService.getLivePrice(symbol);
        
        if (currentPrice <= 0) return;
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            // Check portfolio
            String checkPort = "SELECT quantity FROM portfolio WHERE user_id = ? AND stock_symbol = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkPort)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, symbol);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int existingQty = rs.getInt("quantity");
                    if (existingQty < quantity) {
                        System.out.println("You don't own enough shares! You have: " + existingQty);
                        conn.rollback();
                        return;
                    }
                    
                    // Update portfolio
                    int newQty = existingQty - quantity;
                    if (newQty == 0) {
                        String delPort = "DELETE FROM portfolio WHERE user_id = ? AND stock_symbol = ?";
                        try (PreparedStatement pstmtDel = conn.prepareStatement(delPort)) {
                            pstmtDel.setInt(1, userId);
                            pstmtDel.setString(2, symbol);
                            pstmtDel.executeUpdate();
                        }
                    } else {
                        String updatePort = "UPDATE portfolio SET quantity = ? WHERE user_id = ? AND stock_symbol = ?";
                        try (PreparedStatement pstmtUpd = conn.prepareStatement(updatePort)) {
                            pstmtUpd.setInt(1, newQty);
                            pstmtUpd.setInt(2, userId);
                            pstmtUpd.setString(3, symbol);
                            pstmtUpd.executeUpdate();
                        }
                    }
                } else {
                    System.out.println("You don't own any shares of " + symbol);
                    conn.rollback();
                    return;
                }
            }
            
            // Add funds
            double earnings = quantity * currentPrice;
            String addFunds = "UPDATE users SET bank_balance = bank_balance + ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(addFunds)) {
                pstmt.setDouble(1, earnings);
                pstmt.setInt(2, userId);
                pstmt.executeUpdate();
            }
            
            // Log transaction
            String insertTrans = "INSERT INTO transactions (user_id, stock_symbol, type, quantity, price) VALUES (?, ?, 'SELL', ?, ?)";
            try (PreparedStatement pstmtTrans = conn.prepareStatement(insertTrans)) {
                pstmtTrans.setInt(1, userId);
                pstmtTrans.setString(2, symbol);
                pstmtTrans.setInt(3, quantity);
                pstmtTrans.setDouble(4, currentPrice);
                pstmtTrans.executeUpdate();
            }
            
            conn.commit();
            System.out.printf("Successfully sold %d shares of %s at $%.2f for a total of $%.2f%n", quantity, symbol, currentPrice, earnings);
            
        } catch (SQLException e) {
            System.out.println("Error processing sell transaction: " + e.getMessage());
        }
    }
}
