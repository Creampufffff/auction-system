package com.app.common.mapper;

// Kiểm tra xem với mỗi loại chuỗi truyền vào ("ART", "ELECTRONICS", "VEHICLE"), hệ thống có đúc ra đúng
// class con tương ứng và gán các giá trị mặc định chuẩn xác hay không.

import com.app.common.dto.CreateAuctionRequestDTO;
import com.app.common.entity.Art;
import com.app.common.entity.Electronics;
import com.app.common.entity.Item;
import com.app.common.entity.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử logic khởi tạo đối tượng đa hình của ItemMapper")
public class ItemMapperTest {

    // Hàm phụ trợ (Helper method) để tạo nhanh DTO nhằm tái sử dụng code, tránh lặp lại
    private CreateAuctionRequestDTO createBaseDto(String itemType) {
        return new CreateAuctionRequestDTO(
                "Tên sản phẩm test",
                "Mô tả test",
                "Mới 100%",
                "12 tháng",
                1500.0,
                50.0,
                "2026-05-18",
                "2026-05-25",
                "SELLER-999",
                itemType
        );
    }

    // =========================================================================
    // 1. TEST LOGIC ÉP KIỂU ĐA HÌNH (POLYMORPHISM MAPPING)
    // =========================================================================

    @Test
    @DisplayName("createItem: Type 'ART' sinh ra đối tượng Art với Author lấy từ trường condition")
    void createItem_ArtType_ShouldReturnArtEntityWithUnknownAuthor() {
        // Given
        CreateAuctionRequestDTO dto = createBaseDto("ART");

        // When
        Item item = ItemMapper.createItem(dto);

        // Then
        assertNotNull(item);
        // Kiểm tra xem Item sinh ra có chuẩn xác là đối tượng của class Art không
        assertInstanceOf(Art.class, item, "Type 'ART' phải sinh ra class Art");

        // Ép kiểu để kiểm tra thuộc tính riêng của Art
        Art artItem = (Art) item;
        assertEquals("Mới 100%", artItem.getAuthor(), "Tác phẩm nghệ thuật phải lấy giá trị từ trường condition (nếu có)");
        assertEquals("SELLER-999", artItem.getSellerId(), "Phải map chính xác mã người bán");
    }

    @Test
    @DisplayName("createItem: Type 'electronics' (không phân biệt hoa thường) sinh ra đối tượng Electronics")
    void createItem_ElectronicsType_ShouldReturnElectronicsEntityWithDefaultWarranty() {
        // Given: Test với chữ thường xem hàm equalsIgnoreCase có bắt được không
        CreateAuctionRequestDTO dto = createBaseDto("electronics");

        // When
        Item item = ItemMapper.createItem(dto);

        // Then
        assertNotNull(item);
        assertInstanceOf(Electronics.class, item, "Type 'electronics' phải sinh ra class Electronics");

        Electronics electronicsItem = (Electronics) item;
        assertEquals(12, electronicsItem.getWarrantyMonths(), "Đồ điện tử phải parse số tháng bảo hành từ String");
        assertEquals("Tên sản phẩm test", electronicsItem.getName(), "Phải map chính xác thông tin chung của Base Item");
    }

    @Test
    @DisplayName("createItem: Type 'VEHICLE' sinh ra đối tượng Vehicle với Brand lấy từ trường warranty")
    void createItem_VehicleType_ShouldReturnVehicleEntityWithUnknownBrand() {
        // Given
        CreateAuctionRequestDTO dto = createBaseDto("VEHICLE");

        // When
        Item item = ItemMapper.createItem(dto);

        // Then
        assertNotNull(item);
        assertInstanceOf(Vehicle.class, item, "Type 'VEHICLE' phải sinh ra class Vehicle");

        Vehicle vehicleItem = (Vehicle) item;
        assertEquals("12 tháng", vehicleItem.getBrand(), "Xe cộ phải lấy giá trị brand từ trường warranty");
        assertEquals(1500.0, vehicleItem.getStartPrice(), "Phải map chính xác giá khởi điểm");
    }

    // =========================================================================
    // 2. TEST CÁC TRƯỜNG HỢP NGOẠI LỆ (EDGE CASES)
    // =========================================================================

    @Test
    @DisplayName("createItem: Trả về null an toàn nếu loại sản phẩm không được hỗ trợ")
    void createItem_UnknownType_ShouldReturnNull() {
        // Given: Truyền vào một Type không nằm trong danh sách hỗ trợ
        CreateAuctionRequestDTO dto = createBaseDto("FASHION_CLOTHES");

        // When
        Item item = ItemMapper.createItem(dto);

        // Then: Theo logic của Mapper, nếu if-else không bắt được type nào thì item = null
        assertNull(item, "Với Item Type lạ không hỗ trợ, hệ thống phải trả về null thay vì ném lỗi vỡ App");
    }

    @Test
    @DisplayName("createItem: Trả về null an toàn khi DTO đầu vào bị null")
    void createItem_NullDto_ShouldReturnNull() {
        // Đảm bảo không bị NullPointerException khi request đầu vào bị hỏng
        assertNull(ItemMapper.createItem(null), "Truyền DTO null thì phải trả về null");
    }
}