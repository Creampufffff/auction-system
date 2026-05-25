package com.auction.client.model;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
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
    private final Map<String, Boolean> dialogShownMap = new HashMap<>();

    // --- [MỚI] LOGIC VÍ TIỀN & SESSION ---
    private double userBalance = 5000.0;
    private final Map<String, Double> userHeldMoneyMap = new HashMap<>();
    private final Map<String, String> leadingUserMap = new HashMap<>();

    private Timer globalTimer;

    private ProductDataManager() {
        myProductList = FXCollections.observableArrayList();
        serverAuctionList = FXCollections.observableArrayList();
        historyMap = new HashMap<>();
        currentPriceMap = new HashMap<>();
        timeLeftMap = new HashMap<>();
        startGlobalCountdown();
    }

    public static ProductDataManager getInstance() {
        if (instance == null) {
            instance = new ProductDataManager();
        }
        return instance;
    }

    // --- [MỚI] HÀM TRỢ GIÚP VÍ TIỀN ---
    public double getUserBalance() { return userBalance; }
    public void setUserBalance(double balance) { this.userBalance = balance; }
    public void deductBalance(double amount) { this.userBalance -= amount; }
    public void refundBalance(double amount) { this.userBalance += amount; }
    public double getHeldMoney(String auctionId) { return userHeldMoneyMap.getOrDefault(auctionId, 0.0); }
    public void setHeldMoney(String auctionId, double amount) { userHeldMoneyMap.put(auctionId, amount); }

    private void startGlobalCountdown() {
        globalTimer = new Timer(true);
        globalTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
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
        // TWEAK: Tìm và cập nhật giá trực tiếp vào danh sách trên bảng
        serverAuctionList.stream()
                .filter(a -> a.getAuctionId().equals(auctionId))
                .findFirst()
                .ifPresent(a -> a.setCurrentPrice(price));
    }
    public void removeAuction(String auctionId) {
        serverAuctionList.removeIf(a -> a.getAuctionId().equals(auctionId));
    }

    public int getTimeLeft(String auctionId, int defaultTime) {
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
                p.getId(), "ITEM-" + p.getId(), p.getType(), p.getName(), p.getPrice(),
                Status.OPEN, p.getCondition(), p.getDescription(), p.getWarranty()
        );
        if (serverAuctionList.stream().noneMatch(a -> a.getAuctionId().equals(p.getId()))) {
            serverAuctionList.add(dto);
        }
    }

    public boolean isWinnerDialogShown(String auctionId) {
        return dialogShownMap.getOrDefault(auctionId, false);
    }

    public void setWinnerDialogShown(String auctionId, boolean shown) {
        dialogShownMap.put(auctionId, shown);
    }

    public AuctionListDTO getSelectedAuction() { return selectedAuction; }
    public void setSelectedAuction(AuctionListDTO auction) { this.selectedAuction = auction; }

    public boolean isEnded(String auctionId) {
        return getTimeLeft(auctionId, 30) <= 0;
    }

    public void closeAuction(String auctionId) {
        serverAuctionList.stream()
                .filter(a -> a.getAuctionId().equals(auctionId))
                .findFirst()
                .ifPresent(a -> {
                    a.setAuctionStatus(Status.FINISHED);
                });
    }

    public String getLeadingUser(String auctionId, String defaultUser) {
        return leadingUserMap.getOrDefault(auctionId, defaultUser);
    }

    public void setLeadingUser(String auctionId, String userName) {
        leadingUserMap.put(auctionId, userName);
    }
    public void handleSomeoneElseLeading(String auctionId, double newPrice) {
        double myHeld = getHeldMoney(auctionId);
        if (myHeld > 0) {
            refundBalance(myHeld); // Trả lại tiền vào ví vì mình không còn dẫn đầu
            setHeldMoney(auctionId, 0.0); // Reset số dư đang giữ tại sàn này
        }
        setCurrentPrice(auctionId, newPrice);
    }
    public void deleteProductAndAuction(String auctionId) {
        myProductList.removeIf(p -> p.getId().equals(auctionId));
        serverAuctionList.removeIf(a -> a.getAuctionId().equals(auctionId));
        // Dọn dẹp luôn trạng thái dialog
        dialogShownMap.remove(auctionId);
    }
}
