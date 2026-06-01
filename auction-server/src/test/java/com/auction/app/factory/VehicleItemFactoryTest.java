package com.auction.app.factory;

import com.app.common.entity.Item;
import com.app.common.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử VehicleItemFactory")
class VehicleItemFactoryTest {

    private VehicleItemFactory factory;

    @BeforeEach
    void setUp() {
        factory = new VehicleItemFactory();
    }

    @Test
    @DisplayName("create: Tạo Vehicle Item thành công với mảng 8 tham số hợp lệ")
    void create_ValidArgs_ReturnsVehicleItem() {
        String[] args = {
                "Honda Civic 2024",         // name (0)
                "Xe sedan màu đen",         // description (1)
                "2026-06-01T10:00:00",      // startDate (2)
                "2026-06-15T20:00:00",      // endDate (3)
                "25000.0",                  // startPrice (4)
                "500.0",                    // minIncrement (5)
                "Honda",                    // brand (6)
                "seller123"                 // sellerId (7)
        };

        Item item = factory.create(args);

        assertNotNull(item);
        assertInstanceOf(Vehicle.class, item);
        Vehicle vehicle = (Vehicle) item;

        assertEquals("Honda Civic 2024", vehicle.getName());
        assertEquals("Xe sedan màu đen", vehicle.getDescription());
        assertEquals(25000.0, vehicle.getStartPrice());
        assertEquals(500.0, vehicle.getMinIncreasement());
        assertEquals("Honda", vehicle.getBrand());
        assertEquals("seller123", vehicle.getSellerId());
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu mảng args là null hoặc không đủ 8 tham số")
    void create_InvalidArgsLength_ThrowsException() {
        String[] shortArgs = {"Civic", "Desc", "start", "end", "25000", "500", "Honda"};

        assertThrows(IllegalArgumentException.class, () -> factory.create(null));
        assertThrows(IllegalArgumentException.class, () -> factory.create(shortArgs));
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu một tham số bắt buộc bị rỗng (Blank)")
    void create_BlankParameter_ThrowsException() {
        String[] argsWithBlankBrand = {
                "Civic", "Desc", "start", "end", "25000", "500", "   ", "sellerId"
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.create(argsWithBlankBrand));
        assertEquals("brand cannot be empty", ex.getMessage());
    }

    @Test
    @DisplayName("create: Bắt lỗi nếu bước giá (minIncrement) không phải số thực")
    void create_InvalidMinIncrementFormat_ThrowsException() {
        String[] args = {
                "Civic", "Desc", "start", "end", "25000", "Năm trăm", "Honda", "sellerId"
        };

        assertThrows(NumberFormatException.class, () -> factory.create(args));
    }
}