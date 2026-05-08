package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.auction.app.service.AuctionService;

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
        if (started) {
            return;
        }

        scheduler.scheduleAtFixedRate(
                () -> closeExpiredAuctions(auctionService),
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

    private void closeExpiredAuctions(AuctionService auctionService) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Auction> activeAuctions = auctionService.getActiveAuctions();

            for (Auction auction : activeAuctions) {
                try {
                    if (auction.getItem() == null) {
                        System.err.println("Cảnh báo auto-close: phiên đấu giá " + auction.getId() + " không có item");
                        continue;
                    }

                    LocalDateTime endTime = parseEndTime(auction.getItem().getEndDateString());
                    if (endTime != null && !now.isBefore(endTime)) {
                        auctionService.endAuction(auction.getId());
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi auto-close khi xử lý phiên " + auction.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi auto-close chung: " + e.getMessage());
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
