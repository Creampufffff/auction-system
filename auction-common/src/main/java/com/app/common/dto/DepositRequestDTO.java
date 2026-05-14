package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class DepositRequestDTO implements Serializable {
    // Getters and Setters
    private String userId;
    private double amount;

    public DepositRequestDTO() {}

    public DepositRequestDTO(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

}

