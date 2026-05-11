package com.app.common.dto;

import java.io.Serializable;

public class DepositRequestDTO implements Serializable {
    private String userId;
    private double amount;

    public DepositRequestDTO() {}

    public DepositRequestDTO(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

