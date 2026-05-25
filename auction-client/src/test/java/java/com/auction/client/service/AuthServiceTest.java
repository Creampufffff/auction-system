package java.com.auction.client.service;

import com.app.common.dto.LoginResponseDTO;
import com.app.common.dto.RegisterResponseDTO;
import com.auction.client.service.AuthService;
import com.auction.client.service.SocketClientService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class AuthServiceTest {

    private AuthService authService;
    private MockedStatic<SocketClientService> mockedSocket;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        // Mở Mock cho SocketClientService trước mỗi hàm test
        mockedSocket = mockStatic(SocketClientService.class);
    }

    @AfterEach
    void tearDown() {
        // Bắt buộc phải đóng Mock sau khi test xong để không ảnh hưởng đến các class test khác
        mockedSocket.close();
    }

    // ==========================================
    // TEST CHỨC NĂNG ĐĂNG NHẬP
    // ==========================================

    @Test
    void testLogin_ValidCredentials_ReturnsSuccess() {
        // Chuẩn bị: Giả lập server trả về chuỗi đăng nhập thành công
        String expectedCommand = "LOGIN tam_nguyen password123";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|LOGIN|25020358|tam_nguyen|Bidder");

        // Thực thi
        LoginResponseDTO response = authService.login("tam_nguyen", "password123");

        // Kiểm tra kết quả
        assertTrue(response.isSuccess(), "Đăng nhập phải thành công");
        assertEquals("25020358", response.getUserId());
        assertEquals("tam_nguyen", response.getUsername());
        assertEquals("Bidder", response.getRole());
    }

    @Test
    void testLogin_InvalidCredentials_ReturnsFailed() {
        // Chuẩn bị: Giả lập server trả về lỗi sai mật khẩu
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenReturn("ERR|Sai tên đăng nhập hoặc mật khẩu");

        // Thực thi
        LoginResponseDTO response = authService.login("tam_nguyen", "wrong_pass");

        // Kiểm tra kết quả
        assertFalse(response.isSuccess());
        assertNull(response.getUserId());
    }

    @Test
    void testLogin_EmptyUsernameOrPassword_FailsEarlyWithoutNetworkCall() {
        // Thực thi với username rỗng
        LoginResponseDTO response = authService.login("", "password123");

        // Kiểm tra
        assertFalse(response.isSuccess());

        // Cực kỳ quan trọng: Đảm bảo rằng hàm sendText KHÔNG BAO GIỜ được gọi
        // vì nó đã bị chặn lại ngay từ bước kiểm tra dữ liệu đầu vào.
        mockedSocket.verify(() -> SocketClientService.sendText(anyString()), Mockito.never());
    }

    @Test
    void testLogin_ServerReturnsNull_ReturnsFailed() {
        // Giả lập sập mạng, server không trả về gì
        mockedSocket.when(() -> SocketClientService.sendText(anyString())).thenReturn(null);

        LoginResponseDTO response = authService.login("test", "test");
        assertFalse(response.isSuccess());
    }

    // ==========================================
    // TEST CHỨC NĂNG ĐĂNG KÝ
    // ==========================================

    @Test
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
    void testRegisterSeller_ValidData_ReturnsSuccess() {
        String expectedCommand = "REGISTER_SELLER shop_tam pass123 shop@vnu.edu.vn";
        mockedSocket.when(() -> SocketClientService.sendText(expectedCommand))
                .thenReturn("OK|REGISTER_SELLER|202");

        RegisterResponseDTO response = authService.registerSeller("shop_tam", "pass123", "shop@vnu.edu.vn");

        assertTrue(response.isSuccess());
        assertEquals("202", response.getUserId());
    }

    @Test
    void testRegister_UsernameTaken_ReturnsErrorMsg() {
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenReturn("ERR|Tên đăng nhập đã tồn tại");

        RegisterResponseDTO response = authService.register("exist_user", "pass", "email@test");

        assertFalse(response.isSuccess());
        assertEquals("Tên đăng nhập đã tồn tại", response.getMessage());
    }

    @Test
    void testRegister_NetworkException_ThrowsIllegalStateException() {
        // Giả lập việc socket quăng lỗi kết nối
        mockedSocket.when(() -> SocketClientService.sendText(anyString()))
                .thenThrow(new IllegalStateException("Không thể gửi yêu cầu"));

        // Xác nhận rằng hàm authService.register cũng quăng đúng lỗi đó ra ngoài
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            authService.register("user", "pass", "email");
        });

        assertTrue(exception.getMessage().contains("Không thể gửi yêu cầu xác thực đến server"));
    }
}