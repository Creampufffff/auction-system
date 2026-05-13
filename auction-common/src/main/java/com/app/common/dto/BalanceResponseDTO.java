package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class BalanceResponseDTO implements Serializable {
    // Getters and Setters
    private String userId;
    private double balance;
    private String message;

    public BalanceResponseDTO() {}

    public BalanceResponseDTO(String userId, double balance, String message) {
        this.userId = userId;
        this.balance = balance;
        this.message = message;
    }

}

