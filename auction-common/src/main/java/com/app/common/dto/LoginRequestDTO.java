package src.main.java.com.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Tự động sinh ra tất cả Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Tự động sinh ra Constructor rỗng
@AllArgsConstructor // Tự động sinh ra Constructor có đủ tất cả tham số
public class LoginRequestDTO {
    private String username;
    private String password;

}
