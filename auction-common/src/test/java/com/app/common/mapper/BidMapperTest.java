package com.app.common.mapper;

import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.entity.Item;
import com.app.common.entity.Art;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử BidMapper")
class BidMapperTest {

    @Test
    @DisplayName("toEntity: Tạo BidTransaction thành công với các đối tượng hợp lệ")
    void toEntity_ValidInputs_ReturnsBidTransaction() {
        PlaceBidRequestDTO dto = new PlaceBidRequestDTO("auction-1", "bidder-1", 500.0);
        Bidder bidder = new Bidder("tamnguyen", "pass", "email@uet.edu.vn");

        Item item = new Art("Desc", "Tranh", "start", "end", 100, 10, "Author");
        Auction auction = new Auction(item);

        BidTransaction transaction = BidMapper.toEntity(dto, bidder, auction);

        assertNotNull(transaction);
        assertEquals(bidder, transaction.getBidder());
        assertEquals(auction, transaction.getAuction());
        assertEquals(500.0, transaction.getBidAmount());
    }

    @Test
    @DisplayName("toEntity: Trả về null nếu bất kỳ tham số nào là null")
    void toEntity_NullInputs_ReturnsNull() {
        PlaceBidRequestDTO dto = new PlaceBidRequestDTO("a1", "b1", 100);
        Bidder bidder = new Bidder("user", "pass", "mail");
        Auction auction = new Auction(null);

        assertNull(BidMapper.toEntity(null, bidder, auction));
        assertNull(BidMapper.toEntity(dto, null, auction));
        assertNull(BidMapper.toEntity(dto, bidder, null));
    }
}