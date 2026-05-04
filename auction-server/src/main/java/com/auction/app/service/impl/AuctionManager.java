package src.main.java.com.auction.app.service.impl;

public class AuctionManager {
    private static AuctionManager instance;



    private AuctionManager(){
        // Khoi tao quan ly Auction

    }

    public static AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }
}
