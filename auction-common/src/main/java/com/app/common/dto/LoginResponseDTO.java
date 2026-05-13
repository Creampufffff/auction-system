package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
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

}
