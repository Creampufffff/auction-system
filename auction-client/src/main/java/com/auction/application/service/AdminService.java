package com.auction.application.service;

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.BidHistoryDTO;
import com.app.common.enums.Status;
import com.auction.domain.model.AdminDashboardStats;
import com.auction.domain.model.AdminUserRow;

import java.util.ArrayList;
import java.util.List;

public class AdminService {
    public AdminDashboardStats getDashboardStats() {
        String response = sendAdminCommand("ADMIN_DASHBOARD");
        if (response == null || !response.startsWith("OK|ADMIN_DASHBOARD|")) {
            return new AdminDashboardStats(0, 0, 0, 0, 0, 0, 0);
        }

        String[] parts = response.split("\\|", 3);
        if (parts.length < 3) {
            return new AdminDashboardStats(0, 0, 0, 0, 0, 0, 0);
        }

        String[] fields = parts[2].split(",", -1);
        if (fields.length < 7) {
            return new AdminDashboardStats(0, 0, 0, 0, 0, 0, 0);
        }

        return new AdminDashboardStats(
                parseInt(fields[0]),
                parseInt(fields[1]),
                parseInt(fields[2]),
                parseInt(fields[3]),
                parseInt(fields[4]),
                parseInt(fields[5]),
                parseInt(fields[6])
        );
    }

    public List<AdminUserRow> getUsers() {
        String response = sendAdminCommand("ADMIN_LIST_USERS");
        List<AdminUserRow> users = new ArrayList<>();
        if (isEmptyResponse(response, "OK|ADMIN_USERS|EMPTY") || !response.startsWith("OK|ADMIN_USERS|")) {
            return users;
        }

        String[] records = response.split("\\|");
        for (int i = 2; i < records.length; i++) {
            AdminUserRow user = parseUser(records[i]);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    public List<AuctionListDTO> getAuctions() {
        String response = sendAdminCommand("ADMIN_LIST_AUCTIONS");
        List<AuctionListDTO> auctions = new ArrayList<>();
        if (isEmptyResponse(response, "OK|ADMIN_AUCTIONS|EMPTY") || !response.startsWith("OK|ADMIN_AUCTIONS|")) {
            return auctions;
        }

        String[] records = response.split("\\|");
        for (int i = 2; i < records.length; i++) {
            AuctionListDTO auction = parseAuction(records[i]);
            if (auction != null) {
                auctions.add(auction);
            }
        }
        return auctions;
    }

    public List<BidHistoryDTO> getBids() {
        String response = sendAdminCommand("ADMIN_LIST_BIDS");
        List<BidHistoryDTO> bids = new ArrayList<>();
        if (isEmptyResponse(response, "OK|ADMIN_BIDS|EMPTY") || !response.startsWith("OK|ADMIN_BIDS|")) {
            return bids;
        }

        String[] records = response.split("\\|");
        for (int i = 2; i < records.length; i++) {
            BidHistoryDTO bid = parseBid(records[i]);
            if (bid != null) {
                bids.add(bid);
            }
        }
        return bids;
    }

    private String sendAdminCommand(String command) {
        try {
            return SocketClientService.sendSessionCommand(command);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tải dữ liệu admin từ server.", e);
        }
    }

    private AdminUserRow parseUser(String record) {
        String[] fields = record.split(",", -1);
        if (fields.length < 6) {
            return null;
        }
        return new AdminUserRow(
                fields[0],
                fields[1],
                fields[2],
                fields[3],
                parseDouble(fields[4]),
                parseDouble(fields[5])
        );
    }

    private AuctionListDTO parseAuction(String record) {
        String[] fields = record.split(",", -1);
        if (fields.length < 12) {
            return null;
        }

        try {
            AuctionListDTO auction = new AuctionListDTO();
            auction.setAuctionId(fields[0]);
            auction.setItemId(fields[1]);
            auction.setItemType(fields[2]);
            auction.setName(fields[3]);
            auction.setCurrentPrice(parseDouble(fields[4]));
            auction.setAuctionStatus(Status.valueOf(fields[5]));
            auction.setStartDateTime(fields[6]);
            auction.setEndDateTime(fields[7]);
            auction.setCondition(emptyToNull(fields[8]));
            auction.setDescription(emptyToNull(fields[9]));
            auction.setWarranty(emptyToNull(fields[10]));
            auction.setMinIncrement(parseDouble(fields[11]));
            return auction;
        } catch (Exception e) {
            return null;
        }
    }

    private BidHistoryDTO parseBid(String record) {
        String[] fields = record.split(",", -1);
        if (fields.length < 7) {
            return null;
        }

        return new BidHistoryDTO(
                fields[0],
                fields[1],
                emptyToNull(fields[2]),
                emptyToNull(fields[3]),
                fields[4],
                parseDouble(fields[5]),
                fields[6]
        );
    }

    private boolean isEmptyResponse(String response, String emptyResponse) {
        return response == null || response.isBlank() || response.startsWith("ERR|") || emptyResponse.equals(response);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
