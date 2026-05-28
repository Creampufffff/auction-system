package com.app.common.mapper;// Kiểm tra xem cơ chế ép kiểu đa hình của Java có hoạt động đúng khi bóc tách thông tin bảo hành (warranty) từ một sản phẩm điện tử hay không
// Kiểm tra logic tính toán giá hiện tại của phiên đấu giá.

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.BidHistoryDTO;
import com.app.common.dto.CreateAuctionRequestDTO;
import com.app.common.entity.*;
import com.app.common.enums.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử logic chuyển đổi dữ liệu của AuctionMapper")
public class AuctionMapperTest {

    // =========================================================================
    // 1. TEST LOGIC CHUYỂN ĐỔI SANG AUCTION LIST DTO (toListDTO)
    // =========================================================================

    @Test
    @DisplayName("toListDTO: Ép kiểu Electronics thành công và lấy đúng số tháng bảo hành")
    void toListDTO_ElectronicsItem_ShouldMapWarrantyMonths() {
        // Given: Một phiên đấu giá chứa sản phẩm là Điện tử (Electronics) với 24 tháng bảo hành
        Electronics laptop = new Electronics("Macbook M2", "Laptop", "2026-05-18", "2026-05-25", 1500.0, 50.0, 24);
        Auction auction = new Auction(laptop);

        // When
        AuctionListDTO dto = AuctionMapper.toListDTO(auction);

        // Then: Hệ thống phải nhận diện được đây là Electronics và ép kiểu (instanceof) để lấy ra số 24
        assertNotNull(dto);
        assertEquals("24", dto.getWarranty(), "Sản phẩm điện tử phải map được số tháng bảo hành sang String");
    }

    @Test
    @DisplayName("toListDTO: Tác phẩm nghệ thuật (Art) phải có trường bảo hành là null")
    void toListDTO_ArtItem_ShouldReturnNullWarranty() {
        // Given: Một phiên đấu giá chứa Tác phẩm nghệ thuật (Art)
        Art painting = new Art("Mona Lisa", "Tranh", "2026-05-18", "2026-05-25", 5000.0, 100.0, "Da Vinci");
        Auction auction = new Auction(painting);

        // When
        AuctionListDTO dto = AuctionMapper.toListDTO(auction);

        // Then: Art không có thuộc tính bảo hành, nên phải trả về null
        assertNotNull(dto);
        assertNull(dto.getWarranty(), "Sản phẩm không phải đồ điện tử thì trường bảo hành phải là null");
    }

    @Test
    @DisplayName("toListDTO: Hiển thị giá đấu cao nhất (HighestPrice) nếu đã có người trả giá")
    void toListDTO_HighestPriceGreaterThanZero_ShouldUseHighestPrice() {
        // Given: Sản phẩm đã có người đặt giá cao hơn giá khởi điểm
        Electronics phone = new Electronics("iPhone 15", "Phone", "2026-05-18", "2026-05-25", 800.0, 10.0, 12);
        phone.setHighestCurrentPrice(950.0); // Đã có người trả 950$
        Auction auction = new Auction(phone);

        // When
        AuctionListDTO dto = AuctionMapper.toListDTO(auction);

        // Then: DTO phải lấy giá 950$ để hiển thị lên bảng đấu giá, KHÔNG lấy giá 800$
        assertEquals(950.0, dto.getCurrentPrice(), "Nếu đã có người trả giá, phải hiển thị giá cao nhất hiện tại");
    }

    @Test
    @DisplayName("toListDTO: Hiển thị giá khởi điểm (StartPrice) nếu chưa có ai đặt giá")
    void toListDTO_NoBidsYet_ShouldFallbackToStartPrice() {
        // Given: Sản phẩm mới mở, chưa có ai đặt giá (highestCurrentPrice mặc định là 0.0)
        Vehicle car = new Vehicle("Vinfast VF8", "Car", "2026-05-18", "2026-05-25", 40000.0, 500.0, "Vinfast");
        Auction auction = new Auction(car);

        // When
        AuctionListDTO dto = AuctionMapper.toListDTO(auction);

        // Then: Hệ thống phải tự động fallback về mức giá khởi điểm
        assertEquals(40000.0, dto.getCurrentPrice(), "Nếu chưa ai đặt giá, phải hiển thị mức giá khởi điểm");
    }

