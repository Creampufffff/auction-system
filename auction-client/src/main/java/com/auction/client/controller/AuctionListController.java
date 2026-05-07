package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class AuctionListController {

    @FXML private TableView<?> auctionTable;
    @FXML private TableColumn<?, ?> idColumn;
    @FXML private TableColumn<?, ?> nameColumn;
    @FXML private TableColumn<?, ?> priceColumn;
    @FXML private TableColumn<?, ?> statusColumn;

    @FXML
    public void initialize() {
        // Load data vào table
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        // Logic chuyển sang ProductDetail.fxml
    }

    @FXML
    private void handleJoinBidding(ActionEvent event) {
        // Logic chuyển sang LiveBidding.fxml
    }

    @FXML
    private void handleSellerManagement(ActionEvent event) {
        // Logic chuyển sang ProductManagement.fxml
    }
}