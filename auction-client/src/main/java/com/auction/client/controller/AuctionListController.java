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
        idColumn.setText("Lo\u1ea1i s\u1ea3n ph\u1ea9m");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
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
                messageLabel.setText("Kh\u00f4ng th\u1ec3 t\u1ea3i danh s\u00e1ch \u0111\u1ea5u gi\u00e1.");
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
            accountBalanceLabel.setText("V\u00ed: $" + String.format("%.2f", balance));

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
            messageLabel.setText("\u0110\u00e3 l\u00e0m m\u1edbi danh s\u00e1ch \u0111\u1ea5u gi\u00e1.");
        }
    }

    @FXML
    private void handleViewDetail() {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ProductDataManager.getInstance().setSelectedAuction(selected);
            switchScene("/fxml/ProductDetail.fxml", "Chi ti\u1ebft s\u1ea3n ph\u1ea9m");
        } else {
            if (messageLabel != null) messageLabel.setText("Vui l\u00f2ng ch\u1ecdn s\u1ea3n ph\u1ea9m tr\u00ean b\u1ea3ng!");
        }
    }

    @FXML
    private void handleJoinBidding() {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getAuctionStatus() != Status.RUNNING) {
                if (messageLabel != null) {
                    messageLabel.setText("Phi\u00ean \u0111\u1ea5u gi\u00e1 ch\u01b0a b\u1eaft \u0111\u1ea7u. Vui l\u00f2ng \u0111\u1ee3i tr\u1ea1ng th\u00e1i RUNNING.");
                    messageLabel.setStyle("-fx-text-fill: #ff9800;");
                }
                return;
            }

            if (ProductDataManager.getInstance().isEnded(selected.getAuctionId())) {
                if (messageLabel != null) {
                    messageLabel.setText("Phi\u00ean \u0111\u1ea5u gi\u00e1 '" + selected.getName() + "' \u0111\u00e3 k\u1ebft th\u00fac!");
                    messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
                }
                auctionTable.refresh();
                return;
            }

            ProductDataManager.getInstance().setSelectedAuction(selected);

            try {
                URL fxmlUrl = getClass().getResource("/fxml/LiveBidding.fxml");
                if (fxmlUrl == null) {
                    throw new IllegalStateException("Kh\u00f4ng t\u00ecm th\u1ea5y file FXML: /fxml/LiveBidding.fxml");
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent root = loader.load();

                LiveBiddingController controller = loader.getController();

                Product productModel = new Product(
                        selected.getAuctionId(),
                        selected.getItemType(),
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

                stage.setTitle("\u0110\u1ea5u gi\u00e1 tr\u1ef1c ti\u1ebfp");
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {
                throw new IllegalStateException("Kh\u00f4ng th\u1ec3 m\u1edf m\u00e0n h\u00ecnh \u0111\u1ea5u gi\u00e1 tr\u1ef1c ti\u1ebfp.", e);
            }
        } else {
            if (messageLabel != null) messageLabel.setText("Vui l\u00f2ng ch\u1ecdn phi\u00ean \u0111\u1ec3 tham gia!");
        }
    }

    @FXML
    private void handleSidebarProducts() {
        switchScene("/fxml/ProductManagement.fxml", "Qu\u1ea3n l\u00fd s\u1ea3n ph\u1ea9m");
    }

    @FXML
    private void handleSidebarBidding() {
        handleJoinBidding();
    }

    @FXML
    private void handleSidebarAccount() {
        if (messageLabel != null) {
            messageLabel.setText("S\u1ed1 d\u01b0 v\u00ed: $" + String.format("%.2f", ProductDataManager.getInstance().getUserBalance()));
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalStateException("Kh\u00f4ng t\u00ecm th\u1ea5y file FXML: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            Scene scene = new Scene(root, 1040, 660);
            scene.getStylesheets().add(requireStylesheet());
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new IllegalStateException("Kh\u00f4ng th\u1ec3 chuy\u1ec3n sang m\u00e0n h\u00ecnh: " + title, e);
        }
    }

    private String requireStylesheet() {
        String path = "/css/style.css";
        URL cssUrl = getClass().getResource(path);
        if (cssUrl == null) {
            throw new IllegalStateException("Kh\u00f4ng t\u00ecm th\u1ea5y stylesheet: " + path);
        }
        return cssUrl.toExternalForm();
    }
}
