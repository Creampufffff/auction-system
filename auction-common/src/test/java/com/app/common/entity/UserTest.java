package com.app.common.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Kiểm thử logic tài khoản người dùng (User Abstract Class)")
public class UserTest {

    private User user;

    // Tạo một lớp con cụ thể (Concrete Class) dạng nội bộ để phục vụ riêng cho việc test lớp abstract
    static class ConcreteTestUser extends User {
        public ConcreteTestUser(String username, String password, String email) {
            super(username, password, email);
        }
    }

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng test trước mỗi case, mặc định ban đầu balance = 0
        user = new ConcreteTestUser("nguyenductam", "securePass123", "tam@uet.vnu.edu.vn");
    }

    // =========================================================================
    // 1. TEST HÀM KHỞI TẠO & SET BALANCE (CONSTRUCTOR & MUTATOR LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Khởi tạo: Constructor map đúng thông tin và gán Balance mặc định = 0")
    void constructor_ValidArguments_ShouldInitializeCorrectlyWithZeroBalance() {
        assertEquals("nguyenductam", user.getUsername());
        assertEquals("securePass123", user.getPassword());
        assertEquals("tam@uet.vnu.edu.vn", user.getEmail());
        assertEquals(0.0, user.getBalance(), "Tài khoản mới tạo phải có số dư bằng 0");
    }

    @Test
    @DisplayName("Cập nhật số dư (setBalance): Thành công khi số tiền >= 0")
    void setBalance_PositiveAmount_ShouldUpdateBalanceSuccessfully() {
        user.setBalance(500.0);
        assertEquals(500.0, user.getBalance());
    }

    @Test
    @DisplayName("Cập nhật số dư (setBalance): Bắt lỗi IllegalArgumentException khi gán số dư âm")
    void setBalance_NegativeAmount_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.setBalance(-1.0);
        });
        assertEquals("Balance cannot be negative", exception.getMessage());
    }

    // =========================================================================
    // 2. TEST LOGIC NẠP TIỀN (DEPOSIT LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Nạp tiền (deposit): Tăng số dư chính xác khi nạp số tiền hợp lệ (> 0)")
    void deposit_ValidAmount_ShouldIncreaseBalance() {
        // Given
        user.setBalance(100.0);

        // When
        user.deposit(50.5);

        // Then
        assertEquals(150.5, user.getBalance(), "Nạp thêm 50.5$ vào ví 100$ thì phải được 150.5$");
    }

    @Test
    @DisplayName("Nạp tiền (deposit): Bắt lỗi IllegalArgumentException khi nạp số tiền <= 0")
    void deposit_ZeroOrNegativeAmount_ShouldThrowIllegalArgumentException() {
        // Test nạp số tiền bằng 0
        IllegalArgumentException zeroException = assertThrows(IllegalArgumentException.class, () -> {
            user.deposit(0.0);
        });
        assertEquals("Deposit amount must be greater than 0", zeroException.getMessage());

        // Test nạp số tiền âm
        assertThrows(IllegalArgumentException.class, () -> {
            user.deposit(-20.0);
        }, "Nạp số tiền âm bắt buộc phải ném lỗi");
    }

    // =========================================================================
    // 3. TEST LOGIC RÚT TIỀN (WITHDRAW LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Rút tiền (withdraw): Giảm số dư chính xác khi rút số tiền nhỏ hơn số dư")
    void withdraw_ValidAmount_ShouldDecreaseBalance() {
        // Given
        user.setBalance(300.0);

        // When
        user.withdraw(120.0);

        // Then
        assertEquals(180.0, user.getBalance(), "Ví có 300$ rút 120$ thì phải còn đúng 180$");
    }

    @Test
    @DisplayName("Rút tiền (withdraw): Thành công và đưa số dư về 0 khi rút toàn bộ tiền")
    void withdraw_ExactBalance_ShouldEmptyBalance() {
        // Given
        user.setBalance(250.0);

        // When
        user.withdraw(250.0);

        // Then
        assertEquals(0.0, user.getBalance(), "Rút toàn bộ 250$ thì số dư phải về chính xác 0.0");
    }

    @Test
    @DisplayName("Rút tiền (withdraw): Bắt lỗi IllegalArgumentException khi rút số tiền <= 0")
    void withdraw_ZeroOrNegativeAmount_ShouldThrowIllegalArgumentException() {
        user.setBalance(100.0);

        // Test rút số tiền bằng 0
        IllegalArgumentException zeroException = assertThrows(IllegalArgumentException.class, () -> {
            user.withdraw(0.0);
        });
        assertEquals("Withdraw amount must be greater than 0", zeroException.getMessage());

        // Test rút số tiền âm
        assertThrows(IllegalArgumentException.class, () -> {
            user.withdraw(-5.0);
        });
    }

    @Test
    @DisplayName("Rút tiền (withdraw): Bắt lỗi Insufficient balance khi số tiền rút vượt quá số dư")
    void withdraw_InsufficientBalance_ShouldThrowIllegalArgumentException() {
        // Given: Ví chỉ có 50$
        user.setBalance(50.0);

        // When & Then: Cố tình rút 50.01$ (vượt số dư)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.withdraw(50.01);
        }, "Rút quá số dư hiện tại phải bị chặn lại");

        assertEquals("Insufficient balance", exception.getMessage());
    }
    // =========================================================================
    // 4. TEST LOGIC GIỮ TIỀN ĐẶT GIÁ (RESERVE LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Giữ tiền (reserve): Trừ balance và cộng vào heldBalance khi số dư hợp lệ")
    void reserve_ValidAmount_ShouldTransferFromBalanceToHeldBalance() {
        // Given
        user.setBalance(500.0);

        // When
        user.reserve(200.0);

        // Then
        assertEquals(300.0, user.getBalance(), "Balance khả dụng phải bị trừ 200$");
        assertEquals(200.0, user.getHeldBalance(), "Tiền bị giữ (heldBalance) phải tăng thêm 200$");
    }

    @Test
    @DisplayName("Giữ tiền (reserve): Bắt lỗi khi số tiền giữ <= 0")
    void reserve_ZeroOrNegativeAmount_ShouldThrowIllegalArgumentException() {
        user.setBalance(100.0);

        assertThrows(IllegalArgumentException.class, () -> user.reserve(0.0), "Không thể giữ 0$");
        assertThrows(IllegalArgumentException.class, () -> user.reserve(-50.0), "Không thể giữ số tiền âm");
    }

    @Test
    @DisplayName("Giữ tiền (reserve): Bắt lỗi khi số dư khả dụng không đủ")
    void reserve_InsufficientBalance_ShouldThrowIllegalArgumentException() {
        user.setBalance(100.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.reserve(150.0);
        });
        assertEquals("Insufficient balance to reserve", exception.getMessage());
    }

    // =========================================================================
    // 5. TEST LOGIC HOÀN TRẢ TIỀN BỊ GIỮ (RELEASE HELD LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Hoàn trả tiền (releaseHeld): Trừ heldBalance và trả lại vào balance")
    void releaseHeld_ValidAmount_ShouldTransferFromHeldBalanceToBalance() {
        // Given
        user.setBalance(100.0);
        user.setHeldBalance(300.0); // Đang bị giữ 300$

        // When (Bị outbid, trả lại 300$)
        user.releaseHeld(300.0);

        // Then
        assertEquals(400.0, user.getBalance(), "Số dư khả dụng phải được cộng lại 300$");
        assertEquals(0.0, user.getHeldBalance(), "Tiền bị giữ phải về 0$");
    }

    @Test
    @DisplayName("Hoàn trả tiền (releaseHeld): Bắt lỗi khi số tiền hoàn <= 0")
    void releaseHeld_ZeroOrNegativeAmount_ShouldThrowIllegalArgumentException() {
        user.setHeldBalance(100.0);
        assertThrows(IllegalArgumentException.class, () -> user.releaseHeld(0.0));
        assertThrows(IllegalArgumentException.class, () -> user.releaseHeld(-10.0));
    }

    @Test
    @DisplayName("Hoàn trả tiền (releaseHeld): Bắt lỗi khi hoàn trả nhiều hơn số tiền đang bị giữ")
    void releaseHeld_AmountExceedsHeldBalance_ShouldThrowIllegalArgumentException() {
        user.setHeldBalance(100.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.releaseHeld(150.0);
        });
        assertEquals("Insufficient held balance", exception.getMessage());
    }

    // =========================================================================
    // 6. TEST LOGIC TIÊU HAO TIỀN BỊ GIỮ (CONSUME HELD LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Tiêu hao (consumeHeld): Trừ hẳn số tiền bị giữ khi người dùng thắng đấu giá")
    void consumeHeld_ValidAmount_ShouldDecreaseHeldBalanceOnly() {
        // Given
        user.setBalance(200.0);
        user.setHeldBalance(500.0); // Đang bị giữ 500$ cho một phiên đấu giá

        // When (Thắng đấu giá, trừ hẳn 500$)
        user.consumeHeld(500.0);

        // Then
        assertEquals(200.0, user.getBalance(), "Balance khả dụng không thay đổi");
        assertEquals(0.0, user.getHeldBalance(), "Tiền bị giữ đã bị trừ sạch");
    }

    @Test
    @DisplayName("Tiêu hao (consumeHeld): Bắt lỗi khi số tiền <= 0")
    void consumeHeld_ZeroOrNegativeAmount_ShouldThrowIllegalArgumentException() {
        user.setHeldBalance(100.0);
        assertThrows(IllegalArgumentException.class, () -> user.consumeHeld(0.0));
        assertThrows(IllegalArgumentException.class, () -> user.consumeHeld(-10.0));
    }

    @Test
    @DisplayName("Tiêu hao (consumeHeld): Bắt lỗi khi trừ nhiều hơn số tiền đang bị giữ")
    void consumeHeld_AmountExceedsHeldBalance_ShouldThrowIllegalArgumentException() {
        user.setHeldBalance(100.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.consumeHeld(150.0);
        });
        assertEquals("Insufficient held balance", exception.getMessage());
    }

    // =========================================================================
    // 7. TEST SETTER BỔ SUNG
    // =========================================================================

    @Test
    @DisplayName("Cập nhật số tiền giữ (setHeldBalance): Bắt lỗi khi gán số âm")
    void setHeldBalance_NegativeAmount_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.setHeldBalance(-1.0);
        });
        assertEquals("Held balance cannot be negative", exception.getMessage());
    }
}