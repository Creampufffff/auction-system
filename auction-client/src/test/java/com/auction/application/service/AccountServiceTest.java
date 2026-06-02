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

@DisplayName("Test logic nghiệp vụ Tài khoản (AccountService)")
class AccountServiceTest {

    private AccountService accountService;
    private MockedStatic<SocketClientService> mockedSocket;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();
        mockedSocket = mockStatic(SocketClientService.class);
    }

    @AfterEach
    void tearDown() {
        mockedSocket.close();
    }

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
    @DisplayName("Lấy số dư thất bại khi Server trả về lỗi")
    void getBalance_ErrorResponse_ShouldReturnErrorMessage() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("GET_BALANCE"))
                .thenReturn("ERR|USER_NOT_FOUND|Khong tim thay nguoi dung.");

        BalanceResponseDTO response = accountService.getBalance();

        assertNull(response.getUserId());
        assertEquals(0.0, response.getBalance());
        assertEquals("Khong tim thay nguoi dung.", response.getMessage());
    }

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

    @Test
    @DisplayName("Xử lý an toàn khi Server trả về số dư dị dạng")
    void getBalance_MalformedNumber_ShouldReturnFormatError() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("GET_BALANCE"))
                .thenReturn("OK|BALANCE|USER-123|NOT_A_NUMBER");

        BalanceResponseDTO response = accountService.getBalance();

        assertNull(response.getUserId());
        assertEquals("Phan hoi so du khong hop le.", response.getMessage());
    }

    @Test
    @DisplayName("Xử lý an toàn khi Server không phản hồi hoặc mất mạng")
    void networkFailure_ShouldReturnSafeErrorDTO() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand(anyString()))
                .thenReturn(null);

        BalanceResponseDTO responseGet = accountService.getBalance();
        assertNull(responseGet.getUserId());
        assertEquals("Server khong phan hoi.", responseGet.getMessage());

        mockedSocket.when(() -> SocketClientService.sendSessionCommand(anyString()))
                .thenThrow(new RuntimeException("Mat ket noi mang"));

        BalanceResponseDTO responseDeposit = accountService.deposit(100.0);
        assertNull(responseDeposit.getUserId());
        assertEquals("Nap tien that bai.", responseDeposit.getMessage());
    }

    @Test
    @DisplayName("Rút tiền thành công, cập nhật số dư mới")
    void withdraw_SuccessResponse_ShouldReturnNewBalance() throws Exception {
        mockedSocket.when(() -> SocketClientService.sendSessionCommand("WITHDRAW 500.0"))
                .thenReturn("OK|WITHDRAW|USER-123|4500.0");

        BalanceResponseDTO response = accountService.withdraw(500.0);

        assertEquals("USER-123", response.getUserId());
        assertEquals(4500.0, response.getBalance());
        assertEquals("Rut tien thanh cong.", response.getMessage());
    }

    @Test
    @DisplayName("Lấy lịch sử đặt giá trả về danh sách rỗng khi chưa có dữ liệu")
    void getBidHistory_ShouldReturnEmptyList() {
        assertTrue(accountService.getBidHistory().isEmpty());
    }
}
