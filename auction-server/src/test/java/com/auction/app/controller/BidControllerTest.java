//package com.auction.app.controller;
//
//import com.app.common.dto.PlaceBidRequestDTO;
//import com.app.common.dto.PlaceBidResponseDTO;
//import com.app.common.entity.Auction;
//import com.app.common.entity.Bidder;
//import com.app.common.entity.Item;
//import com.auction.app.service.AuctionService;
//import com.auction.app.service.BidService;
//import com.auction.app.service.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//
//class BidControllerTest {
//    private BidService bidService;
//    private AuctionService auctionService;
//    private UserService userService;
//    private BidController controller;
//
//    @BeforeEach
//    void setUp() {
//        bidService = Mockito.mock(BidService.class);
//        auctionService = Mockito.mock(AuctionService.class);
//        userService = Mockito.mock(UserService.class);
//        controller = new BidController(bidService, auctionService, userService);
//    }
//
//    @Test
//    void placeBid_success() throws Exception {
//        PlaceBidRequestDTO req = new PlaceBidRequestDTO("A1", "u1", 150.0);
//
//        // mock user and auction
//        Bidder bidder = new Bidder("u1", "pass", "email");
//        Mockito.when(userService.getById(req.getBidderId())).thenReturn(bidder);
//        Item item = new Item("desc","name","2026-01-01","2026-12-31",10.0,1.0) {};
//        Auction auction = new Auction(item);
//        Mockito.when(auctionService.getAuctionById(req.getAuctionId())).thenReturn(auction);
//
//        // bidService.placeBid should do nothing (void)
//        Mockito.doNothing().when(bidService).placeBid(any());
//
//        PlaceBidResponseDTO resp = controller.placeBid(req);
//        assertNotNull(resp);
//        assertTrue(resp.isSuccess());
//        assertEquals("Bid placed successfully", resp.getMessage());
//        assertEquals(req.getBidAmount(), resp.getBidAmount());
//    }
//
//    @Test
//    void placeBid_invalidAmount() {
//        PlaceBidRequestDTO req = new PlaceBidRequestDTO("A1", "u1", 0.0);
//
//        Bidder bidder = new Bidder("u1", "pass", "email");
//        Mockito.when(userService.getById(req.getBidderId())).thenReturn(bidder);
//        Item item2 = new Item("desc","name","2026-01-01","2026-12-31",10.0,1.0) {};
//        Auction auction = new Auction(item2);
//        Mockito.when(auctionService.getAuctionById(req.getAuctionId())).thenReturn(auction);
//
//        PlaceBidResponseDTO resp = controller.placeBid(req);
//        assertNotNull(resp);
//        assertFalse(resp.isSuccess());
//        assertTrue(resp.getMessage().toLowerCase().contains("greater than 0"));
//    }
//}
//
