package com.auction.client.controller;

import com.app.common.dto.PlaceBidResponseDTO;
import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import com.auction.client.service.AuctionService;
import com.auction.client.session.SessionManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
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
    @FXML private Label accountBalanceLabel;
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private Button backButton;

    private Product currentProduct;
    private Timer timer;
    private int timeLeft = 30;
    private ObservableList<String> bidHistory;
    private static LiveBiddingController activeController;
    private final AuctionService auctionService = new AuctionService();

    @FXML
    public void initialize() {
        activeController = this;
        setupBackButton();
        startCountdown();
        updateBalanceUI();
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

    private void updateBalanceUI() {
        if (accountBalanceLabel != null) {
            double balance = ProductDataManager.getInstance().getUserBalance();
            accountBalanceLabel.setText("$" + String.format("%.2f", balance));
        }
    }

    @FXML
    private void handlePlaceBid() {
        if (currentProduct == null) return;

        if (timeLeft <= 0) {
            showAlert("Thông báo", "Phiên đấu giá đã kết thúc!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(bidAmountField.getText());
            double currentPrice = currentProduct.getPrice();

            if (bidAmount > currentPrice) {
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
                double previouslyHeld = manager.getHeldMoney(currentProduct.getId());
                manager.refundBalance(previouslyHeld);
                manager.deductBalance(bidAmount);
                manager.setHeldMoney(currentProduct.getId(), bidAmount);
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
                showAlert("Giá thầu thấp", "Bạn phải đặt cao hơn $" + currentPrice);
            }
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
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

    private void applyExternalBidUpdate(String auctionId, double currentPrice, String leadingBidderId) {
        if (currentProduct == null || auctionId == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

        ProductDataManager manager = ProductDataManager.getInstance();
        String currentUserId = SessionManager.getCurrentUserId();
        boolean isMe = currentUserId != null && currentUserId.equals(leadingBidderId);

        if (!isMe) {
            double heldMoney = manager.getHeldMoney(auctionId);
            if (heldMoney > 0) {
                manager.refundBalance(heldMoney);
                manager.setHeldMoney(auctionId, 0.0);
            }
        } else {
            manager.setHeldMoney(auctionId, currentPrice);
        }

        currentProduct.setPrice(currentPrice);
        currentPriceLabel.setText("$" + String.format("%.2f", currentPrice));
        manager.setCurrentPrice(auctionId, currentPrice);
        manager.setLeadingUser(auctionId, isMe ? "Bạn" : leadingBidderId);
        updateBalanceUI();

        if (bidHistory != null) {
            bidHistory.add(0, isMe
                    ? "Bạn đã được cập nhật giá dẫn đầu: $" + currentPrice
                    : "Người khác đã đặt giá: $" + currentPrice + " (" + leadingBidderId + ")");
        }
    }

    private void applyExternalAuctionEnded(String auctionId) {
        if (currentProduct == null || auctionId == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

        ProductDataManager.getInstance().closeAuction(auctionId);
        timerLabel.setText("HẾT GIỜ");
        bidAmountField.setDisable(true);
        stopTimer();

        if (bidHistory != null) {
            bidHistory.add(0, "🏁 Phiên đấu giá đã kết thúc.");
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

    @FXML
    private void handleBack() {
        stopTimer();
        activeController = null;
        switchToAuctionList();
    }

    private void switchToAuctionList() {
        try {
            java.net.URL resource = getClass().getResource("/fxml/AuctionList.fxml");
            if (resource == null) {
                throw new IllegalStateException("Không tìm thấy file FXML: /fxml/AuctionList.fxml");
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) productNameLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1040, 660);
            stage.setTitle("UET Auction System");
            stage.setScene(scene);
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