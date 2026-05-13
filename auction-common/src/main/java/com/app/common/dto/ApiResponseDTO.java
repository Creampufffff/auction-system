package com.app.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ApiResponseDTO implements Serializable {
    // Getters and Setters
    private boolean success;
    private String message;

    public ApiResponseDTO() {}

    public ApiResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

}

