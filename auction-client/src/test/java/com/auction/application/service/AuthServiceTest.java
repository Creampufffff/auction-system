package com.auction.application.service;

import com.app.common.dto.LoginResponseDTO;
import com.app.common.dto.RegisterResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Test logic nghiá»‡p vá»¥ XÃ¡c thá»±c (AuthService)")
class AuthServiceTest {

    private AuthService authService;
    private MockedStatic<SocketClientService> mockedSocket;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        mockedSocket = mockStatic(SocketClientService.class);
    }

    @AfterEach
    void tearDown() {
        mockedSocket.close();
    }

    // ==========================================
    // TEST CHá»¨C NÄ‚NG ÄÄ‚NG NHáº¬P
    // ==========================================

    @Test
    @DisplayName("ÄÄƒng nháº­p thÃ nh cÃ´ng vá»›i thÃ´ng tin há»£p lá»‡")
    void testLogin_ValidCredentials_ReturnsSuccess() {
        String expectedCommand = "LOGIN tam_nguyen password123";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|LOGIN|25020358|tam_nguyen|Bidder");

        LoginResponseDTO response = authService.login("tam_nguyen", "password123");

        assertTrue(response.isSuccess(), "ÄÄƒng nháº­p pháº£i thÃ nh cÃ´ng");
        assertEquals("25020358", response.getUserId());
        assertEquals("tam_nguyen", response.getUsername());
        assertEquals("Bidder", response.getRole());
    }

    @Test
    @DisplayName("ÄÄƒng nháº­p tháº¥t báº¡i khi sai tÃ i khoáº£n hoáº·c máº­t kháº©u")
    void testLogin_InvalidCredentials_ReturnsFailed() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenReturn("ERR|Sai tÃªn Ä‘Äƒng nháº­p hoáº·c máº­t kháº©u");

        LoginResponseDTO response = authService.login("tam_nguyen", "wrong_pass");

        assertFalse(response.isSuccess());
        assertNull(response.getUserId());
    }

    @Test
    @DisplayName("Cháº·n Ä‘Äƒng nháº­p ngay táº¡i Client náº¿u Ä‘á»ƒ trá»‘ng thÃ´ng tin (KhÃ´ng gá»i Server)")
    void testLogin_EmptyUsernameOrPassword_FailsEarlyWithoutNetworkCall() {
        LoginResponseDTO response = authService.login("", "password123");

        assertFalse(response.isSuccess());
        mockedSocket.verify(() -> SocketClientService.sendText(anyString()), Mockito.never());
    }

    @Test
    @DisplayName("ÄÄƒng nháº­p tháº¥t báº¡i khi Server khÃ´ng pháº£n há»“i (tráº£ vá» null)")
    void testLogin_ServerReturnsNull_ReturnsFailed() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString())).thenReturn(null);

        LoginResponseDTO response = authService.login("test", "test");
        assertFalse(response.isSuccess());
    }

    // ==========================================
    // TEST CHá»¨C NÄ‚NG ÄÄ‚NG KÃ
    // ==========================================

    @Test
    @DisplayName("ÄÄƒng kÃ½ tÃ i khoáº£n Bidder thÃ nh cÃ´ng")
    void testRegisterBidder_ValidData_ReturnsSuccess() {
        String expectedCommand = "REGISTER_BIDDER tam_nguyen pass123 tam@vnu.edu.vn";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|REGISTER_BIDDER|101");

        RegisterResponseDTO response = authService.register("tam_nguyen", "pass123", "tam@vnu.edu.vn");

        assertTrue(response.isSuccess());
        assertEquals("101", response.getUserId());
        assertEquals("ÄÄƒng kÃ½ thÃ nh cÃ´ng", response.getMessage());
    }

    @Test
    @DisplayName("ÄÄƒng kÃ½ tÃ i khoáº£n Seller thÃ nh cÃ´ng")
    void testRegisterSeller_ValidData_ReturnsSuccess() {
        String expectedCommand = "REGISTER_SELLER shop_tam pass123 shop@vnu.edu.vn";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|REGISTER_SELLER|202");

        RegisterResponseDTO response = authService.registerSeller("shop_tam", "pass123", "shop@vnu.edu.vn");

        assertTrue(response.isSuccess());
        assertEquals("202", response.getUserId());
    }

    @Test
    @DisplayName("ÄÄƒng kÃ½ tháº¥t báº¡i do Username Ä‘Ã£ tá»“n táº¡i")
    void testRegister_UsernameTaken_ReturnsErrorMsg() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenReturn("ERR|TÃªn Ä‘Äƒng nháº­p Ä‘Ã£ tá»“n táº¡i");

        RegisterResponseDTO response = authService.register("exist_user", "pass", "email@test");

        assertFalse(response.isSuccess());
        assertEquals("TÃªn Ä‘Äƒng nháº­p Ä‘Ã£ tá»“n táº¡i", response.getMessage());
    }

    @Test
    @DisplayName("NÃ©m ngoáº¡i lá»‡ IllegalStateException khi cÃ³ lá»—i káº¿t ná»‘i máº¡ng")
    void testRegister_NetworkException_ThrowsIllegalStateException() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenThrow(new IllegalStateException("KhÃ´ng thá»ƒ gá»­i yÃªu cáº§u"));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            authService.register("user", "pass", "email");
        });

        assertTrue(exception.getMessage().contains("KhÃ´ng thá»ƒ gá»­i yÃªu cáº§u xÃ¡c thá»±c Ä‘áº¿n server"));
    }
}
