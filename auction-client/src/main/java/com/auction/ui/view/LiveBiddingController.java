package com.auction.ui.view;

import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.BidHistoryDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.app.common.dto.AuctionListDTO;
import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import com.auction.application.service.AccountService;
import com.auction.application.service.AuctionService;
import com.auction.application.service.AutoBidService;
import com.auction.application.service.PeriodicUpdateService;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class LiveBiddingController {

    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timerLabel;
    @FXML private Label leadingBidderLabel;
    @FXML private Label accountBalanceLabel;
    @FXML private Label minIncrementLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button backButton;
    @FXML private Button autoBidButton;
    @FXML private VBox biddingControlsPanel;

    @FXML private Label priceChangeLabel;
    @FXML private StackPane chartContainer;
    @FXML private LineChart<String, Number> priceLineChart;
    @FXML private CategoryAxis xAxisLine;
    @FXML private NumberAxis yAxisLine;
    
    @FXML private TableView<BidHistoryModel> bidHistoryTable;
    @FXML private TableColumn<BidHistoryModel, String> timeCol;
    @FXML private TableColumn<BidHistoryModel, String> userCol;
    @FXML private TableColumn<BidHistoryModel, String> priceCol;
    @FXML private TableColumn<BidHistoryModel, String> changeCol;

    private Product currentProduct;
    private Timer timer;
    private long timeLeft;
    private double currentMinIncrement;
    private ObservableList<BidHistoryModel> bidHistoryModels = FXCollections.observableArrayList();
    private static LiveBiddingController activeController;
    private String lastRenderedTopBidKey;
    private boolean sellerMonitorMode;
    private String returnFxmlPath = "/fxml/AuctionList.fxml";
    private String returnTitle = "Auction System";

    public static class BidHistoryModel {
        private final StringProperty time;
        private final StringProperty user;
        private final StringProperty price;
        private final StringProperty change;
        private final BooleanProperty isLeader;
        private final DoubleProperty numericChange;

        public BidHistoryModel(String time, String user, String price, String change, boolean isLeader, double numericChange) {
            this.time = new SimpleStringProperty(time);
            this.user = new SimpleStringProperty(user);
            this.price = new SimpleStringProperty(price);
            this.change = new SimpleStringProperty(change);
            this.isLeader = new SimpleBooleanProperty(isLeader);
            this.numericChange = new SimpleDoubleProperty(numericChange);
        }

        public StringProperty timeProperty() { return time; }
        public StringProperty userProperty() { return user; }
        public StringProperty priceProperty() { return price; }
        public StringProperty changeProperty() { return change; }
        public boolean isLeader() { return isLeader.get(); }
        public double getNumericChange() { return numericChange.get(); }
    }
    private final AuctionService auctionService = new AuctionService();
    private final AccountService accountService = new AccountService();
    private final AutoBidService autoBidService = new AutoBidService();

    @FXML
    public void initialize() {
        activeController = this;
        setupBackButton();
        
        Product p = ProductDataManager.getInstance().getLiveBiddingProductData();
        if (p != null) {
            setProduct(p);
            ProductDataManager.getInstance().setLiveBiddingProductData(null);
        } else {
            startCountdown();
        }
        
        updateBalanceUI();
        configureTableAndChart();
        // Bắt đầu periodic updates khi vào LiveBidding
        startPeriodicUpdates();
    }

    public void setSellerMonitorMode(boolean sellerMonitorMode) {
        this.sellerMonitorMode = sellerMonitorMode;
        if (sellerMonitorMode) {
            setReturnTarget("/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
        }
        updateBiddingControlsVisibility();
    }

    public void setReturnTarget(String fxmlPath, String title) {
        this.returnFxmlPath = fxmlPath == null || fxmlPath.isBlank()
                ? "/fxml/AuctionList.fxml"
                : fxmlPath;
        this.returnTitle = title == null || title.isBlank()
                ? "Auction System"
                : title;
    }

    private void updateBiddingControlsVisibility() {
        boolean showBiddingControls = !sellerMonitorMode;
        if (biddingControlsPanel != null) {
            biddingControlsPanel.setVisible(showBiddingControls);
            biddingControlsPanel.setManaged(showBiddingControls);
        }
        if (bidAmountField != null) {
            bidAmountField.setDisable(sellerMonitorMode || bidAmountField.isDisable());
        }
        if (autoBidButton != null) {
            autoBidButton.setDisable(sellerMonitorMode);
        }
    }

    private void configureTableAndChart() {
        if (bidHistoryTable != null) {
            timeCol.setCellValueFactory(cellData -> cellData.getValue().timeProperty());
            userCol.setCellValueFactory(cellData -> cellData.getValue().userProperty());
            priceCol.setCellValueFactory(cellData -> cellData.getValue().priceProperty());
            changeCol.setCellValueFactory(cellData -> cellData.getValue().changeProperty());

            userCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        BidHistoryModel model = getTableView().getItems().get(getIndex());
                        if (model.isLeader()) {
                            setText(item + " 👑");
                            setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold;"); // Vàng cho người dẫn đầu
                        } else {
                            setText(item);
                            setStyle("-fx-text-fill: #172033;");
                        }
                    }
                }
            });

            changeCol.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                        BidHistoryModel model = getTableView().getItems().get(getIndex());
                        if (model.getNumericChange() > 0) {
                            setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); // Xanh lá
                        } else {
                            setStyle("-fx-text-fill: #9ca3af;"); // Xám
                        }
                    }
                }
            });

            bidHistoryTable.setItems(bidHistoryModels);
        }
    }

    public static void onBidUpdated(String auctionId, double currentPrice, String leadingBidderId) {
        LiveBiddingController controller = activeController;
        if (controller == null) {
            return;
        }

        Platform.runLater(() -> controller.applyExternalBidUpdate(auctionId, currentPrice, leadingBidderId));
    }

    public static void onAuctionEnded(String auctionId) {
        LiveBiddingController controller = activeController;
        if (controller == null) {
            return;
        }

        Platform.runLater(() -> controller.applyExternalAuctionEnded(auctionId));
    }

    public static void onAuctionExtended(String auctionId, String newEndDateTime) {
        LiveBiddingController controller = activeController;
        if (controller == null) {
            return;
        }

        Platform.runLater(() -> controller.applyExternalAuctionExtended(auctionId, newEndDateTime));
    }

    private void updateBalanceUI() {
        if (accountBalanceLabel != null) {
            double balance = ProductDataManager.getInstance().getUserBalance();
            accountBalanceLabel.setText("$" + String.format("%.2f", balance));
        }
    }

    private void updateMinIncrementUI(double minIncrement) {
        currentMinIncrement = Math.max(0, minIncrement);
        if (minIncrementLabel != null) {
            minIncrementLabel.setText(currentMinIncrement > 0
                    ? "$" + String.format("%.2f", currentMinIncrement)
                    : "--");
        }
        if (bidAmountField != null && currentProduct != null && currentMinIncrement > 0) {
            double minimumBid = currentProduct.getPrice() + currentMinIncrement;
            bidAmountField.setPromptText("Tối thiểu $" + String.format("%.2f", minimumBid));
        }
    }

    private void refreshBalanceFromServer() {
        BalanceResponseDTO response = accountService.getBalance();
        if (response != null && response.getUserId() != null) {
            ProductDataManager.getInstance().setUserBalance(response.getBalance());
        }
        updateBalanceUI();
    }

    private void refreshBidHistoryFromServer() {
        if (currentProduct == null || bidHistoryModels == null) {
            return;
        }

        String auctionId = currentProduct.getId();
        ProductDataManager manager = ProductDataManager.getInstance();
        List<BidHistoryDTO> cachedHistory = manager.getCachedBidHistory(auctionId);
        if (bidHistoryModels.isEmpty() && !cachedHistory.isEmpty()) {
            if (Platform.isFxApplicationThread()) {
                renderBidHistory(auctionId, cachedHistory);
            } else {
                Platform.runLater(() -> renderBidHistory(auctionId, cachedHistory));
            }
        }

        CompletableFuture
                .supplyAsync(() -> auctionService.getBidHistory(auctionId))
                .thenAccept(serverHistory -> {
                    manager.cacheBidHistory(auctionId, serverHistory);
                    Platform.runLater(() -> renderBidHistory(auctionId, serverHistory));
                })
                .exceptionally(error -> {
                    System.err.println("[LiveBiddingController] Cannot refresh bid history: " + error.getMessage());
                    return null;
                });
    }

    private void renderBidHistory(String auctionId, List<BidHistoryDTO> serverHistory) {
        if (currentProduct == null || auctionId == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

            bidHistoryModels.clear();

            if (serverHistory == null || serverHistory.isEmpty()) {
                updateLeadingBidder(null);
                updatePriceChangeUI(currentProduct.getPrice(), currentProduct.getPrice());
                
                if (priceLineChart != null) priceLineChart.getData().clear();
                return;
            }

            updateLeadingBidder(serverHistory.get(0));
            
            double startingPrice = serverHistory.get(serverHistory.size() - 1).getBidAmount();
            double currentMaxPrice = serverHistory.get(0).getBidAmount();
            String topBidKey = buildBidKey(serverHistory.get(0));
            boolean hasNewTopBid = lastRenderedTopBidKey != null && !lastRenderedTopBidKey.equals(topBidKey);
            
            updatePriceChangeUI(startingPrice, currentMaxPrice);

            XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
            XYChart.Series<String, Number> areaSeries = new XYChart.Series<>();
            String chartTimePattern = hasDuplicateChartMinuteLabels(serverHistory) ? "HH:mm:ss" : "HH:mm";

            // Build history list and chart data
            for (int i = 0; i < serverHistory.size(); i++) {
                BidHistoryDTO bid = serverHistory.get(i);
                boolean isLeader = (i == 0);
                
                double previousPrice = startingPrice;
                if (i < serverHistory.size() - 1) {
                    previousPrice = serverHistory.get(i + 1).getBidAmount();
                }
                
                double diff = bid.getBidAmount() - previousPrice;
                double diffPercent = previousPrice > 0 ? (diff / previousPrice) * 100 : 0;
                
                String changeText = "";
                if (diff > 0) {
                    changeText = String.format("+ $%.2f (%.2f%%)", diff, diffPercent);
                } else if (diff < 0) {
                    changeText = String.format("- $%.2f (%.2f%%)", Math.abs(diff), diffPercent);
                }
                
                String timeStr = formatTimeStr(bid.getBidTime(), "dd/MM/yyyy HH:mm:ss");
                String username = bid.getBidderUsername() == null ? "Unknown" : bid.getBidderUsername();
                String priceStr = "$" + String.format("%.2f", bid.getBidAmount());
                
                bidHistoryModels.add(new BidHistoryModel(timeStr, username, priceStr, changeText, isLeader, diff));
            }
            
            // For chart, we need oldest to newest (reverse of serverHistory)
            for (int i = serverHistory.size() - 1; i >= 0; i--) {
                BidHistoryDTO bid = serverHistory.get(i);
                boolean isLatest = (i == 0);
                double previousPrice = i < serverHistory.size() - 1
                        ? serverHistory.get(i + 1).getBidAmount()
                        : startingPrice;
                double diff = bid.getBidAmount() - previousPrice;

                String timeStr = formatTimeStr(bid.getBidTime(), chartTimePattern);
                
                XYChart.Data<String, Number> data1 = new XYChart.Data<>(timeStr, bid.getBidAmount());
                
                javafx.scene.layout.StackPane node = new javafx.scene.layout.StackPane();
                node.getStyleClass().addAll("chart-line-symbol", "series0", "default-color0");
                // Tăng kích thước vùng click lên một chút để dễ hover hơn
                node.setPrefSize(14, 14);
                node.setMaxSize(14, 14);
                
                if (isLatest) {
                    javafx.scene.control.Label priceLabel = new javafx.scene.control.Label(String.format("$%.0f", bid.getBidAmount()));
                    priceLabel.setManaged(false);
                    priceLabel.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    node.setStyle("-fx-background-color: #f59e0b, white;");
                    priceLabel.setVisible(true);
                    
                    priceLabel.layoutXProperty().bind(node.widthProperty().divide(2).subtract(priceLabel.widthProperty().divide(2)));
                    priceLabel.layoutYProperty().bind(node.heightProperty().divide(2).subtract(25));
                    
                    node.getChildren().add(priceLabel);
                } else {
                    node.setCursor(javafx.scene.Cursor.HAND);
                }
                installBidTooltip(node, bid, diff);
                
                data1.setNode(node);
                
                lineSeries.getData().add(data1);
            }

            if (priceLineChart != null) priceLineChart.getData().setAll(lineSeries);
            if (hasNewTopBid) {
                flashCurrentPriceLabel();
            }
            lastRenderedTopBidKey = topBidKey;
    }

    private String buildBidKey(BidHistoryDTO bid) {
        if (bid == null) {
            return "";
        }
        String bidId = bid.getBidId();
        if (bidId != null && !bidId.isBlank()) {
            return bidId;
        }
        return String.join("|",
                String.valueOf(bid.getAuctionId()),
                String.valueOf(bid.getBidderUsername()),
                String.valueOf(bid.getBidAmount()),
                String.valueOf(bid.getBidTime()));
    }

    private boolean hasDuplicateChartMinuteLabels(List<BidHistoryDTO> history) {
        java.util.Set<String> labels = new java.util.HashSet<>();
        for (BidHistoryDTO bid : history) {
            String label = formatTimeStr(bid.getBidTime(), "HH:mm");
            if (!labels.add(label)) {
                return true;
            }
        }
        return false;
    }

    private void installBidTooltip(javafx.scene.Node node, BidHistoryDTO bid, double diff) {
        String bidder = bid.getBidderUsername() == null || bid.getBidderUsername().isBlank()
                ? "Unknown"
                : bid.getBidderUsername();
        String changeText = diff > 0 ? String.format("+$%.2f", diff) : "$0.00";
        String tooltipText = String.format(
                "Người đặt: %s%nGiá: $%.2f%nThời gian: %s%nTăng: %s",
                bidder,
                bid.getBidAmount(),
                formatTimeStr(bid.getBidTime(), "dd/MM/yyyy HH:mm:ss"),
                changeText
        );
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
        tooltip.setStyle("-fx-background-color: white; -fx-border-color: #22c55e; -fx-text-fill: #172033; -fx-padding: 8 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12px;");
        tooltip.setShowDelay(javafx.util.Duration.ZERO);
        javafx.scene.control.Tooltip.install(node, tooltip);
    }

    private void flashCurrentPriceLabel() {
        if (currentPriceLabel == null) {
            return;
        }
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), currentPriceLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.35);
        fade.setAutoReverse(true);
        fade.setCycleCount(4);
        fade.play();
    }

    private void updatePriceChangeUI(double startingPrice, double currentPrice) {
        if (priceChangeLabel == null) return;
        double diff = currentPrice - startingPrice;
        double diffPercent = startingPrice > 0 ? (diff / startingPrice) * 100 : 0;
        
        if (diff >= 0) {
            priceChangeLabel.setText(String.format("↑ $%.2f (%.0f%%) so với giá khởi điểm", diff, diffPercent));
            priceChangeLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            priceChangeLabel.setText(String.format("↓ $%.2f (%.0f%%) so với giá khởi điểm", Math.abs(diff), Math.abs(diffPercent)));
            priceChangeLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
    }

    private String formatTimeStr(String rawTime, String pattern) {
        if (rawTime == null || rawTime.isEmpty()) return "";
        try {
            // Parse chuẩn ISO-8601 có chứa múi giờ (VD: 2026-06-02T11:58:00Z)
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(rawTime);
            // Chuyển sang múi giờ Việt Nam
            java.time.ZonedDateTime localTime = zdt.withZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            return localTime.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            // Fallback nếu không có chữ Z hoặc múi giờ
            try {
                // Xử lý chuỗi kiểu SQL Timestamp: "2026-06-01 06:02:07.0" -> "2026-06-01T06:02:07.0"
                String normalizedTime = rawTime.replace(" ", "T");
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(normalizedTime);
                
                // Mặc định dữ liệu thô từ server là UTC (+0), do đó ta gán nó vào UTC trước
                java.time.ZonedDateTime utcZdt = ldt.atZone(java.time.ZoneOffset.UTC);
                // Sau đó mới chuyển sang múi giờ Việt Nam (+7)
                java.time.ZonedDateTime localTime = utcZdt.withZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
                
                return localTime.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ex) {
                // Trả về nguyên bản nếu parse thất bại
                return rawTime;
            }
        }
    }

    private void updateLeadingBidder(BidHistoryDTO highestBid) {
        if (leadingBidderLabel == null) {
            return;
        }

        if (highestBid == null || highestBid.getBidderUsername() == null || highestBid.getBidderUsername().isBlank()) {
            leadingBidderLabel.setText("Chưa có");
            return;
        }

        leadingBidderLabel.setText(highestBid.getBidderUsername());
        ProductDataManager.getInstance().setLeadingUser(currentProduct.getId(), highestBid.getBidderUsername());
    }

    private String getCurrentDisplayName() {
        String username = SessionManager.getCurrentUsername();
        return username == null || username.isBlank() ? "Bạn" : username;
    }

    @FXML
    private void handlePlaceBid() {
        if (sellerMonitorMode) {
            return;
        }
        if (currentProduct == null) return;

        timeLeft = calculateSecondsRemaining(currentProduct.getEndDateTime());
        if (timeLeft <= 0) {
            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(bidAmountField.getText());
            double currentPrice = currentProduct.getPrice();
            double minimumBid = currentPrice + Math.max(0, currentMinIncrement);

            if (bidAmount >= minimumBid) {
                PlaceBidResponseDTO response;
                try {
                    response = auctionService.placeBid(currentProduct.getId(), bidAmount);
                } catch (IllegalStateException ex) {
                    showAlert("Lỗi kết nối", "Không thể kết nối tới server.");
                    return;
                }

                if (response == null || !response.isSuccess()) {
                    showAlert("Đặt giá thất bại", getPlaceBidFailureMessage(response));
                    return;
                }

                ProductDataManager manager = ProductDataManager.getInstance();
                String displayName = getCurrentDisplayName();
                manager.setLeadingUser(currentProduct.getId(), displayName);

                currentProduct.setPrice(bidAmount);
                currentPriceLabel.setText("$" + String.format("%.2f", bidAmount));
                manager.setCurrentPrice(currentProduct.getId(), bidAmount);
                updateMinIncrementUI(currentMinIncrement);

                refreshBalanceFromServer();
                refreshBidHistoryFromServer();

                bidAmountField.clear();

            } else {
                showAlert("Giá thầu thấp", "Bạn phải đặt tối thiểu $" + String.format("%.2f", minimumBid));
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    @FXML
    private void handleOpenAutoBiddingDialog() {
        if (sellerMonitorMode) {
            return;
        }
        if (currentProduct == null) {
            showAlert("Lỗi", "Không có sản phẩm nào được chọn");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AutoBiddingDialog.fxml"));
            Parent root = loader.load();
            AutoBiddingController controller = loader.getController();

            // Set auction info
            controller.setAuctionInfo(
                    currentProduct.getId(),
                    currentProduct.getName(),
                    currentProduct.getPrice(),
                    currentMinIncrement,
                    ProductDataManager.getInstance().getUserBalance()
            );

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Đặt Giá Tự Động");
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            dialogStage.sizeToScene();
            dialogStage.showAndWait();

            // Refresh bid history after setting auto-bid
            Platform.runLater(this::refreshBidHistoryFromServer);
        } catch (Exception e) {
            System.err.println("Error opening auto-bidding dialog: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở dialog đặt giá tự động.\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public void setProduct(Product product) {
        stopTimer();
        this.currentProduct = product;

        boolean alreadyShown = ProductDataManager.getInstance().isWinnerDialogShown(product.getId());

        productNameLabel.setText(product.getName());
        double savedPrice = ProductDataManager.getInstance().getCurrentPrice(product.getId(), product.getPrice());
        currentPriceLabel.setText("$" + String.format("%.2f", savedPrice));
        this.currentProduct.setPrice(savedPrice);
        updateMinIncrementUI(product.getMinIncrement());

        String endDt = product.getEndDateTime();
        loadAuctionDetailAsync(product.getId());

        this.timeLeft = calculateSecondsRemaining(endDt);

        // Debug logging to help diagnose missing time/countdown issues
        System.out.println("[LiveBiddingController] AuctionId=" + product.getId() + " endDateTime='" + endDt + "' parsedSeconds=" + (timeLeft == Long.MAX_VALUE ? "UNKNOWN" : timeLeft));

        if (bidHistoryModels != null) {
            bidHistoryModels.clear();
        }
        refreshBidHistoryFromServer();

        if (timeLeft <= 0) {
            timerLabel.setText("HẾT GIỜ");
            if (bidAmountField != null) {
                bidAmountField.setDisable(true);
            }

            String finalLeader = ProductDataManager.getInstance().getLeadingUser(product.getId(), "");
            String historyName = finalLeader.isEmpty() ? "Không có người đặt giá" : finalLeader;
            if (bidHistoryModels != null) {
                bidHistoryModels.add(0, new BidHistoryModel("", "🏆 NGƯỜI CHIẾN THẮNG: " + historyName, "", "", false, 0));
            }

            if (!alreadyShown) {
                ProductDataManager.getInstance().setWinnerDialogShown(product.getId(), true);
                Platform.runLater(() -> showWinnerDialog(finalLeader));
            }
        } else {
            startCountdown();
        }
    }

    private void loadAuctionDetailAsync(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        CompletableFuture
                .supplyAsync(() -> auctionService.getAuctionById(auctionId))
                .thenAccept(detail -> Platform.runLater(() -> applyAuctionDetail(auctionId, detail)))
                .exceptionally(error -> {
                    System.err.println("[LiveBiddingController] Warning: cannot fetch auction detail: " + error.getMessage());
                    return null;
                });
    }

    private void applyAuctionDetail(String auctionId, AuctionListDTO detail) {
        if (detail == null || currentProduct == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

        if (detail.getEndDateTime() != null && !detail.getEndDateTime().isBlank()) {
            currentProduct.setEndDateTime(detail.getEndDateTime());
            timeLeft = calculateSecondsRemaining(detail.getEndDateTime());
        }
        if (detail.getMinIncrement() > 0) {
            updateMinIncrementUI(detail.getMinIncrement());
        }
    }

    private void startCountdown() {
        if (timer != null) timer.cancel();
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentProduct == null) {
                        stopTimer();
                        return;
                    }

                    timeLeft = calculateSecondsRemaining(currentProduct.getEndDateTime());

                    if (timeLeft == Long.MAX_VALUE) {
                        timerLabel.setText("Chưa rõ thời gian kết thúc");
                    } else if (timeLeft > 0) {
                        timerLabel.setText(formatTimeLeft(timeLeft));
                    } else {
                        timerLabel.setText("HẾT GIỜ");
                        timerLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                        if (bidAmountField != null) {
                            bidAmountField.setDisable(true);
                        }

                        if (!ProductDataManager.getInstance().isWinnerDialogShown(currentProduct.getId())) {
                            ProductDataManager.getInstance().setWinnerDialogShown(currentProduct.getId(), true);

                            // Gửi chuỗi rỗng để showWinnerDialog tự xử lý hiển thị
                            String winnerName = ProductDataManager.getInstance().getLeadingUser(currentProduct.getId(), "");
                            showWinnerDialog(winnerName);

                            stopTimer();
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    private long calculateSecondsRemaining(String endDateTime) {
        LocalDateTime endTime = parseEndDateTime(endDateTime);
        if (endTime == null) {
            return Long.MAX_VALUE;
        }
        return Duration.between(LocalDateTime.now(), endTime).getSeconds();
    }

    private String formatTimeLeft(long secondsRemaining) {
        long days = secondsRemaining / 86400;
        long hours = (secondsRemaining % 86400) / 3600;
        long minutes = (secondsRemaining % 3600) / 60;
        long seconds = secondsRemaining % 60;

        if (days > 0) {
            return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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

    private void applyExternalBidUpdate(String auctionId, double currentPrice, String leadingBidderId) {
        if (currentProduct == null || auctionId == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

        ProductDataManager manager = ProductDataManager.getInstance();
        String currentUserId = SessionManager.getCurrentUserId();
        boolean isMe = currentUserId != null && currentUserId.equals(leadingBidderId);

        currentProduct.setPrice(currentPrice);
        currentPriceLabel.setText("$" + String.format("%.2f", currentPrice));
        manager.setCurrentPrice(auctionId, currentPrice);
        updateMinIncrementUI(currentMinIncrement);
        String displayName = isMe ? getCurrentDisplayName() : leadingBidderId;
        manager.setLeadingUser(auctionId, displayName);
        refreshBalanceFromServer();
        refreshBidHistoryFromServer();
    }

    private void applyExternalAuctionEnded(String auctionId) {
        if (currentProduct == null || auctionId == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

        ProductDataManager.getInstance().closeAuction(auctionId);
        timerLabel.setText("HẾT GIỜ");
        if (bidAmountField != null) {
            bidAmountField.setDisable(true);
        }
        stopTimer();

        if (bidHistoryModels != null) {
            bidHistoryModels.add(0, new BidHistoryModel("", "🏁 Phiên đấu giá đã kết thúc.", "", "", false, 0));
        }
    }

    private void applyExternalAuctionExtended(String auctionId, String newEndDateTime) {
        if (currentProduct == null || auctionId == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

        currentProduct.setEndDateTime(newEndDateTime);
        ProductDataManager.getInstance().updateAuctionEndDate(auctionId, newEndDateTime);
        if (bidAmountField != null) {
            bidAmountField.setDisable(sellerMonitorMode);
        }
        if (bidHistoryModels != null) {
            bidHistoryModels.add(0, new BidHistoryModel("", "Hệ thống: Phiên đấu giá đã được gia hạn.", "", "", false, 0));
        }
    }

    private void showWinnerDialog(String winnerName) {
        // Lấy data thô từ Manager
        String leaderInMap = ProductDataManager.getInstance().getLeadingUser(currentProduct.getId(), "");

        String finalWinner;
        // Logic ưu tiên: Map Manager > Tham số truyền vào > Mặc định
        if (leaderInMap != null && !leaderInMap.isEmpty()) {
            finalWinner = leaderInMap;
        } else if (winnerName != null && !winnerName.isEmpty()) {
            finalWinner = winnerName;
        } else {
            finalWinner = "Không có người đặt giá";
        }

        final String displayWinner = finalWinner;

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kết quả đấu giá");
            alert.setHeaderText("Phiên đấu giá đã kết thúc!");
            alert.setContentText("Người chiến thắng: " + displayWinner + "\nSản phẩm: " + currentProduct.getName());

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI';");

            alert.showAndWait();
        });
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Bắt đầu periodic tasks khi vào LiveBidding
     */
    private void startPeriodicUpdates() {
        // Periodic price refresh mỗi 5 giây
        PeriodicUpdateService.getInstance().startPeriodicPriceRefresh(() -> {
            if (currentProduct != null) {
                AuctionListDTO updated = auctionService.getAuctionById(currentProduct.getId());
                if (updated != null && updated.getCurrentPrice() > 0) {
                    Platform.runLater(() -> {
                        if (updated.getMinIncrement() > 0) {
                            updateMinIncrementUI(updated.getMinIncrement());
                        }
                        if (currentProduct.getPrice() != updated.getCurrentPrice()) {
                            currentProduct.setPrice(updated.getCurrentPrice());
                            currentPriceLabel.setText("$" + String.format("%.2f", updated.getCurrentPrice()));
                            updateMinIncrementUI(currentMinIncrement);
                            System.out.println("[LiveBiddingController] Price refreshed to: " + updated.getCurrentPrice());
                        }
                    });
                }
            }
        });

        // Periodic balance refresh mỗi 30 giây
        PeriodicUpdateService.getInstance().startPeriodicBalanceRefresh(this::refreshBalanceFromServer);

        // Periodic bid history sync mỗi 10 giây
        PeriodicUpdateService.getInstance().startPeriodicBidHistorySync(this::refreshBidHistoryFromServer);
    }

    /**
     * Dừng periodic tasks khi rời khỏi LiveBidding
     */
    private void stopPeriodicUpdates() {
        PeriodicUpdateService.getInstance().stopPeriodicPriceRefresh();
        PeriodicUpdateService.getInstance().stopPeriodicBalanceRefresh();
        PeriodicUpdateService.getInstance().stopPeriodicBidHistorySync();
    }

    @FXML
    private void handleBack() {
        stopTimer();
        stopPeriodicUpdates();
        activeController = null;
        switchToReturnTarget();
    }

    private void switchToReturnTarget() {
        NavigationService.getInstance().navigateTo(returnFxmlPath, returnTitle, 1280, 800);
    }

    private void switchToAuctionList() {
        try {
            java.net.URL resource = getClass().getResource("/fxml/AuctionList.fxml");
            if (resource == null) {
                throw new IllegalStateException("Không tìm thấy file FXML: /fxml/AuctionList.fxml");
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) productNameLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 800);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
            stage.setTitle("UET Auction System");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể chuyển sang màn hình danh sách đấu giá.", e);
        }
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setStyle("-fx-background-color: #22304a; -fx-text-fill: #afb9c7; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 6;");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String getPlaceBidFailureMessage(PlaceBidResponseDTO response) {
        if (response == null) {
            return "Server không phản hồi.";
        }

        String message = response.getMessage();
        return (message == null || message.isBlank()) ? "Đặt giá thất bại." : message;
    }
}

