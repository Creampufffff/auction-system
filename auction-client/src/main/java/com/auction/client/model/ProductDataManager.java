package com.auction.client.model;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ProductDataManager {
    private static ProductDataManager instance;
    private final ObservableList<Product> myProductList;
    private final ObservableList<AuctionListDTO> serverAuctionList;
    private AuctionListDTO selectedAuction;

    private final Map<String, ObservableList<String>> historyMap;
    private final Map<String, Double> currentPriceMap;
    private final Map<String, Integer> timeLeftMap;

    // --- THÊM: Global Timer để chạy background ---
    private Timer globalTimer;

    private ProductDataManager() {
        myProductList = FXCollections.observableArrayList();
        serverAuctionList = FXCollections.observableArrayList();
        historyMap = new HashMap<>();
        currentPriceMap = new HashMap<>();
        timeLeftMap = new HashMap<>();
        startGlobalCountdown(); // Chạy ngay khi app khởi động
    }

    public static ProductDataManager getInstance() {
        if (instance == null) {
            instance = new ProductDataManager();
        }
        return instance;
    }

    // Logic này sẽ chạy xuyên suốt vòng đời của App
    private void startGlobalCountdown() {
        globalTimer = new Timer(true);
        globalTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Duyệt qua tất cả các sản phẩm đang có thời gian trong Map
                for (String auctionId : timeLeftMap.keySet()) {
                    int currentTime = timeLeftMap.get(auctionId);
                    if (currentTime > 0) {
                        timeLeftMap.put(auctionId, currentTime - 1);
                    }
                }
            }
        }, 0, 1000);
    }

    public ObservableList<String> getHistoryForProduct(String auctionId) {
        if (!historyMap.containsKey(auctionId)) {
            historyMap.put(auctionId, FXCollections.observableArrayList());
        }
        return historyMap.get(auctionId);
    }

    public double getCurrentPrice(String auctionId, double defaultPrice) {
        return currentPriceMap.getOrDefault(auctionId, defaultPrice);
    }

    public void setCurrentPrice(String auctionId, double price) {
        currentPriceMap.put(auctionId, price);
    }

    public int getTimeLeft(String auctionId, int defaultTime) {
        // Nếu chưa có trong map (lần đầu mở), nạp giá trị mặc định vào để Timer bắt đầu trừ
        if (!timeLeftMap.containsKey(auctionId)) {
            timeLeftMap.put(auctionId, defaultTime);
        }
        return timeLeftMap.get(auctionId);
    }

    public void setTimeLeft(String auctionId, int time) {
        timeLeftMap.put(auctionId, time);
    }

    public ObservableList<Product> getProductList() { return myProductList; }
    public ObservableList<AuctionListDTO> getServerAuctionList() { return serverAuctionList; }

    public void pushToGlobalAuction(Product p) {
        AuctionListDTO dto = new AuctionListDTO(
                p.getId(), "ITEM-" + p.getId(), p.getName(), p.getPrice(),
                Status.OPEN, p.getCondition(), p.getDescription(), p.getWarranty()
        );
        if (serverAuctionList.stream().noneMatch(a -> a.getAuctionId().equals(p.getId()))) {
            serverAuctionList.add(dto);
        }
    }

    public AuctionListDTO getSelectedAuction() { return selectedAuction; }
    public void setSelectedAuction(AuctionListDTO auction) { this.selectedAuction = auction; }
}