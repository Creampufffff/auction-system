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

    private final AuctionService auctionService = new AuctionService();

    @FXML
    public void initialize() {
        // Cấu hình các cột cho TableView
        idColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));

        loadAuctions();
    }

    private void loadAuctions() {
        // Nếu danh sách dùng chung trống, lấy dữ liệu từ service
        if (ProductDataManager.getInstance().getServerAuctionList().isEmpty()) {
            ProductDataManager.getInstance().getServerAuctionList().addAll(auctionService.getActiveAuctions());
        }

        auctionTable.setItems(ProductDataManager.getInstance().getServerAuctionList());
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        loadAuctions();
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

    // --- FIX HÀM THAM GIA ĐẤU GIÁ: CHUYỂN DỮ LIỆU SANG LIVE BIDDING ---
    @FXML
    private void handleJoinBidding(ActionEvent event) {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ProductDataManager.getInstance().setSelectedAuction(selected);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LiveBidding.fxml"));
                Parent root = loader.load();

                // Lấy controller của LiveBidding để truyền sản phẩm vào
                LiveBiddingController controller = loader.getController();

                // [MỚI] Chuyển đổi sang Model Product: Lấy giá cao nhất đã lưu thay vì giá mặc định
                Product productModel = new Product(
                        selected.getAuctionId(),
                        selected.getName(),
                        // Ưu tiên lấy giá từ Manager (giá đã bid), nếu chưa có thì lấy giá gốc từ DTO
                        ProductDataManager.getInstance().getCurrentPrice(selected.getAuctionId(), selected.getCurrentPrice()),
                        selected.getAuctionStatus().toString(),
                        "New",
                        "No description",
                        "No warranty"
                );

                controller.setProduct(productModel);

                Stage stage = (Stage) auctionTable.getScene().getWindow();
                Scene scene = new Scene(root, 1040, 660);

                // Nạp CSS để đảm bảo UI đồng nhất (nút Back, màu sắc...)
                String css = getClass().getResource("/css/style.css").toExternalForm();
                scene.getStylesheets().add(css);

                stage.setTitle("Đấu giá trực tiếp");
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
                if (messageLabel != null) messageLabel.setText("Lỗi khởi tạo phòng đấu giá!");
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
        // Gọi lại logic JoinBidding để đảm bảo quy trình lấy dữ liệu và chuyển cảnh chuẩn
        handleJoinBidding(event);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        if (messageLabel != null) messageLabel.setText("Tài khoản chưa sẵn sàng.");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            Scene scene = new Scene(root, 1040, 660);

            // Nạp CSS cho mọi màn hình để giữ UI chuẩn
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            if (messageLabel != null) messageLabel.setText("Lỗi chuyển cảnh: " + fxmlPath);
        }
    }
}