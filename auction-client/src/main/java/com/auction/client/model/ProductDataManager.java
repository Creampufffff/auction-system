package com.auction.client.model;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart; // MỚI: Dành cho biểu đồ
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

    // --- [MỚI] QUẢN LÝ THỜI GIAN THỰC ĐỂ TRÁNH TRỤC X BỊ ÂM ---
    private final Map<String, Long> startTimeMap = new HashMap<>();

    // --- [MỚI] LOGIC VÍ TIỀN & SESSION ---
    private double userBalance = 5000.0;
    private final Map<String, Double> userHeldMoneyMap = new HashMap<>();
    private final Map<String, String> leadingUserMap = new HashMap<>();

    // --- [MỚI] LOGIC LIVE GRAPH ---
    private final Map<String, ObservableList<XYChart.Data<Number, Number>>> priceHistoryGraphMap = new HashMap<>();

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

        // --- [MỚI] CẬP NHẬT DỮ LIỆU BIỂU ĐỒ ---
        updatePriceGraphData(auctionId, price);

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
            // KHI KHỞI TẠO AUCTION LẦN ĐẦU: Lưu lại mốc thời gian hệ thống (giây)
            if (!startTimeMap.containsKey(auctionId)) {
                startTimeMap.put(auctionId, System.currentTimeMillis() / 1000);
            }
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
            refundBalance(myHeld);
            setHeldMoney(auctionId, 0.0);
        }
        setCurrentPrice(auctionId, newPrice);
    }
    public void deleteProductAndAuction(String auctionId) {
        myProductList.removeIf(p -> p.getId().equals(auctionId));
        serverAuctionList.removeIf(a -> a.getAuctionId().equals(auctionId));
        dialogShownMap.remove(auctionId);
        priceHistoryGraphMap.remove(auctionId);
        startTimeMap.remove(auctionId); // Dọn dẹp thời gian bắt đầu
    }

    // --- [MỚI] HÀM QUẢN LÝ BIỂU ĐỒ VỚI LOGIC THỜI GIAN TRÔI QUA (ELAPSED) ---

    private void updatePriceGraphData(String auctionId, double price) {
        ObservableList<XYChart.Data<Number, Number>> dataPoints = getPriceGraphData(auctionId);

        // LOGIC MỚI: Tính thời gian trôi qua thực tế dựa trên System Clock
        long now = System.currentTimeMillis() / 1000;
        long start = startTimeMap.getOrDefault(auctionId, now);
        int timeElapsed = (int) (now - start);

        // Cập nhật trên JavaFX Application Thread
        Platform.runLater(() -> {
            dataPoints.add(new XYChart.Data<>(timeElapsed, price));

            // Giới hạn 20 điểm dữ liệu cho mượt
            if (dataPoints.size() > 20) {
                dataPoints.remove(0);
            }
        });
    }

    public ObservableList<XYChart.Data<Number, Number>> getPriceGraphData(String auctionId) {
        if (!priceHistoryGraphMap.containsKey(auctionId)) {
            priceHistoryGraphMap.put(auctionId, FXCollections.observableArrayList());
            // Đảm bảo có mốc 0:00 trên biểu đồ ngay khi khởi tạo
            if (!startTimeMap.containsKey(auctionId)) {
                startTimeMap.put(auctionId, System.currentTimeMillis() / 1000);
            }
        }
        return priceHistoryGraphMap.get(auctionId);
    }
}