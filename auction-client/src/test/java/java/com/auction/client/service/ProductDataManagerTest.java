package java.com.auction.client.service;

import com.auction.client.model.ProductDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProductDataManagerTest {

    private ProductDataManager dataManager;

    @BeforeEach
    void setUp() {
        // Lấy instance của Singleton
        dataManager = ProductDataManager.getInstance();

        // Reset lại số dư ví về mức mặc định trước mỗi bài test để tránh ảnh hưởng lẫn nhau
        dataManager.setUserBalance(5000.0);
    }

    // =========================================================================
    // 1. TEST LOGIC VÍ TIỀN (WALLET LOGIC)
    // =========================================================================

    @Test
    void deductBalance_ValidAmount_ShouldDecreaseBalance() {
        // Given (Chuẩn bị dữ liệu)
        double amountToDeduct = 500.0;
        double expectedBalance = 4500.0;

        // When (Thực thi hành động)
        dataManager.deductBalance(amountToDeduct);

        // Then (Kiểm tra kết quả)
        assertEquals(expectedBalance, dataManager.getUserBalance(), "Số dư ví phải giảm đi đúng 500$");
    }

    @Test
    void refundBalance_ValidAmount_ShouldIncreaseBalance() {
        // Given
        double amountToRefund = 350.0;
        double expectedBalance = 5350.0;

        // When
        dataManager.refundBalance(amountToRefund);

        // Then
        assertEquals(expectedBalance, dataManager.getUserBalance(), "Số dư ví phải được cộng thêm đúng 350$");
    }

    @Test
    void setAndGetHeldMoney_ValidAuctionId_ShouldReturnCorrectAmount() {
        // Given
        String auctionId = "TEST_AUC_01";
        double heldMoney = 1200.0;

        // When
        dataManager.setHeldMoney(auctionId, heldMoney);

        // Then
        assertEquals(heldMoney, dataManager.getHeldMoney(auctionId), "Số tiền bị giam của phiên này phải là 1200$");
    }

    // =========================================================================
    // 2. TEST LOGIC ĐẤU GIÁ (BIDDING LOGIC)
    // =========================================================================

    @Test
    void handleSomeoneElseLeading_UserWasLeading_ShouldRefundMoneyAndSetHeldToZero() {
        // Given: Giả lập kịch bản Bạn đang dẫn đầu phiên đấu giá với 1000$
        String auctionId = "TEST_AUC_02";
        double myHeldMoney = 1000.0;

        dataManager.setUserBalance(4000.0); // Ví còn 4000$ sau khi đặt giá
        dataManager.setHeldMoney(auctionId, myHeldMoney);

        // When: Có người khác nhảy vào dẫn đầu với giá 1200$
        double newPrice = 1200.0;
        dataManager.handleSomeoneElseLeading(auctionId, newPrice);

        // Then 1: Tiền giam 1000$ phải được hoàn trả lại vào ví (4000 + 1000 = 5000)
        assertEquals(5000.0, dataManager.getUserBalance(), "Bạn bị đè giá, tiền giam phải được trả lại vào ví");

        // Then 2: Tiền giam tại phiên này của bạn phải reset về 0
        assertEquals(0.0, dataManager.getHeldMoney(auctionId), "Tiền giam của phiên này phải reset về 0");

        // Then 3: Giá hiện tại của phiên phải được cập nhật lên mức giá mới
        assertEquals(newPrice, dataManager.getCurrentPrice(auctionId, 0.0), "Giá sàn phải cập nhật lên mức 1200$");
    }

    // =========================================================================
    // 3. TEST LOGIC THỜI GIAN (TIMER LOGIC)
    // =========================================================================

    @Test
    void isEnded_TimeLeftIsZero_ShouldReturnTrue() {
        // Given
        String auctionId = "TEST_AUC_03";
        dataManager.setTimeLeft(auctionId, 0);

        // When & Then
        assertTrue(dataManager.isEnded(auctionId), "Thời gian bằng 0 thì phiên đấu giá phải kết thúc (true)");
    }

    @Test
    void isEnded_TimeLeftIsGreaterThanZero_ShouldReturnFalse() {
        // Given
        String auctionId = "TEST_AUC_04";
        dataManager.setTimeLeft(auctionId, 15); // Còn 15 giây

        // When & Then
        assertFalse(dataManager.isEnded(auctionId), "Thời gian còn > 0 thì phiên đấu giá chưa thể kết thúc (false)");
    }
}