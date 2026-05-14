package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class RegisterResponseDTO implements Serializable {
    private boolean success;
    private String message;
    private String userId;

    public RegisterResponseDTO() {
    }

    public RegisterResponseDTO(boolean success, String message, String userId) {
        this.success = success;
        this.message = message;
        this.userId = userId;
    }

}

