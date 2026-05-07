package com.auction.client.controller;

import com.app.common.dto.AuctionListDTO;
import com.auction.client.model.ProductDataManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProductDetailController {

    // Các ID này phải khớp y hệt với fx:id trong file ProductDetail.fxml của bạn
    @FXML private Label productNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label startingPriceLabel; // Khớp với FXML
    @FXML private Label conditionLabel;
    @FXML private Label specsLabel;         // Khớp với FXML (thay cho description)
    @FXML private Label warrantyLabel;
    @FXML private Label endTimeLabel;

    @FXML
    public void initialize() {
        // Lấy dữ liệu từ Manager
        AuctionListDTO selected = ProductDataManager.getInstance().getSelectedAuction();

        if (selected != null) {
            // Đổ dữ liệu vào các Label
            productNameLabel.setText(selected.getName());
            categoryLabel.setText("Mã phiên: " + selected.getAuctionId());
            startingPriceLabel.setText("$" + String.format("%.2f", selected.getCurrentPrice()));

            // Chi tiết kỹ thuật
            conditionLabel.setText(selected.getCondition() != null ? selected.getCondition() : "N/A");
            specsLabel.setText(selected.getDescription() != null ? selected.getDescription() : "Không có mô tả chi tiết.");
            warrantyLabel.setText(selected.getWarranty() != null ? selected.getWarranty() : "Không có thông tin.");

            // Trạng thái thời gian (Ví dụ)
            endTimeLabel.setText("Phiên đấu giá đang diễn ra");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene("/fxml/AuctionList.fxml", "UET Auction System");
    }

    @FXML
    private void handleJoinAuction(ActionEvent event) {
        switchScene("/fxml/LiveBidding.fxml", "Đấu giá trực tiếp");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            // Lấy stage hiện tại từ bất kỳ Label nào có sẵn
            Stage stage = (Stage) productNameLabel.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1040, 660));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Không thể load file FXML: " + fxmlPath);
        }
    }
}