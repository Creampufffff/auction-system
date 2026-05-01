package src.main.java.com.app.common.entity;

import java.util.ArrayList;
import java.util.List;

public class Auction extends BaseEntity{
    private Item item;
    private List<BidTransaction> bidHistory;
    private boolean isActive;

    public Auction(Item item) {

        this.item = item;
        this.bidHistory = new ArrayList<>();
        this.isActive = true;
    }
}


