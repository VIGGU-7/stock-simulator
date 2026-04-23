package com.stockterminal.models;

public class User {
    private int id;
    private String username;
    private double bankBalance;

    public User(int id, String username, double bankBalance) {
        this.id = id;
        this.username = username;
        this.bankBalance = bankBalance;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }
}
