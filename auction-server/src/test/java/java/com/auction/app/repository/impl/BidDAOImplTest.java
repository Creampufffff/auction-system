package java.com.auction.app.repository.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.impl.BidDAOImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BidDAOImplTest {

    private BidDAOImpl bidDAO;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private MockedStatic<DatabaseConfig> mockedDatabaseConfig;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        bidDAO = new BidDAOImpl();

        // 1. Giả lập DatabaseConfig trả về kết nối ảo (mockConnection)
        mockedDatabaseConfig = mockStatic(DatabaseConfig.class);
        mockedDatabaseConfig.when(DatabaseConfig::getConnection).thenReturn(mockConnection);

        // 2. Cấu hình để mọi câu lệnh SQL đều sinh ra mockPreparedStatement
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // 3. Cấu hình để lệnh executeQuery() mặc định trả về mockResultSet
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @AfterEach
    void tearDown() {
        mockedDatabaseConfig.close();
    }

    // ==========================================
    // TEST HÀM XÓA (DELETE)
    // ==========================================
    @Test
    void testDelete_Success() throws SQLException {
        // Giả lập lệnh xóa thành công 1 dòng trong CSDL
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = bidDAO.delete("bid-123");

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "bid-123"); // Đảm bảo ID được truyền đúng vào SQL
        verify(mockPreparedStatement).executeUpdate();
    }

    // ==========================================
    // TEST HÀM ĐỌC DỮ LIỆU CƠ BẢN (SELECT)
    // ==========================================
    @Test
    void testGetMaxBidByBidder_Found() throws SQLException {
        // Giả lập có dữ liệu trả về từ Database
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getDouble("max_bid")).thenReturn(1500.0);

        double maxBid = bidDAO.getMaxBidByBidder("auction-1", "bidder-1");

        assertEquals(1500.0, maxBid);
        verify(mockPreparedStatement).setString(1, "auction-1");
        verify(mockPreparedStatement).setString(2, "bidder-1");
    }

    @Test
    void testGetMaxBidByBidder_NotFound() throws SQLException {
        // Giả lập ResultSet rỗng (không có dòng dữ liệu nào)
        when(mockResultSet.next()).thenReturn(false);

        double maxBid = bidDAO.getMaxBidByBidder("auction-1", "bidder-1");

        assertEquals(0.0, maxBid, "Không có dữ liệu thì phải trả về 0");
    }

    // ==========================================
    // TEST TRANSACTION QUAN TRỌNG NHẤT (PLACE BID SAFELY)
    // ==========================================
    @Test
    void testPlaceBidSafely_InsufficientFunds_RollsbackTransaction() throws SQLException {
        // 1. Chuẩn bị Object để đấu giá (Bid 5000$)
        Bidder bidder = new Bidder("user", "pass", "email");
        bidder.setId("bidder-1");
        Auction auction = new Auction(null);
        auction.setId("auction-1");
        BidTransaction bid = new BidTransaction(bidder, auction, 5000.0);

        // 2. Giả lập Query 1: Lấy thông tin Auction (Trả về trạng thái RUNNING)
        ResultSet mockAuctionRs = mock(ResultSet.class);
        when(mockAuctionRs.next()).thenReturn(true);
        when(mockAuctionRs.getString("status")).thenReturn("RUNNING");
        when(mockAuctionRs.getDouble("start_price")).thenReturn(100.0);
        when(mockAuctionRs.getDouble("min_increment")).thenReturn(10.0);
        when(mockAuctionRs.getDouble("highest_current_price")).thenReturn(200.0);

        // 3. Giả lập Query 2: Lấy thông tin số dư Bidder (Chỉ có 1000$, trong khi bid tận 5000$)
        ResultSet mockBidderRs = mock(ResultSet.class);
        when(mockBidderRs.next()).thenReturn(true);
        when(mockBidderRs.getDouble("balance")).thenReturn(1000.0);

        // 4. KIẾN THỨC MOCKITO NÂNG CAO: Điều hướng PreparedStatement trả về ResultSet theo đúng thứ tự gọi
        // Lần gọi executeQuery đầu tiên -> trả về mockAuctionRs
        // Lần gọi executeQuery thứ hai -> trả về mockBidderRs
        when(mockPreparedStatement.executeQuery())
                .thenReturn(mockAuctionRs)
                .thenReturn(mockBidderRs);

        // 5. Thực thi và Kiểm tra lỗi
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidDAO.placeBidSafely(bid);
        });

        assertEquals("Insufficient funds to place this bid", exception.getMessage());

        // 6. KIỂM TRA ĐỘ AN TOÀN DỮ LIỆU: Phải đảm bảo transaction đã bị Hủy (Rollback)
        verify(mockConnection).rollback();
    }

    @Test
    void testPlaceBidSafely_AuctionNotRunning_ThrowsException() throws SQLException {
        Bidder bidder = new Bidder("user", "pass", "email");
        bidder.setId("bidder-1");
        Auction auction = new Auction(null);
        auction.setId("auction-1");
        BidTransaction bid = new BidTransaction(bidder, auction, 500.0);

        // Giả lập trạng thái Auction đã kết thúc (FINISHED)
        ResultSet mockAuctionRs = mock(ResultSet.class);
        when(mockAuctionRs.next()).thenReturn(true);
        when(mockAuctionRs.getString("status")).thenReturn("FINISHED");

        when(mockPreparedStatement.executeQuery()).thenReturn(mockAuctionRs);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidDAO.placeBidSafely(bid);
        });

        assertEquals("Auction is not active", exception.getMessage());
        verify(mockConnection).rollback();
    }
}