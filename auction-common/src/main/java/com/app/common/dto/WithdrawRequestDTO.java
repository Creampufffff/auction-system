package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class WithdrawRequestDTO implements Serializable {
    // Getters and Setters
    private String userId;
    private double amount;

    public WithdrawRequestDTO() {}

    public WithdrawRequestDTO(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

}

