package com.auction.app.service.impl;

import com.app.common.entity.Auction;
import com.app.common.entity.AutoBid;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.exception.InvalidBidException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.service.AutoBidService;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;

public class AutoBidServiceImpl implements AutoBidService {
    private final List<AutoBid> autoBids = new ArrayList<>();  // Danh sách tất cả AutoBids
    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    
    // PriorityQueue sắp xếp AutoBids theo giá tối đa (cao nhất có ưu tiên)
    private final PriorityQueue<AutoBid> autoBidQueue;

    public AutoBidServiceImpl(AuctionDAO auctionDAO, BidDAO bidDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        
        // Tạo PriorityQueue sắp xếp giảm dần (maxAutoAmount cao nhất đầu tiên)
        this.autoBidQueue = new PriorityQueue<>(
                Comparator.comparingDouble(AutoBid::getMaxAutoAmount).reversed()
        );
    }

    @Override
    public void createAutoBid(AutoBid autoBid) {
        // ========== BƯỚC 1: Validate AutoBid ==========
        if (autoBid == null) {
            throw new InvalidBidException("AutoBid không thể null");
        }
        
        if (autoBid.getAuctionId() == null || autoBid.getAuctionId().isBlank()) {
            throw new InvalidBidException("ID phiên không được để trống");
        }
        
        if (autoBid.getBidderId() == null || autoBid.getBidderId().isBlank()) {
            throw new InvalidBidException("ID người dùng không được để trống");
        }
        
        if (autoBid.getMaxAutoAmount() <= 0) {
            throw new InvalidBidException("Giá tối đa phải lớn hơn 0");
        }

        // ========== BƯỚC 2: Kiểm tra phiên tồn tại ==========
        Auction auction = auctionDAO.findById(autoBid.getAuctionId());
        if (auction == null) {
            throw new InvalidBidException("Không tìm thấy phiên đấu giá");
        }

        // ========== BƯỚC 3: Lưu AutoBid ==========
        autoBids.add(autoBid);       // Lưu vào danh sách
        autoBidQueue.offer(autoBid); // Thêm vào PriorityQueue
    }

    @Override
    public AutoBid getAutoBidById(String autoBidId) {
        return autoBids.stream()
                .filter(ab -> ab.getId().equals(autoBidId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<AutoBid> getAutoBiddsByAuctionId(String auctionId) {
        List<AutoBid> result = new ArrayList<>();
        
        // Lấy tất cả AutoBid hoạt động của phiên này
        for (AutoBid ab : autoBids) {
            if (ab.getAuctionId().equals(auctionId) && ab.isActive()) {
                result.add(ab);
            }
        }
        
        // Sắp xếp theo giá tối đa (cao nhất đầu tiên)
        result.sort(Comparator.comparingDouble(AutoBid::getMaxAutoAmount).reversed());
        return result;
    }

    @Override
    public List<AutoBid> getActiveBidsByBidderId(String bidderId) {
        List<AutoBid> result = new ArrayList<>();
        
        // Lấy tất cả AutoBid hoạt động của người dùng này
        for (AutoBid ab : autoBids) {
            if (ab.getBidderId().equals(bidderId) && ab.isActive()) {
                result.add(ab);
            }
        }
        
        return result;
    }

    @Override
    public void cancelAutoBid(String autoBidId) {
        AutoBid autoBid = getAutoBidById(autoBidId);
        if (autoBid != null) {
            autoBid.setActive(false);  // Đánh dấu là không hoạt động
            autoBidQueue.remove(autoBid);  // Xóa khỏi queue
        }
    }

    @Override
    public void processAutoBidsForAuction(String auctionId) {
        // Lấy tất cả AutoBid hoạt động của phiên
        List<AutoBid> activeAutoBids = getAutoBiddsByAuctionId(auctionId);

        if (activeAutoBids.isEmpty()) {
            return;  // Không có AutoBid nào
        }

        // Lấy thông tin phiên và vật phẩm
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null || auction.getItem() == null) {
            return;
        }

        // ========== BƯỚC 1: Lấy bid hiện tại ==========
        BidTransaction currentHighest = bidDAO.getMaxBidByAuctionId(auctionId);
        double currentPrice = currentHighest != null
                ? currentHighest.getBidAmount()
                : auction.getItem().getStartPrice();  // Nếu chưa có bid, lấy giá khởi điểm

        double minIncrement = auction.getItem().getMinIncreasement();

        // ========== BƯỚC 2: Xử lý AutoBids theo thứ tự ưu tiên ==========
        // Duyệt qua AutoBids từ cao đến thấp (PriorityQueue)
        for (AutoBid autoBid : activeAutoBids) {
            if (!autoBid.isActive()) {
                continue;  // Bỏ qua nếu đã bị hủy
            }

            // Giá bid tiếp theo = giá hiện tại + mức tăng tối thiểu
            double nextBidAmount = currentPrice + minIncrement;

            // ========== BƯỚC 3: Kiểm tra AutoBid có đủ tiền không ==========
            if (autoBid.getMaxAutoAmount() >= nextBidAmount) {
                try {
                    // Tạo đối tượng Bidder tạm để thực hiện bid
                    Bidder bidder = new Bidder("auto", "auto", "auto@system.com");
                    bidder.setId(autoBid.getBidderId());

                    // Tạo BidTransaction
                    BidTransaction autoBidTransaction = new BidTransaction(bidder, auction, nextBidAmount);
                    
                    // Lưu bid vào database (với transaction và lock)
                    bidDAO.placeBidSafely(autoBidTransaction);

                    // Cập nhật giá hiện tại cho AutoBid tiếp theo
                    currentPrice = nextBidAmount;
                } catch (Exception e) {
                    System.err.println("❌ AutoBid thất bại cho ID: " + autoBid.getId() + " - " + e.getMessage());
                    autoBid.setActive(false);  // Đánh dấu AutoBid này là failed
                }
            } else {
                // AutoBid này không đủ tiền để bid tiếp => hủy lệnh
                autoBid.setActive(false);
            }
        }
    }
}

