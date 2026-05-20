package com.auction.app.service.impl;

import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.ItemDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.repository.impl.AuctionDAOImpl;
import com.auction.app.repository.impl.BidDAOImpl;
import com.auction.app.repository.impl.ItemDAOImpl;
import com.auction.app.repository.impl.UserDAOImpl;
import com.auction.app.service.AuctionService;
import com.auction.app.service.AutoBidService;
import com.auction.app.service.BidService;
import com.auction.app.service.ItemService;
import com.auction.app.service.UserService;
import com.auction.app.controller.AuctionController;
import com.auction.app.controller.AutoBidController;
import com.auction.app.controller.BidController;
import com.auction.app.controller.UserController;
import com.auction.app.socket.AuctionSocketServer;

import java.io.IOException;

@Deprecated
public class ServerSocket {
    public ServerSocket() throws IOException {
        UserDAO userDAO = new UserDAOImpl();
        ItemDAO itemDAO = new ItemDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        BidDAO bidDAO = new BidDAOImpl();

        UserService userService = new UserServiceImpl(userDAO);
        ItemService itemService = new ItemServiceImpl(itemDAO);
        AuctionService auctionService = new AuctionServiceImpl(auctionDAO, bidDAO, userDAO);
        BidService bidService = new BidServiceImpl(bidDAO, auctionDAO, userDAO);
        AutoBidService autoBidService = new AutoBidServiceImpl(auctionDAO, bidDAO);

        // Create controllers
        UserController userController = new UserController(userService);
        AuctionController auctionController = new AuctionController(auctionService);
        BidController bidController = new BidController(bidService, auctionService, userService);
        AutoBidController autoBidController = new AutoBidController(autoBidService, auctionService);

        new AuctionSocketServer(5000, userService, auctionService,
                userController, auctionController, bidController, autoBidController).start();
    }
}


