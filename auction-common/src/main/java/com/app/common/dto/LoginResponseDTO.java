package com.app.common.dto;

import java.io.Serializable;

public class LoginResponseDTO implements Serializable {
    private String userId;
    private String username;
    private String role;
    private double balance;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String userId, String username, String role, double balance) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.balance = balance;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
