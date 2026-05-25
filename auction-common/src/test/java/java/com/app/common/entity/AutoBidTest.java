package java.com.app.common.entity;//Lớp test này tập trung kiểm tra xem hàm khởi tạo (constructor) có gán đúng dữ liệu không
//kiểm tra ràng buộc điều kiện của hàm setMaxAutoAmount xem có ném ra ngoại lệ IllegalArgumentException chính xác khi truyền số tiền <0 hay không.

import com.app.common.entity.AutoBid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoBidTest {

    // =========================================================================
    // 1. TEST HÀM KHỞI TẠO (CONSTRUCTOR LOGIC)
    // =========================================================================

    @Test
    void constructor_ValidArguments_ShouldInitializeCorrectly() {
        // Given
        String expectedAuctionId = "AUC-111";
        String expectedBidderId = "USER-222";
        double expectedMaxAmount = 1500.0;

        // When
        AutoBid autoBid = new AutoBid(expectedAuctionId, expectedBidderId, expectedMaxAmount);

        // Then: Đảm bảo các giá trị được gán chính xác và mặc định trạng thái hoạt động là true
        assertEquals(expectedAuctionId, autoBid.getAuctionId());
        assertEquals(expectedBidderId, autoBid.getBidderId());
        assertEquals(expectedMaxAmount, autoBid.getMaxAutoAmount());
        assertTrue(autoBid.isActive(), "Khi vừa tạo mới, cấu hình tự động đặt giá phải ở trạng thái kích hoạt (true)");

        // Kiểm tra tính kế thừa từ BaseEntity: ID tự động sinh bằng UUID không được null
        assertNotNull(autoBid.getId(), "Mã AutoBid ID kế thừa từ BaseEntity phải được tự động sinh bằng UUID");
    }

    // =========================================================================
    // 2. TEST HÀM setMaxAutoAmount VỚI RÀNG BUỘC SỐ TIỀN PHẢI LỚN HƠN 0
    // =========================================================================

    @Test
    void setMaxAutoAmount_PositiveAmount_ShouldUpdateSuccessfully() {
        // Given
        AutoBid autoBid = new AutoBid("AUC-111", "USER-222", 100.0);
        double newValidAmount = 250.75;

        // When
        autoBid.setMaxAutoAmount(newValidAmount);

        // Then
        assertEquals(newValidAmount, autoBid.getMaxAutoAmount(), "Số tiền lớn hơn 0 phải được cập nhật thành công");
    }

    @Test
    void setMaxAutoAmount_ZeroAmount_ShouldThrowIllegalArgumentException() {
        // Given
        AutoBid autoBid = new AutoBid("AUC-111", "USER-222", 100.0);

        // When & Then: Kỳ vọng hệ thống chặn đứng và ném ra ngoại lệ khi truyền vào số tiền bằng 0
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            autoBid.setMaxAutoAmount(0.0);
        });

        assertEquals("Auto-bid amount must be positive", exception.getMessage(),
                "Nội dung thông báo lỗi trong Exception phải trùng khớp với thiết kế");
    }

    @Test
    void setMaxAutoAmount_NegativeAmount_ShouldThrowIllegalArgumentException() {
        // Given
        AutoBid autoBid = new AutoBid("AUC-111", "USER-222", 100.0);

        // When & Then: Kỳ vọng ném lỗi khi truyền vào số tiền âm
        assertThrows(IllegalArgumentException.class, () -> {
            autoBid.setMaxAutoAmount(-50.0);
        }, "Số tiền đặt giá tự động tối đa không được phép là số âm");
    }

    // =========================================================================
    // 3. TEST CÁC HÀM SETTER CƠ BẢN KHÁC (MUTATION LOGIC)
    // =========================================================================

    @Test
    void testSettersAndGetters_ShouldUpdateFieldsCorrectly() {
        // Given
        AutoBid autoBid = new AutoBid("AUC-111", "USER-222", 100.0);

        // When
        autoBid.setAuctionId("AUC-999");
        autoBid.setBidderId("USER-888");
        autoBid.setActive(false);

        // Then
        assertEquals("AUC-999", autoBid.getAuctionId());
        assertEquals("USER-888", autoBid.getBidderId());
        assertFalse(autoBid.isActive(), "Trạng thái hoạt động phải đổi sang false sau khi dùng hàm set");
    }
}