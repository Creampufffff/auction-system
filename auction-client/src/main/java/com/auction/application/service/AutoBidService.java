package com.auction.application.service;

import com.app.common.dto.ApiResponseDTO;
import com.app.common.dto.AutoBidDTO;
import com.auction.shared.session.SessionManager;
import com.auction.application.service.SocketClientService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AutoBidService {
    private static final String SERVER_URL = "http://localhost:8080/api/autobid";
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /**
     * Get all active auto-bids for current user
     */
    public List<AutoBidDTO> getMyActiveBids() {
        try {
            String userId = SessionManager.getCurrentUserId();
            if (userId == null || userId.isBlank()) {
                return new ArrayList<>();
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(SERVER_URL + "/bidder/" + userId).openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                AutoBidDTO[] dtos = objectMapper.readValue(response, AutoBidDTO[].class);
                List<AutoBidDTO> result = new ArrayList<>();
                for (AutoBidDTO dto : dtos) {
                    if (dto.isActive()) {
                        result.add(dto);
                    }
                }
                return result;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("[AutoBidService] Error getting my active bids: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get all auto-bids for a specific auction
     */
    public List<AutoBidDTO> getAuctionBids(String auctionId) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(SERVER_URL + "/auction/" + auctionId).openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                AutoBidDTO[] dtos = objectMapper.readValue(response, AutoBidDTO[].class);
                List<AutoBidDTO> result = new ArrayList<>();
                for (AutoBidDTO dto : dtos) {
                    result.add(dto);
                }
                return result;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("[AutoBidService] Error getting auction bids: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get single auto-bid by ID
     */
    public AutoBidDTO getAutoBid(String autoBidId) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(SERVER_URL + "/" + autoBidId).openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            String response = readResponse(conn);

            if (responseCode == 200) {
                return objectMapper.readValue(response, AutoBidDTO.class);
            } else {
                return null;
            }
        } catch (Exception e) {
            System.err.println("[AutoBidService] Error getting auto-bid: " + e.getMessage());
            return null;
        }
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader;
        if (conn.getResponseCode() >= 400) {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
}

