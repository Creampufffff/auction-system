package com.auction.client.service;

import com.app.common.dto.LoginRequestDTO;
import com.app.common.dto.LoginResponseDTO;
import com.app.common.dto.RegisterRequestDTO;
import com.app.common.dto.RegisterResponseDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuthService {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public LoginResponseDTO login(String username, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        return login(request);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        if (request == null
                || request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            return null;
        }

        String response = sendCommand("LOGIN " + request.getUsername() + " " + request.getPassword());
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
        if (request == null
                || request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()
                || request.getEmail() == null
                || request.getEmail().isBlank()) {
            return createRegisterResponse(false, "Thong tin dang ky khong hop le.", null);
        }

        String response = sendCommand("REGISTER_BIDDER "
                + request.getUsername() + " "
                + request.getPassword() + " "
                + request.getEmail());
        return parseRegisterResponse(response);
    }

    private String sendCommand(String command) {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            reader.readLine(); // OK|CONNECTED|Type HELP for commands

            writer.println(command);
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private LoginResponseDTO parseLoginResponse(String response) {
        if (response == null || !response.startsWith("OK|LOGIN|")) {
            return null;
        }

        String[] parts = response.split("\\|");
        if (parts.length < 5) {
            return null;
        }

        double balance = parts.length >= 6 ? Double.parseDouble(parts[5]) : 0.0;
        return createLoginResponse(parts[2], parts[3], parts[4], balance);
    }

    private RegisterResponseDTO parseRegisterResponse(String response) {
        if (response == null || response.isBlank()) {
            return createRegisterResponse(false, "Server khong phan hoi.", null);
        }

        if (response.startsWith("OK|REGISTER_BIDDER|")) {
            String[] parts = response.split("\\|");
            String userId = parts.length >= 3 ? parts[2] : null;
            return createRegisterResponse(true, "Dang ky thanh cong.", userId);
        }

        if (response.startsWith("ERR|")) {
            return createRegisterResponse(false, response.substring(4), null);
        }

        return createRegisterResponse(false, "Phan hoi dang ky khong hop le.", null);
    }

    private LoginResponseDTO createLoginResponse(String userId, String username, String role, double balance) {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setUserId(userId);
        response.setUsername(username);
        response.setRole(role);
        response.setBalance(balance);
        return response;
    }

    private RegisterResponseDTO createRegisterResponse(boolean success, String message, String userId) {
        RegisterResponseDTO response = new RegisterResponseDTO();
        response.setSuccess(success);
        response.setMessage(message);
        response.setUserId(userId);
        return response;
    }
}
