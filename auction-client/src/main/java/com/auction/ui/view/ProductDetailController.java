package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.auction.application.service.AuctionService;
import com.auction.application.service.PeriodicUpdateService;
import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProductDetailController {
    private AuctionListDTO currentAuction;
    private final AuctionService auctionService = new AuctionService();

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

        updateDetailDisplay(currentAuction);
        startPeriodicStatusRefresh();
    }

    /**
     * Cập nhật lại display với dữ liệu auction
     */
    private void updateDetailDisplay(AuctionListDTO auction) {
        productNameLabel.setText(auction.getName());
        categoryLabel.setText("Loại sản phẩm: " + safeText(auction.getItemType()));
        startingPriceLabel.setText("$" + String.format("%.2f", auction.getCurrentPrice()));
        itemTypeLabel.setText(hasText(auction.getItemType()) ? auction.getItemType() : "N/A");
        statusLabel.setText(auction.getAuctionStatus() != null ? auction.getAuctionStatus().name() : "N/A");
        startTimeLabel.setText(hasText(auction.getStartDateTime()) ? auction.getStartDateTime() : "Chưa có thời gian bắt đầu");
        endTimeLabel.setText(hasText(auction.getEndDateTime())
                ? auction.getEndDateTime()
                : "Chưa có thời gian kết thúc");

        conditionLabel.setText(hasText(auction.getCondition()) ? auction.getCondition() : "N/A");
        specsLabel.setText(hasText(auction.getDescription()) ? auction.getDescription() : "Không có mô tả chi tiết.");
        warrantyLabel.setText(hasText(auction.getWarranty()) ? auction.getWarranty() : "Không có thông tin thêm.");
    }

    /**
     * Bắt đầu refresh trạng thái định kỳ mỗi 10 giây
     */
    private void startPeriodicStatusRefresh() {
        if (currentAuction == null) {
            return;
        }

        PeriodicUpdateService.getInstance().startPeriodicStatusRefresh(() -> {
            if (currentAuction != null) {
                AuctionListDTO updated = auctionService.getAuctionById(currentAuction.getAuctionId());
                if (updated != null) {
                    Platform.runLater(() -> {
                        currentAuction = updated;
                        updateDetailDisplay(updated);
                        System.out.println("[ProductDetailController] Status refreshed to: " + updated.getAuctionStatus());
                    });
                }
            }
        });
    }

    /**
     * Dừng periodic status refresh khi rời khỏi ProductDetail
     */
    private void stopPeriodicStatusRefresh() {
        PeriodicUpdateService.getInstance().stopPeriodicStatusRefresh();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        stopPeriodicStatusRefresh();
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "UET Auction System");
    }

    @FXML
    private void handleJoinAuction(ActionEvent event) {
        if (currentAuction == null) {
            return;
        }

        try {
            stopPeriodicStatusRefresh();
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
            stage.setScene(new Scene(root, 1280, 800));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteProduct(ActionEvent event) {
        stopPeriodicStatusRefresh();
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

