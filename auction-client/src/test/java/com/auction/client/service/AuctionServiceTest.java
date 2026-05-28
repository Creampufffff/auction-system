package com.auction.client.service;

import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.auction.client.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Test logic nghiệp vụ Đấu giá (AuctionService)")
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
    @DisplayName("Đặt giá thất bại khi thiếu mã đấu giá (Auction ID rỗng)")
    void placeBid_NullOrEmptyAuctionId_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("");
        request.setBidderId("USER-101");
        request.setBidAmount(500.0);

        PlaceBidResponseDTO response = auctionService.placeBid(request);

        assertNotNull(response);
        assertFalse(response.isSuccess(), "Phải thất bại vì thiếu mã đấu giá");
        assertEquals("Thông tin đặt giá không hợp lệ.", response.getMessage());
    }

    @Test
    @DisplayName("Đặt giá thất bại khi số tiền bằng 0 hoặc âm")
    void placeBid_NegativeOrZeroAmount_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-001");
        request.setBidderId("USER-101");
        request.setBidAmount(0.0);

        PlaceBidResponseDTO response = auctionService.placeBid(request);

        assertFalse(response.isSuccess(), "Phải thất bại vì số tiền không hợp lệ");
        assertEquals("Thông tin đặt giá không hợp lệ.", response.getMessage());
    }

    // =========================================================================
    // 2. TEST LOGIC PHÂN QUYỀN (ROLE CHECK)
    // =========================================================================

    @Test
    @DisplayName("Tài khoản không phải Bidder (VD: Seller) không được phép đặt giá")
    void placeBid_UserRoleIsNotBidder_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-001");
        request.setBidderId("USER-101");
        request.setBidAmount(150.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class)) {
            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(false);

            PlaceBidResponseDTO response = auctionService.placeBid(request);

            assertFalse(response.isSuccess(), "Seller không được phép đặt giá");
            assertEquals("Chỉ người đấu giá mới được đặt giá.", response.getMessage());
        }
    }

    // =========================================================================
    // 3. TEST LOGIC PARSE PHẢN HỒI TỪ SERVER & XỬ LÝ LỖI MẠNG
    // =========================================================================

    @Test
    @DisplayName("Đặt giá thành công khi Server trả về đúng format giao thức")
    void placeBid_ServerSuccessResponse_ShouldReturnSuccessResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(250.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);
            sessionMock.when(SessionManager::getCurrentUserId).thenReturn("USER-101");

            String mockServerResponse = "OK|BID_PLACED|BID-777|AUC-999|250.0";
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn(mockServerResponse);

            PlaceBidResponseDTO response = auctionService.placeBid(request);

            assertTrue(response.isSuccess());
            assertEquals("Đặt giá thành công.", response.getMessage());
            assertEquals("BID-777", response.getBidId());
            assertEquals("AUC-999", response.getAuctionId());
            assertEquals(250.0, response.getBidAmount());
        }
    }

    @Test
    @DisplayName("Bóc tách đúng thông báo lỗi khi Server từ chối lệnh đặt giá")
    void placeBid_ServerErrorResponse_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(50.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);

            String mockServerResponse = "ERR|LOW_BID|Giá thầu phải cao hơn giá hiện tại.";
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn(mockServerResponse);

            PlaceBidResponseDTO response = auctionService.placeBid(request);

            assertFalse(response.isSuccess());
            assertEquals("Giá thầu phải cao hơn giá hiện tại.", response.getMessage());
        }
    }

    @Test
    @DisplayName("Ném ngoại lệ IllegalStateException khi mất kết nối mạng (Socket throw Exception)")
    void placeBid_NetworkException_ShouldThrowIllegalStateException() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(100.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);
            sessionMock.when(SessionManager::getCurrentUserId).thenReturn("USER-101");

            // Giả lập Socket ném Exception do rớt mạng hoặc timeout
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenThrow(new Exception("Connection reset by peer"));

            // Expect hàm placeBid sẽ ném ra IllegalStateException
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                auctionService.placeBid(request);
            });

            assertEquals("Không thể gửi yêu cầu đấu giá tới server.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Xử lý an toàn khi Server trả về null hoặc chuỗi dị dạng không đủ tham số")
    void placeBid_MalformedOrNullResponse_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(100.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);
            sessionMock.when(SessionManager::getCurrentUserId).thenReturn("USER-101");

            // Trả về Null (Server sập)
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn(null);
            PlaceBidResponseDTO response1 = auctionService.placeBid(request);
            assertFalse(response1.isSuccess());
            assertEquals("Server không phản hồi.", response1.getMessage());

            // Trả về chuỗi thiếu pipe (Dị dạng)
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn("OK|BID_PLACED|BID-777"); // Thiếu Auction ID và Amount
            PlaceBidResponseDTO response2 = auctionService.placeBid(request);
            assertFalse(response2.isSuccess());
            assertEquals("Phản hồi đặt giá không hợp lệ.", response2.getMessage());
        }
    }
}