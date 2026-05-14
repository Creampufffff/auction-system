package com.auction.client.controller;

import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart; // MỚI
import javafx.scene.chart.NumberAxis; // MỚI
import javafx.scene.chart.XYChart; // MỚI
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Timer;
import java.util.TimerTask;

public class LiveBiddingController {

    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timerLabel;
    @FXML private Label accountBalanceLabel;
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private Button backButton;

    // --- [MỚI] LIVE GRAPH COMPONENTS ---
    @FXML private LineChart<Number, Number> priceGraph;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    private XYChart.Series<Number, Number> priceSeries;

    private Product currentProduct;
    private Timer timer;
    private int timeLeft = 30;
    private ObservableList<String> bidHistory;
    private static LiveBiddingController activeController;

    @FXML
    public void initialize() {
        activeController = this;
        setupBackButton();
        setupGraph(); // Khởi tạo cấu hình biểu đồ
        startCountdown();
        updateBalanceUI();
    }

    // --- [MỚI] THIẾT LẬP BIỂU ĐỒ ---
// Tìm đến hàm setupGraph() trong LiveBiddingController và cập nhật đoạn xAxis:

    private void setupGraph() {
        if (priceGraph != null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Biến động giá");
            priceGraph.getData().add(priceSeries);
            priceGraph.setAnimated(true);
            priceGraph.setCreateSymbols(true);

            if (xAxis != null) {
                xAxis.setLabel("Time elapsed"); // Đổi nhãn theo ảnh edited-image.png
                xAxis.setAutoRanging(true);

                // --- PHẦN QUAN TRỌNG: Định dạng số giây thành MM:SS ---
                xAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
                    @Override
                    public String toString(Number object) {
                        int totalSeconds = object.intValue();
                        if (totalSeconds < 0) totalSeconds = 0; // Chặn giá trị âm ngay tại đây

                        int minutes = totalSeconds / 60;
                        int seconds = totalSeconds % 60;
                        return String.format("%d:%02d", minutes, seconds);
                    }

                    @Override
                    public Number fromString(String string) { return 0; }
                });
            }

