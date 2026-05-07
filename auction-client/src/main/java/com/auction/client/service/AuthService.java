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

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            reader.readLine(); // OK|CONNECTED|Type HELP for commands

            writer.println("LOGIN " + request.getUsername() + " " + request.getPassword());
            String response = reader.readLine();

            return parseLoginResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean register(String username, String password) {
        RegisterResponseDTO response = register(username, password, null);
        return response != null && response.isSuccess();
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

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            reader.readLine(); // OK|CONNECTED|Type HELP for commands

            writer.println("REGISTER_BIDDER "
                    + request.getUsername() + " "
                    + request.getPassword() + " "
                    + request.getEmail());
            String response = reader.readLine();

            return parseRegisterResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return createRegisterResponse(false, "Khong the ket noi toi server.", null);
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

        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setUserId(parts[2]);
        loginResponse.setUsername(parts[3]);
        loginResponse.setBalance(0.0);
        return loginResponse;
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

    private RegisterResponseDTO createRegisterResponse(boolean success, String message, String userId) {
        RegisterResponseDTO response = new RegisterResponseDTO();
        response.setSuccess(success);
        response.setMessage(message);
        response.setUserId(userId);
        return response;
    }
}
