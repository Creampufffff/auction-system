package com.auction.ui.view;

import com.app.common.dto.ApiResponseDTO;
import com.auction.application.service.AutoBidService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AutoBiddingController {
    private String auctionId;
    private double userBalance;
    private double nextBidAmount;

    @FXML private Label auctionNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label minIncrementLabel;
    @FXML private Label userBalanceLabel;
    @FXML private TextField maxBidAmountField;
    @FXML private Label errorLabel;

    private final AutoBidService autoBidService = new AutoBidService();

    @FXML
    public void initialize() {
        errorLabel.setText("");
    }

    public void setAuctionInfo(String auctionId, String auctionName, double currentPrice,
                               double minIncrement, double userBalance) {
        this.auctionId = auctionId;
        this.userBalance = userBalance;
        this.nextBidAmount = currentPrice + minIncrement;

        Platform.runLater(() -> {
            auctionNameLabel.setText(auctionName);
            currentPriceLabel.setText("$" + String.format("%.2f", currentPrice));
            minIncrementLabel.setText("$" + String.format("%.2f", minIncrement));
            userBalanceLabel.setText("$" + String.format("%.2f", userBalance));
            maxBidAmountField.setPromptText("Tối thiểu: $" + String.format("%.2f", nextBidAmount));
        });
    }

    @FXML
    private void handleSetAutoBid() {
        errorLabel.setText("");

        String input = maxBidAmountField.getText().trim();
        if (input.isBlank()) {
            showError("Vui lòng nhập giá tối đa.");
            return;
        }

        double maxAutoAmount;
        try {
            maxAutoAmount = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            showError("Giá phải là số hợp lệ.");
            return;
        }

        if (maxAutoAmount < nextBidAmount) {
            showError("Giá tối đa phải từ $" + String.format("%.2f", nextBidAmount) + " trở lên.");
            return;
        }

        if (maxAutoAmount > userBalance) {
            showError("Giá tối đa không được vượt quá số dư tài khoản ($" + String.format("%.2f", userBalance) + ").");
            return;
        }

        ApiResponseDTO response = autoBidService.setAutoBid(auctionId, maxAutoAmount);

        if (response != null && response.isSuccess()) {
            showSuccess("Auto-Bid đã được đặt thành công.\nGiá tối đa: $" + String.format("%.2f", maxAutoAmount));
            closeDialog();
        } else {
            String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
            showError("Không thể đặt Auto-Bid: " + errorMsg);
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) maxBidAmountField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
