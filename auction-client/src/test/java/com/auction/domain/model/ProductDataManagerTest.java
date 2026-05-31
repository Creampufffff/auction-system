package com.auction.domain.model;

import com.app.common.dto.AuctionListDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test logic quáº£n lÃ½ tráº¡ng thÃ¡i Ä‘áº¥u giÃ¡ vÃ  vÃ­ tiá»n (ProductDataManager)")
public class ProductDataManagerTest {

    private ProductDataManager dataManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    @DisplayName("LÃ m sáº¡ch dá»¯ liá»‡u mÃ´i trÆ°á»ng trÆ°á»›c má»—i bÃ i test")
    void setUp() {
        dataManager = ProductDataManager.getInstance();
        dataManager.resetSessionState();
    }

    // =========================================================================
    // 1. TEST LOGIC VÃ TIá»€N (WALLET LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Trá»« tiá»n há»£p lá»‡ -> Sá»‘ dÆ° vÃ­ pháº£i giáº£m tÆ°Æ¡ng á»©ng")
    void deductBalance_ValidAmount_ShouldDecreaseBalance() {
        double amountToDeduct = 500.0;
        double expectedBalance = 4500.0;

        dataManager.deductBalance(amountToDeduct);

        assertEquals(expectedBalance, dataManager.getUserBalance(), "Sá»‘ dÆ° vÃ­ pháº£i giáº£m Ä‘i Ä‘Ãºng 500$");
    }

    @Test
    @DisplayName("HoÃ n tiá»n há»£p lá»‡ -> Sá»‘ dÆ° vÃ­ pháº£i tÄƒng tÆ°Æ¡ng á»©ng")
    void refundBalance_ValidAmount_ShouldIncreaseBalance() {
        double amountToRefund = 350.0;
        double expectedBalance = 5350.0;

        dataManager.refundBalance(amountToRefund);

        assertEquals(expectedBalance, dataManager.getUserBalance(), "Sá»‘ dÆ° vÃ­ pháº£i Ä‘Æ°á»£c cá»™ng thÃªm Ä‘Ãºng 350$");
    }

    @Test
    @DisplayName("Ghi nháº­n vÃ  truy xuáº¥t tiá»n giam -> Tráº£ vá» chÃ­nh xÃ¡c sá»‘ tiá»n Ä‘Ã£ lÆ°u")
    void setAndGetHeldMoney_ValidAuctionId_ShouldReturnCorrectAmount() {
        String auctionId = "TEST_AUC_01";
        double heldMoney = 1200.0;

        dataManager.setHeldMoney(auctionId, heldMoney);

        assertEquals(heldMoney, dataManager.getHeldMoney(auctionId), "Sá»‘ tiá»n bá»‹ giam cá»§a phiÃªn nÃ y pháº£i lÃ  1200$");
    }

    // =========================================================================
    // 2. TEST LOGIC Äáº¤U GIÃ (BIDDING LOGIC)
    // =========================================================================

    @Test
    @DisplayName("Bá»‹ Ä‘Ã¨ giÃ¡ khi Ä‘ang dáº«n Ä‘áº§u -> HoÃ n láº¡i tiá»n giam, reset tiá»n giam vÃ  cáº­p nháº­t giÃ¡ sÃ n")
    void handleSomeoneElseLeading_UserWasLeading_ShouldRefundMoneyAndSetHeldToZero() {
        String auctionId = "TEST_AUC_02";
        double myHeldMoney = 1000.0;

        dataManager.deductBalance(1000.0);
        dataManager.setHeldMoney(auctionId, myHeldMoney);

        double newPrice = 1200.0;
        dataManager.handleSomeoneElseLeading(auctionId, newPrice);

        assertEquals(5000.0, dataManager.getUserBalance(), "Tiá»n giam pháº£i Ä‘Æ°á»£c tráº£ láº¡i vÃ o vÃ­");
        assertEquals(0.0, dataManager.getHeldMoney(auctionId), "Tiá»n giam cá»§a phiÃªn nÃ y pháº£i reset vá» 0");
        assertEquals(newPrice, dataManager.getCurrentPrice(auctionId, 0.0), "GiÃ¡ sÃ n pháº£i cáº­p nháº­t lÃªn má»©c má»›i");
    }

