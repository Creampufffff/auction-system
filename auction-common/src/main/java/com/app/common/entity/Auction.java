package com.app.common.entity;

import com.app.common.enums.Status;

import java.util.ArrayList;
import java.util.List;

public class Auction extends BaseEntity {
    private Item item;
    private List<BidTransaction> bidHistory;
    private Status auctionStatus;

    public Auction(Item item) {
        this.item = item;
        this.bidHistory = new ArrayList<>();
        this.auctionStatus = Status.OPEN;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public void setBidHistory(List<BidTransaction> bidHistory) {
        this.bidHistory = bidHistory;
    }

    public Status getAuctionStatus() {
        return auctionStatus;
    }

    public void setAuctionStatus(Status auctionStatus) {
        this.auctionStatus = auctionStatus;
    }
}


