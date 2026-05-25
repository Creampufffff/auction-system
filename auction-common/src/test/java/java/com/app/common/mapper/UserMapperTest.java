package java.com.app.common.mapper;//test khả năng đọc chuỗi role từ DTO (như "SELLER", "ADMIN") để khởi tạo đúng class con (Subclass)
// tương ứng trong hệ thống phân cấp đối tượng User.

import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.LoginResponseDTO;
import com.app.common.dto.RegisterRequestDTO;
import com.app.common.entity.Admin;
import com.app.common.entity.Bidder;
import com.app.common.entity.Seller;
import com.app.common.entity.User;
import com.app.common.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserMapperTest {

    // Hàm phụ trợ tạo request đăng ký nhanh
    private RegisterRequestDTO createRegisterRequest(String role) {
        return new RegisterRequestDTO("tam_nguyen", "secure123", "tam@uet.vnu.edu.vn", role);
    }

    // =========================================================================
    // 1. TEST LOGIC ÉP KIỂU ĐA HÌNH KHI ĐĂNG KÝ (toEntity)
    // =========================================================================

    @Test
    void toEntity_SellerRole_ShouldReturnSellerInstance() {
        // Given: Request đăng ký với Role là chữ thường hoặc hoa đều phải bắt được
        RegisterRequestDTO dto = createRegisterRequest("seller");

        // When
        User user = UserMapper.toEntity(dto);

        // Then: Phải đúc ra đúng class Seller
        assertNotNull(user);
        assertInstanceOf(Seller.class, user, "Role 'seller' phải sinh ra class Seller");
        assertEquals("tam_nguyen", user.getUsername());
    }

    @Test
    void toEntity_AdminRole_ShouldReturnAdminInstance() {
        // Given
        RegisterRequestDTO dto = createRegisterRequest("ADMIN");

        // When
        User user = UserMapper.toEntity(dto);

        // Then
        assertNotNull(user);
        assertInstanceOf(Admin.class, user, "Role 'ADMIN' phải sinh ra class Admin");
    }

    @Test
    void toEntity_OtherRole_ShouldDefaultToBidder() {
        // Given: Nếu người dùng không truyền role hoặc truyền linh tinh
        RegisterRequestDTO dtoEmpty = createRegisterRequest("");
        RegisterRequestDTO dtoWeird = createRegisterRequest("HACKER");

        // When
        User user1 = UserMapper.toEntity(dtoEmpty);
        User user2 = UserMapper.toEntity(dtoWeird);

        // Then: Hệ thống phải tự động fallback về quyền thấp nhất là Bidder để đảm bảo an toàn
        assertInstanceOf(Bidder.class, user1, "Role rỗng phải fallback về Bidder");
        assertInstanceOf(Bidder.class, user2, "Role không hợp lệ phải fallback về Bidder");
    }

    @Test
    void toEntity_NullInput_ShouldReturnNull() {
        assertNull(UserMapper.toEntity(null), "Truyền null phải trả về null để tránh sập App");
    }

    // =========================================================================
    // 2. TEST LOGIC MAPPER TRẢ VỀ KHI ĐĂNG NHẬP (toLoginResponse)
    // =========================================================================

    @Test
    void toLoginResponse_ValidUser_ShouldReturnCorrectSimpleClassName() {
        // Given: Một user đang có 500$ trong ví
        Seller seller = new Seller("tam_seller", "pass", "email@uet");
        seller.setBalance(500.0);

        // When
        LoginResponseDTO dto = UserMapper.toLoginResponse(seller);

        // Then: Hàm phải lấy đúng tên class là "Seller" để gán vào trường role của DTO
        assertNotNull(dto);
        assertEquals("Seller", dto.getRole(), "Mapper phải lấy đúng tên class con (SimpleName) làm Role");
        assertEquals("tam_seller", dto.getUsername());
        assertEquals(500.0, dto.getBalance());
        assertEquals(seller.getId(), dto.getUserId(), "UUID sinh ra từ BaseEntity phải được giữ nguyên");
    }

    // =========================================================================
    // 3. TEST LOGIC MAPPER TRẢ VỀ SỐ DƯ (toBalanceResponse)
    // =========================================================================

    @Test
    void toBalanceResponse_ValidUser_ShouldReturnPopulatedDTO() {
        // Given
        Bidder bidder = new Bidder("tam_bidder", "pass", "email@uet");
        bidder.setBalance(1250.75);

        // When
        BalanceResponseDTO dto = UserMapper.toBalanceResponse(bidder);

        // Then
        assertNotNull(dto);
        assertEquals(bidder.getId(), dto.getUserId());
        assertEquals(1250.75, dto.getBalance());
        assertTrue(dto.getMessage().contains("tam_bidder"), "Message trả về phải chứa username của người dùng");
    }

    // =========================================================================
    // 4. TEST CÁC HÀM ÉP KIỂU CỤ THỂ (toSeller, toBidder)
    // =========================================================================

    @Test
    void toSeller_ValidDto_ShouldMapFieldsDirectly() {
        RegisterRequestDTO dto = createRegisterRequest("SELLER");
        Seller seller = UserMapper.toSeller(dto);

        assertNotNull(seller);
        assertEquals("tam_nguyen", seller.getUsername());
        assertEquals("tam@uet.vnu.edu.vn", seller.getEmail());
    }

    @Test
    void toBidder_ValidDto_ShouldMapFieldsDirectly() {
        RegisterRequestDTO dto = createRegisterRequest("BIDDER");
        Bidder bidder = UserMapper.toBidder(dto);

        assertNotNull(bidder);
        assertEquals("secure123", bidder.getPassword());
    }
}