    @Test
    @DisplayName("NgÆ°á»i khÃ¡c Ä‘áº·t giÃ¡ nhÆ°ng mÃ¬nh chÆ°a tham gia -> KhÃ´ng Ä‘á»•i sá»‘ dÆ°, chá»‰ cáº­p nháº­t giÃ¡")
    void handleSomeoneElseLeading_UserWasNotLeading_ShouldNotRefundMoney() {
        String auctionId = "TEST_AUC_02_EDGE";
        double initialBalance = dataManager.getUserBalance();

        dataManager.handleSomeoneElseLeading(auctionId, 1200.0);

        assertEquals(initialBalance, dataManager.getUserBalance(), "Sá»‘ dÆ° khÃ´ng Ä‘Æ°á»£c thay Ä‘á»•i do báº¡n chÆ°a Ä‘áº·t giÃ¡");
        assertEquals(1200.0, dataManager.getCurrentPrice(auctionId, 0.0), "GiÃ¡ trá»‹ sáº£n pháº©m váº«n pháº£i Ä‘Æ°á»£c cáº­p nháº­t");
    }

    // =========================================================================
    // 3. TEST LOGIC THá»œI GIAN (TIMER LOGIC & IS ENDED)
    // =========================================================================

    @Test
    @DisplayName("Thá»i gian káº¿t thÃºc náº±m á»Ÿ quÃ¡ khá»© -> Tráº£ vá» True (PhiÃªn Ä‘Ã£ káº¿t thÃºc)")
    void isEnded_TimeHasPassed_ShouldReturnTrue() {
        String auctionId = "TEST_AUC_03";
        AuctionListDTO mockAuction = new AuctionListDTO();
        mockAuction.setAuctionId(auctionId);

        String pastTime = LocalDateTime.now().minusDays(1).format(formatter);
        mockAuction.setEndDateTime(pastTime);

        dataManager.getServerAuctionList().add(mockAuction);

        assertTrue(dataManager.isEnded(auctionId), "Thá»i gian Ä‘Ã£ qua, hÃ m pháº£i tráº£ vá» true");
    }

    @Test
    @DisplayName("Thá»i gian káº¿t thÃºc náº±m á»Ÿ tÆ°Æ¡ng lai -> Tráº£ vá» False (PhiÃªn Ä‘ang diá»…n ra)")
    void isEnded_TimeInFuture_ShouldReturnFalse() {
        String auctionId = "TEST_AUC_04";
        AuctionListDTO mockAuction = new AuctionListDTO();
        mockAuction.setAuctionId(auctionId);

        String futureTime = LocalDateTime.now().plusDays(1).format(formatter);
        mockAuction.setEndDateTime(futureTime);

        dataManager.getServerAuctionList().add(mockAuction);

        assertFalse(dataManager.isEnded(auctionId), "Thá»i gian á»Ÿ tÆ°Æ¡ng lai, hÃ m chÆ°a thá»ƒ tráº£ vá» true");
    }

    @Test
    @DisplayName("KhÃ´ng tÃ¬m tháº¥y ID phiÃªn Ä‘áº¥u giÃ¡ trong danh sÃ¡ch -> Tráº£ vá» False (TrÃ¡nh lá»—i)")
    void isEnded_AuctionIdNotFound_ShouldReturnFalse() {
        String unknownAuctionId = "UNKNOWN_ID";

        assertFalse(dataManager.isEnded(unknownAuctionId), "KhÃ´ng tÃ¬m tháº¥y phiÃªn Ä‘áº¥u giÃ¡, máº·c Ä‘á»‹nh tráº£ vá» false");
    }
}
