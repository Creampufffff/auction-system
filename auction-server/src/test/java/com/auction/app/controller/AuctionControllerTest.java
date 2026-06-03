package com.auction.app.controller;

import com.app.common.dto.ApiResponseDTO;
import com.app.common.dto.CreateAuctionRequestDTO;
import com.auction.app.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử AuctionController (Giao tiếp giữa Client và Service)")
class AuctionControllerTest {

    private AuctionService auctionService;
    private AuctionController controller;

    @BeforeEach
    void setUp() {
        // Giả lập (Mock) tầng Service để cô lập hoàn toàn Controller khỏi Database
        auctionService = Mockito.mock(AuctionService.class);
        controller = new AuctionController(auctionService);
    }

    // =========================================================================
    // 1. TEST LOGIC TẠO PHIÊN ĐẤU GIÁ (createAuction)
    // =========================================================================

    @Test
    @DisplayName("createAuction: Tạo thành công khi dữ liệu đầu vào hợp lệ")
    void createAuction_success() {
        // Given: Dữ liệu hợp lệ với type "ART"
        CreateAuctionRequestDTO dto = new CreateAuctionRequestDTO(
                "Mona Lisa", "A famous painting", "New", "None",
                1000.0, 10.0, "2026-06-01T10:00:00", "2026-06-01T12:00:00",
                "seller-1", "ART"
        );

        // Quy định hành vi cho mock: Khi gọi saveAuction thì không làm gì cả (doNothing)
        doNothing().when(auctionService).saveAuction(any());

        // When
        ApiResponseDTO resp = controller.createAuction(dto);

        // Then
        assertNotNull(resp);
        assertTrue(resp.isSuccess(), "Phải trả về trạng thái success = true");
        assertTrue(resp.getMessage().toLowerCase().contains("created"));

        // Xác minh cực kỳ quan trọng: Đảm bảo Controller CÓ gọi xuống Service đúng 1 lần
        verify(auctionService, times(1)).saveAuction(any());
    }

    @Test
    @DisplayName("createAuction: Báo lỗi khi loại sản phẩm không tồn tại (Mapper trả null)")
    void createAuction_InvalidType_ShouldReturnError() {
        // Given: Dữ liệu bị sai Type (Ví dụ: FASHION không được hệ thống hỗ trợ)
        CreateAuctionRequestDTO dto = new CreateAuctionRequestDTO(
                "Áo khoác", "Workwear", "New", "None",
                100.0, 10.0, "2026-06-01T10:00:00", "2026-06-01T12:00:00",
                "seller-1", "FASHION" // Type lạ
        );

        // When
        ApiResponseDTO resp = controller.createAuction(dto);

        // Then
        assertNotNull(resp);
        assertFalse(resp.isSuccess(), "Phải chặn lại và trả về false");
        assertEquals("Invalid item type", resp.getMessage());

        // Xác minh: Đảm bảo Controller KHÔNG BAO GIỜ gọi hàm saveAuction nếu data bị lỗi
        verify(auctionService, never()).saveAuction(any());
    }

    // =========================================================================
    // 2. TEST LOGIC BẮT ĐẦU PHIÊN ĐẤU GIÁ (startAuction)
    // =========================================================================

    @Test
    @DisplayName("startAuction: Trả về thành công nếu Service không ném lỗi")
    void startAuction_success() {
        // Given
        String auctionId = "AUC-111";
        doNothing().when(auctionService).startAuction(auctionId);

        // When
        ApiResponseDTO resp = controller.startAuction(auctionId);

        // Then
        assertTrue(resp.isSuccess());
        assertEquals("Auction started successfully", resp.getMessage());
        verify(auctionService, times(1)).startAuction(auctionId);
    }

    @Test
    @DisplayName("startAuction: Bắt lỗi duyên dáng (Graceful Exception) khi Service ném Exception")
    void startAuction_failure() {
        // Given
        String auctionId = "nonexistent";
        // Giả lập Service ném lỗi khi không tìm thấy Auction
        doThrow(new IllegalArgumentException("Auction not found")).when(auctionService).startAuction(auctionId);

        // When
        ApiResponseDTO resp = controller.startAuction(auctionId);

        // Then: Controller không được làm sập App (Crash), mà phải bọc lỗi vào ApiResponseDTO
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().contains("Error starting auction: Auction not found"));
    }

    // =========================================================================
    // 3. TEST LOGIC KẾT THÚC PHIÊN ĐẤU GIÁ (endAuction)
    // =========================================================================

    @Test
    @DisplayName("endAuction: Trả về thành công khi kết thúc phiên")
    void endAuction_success() {
        // Given
        String auctionId = "AUC-222";
        doNothing().when(auctionService).endAuction(auctionId);

        // When
        ApiResponseDTO resp = controller.endAuction(auctionId);

        // Then
        assertTrue(resp.isSuccess());
        assertEquals("Auction ended successfully", resp.getMessage());
        verify(auctionService, times(1)).endAuction(auctionId);
    }
}