package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionMapper {

    public static AuctionListDTO toListDTO(Auction auction) {
        if (auction == null) return null;
        
        Item item = auction.getItem();
        if (item == null) return null;
        
        double currentPrice = item.getHighestCurrentPrice() > 0
            ? item.getHighestCurrentPrice()
            : item.getStartPrice();
        String condition = item instanceof Art
            ? ((Art) item).getAuthor()
            : null;
        String warranty = item instanceof Electronics
            ? String.valueOf(((Electronics) item).getWarrantyMonths())
            : item instanceof Vehicle ? ((Vehicle) item).getBrand() : null;
        String itemType = item instanceof Electronics
            ? "ELECTRONICS"
            : item instanceof Vehicle ? "VEHICLE" : "ART";

        return new AuctionListDTO(
            auction.getId(),
            item.getId(),
            itemType,
            item.getName(),
            currentPrice,
            auction.getAuctionStatus(),
            condition,
            item.getDescription(),
            warranty,
            item.getStartDateString(),
            item.getEndDateString(),
            item.getImageBlob()
        );
    }

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

    public static Auction toEntity(CreateAuctionRequestDTO dto) {
        if (dto == null) return null;
        
        Item item = ItemMapper.createItem(dto);
        if (item == null) return null;
        
        Auction auction = new Auction(item);
        auction.setAuctionStatus(com.app.common.enums.Status.OPEN);
        
        return auction;
    }

    public static BidHistoryDTO toBidHistoryDTO(BidTransaction bid) {
        if (bid == null) return null;
        
        Bidder bidder = bid.getBidder();
        String bidderUsername = bidder != null ? bidder.getUsername() : "Unknown";
        Auction auction = bid.getAuction();
        Item item = auction == null ? null : auction.getItem();
        String itemType = resolveItemType(item);
        String itemName = item == null ? "" : item.getName();
        
        return new BidHistoryDTO(
            bid.getId(),
            auction == null ? "" : auction.getId(),
            itemType,
            itemName,
            bidderUsername,
            bid.getBidAmount(),
            bid.getCreatedAt() != null ? bid.getCreatedAt() : ""
        );
    }

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

    private static String resolveItemType(Item item) {
        if (item instanceof Electronics) {
            return "ELECTRONICS";
        }
        if (item instanceof Vehicle) {
            return "VEHICLE";
        }
        if (item instanceof Art) {
            return "ART";
        }
        return "";
    }
}

