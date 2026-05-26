package com.auction.client.controller;

import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import com.auction.client.service.AccountService;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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
    private long timeLeft;
    private ObservableList<String> bidHistory;
    private static LiveBiddingController activeController;
    private final AuctionService auctionService = new AuctionService();
    private final AccountService accountService = new AccountService();

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

    private void refreshBalanceFromServer() {
        BalanceResponseDTO response = accountService.getBalance();
        if (response != null && response.getUserId() != null) {
            ProductDataManager.getInstance().setUserBalance(response.getBalance());
        }
        updateBalanceUI();
    }

    private String getCurrentDisplayName() {
        String username = SessionManager.getCurrentUsername();
        return username == null || username.isBlank() ? "Bạn" : username;
    }

    @FXML
    private void handlePlaceBid() {
        if (currentProduct == null) return;

        timeLeft = calculateSecondsRemaining(currentProduct.getEndDateTime());
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
                String displayName = getCurrentDisplayName();
                manager.setLeadingUser(currentProduct.getId(), displayName);

                currentProduct.setPrice(bidAmount);
                currentPriceLabel.setText("$" + String.format("%.2f", bidAmount));
                manager.setCurrentPrice(currentProduct.getId(), bidAmount);

                refreshBalanceFromServer();

                bidHistory.add(0, displayName + " đã đặt giá thành công: $" + bidAmount);
                bidAmountField.clear();

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

        this.timeLeft = calculateSecondsRemaining(product.getEndDateTime());

        this.bidHistory = ProductDataManager.getInstance().getHistoryForProduct(product.getId());
        this.bidHistory.clear();
        bidHistoryList.setItems(this.bidHistory);

        // Sử dụng chuỗi rỗng làm mặc định để check logic sạch hơn
        String leader = ProductDataManager.getInstance().getLeadingUser(product.getId(), "");

        if (!leader.isEmpty()) {
            bidHistory.add(0, "ℹ️ Người đang dẫn đầu: " + leader + " ($" + savedPrice + ")");
        } else {
            bidHistory.add(0, "ℹ️ Bạn chưa đặt giá cho sản phẩm này.");
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

                    timeLeft = calculateSecondsRemaining(currentProduct.getEndDateTime());

                    if (timeLeft == Long.MAX_VALUE) {
                        timerLabel.setText("Chưa rõ thời gian kết thúc");
                    } else if (timeLeft > 0) {
                        timerLabel.setText(formatTimeLeft(timeLeft));
                    } else {
                        timerLabel.setText("HẾT GIỜ");
                        timerLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                        bidAmountField.setDisable(true);

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
        String displayName = isMe ? getCurrentDisplayName() : leadingBidderId;
        manager.setLeadingUser(auctionId, displayName);
        refreshBalanceFromServer();

        if (bidHistory != null) {
            bidHistory.add(0, isMe
                    ? displayName + " đã được cập nhật giá dẫn đầu: $" + currentPrice
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

    private void applyExternalAuctionExtended(String auctionId, String newEndDateTime) {
        if (currentProduct == null || auctionId == null || !auctionId.equals(currentProduct.getId())) {
            return;
        }

        currentProduct.setEndDateTime(newEndDateTime);
        ProductDataManager.getInstance().updateAuctionEndDate(auctionId, newEndDateTime);
        bidAmountField.setDisable(false);
        if (bidHistory != null) {
            bidHistory.add(0, "Hệ thống: Phiên đấu giá đã được gia hạn.");
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
