package src.main.java.com.auction.app.repository;

import src.main.java.com.app.common.entity.BidTransaction;

import java.util.List;

public interface BidDAO extends BaseDAO<BidTransaction> {
    // Lấy toàn bộ lịch sử đặt giá của một phiên để vẽ biểu đồ
    List<BidTransaction> findByAuctionId(String auctionId);

    // Lấy giá cao nhất mà một người dùng đã đặt trong một phiên
    double getMaxBidByBidder(String auctionId, String bidderId);

    // Lấy giá cao nhất của phiên đấu giá
    BidTransaction getMaxBidByAuctionId(String auctionId);
}