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

    public synchronized void startAutoClose(AuctionService auctionService) {
        startAutoClose(auctionService, null);
    }

    public synchronized void startAutoClose(AuctionService auctionService, AuctionSocketServer socketServer) {
        if (started) {
            return;
        }

        scheduler.scheduleAtFixedRate(
                () -> closeExpiredAuctions(auctionService, socketServer),
                0,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        started = true;
    }

    public synchronized void stopAutoClose() {
        scheduler.shutdownNow();
        started = false;
    }

    private void closeExpiredAuctions(AuctionService auctionService, AuctionSocketServer socketServer) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Auction> activeAuctions = auctionService.getActiveAuctions();

            for (Auction auction : activeAuctions) {
                try {
                    if (auction.getAuctionStatus() != Status.RUNNING) {
                        continue;
                    }

                    if (auction.getItem() == null) {
                        System.err.println("Auto-close warning: auction " + auction.getId() + " has no item");
                        continue;
                    }

                    LocalDateTime endTime = parseEndTime(auction.getItem().getEndDateString());
                    if (endTime != null && !now.isBefore(endTime)) {
                        auctionService.endAuction(auction.getId());
                        if (socketServer != null) {
                            socketServer.broadcast("EVENT|AUCTION_ENDED|" + auction.getId());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Auto-close error when processing auction " + auction.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Auto-close general error: " + e.getMessage());
        }
    }

    private LocalDateTime parseEndTime(String endDateString) {
        if (endDateString == null || endDateString.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : SUPPORTED_FORMATS) {
            try {
                return LocalDateTime.parse(endDateString, formatter);
            } catch (DateTimeParseException ignored) {

            }
        }

        try {
            return LocalDate.parse(endDateString, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

}
