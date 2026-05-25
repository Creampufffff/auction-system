package java.com.auction.app.service.impl;

import com.app.common.entity.*;
import com.app.common.exception.InvalidBidException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.service.impl.AutoBidServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutoBidServiceImplTest {

    @Mock
    private AuctionDAO auctionDAO;

    @Mock
    private BidDAO bidDAO;

    private AutoBidServiceImpl autoBidService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        autoBidService = new AutoBidServiceImpl(auctionDAO, bidDAO);
    }

    // ==========================================
    // 1. TEST CHỨC NĂNG TẠO AUTO BID (CREATE)
    // ==========================================

    @Test
    void testCreateAutoBid_ValidData_Success() {
        AutoBid autoBid = new AutoBid("auction-1", "bidder-1", 500.0);

        // Sử dụng Object thật thay vì Mock
        Auction realAuction = createRealAuction("auction-1", 100.0, 10.0);
        when(auctionDAO.findById("auction-1")).thenReturn(realAuction);

        autoBidService.createAutoBid(autoBid);

        AutoBid savedBid = autoBidService.getAutoBidById(autoBid.getId());
        assertNotNull(savedBid);
        assertTrue(savedBid.isActive());
        assertEquals(500.0, savedBid.getMaxAutoAmount());
    }

    @Test
    void testCreateAutoBid_AuctionNotFound_ThrowsException() {
        AutoBid autoBid = new AutoBid("invalid-auction", "bidder-1", 500.0);

        when(auctionDAO.findById("invalid-auction")).thenReturn(null);

        InvalidBidException exception = assertThrows(InvalidBidException.class, () -> {
            autoBidService.createAutoBid(autoBid);
        });
        assertEquals("Auction not found", exception.getMessage());
    }

    @Test
    void testCreateAutoBid_InvalidAmount_ThrowsException() {
        AutoBid autoBid = new AutoBid("auction-1", "bidder-1", 0.0);

        InvalidBidException exception = assertThrows(InvalidBidException.class, () -> {
            autoBidService.createAutoBid(autoBid);
        });
        assertEquals("Max auto amount must be greater than 0", exception.getMessage());
    }

    // ==========================================
    // 2. TEST CHỨC NĂNG HỦY AUTO BID (CANCEL)
    // ==========================================

    @Test
    void testCancelAutoBid_ValidId_SetsInactive() {
        AutoBid autoBid = new AutoBid("auction-1", "bidder-1", 500.0);
        Auction realAuction = createRealAuction("auction-1", 100.0, 10.0);

        when(auctionDAO.findById("auction-1")).thenReturn(realAuction);
        autoBidService.createAutoBid(autoBid);

        autoBidService.cancelAutoBid(autoBid.getId());

        AutoBid canceledBid = autoBidService.getAutoBidById(autoBid.getId());
        assertFalse(canceledBid.isActive(), "AutoBid phải bị vô hiệu hóa (isActive = false)");
    }

    // ==========================================
    // 3. TEST QUY TRÌNH AUTO BIDDING (PROCESS)
    // ==========================================

    @Test
    void testProcessAutoBids_SufficientFunds_PlacesBidSuccessfully() {
        AutoBid autoBid = new AutoBid("auction-1", "bidder-1", 1000.0);
        Auction realAuction = createRealAuction("auction-1", 100.0, 10.0); // Bước giá = 10

        when(auctionDAO.findById("auction-1")).thenReturn(realAuction);
        autoBidService.createAutoBid(autoBid);

        // Tạo một giao dịch thật (Real Object) đang giữ giá cao nhất là 200
        BidTransaction realHighestBid = createRealBidTransaction(realAuction, "other-bidder", 200.0);
        when(bidDAO.getMaxBidByAuctionId("auction-1")).thenReturn(realHighestBid);

        autoBidService.processAutoBidsForAuction("auction-1");

        ArgumentCaptor<BidTransaction> bidCaptor = ArgumentCaptor.forClass(BidTransaction.class);
        verify(bidDAO).placeBidSafely(bidCaptor.capture());

        BidTransaction placedBid = bidCaptor.getValue();
        assertEquals("bidder-1", placedBid.getBidder().getId());
        assertEquals(210.0, placedBid.getBidAmount()); // 200 + 10 = 210
        assertTrue(autoBid.isActive(), "AutoBid vẫn đủ tiền nên phải giữ trạng thái active");
    }

    @Test
    void testProcessAutoBids_InsufficientFunds_CancelsAutoBid() {
        AutoBid autoBid = new AutoBid("auction-1", "poor-bidder", 150.0); // Giới hạn chỉ có 150
        Auction realAuction = createRealAuction("auction-1", 100.0, 10.0);

        when(auctionDAO.findById("auction-1")).thenReturn(realAuction);
        autoBidService.createAutoBid(autoBid);

        // Giá cao nhất hiện tại đã bị đẩy lên tận 200
        BidTransaction realHighestBid = createRealBidTransaction(realAuction, "rich-bidder", 200.0);
        when(bidDAO.getMaxBidByAuctionId("auction-1")).thenReturn(realHighestBid);

        autoBidService.processAutoBidsForAuction("auction-1");

        verify(bidDAO, never()).placeBidSafely(any());
        assertFalse(autoBid.isActive(), "AutoBid phải bị hủy vì hết ngân sách giới hạn");
    }

    @Test
    void testProcessAutoBids_PrioritySorting_HighestMaxAmountGoesFirst() {
        Auction realAuction = createRealAuction("auction-1", 100.0, 10.0);
        when(auctionDAO.findById("auction-1")).thenReturn(realAuction);

        AutoBid bidLow = new AutoBid("auction-1", "bidder-low", 200.0);
        AutoBid bidHigh = new AutoBid("auction-1", "bidder-high", 1000.0);
        AutoBid bidMid = new AutoBid("auction-1", "bidder-mid", 500.0);

        autoBidService.createAutoBid(bidLow);
        autoBidService.createAutoBid(bidHigh);
        autoBidService.createAutoBid(bidMid);

        List<AutoBid> activeBids = autoBidService.getAutoBiddsByAuctionId("auction-1");

        assertEquals(3, activeBids.size());
        assertEquals("bidder-high", activeBids.get(0).getBidderId(), "Người có ngân sách 1000 phải lên top 1");
        assertEquals("bidder-mid", activeBids.get(1).getBidderId(), "Người có ngân sách 500 ở top 2");
        assertEquals("bidder-low", activeBids.get(2).getBidderId(), "Người có ngân sách 200 ở bét");
    }

    @Test
    void testProcessAutoBids_PlaceBidThrowsException_AutoBidGetsCanceled() {
        AutoBid autoBid = new AutoBid("auction-1", "bidder-1", 1000.0);
        Auction realAuction = createRealAuction("auction-1", 100.0, 10.0);

        when(auctionDAO.findById("auction-1")).thenReturn(realAuction);
        autoBidService.createAutoBid(autoBid);
        when(bidDAO.getMaxBidByAuctionId("auction-1")).thenReturn(null);

        // Giả lập lỗi ghi xuống Database
        doThrow(new IllegalStateException("DB Locked"))
                .when(bidDAO).placeBidSafely(any(BidTransaction.class));

        autoBidService.processAutoBidsForAuction("auction-1");

        assertFalse(autoBid.isActive(), "AutoBid gây lỗi DB phải bị vô hiệu hóa");
    }

    // ==========================================
    // HÀM HỖ TRỢ TẠO DỮ LIỆU THẬT (KHÔNG DÙNG MOCK)
    // ==========================================

    private Auction createRealAuction(String auctionId, double startPrice, double minIncrement) {
        // Dùng class Art làm vật phẩm đại diện
        Art realItem = new Art(
                "Mô tả", "Tranh sơn dầu", "2026-05-15", "2026-05-22",
                startPrice, minIncrement, "Picasso"
        );
        realItem.setId("item-" + auctionId);

        Auction realAuction = new Auction(realItem);
        realAuction.setId(auctionId);
        return realAuction;
    }

    private BidTransaction createRealBidTransaction(Auction auction, String bidderId, double amount) {
        Bidder realBidder = new Bidder("username", "password", "email@test.com");
        realBidder.setId(bidderId);
        return new BidTransaction(realBidder, auction, amount);
    }
}