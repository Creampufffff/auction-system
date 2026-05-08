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
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.Timer;
import java.util.TimerTask;

public class LiveBiddingController {

    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timerLabel;
    @FXML private Label accountBalanceLabel; // MỚI: Thêm Label ví tiền
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private Button backButton;

    private Product currentProduct;
    private Timer timer;
    private int timeLeft = 30;
    private boolean isDialogShown = false; // Biến cờ để kiểm tra
    private ObservableList<String> bidHistory;
    private static LiveBiddingController activeController;

    @FXML
    public void initialize() {
        activeController = this;
        setupBackButton();
        startCountdown();
        updateBalanceUI(); // Cập nhật ví ngay khi vào phòng
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
                    // Xử lý trừ tiền
                    manager.refundBalance(previouslyHeld);
                    manager.deductBalance(bidAmount);
                    manager.setHeldMoney(currentProduct.getId(), bidAmount);

                    // Cập nhật giá
                    currentProduct.setPrice(bidAmount);
                    currentPriceLabel.setText("$" + String.format("%.2f", bidAmount));
                    manager.setCurrentPrice(currentProduct.getId(), bidAmount);

                    // Cập nhật ví trên UI ngay lập tức
                    updateBalanceUI();

                    bidHistory.add(0, "Bạn đã đặt giá thành công: $" + bidAmount);
                    bidAmountField.clear();

                    // Tweak gia hạn thời gian
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
        this.currentProduct = product;
        this.isDialogShown = false;
        this.currentProduct = product;
        productNameLabel.setText(product.getName());

        double savedPrice = ProductDataManager.getInstance().getCurrentPrice(product.getId(), product.getPrice());
        currentPriceLabel.setText("$" + String.format("%.2f", savedPrice));
        this.currentProduct.setPrice(savedPrice);

        this.timeLeft = ProductDataManager.getInstance().getTimeLeft(product.getId(), 30);

        // CHỐNG LẶP: Clear sạch lịch sử trước khi nạp
        this.bidHistory = ProductDataManager.getInstance().getHistoryForProduct(product.getId());
        this.bidHistory.clear();
        bidHistoryList.setItems(this.bidHistory);

        // Hiển thị trạng thái giữ chỗ
        double myHeldMoney = ProductDataManager.getInstance().getHeldMoney(product.getId());
        if (myHeldMoney > 0) {
            bidHistory.add(0, "ℹ️ Bạn đang dẫn đầu sàn này với: $" + myHeldMoney);
        } else {
            bidHistory.add(0, "ℹ️ Bạn chưa đặt giá cho sản phẩm này.");
        }

        if (timeLeft <= 0) {
            timerLabel.setText("HẾT GIỜ");
            bidAmountField.setDisable(true);
        }
    }

    // ... (Giữ nguyên các hàm startCountdown, handleBack, switchScene từ code cũ của bạn) ...
    private void startCountdown() {
        if (timer != null) timer.cancel();
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (activeController != LiveBiddingController.this || currentProduct == null) {
                        stopTimer();
                        return;
                    }

                    timeLeft = ProductDataManager.getInstance().getTimeLeft(currentProduct.getId(), 30);

                    if (timeLeft > 0) {
                        // Reset cờ nếu thời gian được gia hạn (ví dụ bid ở giây cuối)
                        isDialogShown = false;

                        int mins = timeLeft / 60;
                        int secs = timeLeft % 60;
                        timerLabel.setText(String.format("%02d:%02d", mins, secs));
                    } else {
                        timerLabel.setText("HẾT GIỜ");
                        timerLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                        bidAmountField.setDisable(true);

                        // CHỈ HIỆN DIALOG NẾU CHƯA HIỆN
                        if (!isDialogShown) {
                            isDialogShown = true; // Đánh dấu đã hiện

                            ProductDataManager.getInstance().closeAuction(currentProduct.getId());
                            String winnerName = ProductDataManager.getInstance().getLeadingUser(currentProduct.getId(), "Không có ai");

                            showWinnerDialog(winnerName);
                            stopTimer(); // Dừng hẳn timer sau khi hiện xong
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    private void showWinnerDialog(String winnerName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kết quả đấu giá");
        alert.setHeaderText("Phiên đấu giá đã kết thúc!");
        alert.setContentText("Người chiến thắng: " + winnerName + "\nSản phẩm: " + currentProduct.getName());

        // Thêm một chút style cho chuyên nghiệp
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-family: 'Segoe UI';");

        alert.showAndWait(); // Dùng showAndWait để block người dùng xem kết quả trước khi làm việc khác
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