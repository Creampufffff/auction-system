package com.auction.app.controller;

import com.app.common.dto.*;
import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.mapper.AuctionMapper;
import com.app.common.mapper.BidMapper;
import com.auction.app.service.AuctionService;
import com.auction.app.service.BidService;
import com.auction.app.service.UserService;

import java.util.List;


public class BidController {
    private final BidService bidService;
    private final AuctionService auctionService;
    private final UserService userService;

    public BidController(BidService bidService, AuctionService auctionService, UserService userService) {
        this.bidService = bidService;
        this.auctionService = auctionService;
        this.userService = userService;
    }

    // ✅ Dùng DTO Request/Response
    public PlaceBidResponseDTO placeBid(PlaceBidRequestDTO request) {
        try {
            Bidder bidder = (Bidder) userService.getById(request.getBidderId());
            Auction auction = auctionService.getAuctionById(request.getAuctionId());

            if (bidder == null || auction == null) {
                return new PlaceBidResponseDTO(false, "Người dùng hoặc phiên đấu giá không tồn tại", null, null, 0);
            }

            if (request.getBidAmount() <= 0) {
                return new PlaceBidResponseDTO(false, "Giá đặt phải lớn hơn 0", null, null, 0);
            }

            BidTransaction bid = BidMapper.toEntity(request, bidder, auction);
            bidService.placeBid(bid);

            return new PlaceBidResponseDTO(
                true,
                "Đặt giá thành công",
                bid.getId(),
                auction.getId(),
                request.getBidAmount()
            );
        } catch (Exception e) {
            return new PlaceBidResponseDTO(false, "Lỗi đặt giá: " + e.getMessage(), null, null, 0);
        }
    }

    // ✅ Trả về DTO thay vì Entity
    public BidHistoryDTO getBid(String bidId) {
        BidTransaction bid = bidService.getBidById(bidId);
        return AuctionMapper.toBidHistoryDTO(bid);
    }

    // ✅ Trả về List DTO thay vì List Entity
    public List<BidHistoryDTO> getAllBids() {
        List<BidTransaction> bids = bidService.getAllBids();
        return AuctionMapper.toBidHistoryDTOs(bids);
    }

    // ✅ Trả về List DTO thay vì List Entity
    public List<BidHistoryDTO> getAuctionBidHistory(String auctionId) {
        List<BidTransaction> bids = bidService.getBidByAuctionId(auctionId);
        return AuctionMapper.toBidHistoryDTOs(bids);
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO deleteBid(String bidId) {
        try {
            bidService.deleteBid(bidId);
            return new ApiResponseDTO(true, "Xóa đặt giá thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi xóa đặt giá: " + e.getMessage());
        }
    }
}


