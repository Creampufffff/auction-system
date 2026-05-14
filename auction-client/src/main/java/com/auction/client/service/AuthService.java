package com.auction.client.service;

import com.app.common.dto.LoginRequestDTO;
import com.app.common.dto.LoginResponseDTO;
import com.app.common.dto.RegisterRequestDTO;
import com.app.common.dto.RegisterResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthService {
    private static final ObjectMapper MAPPER = SocketClientService.mapper();

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

        String response = sendEnvelope("LOGIN", request);
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

        String response = sendEnvelope("REGISTER_BIDDER", request);
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

        // Use legacy text protocol to register seller because server JSON handler creates Bidder by default
        String payload = String.format("REGISTER_SELLER %s %s %s", username, password, email);
        String response;
        try {
            response = SocketClientService.sendText(payload);
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

    private String sendEnvelope(String type, Object payload) {
        try {
            ObjectNode envelope = MAPPER.createObjectNode();
            envelope.put("type", type);
            envelope.set("payload", MAPPER.valueToTree(payload));
            return SocketClientService.sendJson(MAPPER.writeValueAsString(envelope));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi yêu cầu xác thực đến server.", e);
        }
    }

    private LoginResponseDTO parseLoginResponse(String response) {
        if (response == null || response.isBlank()) {
            return createFailedLoginResponse();
        }

        if (response.startsWith("{")) {
            try {
                JsonNode root = MAPPER.readTree(response);
                JsonNode payload = root.path("payload");
                if (payload.isMissingNode() || payload.isNull()) {
                    return createFailedLoginResponse();
                }
                LoginResponseDTO dto = MAPPER.treeToValue(payload, LoginResponseDTO.class);
                return dto == null ? createFailedLoginResponse() : dto;
            } catch (Exception e) {
                return createFailedLoginResponse();
            }
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

        if (response.startsWith("{")) {
            try {
                JsonNode root = MAPPER.readTree(response);
                JsonNode payload = root.path("payload");
                if (payload.isMissingNode() || payload.isNull()) {
                    return createRegisterResponse(false, "Đăng ký thất bại.", null);
                }
                RegisterResponseDTO dto = MAPPER.treeToValue(payload, RegisterResponseDTO.class);
                return dto == null ? createRegisterResponse(false, "Đăng ký thất bại.", null) : dto;
            } catch (Exception e) {
                return createRegisterResponse(false, "Đăng ký thất bại.", null);
            }
        }

        if (response.startsWith("ERR|")) {
            return createRegisterResponse(false, extractErrorMessage(response), null);
        }

        // Support legacy OK|REGISTER_BIDDER|id and OK|REGISTER_SELLER|id responses
        if (response.startsWith("OK|REGISTER_BIDDER|") || response.startsWith("OK|REGISTER_SELLER|")) {
            String[] parts = response.split("\\|", 3);
            String id = parts.length >= 3 ? parts[2] : null;
            return createRegisterResponse(true, "Đăng ký thành công", id);
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
