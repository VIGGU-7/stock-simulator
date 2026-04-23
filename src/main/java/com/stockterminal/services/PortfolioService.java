package com.stockterminal.services;

import com.stockterminal.db.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PortfolioService {
    private final StockApiService stockApiService = new StockApiService();
    
    public void viewPortfolio(int userId) {
        String sql = "SELECT stock_symbol, quantity, average_buy_price FROM portfolio WHERE user_id = ?";
        double totalPortfolioValue = 0;
        double totalInvested = 0;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n--- Your Portfolio ---");
            System.out.printf("%-10s %-10s %-15s %-15s %-15s %-15s%n", "Symbol", "Shares", "Avg Price", "Live Price", "Total Value", "P/L");
            System.out.println("----------------------------------------------------------------------------------");
            
            boolean hasStocks = false;
            while (rs.next()) {
                hasStocks = true;
                String symbol = rs.getString("stock_symbol");
                int qty = rs.getInt("quantity");
                double avgPrice = rs.getDouble("average_buy_price");
                
                double livePrice = stockApiService.getLivePrice(symbol);
                if (livePrice <= 0) livePrice = avgPrice; // Fallback to avoid breaking display
                
                double invested = qty * avgPrice;
                double currentValue = qty * livePrice;
                double pnl = currentValue - invested;
                
                totalInvested += invested;
                totalPortfolioValue += currentValue;
                
                System.out.printf("%-10s %-10d $%-14.2f $%-14.2f $%-14.2f $%-14.2f%n", 
                        symbol, qty, avgPrice, livePrice, currentValue, pnl);
            }
            
            if (!hasStocks) {
                System.out.println("Your portfolio is empty.");
                return;
            }
            
            System.out.println("----------------------------------------------------------------------------------");
            double totalPnl = totalPortfolioValue - totalInvested;
            System.out.printf("Total Invested: $%.2f | Current Portfolio Value: $%.2f | Total P/L: $%.2f%n", totalInvested, totalPortfolioValue, totalPnl);
            
        } catch (SQLException e) {
            System.out.println("Error viewing portfolio: " + e.getMessage());
        }
    }
    
    public void viewTransactions(int userId) {
        String sql = "SELECT stock_symbol, type, quantity, price, timestamp FROM transactions WHERE user_id = ? ORDER BY timestamp DESC LIMIT 10";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n--- Recent Transactions ---");
            System.out.printf("%-10s %-5s %-10s %-15s %-20s%n", "Symbol", "Type", "Shares", "Price", "Date");
            System.out.println("----------------------------------------------------------------");
            
            boolean hasTrans = false;
            while (rs.next()) {
                hasTrans = true;
                System.out.printf("%-10s %-5s %-10d $%-14.2f %-20s%n",
                        rs.getString("stock_symbol"),
                        rs.getString("type"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("timestamp")
                );
            }
            if (!hasTrans) {
                System.out.println("No recent transactions.");
            }
        } catch (SQLException e) {
            System.out.println("Error viewing transactions: " + e.getMessage());
        }
    }
}
