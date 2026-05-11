package com.auction.app.controller;

import com.app.common.dto.*;
import com.app.common.entity.Auction;
import com.app.common.entity.AutoBid;
import com.app.common.mapper.AutoBidMapper;
import com.auction.app.service.AuctionService;
import com.auction.app.service.AutoBidService;

import java.util.ArrayList;
import java.util.List;

public class AutoBidController {
    private final AutoBidService autoBidService;
    private final AuctionService auctionService;

    public AutoBidController(AutoBidService autoBidService, AuctionService auctionService) {
        this.autoBidService = autoBidService;
        this.auctionService = auctionService;
    }

    // ✅ Dùng DTO Request/Response
    public ApiResponseDTO setAutoBid(SetAutoBidRequestDTO request) {
        try {
            if (request.getMaxAutoAmount() <= 0) {
                return new ApiResponseDTO(false, "Số tiền auto-bid phải lớn hơn 0");
            }

            AutoBid autoBid = AutoBidMapper.toEntity(request);
            autoBidService.createAutoBid(autoBid);

            return new ApiResponseDTO(true, "Thiết lập auto-bid thành công. ID: " + autoBid.getId());
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi thiết lập auto-bid: " + e.getMessage());
        }
    }

    // ✅ Trả về DTO thay vì Entity
    public AutoBidDTO getAutoBid(String autoBidId) {
        AutoBid autoBid = autoBidService.getAutoBidById(autoBidId);
        if (autoBid == null) return null;

        Auction auction = auctionService.getAuctionById(autoBid.getAuctionId());
        String itemName = auction != null && auction.getItem() != null
            ? auction.getItem().getName()
            : "Unknown";

        return AutoBidMapper.toDTO(autoBid, itemName);
    }

    // ✅ Trả về List DTO thay vì List Entity
    public List<AutoBidDTO> getAuctionAutoBids(String auctionId) {
        List<AutoBid> autoBids = autoBidService.getAutoBiddsByAuctionId(auctionId);
        if (autoBids == null) return new ArrayList<>();

        Auction auction = auctionService.getAuctionById(auctionId);
        String itemName = auction != null && auction.getItem() != null
            ? auction.getItem().getName()
            : "Unknown";

        List<AutoBidDTO> dtos = new ArrayList<>();
        for (AutoBid autoBid : autoBids) {
            dtos.add(AutoBidMapper.toDTO(autoBid, itemName));
        }
        return dtos;
    }

    // ✅ Trả về List DTO thay vì List Entity
    public List<AutoBidDTO> getBidderAutoBids(String bidderId) {
        List<AutoBid> autoBids = autoBidService.getActiveBidsByBidderId(bidderId);
        if (autoBids == null) return new ArrayList<>();

        List<AutoBidDTO> dtos = new ArrayList<>();
        for (AutoBid autoBid : autoBids) {
            Auction auction = auctionService.getAuctionById(autoBid.getAuctionId());
            String itemName = auction != null && auction.getItem() != null
                ? auction.getItem().getName()
                : "Unknown";
            dtos.add(AutoBidMapper.toDTO(autoBid, itemName));
        }
        return dtos;
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO cancelAutoBid(String autoBidId) {
        try {
            autoBidService.cancelAutoBid(autoBidId);
            return new ApiResponseDTO(true, "Hủy auto-bid thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi hủy auto-bid: " + e.getMessage());
        }
    }

    // ✅ Dùng DTO Response
    public ApiResponseDTO processAutoBids(String auctionId) {
        try {
            autoBidService.processAutoBidsForAuction(auctionId);
            return new ApiResponseDTO(true, "Xử lý auto-bid thành công");
        } catch (Exception e) {
            return new ApiResponseDTO(false, "Lỗi xử lý auto-bid: " + e.getMessage());
        }
    }
}