            if (yAxis != null) {
                yAxis.setLabel("Price"); // Đổi nhãn cho giống ảnh mẫu
                yAxis.setForceZeroInRange(false);
            }
        }
    }

    private void updateBalanceUI() {
        if (accountBalanceLabel != null) {
            double balance = ProductDataManager.getInstance().getUserBalance();
            accountBalanceLabel.setText("$" + String.format("%.2f", balance));
        }
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        if (currentProduct == null) return;

        if (timeLeft <= 0) {
            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(bidAmountField.getText());
            double currentPrice = currentProduct.getPrice();

            if (bidAmount > currentPrice) {
                ProductDataManager manager = ProductDataManager.getInstance();
                double previouslyHeld = manager.getHeldMoney(currentProduct.getId());

                if (manager.getUserBalance() + previouslyHeld >= bidAmount) {
                    manager.refundBalance(previouslyHeld);
                    manager.deductBalance(bidAmount);
                    manager.setHeldMoney(currentProduct.getId(), bidAmount);

                    // CẬP NHẬT NGƯỜI DẪN ĐẦU VÀO MANAGER
                    manager.setLeadingUser(currentProduct.getId(), "Bạn");

                    currentProduct.setPrice(bidAmount);
                    currentPriceLabel.setText("$" + String.format("%.2f", bidAmount));
                    manager.setCurrentPrice(currentProduct.getId(), bidAmount);

                    updateBalanceUI();

                    bidHistory.add(0, "Bạn đã đặt giá thành công: $" + bidAmount);
                    bidAmountField.clear();

                    if (timeLeft <= 30) {
                        timeLeft += 30;
                        manager.setTimeLeft(currentProduct.getId(), timeLeft);
                        bidHistory.add(0, "⚠️ Hệ thống: Gia hạn thêm 30 giây!");
                    }
                } else {
                    showAlert("Số dư không đủ", "Bạn không đủ tiền để thực hiện mức giá này!");
                }
            } else {
                showAlert("Giá thầu thấp", "Bạn phải đặt cao hơn $" + currentPrice);
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    public void setProduct(Product product) {
        stopTimer();
        this.currentProduct = product;

        // --- [MỚI] KẾT NỐI DỮ LIỆU BIỂU ĐỒ ---
        if (priceSeries != null) {
            priceSeries.setData(ProductDataManager.getInstance().getPriceGraphData(product.getId()));
        }

        boolean alreadyShown = ProductDataManager.getInstance().isWinnerDialogShown(product.getId());

        productNameLabel.setText(product.getName());
        double savedPrice = ProductDataManager.getInstance().getCurrentPrice(product.getId(), product.getPrice());
        currentPriceLabel.setText("$" + String.format("%.2f", savedPrice));
        this.currentProduct.setPrice(savedPrice);

        this.timeLeft = ProductDataManager.getInstance().getTimeLeft(product.getId(), 30);

        this.bidHistory = ProductDataManager.getInstance().getHistoryForProduct(product.getId());
        this.bidHistory.clear();
        bidHistoryList.setItems(this.bidHistory);

        // Sử dụng chuỗi rỗng làm mặc định để check logic sạch hơn
        String leader = ProductDataManager.getInstance().getLeadingUser(product.getId(), "");
        double myHeldMoney = ProductDataManager.getInstance().getHeldMoney(product.getId());

        if (myHeldMoney > 0) {
            bidHistory.add(0, "ℹ️ Bạn đang dẫn đầu sàn này với: $" + myHeldMoney);
        } else {
            if (!leader.isEmpty()) {
                bidHistory.add(0, "ℹ️ Người đang dẫn đầu: " + leader + " ($" + savedPrice + ")");
            } else {
                bidHistory.add(0, "ℹ️ Bạn chưa đặt giá cho sản phẩm này.");
            }
        }

        if (timeLeft <= 0) {
            timerLabel.setText("HẾT GIỜ");
            bidAmountField.setDisable(true);

            String finalLeader = ProductDataManager.getInstance().getLeadingUser(product.getId(), "");
            String historyName = finalLeader.isEmpty() ? "Không có người đặt giá" : finalLeader;
            bidHistory.add(0, "🏆 NGƯỜI CHIẾN THẮNG CUỐI CÙNG: " + historyName);

            if (!alreadyShown) {
                ProductDataManager.getInstance().setWinnerDialogShown(product.getId(), true);
                Platform.runLater(() -> showWinnerDialog(finalLeader));
            }
        } else {
            startCountdown();
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

                    timeLeft = ProductDataManager.getInstance().getTimeLeft(currentProduct.getId(), 30);

                    if (timeLeft > 0) {
                        int mins = timeLeft / 60;
                        int secs = timeLeft % 60;
                        timerLabel.setText(String.format("%02d:%02d", mins, secs));
                    } else {
                        timerLabel.setText("HẾT GIỜ");
                        timerLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                        bidAmountField.setDisable(true);

                        if (!ProductDataManager.getInstance().isWinnerDialogShown(currentProduct.getId())) {
                            ProductDataManager.getInstance().setWinnerDialogShown(currentProduct.getId(), true);

                            // Gửi chuỗi rỗng để showWinnerDialog tự xử lý hiển thị
                            String winnerName = ProductDataManager.getInstance().getLeadingUser(currentProduct.getId(), "");
                            showWinnerDialog(winnerName);

                            ProductDataManager.getInstance().closeAuction(currentProduct.getId());
                            stopTimer();
                        }
                    }
                });
            }
        }, 0, 1000);
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

    @FXML
    private void handleBack(ActionEvent event) {
        stopTimer();
        activeController = null;
        switchScene("/fxml/AuctionList.fxml", "UET Auction System");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) productNameLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1040, 660);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
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
}