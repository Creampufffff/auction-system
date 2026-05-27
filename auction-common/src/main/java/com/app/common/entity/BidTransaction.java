package com.app.common.entity;

public class BidTransaction extends BaseEntity {
    private Bidder bidder;
    private Auction auction;
    private double bidAmount;
    private String createdAt;

    public BidTransaction() {
        super();
    }

    public BidTransaction(Bidder bidder, Auction auction, double bidAmount) {
        super();
        this.bidder = bidder;
        this.auction = auction;
        this.bidAmount = bidAmount;
    }

    public Bidder getBidder() { return bidder; }
    public Auction getAuction() { return auction; }
    public double getBidAmount() { return bidAmount; }
    public String getCreatedAt() { return createdAt; }

    public void setBidder(Bidder bidder) { this.bidder = bidder; }
    public void setAuction(Auction auction) { this.auction = auction; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
