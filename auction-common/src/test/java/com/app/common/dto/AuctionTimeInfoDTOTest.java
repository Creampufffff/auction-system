package com.app.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Kiểm thử logic thời gian của AuctionTimeInfoDTO")
public class AuctionTimeInfoDTOTest {

    // =========================================================================
    // TEST HÀM getStatus() VỚI CÁC MỐC THỜI GIAN KHÁC NHAU
    // =========================================================================

    @Test
    @DisplayName("Trạng thái ENDED: Khi thời gian còn lại là số âm (đã quá hạn)")
    void getStatus_SecondsRemainingIsNegative_ShouldReturnEnded() {
        // Given: Thời gian còn lại là số âm (đã quá giờ kết thúc)
        long secondsRemaining = -15;
        AuctionTimeInfoDTO dto = new AuctionTimeInfoDTO("AUC-01", "2026-05-18 22:00:00", secondsRemaining, 0, false);

        // When & Then
        assertEquals("ENDED", dto.getStatus(), "Nếu thời gian âm, trạng thái bắt buộc phải là ENDED");
    }

    @Test
    @DisplayName("Trạng thái ENDING_SOON: Khi thời gian còn lại dưới 5 phút (300 giây)")
    void getStatus_SecondsRemainingIsLessThan300_ShouldReturnEndingSoon() {
        // Given: Thời gian còn lại dưới 300 giây (dưới 5 phút, ví dụ: 299 giây và 0 giây)
        AuctionTimeInfoDTO dtoBoundaryLower = new AuctionTimeInfoDTO("AUC-02", "2026-05-18 22:05:00", 0, 0, true);
        AuctionTimeInfoDTO dtoBoundaryUpper = new AuctionTimeInfoDTO("AUC-02", "2026-05-18 22:05:00", 299, 1, true);

        // When & Then
        assertEquals("ENDING_SOON", dtoBoundaryLower.getStatus(), "Thời gian bằng 0 giây phải là ENDING_SOON");
        assertEquals("ENDING_SOON", dtoBoundaryUpper.getStatus(), "Thời gian 299 giây (gần 5 phút) phải là ENDING_SOON");
    }

    @Test
    @DisplayName("Trạng thái ACTIVE: Khi thời gian còn lại từ 5 phút đến dưới 1 tiếng")
    void getStatus_SecondsRemainingIsLessThan3600_ShouldReturnActive() {
        // Given: Thời gian từ 300 giây đến dưới 3600 giây (từ 5 phút đến dưới 1 tiếng)
        AuctionTimeInfoDTO dtoBoundaryLower = new AuctionTimeInfoDTO("AUC-03", "2026-05-18 22:30:00", 300, 0, true);
        AuctionTimeInfoDTO dtoBoundaryUpper = new AuctionTimeInfoDTO("AUC-03", "2026-05-18 22:30:00", 3599, 0, true);

        // When & Then
        assertEquals("ACTIVE", dtoBoundaryLower.getStatus(), "Đúng 300 giây (5 phút) phải chuyển sang trạng thái ACTIVE");
        assertEquals("ACTIVE", dtoBoundaryUpper.getStatus(), "3599 giây (gần 1 tiếng) phải là trạng thái ACTIVE");
    }

    @Test
    @DisplayName("Trạng thái PLENTY_OF_TIME: Khi thời gian còn lại từ 1 tiếng trở lên")
    void getStatus_SecondsRemainingIs3600OrMore_ShouldReturnPlentyOfTime() {
        // Given: Thời gian từ 3600 giây trở lên (từ 1 tiếng trở lên)
        AuctionTimeInfoDTO dtoBoundaryLower = new AuctionTimeInfoDTO("AUC-04", "2026-05-19 00:00:00", 3600, 0, true);
        AuctionTimeInfoDTO dtoNormal = new AuctionTimeInfoDTO("AUC-04", "2026-05-25 00:00:00", 86400, 0, true); // 1 ngày

        // When & Then
        assertEquals("PLENTY_OF_TIME", dtoBoundaryLower.getStatus(), "Đúng 3600 giây (1 tiếng) phải là PLENTY_OF_TIME");
        assertEquals("PLENTY_OF_TIME", dtoNormal.getStatus(), "Thời gian còn rất dài (86400 giây) phải là PLENTY_OF_TIME");
    }

    // =========================================================================
    // TEST KIỂM TRA ĐỘ CHÍNH XÁC CỦA GETTER (DATA INTEGRITY)
    // =========================================================================

    @Test
    @DisplayName("Khởi tạo đối tượng: Kiểm tra dữ liệu được gán chính xác thông qua Constructor")
    void testGetters_ShouldReturnCorrectConstructorValues() {
        // Given
        String expectedId = "AUC-100";
        String expectedEndTime = "2026-05-20 15:00:00";
        long expectedSeconds = 7200;
        int expectedExtensions = 3;
        boolean expectedCanExtend = true;

        // When
        AuctionTimeInfoDTO dto = new AuctionTimeInfoDTO(expectedId, expectedEndTime, expectedSeconds, expectedExtensions, expectedCanExtend);

        // Then: Đảm bảo dữ liệu truyền vào constructor không bị gán sai biến
        assertAll("Kiểm tra toàn bộ các hàm getter cơ bản",
                () -> assertEquals(expectedId, dto.getAuctionId()),
                () -> assertEquals(expectedEndTime, dto.getEndDateTime()),
                () -> assertEquals(expectedSeconds, dto.getSecondsRemaining()),
                () -> assertEquals(expectedExtensions, dto.getExtensionCount()),
                () -> assertEquals(expectedCanExtend, dto.isCanBeExtended())
        );
    }
}