package java.com.auction.app.repository.impl;

import com.app.common.entity.Bidder;
import com.app.common.entity.User;
import com.auction.app.config.DatabaseConfig;
import com.auction.app.repository.impl.UserDAOImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOImplTest {

    private UserDAOImpl userDAO;
    private Connection h2Connection;
    private MockedStatic<DatabaseConfig> mockedDatabaseConfig;

    @BeforeEach
    void setUp() throws Exception {
        // Khởi tạo Database H2 chạy hoàn toàn trên RAM độc lập
        h2Connection = DriverManager.getConnection("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");

        try (Statement stmt = h2Connection.createStatement()) {
            // Tạo bảng ảo nếu chưa tồn tại nhằm tránh lỗi trùng lặp khi chạy nhiều test case
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "username VARCHAR(255) UNIQUE, " +
                    "password VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "role VARCHAR(50), " +
                    "balance DOUBLE DEFAULT 0.0)");

            // Làm sạch dữ liệu rác sau mỗi lượt test để đảm bảo tính độc lập tuyệt đối
            stmt.execute("TRUNCATE TABLE users");
        }

        // Tạo Mock tĩnh cho DatabaseConfig. Lúc này do áp dụng Lazy Load nên khối kết nối MySQL thật sẽ bị bỏ qua
        mockedDatabaseConfig = Mockito.mockStatic(DatabaseConfig.class);
        mockedDatabaseConfig.when(DatabaseConfig::getConnection).thenReturn(h2Connection);

        userDAO = new UserDAOImpl();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockedDatabaseConfig != null) {
            mockedDatabaseConfig.close();
        }
        if (h2Connection != null && !h2Connection.isClosed()) {
            h2Connection.close();
        }
    }

    @Test
    void testSaveUser_Success() throws Exception {
        User newUser = new Bidder("tamnguyen", "password123", "tam@uet.edu.vn");
        newUser.setId("user-1");

        boolean isSaved = userDAO.save(newUser);

        assertTrue(isSaved, "Hàm save phải trả về true khi lưu thành công");

        try (Statement stmt = h2Connection.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM users WHERE id = 'user-1'")) {
            assertTrue(rs.next());
            assertEquals("tamnguyen", rs.getString("username"));
            assertEquals("tam@uet.edu.vn", rs.getString("email"));
        }
    }

    @Test
    void testFindByUsername_Found() throws Exception {
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("INSERT INTO users (id, username, password, email, role) " +
                    "VALUES ('user-2', 'admin_tam', 'hashed_pass', 'admin@test.com', 'ADMIN')");
        }

        User foundUser = userDAO.findByUsername("admin_tam");

        assertNotNull(foundUser, "Phải tìm thấy thực thể người dùng khi đúng username");
        assertEquals("user-2", foundUser.getId());
        assertEquals("admin@test.com", foundUser.getEmail());
    }

    @Test
    void testFindByUsername_NotFound() {
        User foundUser = userDAO.findByUsername("ghost_user");
        assertNull(foundUser, "Phải trả về null nếu không tồn tại username trong hệ thống");
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("INSERT INTO users (id, username) VALUES ('user-3', 'delete_me')");
        }

        boolean isDeleted = userDAO.delete("user-3");

        assertTrue(isDeleted);

        try (Statement stmt = h2Connection.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users WHERE id = 'user-3'")) {
            rs.next();
            assertEquals(0, rs.getInt("total"), "Dữ liệu phải bị xóa sạch khỏi bảng");
        }
    }
}