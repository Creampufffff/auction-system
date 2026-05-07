package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.Item;
import com.app.common.enums.Status;
import com.auction.app.service.AuctionService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionLifecycleManager {
    // quản lí vòng đời các phiên đấu giá
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuctionService auctionService;
    private final ScheduledExecutorService scheduler;

    public AuctionLifecycleManager(AuctionService auctionService) {
        this.auctionService = auctionService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::checkAuctionLifecycle,
                0,
                30,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void checkAuctionLifecycle() {
        try {
            List<Auction> auctions = auctionService.getAllAuction();
            LocalDateTime now = LocalDateTime.now();

            for (Auction auction : auctions) {
                updateAuctionStatusIfNeeded(auction, now);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAuctionStatusIfNeeded(Auction auction, LocalDateTime now) {
        if (auction == null || auction.getItem() == null) {
            return;
        }

        Item item = auction.getItem();
        LocalDateTime startTime = parseDateTime(item.getStartDateString());
        LocalDateTime endTime = parseDateTime(item.getEndDateString());

        if (startTime == null || endTime == null) {
            return;
        }

        if (auction.getAuctionStatus() == Status.OPEN
                && !now.isBefore(startTime)
                && now.isBefore(endTime)) {
            auctionService.startAuction(auction.getId());
            return;
        }

        if (auction.getAuctionStatus() == Status.RUNNING
                && !now.isBefore(endTime)) {
            auctionService.endAuction(auction.getId());
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid auction datetime: " + value);
            return null;
        }
    }
}
