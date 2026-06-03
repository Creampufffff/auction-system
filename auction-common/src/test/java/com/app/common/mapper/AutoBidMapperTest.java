package com.app.common.mapper;

import com.app.common.dto.AutoBidDTO;
import com.app.common.dto.SetAutoBidRequestDTO;
import com.app.common.entity.AutoBid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử AutoBidMapper")
class AutoBidMapperTest {

    @Test
    @DisplayName("toDTO: Trả về DTO hợp lệ khi truyền AutoBid hợp lệ")
    void toDTO_ValidAutoBid_ReturnsDTO() {
        AutoBid autoBid = new AutoBid("auction-123", "bidder-456", 1500.0);
        autoBid.setId("autobid-789"); // Kế thừa từ BaseEntity

        AutoBidDTO dto = AutoBidMapper.toDTO(autoBid, "Bức tranh Mona Lisa");

        assertNotNull(dto);
        assertEquals("autobid-789", dto.getAutoBidId());
        assertEquals("auction-123", dto.getAuctionId());
        assertEquals("Bức tranh Mona Lisa", dto.getItemName());
        assertEquals("bidder-456", dto.getBidderId());
        assertEquals(1500.0, dto.getMaxAutoAmount());
        assertTrue(dto.isActive());
    }

    @Test
    @DisplayName("toDTO: Trả về null nếu đầu vào là null")
    void toDTO_NullAutoBid_ReturnsNull() {
        assertNull(AutoBidMapper.toDTO(null, "Tên sản phẩm"));
    }

    @Test
    @DisplayName("toEntity: Trả về Entity hợp lệ khi truyền Request DTO")
    void toEntity_ValidRequest_ReturnsEntity() {
        SetAutoBidRequestDTO request = new SetAutoBidRequestDTO("auction-1", "bidder-1", 2000.0);

        AutoBid entity = AutoBidMapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("auction-1", entity.getAuctionId());
        assertEquals("bidder-1", entity.getBidderId());
        assertEquals(2000.0, entity.getMaxAutoAmount());
        assertTrue(entity.isActive()); // Constructor tự set true
    }

    @Test
    @DisplayName("toEntity: Trả về null nếu đầu vào là null")
    void toEntity_NullRequest_ReturnsNull() {
        assertNull(AutoBidMapper.toEntity(null));
    }
}