package com.app.common.entity;

import lombok.Getter;
import lombok.Setter;
import com.app.common.enums.Status;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Auction extends BaseEntity{
    private Item item;
    private List<BidTransaction> bidHistory;
    private Status auctionStatus;



    public Auction(Item item) {
        this.item = item;
        this.bidHistory = new ArrayList<>();
        this.auctionStatus = Status.OPEN;
    }

}


