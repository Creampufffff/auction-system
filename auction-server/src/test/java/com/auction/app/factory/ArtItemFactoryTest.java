package com.auction.app.factory;

import com.app.common.entity.Art;
import com.app.common.entity.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ArtItemFactory")
class ArtItemFactoryTest {

    private ArtItemFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ArtItemFactory();
    }

    @Test
    @DisplayName("create: Tạo Art Item thành công với mảng 8 tham số hợp lệ")
    void create_ValidArgs_ReturnsArtItem() {
        String[] args = {
                "Mona Lisa",                // name (0)
                "Bức tranh sơn dầu",        // description (1)
                "2026-06-01T10:00:00",      // startDate (2)
                "2026-06-15T20:00:00",      // endDate (3)
                "5000.0",                   // startPrice (4)
                "100.0",                    // minIncrement (5)
                "Leonardo da Vinci",        // author (6)
                "seller123"                 // sellerId (7)
        };

        Item item = factory.create(args);

        assertNotNull(item);
        assertInstanceOf(Art.class, item);
        Art art = (Art) item;

        assertEquals("Mona Lisa", art.getName());
        assertEquals("Bức tranh sơn dầu", art.getDescription());
        assertEquals(5000.0, art.getStartPrice());
        assertEquals(100.0, art.getMinIncreasement());
        assertEquals("Leonardo da Vinci", art.getAuthor());
        assertEquals("seller123", art.getSellerId());
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu mảng args là null hoặc không đủ 8 tham số")
    void create_InvalidArgsLength_ThrowsException() {
        String[] shortArgs = {"Mona Lisa", "Desc", "start", "end", "100", "10", "Author"};

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> factory.create(null));
        assertTrue(ex1.getMessage().contains("requires 8 parameters"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> factory.create(shortArgs));
        assertTrue(ex2.getMessage().contains("requires 8 parameters"));
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu một tham số bắt buộc bị rỗng (Blank)")
    void create_BlankParameter_ThrowsException() {
        String[] argsWithBlankName = {
                "   ", "Desc", "start", "end", "100", "10", "Author", "sellerId"
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(argsWithBlankName));
        assertEquals("name cannot be empty", ex.getMessage());
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu giá (Price) không phải số thực")
    void create_InvalidPriceFormat_ThrowsException() {
        String[] args = {
                "Mona Lisa", "Desc", "start", "end", "Năm ngàn", "100.0", "Author", "sellerId"
        };

        assertThrows(NumberFormatException.class, () -> factory.create(args));
    }
}