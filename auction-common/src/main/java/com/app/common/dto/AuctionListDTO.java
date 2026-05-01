package src.main.java.com.app.common.dto;
import src.main.java.com.app.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Tự động sinh ra tất cả Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Tự động sinh ra Constructor rỗng
@AllArgsConstructor // Tự động sinh ra Constructor có đủ tất cả tham số

public class AuctionListDTO {
    private String auctionId;
    private String itemName;
    private double currentPrice;
    private Status AuctionStatus;
}
