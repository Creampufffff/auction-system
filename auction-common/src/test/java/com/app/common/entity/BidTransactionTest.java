package com.app.common.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Kiểm thử logic nghiệp vụ của thực thể BidTransaction")
public class BidTransactionTest {

    // =========================================================================
    // 1. TEST KHỞI TẠO RỖNG & TỰ ĐỘNG SINH ID (KẾ THỪA BASE ENTITY)
    // =========================================================================

    @Test
    @DisplayName("Khởi tạo rỗng: Kế thừa BaseEntity phải tự động sinh UUID hợp lệ")
    void constructor_Empty_ShouldGenerateId() {
        // When: Tạo một giao dịch mới bằng constructor rỗng
        BidTransaction bid = new BidTransaction();

        // Then: Dù không truyền gì, lớp BaseEntity cha vẫn phải cấp cho nó một mã UUID
        assertNotNull(bid.getId(), "Khi khởi tạo, kế thừa từ BaseEntity phải tự động sinh ra chuỗi UUID ID");
    }

    // =========================================================================
    // 2. TEST KHỞI TẠO CÓ THAM SỐ (PARAMETERIZED CONSTRUCTOR)
    // =========================================================================

    @Test
    @DisplayName("Khởi tạo có tham số: Map chính xác dữ liệu Bidder, Auction và số tiền đặt giá")
    void constructor_WithArgs_ShouldInitializeCorrectly() {
        // Given: Chuẩn bị sẵn dữ liệu giả (Mock data) cho Bidder và Auction
        Bidder mockBidder = new Bidder("tam_bidder", "pass123", "tam@uet.vnu.edu.vn");

        Art mockArt = new Art("Mô tả tranh", "Mona Lisa", "2026-05-18", "2026-05-25", 1000.0, 50.0, "Da Vinci");
        Auction mockAuction = new Auction(mockArt);

        double expectedBidAmount = 1500.0;

        // When
        BidTransaction bid = new BidTransaction(mockBidder, mockAuction, expectedBidAmount);

        // Then: Đảm bảo dữ liệu gán vào không bị nhầm lẫn vị trí
        assertNotNull(bid.getId(), "ID phải được tự động sinh ngay cả khi dùng constructor có tham số");
        assertEquals(mockBidder, bid.getBidder(), "Phải map đúng đối tượng người đặt giá");
        assertEquals(mockAuction, bid.getAuction(), "Phải map đúng đối tượng phiên đấu giá");
        assertEquals(expectedBidAmount, bid.getBidAmount(), "Phải map đúng số tiền đặt giá");
    }

    // =========================================================================
    // 3. TEST CÁC HÀM GETTER / SETTER
    // =========================================================================

    @Test
    @DisplayName("Setters/Getters: Cập nhật và truy xuất dữ liệu chính xác cho các trường")
    void settersAndGetters_ShouldUpdateFieldsCorrectly() {
        // Given
        BidTransaction bid = new BidTransaction();

        Bidder newBidder = new Bidder("tam_bidder", "pass123", "tam@uet.vnu.edu.vn");
        Auction newAuction = new Auction(new Vehicle("Xe cũ", "Vinfast", "start", "end", 200, 10, "Vinfast"));
        double newAmount = 250.0;

        // When
        bid.setBidder(newBidder);
        bid.setAuction(newAuction);
        bid.setBidAmount(newAmount);

        // Then
        assertEquals(newBidder, bid.getBidder());
        assertEquals(newAuction, bid.getAuction());
        assertEquals(newAmount, bid.getBidAmount());
    }
}