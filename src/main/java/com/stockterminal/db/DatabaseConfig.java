package com.stockterminal.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final String URL = "jdbc:sqlite:stock_trading.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver not found on the classpath.", e);
        }
    }

    // Method to get a connection to the database
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return null;
        }
    }

    // Initialize database tables
    public static void initializeDatabase() {
        String usersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "bank_balance REAL DEFAULT 10000.0" // Giving $10k initial virtual money
                + ");";

        String portfolioTable = "CREATE TABLE IF NOT EXISTS portfolio ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL,"
                + "stock_symbol TEXT NOT NULL,"
                + "quantity INTEGER NOT NULL,"
                + "average_buy_price REAL NOT NULL,"
                + "FOREIGN KEY(user_id) REFERENCES users(id)"
                + ");";

        String transactionsTable = "CREATE TABLE IF NOT EXISTS transactions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_id INTEGER NOT NULL,"
                + "stock_symbol TEXT NOT NULL,"
                + "type TEXT NOT NULL," // BUY or SELL
                + "quantity INTEGER NOT NULL,"
                + "price REAL NOT NULL,"
                + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(user_id) REFERENCES users(id)"
                + ");";

        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.err.println("Database initialization failed because no connection could be created.");
                return;
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(usersTable);
                stmt.execute(portfolioTable);
                stmt.execute(transactionsTable);
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }
}
