package com.auction.app.service;

// --- CÁC IMPORT THỰC THỂ & DAO CỦA PROJECT ---
import com.app.common.entity.Bidder;
import com.app.common.entity.User;
import com.app.common.exception.UserAuthException;
import com.auction.app.repository.UserDAO;
import com.auction.app.service.impl.UserServiceImpl;

// --- CÁC IMPORT CỦA JUNIT 5 & MOCKITO ---
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

// --- CÁC IMPORT TĨNH (STATIC IMPORTS) BẮT BUỘC PHẢI CÓ ĐỂ DÙNG HÀM ---
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử Tầng Service của Người Dùng (UserServiceImpl)")
class UserServiceImplTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private UserServiceImpl userService;

    // =========================================================================
    // 1. TEST CHỨC NĂNG ĐĂNG KÝ (REGISTER)
    // =========================================================================

    @Test
    @DisplayName("register: Đăng ký thành công và lưu user xuống DB")
    void register_ValidUser_SavesUser() {
        User user = new Bidder("tamnguyen", "pass123", "tam@uet.vnu.edu.vn");
        when(userDAO.findByUsername("tamnguyen")).thenReturn(null);
        when(userDAO.save(user)).thenReturn(true);

        assertDoesNotThrow(() -> userService.register(user));
        verify(userDAO, times(1)).save(user);
    }

    @Test
    @DisplayName("register: Bắt lỗi IllegalArgumentException nếu Username đã tồn tại")
    void register_DuplicateUsername_ThrowsException() {
        User newUser = new Bidder("alice", "pass", "alice@example.com");
        User existingUser = new Bidder("alice", "oldpass", "old@example.com");

        when(userDAO.findByUsername("alice")).thenReturn(existingUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(newUser)
        );
        assertEquals("Username already exists", exception.getMessage());
        verify(userDAO, never()).save(any());
    }

    @Test
    @DisplayName("register: Bắt lỗi nếu thiếu thông tin bắt buộc (Username, Password, Email)")
    void register_MissingInformation_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.register(new Bidder("", "pass", "email")));
        assertThrows(IllegalArgumentException.class, () -> userService.register(new Bidder("tam", null, "email")));
        assertThrows(IllegalArgumentException.class, () -> userService.register(new Bidder("tam", "pass", "  ")));
    }

    @Test
    @DisplayName("register: Bắt lỗi IllegalStateException nếu DB lưu user thất bại")
    void register_SaveFails_ThrowsIllegalStateException() {
        User user = new Bidder("tamnguyen", "pass123", "tam@uet.vnu.edu.vn");
        when(userDAO.findByUsername("tamnguyen")).thenReturn(null);
        when(userDAO.save(user)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userService.register(user)
        );
        assertEquals("Failed to save user", exception.getMessage());
    }

    // =========================================================================
    // 2. TEST CHỨC NĂNG ĐĂNG NHẬP (LOGIN)
    // =========================================================================

    @Test
    @DisplayName("login: Trả về Object User nếu thông tin đăng nhập chính xác")
    void login_ValidCredentials_ReturnsUser() {
        User user = new Bidder("alice", "pass123", "alice@example.com");
        when(userDAO.findByUsername("alice")).thenReturn(user);

        User loggedInUser = userService.login("alice", "pass123");

        assertNotNull(loggedInUser);
        assertSame(user, loggedInUser);
    }

    @Test
    @DisplayName("login: Bắt lỗi UserAuthException nếu sai mật khẩu")
    void login_WrongPassword_ThrowsException() {
        User user = new Bidder("alice", "pass123", "alice@example.com");
        when(userDAO.findByUsername("alice")).thenReturn(user);

        assertThrows(UserAuthException.class, () -> userService.login("alice", "wrongpass"));
    }

    @Test
    @DisplayName("login: Bắt lỗi UserAuthException nếu Username không tồn tại")
    void login_UserNotFound_ThrowsException() {
        when(userDAO.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserAuthException.class, () -> userService.login("ghost", "pass"));
    }

    @Test
    @DisplayName("login: Bắt lỗi IllegalArgumentException nếu username hoặc password trống")
    void login_EmptyCredentials_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.login("", "pass"));
        assertThrows(IllegalArgumentException.class, () -> userService.login("user", "  "));
    }

    // =========================================================================
    // 3. TEST CÁC HÀM CƠ BẢN (getById, getAllUser, deleteUser)
    // =========================================================================

    @Test
    @DisplayName("getById: Lấy đúng User theo ID")
    void getById_ValidId_ReturnsUser() {
        User user = new Bidder("tam", "pass", "email");
        when(userDAO.findById("u1")).thenReturn(user);

        User result = userService.getById("u1");
        assertEquals(user, result);
    }

    @Test
    @DisplayName("getById: Bắt lỗi IllegalArgumentException nếu ID trống")
    void getById_EmptyId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.getById("   "));
    }

    @Test
    @DisplayName("getAllUser: Lấy danh sách toàn bộ User")
    void getAllUser_ReturnsList() {
        when(userDAO.findAll()).thenReturn(Arrays.asList(new Bidder("tam", "p", "e")));

        List<User> users = userService.getAllUser();

        assertEquals(1, users.size());
    }

    @Test
    @DisplayName("deleteUser: Xóa user thành công")
    void deleteUser_ValidId_Success() {
        when(userDAO.delete("u1")).thenReturn(true);
        assertDoesNotThrow(() -> userService.deleteUser("u1"));
        verify(userDAO, times(1)).delete("u1");
    }

    @Test
    @DisplayName("deleteUser: Bắt lỗi IllegalArgumentException nếu không tìm thấy User để xóa")
    void deleteUser_UserNotFound_ThrowsException() {
        when(userDAO.delete("ghost_id")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.deleteUser("ghost_id")
        );
        assertEquals("User not found", exception.getMessage());
    }

    // =========================================================================
    // 4. TEST CHỨC NĂNG VÍ TIỀN (BALANCE, DEPOSIT, WITHDRAW)
    // =========================================================================

    @Test
    @DisplayName("getBalance: Lấy số dư thành công")
    void getBalance_ValidUser_ReturnsBalance() {
        User user = new Bidder("tam", "pass", "email");
        user.setBalance(250.0);
        when(userDAO.findById("u1")).thenReturn(user);

        double balance = userService.getBalance("u1");
        assertEquals(250.0, balance);
    }

    @Test
    @DisplayName("getBalance: Bắt lỗi nếu không tìm thấy User")
    void getBalance_UserNotFound_ThrowsException() {
        when(userDAO.findById("ghost")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> userService.getBalance("ghost"));
    }

    @Test
    @DisplayName("deposit: Nạp tiền thành công và cập nhật số dư vào DB")
    void deposit_ValidAmount_UpdatesBalanceAndSaves() {
        User user = new Bidder("tam", "pass", "email");
        user.setId("u1");
        user.setBalance(100.0);

        when(userDAO.findById("u1")).thenReturn(user);
        when(userDAO.updateBalance(any(User.class))).thenReturn(true);

        userService.deposit("u1", 50.0);

        assertEquals(150.0, user.getBalance());
        verify(userDAO, times(1)).updateBalance(any(User.class));
    }

    @Test
    @DisplayName("deposit: Bắt lỗi IllegalStateException nếu DB cập nhật số dư thất bại")
    void deposit_SaveFails_ThrowsIllegalStateException() {
        User user = new Bidder("tam", "pass", "email");
        user.setId("u1");

        when(userDAO.findById("u1")).thenReturn(user);
        when(userDAO.updateBalance(any(User.class))).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userService.deposit("u1", 50.0)
        );
        assertEquals("Failed to update balance", exception.getMessage());
    }

    @Test
    @DisplayName("withdraw: Rút tiền thành công và cập nhật số dư vào DB")
    void withdraw_ValidAmount_UpdatesBalanceAndSaves() {
        User user = new Bidder("tam", "pass", "email");
        user.setId("u1");
        user.setBalance(200.0);

        when(userDAO.findById("u1")).thenReturn(user);
        when(userDAO.updateBalance(any(User.class))).thenReturn(true);

        userService.withdraw("u1", 50.0);

        assertEquals(150.0, user.getBalance());
        verify(userDAO, times(1)).updateBalance(any(User.class));
    }

    @Test
    @DisplayName("withdraw: Bắt lỗi IllegalStateException nếu DB cập nhật số dư thất bại")
    void withdraw_SaveFails_ThrowsIllegalStateException() {
        User user = new Bidder("tam", "pass", "email");
        user.setId("u1");
        user.setBalance(200.0); // <--- BÍ QUYẾT LÀ ĐÂY: Phải nạp tiền thì mới qua được bước kiểm tra số dư!

        when(userDAO.findById("u1")).thenReturn(user);
        when(userDAO.updateBalance(any(User.class))).thenReturn(false);

        // Trả lại kỳ vọng là IllegalStateException
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userService.withdraw("u1", 100.0)
        );
        assertEquals("Failed to update balance", exception.getMessage());
    }
}