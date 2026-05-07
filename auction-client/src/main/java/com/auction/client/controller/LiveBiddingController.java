package com.auction.client.controller;

import com.auction.client.model.Product;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
    private int timeLeft = 300;
    private final ObservableList<String> bidHistory = FXCollections.observableArrayList();
    private static LiveBiddingController activeController;

    @FXML
    public void initialize() {
        activeController = this;
        setupBackButton();
        bidHistoryList.setItems(bidHistory);
        startCountdown();
    }

    // ĐỔI TÊN HÀM NÀY ĐỂ KHỚP VỚI FXML CỦA BẠN
    @FXML
    private void handlePlaceBid(ActionEvent event) {
        if (currentProduct == null) return;

        try {
            double bidAmount = Double.parseDouble(bidAmountField.getText());
            // Lấy giá hiện tại từ object Product đã được truyền sang
            double currentPrice = currentProduct.getPrice();

            if (bidAmount > currentPrice) {
                // Cập nhật giá thầu mới
                currentProduct.setPrice(bidAmount);
                currentPriceLabel.setText("$" + String.format("%.2f", bidAmount));

                // Lưu vào lịch sử
                bidHistory.add(0, "Bạn đã đặt giá: $" + bidAmount);
                bidAmountField.clear();
            } else {
                // Hiển thị cảnh báo nếu bid thấp hơn hoặc bằng giá hiện tại
                showAlert("Giá thầu không hợp lệ", "Giá thầu của bạn phải lớn hơn giá hiện tại ($" + currentPrice + ")");
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        }
    }

    private void startCountdown() {
        if (timer != null) timer.cancel();
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (activeController != LiveBiddingController.this) {
                        stopTimer();
                        return;
                    }
                    if (timeLeft > 0) {
                        timeLeft--;
                        int mins = timeLeft / 60;
                        int secs = timeLeft % 60;
                        timerLabel.setText(String.format("%02d:%02d", mins, secs));
                    } else {
                        timerLabel.setText("HẾT GIỜ");
                        stopTimer();
                    }
                });
            }
        }, 0, 1000);
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
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setProduct(Product product) {
        this.currentProduct = product;
        productNameLabel.setText(product.getName());
        currentPriceLabel.setText("$" + String.format("%.2f", product.getPrice()));
    }
}