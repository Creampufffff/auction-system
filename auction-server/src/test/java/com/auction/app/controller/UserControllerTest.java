package com.auction.app.controller;

import com.app.common.dto.*;
import com.app.common.entity.Bidder;
import com.app.common.entity.User;
import com.auction.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử UserController (Luồng Đăng ký / Đăng nhập / Giao dịch)")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new Bidder("tamnguyen", "pass123", "tam@uet.vnu.edu.vn");
        validUser.setId("u1");
        validUser.setBalance(500.0);
    }

    // =========================================================================
    // 1. TEST LUỒNG ĐĂNG KÝ (REGISTER)
    // =========================================================================

    @Test
    @DisplayName("register: Đăng ký thành công trả về RegisterResponseDTO(true)")
    void register_ValidRequest_ShouldReturnSuccess() {
        RegisterRequestDTO request = new RegisterRequestDTO("tamnguyen", "pass123", "tam@uet.vnu.edu.vn", "BIDDER");
        doNothing().when(userService).register(any(User.class));

        RegisterResponseDTO response = userController.register(request);

        assertTrue(response.isSuccess());
        assertEquals("Registration successful", response.getMessage());
        verify(userService, times(1)).register(any(User.class));
    }

    @Test
    @DisplayName("register: Bắt ngoại lệ từ Service và trả về RegisterResponseDTO(false)")
    void register_ServiceThrowsException_ShouldReturnFalse() {
        RegisterRequestDTO request = new RegisterRequestDTO("tamnguyen", "pass", "email", "BIDDER");
        doThrow(new IllegalArgumentException("Username already exists")).when(userService).register(any(User.class));

        RegisterResponseDTO response = userController.register(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Username already exists"));
    }

    // =========================================================================
    // 2. TEST LUỒNG ĐĂNG NHẬP (LOGIN)
    // =========================================================================

    @Test
    @DisplayName("login: Đăng nhập thành công trả về LoginResponseDTO hợp lệ")
    void login_ValidCredentials_ShouldReturnLoginResponseDTO() {
        LoginRequestDTO request = new LoginRequestDTO("tamnguyen", "pass123");
        when(userService.login("tamnguyen", "pass123")).thenReturn(validUser);

        LoginResponseDTO response = userController.login(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("u1", response.getUserId());
        assertEquals("tamnguyen", response.getUsername());
        assertEquals(500.0, response.getBalance());
    }

    @Test
    @DisplayName("login: Đăng nhập thất bại (Service trả về null) -> Controller trả về null")
    void login_InvalidCredentials_ShouldReturnNull() {
        LoginRequestDTO request = new LoginRequestDTO("ghost", "wrongpass");
        when(userService.login("ghost", "wrongpass")).thenReturn(null);

        LoginResponseDTO response = userController.login(request);

        assertNull(response);
    }

    // =========================================================================
    // 3. TEST LUỒNG LẤY THÔNG TIN (PROFILE & BALANCE)
    // =========================================================================

    @Test
    @DisplayName("getUserProfile: Trả về DTO thông tin user khi truyền đúng ID")
    void getUserProfile_ValidId_ShouldReturnDetails() {
        when(userService.getById("u1")).thenReturn(validUser);

        LoginResponseDTO response = userController.getUserProfile("u1");

        assertNotNull(response);
        assertEquals("u1", response.getUserId());
        assertEquals("tamnguyen", response.getUsername());
    }

    @Test
    @DisplayName("getBalance: Trả về số dư khi User tồn tại")
    void getBalance_ValidUser_ShouldReturnBalance() {
        when(userService.getById("u1")).thenReturn(validUser);

        BalanceResponseDTO response = userController.getBalance("u1");

        assertNotNull(response);
        assertEquals("u1", response.getUserId());
        assertEquals(500.0, response.getBalance());
    }

    @Test
    @DisplayName("getBalance: Trả về thông báo lỗi nếu User không tồn tại")
    void getBalance_UserNotFound_ShouldReturnErrorMessage() {
        when(userService.getById("ghost")).thenReturn(null);

        BalanceResponseDTO response = userController.getBalance("ghost");

        assertEquals(0.0, response.getBalance());
        assertEquals("User does not exist", response.getMessage());
    }

    // =========================================================================
    // 4. TEST LUỒNG GIAO DỊCH (DEPOSIT & WITHDRAW)
    // =========================================================================

    @Test
    @DisplayName("deposit: Nạp tiền thành công trả về ApiResponseDTO(true)")
    void deposit_ValidAmount_ShouldReturnSuccess() {
        DepositRequestDTO request = new DepositRequestDTO("u1", 100.0);
        doNothing().when(userService).deposit("u1", 100.0);

        ApiResponseDTO response = userController.deposit(request);

        assertTrue(response.isSuccess());
        assertEquals("Deposit successful", response.getMessage());
        verify(userService, times(1)).deposit("u1", 100.0);
    }

    @Test
    @DisplayName("deposit: Chặn nạp tiền <= 0 ngay tại Controller")
    void deposit_InvalidAmount_ShouldReturnFalse() {
        DepositRequestDTO request = new DepositRequestDTO("u1", -50.0);

        ApiResponseDTO response = userController.deposit(request);

        assertFalse(response.isSuccess());
        assertEquals("Amount must be greater than 0", response.getMessage());
        verify(userService, never()).deposit(anyString(), anyDouble());
    }

    @Test
    @DisplayName("withdraw: Rút tiền thành công trả về ApiResponseDTO(true)")
    void withdraw_ValidAmount_ShouldReturnSuccess() {
        WithdrawRequestDTO request = new WithdrawRequestDTO("u1", 50.0);
        doNothing().when(userService).withdraw("u1", 50.0);

        ApiResponseDTO response = userController.withdraw(request);

        assertTrue(response.isSuccess());
        assertEquals("Withdrawal successful", response.getMessage());
    }

    @Test
    @DisplayName("withdraw: Chặn rút tiền <= 0 ngay tại Controller")
    void withdraw_InvalidAmount_ShouldReturnFalse() {
        WithdrawRequestDTO request = new WithdrawRequestDTO("u1", 0.0);

        ApiResponseDTO response = userController.withdraw(request);

        assertFalse(response.isSuccess());
        assertEquals("Amount must be greater than 0", response.getMessage());
        verify(userService, never()).withdraw(anyString(), anyDouble());
    }

    @Test
    @DisplayName("withdraw: Bắt ngoại lệ (thiếu số dư) từ Service và trả về lỗi")
    void withdraw_InsufficientBalance_ShouldReturnFalse() {
        WithdrawRequestDTO request = new WithdrawRequestDTO("u1", 1000.0);
        doThrow(new IllegalArgumentException("Insufficient balance")).when(userService).withdraw("u1", 1000.0);

        ApiResponseDTO response = userController.withdraw(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Insufficient balance"));
    }

    // =========================================================================
    // 5. TEST XÓA USER
    // =========================================================================

    @Test
    @DisplayName("deleteUser: Xóa thành công")
    void deleteUser_ValidId_ShouldReturnSuccess() {
        doNothing().when(userService).deleteUser("u1");

        ApiResponseDTO response = userController.deleteUser("u1");

        assertTrue(response.isSuccess());
        assertEquals("User deleted successfully", response.getMessage());
    }

    @Test
    @DisplayName("deleteUser: Bắt ngoại lệ nếu Service không tìm thấy User")
    void deleteUser_UserNotFound_ShouldReturnFalse() {
        doThrow(new IllegalArgumentException("User not found")).when(userService).deleteUser("ghost");

        ApiResponseDTO response = userController.deleteUser("ghost");

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("User not found"));
    }
}