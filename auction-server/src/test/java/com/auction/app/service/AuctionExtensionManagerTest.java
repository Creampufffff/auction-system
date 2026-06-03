package com.auction.app.service;

import com.app.common.entity.Art;
import com.app.common.entity.Auction;
import com.app.common.entity.Item;
import com.auction.app.service.impl.AuctionExtensionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử AuctionExtensionManager (Anti-sniping)")
class AuctionExtensionManagerTest {

    private Auction auction;
    private Item item;

    @BeforeEach
    void setUp() {
        item = new Art("Desc", "Bức tranh", "2026-05-31T20:00:00", "2026-05-31T22:00:00", 100, 10, "Author");
        auction = new Auction(item);
    }

    private String formatTime(LocalDateTime time) {
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // =========================================================================
    // 1. TEST LOGIC CHECK VÀ GIA HẠN (CHECK AND EXTEND)
    // =========================================================================

    @Test
    @DisplayName("checkAndExtend: KHÔNG gia hạn nếu thời gian hiện tại CÒN NHIỀU HƠN 5 phút")
    void checkAndExtend_TimeGreaterThan5Minutes_ShouldNotExtend() {
        // Thiết lập end time = Hiện tại + 10 phút
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(10);
        String expectedEndTime = formatTime(futureTime);
        item.setEndDateString(expectedEndTime);

        boolean isExtended = AuctionExtensionManager.checkAndExtend(auction);

        assertFalse(isExtended, "Không được phép gia hạn khi thời gian còn nhiều hơn 5 phút");
        assertEquals(expectedEndTime, item.getEndDateString(), "Thời gian kết thúc không được thay đổi");
    }

    @Test
    @DisplayName("checkAndExtend: GIA HẠN THÀNH CÔNG nếu hiện tại NẰM TRONG 5 phút cuối")
    void checkAndExtend_TimeWithin5Minutes_ShouldExtend() {
        // Thiết lập end time = Hiện tại + 3 phút (Nằm trong vùng 5 phút chót)
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(3);
        item.setEndDateString(formatTime(futureTime));

        boolean isExtended = AuctionExtensionManager.checkAndExtend(auction);

        assertTrue(isExtended, "Phải gia hạn thành công khi thời gian còn dưới 5 phút");

        // Thời gian mới phải = Thời gian cũ + 5 phút (300 giây)
        LocalDateTime expectedNewEndTime = futureTime.plusMinutes(5);

        // Do có độ trễ mili-giây khi chạy test, ta parse ra và so sánh chuỗi
        String actualEndTime = item.getEndDateString();
        assertTrue(actualEndTime.startsWith(formatTime(expectedNewEndTime).substring(0, 16)),
                "Thời gian kết thúc phải được cộng thêm 5 phút");
    }

    @Test
    @DisplayName("checkAndExtend: KHÔNG gia hạn nếu đã quá số lần cho phép (Max = 3)")
    void checkAndExtend_MaxExtensionsReached_ShouldNotExtend() {
        // Thiết lập end time = Hiện tại + 3 phút, NHƯNG đã gia hạn 3 lần ("|3")
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(3);
        String expectedEndTime = formatTime(futureTime) + "|3";
        item.setEndDateString(expectedEndTime);

        boolean isExtended = AuctionExtensionManager.checkAndExtend(auction);

        assertFalse(isExtended, "Không được phép gia hạn vì đã đạt giới hạn 3 lần");
        assertEquals(expectedEndTime, item.getEndDateString());
    }

    @Test
    @DisplayName("checkAndExtend: Trả về false nếu Auction hoặc Item bị Null")
    void checkAndExtend_NullInputs_ShouldReturnFalse() {
        assertFalse(AuctionExtensionManager.checkAndExtend(null));
        assertFalse(AuctionExtensionManager.checkAndExtend(new Auction(null)));
    }

    // =========================================================================
    // 2. TEST TÍNH TOÁN THỜI GIAN CÒN LẠI (GET TIME REMAINING)
    // =========================================================================

    @Test
    @DisplayName("getTimeRemaining: Tính đúng số giây còn lại")
    void getTimeRemaining_ValidTime_ShouldReturnSeconds() {
        // Hiện tại + 30 giây
        LocalDateTime futureTime = LocalDateTime.now().plusSeconds(30);
        item.setEndDateString(formatTime(futureTime));

        long secondsLeft = AuctionExtensionManager.getTimeRemaining(auction);

        // Do thời gian chạy test mất vài mili-giây, kết quả có thể là 30 hoặc 29
        assertTrue(secondsLeft == 30 || secondsLeft == 29, "Số giây còn lại phải xấp xỉ 30");
    }

    @Test
    @DisplayName("getTimeRemaining: Trả về -1 nếu thời gian hiện tại ĐÃ QUÁ thời gian kết thúc")
    void getTimeRemaining_TimeAlreadyPassed_ShouldReturnMinusOne() {
        // Quá khứ 1 phút
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);
        item.setEndDateString(formatTime(pastTime));

        long secondsLeft = AuctionExtensionManager.getTimeRemaining(auction);

        assertEquals(-1, secondsLeft);
    }

    @Test
    @DisplayName("getTimeRemaining: Xử lý đúng định dạng thời gian chứa số lần gia hạn (Pipe | separator)")
    void getTimeRemaining_WithExtensionCountFormat_ShouldParseCorrectly() {
        // Hiện tại + 60 giây, kèm theo chuỗi |2
        LocalDateTime futureTime = LocalDateTime.now().plusSeconds(60);
        item.setEndDateString(formatTime(futureTime) + "|2");

        long secondsLeft = AuctionExtensionManager.getTimeRemaining(auction);

        assertTrue(secondsLeft == 60 || secondsLeft == 59);
    }

    // =========================================================================
    // 3. TEST LOGIC KẾT THÚC PHIÊN (SHOULD CLOSE)
    // =========================================================================

    @Test
    @DisplayName("shouldClose: Trả về true nếu thời gian còn lại <= 0")
    void shouldClose_TimeEnded_ShouldReturnTrue() {
        // Quá khứ 10 giây
        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(10);
        item.setEndDateString(formatTime(pastTime));

        assertTrue(AuctionExtensionManager.shouldClose(auction));
    }

    @Test
    @DisplayName("shouldClose: Trả về false nếu thời gian vẫn còn")
    void shouldClose_TimeRemaining_ShouldReturnFalse() {
        // Tương lai 1 tiếng
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        item.setEndDateString(formatTime(futureTime));

        assertFalse(AuctionExtensionManager.shouldClose(auction));
    }
}