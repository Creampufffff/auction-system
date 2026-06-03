package com.auction.app;

import io.github.cdimascio.dotenv.Dotenv;
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
import com.auction.app.service.ItemImageService;
import com.auction.app.service.ItemService;
import com.auction.app.service.UserService;
import com.auction.app.service.impl.AuctionServiceImpl;
import com.auction.app.service.impl.AutoBidServiceImpl;
import com.auction.app.service.impl.AuctionManager;
import com.auction.app.service.impl.BidServiceImpl;
import com.auction.app.service.impl.ItemImageServiceImpl;
import com.auction.app.service.impl.ItemServiceImpl;
import com.auction.app.service.impl.UserServiceImpl;
import com.auction.app.controller.AuctionController;
import com.auction.app.controller.AutoBidController;
import com.auction.app.controller.BidController;
import com.auction.app.controller.UserController;
import com.auction.app.socket.AuctionSocketServer;

import java.io.IOException;

public class AuctionApplication {
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        int port = Integer.parseInt(resolveConfig(dotenv, "AUCTION_SERVER_PORT", String.valueOf(DEFAULT_PORT)));

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

        AuctionSocketServer server = new AuctionSocketServer(port, userService, auctionService, 
                userController, auctionController, bidController, autoBidController);

        AuctionManager auctionManager = AuctionManager.getInstance();
        auctionManager.startStatusSync(auctionService, server);

        Runtime.getRuntime().addShutdownHook(new Thread(auctionManager::stopStatusSync));

        System.out.println("Khởi động Auction Server trên port " + port + "...");
        server.start();
    }

    private static String resolveConfig(Dotenv dotenv, String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String dotenvValue = dotenv.get(key);
        return dotenvValue == null || dotenvValue.isBlank() ? defaultValue : dotenvValue;
    }

}
