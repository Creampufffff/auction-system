package com.auction.app.service;

import com.app.common.entity.Art;
import com.app.common.entity.Item;
import com.auction.app.repository.ItemDAO;
import com.auction.app.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử ItemServiceImpl")
class ItemServiceImplTest {

    @Mock
    private ItemDAO itemDAO;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item validItem;

    @BeforeEach
    void setUp() {
        // Vì Item là Abstract Class, ta dùng Art (một class con) để tạo dữ liệu test
        validItem = new Art(
                "Mô tả chi tiết bức tranh",
                "Bức tranh mùa thu",
                "2026-06-01T08:00:00",
                "2026-06-15T20:00:00",
                100.0,
                10.0,
                "Nguyễn Văn A"
        );
        validItem.setId("item-123");
    }

    // =========================================================================
    // 1. TEST CHỨC NĂNG LƯU VẬT PHẨM (SAVE ITEM)
    // =========================================================================

    @Test
    @DisplayName("saveItem: Lưu thành công khi dữ liệu hợp lệ")
    void saveItem_ValidItem_ShouldSaveSuccessfully() {
        when(itemDAO.save(validItem)).thenReturn(true);

        assertDoesNotThrow(() -> itemService.saveItem(validItem));
        verify(itemDAO, times(1)).save(validItem);
    }

    @Test
    @DisplayName("saveItem: Ném ngoại lệ nếu DAO lưu thất bại")
    void saveItem_DaoFails_ShouldThrowIllegalStateException() {
        when(itemDAO.save(validItem)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> itemService.saveItem(validItem));
        assertEquals("Failed to save item", exception.getMessage());
    }

    @Test
    @DisplayName("saveItem: Bắt lỗi Validation - Item null")
    void saveItem_NullItem_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> itemService.saveItem(null));
        assertEquals("Item cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("saveItem: Bắt lỗi Validation - Tên bị trống")
    void saveItem_BlankName_ShouldThrowIllegalArgumentException() {
        Item invalidItem = new Art("Desc", "", "start", "end", 100, 10, "Author");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> itemService.saveItem(invalidItem));
        assertEquals("Item name cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("saveItem: Bắt lỗi Validation - Thiếu ngày bắt đầu / kết thúc")
    void saveItem_BlankDates_ShouldThrowIllegalArgumentException() {
        Item invalidStart = new Art("Desc", "Name", "", "end", 100, 10, "Author");
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> itemService.saveItem(invalidStart));
        assertEquals("Start date cannot be empty", ex1.getMessage());

        Item invalidEnd = new Art("Desc", "Name", "start", null, 100, 10, "Author");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> itemService.saveItem(invalidEnd));
        assertEquals("End date cannot be empty", ex2.getMessage());
    }

    @Test
    @DisplayName("saveItem: Bắt lỗi Validation - Giá khởi điểm < 0")
    void saveItem_NegativeStartPrice_ShouldThrowIllegalArgumentException() {
        Item invalidItem = new Art("Desc", "Name", "start", "end", -5.0, 10, "Author");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> itemService.saveItem(invalidItem));
        assertEquals("Start price cannot be less than 0", exception.getMessage());
    }

    @Test
    @DisplayName("saveItem: Bắt lỗi Validation - Bước giá <= 0")
    void saveItem_InvalidMinIncrement_ShouldThrowIllegalArgumentException() {
        Item invalidItem = new Art("Desc", "Name", "start", "end", 100, 0.0, "Author");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> itemService.saveItem(invalidItem));
        assertEquals("Minimum increment must be greater than 0", exception.getMessage());
    }

    // =========================================================================
    // 2. TEST CHỨC NĂNG TÌM KIẾM (GET BY ID & GET ALL)
    // =========================================================================

    @Test
    @DisplayName("getById: Trả về Item khi ID hợp lệ")
    void getById_ValidId_ShouldReturnItem() {
        when(itemDAO.findById("item-123")).thenReturn(validItem);

        Item result = itemService.getById("item-123");

        assertNotNull(result);
        assertEquals("item-123", result.getId());
        assertEquals("Bức tranh mùa thu", result.getName());
    }

    @Test
    @DisplayName("getById: Bắt lỗi Validation khi ID rỗng")
    void getById_BlankId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> itemService.getById(""));
        assertThrows(IllegalArgumentException.class, () -> itemService.getById(null));
    }

    @Test
    @DisplayName("getItemsList: Lấy toàn bộ danh sách Item từ DAO")
    void getItemsList_ShouldReturnListOfItems() {
        List<Item> mockList = Arrays.asList(validItem, validItem);
        when(itemDAO.findAll()).thenReturn(mockList);

        List<Item> result = itemService.getItemsList();

        assertEquals(2, result.size());
        verify(itemDAO, times(1)).findAll();
    }

    // =========================================================================
    // 3. TEST CHỨC NĂNG XÓA (DELETE ITEM)
    // =========================================================================

    @Test
    @DisplayName("deleteItem: Xóa thành công khi DAO trả về true")
    void deleteItem_ValidId_ShouldDeleteSuccessfully() {
        when(itemDAO.delete("item-123")).thenReturn(true);

        assertDoesNotThrow(() -> itemService.deleteItem("item-123"));
        verify(itemDAO, times(1)).delete("item-123");
    }

    @Test
    @DisplayName("deleteItem: Ném ngoại lệ nếu DAO trả về false (không tìm thấy Item)")
    void deleteItem_NotFound_ShouldThrowIllegalArgumentException() {
        when(itemDAO.delete("ghost-id")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> itemService.deleteItem("ghost-id"));
        assertEquals("Item not found with id: ghost-id", exception.getMessage());
    }

    @Test
    @DisplayName("deleteItem: Bắt lỗi Validation khi ID rỗng")
    void deleteItem_BlankId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> itemService.deleteItem("   "));
        assertThrows(IllegalArgumentException.class, () -> itemService.deleteItem(null));
    }
}