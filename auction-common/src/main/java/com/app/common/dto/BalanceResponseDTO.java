package com.app.common.dto;

import java.io.Serializable;

public class BalanceResponseDTO implements Serializable {
    private String userId;
    private double balance;
    private String message;

    public BalanceResponseDTO() {}

    public BalanceResponseDTO(String userId, double balance, String message) {
        this.userId = userId;
        this.balance = balance;
        this.message = message;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

