package com.auction.shared.session;

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
        assertFalse(SessionManager.isLoggedIn(), "Ban Ä‘áº§u chÆ°a Ä‘Äƒng nháº­p thÃ¬ pháº£i tráº£ vá» false");
        assertNull(SessionManager.getCurrentUserId());
        assertNull(SessionManager.getCurrentUsername());
        assertNull(SessionManager.getCurrentUserRole());
        assertEquals(0.0, SessionManager.getCurrentUserBalance(), "TÃ i khoáº£n máº·c Ä‘á»‹nh khi chÆ°a login pháº£i lÃ  0.0");
    }

    @Test
    void testSetCurrentUser_ValidUser_UpdatesSession() {
        // Chuáº©n bá»‹ dá»¯ liá»‡u
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setUserId("25020358");
        mockUser.setUsername("tam_nguyen");
        mockUser.setRole("Bidder");
        mockUser.setBalance(1500.50);

        // Thá»±c thi
        SessionManager.setCurrentUser(mockUser);

        // Kiá»ƒm tra
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

        // Kiá»ƒm tra phÃ¢n biá»‡t hoa thÆ°á»ng (case-insensitive)
        assertTrue(SessionManager.hasRole("Seller"), "Pháº£i nháº­n diá»‡n Ä‘Ãºng role Seller");
        assertTrue(SessionManager.hasRole("seller"), "Pháº£i bá» qua viáº¿t hoa viáº¿t thÆ°á»ng");
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
        // Cá»‘ tÃ¬nh gÃ¡n role lÃ  null Ä‘á»ƒ xem hÃ m xá»­ lÃ½ cÃ³ bá»‹ lá»—i NullPointerException khÃ´ng
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setRole(null);
        SessionManager.setCurrentUser(mockUser);

        assertFalse(SessionManager.hasRole("Bidder"));

        // Test cáº£ trÆ°á»ng há»£p truyá»n tham sá»‘ null vÃ o
        assertFalse(SessionManager.hasRole(null));
    }

    @Test
    void testClear_RemovesUserFromSession() {
        // 1. GÃ¡n user vÃ o
        LoginResponseDTO mockUser = new LoginResponseDTO();
        mockUser.setUsername("test_user");
        SessionManager.setCurrentUser(mockUser);
        assertTrue(SessionManager.isLoggedIn());

        // 2. Gá»i hÃ m clear
        SessionManager.clear();

        // 3. Kiá»ƒm tra xem Ä‘Ã£ dá»n sáº¡ch chÆ°a
        assertFalse(SessionManager.isLoggedIn());
        assertNull(SessionManager.getCurrentUser());
    }
}
