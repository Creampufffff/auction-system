package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionExtensionManager {
    // ========== CẤU HÌNH THỜI GIAN ==========
    private static final int EXTENSION_THRESHOLD_SECONDS = 300;  // 5 phút - mốc phát hiện sniping
    private static final int EXTENSION_DURATION_SECONDS = 300;   // 5 phút - thời gian gia hạn
    private static final int MAX_EXTENSIONS = 3;                  // Tối đa gia hạn 3 lần

    public static boolean checkAndExtend(Auction auction) {
        if (auction == null || auction.getItem() == null) {
            return false;
        }

        String endDateString = auction.getItem().getEndDateString();
        if (endDateString == null || endDateString.isBlank()) {
            return false;
        }

        try {
            // ========== BƯỚC 1: Parse thời gian kết thúc ==========
            LocalDateTime endTime = parseDateTime(endDateString);
            LocalDateTime now = LocalDateTime.now();

            // Mốc 5 phút trước hết giờ
            LocalDateTime thresholdTime = endTime.minusSeconds(EXTENSION_THRESHOLD_SECONDS);

            // ========== BƯỚC 2: Kiểm tra nếu hiện tại ở trong 5 phút cuối ==========
            if (now.isAfter(thresholdTime) && now.isBefore(endTime)) {
                // ========== BƯỚC 3: Kiểm tra còn được gia hạn không ==========
                int extensionCount = getExtensionCount(auction);
                if (extensionCount < MAX_EXTENSIONS) {
                    // ========== BƯỚC 4: Gia hạn phiên ==========
                    LocalDateTime newEndTime = endTime.plusSeconds(EXTENSION_DURATION_SECONDS);
                    String newEndDateString = newEndTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    auction.getItem().setEndDateString(newEndDateString);

                    System.out.println("⏱️  Phiên được gia hạn thêm 5 phút (lần " + (extensionCount + 1) + "/3)");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi kiểm tra gia hạn phiên: " + e.getMessage());
        }

        return false;
    }

    /**
     * Lấy số lần phiên đã được gia hạn
     *
     * Cách lưu: "2026-05-08T15:30:00|2"
     * - Phần 1: Thời gian kết thúc
     * - Phần 2: Số lần gia hạn
     *
     * @param auction Phiên cần kiểm tra
     * @return Số lần gia hạn
     */
    private static int getExtensionCount(Auction auction) {
        String endDateString = auction.getItem().getEndDateString();

        // Nếu có ký hiệu "|" thì có thông tin gia hạn
        if (endDateString.contains("|")) {
            try {
                String[] parts = endDateString.split("\\|");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1]);
                }
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;  // Mới tạo, chưa gia hạn lần nào
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
            throw new IllegalArgumentException("Không thể parse thời gian: " + dateTimeString, e);
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


