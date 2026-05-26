package com.auction.client.service;

import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.BidHistoryDTO;

import java.util.ArrayList;
import java.util.List;

public class AccountService {

    public BalanceResponseDTO getBalance() {
        try {
            return parseBalanceResponse(SocketClientService.sendSessionCommand("GET_BALANCE"));
        } catch (Exception e) {
            return new BalanceResponseDTO(null, 0, "Khong the tai so du.");
        }
    }

    public BalanceResponseDTO deposit(double amount) {
        try {
            return parseDepositResponse(SocketClientService.sendSessionCommand("DEPOSIT " + amount));
        } catch (Exception e) {
            return new BalanceResponseDTO(null, 0, "Nap tien that bai.");
        }
    }

    public BalanceResponseDTO withdraw(double amount) {
        return new BalanceResponseDTO(null, 0, "Server chua ho tro rut tien.");
    }

    public List<BidHistoryDTO> getBidHistory() {
        return new ArrayList<>();
    }

    private BalanceResponseDTO parseBalanceResponse(String response) {
        if (response == null || response.isBlank()) {
            return new BalanceResponseDTO(null, 0, "Server khong phan hoi.");
        }

        if (response.startsWith("OK|BALANCE|")) {
            String[] parts = response.split("\\|", 4);
            if (parts.length >= 4) {
                return createBalanceResponse(parts[2], parts[3], "Tai so du thanh cong.");
            }
        }

        return new BalanceResponseDTO(null, 0, extractErrorMessage(response));
    }

    private BalanceResponseDTO parseDepositResponse(String response) {
        if (response == null || response.isBlank()) {
            return new BalanceResponseDTO(null, 0, "Server khong phan hoi.");
        }

        if (response.startsWith("OK|DEPOSIT|")) {
            String[] parts = response.split("\\|", 4);
            if (parts.length >= 4) {
                return createBalanceResponse(parts[2], parts[3], "Nap tien thanh cong.");
            }
        }

        return new BalanceResponseDTO(null, 0, extractErrorMessage(response));
    }

    private BalanceResponseDTO createBalanceResponse(String userId, String balanceValue, String message) {
        try {
            return new BalanceResponseDTO(userId, Double.parseDouble(balanceValue), message);
        } catch (NumberFormatException e) {
            return new BalanceResponseDTO(null, 0, "Phan hoi so du khong hop le.");
        }
    }

    private String extractErrorMessage(String response) {
        if (response == null || response.isBlank()) {
            return "Yeu cau that bai.";
        }

        String[] parts = response.split("\\|", 3);
        if (parts.length >= 3) {
            return parts[2];
        }
        if (parts.length >= 2) {
            return parts[1];
        }
        return "Yeu cau that bai.";
    }
}
