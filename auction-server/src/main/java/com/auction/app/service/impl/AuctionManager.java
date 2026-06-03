package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.enums.Status;
import com.auction.app.service.AuctionService;
import com.auction.app.socket.AuctionSocketServer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    private static final long CHECK_INTERVAL_SECONDS = 5;
    private static final List<DateTimeFormatter> SUPPORTED_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    );

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean started;

    private AuctionManager() {

    }

    private static final class AuctionManagerInstanceHolder {
        private static final AuctionManager AuctionManagerInstance = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return AuctionManagerInstanceHolder.AuctionManagerInstance;
    }

    public synchronized void startStatusSync(AuctionService auctionService) {
        startStatusSync(auctionService, null);
    }

    public synchronized void startStatusSync(AuctionService auctionService, AuctionSocketServer socketServer) {
        if (started) {
            return;
        }

        scheduler.scheduleAtFixedRate(
                () -> syncAuctionStatuses(auctionService, socketServer),
                0,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        started = true;
    }

    public synchronized void stopStatusSync() {
        scheduler.shutdownNow();
        started = false;
    }

    private void syncAuctionStatuses(AuctionService auctionService, AuctionSocketServer socketServer) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Auction> activeAuctions = auctionService.getActiveAuctions();

            for (Auction auction : activeAuctions) {
                try {
                    if (auction.getItem() == null) {
                        System.err.println("Auction status sync warning: auction " + auction.getId() + " has no item");
                        continue;
                    }

                    LocalDateTime startTime = parseStartTime(auction.getItem().getStartDateString());
                    LocalDateTime endTime = parseEndTime(auction.getItem().getEndDateString());

                    // DEBUG: Print auction status details
                    System.out.println("[AuctionManager] Checking auction: " + auction.getId()
                            + " | Status: " + auction.getAuctionStatus()
                            + " | Now: " + now
                            + " | StartDtString: " + auction.getItem().getStartDateString()
                            + " | StartTime: " + startTime
                            + " | EndDtString: " + auction.getItem().getEndDateString()
                            + " | EndTime: " + endTime);

                    // End time wins first, so an expired OPEN auction is not started accidentally.
                    if (endTime != null && !now.isBefore(endTime)) {
                        System.out.println("[AuctionManager] Ending auction " + auction.getId() + " (endTime passed)");
                        auctionService.endAuction(auction.getId());
                        if (socketServer != null) {
                            socketServer.broadcast("EVENT|AUCTION_ENDED|" + auction.getId());
                        }
                        continue;
                    }

                    // Auto-start auctions once their configured start time has arrived.
                    if (auction.getAuctionStatus() == Status.OPEN
                            && startTime != null
                            && !now.isBefore(startTime)) {
                        System.out.println("[AuctionManager] Starting auction " + auction.getId() + " (startTime arrived)");
                        auctionService.startAuction(auction.getId());
                        if (socketServer != null) {
                            socketServer.broadcast("EVENT|AUCTION_STARTED|" + auction.getId());
                        }
                    } else if (auction.getAuctionStatus() == Status.OPEN) {
                        System.out.println("[AuctionManager] Auction " + auction.getId()
                                + " NOT started: startTime=" + startTime
                                + " | isBefore(now): " + (startTime != null ? now.isBefore(startTime) : "N/A"));
                    }
                } catch (Exception e) {
                    System.err.println("Auction status sync error when processing auction " + auction.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Auction status sync general error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LocalDateTime parseStartTime(String startDateString) {
        LocalDateTime dateTime = parseDateTime(startDateString);
        if (dateTime != null) {
            return dateTime;
        }

        try {
            return LocalDate.parse(startDateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseEndTime(String endDateString) {
        LocalDateTime dateTime = parseDateTime(endDateString);
        if (dateTime != null) {
            return dateTime;
        }

        try {
            return LocalDate.parse(endDateString, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        if (value.contains("|")) {
            value = value.split("\\|", 2)[0];
        }

        for (DateTimeFormatter formatter : SUPPORTED_FORMATS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {

            }
        }

        return null;
    }

}
