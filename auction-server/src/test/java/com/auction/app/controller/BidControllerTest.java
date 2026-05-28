package com.auction.app.controller;

import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.app.common.entity.Auction;
import com.app.common.entity.Bidder;
import com.app.common.entity.Item;
import com.auction.app.service.AuctionService;
import com.auction.app.service.BidService;
import com.auction.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử BidController (Xử lý đặt giá và Anti-sniping)")
class BidControllerTest {
    private BidService bidService;
    private AuctionService auctionService;
    private UserService userService;
    private BidController controller;

    @BeforeEach
    void setUp() {
        bidService = Mockito.mock(BidService.class);
        auctionService = Mockito.mock(AuctionService.class);
        userService = Mockito.mock(UserService.class);
        controller = new BidController(bidService, auctionService, userService);
    }

    // =========================================================================
    // 1. TEST ĐẶT GIÁ THÀNH CÔNG (SUCCESS CASES)
    // =========================================================================

    @Test
    @DisplayName("placeBid: Đặt giá thành công, KHÔNG kích hoạt gia hạn (còn nhiều thời gian)")
    void placeBid_Success_WithoutExtension() throws Exception {
        // Given
        PlaceBidRequestDTO req = new PlaceBidRequestDTO("A1", "u1", 150.0);

        Bidder bidder = new Bidder("u1", "pass", "email");
        Item item = new Item("desc", "name", "2026-01-01", "2026-12-31", 10.0, 1.0) {};
        Auction auction = new Auction(item);
        auction.setId("A1");

        when(userService.getById(req.getBidderId())).thenReturn(bidder);
        when(auctionService.getAuctionById(req.getAuctionId())).thenReturn(auction);
        doNothing().when(bidService).placeBid(any());

        // Giả lập không cần gia hạn
        when(auctionService.extendIfEndingSoon(eq("A1"), anyLong(), anyLong())).thenReturn(false);

        // When
        PlaceBidResponseDTO resp = controller.placeBid(req);

        // Then
        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals("Bid placed successfully", resp.getMessage());
        assertFalse(resp.isAuctionExtended(), "Không kích hoạt gia hạn");

        verify(bidService, times(1)).placeBid(any());
    }

    @Test
    @DisplayName("placeBid: Đặt giá thành công và KÍCH HOẠT gia hạn Anti-sniping")
    void placeBid_Success_WithExtension() throws Exception {
        // Given
        PlaceBidRequestDTO req = new PlaceBidRequestDTO("A1", "u1", 200.0);

        Bidder bidder = new Bidder("u1", "pass", "email");
        Item item = new Item("desc", "name", "2026-01-01", "2026-12-31", 10.0, 1.0) {};
        Auction auction = new Auction(item);
        auction.setId("A1");

        when(userService.getById(req.getBidderId())).thenReturn(bidder);
        when(auctionService.getAuctionById(req.getAuctionId())).thenReturn(auction);
        doNothing().when(bidService).placeBid(any());

        // Giả lập kích hoạt gia hạn thành công (thời gian mới là '2026-12-31|1')
        when(auctionService.extendIfEndingSoon(eq("A1"), anyLong(), anyLong())).thenReturn(true);
        Item updatedItem = new Item("desc", "name", "2026-01-01", "2026-12-31|1", 10.0, 1.0) {};
        Auction updatedAuction = new Auction(updatedItem);
        updatedAuction.setId("A1");

        // Mock lại lần gọi getAuctionById thứ 2 (để lấy thời gian mới trả về cho DTO)
        when(auctionService.getAuctionById(req.getAuctionId())).thenReturn(auction).thenReturn(updatedAuction);

        // When
        PlaceBidResponseDTO resp = controller.placeBid(req);

        // Then
        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertTrue(resp.getMessage().contains("Auction extended by 60s"), "Thông báo phải chứa chi tiết gia hạn");
        assertTrue(resp.isAuctionExtended());
        assertEquals("2026-12-31|1", resp.getNewEndDate(), "Phải lấy được lịch gian hạn mới");

        verify(bidService, times(1)).placeBid(any());
    }

    // =========================================================================
    // 2. TEST CÁC TRƯỜNG HỢP LỖI (FAILURE CASES)
    // =========================================================================

    @Test
    @DisplayName("placeBid: Trả về lỗi khi số tiền đặt giá <= 0")
    void placeBid_InvalidAmount_ShouldFail() {
        // Given
        PlaceBidRequestDTO req = new PlaceBidRequestDTO("A1", "u1", 0.0);

        Bidder bidder = new Bidder("u1", "pass", "email");
        Item item = new Item("desc", "name", "2026-01-01", "2026-12-31", 10.0, 1.0) {};
        Auction auction = new Auction(item);

        when(userService.getById(req.getBidderId())).thenReturn(bidder);
        when(auctionService.getAuctionById(req.getAuctionId())).thenReturn(auction);

        // When
        PlaceBidResponseDTO resp = controller.placeBid(req);

        // Then
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().toLowerCase().contains("greater than 0"));

        // Đảm bảo Service tuyệt đối không được gọi
        verify(bidService, never()).placeBid(any());
    }

    @Test
    @DisplayName("placeBid: Trả về lỗi khi không tìm thấy User hoặc Auction")
    void placeBid_UserOrAuctionNotFound_ShouldFail() {
        // Given
        PlaceBidRequestDTO req = new PlaceBidRequestDTO("A_GHOST", "U_GHOST", 150.0);

        // Giả lập không tìm thấy dữ liệu (trả về null)
        when(userService.getById(req.getBidderId())).thenReturn(null);
        when(auctionService.getAuctionById(req.getAuctionId())).thenReturn(null);

        // When
        PlaceBidResponseDTO resp = controller.placeBid(req);

        // Then
        assertFalse(resp.isSuccess());
        assertEquals("User or auction does not exist", resp.getMessage());

        verify(bidService, never()).placeBid(any());
    }
}