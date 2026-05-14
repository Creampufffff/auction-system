package com.auction.app;

import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.ItemDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.repository.impl.AuctionDAOImpl;
import com.auction.app.repository.impl.BidDAOImpl;
import com.auction.app.repository.impl.ItemDAOImpl;
import com.auction.app.repository.impl.UserDAOImpl;
import com.auction.app.service.AuctionService;
import com.auction.app.service.BidService;
import com.auction.app.service.ItemService;
import com.auction.app.service.UserService;
import com.auction.app.service.impl.AuctionServiceImpl;
import com.auction.app.service.impl.AuctionManager;
import com.auction.app.service.impl.BidServiceImpl;
import com.auction.app.service.impl.ItemServiceImpl;
import com.auction.app.service.impl.UserServiceImpl;
import com.auction.app.socket.AuctionSocketServer;

import java.io.IOException;

public class AuctionApplication {
    private static final int DEFAULT_PORT = 5000;
    private static final String PORT_ENV = "AUCTION_SERVER_PORT";
    private static final String PLATFORM_PORT_ENV = "PORT";

    public static void main(String[] args) throws IOException {
        // Các DAO
        UserDAO userDAO = new UserDAOImpl();
        ItemDAO itemDAO = new ItemDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        BidDAO bidDAO = new BidDAOImpl();

        UserService userService = new UserServiceImpl(userDAO);
        ItemService itemService = new ItemServiceImpl(itemDAO);
        AuctionService auctionService = new AuctionServiceImpl(auctionDAO, bidDAO, userDAO);
        BidService bidService = new BidServiceImpl(bidDAO, auctionDAO, userDAO);

        AuctionSocketServer server = new AuctionSocketServer(DEFAULT_PORT, userService, itemService, auctionService, bidService);

        AuctionManager auctionManager = AuctionManager.getInstance();

        Runtime.getRuntime().addShutdownHook(new Thread(auctionManager::stopAutoClose));

        System.out.println("Khởi động Auction Server...");
        server.start();
    }

}
