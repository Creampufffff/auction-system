package com.auction.client.service;

import com.app.common.dto.LoginRequestDTO;
import com.app.common.dto.LoginResponseDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;

public class AuthService {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public LoginResponseDTO login(String username, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        setField(request, "username", username);
        setField(request, "password", password);
        return login(request);
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        String username = getStringField(request, "username");
        String password = getStringField(request, "password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            reader.readLine(); // OK|CONNECTED|Type HELP for commands

            writer.println("LOGIN " + username + " " + password);
            String response = reader.readLine();

            return parseLoginResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean register(String username, String password) {
        return false;
    }

    private LoginResponseDTO parseLoginResponse(String response) {
        if (response == null || !response.startsWith("OK|LOGIN|")) {
            return null;
        }

        String[] parts = response.split("\\|");
        if (parts.length < 5) {
            return null;
        }

        String userId = parts[2];
        String username = parts[3];

        LoginResponseDTO loginResponse = new LoginResponseDTO();
        setField(loginResponse, "userId", userId);
        setField(loginResponse, "username", username);
        setField(loginResponse, "balance", 0.0);
        return loginResponse;
    }

    private String getStringField(Object target, String fieldName) {
        Object value = getField(target, fieldName);
        return value == null ? null : value.toString();
    }

    private Object getField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        if (target == null) {
            return;
        }

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }
}
