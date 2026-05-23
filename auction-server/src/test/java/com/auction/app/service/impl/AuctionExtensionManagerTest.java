package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.Art;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionExtensionManagerTest {

    @Test
    public void testExtendWhenWithinThreshold() {
        LocalDateTime now = LocalDateTime.now();
        // Auction end in 200s which is within default threshold 300s
        LocalDateTime end = now.plusSeconds(200);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Art art = new Art("desc", "name", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), endStr, 100.0, 1.0, "author");
        Auction auction = new Auction(art);

        String originalEnd = auction.getItem().getEndDateString();
        boolean extended = AuctionExtensionManager.checkAndExtend(auction);

        assertTrue(extended, "Auction should be extended when within threshold");
        assertNotNull(auction.getItem().getEndDateString());
        assertNotEquals(originalEnd, auction.getItem().getEndDateString(), "End date should be updated when extended");
    }

    @Test
    public void testNotExtendWhenOutsideThreshold() {
        LocalDateTime now = LocalDateTime.now();
        // Auction end in 600s which is outside default threshold 300s
        LocalDateTime end = now.plusSeconds(600);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Art art = new Art("desc", "name", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), endStr, 100.0, 1.0, "author");
        Auction auction = new Auction(art);

        String originalEnd = auction.getItem().getEndDateString();
        boolean extended = AuctionExtensionManager.checkAndExtend(auction);

        assertFalse(extended, "Auction should not be extended when outside threshold");
        assertEquals(originalEnd, auction.getItem().getEndDateString(), "End date should remain the same when not extended");
    }
}

