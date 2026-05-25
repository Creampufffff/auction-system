package java.com.app.common.entity;//kiểm tra chính xác các logic lõi của User như nạp tiền (deposit), rút tiền (withdraw)
// hay chặn số dư âm mà không bị phụ thuộc vào các lớp Bidder hay Seller.

import com.app.common.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void constructor_ValidArguments_ShouldInitializeCorrectlyWithZeroBalance() {
        assertEquals("nguyenductam", user.getUsername());
        assertEquals("securePass123", user.getPassword());
        assertEquals("tam@uet.vnu.edu.vn", user.getEmail());
        assertEquals(0.0, user.getBalance(), "Tài khoản mới tạo phải có số dư bằng 0");
    }

    @Test
    void setBalance_PositiveAmount_ShouldUpdateBalanceSuccessfully() {
        user.setBalance(500.0);
        assertEquals(500.0, user.getBalance());
    }

    @Test
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
    void deposit_ValidAmount_ShouldIncreaseBalance() {
        // Given
        user.setBalance(100.0);

        // When
        user.deposit(50.5);

        // Then
        assertEquals(150.5, user.getBalance(), "Nạp thêm 50.5$ vào ví 100$ thì phải được 150.5$");
    }

    @Test
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
    void withdraw_ValidAmount_ShouldDecreaseBalance() {
        // Given
        user.setBalance(300.0);

        // When
        user.withdraw(120.0);

        // Then
        assertEquals(180.0, user.getBalance(), "Ví có 300$ rút 120$ thì phải còn đúng 180$");
    }

    @Test
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
    void withdraw_InsufficientBalance_ShouldThrowIllegalArgumentException() {
        // Given: Ví chỉ có 50$
        user.setBalance(50.0);

        // When & Then: Cố tình rút 50.01$ (vượt số dư)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            user.withdraw(50.01);
        }, "Rút quá số dư hiện tại phải bị chặn lại");

        assertEquals("Insufficient balance", exception.getMessage());
    }
}