package com.auction.client.controller;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import com.auction.client.service.AuctionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.net.URL;

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

        Platform.runLater(this::updateUIWithBalance);
    }

    private void loadAuctions() {
        try {
            ProductDataManager.getInstance().getServerAuctionList().setAll(auctionService.getActiveAuctions());
        } catch (IllegalStateException ex) {
            if (messageLabel != null) {
                messageLabel.setText("Không thể tải danh sách đấu giá.");
            }
            return;
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
    private void handleCurrentAuctions() {
        loadAuctions();
        updateUIWithBalance();
        if (messageLabel != null) {
            messageLabel.setText("Đã làm mới danh sách đấu giá.");
        }
    }

    @FXML
    private void handleViewDetail() {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ProductDataManager.getInstance().setSelectedAuction(selected);
            switchScene("/fxml/ProductDetail.fxml", "Chi tiết sản phẩm");
        } else {
            if (messageLabel != null) messageLabel.setText("Vui lòng chọn sản phẩm trên bảng!");
        }
    }

    @FXML
    private void handleJoinBidding() {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getAuctionStatus() != Status.RUNNING) {
                if (messageLabel != null) {
                    messageLabel.setText("Phiên đấu giá chưa bắt đầu. Vui lòng đợi trạng thái RUNNING.");
                    messageLabel.setStyle("-fx-text-fill: #ff9800;");
                }
                return;
            }

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
                URL fxmlUrl = getClass().getResource("/fxml/LiveBidding.fxml");
                if (fxmlUrl == null) {
                    throw new IllegalStateException("Không tìm thấy file FXML: /fxml/LiveBidding.fxml");
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
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

                    String css = requireStylesheet();
                scene.getStylesheets().add(css);

                stage.setTitle("Đấu giá trực tiếp");
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {
                throw new IllegalStateException("Không thể mở màn hình đấu giá trực tiếp.", e);
            }
        } else {
            if (messageLabel != null) messageLabel.setText("Vui lòng chọn phiên để tham gia!");
        }
    }

    @FXML
    private void handleSidebarProducts() {
        switchScene("/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
    }

    @FXML
    private void handleSidebarBidding() {
        handleJoinBidding();
    }

    @FXML
    private void handleSidebarAccount() {
        if (messageLabel != null) {
            messageLabel.setText("Số dư ví: $" + String.format("%.2f", ProductDataManager.getInstance().getUserBalance()));
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalStateException("Không tìm thấy file FXML: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            Scene scene = new Scene(root, 1040, 660);
            scene.getStylesheets().add(requireStylesheet());
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể chuyển sang màn hình: " + title, e);
        }
    }

    private String requireStylesheet() {
        String path = "/css/style.css";
        URL cssUrl = getClass().getResource(path);
        if (cssUrl == null) {
            throw new IllegalStateException("Không tìm thấy stylesheet: " + path);
        }
        return cssUrl.toExternalForm();
    }
}