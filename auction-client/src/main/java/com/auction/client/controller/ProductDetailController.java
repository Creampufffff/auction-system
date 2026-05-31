package com.auction.client.controller;

import com.app.common.dto.AuctionListDTO;
import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import com.auction.client.service.AuctionService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProductDetailController {
    private final AuctionService auctionService = new AuctionService();
    private AuctionListDTO currentAuction;

    @FXML private Label productNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label conditionTitleLabel;
    @FXML private Label conditionLabel;
    @FXML private Label specsTitleLabel;
    @FXML private Label specsLabel;
    @FXML private Label warrantyTitleLabel;
    @FXML private Label warrantyLabel;
    @FXML private Label endTimeLabel;

    @FXML
    public void initialize() {
        AuctionListDTO selected = ProductDataManager.getInstance().getSelectedAuction();
        if (selected == null) {
            return;
        }

        AuctionListDTO serverAuction = auctionService.getAuctionById(selected.getAuctionId());
        currentAuction = serverAuction != null ? serverAuction : selected;
        ProductDataManager.getInstance().setSelectedAuction(currentAuction);

        productNameLabel.setText(currentAuction.getName());
        categoryLabel.setText("Ma phien: " + currentAuction.getAuctionId());
        startingPriceLabel.setText("$" + String.format("%.2f", currentAuction.getCurrentPrice()));
        applyTechnicalDetails();
        endTimeLabel.setText(hasText(currentAuction.getEndDateTime())
                ? "Ket thuc: " + currentAuction.getEndDateTime()
                : "Chua co thoi gian ket thuc");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene("/fxml/AuctionList.fxml", "UET Auction System");
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
                    currentAuction.getAuctionStatus().toString(),
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

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) productNameLabel.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1040, 660));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Khong the load file FXML: " + fxmlPath);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void applyTechnicalDetails() {
        String itemType = currentAuction.getItemType() == null ? "ART" : currentAuction.getItemType();
        boolean isArt = "ART".equalsIgnoreCase(itemType);
        boolean isElectronics = "ELECTRONICS".equalsIgnoreCase(itemType);
        boolean isVehicle = "VEHICLE".equalsIgnoreCase(itemType);

        conditionTitleLabel.setText(isArt ? "T\u00e1c gi\u1ea3:" : "T\u00ecnh tr\u1ea1ng:");
        conditionLabel.setText(hasText(currentAuction.getCondition()) ? currentAuction.getCondition() : "N/A");

        specsTitleLabel.setText("M\u00f4 t\u1ea3:");
        specsLabel.setText(hasText(currentAuction.getDescription()) ? currentAuction.getDescription() : "Kh\u00f4ng c\u00f3 m\u00f4 t\u1ea3 chi ti\u1ebft.");

        boolean showWarranty = !isArt && hasText(currentAuction.getWarranty());
        warrantyTitleLabel.setVisible(showWarranty);
        warrantyTitleLabel.setManaged(showWarranty);
        warrantyLabel.setVisible(showWarranty);
        warrantyLabel.setManaged(showWarranty);

        if (showWarranty) {
            warrantyTitleLabel.setText(isElectronics ? "B\u1ea3o h\u00e0nh:" : isVehicle ? "H\u00e3ng xe:" : "Th\u00f4ng tin:");
            warrantyLabel.setText(currentAuction.getWarranty());
        }
    }
}
