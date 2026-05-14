package com.auction.client.service;

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.app.common.enums.Status;
import com.auction.client.session.SessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class AuctionService {
    private static final ObjectMapper MAPPER = SocketClientService.mapper();

    public List<AuctionListDTO> getActiveAuctions() {
        String response = sendEnvelope("LIST_AUCTIONS", MAPPER.createObjectNode());
        return parseAuctionListResponse(response);
    }

    public AuctionListDTO getAuctionById(String auctionId) {
        if (isBlank(auctionId)) {
            return null;
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("auctionId", auctionId);
        String response = sendEnvelope("GET_AUCTION", payload);
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

        // Prefer sending on authenticated session socket (server requires auth on same connection)
        try {
            String auctionId = request.getAuctionId();
            String bidderId = request.getBidderId();
            double amount = request.getBidAmount();

            String cmd = String.format("PLACE_BID %s %s %s", auctionId, bidderId, String.valueOf(amount));
            String resp = SocketClientService.sendSessionCommand(cmd);
            System.out.println("[AuctionService] PLACE_BID sent: " + cmd + " | resp=" + resp);

            if (resp == null || resp.isBlank()) {
                return createPlaceBidResponse(false, "Server không phản hồi.", null, null, 0);
            }

            if (resp.startsWith("OK|BID_PLACED|")) {
                String[] parts = resp.split("\\|", 5);
                String bidId = parts.length >= 3 ? parts[2] : null;
                String respAuctionId = parts.length >= 4 ? parts[3] : auctionId;
                double bidAmount = parts.length >= 5 ? Double.parseDouble(parts[4]) : amount;
                return createPlaceBidResponse(true, "Đặt giá thành công.", bidId, respAuctionId, bidAmount);
            }

            if (resp.startsWith("ERR|")) {
                String msg = extractErrorMessage(resp);
                return createPlaceBidResponse(false, msg, null, null, 0);
            }

            // Fallback: try parse JSON-style response
            return parsePlaceBidResponse(resp);
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

    private String sendEnvelope(String type, Object payload) {
        try {
            ObjectNode envelope = MAPPER.createObjectNode();
            envelope.put("type", type);
            envelope.set("payload", MAPPER.valueToTree(payload));
            return SocketClientService.sendJson(MAPPER.writeValueAsString(envelope));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi yêu cầu đấu giá tới server.", e);
        }
    }

    private List<AuctionListDTO> parseAuctionListResponse(String response) {
        List<AuctionListDTO> auctions = new ArrayList<>();

        JsonNode root = parseJson(response);
        if (root == null) {
            return auctions;
        }

        JsonNode payload = root.path("payload");
        if (!root.path("success").asBoolean(false) || !payload.isArray() || payload.isEmpty()) {
            return auctions;
        }

        for (JsonNode node : payload) {
            AuctionListDTO auction = parseAuctionNode(node);
            if (auction != null) {
                auctions.add(auction);
            }
        }

        return auctions;
    }

    private AuctionListDTO parseAuctionDetailResponse(String response) {
        JsonNode root = parseJson(response);
        if (root == null || !root.path("success").asBoolean(false)) {
            return null;
        }

        JsonNode payload = root.path("payload");
        if (payload.isMissingNode() || payload.isNull()) {
            return null;
        }

        return parseAuctionNode(payload);
    }

    private PlaceBidResponseDTO parsePlaceBidResponse(String response) {
        if (response == null || response.isBlank()) {
            return createPlaceBidResponse(false, "Server không phản hồi.", null, null, 0);
        }

        JsonNode root = parseJson(response);
        if (root != null) {
            JsonNode payload = root.path("payload");
            if (payload.isMissingNode() || payload.isNull()) {
                String message = root.path("message").asText("Phản hồi đặt giá không hợp lệ.");
                return createPlaceBidResponse(false, message, null, null, 0);
            }

            PlaceBidResponseDTO dto = parsePlaceBidNode(payload);
            return dto == null ? createPlaceBidResponse(false, "Phản hồi đặt giá không hợp lệ.", null, null, 0) : dto;
        }

        if (response.startsWith("ERR|")) {
            String message = extractErrorMessage(response);
            return createPlaceBidResponse(false, message, null, null, 0);
        }

        return createPlaceBidResponse(false, "Phản hồi đặt giá không hợp lệ.", null, null, 0);
    }

    private JsonNode parseJson(String response) {
        if (response == null || !response.startsWith("{")) {
            return null;
        }

        try {
            return MAPPER.readTree(response);
        } catch (Exception e) {
            return null;
        }
    }

    private AuctionListDTO parseAuctionNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        try {
            AuctionListDTO auction = MAPPER.treeToValue(node, AuctionListDTO.class);
            if (auction == null) {
                return null;
            }

            if (auction.getAuctionStatus() == null && node.hasNonNull("auctionStatus")) {
                auction.setAuctionStatus(Status.valueOf(node.get("auctionStatus").asText()));
            }
            return auction;
        } catch (Exception e) {
            return null;
        }
    }

    private PlaceBidResponseDTO parsePlaceBidNode(JsonNode node) {
        try {
            return MAPPER.treeToValue(node, PlaceBidResponseDTO.class);
        } catch (Exception e) {
            return null;
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
