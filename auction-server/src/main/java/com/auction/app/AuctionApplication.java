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

/*
Khởi tạo tất cả dependencies (DAOs, Services)
Tạo Socket Server để lắng nghe kết nối từ clients
Khởi động auto-close task cho các phiên đấu giá hết hạn
 */
public class AuctionApplication {
    private static final int DEFAULT_PORT = 5000;  // Cổng mặc định nếu không chỉ định
    private static final String PORT_ENV = "AUCTION_SERVER_PORT";  // Biến môi trường để đọc port
    private static final String PLATFORM_PORT_ENV = "PORT";  // Port do nền tảng (Heroku, Render, v.v.) cung cấp

    public static void main(String[] args) throws IOException {
        // Các DAO
        UserDAO userDAO = new UserDAOImpl();
        ItemDAO itemDAO = new ItemDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        BidDAO bidDAO = new BidDAOImpl();

        // Các Service
        UserService userService = new UserServiceImpl(userDAO);
        ItemService itemService = new ItemServiceImpl(itemDAO);
        AuctionService auctionService = new AuctionServiceImpl(auctionDAO, bidDAO, userDAO);
        BidService bidService = new BidServiceImpl(bidDAO, auctionDAO, userDAO);

        // Mỗi client kết nối sẽ được xử lý trên một thread
        AuctionSocketServer server = new AuctionSocketServer(
                resolvePort(args),      // Xác định port để lắng nghe
                userService,
                itemService,
                auctionService,
                bidService
        );

        AuctionManager auctionManager = AuctionManager.getInstance();
        auctionManager.startAutoClose(auctionService);

        //dừng auto-close task để clean up tài nguyên
        Runtime.getRuntime().addShutdownHook(new Thread(auctionManager::stopAutoClose));

        System.out.println("🚀 Khởi động Auction Server...");
        server.start();
    }

    private static int resolvePort(String[] args) {
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            return parsePort(args[0]);
        }

        String configuredPort = System.getenv(PLATFORM_PORT_ENV);
        if (configuredPort != null && !configuredPort.isBlank()) {
            return parsePort(configuredPort);
        }

        configuredPort = System.getenv(PORT_ENV);
        if (configuredPort != null && !configuredPort.isBlank()) {
            return parsePort(configuredPort);
        }

        return DEFAULT_PORT;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            // Kiểm tra port có nằm trong range hợp lệ không
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port phải nằm trong khoảng 1 đến 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port phải là một số: " + value, e);
        }
    }
}
