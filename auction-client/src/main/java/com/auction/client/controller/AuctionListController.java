package com.auction.client.controller;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import com.auction.client.service.AuctionService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AuctionListController {

    @FXML
    private TableView<AuctionListDTO> auctionTable;

    @FXML
    private TableColumn<AuctionListDTO, String> idColumn;

    @FXML
    private TableColumn<AuctionListDTO, String> nameColumn;

    @FXML
    private TableColumn<AuctionListDTO, Double> priceColumn;

    @FXML
    private TableColumn<AuctionListDTO, Status> statusColumn;

    @FXML
    private Label messageLabel;

    private final AuctionService auctionService = new AuctionService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));

        loadAuctions();
    }

    private void loadAuctions() {
        auctionTable.setItems(FXCollections.observableArrayList(auctionService.getActiveAuctions()));
        messageLabel.setText(auctionTable.getItems().isEmpty() ? "No active auctions found." : "");
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        AuctionListDTO selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            messageLabel.setText("Please select an auction first.");
            return;
        }

        messageLabel.setText("Selected auction: " + selectedAuction.getAuctionId());
    }

    @FXML
    private void handleJoinBidding(ActionEvent event) {
        AuctionListDTO selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            messageLabel.setText("Please select an auction first.");
            return;
        }

        messageLabel.setText("Join bidding: " + selectedAuction.getName());
    }
}
