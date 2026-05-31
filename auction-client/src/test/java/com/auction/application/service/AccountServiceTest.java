package com.auction.application.service;

import com.app.common.dto.BalanceResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Test logic nghiá»‡p vá»¥ TÃ i khoáº£n (AccountService)")
class AccountServiceTest {

    private AccountService accountService;
    private MockedStatic<SocketClientService> mockedSocket;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();
        // Má»Ÿ Mock cho SocketClientService trÆ°á»›c má»—i hÃ m test
        mockedSocket = mockStatic(SocketClientService.class);
    }

    @AfterEach
    void tearDown() {
        // ÄÃ³ng Mock sau khi test xong Ä‘á»ƒ giáº£i phÃ³ng bá»™ nhá»›
        mockedSocket.close();
    }

    // =========================================================================
    // 1. TEST CHá»¨C NÄ‚NG Láº¤Y Sá» DÆ¯ (GET BALANCE)
    // =========================================================================

    @Test
    @DisplayName("Láº¥y sá»‘ dÆ° thÃ nh cÃ´ng khi Server tráº£ vá» Ä‘Ãºng format")
    void getBalance_SuccessResponse_ShouldReturnCorrectBalance() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("GET_BALANCE"))
                .thenReturn("OK|BALANCE|USER-123|5000.5");

        BalanceResponseDTO response = accountService.getBalance();

        assertNotNull(response.getUserId());
        assertEquals("USER-123", response.getUserId());
        assertEquals(5000.5, response.getBalance());
        assertEquals("Tai so du thanh cong.", response.getMessage());
    }

    @Test
    @DisplayName("Láº¥y sá»‘ dÆ° tháº¥t báº¡i khi Server tráº£ vá» lá»—i (ERR)")
    void getBalance_ErrorResponse_ShouldReturnErrorMessage() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("GET_BALANCE"))
                .thenReturn("ERR|USER_NOT_FOUND|Khong tim thay nguoi dung.");

        BalanceResponseDTO response = accountService.getBalance();

        assertNull(response.getUserId());
        assertEquals(0.0, response.getBalance());
        assertEquals("Khong tim thay nguoi dung.", response.getMessage());
    }

    // =========================================================================
    // 2. TEST CHá»¨C NÄ‚NG Náº P TIá»€N (DEPOSIT)
    // =========================================================================

    @Test
    @DisplayName("Náº¡p tiá»n thÃ nh cÃ´ng, cáº­p nháº­t sá»‘ dÆ° má»›i")
    void deposit_SuccessResponse_ShouldReturnNewBalance() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("DEPOSIT 1500.0"))
                .thenReturn("OK|DEPOSIT|USER-123|6500.0");

        BalanceResponseDTO response = accountService.deposit(1500.0);

        assertEquals("USER-123", response.getUserId());
        assertEquals(6500.0, response.getBalance());
        assertEquals("Nap tien thanh cong.", response.getMessage());
    }

    @Test
    @DisplayName("Náº¡p tiá»n tháº¥t báº¡i, trÃ­ch xuáº¥t Ä‘Ãºng thÃ´ng bÃ¡o lá»—i tá»« Server")
    void deposit_ErrorResponse_ShouldReturnErrorMessage() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("DEPOSIT -500.0"))
                .thenReturn("ERR|INVALID_AMOUNT|So tien khong hop le.");

        BalanceResponseDTO response = accountService.deposit(-500.0);

        assertNull(response.getUserId());
        assertEquals(0.0, response.getBalance());
        assertEquals("So tien khong hop le.", response.getMessage());
    }

    // =========================================================================
    // 3. TEST CÃC Ká»ŠCH Báº¢N NGOáº I Lá»† (EDGE CASES) VÃ€ Máº NG
    // =========================================================================

    @Test
    @DisplayName("Xá»­ lÃ½ an toÃ n khi Server tráº£ vá» chuá»—i dá»‹ dáº¡ng khÃ´ng pháº£i sá»‘ (NumberFormatException)")
    void getBalance_MalformedNumber_ShouldReturnFormatError() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("GET_BALANCE"))
                .thenReturn("OK|BALANCE|USER-123|NOT_A_NUMBER");

        BalanceResponseDTO response = accountService.getBalance();

        assertNull(response.getUserId());
        assertEquals("Phan hoi so du khong hop le.", response.getMessage());
    }

    @Test
    @DisplayName("Xá»­ lÃ½ an toÃ n khi Server sáº­p (tráº£ vá» null) hoáº·c máº¥t máº¡ng (NÃ©m Exception)")
    void networkFailure_ShouldReturnSafeErrorDTO() throws Exception {
        // Ká»‹ch báº£n 1: Server sáº­p, tráº£ vá» null
        mockedSocket.when(() -> SocketClientService.sendSessionCommand(anyString()))
                .thenReturn(null);

        BalanceResponseDTO responseGet = accountService.getBalance();
        assertNull(responseGet.getUserId());
        assertEquals("Server khong phan hoi.", responseGet.getMessage());

        // Ká»‹ch báº£n 2: Äá»©t cÃ¡p/timeout, Socket nÃ©m Exception (ÄÃ£ Ä‘Æ°á»£c try-catch trong Service)
        mockedSocket.when(() -> SocketClientService.sendSessionCommand(anyString()))
                .thenThrow(new RuntimeException("Máº¥t káº¿t ná»‘i máº¡ng"));

        BalanceResponseDTO responseDeposit = accountService.deposit(100.0);
        assertNull(responseDeposit.getUserId());
        assertEquals("Nap tien that bai.", responseDeposit.getMessage());
    }

    // =========================================================================
    // 4. TEST CÃC TÃNH NÄ‚NG CHÆ¯A HOÃ€N THIá»†N (HARDCODED)
    // =========================================================================

    @Test
    @DisplayName("HÃ m rÃºt tiá»n (Withdraw) táº¡m thá»i tráº£ vá» thÃ´ng bÃ¡o chÆ°a há»— trá»£")
    void withdraw_ShouldReturnUnsupportedMessage() {
        BalanceResponseDTO response = accountService.withdraw(500.0);

        assertNull(response.getUserId());
        assertEquals("Server chua ho tro rut tien.", response.getMessage());
    }

    @Test
    @DisplayName("HÃ m láº¥y lá»‹ch sá»­ (Bid History) táº¡m thá»i tráº£ vá» danh sÃ¡ch rá»—ng")
    void getBidHistory_ShouldReturnEmptyList() {
        assertTrue(accountService.getBidHistory().isEmpty(), "Lá»‹ch sá»­ Ä‘áº·t giÃ¡ hiá»‡n táº¡i pháº£i lÃ  danh sÃ¡ch rá»—ng");
    }
}
