package com.auction.app.service;

import com.app.common.entity.Art;
import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.exception.InsufficientBalanceException;
import com.app.common.exception.InvalidBidException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.service.impl.BidServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Tầng Service của Giao dịch Đặt giá (BidServiceImplUnitTest)")
class BidServiceImplUnitTest {

    private BidDAO bidDAO;
    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private BidServiceImpl bidService;

    @BeforeEach
    void setUp() {
        bidDAO = Mockito.mock(BidDAO.class);
        auctionDAO = Mockito.mock(AuctionDAO.class);
        userDAO = Mockito.mock(UserDAO.class);
        bidService = new BidServiceImpl(bidDAO, auctionDAO, userDAO);
    }

    // Hàm hỗ trợ khởi tạo dữ liệu mẫu nhanh chóng
    private BidTransaction sampleBid(double bidderBalance, double bidAmount) {
        Bidder bidder = new Bidder("u1", "pass", "email");
        bidder.setId("bidder-1");
        bidder.setBalance(bidderBalance);

        com.app.common.entity.Item item = new Art("desc", "name", "2026-01-01", "2026-12-31", 10.0, 1.0, "Author");
        Auction auction = new Auction(item);
        auction.setId("auction-1");

        return new BidTransaction(bidder, auction, bidAmount);
    }

    // =========================================================================
    // TEST CÁC TRƯỜNG HỢP LỖI (FAILURE CASES)
    // =========================================================================

    @Test
    @DisplayName("placeBid: Ném lỗi InsufficientBalanceException khi ví người dùng không đủ tiền")
    void placeBid_insufficientBalance_throws() {
        // Given: Người dùng có 10$ nhưng muốn bid 100$
        BidTransaction bid = sampleBid(10.0, 100.0);

        when(userDAO.findById(bid.getBidder().getId())).thenReturn(bid.getBidder());

        // When & Then
        assertThrows(InsufficientBalanceException.class, () -> bidService.placeBid(bid));

        // Đảm bảo không có giao dịch nào được lưu xuống DB
        verify(bidDAO, never()).placeBidSafely(any());
    }

    @Test
    @DisplayName("placeBid: Ném lỗi InvalidBidException khi số tiền đặt giá <= 0")
    void placeBid_invalidAmount_throws() {
        // Given: Người dùng có 100$ nhưng lại nhập số tiền đặt giá là 0$
        BidTransaction bid = sampleBid(100.0, 0.0);

        when(userDAO.findById(bid.getBidder().getId())).thenReturn(bid.getBidder());

        // When & Then
        InvalidBidException exception = assertThrows(InvalidBidException.class, () -> bidService.placeBid(bid));
        assertEquals("Bid amount must be greater than 0", exception.getMessage());

        verify(bidDAO, never()).placeBidSafely(any());
    }

    @Test
    @DisplayName("placeBid: Ném lỗi InvalidBidException khi không tìm thấy thông tin người dùng trong hệ thống")
    void placeBid_userNotFound_throws() {
        BidTransaction bid = sampleBid(100.0, 50.0);

        // Giả lập Database không tìm thấy User
        when(userDAO.findById(bid.getBidder().getId())).thenReturn(null);

        InvalidBidException exception = assertThrows(InvalidBidException.class, () -> bidService.placeBid(bid));
        assertEquals("User not found", exception.getMessage());
    }

    // =========================================================================
    // TEST TRƯỜNG HỢP THÀNH CÔNG (SUCCESS CASE)
    // =========================================================================

    @Test
    @DisplayName("placeBid: Gọi DAO lưu dữ liệu an toàn khi các điều kiện đều hợp lệ")
    void placeBid_validBid_success() {
        // Given: Ví có 200$, đặt giá 100$ (Hợp lệ)
        BidTransaction bid = sampleBid(200.0, 100.0);

        when(userDAO.findById(bid.getBidder().getId())).thenReturn(bid.getBidder());
        when(bidDAO.placeBidSafely(bid)).thenReturn(true);

        // When
        assertDoesNotThrow(() -> bidService.placeBid(bid));

        // Then: DAO phải được gọi để thực thi Transaction ghi dữ liệu
        verify(bidDAO, times(1)).placeBidSafely(bid);
    }
}