package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.BidHistoryDTO;
import com.auction.application.service.AuctionService;
import com.auction.application.service.PeriodicUpdateService;
import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProductDetailController {
    private AuctionListDTO currentAuction;
    private final AuctionService auctionService = new AuctionService();

    @FXML private Label productNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label itemTypeLabel;
    @FXML private Label statusLabel;
    @FXML private Label startTimeLabel;
    @FXML private Label conditionTitleLabel;
    @FXML private Label conditionLabel;
    @FXML private Label specsLabel;
    @FXML private Label typeSpecificTitleLabel;
    @FXML private Label warrantyLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label winnerTitleLabel;
    @FXML private Label winnerLabel;
    @FXML private Button joinAuctionButton;
    @FXML private Button deleteProductButton;
    @FXML private Separator detailActionSeparator;
    @FXML private HBox detailActionBar;
    @FXML private ImageView productImageView;
    @FXML private Label productImagePlaceholder;

    @FXML
    public void initialize() {
        AuctionListDTO selected = ProductDataManager.getInstance().getSelectedAuction();
        if (selected == null) {
            return;
        }

        currentAuction = selected;
        ProductDataManager.getInstance().setSelectedAuction(currentAuction);

        configureActionsForRole();
        updateDetailDisplay(currentAuction);
        loadDetailFromServerAsync(currentAuction.getAuctionId());
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
        updateWinnerDisplay(auction);
        startTimeLabel.setText(hasText(auction.getStartDateTime()) ? auction.getStartDateTime() : "Chưa có thời gian bắt đầu");
        endTimeLabel.setText(hasText(auction.getEndDateTime())
                ? auction.getEndDateTime()
                : "Chưa có thời gian kết thúc");

        specsLabel.setText(hasText(auction.getDescription()) ? auction.getDescription() : "Không có mô tả chi tiết.");
        updateTypeSpecificDisplay(auction);
        updateProductImage(auction.getImageBlob());
    }

    private void updateTypeSpecificDisplay(AuctionListDTO auction) {
        String itemType = normalizeItemType(auction.getItemType());
        boolean isArt = "ART".equals(itemType);

        if (conditionTitleLabel != null) {
            conditionTitleLabel.setVisible(!isArt);
            conditionTitleLabel.setManaged(!isArt);
        }
        if (conditionLabel != null) {
            conditionLabel.setVisible(!isArt);
            conditionLabel.setManaged(!isArt);
            conditionLabel.setText(hasText(auction.getCondition()) ? auction.getCondition() : "N/A");
        }

        if (typeSpecificTitleLabel != null) {
            typeSpecificTitleLabel.setText(typeSpecificLabelText(itemType));
        }
        warrantyLabel.setText(typeSpecificValueText(auction, itemType));
    }

    private String normalizeItemType(String itemType) {
        return hasText(itemType) ? itemType.trim().toUpperCase() : "";
    }

    private String typeSpecificLabelText(String itemType) {
        return switch (itemType) {
            case "ART" -> "Tác giả:";
            case "VEHICLE" -> "Hãng xe:";
            default -> "Bảo hành (tháng):";
        };
    }

    private String typeSpecificValueText(AuctionListDTO auction, String itemType) {
        if ("ART".equals(itemType)) {
            return hasText(auction.getCondition()) ? auction.getCondition() : "Không có thông tin tác giả.";
        }
        if ("VEHICLE".equals(itemType)) {
            return hasText(auction.getWarranty()) ? auction.getWarranty() : "Không có thông tin hãng xe.";
        }
        return hasText(auction.getWarranty()) ? auction.getWarranty() : "Không có thông tin bảo hành.";
    }

    private void configureActionsForRole() {
        boolean isSeller = SessionManager.hasRole("Seller");
        boolean canDeleteProduct = isSeller && isOpenedFromProductManagement();
        if (joinAuctionButton != null) {
            joinAuctionButton.setVisible(!isSeller);
            joinAuctionButton.setManaged(!isSeller);
        }
        if (deleteProductButton != null) {
            deleteProductButton.setVisible(canDeleteProduct);
            deleteProductButton.setManaged(canDeleteProduct);
        }
        if (detailActionSeparator != null) {
            detailActionSeparator.setVisible(canDeleteProduct);
            detailActionSeparator.setManaged(canDeleteProduct);
        }
        if (detailActionBar != null) {
            detailActionBar.setVisible(canDeleteProduct);
            detailActionBar.setManaged(canDeleteProduct);
        }
    }

    private boolean isOpenedFromProductManagement() {
        return "/fxml/ProductManagement.fxml".equals(ProductDataManager.getInstance().getProductDetailReturnPath());
    }

    private void updateWinnerDisplay(AuctionListDTO auction) {
        if (winnerTitleLabel == null || winnerLabel == null) {
            return;
        }

        boolean isSeller = SessionManager.hasRole("Seller");
        winnerTitleLabel.setVisible(isSeller);
        winnerTitleLabel.setManaged(isSeller);
        winnerLabel.setVisible(isSeller);
        winnerLabel.setManaged(isSeller);
        if (!isSeller) {
            return;
        }

        boolean isFinished = auction.getAuctionStatus() != null
                && "FINISHED".equals(auction.getAuctionStatus().name());
        winnerTitleLabel.setText(isFinished ? "Người thắng:" : "Người đang dẫn đầu:");
        String bidderUsername = getCachedOrDtoHighestBidder(auction);
        winnerLabel.setText(hasText(bidderUsername) ? bidderUsername : "Đang tải...");
    }

    private String getCachedOrDtoHighestBidder(AuctionListDTO auction) {
        ProductDataManager manager = ProductDataManager.getInstance();
        if (manager.hasLeadingUser(auction.getAuctionId())) {
            return manager.getLeadingUser(auction.getAuctionId(), null);
        }

        String bidderUsername = auction.getHighestBidderUsername();
        if (hasText(bidderUsername)) {
            manager.setLeadingUser(auction.getAuctionId(), bidderUsername);
        }
        return bidderUsername;
    }

    private void loadDetailFromServerAsync(String auctionId) {
        if (!hasText(auctionId)) {
            return;
        }

        CompletableFuture
                .supplyAsync(() -> {
                    AuctionListDTO detail = auctionService.getAuctionById(auctionId);
                    if (detail == null) {
                        detail = currentAuction;
                    }
                    if (SessionManager.hasRole("Seller") && detail != null && !hasText(getCachedOrDtoHighestBidder(detail))) {
                        String bidderUsername = resolveHighestBidderFromHistory(auctionId);
                        detail.setHighestBidderUsername(bidderUsername);
                        ProductDataManager.getInstance().setLeadingUser(auctionId, bidderUsername);
                    }
                    return detail;
                })
                .thenAccept(detail -> {
                    if (detail == null) {
                        return;
                    }
                    Platform.runLater(() -> {
                        currentAuction = detail;
                        ProductDataManager.getInstance().setSelectedAuction(currentAuction);
                        updateDetailDisplay(currentAuction);
                    });
                })
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        if (winnerLabel != null) {
                            winnerLabel.setText("Chưa có");
                        }
                    });
                    return null;
                });
    }

    private String resolveHighestBidderFromHistory(String auctionId) {
        if (!hasText(auctionId)) {
            return null;
        }

        List<BidHistoryDTO> bids = auctionService.getBidHistory(auctionId);
        if (bids == null || bids.isEmpty()) {
            return null;
        }

        String bidderUsername = bids.get(0).getBidderUsername();
        ProductDataManager.getInstance().setLeadingUser(auctionId, bidderUsername);
        return bidderUsername;
    }

    private void updateProductImage(byte[] imageBlob) {
        if (imageBlob == null || imageBlob.length == 0) {
            productImageView.setImage(null);
            productImagePlaceholder.setVisible(true);
            return;
        }

        Image image = new Image(new ByteArrayInputStream(imageBlob));
        if (image.isError()) {
            productImageView.setImage(null);
            productImagePlaceholder.setVisible(true);
            productImagePlaceholder.setText("[ KHÔNG THỂ HIỂN THỊ ẢNH ]");
            return;
        }

        productImageView.setImage(image);
        productImagePlaceholder.setVisible(false);
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
                        if (!hasText(updated.getHighestBidderUsername())) {
                            updated.setHighestBidderUsername(currentAuction.getHighestBidderUsername());
                        }
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
        ProductDataManager manager = ProductDataManager.getInstance();
        NavigationService.getInstance().navigateTo(
                manager.getProductDetailReturnPath(),
                manager.getProductDetailReturnTitle(),
                1280,
                800
        );
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

