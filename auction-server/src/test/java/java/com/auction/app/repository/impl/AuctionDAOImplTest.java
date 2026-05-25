package java.com.auction.app.repository.impl;

import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.impl.AuctionDAOImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class AuctionDAOImplTest {

    private AuctionDAOImpl auctionDAO;
    private Connection h2Connection;
    private MockedStatic<DatabaseConfig> mockedDatabaseConfig;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // 1. Khởi tạo Database H2 chạy trên RAM (Mô phỏng MySQL)
        h2Connection = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");

        // 2. Tạo sẵn các bảng y hệt Database thật
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("CREATE TABLE items (id VARCHAR(255) PRIMARY KEY, seller_id VARCHAR(255), type VARCHAR(50), name VARCHAR(255), description TEXT, start_date VARCHAR(50), end_date VARCHAR(50), start_price DOUBLE, min_increment DOUBLE, highest_current_price DOUBLE)");
            stmt.execute("CREATE TABLE auctions (id VARCHAR(255) PRIMARY KEY, item_id VARCHAR(255), status VARCHAR(50), last_bidder_id VARCHAR(255), current_version INT DEFAULT 0)");
            stmt.execute("CREATE TABLE bidder (id VARCHAR(255) PRIMARY KEY, balance DOUBLE)");
            stmt.execute("CREATE TABLE seller (id VARCHAR(255) PRIMARY KEY, balance DOUBLE)");
            stmt.execute("CREATE TABLE bid_transactions (id VARCHAR(255) PRIMARY KEY, auction_id VARCHAR(255), bidder_id VARCHAR(255), bid_amount DOUBLE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }

        // 3. Ép cấu hình Database của hệ thống trả về kết nối H2 này
        mockedDatabaseConfig = Mockito.mockStatic(DatabaseConfig.class);
        mockedDatabaseConfig.when(DatabaseConfig::getConnection).thenReturn(h2Connection);

        auctionDAO = new AuctionDAOImpl();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockedDatabaseConfig.close();
        if (h2Connection != null && !h2Connection.isClosed()) {
            h2Connection.close();
        }
    }

    @Test
    void testSettleAuction_WithWinner_UpdatesBalancesAndFinishes() throws Exception {
        // Nhét dữ liệu mẫu vào DB ảo
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("INSERT INTO seller (id, balance) VALUES ('seller-1', 2000.0)");
            stmt.execute("INSERT INTO bidder (id, balance) VALUES ('bidder-1', 1000.0)");
            stmt.execute("INSERT INTO items (id, seller_id, name) VALUES ('item-1', 'seller-1', 'Bức Tranh')");
            stmt.execute("INSERT INTO auctions (id, item_id, status) VALUES ('auction-1', 'item-1', 'RUNNING')");
            stmt.execute("INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount) VALUES ('bid-1', 'auction-1', 'bidder-1', 500.0)");
        }

        // Chạy hàm cần Test
        boolean result = auctionDAO.settleAndFinishAuction("auction-1");

        assertTrue(result);

        // Kiểm tra xem tiền người mua đã bị trừ đúng 500$ chưa
        try (Statement stmt = h2Connection.createStatement();
             var rs = stmt.executeQuery("SELECT balance FROM bidder WHERE id = 'bidder-1'")) {
            rs.next();
            assertEquals(500.0, rs.getDouble("balance"));
        }

        // Kiểm tra tiền người bán đã được cộng thêm 500$ chưa
        try (Statement stmt = h2Connection.createStatement();
             var rs = stmt.executeQuery("SELECT balance FROM seller WHERE id = 'seller-1'")) {
            rs.next();
            assertEquals(2500.0, rs.getDouble("balance"));
        }
    }

    @Test
    void testSettleAuction_WinnerInsufficientFunds_Rollsback() throws Exception {
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("INSERT INTO seller (id, balance) VALUES ('seller-2', 2000.0)");
            stmt.execute("INSERT INTO bidder (id, balance) VALUES ('bidder-2', 100.0)"); // Chỉ có 100$
            stmt.execute("INSERT INTO items (id, seller_id, name) VALUES ('item-2', 'seller-2', 'Xe Hơi')");
            stmt.execute("INSERT INTO auctions (id, item_id, status) VALUES ('auction-2', 'item-2', 'RUNNING')");
            stmt.execute("INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount) VALUES ('bid-2', 'auction-2', 'bidder-2', 5000.0)"); // Nhưng trúng thầu 5000$
        }

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            auctionDAO.settleAndFinishAuction("auction-2");
        });

        assertEquals("Winner has insufficient balance", exception.getMessage());

        // Kiểm tra Rollback: Tiền Seller giữ nguyên không được cộng láo
        try (Statement stmt = h2Connection.createStatement();
             var rs = stmt.executeQuery("SELECT balance FROM seller WHERE id = 'seller-2'")) {
            rs.next();
            assertEquals(2000.0, rs.getDouble("balance"));
        }
    }

    @Test
    void testUpdateCurrentPrice_Success() throws Exception {
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("INSERT INTO items (id, highest_current_price) VALUES ('item-3', 100.0)");
            stmt.execute("INSERT INTO auctions (id, item_id, current_version) VALUES ('auction-3', 'item-3', 1)");
        }

        boolean result = auctionDAO.updateCurrentPrice("auction-3", 150.0, "bidder-x", 1);

        assertTrue(result);

        try (Statement stmt = h2Connection.createStatement();
             var rs = stmt.executeQuery("SELECT highest_current_price FROM items WHERE id = 'item-3'")) {
            rs.next();
            assertEquals(150.0, rs.getDouble("highest_current_price"));
        }
    }
}