package com.app.common.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Tự động sinh ra tất cả Getter, Setter, toString, equals, hashCode
@NoArgsConstructor // Tự động sinh ra Constructor rỗng
@AllArgsConstructor // Tự động sinh ra Constructor có đủ tất cả tham số
public class AuctionUpdateDTO implements Serializable {
    private String auctionId;
    private double currentHighestBid;
    private String highestBidderName;
    private String message;
}
