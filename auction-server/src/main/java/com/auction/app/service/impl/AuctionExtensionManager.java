package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionExtensionManager {
    private static final int DEFAULT_EXTENSION_THRESHOLD_SECONDS = 10;
    private static final int DEFAULT_EXTENSION_DURATION_SECONDS = 60;

    public static boolean checkAndExtend(Auction auction) {
        return checkAndExtend(auction, DEFAULT_EXTENSION_THRESHOLD_SECONDS, DEFAULT_EXTENSION_DURATION_SECONDS);
    }

    public static boolean checkAndExtend(Auction auction, long thresholdSeconds, long extensionSeconds) {
        if (auction == null || auction.getItem() == null) {
            return false;
        }

        if (thresholdSeconds <= 0 || extensionSeconds <= 0) {
            return false;
        }

        String endDateString = auction.getItem().getEndDateString();
        if (endDateString == null || endDateString.isBlank()) {
            return false;
        }

        try {
            LocalDateTime endTime = parseDateTime(endDateString);
            LocalDateTime now = LocalDateTime.now();

            long secondsRemaining = java.time.temporal.ChronoUnit.SECONDS.between(now, endTime);
            if (secondsRemaining >= 0 && secondsRemaining <= thresholdSeconds) {
                LocalDateTime newEndTime = endTime.plusSeconds(extensionSeconds);
                String newEndDateString = newEndTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                auction.getItem().setEndDateString(newEndDateString);

                System.out.println("Auction extended by " + extensionSeconds + " seconds");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error checking auction extension: " + e.getMessage());
        }

        return false;
    }

    /**
     * Parse thời gian từ string (hỗ trợ nhiều định dạng)
     *
     * @param dateTimeString String chứa thời gian
     * @return LocalDateTime đã parse
     */
    private static LocalDateTime parseDateTime(String dateTimeString) {
        try {
            // Nếu có "|" thì lấy phần đầu (trước "|")
            String dateTimePart = dateTimeString.contains("|")
                    ? dateTimeString.split("\\|")[0]
                    : dateTimeString;

            try {
                // ✓ Cố gắng parse theo ISO_LOCAL_DATE_TIME (định dạng tiêu chuẩn)
                return LocalDateTime.parse(dateTimePart, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                // ✓ Định dạng thay thế: "yyyy-MM-dd HH:mm:ss"
                return LocalDateTime.parse(dateTimePart,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse datetime: " + dateTimeString, e);
        }
    }

    /**
     * Lấy thời gian còn lại trước khi phiên kết thúc
     *
     * @param auction Phiên cần kiểm tra
     * @return Số giây còn lại, hoặc -1 nếu đã kết thúc
     */
    public static long getTimeRemaining(Auction auction) {
        if (auction == null || auction.getItem() == null) {
            return -1;
        }

        try {
            LocalDateTime endTime = parseDateTime(auction.getItem().getEndDateString());
            LocalDateTime now = LocalDateTime.now();

            // Nếu đã qua hết giờ rồi
            if (now.isAfter(endTime)) {
                return -1;
            }

            // Tính số giây còn lại
            return java.time.temporal.ChronoUnit.SECONDS.between(now, endTime);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Kiểm tra phiên có được phép kết thúc không
     * Phiên chỉ kết thúc được khi hết thời điểm end_time
     *
     * @param auction Phiên cần kiểm tra
     * @return true nếu phiên đã hết giờ, false nếu còn thời gian
     */
    public static boolean shouldClose(Auction auction) {
        return getTimeRemaining(auction) <= 0;
    }
}


