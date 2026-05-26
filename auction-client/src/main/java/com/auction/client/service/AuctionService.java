package com.auction.client.service;

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.app.common.enums.Status;
import com.auction.client.session.SessionManager;
import com.auction.client.service.SocketClientService;

import java.util.ArrayList;
import java.util.List;

public class AuctionService {
    public String createArtAuction(
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String author
    ) {
        if (!SessionManager.hasRole("Seller")) {
            return "ERR|Chỉ Seller mới được tạo phiên.";
        }

        String payload = String.join(
                "|",
                name,
                description,
                startDate,
                endDate,
                String.valueOf(startPrice),
                minIncrement,
                author
        );
        String command = "CREATE_ART_AUCTION " + payload;
        try {
            return SocketClientService.sendSessionCommand(command);
        } catch (Exception e) {
            return "ERR|Không thể gửi yêu cầu tới server.";
        }
    }

    public String createElectronicsAuction(
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String warrantyMonths
    ) {
        return createAuction(
                "CREATE_ELECTRONICS_AUCTION",
                name,
                description,
                startDate,
                endDate,
                startPrice,
                minIncrement,
                warrantyMonths
        );
    }

    public String createVehicleAuction(
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String brand
    ) {
        return createAuction(
                "CREATE_VEHICLE_AUCTION",
                name,
                description,
                startDate,
                endDate,
                startPrice,
                minIncrement,
                brand
        );
    }

    private String createAuction(
            String commandName,
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String typeSpecificValue
    ) {
        if (!SessionManager.hasRole("Seller")) {
            return "ERR|Chỉ Seller mới được tạo phiên.";
        }

        String payload = String.join(
                "|",
                name,
                description,
                startDate,
                endDate,
                String.valueOf(startPrice),
                minIncrement,
                typeSpecificValue
        );
        String command = commandName + " " + payload;
        try {
            return SocketClientService.sendSessionCommand(command);
        } catch (Exception e) {
            return "ERR|Không thể gửi yêu cầu tới server.";
        }
    }

    public List<AuctionListDTO> getActiveAuctions() {
        String response = sendCommand("LIST_AUCTIONS");
        return parseAuctionListResponse(response);
    }

    public List<AuctionListDTO> getMyAuctions() {
        try {
            String response = SocketClientService.sendSessionCommand("LIST_MY_AUCTIONS");
            return parseAuctionListResponse(response, "OK|MY_AUCTIONS|", "OK|MY_AUCTIONS|EMPTY");
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải kho hàng từ server.", e);
        }
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

        if (isBlank(request.getBidderId())) {
            request.setBidderId(SessionManager.getCurrentUserId());
        }

        try {
            String cmd = String.format(
                    "PLACE_BID %s %s %s",
                    request.getAuctionId(),
                    request.getBidderId(),
                    request.getBidAmount()
            );
            String resp = SocketClientService.sendSessionCommand(cmd);
            System.out.println("[AuctionService] PLACE_BID sent: " + cmd + " | resp=" + resp);
            return parsePlaceBidResponse(resp, request.getBidAmount());
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi yêu cầu đấu giá tới server.", e);
        }
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
        try {
            return SocketClientService.sendText(command);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi yêu cầu đấu giá tới server.", e);
        }
    }

    private List<AuctionListDTO> parseAuctionListResponse(String response) {
        return parseAuctionListResponse(response, "OK|AUCTIONS|", "OK|AUCTIONS|EMPTY");
    }

    private List<AuctionListDTO> parseAuctionListResponse(String response, String successPrefix, String emptyResponse) {
        List<AuctionListDTO> auctions = new ArrayList<>();
        if (response == null || response.isBlank() || response.startsWith("ERR|") || emptyResponse.equals(response)) {
            return auctions;
        }

        if (!response.startsWith(successPrefix)) {
            return auctions;
        }

        String[] parts = response.split("\\|");
        for (int i = 2; i < parts.length; i++) {
            AuctionListDTO auction = parseAuctionRecord(parts[i]);
            if (auction != null) {
                auctions.add(auction);
            }
        }
        return auctions;
    }

    private AuctionListDTO parseAuctionDetailResponse(String response) {
        if (response == null || response.isBlank() || response.startsWith("ERR|") || !response.startsWith("OK|AUCTION|")) {
            return null;
        }

        String[] parts = response.split("\\|", 3);
        if (parts.length < 3) {
            return null;
        }
        return parseAuctionRecord(parts[2]);
    }

    private AuctionListDTO parseAuctionRecord(String record) {
        if (record == null || record.isBlank()) {
            return null;
        }

        try {
            String[] fields = record.split(",", -1);
            if (fields.length < 5) {
                return null;
            }

            AuctionListDTO auction = new AuctionListDTO();
            auction.setAuctionId(fields[0]);
            auction.setItemId(fields[1]);
            if (fields.length >= 6) {
                auction.setItemType(fields[2]);
                auction.setName(fields[3]);
                auction.setCurrentPrice(Double.parseDouble(fields[4]));
                auction.setAuctionStatus(Status.valueOf(fields[5]));
                if (fields.length >= 8) {
                    auction.setStartDateTime(fields[6]);
                    auction.setEndDateTime(fields[7]);
                }
            } else {
                auction.setItemType("ART");
                auction.setName(fields[2]);
                auction.setCurrentPrice(Double.parseDouble(fields[3]));
                auction.setAuctionStatus(Status.valueOf(fields[4]));
            }
            return auction;
        } catch (Exception e) {
            return null;
        }
    }

    private PlaceBidResponseDTO parsePlaceBidResponse(String response, double amount) {
        if (response == null || response.isBlank()) {
            return createPlaceBidResponse(false, "Server không phản hồi.", null, null, 0);
        }

        if (response.startsWith("OK|BID_PLACED|")) {
            String[] parts = response.split("\\|", 5);
            if (parts.length >= 5) {
                PlaceBidResponseDTO dto = new PlaceBidResponseDTO();
                dto.setSuccess(true);
                dto.setMessage("Đặt giá thành công.");
                dto.setBidId(parts[2]);
                dto.setAuctionId(parts[3]);
                dto.setBidAmount(parseAmount(parts[4], amount));
                return dto;
            }
            return createPlaceBidResponse(false, "Phản hồi đặt giá không hợp lệ.", null, null, 0);
        }

        if (response.startsWith("ERR|")) {
            String message = extractErrorMessage(response);
            return createPlaceBidResponse(false, message, null, null, 0);
        }

        return createPlaceBidResponse(false, "Phản hồi đặt giá không hợp lệ.", null, null, 0);
    }

    private double parseAmount(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
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

    private String extractErrorMessage(String response) {
        String[] parts = response.split("\\|", 3);
        if (parts.length >= 3) {
            return parts[2];
        }
        if (parts.length >= 2) {
            return parts[1];
        }
        return "Đặt giá thất bại.";
    }
}
