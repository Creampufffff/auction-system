package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import com.auction.ui.navigation.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProductDetailController {
    private AuctionListDTO currentAuction;

    @FXML private Label productNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label itemTypeLabel;
    @FXML private Label statusLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label conditionLabel;
    @FXML private Label specsLabel;
    @FXML private Label warrantyLabel;
    @FXML private Label endTimeLabel;

    @FXML
    public void initialize() {
        AuctionListDTO selected = ProductDataManager.getInstance().getSelectedAuction();
        if (selected == null) {
            return;
        }

        currentAuction = selected;
        ProductDataManager.getInstance().setSelectedAuction(currentAuction);

        productNameLabel.setText(currentAuction.getName());
        categoryLabel.setText("Loại sản phẩm: " + safeText(currentAuction.getItemType()));
        startingPriceLabel.setText("$" + String.format("%.2f", currentAuction.getCurrentPrice()));
        itemTypeLabel.setText(hasText(currentAuction.getItemType()) ? currentAuction.getItemType() : "N/A");
        statusLabel.setText(currentAuction.getAuctionStatus() != null ? currentAuction.getAuctionStatus().name() : "N/A");
        startTimeLabel.setText(hasText(currentAuction.getStartDateTime()) ? currentAuction.getStartDateTime() : "Chưa có thời gian bắt đầu");
        endTimeLabel.setText(hasText(currentAuction.getEndDateTime())
                ? currentAuction.getEndDateTime()
                : "Chưa có thời gian kết thúc");

        conditionLabel.setText(hasText(currentAuction.getCondition()) ? currentAuction.getCondition() : "N/A");
        specsLabel.setText(hasText(currentAuction.getDescription()) ? currentAuction.getDescription() : "Không có mô tả chi tiết.");
        warrantyLabel.setText(hasText(currentAuction.getWarranty()) ? currentAuction.getWarranty() : "Không có thông tin thêm.");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "UET Auction System");
    }

    @FXML
    private void handleJoinAuction(ActionEvent event) {
        if (currentAuction == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LiveBidding.fxml"));
            Parent root = loader.load();
            LiveBiddingController controller = loader.getController();
            controller.setProduct(new Product(
                    currentAuction.getAuctionId(),
                    currentAuction.getItemType(),
                    currentAuction.getName(),
                    ProductDataManager.getInstance().getCurrentPrice(currentAuction.getAuctionId(), currentAuction.getCurrentPrice()),
                    currentAuction.getAuctionStatus() != null ? currentAuction.getAuctionStatus().toString() : "UNKNOWN",
                    currentAuction.getCondition(),
                    currentAuction.getDescription(),
                    currentAuction.getWarranty(),
                    currentAuction.getEndDateTime()
            ));

            Stage stage = (Stage) productNameLabel.getScene().getWindow();
            stage.setTitle("Dau gia truc tiep");
            stage.setScene(new Scene(root, 1040, 660));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteProduct(ActionEvent event) {
        AuctionListDTO selected = ProductDataManager.getInstance().getSelectedAuction();
        if (selected != null) {
            ProductDataManager.getInstance().deleteProductAndAuction(selected.getAuctionId());
            handleBack(event);
        }
    }


    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeText(String value) {
        return hasText(value) ? value : "N/A";
    }

}

