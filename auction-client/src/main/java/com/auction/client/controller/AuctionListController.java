package com.auction.client.controller;

import com.app.common.dto.AuctionListDTO;
import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import com.auction.client.service.AuctionService;
import javafx.application.Platform;
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

    @FXML private TableView<AuctionListDTO> auctionTable;
    @FXML private TableColumn<AuctionListDTO, String> idColumn;
    @FXML private TableColumn<AuctionListDTO, String> nameColumn;
    @FXML private TableColumn<AuctionListDTO, Double> priceColumn;
    @FXML private TableColumn<AuctionListDTO, Object> statusColumn;
    @FXML private Label messageLabel;

    // [MỚI] Khai báo Label ví tiền
    @FXML private Label accountBalanceLabel;

    private final AuctionService auctionService = new AuctionService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));

        loadAuctions();

        auctionTable.refresh();

        Platform.runLater(() -> {
            updateUIWithBalance();
        });
    }

    private void loadAuctions() {
        if (ProductDataManager.getInstance().getServerAuctionList().isEmpty()) {
            ProductDataManager.getInstance().getServerAuctionList().addAll(auctionService.getActiveAuctions());
        }
        auctionTable.setItems(ProductDataManager.getInstance().getServerAuctionList());
        auctionTable.refresh();
    }

    // [MỚI] Hàm cập nhật tiền lên UI
    private void updateUIWithBalance() {
        if (accountBalanceLabel != null) {
            double balance = ProductDataManager.getInstance().getUserBalance();
            accountBalanceLabel.setText("Ví: $" + String.format("%.2f", balance));

            // Tweak: Đổi màu nếu sắp hết tiền
            if (balance < 100) {
                accountBalanceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;");
            }
        } else {
            System.out.println("DEBUG: accountBalanceLabel vẫn đang bị null!");
        }
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        loadAuctions();
        updateUIWithBalance();
        if (messageLabel != null) {
            messageLabel.setText("Đã làm mới danh sách đấu giá.");
        }
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ProductDataManager.getInstance().setSelectedAuction(selected);
            switchScene("/fxml/ProductDetail.fxml", "Chi tiết sản phẩm");
        } else {
            if (messageLabel != null) messageLabel.setText("Vui lòng chọn sản phẩm trên bảng!");
        }
    }

    @FXML
    private void handleJoinBidding(ActionEvent event) {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (ProductDataManager.getInstance().isEnded(selected.getAuctionId())) {
                if (messageLabel != null) {
                    messageLabel.setText("Phiên đấu giá '" + selected.getName() + "' đã kết thúc!");
                    messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
                }
                auctionTable.refresh();
                return;
            }

            ProductDataManager.getInstance().setSelectedAuction(selected);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LiveBidding.fxml"));
                Parent root = loader.load();

                LiveBiddingController controller = loader.getController();

                Product productModel = new Product(
                        selected.getAuctionId(),
                        selected.getName(),
                        ProductDataManager.getInstance().getCurrentPrice(selected.getAuctionId(), selected.getCurrentPrice()),
                        selected.getAuctionStatus().toString(),
                        "New",
                        "No description",
                        "No warranty"
                );

                controller.setProduct(productModel);

                Stage stage = (Stage) auctionTable.getScene().getWindow();
                Scene scene = new Scene(root, 1040, 660);

                String css = getClass().getResource("/css/style.css").toExternalForm();
                scene.getStylesheets().add(css);

                stage.setTitle("Đấu giá trực tiếp");
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (messageLabel != null) messageLabel.setText("Vui lòng chọn phiên để tham gia!");
        }
    }

    @FXML
    private void handleSidebarProducts(ActionEvent event) {
        switchScene("/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
    }

    @FXML
    private void handleSidebarBidding(ActionEvent event) {
        handleJoinBidding(event);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        if (messageLabel != null) {
            messageLabel.setText("Số dư ví: $" + String.format("%.2f", ProductDataManager.getInstance().getUserBalance()));
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            Scene scene = new Scene(root, 1040, 660);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}