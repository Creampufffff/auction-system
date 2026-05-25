package java.com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.Item;
import com.auction.app.service.impl.AuctionExtensionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionExtensionManagerTest {

    private Auction mockAuction;
    private Item mockItem;
    private MockedStatic<LocalDateTime> mockedLocalDateTime;

    // Lấy một mốc thời gian cố định làm chuẩn: 10:00:00 sáng ngày 23/05/2026
    private final String BASE_END_TIME = "2026-05-23T10:00:00";

    @BeforeEach
    void setUp() {
        mockAuction = mock(Auction.class);
        mockItem = mock(Item.class);
        when(mockAuction.getItem()).thenReturn(mockItem);

        // Mở Mock cho LocalDateTime (chỉ mock hàm now(), giữ nguyên các hàm khác như parse)
        mockedLocalDateTime = mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        // Bắt buộc phải đóng Mock sau mỗi test để không làm hỏng thời gian của máy ở các test khác
        mockedLocalDateTime.close();
    }

    // ==========================================
    // TEST HÀM CHECK_AND_EXTEND (ANTI-SNIPING)
    // ==========================================

    @Test
    void testCheckAndExtend_Outside5Minutes_DoesNotExtend() {
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME);

        // Giả lập thời gian hiện tại là 09:50:00 (còn 10 phút, nằm ngoài mốc 5 phút cuối)
        setFakeCurrentTime("2026-05-23T09:50:00");

        boolean isExtended = AuctionExtensionManager.checkAndExtend(mockAuction);

        assertFalse(isExtended, "Không được gia hạn nếu thời gian còn lớn hơn 5 phút");
        verify(mockItem, never()).setEndDateString(anyString());
    }

    @Test
    void testCheckAndExtend_Inside5Minutes_ExtendsSuccessfully() {
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME);

        // Giả lập thời gian hiện tại là 09:57:00 (còn 3 phút, nằm trong mốc 5 phút cuối)
        setFakeCurrentTime("2026-05-23T09:57:00");

        boolean isExtended = AuctionExtensionManager.checkAndExtend(mockAuction);

        assertTrue(isExtended, "Phải gia hạn thành công khi có người bid trong 5 phút cuối");

        // Dùng ArgumentCaptor để "tóm" lấy chuỗi thời gian mới được set vào Item
        ArgumentCaptor<String> dateCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockItem).setEndDateString(dateCaptor.capture());

        // Kiểm tra xem thời gian mới có cộng đúng 5 phút và có hậu tố |1 không
        assertEquals("2026-05-23T10:05:00|1", dateCaptor.getValue());
    }

    @Test
    void testCheckAndExtend_AlreadyMaxExtensions_DoesNotExtend() {
        // Đã gia hạn 3 lần (|3)
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME + "|3");

        // Có người tiếp tục bid vào phút chót (09:58:00)
        setFakeCurrentTime("2026-05-23T09:58:00");

        boolean isExtended = AuctionExtensionManager.checkAndExtend(mockAuction);

        assertFalse(isExtended, "Không được gia hạn khi đã vượt quá MAX_EXTENSIONS (3 lần)");
        verify(mockItem, never()).setEndDateString(anyString()); // Không set ngày mới
    }

    @Test
    void testCheckAndExtend_AfterEndTime_DoesNotExtend() {
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME);

        // Giả lập thời gian hiện tại là 10:01:00 (Phiên đã qua giờ kết thúc)
        setFakeCurrentTime("2026-05-23T10:01:00");

        boolean isExtended = AuctionExtensionManager.checkAndExtend(mockAuction);

        assertFalse(isExtended, "Không được gia hạn nếu phiên đã hết giờ");
    }

    // ==========================================
    // TEST CÁC HÀM TIỆN ÍCH (TIME REMAINING / SHOULD CLOSE)
    // ==========================================

    @Test
    void testGetTimeRemaining_AuctionRunning_ReturnsSeconds() {
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME);

        // Giả lập thời gian hiện tại là 09:59:00 (còn 60 giây)
        setFakeCurrentTime("2026-05-23T09:59:00");

        long remaining = AuctionExtensionManager.getTimeRemaining(mockAuction);

        assertEquals(60, remaining);
    }

    @Test
    void testGetTimeRemaining_AuctionEnded_ReturnsMinusOne() {
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME);

        // Thời gian hiện tại là 10:05:00 (qua giờ)
        setFakeCurrentTime("2026-05-23T10:05:00");

        long remaining = AuctionExtensionManager.getTimeRemaining(mockAuction);

        assertEquals(-1, remaining);
    }

    @Test
    void testShouldClose_TimeRemainingIsZeroOrLess_ReturnsTrue() {
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME);

        // Đúng mốc kết thúc
        setFakeCurrentTime(BASE_END_TIME);
        assertTrue(AuctionExtensionManager.shouldClose(mockAuction));

        // Qua mốc kết thúc
        setFakeCurrentTime("2026-05-23T10:01:00");
        assertTrue(AuctionExtensionManager.shouldClose(mockAuction));
    }

    @Test
    void testShouldClose_TimeRemainingIsPositive_ReturnsFalse() {
        when(mockItem.getEndDateString()).thenReturn(BASE_END_TIME);

        // Còn 10 phút nữa mới kết thúc
        setFakeCurrentTime("2026-05-23T09:50:00");

        assertFalse(AuctionExtensionManager.shouldClose(mockAuction));
    }

    // Hàm hỗ trợ để set nhanh thời gian giả cho Mockito
    private void setFakeCurrentTime(String isoDateTime) {
        LocalDateTime fakeTime = LocalDateTime.parse(isoDateTime);
        mockedLocalDateTime.when(LocalDateTime::now).thenReturn(fakeTime);
    }
}