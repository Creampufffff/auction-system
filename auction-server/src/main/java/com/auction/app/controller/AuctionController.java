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
                return new ApiResponseDTO(false, "Invalid item type");
            }
            auctionService.saveAuction(auction);
            return new ApiResponseDTO(true, "Auction created successfully. ID: " + auction.getId());
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error creating auction: " + e.getMessage());
        }
    }

    public ApiResponseDTO startAuction(String auctionId) {
        try {
            auctionService.startAuction(auctionId);
            return new ApiResponseDTO(true, "Auction started successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error starting auction: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO endAuction(String auctionId) {
        try {
            auctionService.endAuction(auctionId);
            return new ApiResponseDTO(true, "Auction ended successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error ending auction: " + e.getMessage());
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

    public List<AuctionListDTO> getAuctionsBySellerId(String sellerId) {
        List<Auction> auctions = auctionService.getAuctionsBySellerId(sellerId);
        return AuctionMapper.toListDTOs(auctions);
    }

    public List<AuctionListDTO> getWonAuctionsByBidderId(String bidderId) {
        List<Auction> auctions = auctionService.getWonAuctionsByBidderId(bidderId);
        return AuctionMapper.toListDTOs(auctions);
    }

    public ApiResponseDTO updateAuction(Auction auction) {
        try {
            auctionService.updateAuction(auction);
            return new ApiResponseDTO(true, "Auction updated successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error updating auction: " + e.getMessage());
        }
    }

    public ApiResponseDTO deleteAuction(String auctionId) {
        try {
            auctionService.deleteAuction(auctionId);
            return new ApiResponseDTO(true, "Auction deleted successfully");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Error deleting auction: " + e.getMessage());
        }
    }
}


