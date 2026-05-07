package com.auction.app.service.impl;

import com.app.common.entity.BidTransaction;
import com.app.common.entity.User;
import com.app.common.exception.InsufficientBalanceException;
import com.app.common.exception.InvalidBidException;
import com.auction.app.repository.AuctionDAO;
import com.auction.app.repository.BidDAO;
import com.auction.app.repository.UserDAO;
import com.auction.app.repository.impl.UserDAOImpl;
import com.auction.app.service.BidService;

import java.util.List;

public class BidServiceImpl implements BidService {
    private final BidDAO bidDAO;        // DAO để truy cập database
    private final UserDAO userDAO;      // DAO để kiểm tra user

    public BidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO) {
        this(bidDAO, auctionDAO, new UserDAOImpl());
    }

    public BidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO, UserDAO userDAO) {
        this.bidDAO = bidDAO;
        this.userDAO = userDAO;
    }

    @Override
    public void placeBid(BidTransaction bid) {
        if (bid == null) {
            throw new InvalidBidException("Bid không thể null");
        }

        if (bid.getAuction() == null || bid.getAuction().getId() == null || bid.getAuction().getId().isBlank()) {
            throw new InvalidBidException("ID phiên đấu giá không được để trống");
        }

        if (bid.getBidder() == null || bid.getBidder().getId() == null || bid.getBidder().getId().isBlank()) {
            throw new InvalidBidException("ID người đặt giá không được để trống");
        }

        if (bid.getBidAmount() <= 0) {
            throw new InvalidBidException("Số tiền đấu giá phải lớn hơn 0");
        }

        User bidder = userDAO.findById(bid.getBidder().getId());
        if (bidder == null) {
            throw new InvalidBidException("Không tìm thấy người dùng");
        }

        if (bidder.getBalance() < bid.getBidAmount()) {
            throw new InsufficientBalanceException("Tài khoản không đủ tiền để đặt giá này");
        }

        if (!bidDAO.placeBidSafely(bid)) {
            throw new IllegalStateException("Không thể lưu bid vào database");
        }
    }

    @Override
    public BidTransaction getBidById(String bidId) {
        validateId(bidId, "ID bid");
        return bidDAO.findById(bidId);
    }

    @Override
    public List<BidTransaction> getAllBids() {
        return bidDAO.findAll();
    }

    @Override
    public List<BidTransaction> getBidByAuctionId(String auctionId) {
        validateId(auctionId, "ID phiên");
        // Trả về danh sách bid sắp xếp từ cao đến thấp
        return bidDAO.findByAuctionId(auctionId);
    }

    @Override
    public void deleteBid(String bidId) {
        validateId(bidId, "ID bid");
        if (!bidDAO.delete(bidId)) {
            throw new IllegalArgumentException("Không tìm thấy bid để xóa");
        }
    }

    //Kiểm tra ID không null và không rỗng
    private void validateId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
    }
}
