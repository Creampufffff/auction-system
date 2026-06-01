package com.auction.app.factory;

import com.app.common.entity.Electronics;
import com.app.common.entity.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ElectronicsItemFactory")
class ElectronicsItemFactoryTest {

    private ElectronicsItemFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ElectronicsItemFactory();
    }

    @Test
    @DisplayName("create: Tạo Electronics Item thành công với mảng 8 tham số hợp lệ")
    void create_ValidArgs_ReturnsElectronicsItem() {
        String[] args = {
                "iPhone 15 Pro",            // name (0)
                "Điện thoại Apple",         // description (1)
                "2026-06-01T10:00:00",      // startDate (2)
                "2026-06-15T20:00:00",      // endDate (3)
                "1000.0",                   // startPrice (4)
                "50.0",                     // minIncrement (5)
                "12",                       // warrantyMonths (6)
                "seller123"                 // sellerId (7)
        };

        Item item = factory.create(args);

        assertNotNull(item);
        assertInstanceOf(Electronics.class, item);
        Electronics electronics = (Electronics) item;

        assertEquals("iPhone 15 Pro", electronics.getName());
        assertEquals("Điện thoại Apple", electronics.getDescription());
        assertEquals(1000.0, electronics.getStartPrice());
        assertEquals(50.0, electronics.getMinIncreasement());
        assertEquals(12, electronics.getWarrantyMonths());
        assertEquals("seller123", electronics.getSellerId());
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu mảng args là null hoặc không đủ 8 tham số")
    void create_InvalidArgsLength_ThrowsException() {
        String[] shortArgs = {"iPhone", "Desc", "start", "end", "1000", "50", "12"};

        assertThrows(IllegalArgumentException.class, () -> factory.create(null));
        assertThrows(IllegalArgumentException.class, () -> factory.create(shortArgs));
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu một tham số bắt buộc bị rỗng (Blank)")
    void create_BlankParameter_ThrowsException() {
        String[] argsWithBlankDesc = {
                "iPhone", "   ", "start", "end", "1000", "50", "12", "sellerId"
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(argsWithBlankDesc));
        assertEquals("description cannot be empty", ex.getMessage());
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu warrantyMonths không phải là số nguyên")
    void create_InvalidWarrantyFormat_ThrowsException() {
        String[] args = {
                "iPhone", "Desc", "start", "end", "1000", "50", "Mười hai tháng", "seller123"
        };

        assertThrows(NumberFormatException.class, () -> factory.create(args));
    }
}