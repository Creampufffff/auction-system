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

        // [MỚI] Ép TableView vẽ lại dữ liệu mới nhất (giá, trạng thái) từ Manager
        auctionTable.refresh();
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

    @FXML
    private void handleJoinBidding(ActionEvent event) {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {

            // Kiểm tra kết thúc: Nếu Global Timer báo hết giờ, không cho vào phòng
            if (ProductDataManager.getInstance().isEnded(selected.getAuctionId())) {
                if (messageLabel != null) {
                    messageLabel.setText("Phiên đấu giá '" + selected.getName() + "' đã kết thúc!");
                    messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
                }
                // Refresh lại bảng để cập nhật trạng thái FINISHED nếu cần
                auctionTable.refresh();
                return;
            }

            ProductDataManager.getInstance().setSelectedAuction(selected);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LiveBidding.fxml"));
                Parent root = loader.load();

                LiveBiddingController controller = loader.getController();

                // Cập nhật Model Product với giá mới nhất từ Manager trước khi vào phòng
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