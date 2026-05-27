package com.auction.client.model;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import com.auction.client.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

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

    // --- [MỚI] LOGIC PHÂN TRANG & TÌM KIẾM ---
    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 1;
    private String searchKeyword = "";
    private List<AuctionListDTO> filteredAuctionList = List.of();

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

    public void syncBalanceFromSession() {
        if (SessionManager.isLoggedIn()) {
            userBalance = SessionManager.getCurrentUserBalance();
        }
    }

    public void resetSessionState() {
        userBalance = 5000.0;
        userHeldMoneyMap.clear();
        leadingUserMap.clear();
        dialogShownMap.clear();
        currentPriceMap.clear();
        timeLeftMap.clear();
        selectedAuction = null;
        currentPage = 1;
        searchKeyword = "";
        filteredAuctionList = List.of();
    }

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
        return serverAuctionList.stream()
                .filter(a -> a.getAuctionId().equals(auctionId))
                .findFirst()
                .map(a -> isEndDateReached(a.getEndDateTime()))
                .orElse(false);
    }

    public void closeAuction(String auctionId) {
        serverAuctionList.stream()
                .filter(a -> a.getAuctionId().equals(auctionId))
                .findFirst()
                .ifPresent(a -> {
                    a.setAuctionStatus(Status.FINISHED);
                });
    }

    public void updateAuctionEndDate(String auctionId, String endDateTime) {
        serverAuctionList.stream()
                .filter(a -> a.getAuctionId().equals(auctionId))
                .findFirst()
                .ifPresent(a -> a.setEndDateTime(endDateTime));
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

    private boolean isEndDateReached(String endDateTime) {
        LocalDateTime endTime = parseEndDateTime(endDateTime);
        return endTime != null && !LocalDateTime.now().isBefore(endTime);
    }

    private LocalDateTime parseEndDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("|")) {
            value = value.split("\\|", 2)[0];
        }

        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    // --- [MỚI] PHƯƠNG THỨC TÌM KIẾM VÀ PHÂN TRANG ---
    public void setSearchKeyword(String keyword) {
        this.searchKeyword = keyword.toLowerCase().trim();
        this.currentPage = 1;
        applyFilter();
    }

    private void applyFilter() {
        if (searchKeyword.isEmpty()) {
            filteredAuctionList = new java.util.ArrayList<>(serverAuctionList);
        } else {
            filteredAuctionList = serverAuctionList.stream()
                    .filter(a -> a.getName().toLowerCase().contains(searchKeyword) ||
                               a.getItemType().toLowerCase().contains(searchKeyword) ||
                               a.getAuctionId().toLowerCase().contains(searchKeyword))
                    .collect(Collectors.toList());
        }
    }

    public ObservableList<AuctionListDTO> getPagedAuctions() {
        applyFilter();
        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredAuctionList.size());

        if (start >= filteredAuctionList.size()) {
            return FXCollections.observableArrayList();
        }

        return FXCollections.observableArrayList(filteredAuctionList.subList(start, end));
    }

    public void nextPage() {
        applyFilter();
        int totalPages = getTotalPages();
        if (currentPage < totalPages) {
            currentPage++;
        }
    }

    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
        }
    }

    public void goToPage(int pageNumber) {
        applyFilter();
        int totalPages = getTotalPages();
        if (pageNumber >= 1 && pageNumber <= totalPages) {
            currentPage = pageNumber;
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        applyFilter();
        return (int) Math.ceil((double) filteredAuctionList.size() / ITEMS_PER_PAGE);
    }

    public int getTotalFilteredItems() {
        applyFilter();
        return filteredAuctionList.size();
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }
}
