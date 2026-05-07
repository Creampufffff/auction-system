package com.auction.client.controller;

import com.auction.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class LiveBiddingController {

    @FXML private ListView<String> bidHistoryList;
    @FXML private TextField bidAmountField;
    @FXML private Label timerLabel;
    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;

    @FXML
    public void initialize() {
        // Khởi tạo text bằng Java để an toàn tuyệt đối
        productNameLabel.setText("MacBook Pro M3 Max");
        currentPriceLabel.setText("$1,250");
        timerLabel.setText("00:05:30");
        bidHistoryList.getItems().clear();
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String amount = bidAmountField.getText();
        if (amount != null && !amount.isEmpty()) {
            try {
                Double.parseDouble(amount);
                currentPriceLabel.setText("$" + amount);
                bidHistoryList.getItems().add(0, "Bạn đã đặt: $" + amount + " (Thành công)");
                bidAmountField.clear();
            } catch (NumberFormatException e) {
                bidAmountField.setStyle("-fx-border-color: #d92d20;");
            }
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        MainApp.setRoot("/fxml/AuctionList.fxml", "UET Auction System - Danh sách");
    }
}