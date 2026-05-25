package java.com.auction.app.service.impl;

import com.app.common.entity.Art;
import com.app.common.entity.Auction;
import com.app.common.enums.Status;
import com.auction.app.service.AuctionService;
import com.auction.app.service.impl.AuctionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuctionManagerTest {

    @Mock
    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ĐÃ XÓA TOÀN BỘ CODE MOCK THỜI GIAN ĐỂ TRÁNH LỖI BẢO MẬT JAVA
    }

    // ==========================================
    // CÁC TEST CASE CHO LOGIC ĐÓNG PHIÊN
    // ==========================================

    @Test
    void testCloseExpiredAuctions_TimeIsUp_EndsAuction() throws Exception {
        // MẸO: Cố tình set năm 2000 để chắc chắn nó luôn là quá khứ so với thời gian máy tính hiện tại
        Auction expiredAuction = createRealAuction("auction-1", "2000-01-01T00:00:00", Status.RUNNING);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(expiredAuction));

        invokeCloseExpiredAuctions();

        verify(auctionService, times(1)).endAuction("auction-1");
    }

    @Test
    void testCloseExpiredAuctions_TimeNotUpYet_DoesNothing() throws Exception {
        // MẸO: Cố tình set năm 2099 để chắc chắn nó luôn là tương lai (chưa hết hạn)
        Auction runningAuction = createRealAuction("auction-2", "2099-01-01T00:00:00", Status.RUNNING);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(runningAuction));

        invokeCloseExpiredAuctions();

        verify(auctionService, never()).endAuction(anyString());
    }

    @Test
    void testCloseExpiredAuctions_StatusNotRunning_IgnoresAuction() throws Exception {
        // Hết hạn (năm 2000) nhưng trạng thái mới chỉ là OPEN
        Auction openAuction = createRealAuction("auction-3", "2000-01-01T00:00:00", Status.OPEN);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(openAuction));

        invokeCloseExpiredAuctions();

        verify(auctionService, never()).endAuction(anyString());
    }

    @Test
    void testCloseExpiredAuctions_ExceptionHandling_ContinuesProcessing() throws Exception {
        Auction errorAuction = new Auction(null);
        errorAuction.setId("error-auction");
        errorAuction.setAuctionStatus(Status.RUNNING);

        Auction normalExpiredAuction = createRealAuction("good-auction", "2000-01-01T00:00:00", Status.RUNNING);

        when(auctionService.getActiveAuctions()).thenReturn(List.of(errorAuction, normalExpiredAuction));

        invokeCloseExpiredAuctions();

        verify(auctionService, times(1)).endAuction("good-auction");
    }

    @Test
    void testCloseExpiredAuctions_EmptyList_DoesNothing() throws Exception {
        when(auctionService.getActiveAuctions()).thenReturn(Collections.emptyList());

        invokeCloseExpiredAuctions();

        verify(auctionService, never()).endAuction(anyString());
    }

    // ==========================================
    // HÀM HỖ TRỢ (HELPER METHODS)
    // ==========================================

    private void invokeCloseExpiredAuctions() throws Exception {
        Method method = AuctionManager.class.getDeclaredMethod(
                "closeExpiredAuctions",
                AuctionService.class,
                com.auction.app.socket.AuctionSocketServer.class
        );
        method.setAccessible(true);
        method.invoke(AuctionManager.getInstance(), auctionService, null);
    }

    private Auction createRealAuction(String auctionId, String endDate, Status status) {
        Art realItem = new Art(
                "Mô tả", "Bức tranh", "1999-01-01T00:00:00", endDate,
                100.0, 10.0, "Picasso"
        );
        realItem.setId("item-" + auctionId);

        Auction realAuction = new Auction(realItem);
        realAuction.setId(auctionId);
        realAuction.setAuctionStatus(status);
        return realAuction;
    }
}