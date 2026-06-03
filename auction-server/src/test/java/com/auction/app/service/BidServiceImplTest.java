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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Tầng Service của Giao dịch Đặt giá (BidServiceImpl)")
class BidServiceImplTest {

    @Mock
    private BidDAO bidDAO;

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private UserDAO userDAO;

    private BidServiceImpl bidService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bidService = new BidServiceImpl(bidDAO, auctionDAO, userDAO);
    }

    // Hàm tạo dữ liệu mẫu nhanh
    private BidTransaction sampleBid(double amount) {
        Art item = new Art("description", "Painting", "2026-01-01", "2026-01-02", 100, 10, "Author");
        Auction auction = new Auction(item);
        auction.setId("auc-1");

        Bidder bidder = new Bidder("alice", "pass", "alice@example.com");
        bidder.setId("bidder-1");
        bidder.setBalance(1000.0); // Ví có sẵn 1000$

        return new BidTransaction(bidder, auction, amount);
    }

    // =========================================================================
    // 1. TEST CHỨC NĂNG ĐẶT GIÁ (PLACE BID)
    // =========================================================================

    @Test
    @DisplayName("placeBid: Giao dịch hợp lệ được lưu an toàn xuống Database")
    void placeBid_ValidBid_DelegatesToDaoSuccessfully() {
        BidTransaction bid = sampleBid(150.0);

        // Mock User hợp lệ
        when(userDAO.findById("bidder-1")).thenReturn(bid.getBidder());
        // Giả lập lưu thành công
        when(bidDAO.placeBidSafely(bid)).thenReturn(true);

        bidService.placeBid(bid);

        verify(userDAO, times(1)).findById("bidder-1");
        verify(bidDAO, times(1)).placeBidSafely(bid);
    }

    @Test
    @DisplayName("placeBid: Bắt lỗi InvalidBidException nếu số tiền đặt giá <= 0")
    void placeBid_NonPositiveAmount_ThrowsException() {
        BidTransaction bid = sampleBid(0.0); // Đặt 0$

        InvalidBidException exception = assertThrows(InvalidBidException.class, () -> bidService.placeBid(bid));
        assertEquals("Bid amount must be greater than 0", exception.getMessage());

        verify(bidDAO, never()).placeBidSafely(any());
    }

    @Test
    @DisplayName("placeBid: Bắt lỗi InsufficientBalanceException nếu không đủ số dư")
    void placeBid_InsufficientFunds_ThrowsException() {
        BidTransaction bid = sampleBid(1500.0); // Đặt 1500$ nhưng ví chỉ có 1000$

        when(userDAO.findById("bidder-1")).thenReturn(bid.getBidder());
        when(bidDAO.placeBidSafely(bid)).thenThrow(new IllegalArgumentException("Insufficient funds to place this bid"));

        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> bidService.placeBid(bid));
        assertEquals("Insufficient funds to place this bid", exception.getMessage());

        verify(bidDAO, times(1)).placeBidSafely(bid);
    }

    @Test
    @DisplayName("placeBid: Bắt lỗi InvalidBidException khi thiếu thông tin ID của User hoặc Auction")
    void placeBid_MissingIds_ThrowsException() {
        BidTransaction bidMissingAuctionId = sampleBid(150.0);
        bidMissingAuctionId.getAuction().setId(null);

        assertThrows(InvalidBidException.class, () -> bidService.placeBid(bidMissingAuctionId), "Phải báo lỗi nếu thiếu Auction ID");

        BidTransaction bidMissingBidderId = sampleBid(150.0);
        bidMissingBidderId.getBidder().setId("  "); // Blank ID

        assertThrows(InvalidBidException.class, () -> bidService.placeBid(bidMissingBidderId), "Phải báo lỗi nếu thiếu Bidder ID");
    }

    @Test
    @DisplayName("placeBid: Ném IllegalStateException nếu Database từ chối lưu")
    void placeBid_DaoFailsToSave_ThrowsException() {
        BidTransaction bid = sampleBid(150.0);

        when(userDAO.findById("bidder-1")).thenReturn(bid.getBidder());
        when(bidDAO.placeBidSafely(bid)).thenReturn(false); // DAO báo lưu thất bại

        assertThrows(IllegalStateException.class, () -> bidService.placeBid(bid));
    }

    // =========================================================================
    // 2. TEST CÁC HÀM TÌM KIẾM VÀ XÓA CƠ BẢN
    // =========================================================================

    @Test
    @DisplayName("getBidByAuctionId: Lấy danh sách lịch sử đấu giá của một phiên")
    void getBidByAuctionId_ValidId_ReturnsList() {
        BidTransaction bid = sampleBid(150.0);
        when(bidDAO.findByAuctionId("auc-1")).thenReturn(List.of(bid));

        List<BidTransaction> bids = bidService.getBidByAuctionId("auc-1");

        assertFalse(bids.isEmpty());
        assertEquals("auc-1", bids.get(0).getAuction().getId());
    }

    @Test
    @DisplayName("deleteBid: Xóa Bid thành công")
    void deleteBid_ValidId_Success() {
        when(bidDAO.delete("bid-123")).thenReturn(true);

        assertDoesNotThrow(() -> bidService.deleteBid("bid-123"));
        verify(bidDAO, times(1)).delete("bid-123");
    }

    @Test
    @DisplayName("deleteBid: Báo lỗi nếu xóa Bid không tồn tại")
    void deleteBid_InvalidId_ThrowsException() {
        when(bidDAO.delete("ghost-bid")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> bidService.deleteBid("ghost-bid"));
    }
}
