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

/**
 * AuctionApplication: Điểm khởi động chính của ứng dụng Auction Server
 * Trách nhiệm:
 *  1. Khởi tạo tất cả dependencies (DAOs, Services)
 *  2. Tạo Socket Server để lắng nghe kết nối từ clients
 *  3. Khởi động auto-close task cho các phiên đấu giá hết hạn
 */
public class AuctionApplication {
    private static final int DEFAULT_PORT = 5000;  // Cổng mặc định nếu không chỉ định
    private static final String PORT_ENV = "AUCTION_SERVER_PORT";  // Biến môi trường để đọc port

    public static void main(String[] args) throws IOException {
        // ========== BƯỚC 1: Khởi tạo Data Access Layer (DAO) ==========
        // Các DAO chịu trách nhiệm giao tiếp với database
        UserDAO userDAO = new UserDAOImpl();        // DAO cho bảng users
        ItemDAO itemDAO = new ItemDAOImpl();        // DAO cho bảng items
        AuctionDAO auctionDAO = new AuctionDAOImpl();  // DAO cho bảng auctions
        BidDAO bidDAO = new BidDAOImpl();           // DAO cho bảng bid_transactions

        // ========== BƯỚC 2: Khởi tạo Business Logic Layer (Services) ==========
        // Các Service thực hiện logic nghiệp vụ
        UserService userService = new UserServiceImpl(userDAO);
        ItemService itemService = new ItemServiceImpl(itemDAO);
        AuctionService auctionService = new AuctionServiceImpl(auctionDAO, bidDAO, userDAO);
        BidService bidService = new BidServiceImpl(bidDAO, auctionDAO, userDAO);

        // ========== BƯỚC 3: Khởi tạo Socket Server ==========
        // Server này lắng nghe các kết nối từ clients thông qua Socket
        // Mỗi client kết nối sẽ được xử lý trên một luồng riêng (thread)
        AuctionSocketServer server = new AuctionSocketServer(
                resolvePort(args),      // Xác định port để lắng nghe
                userService,
                itemService,
                auctionService,
                bidService
        );

        // ========== BƯỚC 4: Khởi tạo Auto-Close Manager ==========
        // Quản lý các tác vụ nền (background tasks) liên quan đến phiên đấu giá
        // Ví dụ: Tự động đóng phiên nếu hết thời gian
        AuctionManager auctionManager = AuctionManager.getInstance();
        auctionManager.startAutoClose(auctionService);

        // Khi server tắt, dừng auto-close task để clean up tài nguyên
        Runtime.getRuntime().addShutdownHook(new Thread(auctionManager::stopAutoClose));

        // ========== BƯỚC 5: Bắt đầu Server ==========
        // Server sẽ chờ các kết nối từ clients
        System.out.println("🚀 Khởi động Auction Server...");
        server.start();
    }

    /**
     * Xác định port để server lắng nghe
     * Ưu tiên: args[0] > biến môi trường > mặc định (5000)
     */
    private static int resolvePort(String[] args) {
        // Ưu tiên 1: Tham số dòng lệnh (args[0])
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            return parsePort(args[0]);
        }

        // Ưu tiên 2: Biến môi trường AUCTION_SERVER_PORT
        String configuredPort = System.getenv(PORT_ENV);
        if (configuredPort != null && !configuredPort.isBlank()) {
            return parsePort(configuredPort);
        }

        // Ưu tiên 3: Port mặc định
        return DEFAULT_PORT;
    }

    /**
     * Chuyển đổi string thành port number với validation
     * @param value String chứa số port
     * @return Port hợp lệ (1-65535)
     */
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
