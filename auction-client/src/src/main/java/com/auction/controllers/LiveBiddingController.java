package com.auction.controllers;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class LiveBiddingController {
    @FXML private ListView<String> bidHistoryList;
    @FXML private TextField bidAmountField;

    @FXML public void initialize() {}
    @FXML private void handlePlaceBid(ActionEvent event) {}
}