    @Test
    @DisplayName("toListDTO: Trả về null an toàn khi dữ liệu đầu vào (Auction hoặc Item) bị null")
    void toListDTO_NullInput_ShouldReturnNull() {
        // Đảm bảo hệ thống không bị crash (NullPointerException) khi dữ liệu truyền vào bị null
        assertNull(AuctionMapper.toListDTO(null));

        Auction emptyAuction = new Auction(null); // Auction mất Item
        assertNull(AuctionMapper.toListDTO(emptyAuction));
    }

    // =========================================================================
    // 2. TEST LOGIC CHUYỂN ĐỔI TỪ DTO SANG ENTITY (toEntity)
    // =========================================================================

    @Test
    @DisplayName("toEntity: Phiên đấu giá mới khởi tạo phải có trạng thái mặc định là OPEN")
    void toEntity_ValidDto_ShouldSetStatusToOpen() {
        // Given: Yêu cầu tạo mới một phiên đấu giá
        CreateAuctionRequestDTO dto = new CreateAuctionRequestDTO(
                "iPad Pro", "Tablet", "New", "12 tháng", 600.0, 20.0,
                "2026-05-18", "2026-05-25", "SELLER-01", "ELECTRONICS"
        );

        // When
        Auction auction = AuctionMapper.toEntity(dto);

        // Then: Chắc chắn trạng thái khởi tạo mặc định phải là OPEN (chưa chạy)
        assertNotNull(auction);
        assertNotNull(auction.getItem());
        assertEquals(Status.OPEN, auction.getAuctionStatus(), "Phiên đấu giá mới tạo phải luôn ở trạng thái OPEN");
    }

    // =========================================================================
    // 3. TEST LOGIC LỊCH SỬ ĐẤU GIÁ (toBidHistoryDTO)
    // =========================================================================

    @Test
    @DisplayName("toBidHistoryDTO: Lấy chính xác tên người dùng (username) từ lịch sử đấu giá")
    void toBidHistoryDTO_ValidBidder_ShouldMapUsername() {
        // Given
        Bidder bidder = new Bidder("tam_bidder", "pass", "tam@uet");
        Auction auction = new Auction(new Art("Art", "Name", "start", "end", 100, 10, "Author"));
        BidTransaction bid = new BidTransaction(bidder, auction, 150.0);

        // When
        BidHistoryDTO dto = AuctionMapper.toBidHistoryDTO(bid);

        // Then
        assertNotNull(dto);
        assertEquals("tam_bidder", dto.getBidderUsername());
        assertEquals(150.0, dto.getBidAmount());
    }

    @Test
    @DisplayName("toBidHistoryDTO: Gán tên 'Unknown' nếu mất thông tin người dùng (tránh NPE)")
    void toBidHistoryDTO_NullBidder_ShouldMapToUnknown() {
        // Given: Giao dịch bị lỗi mất thông tin người đặt giá
        Auction auction = new Auction(new Art("Art", "Name", "start", "end", 100, 10, "Author"));
        BidTransaction bid = new BidTransaction(null, auction, 200.0); // Bidder bị null

        // When
        BidHistoryDTO dto = AuctionMapper.toBidHistoryDTO(bid);

        // Then: Ứng dụng không được sập, mà phải gán giá trị mặc định là "Unknown"
        assertNotNull(dto);
        assertEquals("Unknown", dto.getBidderUsername(), "Nếu mất thông tin User, hệ thống phải tự map thành 'Unknown'");
    }

    // =========================================================================
    // 4. TEST HÀM CHUYỂN ĐỔI LIST
    // =========================================================================

    @Test
    @DisplayName("toListDTOs: Chuyển đổi danh sách và tự động lọc bỏ các phần tử null")
    void toListDTOs_ValidList_ShouldReturnMappedList() {
        Auction auction1 = new Auction(new Electronics("Phone", "Name", "start", "end", 100, 10, 12));
        Auction auction2 = new Auction(new Art("Art", "Name", "start", "end", 500, 50, "Author"));

        List<AuctionListDTO> dtoList = AuctionMapper.toListDTOs(Arrays.asList(auction1, auction2, null));

        // Phải lọc bỏ được phần tử null và trả về 2 phần tử hợp lệ
        assertEquals(2, dtoList.size());
    }
}