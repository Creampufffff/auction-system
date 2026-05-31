package com.auction.client.service;

import com.app.common.dto.BalanceResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Test logic nghiệp vụ Tài khoản (AccountService)")
class AccountServiceTest {

    private AccountService accountService;
    private MockedStatic<SocketClientService> mockedSocket;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();
        // Mở Mock cho SocketClientService trước mỗi hàm test
        mockedSocket = mockStatic(SocketClientService.class);
    }

    @AfterEach
    void tearDown() {
        // Đóng Mock sau khi test xong để giải phóng bộ nhớ
        mockedSocket.close();
    }

    // =========================================================================
    // 1. TEST CHỨC NĂNG LẤY SỐ DƯ (GET BALANCE)
    // =========================================================================

    @Test
    @DisplayName("Lấy số dư thành công khi Server trả về đúng format")
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
    @DisplayName("Lấy số dư thất bại khi Server trả về lỗi (ERR)")
    void getBalance_ErrorResponse_ShouldReturnErrorMessage() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("GET_BALANCE"))
                .thenReturn("ERR|USER_NOT_FOUND|Khong tim thay nguoi dung.");

        BalanceResponseDTO response = accountService.getBalance();

        assertNull(response.getUserId());
        assertEquals(0.0, response.getBalance());
        assertEquals("Khong tim thay nguoi dung.", response.getMessage());
    }

    // =========================================================================
    // 2. TEST CHỨC NĂNG NẠP TIỀN (DEPOSIT)
    // =========================================================================

    @Test
    @DisplayName("Nạp tiền thành công, cập nhật số dư mới")
    void deposit_SuccessResponse_ShouldReturnNewBalance() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("DEPOSIT 1500.0"))
                .thenReturn("OK|DEPOSIT|USER-123|6500.0");

        BalanceResponseDTO response = accountService.deposit(1500.0);

        assertEquals("USER-123", response.getUserId());
        assertEquals(6500.0, response.getBalance());
        assertEquals("Nap tien thanh cong.", response.getMessage());
    }

    @Test
    @DisplayName("Nạp tiền thất bại, trích xuất đúng thông báo lỗi từ Server")
    void deposit_ErrorResponse_ShouldReturnErrorMessage() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("DEPOSIT -500.0"))
                .thenReturn("ERR|INVALID_AMOUNT|So tien khong hop le.");

        BalanceResponseDTO response = accountService.deposit(-500.0);

        assertNull(response.getUserId());
        assertEquals(0.0, response.getBalance());
        assertEquals("So tien khong hop le.", response.getMessage());
    }

    // =========================================================================
    // 3. TEST CÁC KỊCH BẢN NGOẠI LỆ (EDGE CASES) VÀ MẠNG
    // =========================================================================

    @Test
    @DisplayName("Xử lý an toàn khi Server trả về chuỗi dị dạng không phải số (NumberFormatException)")
    void getBalance_MalformedNumber_ShouldReturnFormatError() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("GET_BALANCE"))
                .thenReturn("OK|BALANCE|USER-123|NOT_A_NUMBER");

        BalanceResponseDTO response = accountService.getBalance();

        assertNull(response.getUserId());
        assertEquals("Phan hoi so du khong hop le.", response.getMessage());
    }

    @Test
    @DisplayName("Xử lý an toàn khi Server sập (trả về null) hoặc mất mạng (Ném Exception)")
    void networkFailure_ShouldReturnSafeErrorDTO() throws Exception {
        // Kịch bản 1: Server sập, trả về null
        mockedSocket.when(() -> SocketClientService.sendSessionCommand(anyString()))
                .thenReturn(null);

        BalanceResponseDTO responseGet = accountService.getBalance();
        assertNull(responseGet.getUserId());
        assertEquals("Server khong phan hoi.", responseGet.getMessage());

        // Kịch bản 2: Đứt cáp/timeout, Socket ném Exception (Đã được try-catch trong Service)
        mockedSocket.when(() -> SocketClientService.sendSessionCommand(anyString()))
                .thenThrow(new RuntimeException("Mất kết nối mạng"));

        BalanceResponseDTO responseDeposit = accountService.deposit(100.0);
        assertNull(responseDeposit.getUserId());
        assertEquals("Nap tien that bai.", responseDeposit.getMessage());
    }

    // =========================================================================
    // 4. TEST CÁC TÍNH NĂNG CHƯA HOÀN THIỆN (HARDCODED)
    // =========================================================================

    @Test
    @DisplayName("Hàm rút tiền (Withdraw) tạm thời trả về thông báo chưa hỗ trợ")
    void withdraw_ShouldReturnUnsupportedMessage() {
        BalanceResponseDTO response = accountService.withdraw(500.0);

        assertNull(response.getUserId());
        assertEquals("Server chua ho tro rut tien.", response.getMessage());
    }

    @Test
    @DisplayName("Hàm lấy lịch sử (Bid History) tạm thời trả về danh sách rỗng")
    void getBidHistory_ShouldReturnEmptyList() {
        assertTrue(accountService.getBidHistory().isEmpty(), "Lịch sử đặt giá hiện tại phải là danh sách rỗng");
    }
}