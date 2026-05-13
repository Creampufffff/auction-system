package com.auction.app.controller;

import com.app.common.dto.*;
import com.app.common.entity.Auction;
import com.app.common.mapper.AuctionMapper;
import com.auction.app.service.AuctionService;

import java.util.List;

public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public ApiResponseDTO createAuction(CreateAuctionRequestDTO request) {
        try {
            Auction auction = AuctionMapper.toEntity(request);
            if (auction == null) {
                return new ApiResponseDTO(false, "Loại sản phẩm không hợp lệ");
            }
            auctionService.saveAuction(auction);
            return new ApiResponseDTO(true, "Tạo phiên đấu giá thành công. ID: " + auction.getId());
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi tạo phiên đấu giá: " + e.getMessage());
        }
    }

    public ApiResponseDTO startAuction(String auctionId) {
        try {
            auctionService.startAuction(auctionId);
            return new ApiResponseDTO(true, "Bắt đầu phiên đấu giá thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi bắt đầu phiên đấu giá: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO endAuction(String auctionId) {
        try {
            auctionService.endAuction(auctionId);
            return new ApiResponseDTO(true, "Kết thúc phiên đấu giá thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi kết thúc phiên đấu giá: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response (không trả về toàn bộ Entity)
    public AuctionListDTO getAuction(String auctionId) {
        Auction auction = auctionService.getAuctionById(auctionId);
        return AuctionMapper.toListDTO(auction);
    }

    // ✅ Dùng DTO Response
    public List<AuctionListDTO> getAllAuctions() {
        List<Auction> auctions = auctionService.getAllAuction();
        return AuctionMapper.toListDTOs(auctions);
    }

    // ✅ Dùng DTO Response
    public List<AuctionListDTO> getActiveAuctions() {
        List<Auction> auctions = auctionService.getActiveAuctions();
        return AuctionMapper.toListDTOs(auctions);
    }
}


