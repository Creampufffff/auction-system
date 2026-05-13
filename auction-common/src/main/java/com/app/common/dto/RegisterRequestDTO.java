package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class RegisterRequestDTO implements Serializable {
    private String username;
    private String password;
    private String email;

    public RegisterRequestDTO() {
    }

    public RegisterRequestDTO(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

}

