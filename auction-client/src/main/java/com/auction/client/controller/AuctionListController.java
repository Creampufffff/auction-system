package com.auction.client.controller;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import com.auction.client.service.AuctionService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

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
        messageLabel.setText(auctionTable.getItems().isEmpty() ? "Không có phiên đấu giá nào đang hoạt động." : "");
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        AuctionListDTO selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            messageLabel.setText("Vui lòng chọn một phiên đấu giá trước.");
            return;
        }

        switchScene("/fxml/ProductDetail.fxml", "Chi tiết sản phẩm");
    }

    @FXML
    private void handleJoinBidding(ActionEvent event) {
        AuctionListDTO selectedAuction = auctionTable.getSelectionModel().getSelectedItem();
        if (selectedAuction == null) {
            messageLabel.setText("Vui lòng chọn một phiên đấu giá trước.");
            return;
        }

        switchScene("/fxml/LiveBidding.fxml", "Đấu giá trực tiếp");
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        loadAuctions();
    }

    @FXML
    private void handleSidebarBidding(ActionEvent event) {
        switchScene("/fxml/LiveBidding.fxml", "Đấu giá trực tiếp");
    }

    @FXML
    private void handleSidebarProducts(ActionEvent event) {
        switchScene("/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        messageLabel.setText("Màn hình tài khoản chưa sẵn sàng.");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 980, 640));
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Không thể mở màn hình: " + title);
        }
    }
}
