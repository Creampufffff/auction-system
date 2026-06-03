package com.auction.domain.model;

public class AdminUserRow {
    private final String userId;
    private final String username;
    private final String email;
    private final String role;
    private final double balance;
    private final double heldBalance;

    public AdminUserRow(String userId, String username, String email, String role, double balance, double heldBalance) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
        this.heldBalance = heldBalance;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public double getBalance() {
        return balance;
    }

    public double getHeldBalance() {
        return heldBalance;
    }
}
