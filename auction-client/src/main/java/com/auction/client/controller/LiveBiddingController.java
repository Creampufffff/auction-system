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
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private Button backButton;

    private Product currentProduct;
    private Timer timer;
    private int timeLeft = 30;

    private ObservableList<String> bidHistory;
    private static LiveBiddingController activeController;

    @FXML
    public void initialize() {
        activeController = this;
        setupBackButton();
        startCountdown();
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        if (currentProduct == null) return;

        if (timeLeft <= 0) {
            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
            bidAmountField.setDisable(true);
            return;
        }

        try {
            double bidAmount = Double.parseDouble(bidAmountField.getText());
            double currentPrice = currentProduct.getPrice();

            if (bidAmount > currentPrice) {
                currentProduct.setPrice(bidAmount);
                currentPriceLabel.setText("$" + String.format("%.2f", bidAmount));

                // 1. Lưu giá vào Manager để đồng bộ dữ liệu
                ProductDataManager.getInstance().setCurrentPrice(currentProduct.getId(), bidAmount);

                // [MỚI] 2. CẬP NHẬT GIÁ REAL-TIME CHO BẢNG DANH SÁCH BÊN NGOÀI
                ProductDataManager.getInstance().getServerAuctionList().stream()
                        .filter(a -> a.getAuctionId().equals(currentProduct.getId()))
                        .findFirst()
                        .ifPresent(a -> a.setCurrentPrice(bidAmount));

                // 3. Cập nhật người dẫn đầu
                ProductDataManager.getInstance().setLeadingUser(currentProduct.getId(), "dang_thang_uet");

                // Anti-Sniping logic
                if (timeLeft <= 30) {
                    timeLeft += 30;
                    ProductDataManager.getInstance().setTimeLeft(currentProduct.getId(), timeLeft);
                    bidHistory.add(0, "⚠️ Hệ thống: Gia hạn thêm 30 giây!");
                }

                bidHistory.add(0, "Bạn đã đặt giá: $" + bidAmount);
                bidAmountField.clear();
            } else {
                showAlert("Giá thầu thấp", "Bạn phải đặt cao hơn $" + currentPrice);
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    public void setProduct(Product product) {
        this.currentProduct = product;
        productNameLabel.setText(product.getName());

        double savedPrice = ProductDataManager.getInstance().getCurrentPrice(product.getId(), product.getPrice());
        currentPriceLabel.setText("$" + String.format("%.2f", savedPrice));
        this.currentProduct.setPrice(savedPrice);

        this.timeLeft = ProductDataManager.getInstance().getTimeLeft(product.getId(), 30);

        this.bidHistory = ProductDataManager.getInstance().getHistoryForProduct(product.getId());
        bidHistoryList.setItems(this.bidHistory);

        if (timeLeft <= 0) {
            timerLabel.setText("HẾT GIỜ");
            bidAmountField.setDisable(true);
        }
    }

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
                        int mins = timeLeft / 60;
                        int secs = timeLeft % 60;
                        timerLabel.setText(String.format("%02d:%02d", mins, secs));
                    } else {
                        timerLabel.setText("HẾT GIỜ");
                        timerLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                        bidAmountField.setDisable(true);

                        ProductDataManager.getInstance().closeAuction(currentProduct.getId());

                        String winnerName = ProductDataManager.getInstance().getLeadingUser(currentProduct.getId(), "Không có ai");
                        showWinnerDialog(winnerName);

                        stopTimer();
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
        alert.show();
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
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
            root.requestFocus();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setFocusTraversable(false);
            String style = "-fx-background-color: rgba(255, 255, 255, 0.05) !important;" +
                    "-fx-background-insets: 0 !important;" +
                    "-fx-background-radius: 6 !important;" +
                    "-fx-border-color: rgba(255, 255, 255, 0.2) !important;" +
                    "-fx-border-width: 1 !important;" +
                    "-fx-border-radius: 6 !important;" +
                    "-fx-text-fill: #afb9c7 !important;" +
                    "-fx-font-weight: bold !important;" +
                    "-fx-padding: 11 14 !important;" +
                    "-fx-effect: null !important;";
            backButton.setStyle(style);
            backButton.setOnMouseEntered(e -> backButton.setStyle(style + "-fx-background-color: #2d6cdf !important; -fx-text-fill: white !important;"));
            backButton.setOnMouseExited(e -> backButton.setStyle(style));
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