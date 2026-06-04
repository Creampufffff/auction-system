package com.auction.application.service;

import com.app.common.dto.ApiResponseDTO;
import com.auction.shared.session.SessionManager;
import java.util.Locale;

public class AutoBidService {
    /**
     * Set auto-bid for an auction
     */
    public ApiResponseDTO setAutoBid(String auctionId, double maxAmount) {
        try {
            String userId = SessionManager.getCurrentUserId();
            if (userId == null || userId.isBlank()) {
                return new ApiResponseDTO(false, "User not logged in");
            }

            if (maxAmount <= 0) {
                return new ApiResponseDTO(false, "Max auto-bid amount must be greater than 0");
            }

            // Use socket text command (session) to set auto-bid so it works with existing socket server
            if (!SocketClientService.isSessionAlive()) {
                return new ApiResponseDTO(false, "Chưa đăng nhập hoặc kết nối tới server chưa được thiết lập. Vui lòng đăng nhập lại.");
            }

            String cmd = String.format(Locale.US , "SET_AUTO_BID %s %.2f", auctionId, maxAmount);
            String resp = SocketClientService.sendSessionCommand(cmd);
            if (resp != null && resp.startsWith("OK|AUTO_BID_SET")) {
                // OK|AUTO_BID_SET|{id}
                String[] parts = resp.split("\\|", 3);
                String msg = parts.length >= 3 ? parts[2] : "Auto-bid set";
                System.out.println("[AutoBidService] Auto-bid set successfully: " + msg);
                return new ApiResponseDTO(true, msg);
            }
            if (resp != null && resp.startsWith("ERR|")) {
                String[] parts = resp.split("\\|", 3);
                String msg = parts.length >= 3 ? parts[2] : parts.length >= 2 ? parts[1] : "Unknown error";
                return new ApiResponseDTO(false, msg);
            }
            return new ApiResponseDTO(false, "No response from server");
        } catch (Exception e) {
            System.err.println("[AutoBidService] Error setting auto-bid: " + e.getMessage());
            return new ApiResponseDTO(false, "Error setting auto-bid: " + e.getMessage());
        }
    }

    /**
     * Cancel an auto-bid
     */
    public ApiResponseDTO cancelAutoBid(String autoBidId) {
        try {
            String cmd = String.format("CANCEL_AUTO_BID %s", autoBidId);
            String resp = SocketClientService.sendSessionCommand(cmd);
            if (resp != null && resp.startsWith("OK|AUTO_BID_CANCELED")) {
                return new ApiResponseDTO(true, "Auto-bid canceled: " + autoBidId);
            }
            if (resp != null && resp.startsWith("ERR|")) {
                String[] parts = resp.split("\\|", 3);
                String msg = parts.length >= 3 ? parts[2] : parts.length >= 2 ? parts[1] : "Unknown error";
                return new ApiResponseDTO(false, msg);
            }
            return new ApiResponseDTO(false, "No response from server");
        } catch (Exception e) {
            System.err.println("[AutoBidService] Error canceling auto-bid: " + e.getMessage());
            return new ApiResponseDTO(false, "Error canceling auto-bid: " + e.getMessage());
        }
    }

}

