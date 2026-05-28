package com.auction.client.service;

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

@DisplayName("Test logic nghiệp vụ Xác thực (AuthService)")
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
    // TEST CHỨC NĂNG ĐĂNG NHẬP
    // ==========================================

    @Test
    @DisplayName("Đăng nhập thành công với thông tin hợp lệ")
    void testLogin_ValidCredentials_ReturnsSuccess() {
        String expectedCommand = "LOGIN tam_nguyen password123";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|LOGIN|25020358|tam_nguyen|Bidder");

        LoginResponseDTO response = authService.login("tam_nguyen", "password123");

        assertTrue(response.isSuccess(), "Đăng nhập phải thành công");
        assertEquals("25020358", response.getUserId());
        assertEquals("tam_nguyen", response.getUsername());
        assertEquals("Bidder", response.getRole());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai tài khoản hoặc mật khẩu")
    void testLogin_InvalidCredentials_ReturnsFailed() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenReturn("ERR|Sai tên đăng nhập hoặc mật khẩu");

        LoginResponseDTO response = authService.login("tam_nguyen", "wrong_pass");

        assertFalse(response.isSuccess());
        assertNull(response.getUserId());
    }

    @Test
    @DisplayName("Chặn đăng nhập ngay tại Client nếu để trống thông tin (Không gọi Server)")
    void testLogin_EmptyUsernameOrPassword_FailsEarlyWithoutNetworkCall() {
        LoginResponseDTO response = authService.login("", "password123");

        assertFalse(response.isSuccess());
        mockedSocket.verify(() -> SocketClientService.sendText(anyString()), Mockito.never());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi Server không phản hồi (trả về null)")
    void testLogin_ServerReturnsNull_ReturnsFailed() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString())).thenReturn(null);

        LoginResponseDTO response = authService.login("test", "test");
        assertFalse(response.isSuccess());
    }

    // ==========================================
    // TEST CHỨC NĂNG ĐĂNG KÝ
    // ==========================================

    @Test
    @DisplayName("Đăng ký tài khoản Bidder thành công")
    void testRegisterBidder_ValidData_ReturnsSuccess() {
        String expectedCommand = "REGISTER_BIDDER tam_nguyen pass123 tam@vnu.edu.vn";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|REGISTER_BIDDER|101");

        RegisterResponseDTO response = authService.register("tam_nguyen", "pass123", "tam@vnu.edu.vn");

        assertTrue(response.isSuccess());
        assertEquals("101", response.getUserId());
        assertEquals("Đăng ký thành công", response.getMessage());
    }

    @Test
    @DisplayName("Đăng ký tài khoản Seller thành công")
    void testRegisterSeller_ValidData_ReturnsSuccess() {
        String expectedCommand = "REGISTER_SELLER shop_tam pass123 shop@vnu.edu.vn";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|REGISTER_SELLER|202");

        RegisterResponseDTO response = authService.registerSeller("shop_tam", "pass123", "shop@vnu.edu.vn");

        assertTrue(response.isSuccess());
        assertEquals("202", response.getUserId());
    }

    @Test
    @DisplayName("Đăng ký thất bại do Username đã tồn tại")
    void testRegister_UsernameTaken_ReturnsErrorMsg() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenReturn("ERR|Tên đăng nhập đã tồn tại");

        RegisterResponseDTO response = authService.register("exist_user", "pass", "email@test");

        assertFalse(response.isSuccess());
        assertEquals("Tên đăng nhập đã tồn tại", response.getMessage());
    }

    @Test
    @DisplayName("Ném ngoại lệ IllegalStateException khi có lỗi kết nối mạng")
    void testRegister_NetworkException_ThrowsIllegalStateException() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenThrow(new IllegalStateException("Không thể gửi yêu cầu"));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            authService.register("user", "pass", "email");
        });

        assertTrue(exception.getMessage().contains("Không thể gửi yêu cầu xác thực đến server"));
    }
}