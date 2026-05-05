package com.auction.app.repository;

import com.app.common.entity.BidTransaction;

import java.util.List;

public interface BidDAO extends BaseDAO<BidTransaction> {
    // Lấy toàn bộ lịch sử đặt giá của một phiên để vẽ biểu đồ
    List<BidTransaction> findByAuctionId(String auctionId);

    // Lấy giá cao nhất mà một người dùng đã đặt trong một phiên
    double getMaxBidByBidder(String auctionId, String bidderId);

    // Lấy giá cao nhất của phiên đấu giá
    BidTransaction getMaxBidByAuctionId(String auctionId);

    // Đặt giá trong một transaction để tránh lost update khi nhiều client bid cùng lúc
    boolean placeBidSafely(BidTransaction bid);
}
