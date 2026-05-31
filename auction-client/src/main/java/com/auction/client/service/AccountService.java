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
        try {
            return parseWithdrawResponse(SocketClientService.sendSessionCommand("WITHDRAW " + amount));
        } catch (Exception e) {
            return new BalanceResponseDTO(null, 0, "Rut tien that bai.");
        }
    }

    public List<BidHistoryDTO> getBidHistory() {
        try {
            return parseBidHistoryResponse(SocketClientService.sendSessionCommand("GET_MY_BID_HISTORY"));
        } catch (Exception e) {
            return new ArrayList<>();
        }
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

    private BalanceResponseDTO parseWithdrawResponse(String response) {
        if (response == null || response.isBlank()) {
            return new BalanceResponseDTO(null, 0, "Server khong phan hoi.");
        }

        if (response.startsWith("OK|WITHDRAW|")) {
            String[] parts = response.split("\\|", 4);
            if (parts.length >= 4) {
                return createBalanceResponse(parts[2], parts[3], "Rut tien thanh cong.");
            }
        }

        return new BalanceResponseDTO(null, 0, extractErrorMessage(response));
    }

    private List<BidHistoryDTO> parseBidHistoryResponse(String response) {
        List<BidHistoryDTO> bids = new ArrayList<>();
        if (response == null || response.isBlank() || response.startsWith("ERR|") || "OK|BID_HISTORY|EMPTY".equals(response)) {
            return bids;
        }
        if (!response.startsWith("OK|BID_HISTORY|")) {
            return bids;
        }

        String[] records = response.split("\\|");
        for (int i = 2; i < records.length; i++) {
            BidHistoryDTO bid = parseBidHistoryRecord(records[i]);
            if (bid != null) {
                bids.add(bid);
            }
        }
        return bids;
    }

    private BidHistoryDTO parseBidHistoryRecord(String record) {
        String[] fields = record.split(",", -1);
        if (fields.length < 7) {
            return null;
        }

        try {
            return new BidHistoryDTO(
                    fields[0],
                    fields[1],
                    emptyToNull(fields[2]),
                    emptyToNull(fields[3]),
                    fields[4],
                    Double.parseDouble(fields[5]),
                    fields[6]
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
