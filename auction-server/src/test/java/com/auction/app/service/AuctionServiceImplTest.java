package com.auction.app.service;

import com.app.common.entity.Art;
import com.app.common.entity.Auction;
import com.app.common.enums.Status;
import com.app.common.exception.AuctionClosedException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.service.impl.AuctionExtensionManager;
import com.auction.app.service.impl.AuctionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Tầng Service của Phiên Đấu Giá (AuctionServiceImpl)")
class AuctionServiceImplTest {

    @Mock
    private AuctionDAO auctionDAO;
    @Mock
    private BidDAO bidDAO;
    @Mock
    private UserDAO userDAO;

    private AuctionServiceImpl auctionService;
    private MockedStatic<AuctionExtensionManager> mockedExtensionManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auctionService = new AuctionServiceImpl(auctionDAO, bidDAO, userDAO);
    }

    @AfterEach
    void tearDown() {
        if (mockedExtensionManager != null) {
            mockedExtensionManager.close();
        }
    }

    private Art sampleArt() {
        return new Art("description", "Painting", "2026-01-01", "2026-01-02", 100, 10, "Author");
    }

    // =========================================================================
    // 1. TEST HÀM TẠO PHIÊN (SAVE AUCTION)
    // =========================================================================

    @Test
    @DisplayName("saveAuction: Tự động gán trạng thái OPEN cho phiên mới và lưu thành công")
    void saveAuction_DefaultsStatusToOpen() {
        Auction auction = new Auction(sampleArt());
        auction.setAuctionStatus(null); // Giả lập trạng thái bị bỏ trống

        when(auctionDAO.save(any(Auction.class))).thenReturn(true);

        auctionService.saveAuction(auction);

        // Đảm bảo Service tự động gán là OPEN
        assertEquals(Status.OPEN, auction.getAuctionStatus(), "Phiên mới tạo phải mặc định là OPEN");
        verify(auctionDAO, times(1)).save(auction);
    }

    @Test
    @DisplayName("saveAuction: Bắt lỗi nếu truyền vào Auction null hoặc Item null")
    void saveAuction_NullInputs_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> auctionService.saveAuction(null));

        Auction auctionWithoutItem = new Auction(null);
        assertThrows(IllegalArgumentException.class, () -> auctionService.saveAuction(auctionWithoutItem));
    }

    // =========================================================================
    // 2. TEST HÀM KHỞI CHẠY PHIÊN (START AUCTION)
    // =========================================================================

    @Test
    @DisplayName("startAuction: Chuyển trạng thái từ OPEN sang RUNNING thành công")
    void startAuction_MovesOpenAuctionToRunning() {
        Auction auction = new Auction(sampleArt());
        auction.setId("auc-1");
        auction.setAuctionStatus(Status.OPEN);

        when(auctionDAO.findById("auc-1")).thenReturn(auction);
        when(auctionDAO.save(auction)).thenReturn(true);

        auctionService.startAuction("auc-1");

        assertEquals(Status.RUNNING, auction.getAuctionStatus());
        verify(auctionDAO, times(1)).save(auction);
    }

    @Test
    @DisplayName("startAuction: Bắt lỗi AuctionClosedException nếu phiên đang không ở trạng thái OPEN")
    void startAuction_RejectsFinishedAuction() {
        Auction auction = new Auction(sampleArt());
        auction.setId("auc-1");
        auction.setAuctionStatus(Status.FINISHED); // Đã kết thúc thì không được Start

        when(auctionDAO.findById("auc-1")).thenReturn(auction);

        assertThrows(AuctionClosedException.class, () -> auctionService.startAuction("auc-1"));
        verify(auctionDAO, never()).save(any()); // Đảm bảo không lưu bậy bạ
    }

    // =========================================================================
    // 3. TEST HÀM KẾT THÚC PHIÊN (END AUCTION)
    // =========================================================================

    @Test
    @DisplayName("endAuction: Gọi DAO để chốt thanh toán và kết thúc phiên")
    void endAuction_SettlesWinnerAndSellerBalance() {
        Auction auction = new Auction(sampleArt());
        auction.setId("auc-1");
        auction.setAuctionStatus(Status.RUNNING);

        when(auctionDAO.findById("auc-1")).thenReturn(auction);
        // Giao việc thanh toán cho DAO, Service chỉ kiểm tra kết quả
        when(auctionDAO.settleAndFinishAuction("auc-1")).thenReturn(true);

        auctionService.endAuction("auc-1");

        verify(auctionDAO, times(1)).settleAndFinishAuction("auc-1");
    }

    @Test
    @DisplayName("endAuction: Bỏ qua không làm gì nếu phiên đã kết thúc từ trước")
    void endAuction_AlreadyFinished_ReturnsEarly() {
        Auction auction = new Auction(sampleArt());
        auction.setId("auc-1");
        auction.setAuctionStatus(Status.FINISHED);

        when(auctionDAO.findById("auc-1")).thenReturn(auction);

        auctionService.endAuction("auc-1");

        // Đảm bảo hàm settle không bị gọi lại lần thứ 2 gây trừ tiền oan của User
        verify(auctionDAO, never()).settleAndFinishAuction(anyString());
    }

    // =========================================================================
    // 4. TEST HÀM GIA HẠN (ANTI-SNIPING)
    // =========================================================================

    @Test
    @DisplayName("extendIfEndingSoon: Gọi ExtensionManager và update DB thành công")
    void extendIfEndingSoon_Success() {
        Auction auction = new Auction(sampleArt());
        auction.setId("auc-1");
        auction.setAuctionStatus(Status.RUNNING);

        when(auctionDAO.findById("auc-1")).thenReturn(auction);

        // Mock static class AuctionExtensionManager
        mockedExtensionManager = Mockito.mockStatic(AuctionExtensionManager.class);
        mockedExtensionManager.when(() -> AuctionExtensionManager.checkAndExtend(auction)).thenReturn(true);
        when(auctionDAO.updateItemEndDate(eq("auc-1"), any())).thenReturn(true);

        boolean result = auctionService.extendIfEndingSoon("auc-1", 10, 60);

        assertTrue(result, "Hàm phải trả về true khi gia hạn thành công");
        verify(auctionDAO, times(1)).updateItemEndDate(eq("auc-1"), any());
    }
}