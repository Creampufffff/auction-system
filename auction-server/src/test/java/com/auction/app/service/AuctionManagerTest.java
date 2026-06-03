package com.auction.app.service;

import com.app.common.entity.Art;
import com.app.common.entity.Auction;
import com.app.common.enums.Status;
import com.auction.app.service.impl.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Trình quản lý Đồng bộ trạng thái Phiên đấu giá (AuctionManager)")
class AuctionManagerTest {

    @Mock
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Dùng trick "Năm 2000" (quá khứ) và "Năm 2099" (tương lai) thay vì mock LocalDateTime
    }

    // ==========================================
    // 1. TEST CASE CHO LOGIC ĐÓNG PHIÊN (END AUCTION)
    // ==========================================

    @Test
    @DisplayName("syncAuctionStatuses: Đóng phiên đấu giá khi đã qua thời gian kết thúc (Năm 2000)")
    void testSync_TimeIsUp_EndsAuction() throws Exception {
        // Given: Kết thúc vào năm 2000 -> Đã hết hạn
        Auction expiredAuction = createRealAuction("auction-1", "1999-01-01T00:00:00", "2000-01-01T00:00:00", Status.RUNNING);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(expiredAuction));

        // When
        invokeSyncAuctionStatuses();

        // Then
        verify(auctionService, times(1)).endAuction("auction-1");
        verify(auctionService, never()).startAuction(anyString());
    }

    @Test
    @DisplayName("syncAuctionStatuses: Không làm gì nếu chưa đến thời gian kết thúc và bắt đầu (Năm 2099)")
    void testSync_TimeNotUpYet_DoesNothing() throws Exception {
        // Given: Cả startTime và endTime đều ở năm 2099 -> Chưa đến lúc chạy
        Auction futureAuction = createRealAuction("auction-2", "2099-01-01T00:00:00", "2099-12-31T00:00:00", Status.OPEN);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(futureAuction));

        // When
        invokeSyncAuctionStatuses();

        // Then: KHÔNG gọi endAuction và cũng KHÔNG gọi startAuction
        verify(auctionService, never()).endAuction(anyString());
        verify(auctionService, never()).startAuction(anyString());
    }

    // ==========================================
    // 2. TEST CASE CHO LOGIC MỞ PHIÊN (START AUCTION)
    // ==========================================

    @Test
    @DisplayName("syncAuctionStatuses: Tự động khởi chạy phiên khi đến giờ startTime")
    void testSync_StartTimeArrived_StatusOpen_StartsAuction() throws Exception {
        // Given: startTime là năm 2000 (đã đến lúc chạy), endTime là 2099 (chưa hết hạn), trạng thái OPEN
        Auction readyToStartAuction = createRealAuction("auction-start", "2000-01-01T00:00:00", "2099-01-01T00:00:00", Status.OPEN);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(readyToStartAuction));

        // When
        invokeSyncAuctionStatuses();

        // Then: Phải kích hoạt hàm startAuction
        verify(auctionService, times(1)).startAuction("auction-start");
        verify(auctionService, never()).endAuction(anyString());
    }

    // ==========================================
    // 3. TEST CASE NGOẠI LỆ & BẢO VỆ CHƯƠNG TRÌNH
    // ==========================================

    @Test
    @DisplayName("syncAuctionStatuses: Bỏ qua lỗi của 1 phiên và tiếp tục xử lý các phiên còn lại")
    void testSync_ExceptionHandling_ContinuesProcessing() throws Exception {
        // Given: Một auction bị lỗi mất dữ liệu Item
        Auction errorAuction = new Auction(null);
        errorAuction.setId("error-auction");
        errorAuction.setAuctionStatus(Status.RUNNING);

        // Một auction bình thường đã hết hạn
        Auction normalExpiredAuction = createRealAuction("good-auction", "1999-01-01T00:00:00", "2000-01-01T00:00:00", Status.RUNNING);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(errorAuction, normalExpiredAuction));

        // When
        invokeSyncAuctionStatuses();

        // Then: Dù phiên đầu lỗi, phiên sau vẫn phải được xử lý thành công
        verify(auctionService, times(1)).endAuction("good-auction");
    }

    @Test
    @DisplayName("syncAuctionStatuses: Hoạt động an toàn khi danh sách rỗng")
    void testSync_EmptyList_DoesNothing() throws Exception {
        when(auctionService.getActiveAuctions()).thenReturn(Collections.emptyList());

        invokeSyncAuctionStatuses();

        verify(auctionService, never()).endAuction(anyString());
        verify(auctionService, never()).startAuction(anyString());
    }

    // ==========================================
    // HÀM HỖ TRỢ (HELPER METHODS)
    // ==========================================

    private void invokeSyncAuctionStatuses() throws Exception {
        // Dùng reflection gọi hàm private 'syncAuctionStatuses'
        Method method = AuctionManager.class.getDeclaredMethod(
                "syncAuctionStatuses",
                AuctionService.class,
                com.auction.app.socket.AuctionSocketServer.class
        );
        method.setAccessible(true);
        // Truyền null cho SocketServer vì trong test logic ta chỉ cần kiểm tra Service
        method.invoke(AuctionManager.getInstance(), auctionService, null);
    }

    private Auction createRealAuction(String auctionId, String startDate, String endDate, Status status) {
        Art realItem = new Art(
                "Mô tả", "Bức tranh test", startDate, endDate,
                100.0, 10.0, "Picasso"
        );
        realItem.setId("item-" + auctionId);

        Auction realAuction = new Auction(realItem);
        realAuction.setId(auctionId);
        realAuction.setAuctionStatus(status);
        return realAuction;
    }
}