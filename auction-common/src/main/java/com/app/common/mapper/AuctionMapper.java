package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AuctionMapper - Chuyển đổi giữa Auction Entity và các DTO liên quan
 */
public class AuctionMapper {

    /**
     * Chuyển Auction Entity thành AuctionListDTO
     */
    public static AuctionListDTO toListDTO(Auction auction) {
        if (auction == null) return null;
        
        Item item = auction.getItem();
        if (item == null) return null;
        
        return new AuctionListDTO(
            auction.getId(),
            item.getId(),
            item.getName(),
            item.getHighestCurrentPrice(),
            auction.getAuctionStatus(),
            item.getClass().getSimpleName(), // "Art", "Electronics", "Vehicle"
            item.getDescription(),
            String.valueOf(item.getId()) // Có thể thay bằng warranty info nếu cần
        );
    }

    /**
     * Chuyển một list Auction thành list AuctionListDTO
     */
    public static List<AuctionListDTO> toListDTOs(List<Auction> auctions) {
        List<AuctionListDTO> dtos = new ArrayList<>();
        if (auctions != null) {
            for (Auction auction : auctions) {
                AuctionListDTO dto = toListDTO(auction);
                if (dto != null) {
                    dtos.add(dto);
                }
            }
        }
        return dtos;
    }

    /**
     * Chuyển CreateAuctionRequestDTO thành Auction Entity
     * (Yêu cầu tạo Item theo itemType)
     */
    public static Auction toEntity(CreateAuctionRequestDTO dto) {
        if (dto == null) return null;
        
        Item item = ItemMapper.createItem(dto);
        if (item == null) return null;
        
        Auction auction = new Auction(item);
        auction.setAuctionStatus(com.app.common.enums.Status.OPEN);
        
        return auction;
    }

    /**
     * Chuyển BidTransaction Entity thành BidHistoryDTO
     */
    public static BidHistoryDTO toBidHistoryDTO(BidTransaction bid) {
        if (bid == null) return null;
        
        Bidder bidder = bid.getBidder();
        String bidderUsername = bidder != null ? bidder.getUsername() : "Unknown";
        
        return new BidHistoryDTO(
            bid.getId(),
            bid.getAuction().getId(),
            bidderUsername,
            bid.getBidAmount(),
            bid.getId() // Có thể thay bằng timestamp nếu có
        );
    }

    /**
     * Chuyển List BidTransaction thành List BidHistoryDTO
     */
    public static List<BidHistoryDTO> toBidHistoryDTOs(List<BidTransaction> bidTransactions) {
        List<BidHistoryDTO> dtos = new ArrayList<>();
        if (bidTransactions != null) {
            for (BidTransaction bid : bidTransactions) {
                BidHistoryDTO dto = toBidHistoryDTO(bid);
                if (dto != null) {
                    dtos.add(dto);
                }
            }
        }
        return dtos;
    }
}


