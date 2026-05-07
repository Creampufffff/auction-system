package com.auction.client.service;

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.app.common.enums.Status;
import com.auction.client.session.SessionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AuctionService {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public List<AuctionListDTO> getActiveAuctions() {
        String response = sendCommand("LIST_AUCTIONS");
        return parseAuctionListResponse(response);
    }

    public AuctionListDTO getAuctionById(String auctionId) {
        if (isBlank(auctionId)) {
            return null;
        }

        String response = sendCommand("GET_AUCTION " + auctionId);
        return parseAuctionDetailResponse(response);
    }

    public PlaceBidResponseDTO placeBid(String auctionId, double bidAmount) {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId(auctionId);
        request.setBidderId(SessionManager.getCurrentUserId());
        request.setBidAmount(bidAmount);
        return placeBid(request);
    }

    public PlaceBidResponseDTO placeBid(PlaceBidRequestDTO request) {
        if (isInvalidBidRequest(request)) {
            return createPlaceBidResponse(false, "Thông tin đặt giá không hợp lệ.", null, null, 0);
        }

        if (!SessionManager.hasRole("Bidder")) {
            return createPlaceBidResponse(false, "Chỉ người đấu giá mới được đặt giá.", null, null, 0);
        }

        String command = "PLACE_BID "
                + request.getAuctionId() + " "
                + request.getBidderId() + " "
                + request.getBidAmount();
        String response = sendCommand(command);
        return parsePlaceBidResponse(response);
    }

    private boolean isInvalidBidRequest(PlaceBidRequestDTO request) {
        return request == null
                || isBlank(request.getAuctionId())
                || isBlank(request.getBidderId())
                || request.getBidAmount() <= 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    private List<AuctionListDTO> parseAuctionListResponse(String response) {
        List<AuctionListDTO> auctions = new ArrayList<>();

        if (response == null || !response.startsWith("OK|AUCTIONS")) {
            return auctions;
        }

        String[] parts = response.split("\\|");
        if (parts.length < 3 || "EMPTY".equals(parts[2])) {
            return auctions;
        }

        for (int i = 2; i < parts.length; i++) {
            AuctionListDTO auction = parseAuction(parts[i]);
            if (auction != null) {
                auctions.add(auction);
            }
        }

        return auctions;
    }

    private AuctionListDTO parseAuctionDetailResponse(String response) {
        if (response == null || !response.startsWith("OK|AUCTION|")) {
            return null;
        }

        String[] parts = response.split("\\|");
        if (parts.length < 3) {
            return null;
        }

        return parseAuction(parts[2]);
    }

    private PlaceBidResponseDTO parsePlaceBidResponse(String response) {
        if (response == null || response.isBlank()) {
            return createPlaceBidResponse(false, "Server không phản hồi.", null, null, 0);
        }

        if (response.startsWith("OK|BID_PLACED|")) {
            String[] parts = response.split("\\|");
            if (parts.length < 5) {
                return createPlaceBidResponse(false, "Phản hồi đặt giá không hợp lệ.", null, null, 0);
            }

            return createPlaceBidResponse(
                    true,
                    "Đặt giá thành công.",
                    parts[2],
                    parts[3],
                    Double.parseDouble(parts[4])
            );
        }

        if (response.startsWith("ERR|")) {
            return createPlaceBidResponse(false, response.substring(4), null, null, 0);
        }

        return createPlaceBidResponse(false, "Phản hồi đặt giá không hợp lệ.", null, null, 0);
    }

    private AuctionListDTO parseAuction(String value) {
        String[] fields = value.split(",", -1);
        if (fields.length < 5) {
            return null;
        }

        AuctionListDTO auction = new AuctionListDTO();
        auction.setAuctionId(fields[0]);
        auction.setItemId(fields[1]);
        auction.setName(fields[2]);
        auction.setCurrentPrice(Double.parseDouble(fields[3]));
        auction.setAuctionStatus(Status.valueOf(fields[4]));
        return auction;
    }

    private PlaceBidResponseDTO createPlaceBidResponse(
            boolean success,
            String message,
            String bidId,
            String auctionId,
            double bidAmount
    ) {
        PlaceBidResponseDTO response = new PlaceBidResponseDTO();
        response.setSuccess(success);
        response.setMessage(message);
        response.setBidId(bidId);
        response.setAuctionId(auctionId);
        response.setBidAmount(bidAmount);
        return response;
    }
}
