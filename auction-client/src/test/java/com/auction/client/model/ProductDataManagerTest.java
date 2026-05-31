package com.auction.client.model;

import com.app.common.dto.AuctionListDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test logic quản lý trạng thái đấu giá và ví tiền (ProductDataManager)")
public class ProductDataManagerTest {

    private ProductDataManager dataManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    @DisplayName("Làm sạch dữ liệu môi trường trước mỗi bài test")
    void setUp() {
        dataManager = ProductDataManager.getInstance();
        dataManager.resetSessionState();
    }

    // =========================================================================
    // 1. TEST LOGIC VÍ TIỀN (WALLET LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Trừ tiền hợp lệ -> Số dư ví phải giảm tương ứng")
    void deductBalance_ValidAmount_ShouldDecreaseBalance() {
        double amountToDeduct = 500.0;
        double expectedBalance = 4500.0;

        dataManager.deductBalance(amountToDeduct);

        assertEquals(expectedBalance, dataManager.getUserBalance(), "Số dư ví phải giảm đi đúng 500$");
    }

    @Test
    @DisplayName("Hoàn tiền hợp lệ -> Số dư ví phải tăng tương ứng")
    void refundBalance_ValidAmount_ShouldIncreaseBalance() {
        double amountToRefund = 350.0;
        double expectedBalance = 5350.0;

        dataManager.refundBalance(amountToRefund);

        assertEquals(expectedBalance, dataManager.getUserBalance(), "Số dư ví phải được cộng thêm đúng 350$");
    }

    @Test
    @DisplayName("Ghi nhận và truy xuất tiền giam -> Trả về chính xác số tiền đã lưu")
    void setAndGetHeldMoney_ValidAuctionId_ShouldReturnCorrectAmount() {
        String auctionId = "TEST_AUC_01";
        double heldMoney = 1200.0;

        dataManager.setHeldMoney(auctionId, heldMoney);

        assertEquals(heldMoney, dataManager.getHeldMoney(auctionId), "Số tiền bị giam của phiên này phải là 1200$");
    }

    // =========================================================================
    // 2. TEST LOGIC ĐẤU GIÁ (BIDDING LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Bị đè giá khi đang dẫn đầu -> Hoàn lại tiền giam, reset tiền giam và cập nhật giá sàn")
    void handleSomeoneElseLeading_UserWasLeading_ShouldRefundMoneyAndSetHeldToZero() {
        String auctionId = "TEST_AUC_02";
        double myHeldMoney = 1000.0;

        dataManager.deductBalance(1000.0);
        dataManager.setHeldMoney(auctionId, myHeldMoney);

        double newPrice = 1200.0;
        dataManager.handleSomeoneElseLeading(auctionId, newPrice);

        assertEquals(5000.0, dataManager.getUserBalance(), "Tiền giam phải được trả lại vào ví");
        assertEquals(0.0, dataManager.getHeldMoney(auctionId), "Tiền giam của phiên này phải reset về 0");
        assertEquals(newPrice, dataManager.getCurrentPrice(auctionId, 0.0), "Giá sàn phải cập nhật lên mức mới");
    }

    @Test
    @DisplayName("Người khác đặt giá nhưng mình chưa tham gia -> Không đổi số dư, chỉ cập nhật giá")
    void handleSomeoneElseLeading_UserWasNotLeading_ShouldNotRefundMoney() {
        String auctionId = "TEST_AUC_02_EDGE";
        double initialBalance = dataManager.getUserBalance();

        dataManager.handleSomeoneElseLeading(auctionId, 1200.0);

        assertEquals(initialBalance, dataManager.getUserBalance(), "Số dư không được thay đổi do bạn chưa đặt giá");
        assertEquals(1200.0, dataManager.getCurrentPrice(auctionId, 0.0), "Giá trị sản phẩm vẫn phải được cập nhật");
    }

    // =========================================================================
    // 3. TEST LOGIC THỜI GIAN (TIMER LOGIC & IS ENDED)
    // =========================================================================

    @Test
    @DisplayName("Thời gian kết thúc nằm ở quá khứ -> Trả về True (Phiên đã kết thúc)")
    void isEnded_TimeHasPassed_ShouldReturnTrue() {
        String auctionId = "TEST_AUC_03";
        AuctionListDTO mockAuction = new AuctionListDTO();
        mockAuction.setAuctionId(auctionId);

        String pastTime = LocalDateTime.now().minusDays(1).format(formatter);
        mockAuction.setEndDateTime(pastTime);

        dataManager.getServerAuctionList().add(mockAuction);

        assertTrue(dataManager.isEnded(auctionId), "Thời gian đã qua, hàm phải trả về true");
    }

    @Test
    @DisplayName("Thời gian kết thúc nằm ở tương lai -> Trả về False (Phiên đang diễn ra)")
    void isEnded_TimeInFuture_ShouldReturnFalse() {
        String auctionId = "TEST_AUC_04";
        AuctionListDTO mockAuction = new AuctionListDTO();
        mockAuction.setAuctionId(auctionId);

        String futureTime = LocalDateTime.now().plusDays(1).format(formatter);
        mockAuction.setEndDateTime(futureTime);

        dataManager.getServerAuctionList().add(mockAuction);

        assertFalse(dataManager.isEnded(auctionId), "Thời gian ở tương lai, hàm chưa thể trả về true");
    }

    @Test
    @DisplayName("Không tìm thấy ID phiên đấu giá trong danh sách -> Trả về False (Tránh lỗi)")
    void isEnded_AuctionIdNotFound_ShouldReturnFalse() {
        String unknownAuctionId = "UNKNOWN_ID";

        assertFalse(dataManager.isEnded(unknownAuctionId), "Không tìm thấy phiên đấu giá, mặc định trả về false");
    }
}