package com.auction.app.repository;

import com.app.common.entity.Auction;
import com.auction.app.repository.BaseDAO;

import java.util.List;

public interface AuctionDAO extends BaseDAO<Auction> {
    // Lấy danh sách phiên đang mở (Status = OPEN)
    List<Auction> findActiveAuctions();

    boolean updateCurrentPrice(String auctionId, double newPrice, String lastBidderId, int currentVersion);
}
