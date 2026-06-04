package com.auction.application.service;

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.BidHistoryDTO;
import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.app.common.enums.Status;
import com.auction.shared.session.SessionManager;

import java.util.ArrayList;
import java.util.Base64;
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
        return createArtAuction(name, description, startDate, endDate, startPrice, minIncrement, author, null);
    }

    public String createArtAuction(
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String author,
            byte[] imageBlob
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
                author,
                encodeImage(imageBlob)
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
        return createElectronicsAuction(name, description, startDate, endDate, startPrice, minIncrement, warrantyMonths, null);
    }

    public String createElectronicsAuction(
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String warrantyMonths,
            byte[] imageBlob
    ) {
        return createAuction(
                "CREATE_ELECTRONICS_AUCTION",
                name,
                description,
                startDate,
                endDate,
                startPrice,
                minIncrement,
                normalizeWarrantyMonths(warrantyMonths),
                imageBlob
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
        return createVehicleAuction(name, description, startDate, endDate, startPrice, minIncrement, brand, null);
    }

    public String createVehicleAuction(
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String brand,
            byte[] imageBlob
    ) {
        return createAuction(
                "CREATE_VEHICLE_AUCTION",
                name,
                description,
                startDate,
                endDate,
                startPrice,
                minIncrement,
                brand,
                imageBlob
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
            String typeSpecificValue,
            byte[] imageBlob
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
                typeSpecificValue,
                encodeImage(imageBlob)
        );
        String command = commandName + " " + payload;
        try {
            return SocketClientService.sendSessionCommand(command);
        } catch (Exception e) {
            return "ERR|Không thể gửi yêu cầu tới server.";
        }
    }

    private String encodeImage(byte[] imageBlob) {
        return imageBlob == null || imageBlob.length == 0 ? "" : Base64.getEncoder().encodeToString(imageBlob);
    }

    private String normalizeWarrantyMonths(String warrantyMonths) {
        if (warrantyMonths == null || warrantyMonths.isBlank()) {
            return "0";
        }

        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+").matcher(warrantyMonths.trim());
            if (!matcher.find()) {
                return "0";
            }
            int parsed = Integer.parseInt(matcher.group());
            return String.valueOf(Math.max(0, parsed));
        } catch (NumberFormatException e) {
            return "0";
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

    public List<AuctionListDTO> getMyWonItems() {
        try {
            String response = SocketClientService.sendSessionCommand("GET_MY_ITEMS");
            return parseAuctionListResponse(response, "OK|MY_ITEMS|", "OK|MY_ITEMS|EMPTY");
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public AuctionListDTO getAuctionById(String auctionId) {
        if (isBlank(auctionId)) {
            return null;
        }

        String response = sendCommand("GET_AUCTION " + auctionId);
        return parseAuctionDetailResponse(response);
    }

    public List<BidHistoryDTO> getBidHistory(String auctionId) {
        if (isBlank(auctionId)) {
            return new ArrayList<>();
        }

        try {
            String response = sendCommand("GET_BID_HISTORY " + auctionId);
            return parseBidHistoryResponse(response);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String updateAuction(
            String auctionId,
            String name,
            String description,
            String startDate,
            String endDate,
            double startPrice,
            String minIncrement,
            String typeSpecificValue
    ) {
        if (!SessionManager.hasRole("Seller")) {
            return "ERR|Chỉ Seller mới được sửa phiên.";
        }

        String payload = String.join(
                "|",
                auctionId,
                name,
                description,
                startDate,
                endDate,
                String.valueOf(startPrice),
                minIncrement,
                typeSpecificValue
        );
        try {
            return SocketClientService.sendSessionCommand("UPDATE_AUCTION " + payload);
        } catch (Exception e) {
            return "ERR|Không thể gửi yêu cầu sửa phiên tới server.";
        }
    }

    public String renameAuction(String auctionId, String name) {
        if (!SessionManager.hasRole("Seller")) {
            return "ERR|Chỉ Seller mới được sửa phiên.";
        }

        try {
            return SocketClientService.sendSessionCommand("UPDATE_AUCTION " + auctionId + "|" + name);
        } catch (Exception e) {
            return "ERR|Không thể gửi yêu cầu sửa phiên tới server.";
        }
    }

    public String deleteAuction(String auctionId) {
        if (!SessionManager.hasRole("Seller")) {
            return "ERR|Chỉ Seller mới được xóa phiên.";
        }

        try {
            return SocketClientService.sendSessionCommand("DELETE_AUCTION " + auctionId);
        } catch (Exception e) {
            return "ERR|Không thể gửi yêu cầu xóa phiên tới server.";
        }
    }

    public String uploadAuctionImage(String auctionId, byte[] imageBlob) {
        if (!SessionManager.hasRole("Seller")) {
            return "ERR|Chỉ Seller mới được upload ảnh.";
        }
        if (isBlank(auctionId) || imageBlob == null || imageBlob.length == 0) {
            return "ERR|Thông tin ảnh không hợp lệ.";
        }

        try {
            return SocketClientService.sendSessionCommand("UPLOAD_IMAGE " + auctionId + "|" + encodeImage(imageBlob));
        } catch (Exception e) {
            return "ERR|Không thể gửi ảnh tới server.";
        }
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
                if (fields.length >= 11) {
                    auction.setCondition(emptyToNull(fields[8]));
                    auction.setDescription(emptyToNull(fields[9]));
                    auction.setWarranty(emptyToNull(fields[10]));
                }
                if (fields.length >= 12) {
                    if (isNumeric(fields[11])) {
                        auction.setMinIncrement(Double.parseDouble(fields[11]));
                    } else {
                        auction.setImageBlob(decodeImage(fields[11]));
                    }
                }
                if (fields.length >= 13) {
                    if (isNumeric(fields[11])) {
                        auction.setImageBlob(decodeImage(fields[12]));
                    } else {
                        auction.setHighestBidderUsername(emptyToNull(fields[11]));
                        auction.setImageBlob(decodeImage(fields[12]));
                    }
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

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private byte[] decodeImage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
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

