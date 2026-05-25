package java.com.auction.client.service;

import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.auction.client.service.AuctionService;
import com.auction.client.service.SocketClientService;
import com.auction.client.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

public class AuctionServiceTest {

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService();
    }

    // =========================================================================
    // 1. TEST LOGIC VALIDATION (KIỂM TRA DỮ LIỆU ĐẦU VÀO)
    // =========================================================================

    @Test
    void placeBid_NullOrEmptyAuctionId_ShouldReturnFailedResponse() {
        // Given: Gửi một request thiếu Auction ID
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("");
        request.setBidderId("USER-101");
        request.setBidAmount(500.0);

        // When
        PlaceBidResponseDTO response = auctionService.placeBid(request);

        // Then
        assertNotNull(response);
        assertFalse(response.isSuccess(), "Phải thất bại vì thiếu mã đấu giá");
        assertEquals("Thông tin đặt giá không hợp lệ.", response.getMessage());
    }

    @Test
    void placeBid_NegativeOrZeroAmount_ShouldReturnFailedResponse() {
        // Given: Gửi số tiền đặt giá bằng 0
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-001");
        request.setBidderId("USER-101");
        request.setBidAmount(0.0);

        // When
        PlaceBidResponseDTO response = auctionService.placeBid(request);

        // Then
        assertFalse(response.isSuccess(), "Phải thất bại vì số tiền không hợp lệ");
        assertEquals("Thông tin đặt giá không hợp lệ.", response.getMessage());
    }

    // =========================================================================
    // 2. TEST LOGIC PHÂN QUYỀN (ROLE CHECK)
    // =========================================================================

    @Test
    void placeBid_UserRoleIsNotBidder_ShouldReturnFailedResponse() {
        // Given: Thiết lập một request hợp lệ
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-001");
        request.setBidderId("USER-101");
        request.setBidAmount(150.0);

        // Sử dụng MockedStatic để làm giả hàm static SessionManager.hasRole
        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class)) {
            // Ép SessionManager.hasRole("Bidder") trả về false (Ví dụ: User này là Seller)
            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(false);

            // When
            PlaceBidResponseDTO response = auctionService.placeBid(request);

            // Then
            assertFalse(response.isSuccess(), "Seller không được phép đặt giá");
            assertEquals("Chỉ người đấu giá mới được đặt giá.", response.getMessage());
        }
    }

    // =========================================================================
    // 3. TEST LOGIC PARSE PHẢN HỒI TỪ SERVER (SERVER RESPONSE PARSING)
    // =========================================================================

    @Test
    void placeBid_ServerSuccessResponse_ShouldReturnSuccessResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(250.0);

        // Mock cùng lúc cả SessionManager và SocketClientService
        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);
            sessionMock.when(SessionManager::getCurrentUserId).thenReturn("USER-101");

            // Giả lập: Khi gọi server, server trả về chuỗi text thành công theo đúng giao thức protocol của bạn
            String mockServerResponse = "OK|BID_PLACED|BID-777|AUC-999|250.0";
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn(mockServerResponse);

            // When
            PlaceBidResponseDTO response = auctionService.placeBid(request);

            // Then: Kiểm tra xem code có bóc tách chuỗi (parse) chuẩn không
            assertTrue(response.isSuccess());
            assertEquals("Đặt giá thành công.", response.getMessage());
            assertEquals("BID-777", response.getBidId());
            assertEquals("AUC-999", response.getAuctionId());
            assertEquals(250.0, response.getBidAmount());
        }
    }

    @Test
    void placeBid_ServerErrorResponse_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(50.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);

            // Giả lập: Server trả về chuỗi lỗi (ví dụ: giá đặt thấp hơn giá hiện tại)
            String mockServerResponse = "ERR|LOW_BID|Giá thầu phải cao hơn giá hiện tại.";
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn(mockServerResponse);

            // When
            PlaceBidResponseDTO response = auctionService.placeBid(request);

            // Then
            assertFalse(response.isSuccess());
            assertEquals("Giá thầu phải cao hơn giá hiện tại.", response.getMessage(),
                    "Hàm extractErrorMessage phải trích xuất đúng nội dung lỗi sau dấu '|' thứ 2");
        }
    }
}
