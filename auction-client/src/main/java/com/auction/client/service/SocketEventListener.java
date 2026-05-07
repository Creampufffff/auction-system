package com.auction.client.service;

import com.app.common.enums.Status;
import com.auction.client.model.ProductDataManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketEventListener implements Runnable {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    public static void ensureStarted() {
        if (STARTED.compareAndSet(false, true)) {
            Thread thread = new Thread(new SocketEventListener());
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public void run() {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            reader.readLine(); // OK|CONNECTED|Type HELP for commands

            String line;
            while ((line = reader.readLine()) != null) {
                handleEvent(line.trim());
            }
        } catch (IOException e) {
            // Best-effort: listener can be restarted on next UI entry.
            STARTED.set(false);
        }
    }

    private void handleEvent(String line) {
        if (line == null || line.isBlank() || !line.startsWith("EVENT|")) {
            return;
        }

        String[] parts = line.split("\\|");
        if (parts.length < 3) {
            return;
        }

        String eventType = parts[1];
        switch (eventType) {
            case "BID_UPDATED":
                handleBidUpdated(parts);
                break;
            case "AUCTION_STARTED":
                handleAuctionStatus(parts, Status.RUNNING);
                break;
            case "AUCTION_ENDED":
                handleAuctionStatus(parts, Status.FINISHED);
                break;
            default:
                break;
        }
    }

    private void handleBidUpdated(String[] parts) {
        if (parts.length < 4) {
            return;
        }

        String auctionId = parts[2];
        try {
            double bidAmount = Double.parseDouble(parts[3]);
            ProductDataManager.getInstance().updateAuctionPrice(auctionId, bidAmount);
        } catch (NumberFormatException ignored) {
            // Ignore malformed bid amount.
        }
    }

    private void handleAuctionStatus(String[] parts, Status status) {
        if (parts.length < 3) {
            return;
        }

        String auctionId = parts[2];
        ProductDataManager.getInstance().updateAuctionStatus(auctionId, status);
    }
}

