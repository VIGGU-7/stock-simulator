package com.stockterminal;

import com.stockterminal.db.DatabaseConfig;
import com.stockterminal.models.User;
import com.stockterminal.services.AuthService;
import com.stockterminal.services.PortfolioService;
import com.stockterminal.services.StockApiService;
import com.stockterminal.services.TradingService;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final AuthService authService = new AuthService();
    private static final StockApiService stockApiService = new StockApiService();
    private static final TradingService tradingService = new TradingService();
    private static final PortfolioService portfolioService = new PortfolioService();
    private static User loggedInUser = null;

    public static void main(String[] args) {
        System.out.println("Initializing Database...");
        DatabaseConfig.initializeDatabase();
        System.out.println("Database Initialized!");

        while (true) {
            if (loggedInUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    private static void showAuthMenu() {
        System.out.println("\n==============================");
        System.out.println("    Stock Trading Terminal    ");
        System.out.println("==============================");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                handleLogin();
                break;
            case "2":
                handleRegister();
                break;
            case "3":
                System.out.println("Goodbye!");
                System.exit(0);
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }

    private static void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        loggedInUser = authService.login(username, password);
        if (loggedInUser != null) {
            System.out.println("Login successful! Welcome, " + loggedInUser.getUsername());
        } else {
            System.out.println("Invalid credentials.");
        }
    }

    private static void handleRegister() {
        System.out.print("Enter new username: ");
        String username = scanner.nextLine();
        System.out.print("Enter new password: ");
        String password = scanner.nextLine();

        loggedInUser = authService.register(username, password);
        if (loggedInUser != null) {
            System.out.println("Registration successful! You have been granted $10,000 virtual balance.");
        }
    }

    private static void showMainMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. View Bank Balance");
        System.out.println("2. View Live Stock Price & Chart");
        System.out.println("3. Buy Stock");
        System.out.println("4. Sell Stock");
        System.out.println("5. View Portfolio");
        System.out.println("6. View Recent Transactions");
        System.out.println("7. Logout");
        System.out.println("8. Exit");
        System.out.print("Choose an option: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                double balance = authService.getBalance(loggedInUser.getId());
                System.out.printf("Current Bank Balance: $%.2f%n", balance);
                break;
            case "2":
                System.out.print("Enter stock symbol (e.g., AAPL): ");
                String symbol = scanner.nextLine().toUpperCase();
                double price = stockApiService.getLivePrice(symbol);
                if (price > 0) {
                    System.out.printf("Live Price of %s: $%.2f%n", symbol, price);
                    stockApiService.showTradingViewLink(symbol);
                }
                break;
            case "3":
                System.out.print("Enter stock symbol to buy: ");
                String buySymbol = scanner.nextLine().toUpperCase();
                System.out.print("Enter quantity: ");
                try {
                    int buyQty = Integer.parseInt(scanner.nextLine());
                    if (buyQty > 0) {
                        tradingService.buyStock(loggedInUser.getId(), buySymbol, buyQty);
                    } else {
                        System.out.println("Quantity must be greater than 0.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid quantity.");
                }
                break;
            case "4":
                System.out.print("Enter stock symbol to sell: ");
                String sellSymbol = scanner.nextLine().toUpperCase();
                System.out.print("Enter quantity: ");
                try {
                    int sellQty = Integer.parseInt(scanner.nextLine());
                    if (sellQty > 0) {
                        tradingService.sellStock(loggedInUser.getId(), sellSymbol, sellQty);
                    } else {
                        System.out.println("Quantity must be greater than 0.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid quantity.");
                }
                break;
            case "5":
                portfolioService.viewPortfolio(loggedInUser.getId());
                break;
            case "6":
                portfolioService.viewTransactions(loggedInUser.getId());
                break;
            case "7":
                loggedInUser = null;
                System.out.println("Logged out successfully.");
                break;
            case "8":
                System.out.println("Goodbye!");
                System.exit(0);
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }
}
