package com.auction.client.session;

import com.app.common.dto.LoginResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @BeforeEach
    void setUp() {
        SessionManager.clear();
    }

    @Test
    void testInitialState_NotLoggedIn() {
        assertFalse(SessionManager.isLoggedIn(), "Ban đầu chưa đăng nhập thì phải trả về false");
        assertNull(SessionManager.getCurrentUserId());
        assertNull(SessionManager.getCurrentUsername());
        assertNull(SessionManager.getCurrentUserRole());
        assertEquals(0.0, SessionManager.getCurrentUserBalance(), "Tài khoản mặc định khi chưa login phải là 0.0");
    }

    @Test
    void testSetCurrentUser_ValidUser_UpdatesSession() {
        // Chuẩn bị dữ liệu
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setUserId("25020358");
        mockUser.setUsername("tam_nguyen");
        mockUser.setRole("Bidder");
        mockUser.setBalance(1500.50);

        // Thực thi
        SessionManager.setCurrentUser(mockUser);

        // Kiểm tra
        assertTrue(SessionManager.isLoggedIn());
        assertEquals("25020358", SessionManager.getCurrentUserId());
        assertEquals("tam_nguyen", SessionManager.getCurrentUsername());
        assertEquals("Bidder", SessionManager.getCurrentUserRole());
        assertEquals(1500.50, SessionManager.getCurrentUserBalance());
    }

    @Test
    void testHasRole_MatchingRole_ReturnsTrue() {
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setRole("Seller");
        SessionManager.setCurrentUser(mockUser);

        // Kiểm tra phân biệt hoa thường (case-insensitive)
        assertTrue(SessionManager.hasRole("Seller"), "Phải nhận diện đúng role Seller");
        assertTrue(SessionManager.hasRole("seller"), "Phải bỏ qua viết hoa viết thường");
        assertTrue(SessionManager.hasRole("SELLER"));
    }

    @Test
    void testHasRole_DifferentRole_ReturnsFalse() {
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setRole("Bidder");
        SessionManager.setCurrentUser(mockUser);

        assertFalse(SessionManager.hasRole("Seller"));
        assertFalse(SessionManager.hasRole("Admin"));
    }

    @Test
    void testHasRole_NullRole_ReturnsFalse() {
        // Cố tình gán role là null để xem hàm xử lý có bị lỗi NullPointerException không
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setRole(null);
        SessionManager.setCurrentUser(mockUser);

        assertFalse(SessionManager.hasRole("Bidder"));

        // Test cả trường hợp truyền tham số null vào
        assertFalse(SessionManager.hasRole(null));
    }

    @Test
    void testClear_RemovesUserFromSession() {
        // 1. Gán user vào
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setUsername("test_user");
        SessionManager.setCurrentUser(mockUser);
        assertTrue(SessionManager.isLoggedIn());

        // 2. Gọi hàm clear
        SessionManager.clear();

        // 3. Kiểm tra xem đã dọn sạch chưa
        assertFalse(SessionManager.isLoggedIn());
        assertNull(SessionManager.getCurrentUser());
    }
}