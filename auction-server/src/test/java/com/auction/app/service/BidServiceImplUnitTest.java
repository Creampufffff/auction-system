package com.auction.app.service;

import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.exception.InsufficientBalanceException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.service.impl.BidServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void placeBid_insufficientBalance_throws() {
        Bidder bidder = new Bidder("u1", "pass", "email");
        bidder.setBalance(10.0);
        com.app.common.entity.Item item = new com.app.common.entity.Item("desc","name","2026-01-01","2026-12-31",10.0,1.0) {};
        Auction auction = new Auction(item);

        Mockito.when(userDAO.findById(bidder.getId())).thenReturn(bidder);

        BidTransaction bid = new BidTransaction(bidder, auction, 100.0);

        assertThrows(InsufficientBalanceException.class, () -> bidService.placeBid(bid));
    }
}

