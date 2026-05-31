package com.auction.application.service;

import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.auction.shared.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Test logic nghiá»‡p vá»¥ Äáº¥u giÃ¡ (AuctionService)")
public class AuctionServiceTest {

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService();
    }

    // =========================================================================
    // 1. TEST LOGIC VALIDATION (KIá»‚M TRA Dá»® LIá»†U Äáº¦U VÃ€O)
    // =========================================================================

    @Test
    @DisplayName("Äáº·t giÃ¡ tháº¥t báº¡i khi thiáº¿u mÃ£ Ä‘áº¥u giÃ¡ (Auction ID rá»—ng)")
    void placeBid_NullOrEmptyAuctionId_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("");
        request.setBidderId("USER-101");
        request.setBidAmount(500.0);

        PlaceBidResponseDTO response = auctionService.placeBid(request);

        assertNotNull(response);
        assertFalse(response.isSuccess(), "Pháº£i tháº¥t báº¡i vÃ¬ thiáº¿u mÃ£ Ä‘áº¥u giÃ¡");
        assertEquals("ThÃ´ng tin Ä‘áº·t giÃ¡ khÃ´ng há»£p lá»‡.", response.getMessage());
    }

    @Test
    @DisplayName("Äáº·t giÃ¡ tháº¥t báº¡i khi sá»‘ tiá»n báº±ng 0 hoáº·c Ã¢m")
    void placeBid_NegativeOrZeroAmount_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-001");
        request.setBidderId("USER-101");
        request.setBidAmount(0.0);

        PlaceBidResponseDTO response = auctionService.placeBid(request);

        assertFalse(response.isSuccess(), "Pháº£i tháº¥t báº¡i vÃ¬ sá»‘ tiá»n khÃ´ng há»£p lá»‡");
        assertEquals("ThÃ´ng tin Ä‘áº·t giÃ¡ khÃ´ng há»£p lá»‡.", response.getMessage());
    }

    // =========================================================================
    // 2. TEST LOGIC PHÃ‚N QUYá»€N (ROLE CHECK)
    // =========================================================================

    @Test
    @DisplayName("TÃ i khoáº£n khÃ´ng pháº£i Bidder (VD: Seller) khÃ´ng Ä‘Æ°á»£c phÃ©p Ä‘áº·t giÃ¡")
    void placeBid_UserRoleIsNotBidder_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-001");
        request.setBidderId("USER-101");
        request.setBidAmount(150.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class)) {
            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(false);

            PlaceBidResponseDTO response = auctionService.placeBid(request);

            assertFalse(response.isSuccess(), "Seller khÃ´ng Ä‘Æ°á»£c phÃ©p Ä‘áº·t giÃ¡");
            assertEquals("Chá»‰ ngÆ°á»i Ä‘áº¥u giÃ¡ má»›i Ä‘Æ°á»£c Ä‘áº·t giÃ¡.", response.getMessage());
        }
    }

    // =========================================================================
    // 3. TEST LOGIC PARSE PHáº¢N Há»’I Tá»ª SERVER & Xá»¬ LÃ Lá»–I Máº NG
    // =========================================================================

    @Test
    @DisplayName("Äáº·t giÃ¡ thÃ nh cÃ´ng khi Server tráº£ vá» Ä‘Ãºng format giao thá»©c")
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
            assertEquals("Äáº·t giÃ¡ thÃ nh cÃ´ng.", response.getMessage());
            assertEquals("BID-777", response.getBidId());
            assertEquals("AUC-999", response.getAuctionId());
            assertEquals(250.0, response.getBidAmount());
        }
    }

    @Test
    @DisplayName("BÃ³c tÃ¡ch Ä‘Ãºng thÃ´ng bÃ¡o lá»—i khi Server tá»« chá»‘i lá»‡nh Ä‘áº·t giÃ¡")
    void placeBid_ServerErrorResponse_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(50.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);

            String mockServerResponse = "ERR|LOW_BID|GiÃ¡ tháº§u pháº£i cao hÆ¡n giÃ¡ hiá»‡n táº¡i.";
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn(mockServerResponse);

            PlaceBidResponseDTO response = auctionService.placeBid(request);

            assertFalse(response.isSuccess());
            assertEquals("GiÃ¡ tháº§u pháº£i cao hÆ¡n giÃ¡ hiá»‡n táº¡i.", response.getMessage());
        }
    }

    @Test
    @DisplayName("NÃ©m ngoáº¡i lá»‡ IllegalStateException khi máº¥t káº¿t ná»‘i máº¡ng (Socket throw Exception)")
    void placeBid_NetworkException_ShouldThrowIllegalStateException() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(100.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);
            sessionMock.when(SessionManager::getCurrentUserId).thenReturn("USER-101");

            // Giáº£ láº­p Socket nÃ©m Exception do rá»›t máº¡ng hoáº·c timeout
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenThrow(new Exception("Connection reset by peer"));

            // Expect hÃ m placeBid sáº½ nÃ©m ra IllegalStateException
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                auctionService.placeBid(request);
            });

            assertEquals("KhÃ´ng thá»ƒ gá»­i yÃªu cáº§u Ä‘áº¥u giÃ¡ tá»›i server.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Xá»­ lÃ½ an toÃ n khi Server tráº£ vá» null hoáº·c chuá»—i dá»‹ dáº¡ng khÃ´ng Ä‘á»§ tham sá»‘")
    void placeBid_MalformedOrNullResponse_ShouldReturnFailedResponse() {
        PlaceBidRequestDTO request = new PlaceBidRequestDTO();
        request.setAuctionId("AUC-999");
        request.setBidderId("USER-101");
        request.setBidAmount(100.0);

        try (MockedStatic<SessionManager> sessionMock = mockStatic(SessionManager.class);
             MockedStatic<SocketClientService> socketMock = mockStatic(SocketClientService.class)) {

            sessionMock.when(() -> SessionManager.hasRole("Bidder")).thenReturn(true);
            sessionMock.when(SessionManager::getCurrentUserId).thenReturn("USER-101");

            // Tráº£ vá» Null (Server sáº­p)
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn(null);
            PlaceBidResponseDTO response1 = auctionService.placeBid(request);
            assertFalse(response1.isSuccess());
            assertEquals("Server khÃ´ng pháº£n há»“i.", response1.getMessage());

            // Tráº£ vá» chuá»—i thiáº¿u pipe (Dá»‹ dáº¡ng)
            socketMock.when(() -> SocketClientService.sendSessionCommand(Mockito.anyString()))
                    .thenReturn("OK|BID_PLACED|BID-777"); // Thiáº¿u Auction ID vÃ  Amount
            PlaceBidResponseDTO response2 = auctionService.placeBid(request);
            assertFalse(response2.isSuccess());
            assertEquals("Pháº£n há»“i Ä‘áº·t giÃ¡ khÃ´ng há»£p lá»‡.", response2.getMessage());
        }
    }
}
