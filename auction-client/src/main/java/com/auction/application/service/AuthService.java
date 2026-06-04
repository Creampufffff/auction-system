package com.auction.application.service;

import com.app.common.dto.LoginRequestDTO;
import com.app.common.dto.LoginResponseDTO;
import com.app.common.dto.RegisterRequestDTO;
import com.app.common.dto.RegisterResponseDTO;

public class AuthService {
    public LoginResponseDTO login(String username, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        return login(request);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        if (isInvalidLoginRequest(request)) {
            return createFailedLoginResponse();
        }

        String response = sendCommand(String.format("LOGIN %s %s", request.getUsername(), request.getPassword()));
        return parseLoginResponse(response);
    }

    public RegisterResponseDTO register(String username, String password, String email) {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        return register(request);
    }

    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (isInvalidRegisterRequest(request)) {
            return createRegisterResponse(false, "Thông tin đăng ký không hợp lệ.", null);
        }

        String response = sendCommand(String.format(
                "REGISTER_BIDDER %s %s %s",
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        ));
        return parseRegisterResponse(response);
    }

    public RegisterResponseDTO registerSeller(String username, String password, String email) {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        if (isInvalidRegisterRequest(request)) {
            return createRegisterResponse(false, "Thông tin đăng ký không hợp lệ.", null);
        }

        // Seller registration uses its dedicated text command.
        String payload = String.format("REGISTER_SELLER %s %s %s", username, password, email);
        String response;
        try {
            response = sendCommand(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi yêu cầu đăng ký tới server.", e);
        }
        return parseRegisterResponse(response);
    }

    private boolean isInvalidLoginRequest(LoginRequestDTO request) {
        return request == null
                || isBlank(request.getUsername())
                || isBlank(request.getPassword());
    }

    private boolean isInvalidRegisterRequest(RegisterRequestDTO request) {
        return request == null
                || isBlank(request.getUsername())
                || isBlank(request.getPassword())
                || isBlank(request.getEmail());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String sendCommand(String command) {
        try {
            return SocketClientService.sendText(command);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi yêu cầu xác thực đến server.", e);
        }
    }

    private LoginResponseDTO parseLoginResponse(String response) {
        if (response == null || response.isBlank()) {
            return createFailedLoginResponse();
        }

        if (response.startsWith("OK|LOGIN|")) {
            String[] parts = response.split("\\|", 5);
            if (parts.length >= 5) {
                LoginResponseDTO dto = new LoginResponseDTO();
                dto.setUserId(parts[2]);
                dto.setUsername(parts[3]);
                dto.setRole(parts[4]);
                dto.setSuccess(true);
                return dto;
            }
            return createFailedLoginResponse();
        }

        if (response.startsWith("ERR|")) {
            return createFailedLoginResponse();
        }

        return createFailedLoginResponse();
    }

    private RegisterResponseDTO parseRegisterResponse(String response) {
        if (response == null || response.isBlank()) {
            return createRegisterResponse(false, "Server không phản hồi.", null);
        }

        if (response.startsWith("OK|REGISTER_BIDDER|") || response.startsWith("OK|REGISTER_SELLER|")) {
            String[] parts = response.split("\\|", 3);
            String id = parts.length >= 3 ? parts[2] : null;
            return createRegisterResponse(true, "Đăng ký thành công", id);
        }

        if (response.startsWith("ERR|")) {
            return createRegisterResponse(false, extractErrorMessage(response), null);
        }

        return createRegisterResponse(false, "Phản hồi đăng ký không hợp lệ.", null);
    }

    private LoginResponseDTO createFailedLoginResponse() {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setSuccess(false);
        return response;
    }

    private RegisterResponseDTO createRegisterResponse(boolean success, String message, String userId) {
        RegisterResponseDTO response = new RegisterResponseDTO();
        response.setSuccess(success);
        response.setMessage(message);
        response.setUserId(userId);
        return response;
    }

    private String extractErrorMessage(String response) {
        String[] parts = response.split("\\|", 3);
        if (parts.length >= 3) {
            return parts[2];
        }
        if (parts.length >= 2) {
            return parts[1];
        }
        return "Yêu cầu thất bại.";
    }
}

