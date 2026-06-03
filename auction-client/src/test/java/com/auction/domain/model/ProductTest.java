package com.auction.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Mô hình Sản phẩm (Product)")
class ProductTest {

    // =========================================================================
    // 1. TEST CÁC CONSTRUCTOR KHỞI TẠO DỮ LIỆU
    // =========================================================================

    @Test
    @DisplayName("Khởi tạo Product với constructor đầy đủ tham số")
    void constructor_AllArgs_InitializesCorrectly() {
        Product product = new Product(
                "p1",
                "ELECTRONICS",
                "Laptop Gaming",
                1500.0,
                "OPEN",
                "New",
                "Mô tả chi tiết",
                "12 tháng",
                "2026-12-31 23:59:59"
        );

        assertEquals("p1", product.getId());
        assertEquals("ELECTRONICS", product.getType());
        assertEquals("Laptop Gaming", product.getName());
        assertEquals(1500.0, product.getPrice());
        assertEquals("OPEN", product.getStatus());
        assertEquals("New", product.getCondition());
        assertEquals("Mô tả chi tiết", product.getDescription());
        assertEquals("12 tháng", product.getWarranty());
        assertEquals("2026-12-31 23:59:59", product.getEndDateTime());
    }

    @Test
    @DisplayName("Khởi tạo Product với constructor ẩn (mặc định loại ART)")
    void constructor_DefaultTypeArt_InitializesCorrectly() {
        Product product = new Product(
                "p2",
                "Bức tranh phong cảnh",
                500.0,
                "FINISHED",
                "Tốt",
                "Tranh sơn dầu",
                "Không"
        );

        assertEquals("p2", product.getId());
        assertEquals("ART", product.getType(), "Nếu không truyền type, mặc định phải là ART");
        assertEquals("Bức tranh phong cảnh", product.getName());
        assertEquals(500.0, product.getPrice());
        assertNull(product.getEndDateTime(), "Nếu không truyền endDateTime, mặc định phải là null");
    }

    // =========================================================================
    // 2. TEST CÁC HÀM GETTER & SETTER
    // =========================================================================

    @Test
    @DisplayName("Kiểm tra các hàm Setter cập nhật giá trị chính xác")
    void setters_UpdateValuesCorrectly() {
        // Khởi tạo một đối tượng ban đầu
        Product product = new Product("p_old", "Name", 100.0, "OPEN", "New", "Desc", "Warranty");

        // Gọi các setter để thay đổi toàn bộ dữ liệu
        product.setId("p_new");
        product.setType("VEHICLE");
        product.setName("Xe ô tô");
        product.setPrice(25000.0);
        product.setStatus("CLOSED");
        product.setCondition("Cũ");
        product.setDescription("Đã qua sử dụng");
        product.setWarranty("Không bảo hành");
        product.setEndDateTime("2026-06-01 10:00:00");

        // Kiểm tra lại xem dữ liệu đã được cập nhật chưa
        assertEquals("p_new", product.getId());
        assertEquals("VEHICLE", product.getType());
        assertEquals("Xe ô tô", product.getName());
        assertEquals(25000.0, product.getPrice());
        assertEquals("CLOSED", product.getStatus());
        assertEquals("Cũ", product.getCondition());
        assertEquals("Đã qua sử dụng", product.getDescription());
        assertEquals("Không bảo hành", product.getWarranty());
        assertEquals("2026-06-01 10:00:00", product.getEndDateTime());
    }

    // =========================================================================
    // 3. TEST CÁC HÀM JAVAFX PROPERTY (Dùng cho TableView/Binding)
    // =========================================================================

    @Test
    @DisplayName("Kiểm tra các hàm Property trả về đúng đối tượng và giá trị")
    void properties_ReturnNonNullAndCorrectValues() {
        Product product = new Product("p3", "Đồng hồ", 200.0, "RUNNING", "Mới", "Mô tả", "1 năm");

        // Property không được null để JavaFX TableView có thể render được
        assertNotNull(product.idProperty());
        assertNotNull(product.nameProperty());
        assertNotNull(product.priceProperty());
        assertNotNull(product.statusProperty());

        // Giá trị bên trong Property phải khớp với giá trị đã khởi tạo
        assertEquals("p3", product.idProperty().get());
        assertEquals("Đồng hồ", product.nameProperty().get());
        assertEquals(200.0, product.priceProperty().get());
        assertEquals("RUNNING", product.statusProperty().get());
    }
}