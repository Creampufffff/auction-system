package com.auction.client.service;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;

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
}
