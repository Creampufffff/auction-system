package com.auction.ui.view;

import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.BidHistoryDTO;
import com.auction.application.service.AccountService;
import com.auction.domain.model.ProductDataManager;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BidHistoryController {

    @FXML private Label sidebarRoleLabel;
    @FXML private Label sidebarBalanceLabel;
    @FXML private VBox bidHistoryCard;
    @FXML private Button productsSidebarButton;
    @FXML private Button bidHistorySidebarButton;
    @FXML private TableView<BidHistoryDTO> bidHistoryTable;
    @FXML private TableColumn<BidHistoryDTO, String> colBidId;
    @FXML private TableColumn<BidHistoryDTO, String> colAuctionId;
    @FXML private TableColumn<BidHistoryDTO, String> colBidTime;
    @FXML private TableColumn<BidHistoryDTO, Double> colBidAmount;
    @FXML private Label messageLabel;

    private final AccountService accountService = new AccountService();

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole("Bidder") && bidHistoryCard != null) {
            bidHistoryCard.setVisible(false);
            bidHistoryCard.setManaged(false);
        }

        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText("Role: " + SessionManager.getCurrentUserRole());
        }
        configureSidebarForRole();

        configureTable();
        Platform.runLater(() -> {
            loadBalance();
            if (SessionManager.hasRole("Bidder")) {
                loadBidHistory();
            }
        });
    }

    private void configureSidebarForRole() {
        if (productsSidebarButton != null) {
            boolean isSeller = SessionManager.hasRole("Seller");
            productsSidebarButton.setVisible(isSeller);
            productsSidebarButton.setManaged(isSeller);
        }
        if (bidHistorySidebarButton != null) {
            boolean isBidder = SessionManager.hasRole("Bidder");
            bidHistorySidebarButton.setVisible(isBidder);
            bidHistorySidebarButton.setManaged(isBidder);
        }
    }

    private void configureTable() {
        if (bidHistoryTable == null) {
            return;
        }

        colBidId.setText("Loại sản phẩm");
        colAuctionId.setText("Tên sản phẩm");
        colBidId.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colBidAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colBidTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));

        if (!bidHistoryTable.getStyleClass().contains("auction-table")) {
            bidHistoryTable.getStyleClass().add("auction-table");
        }
        if (!bidHistoryTable.getStyleClass().contains("bid-history-table")) {
            bidHistoryTable.getStyleClass().add("bid-history-table");
        }
        bidHistoryTable.setFixedCellSize(48);

        colBidId.getStyleClass().add("centered-table-column");
        colAuctionId.getStyleClass().add("centered-table-column");
        colBidAmount.getStyleClass().add("centered-table-column");
        colBidTime.getStyleClass().add("centered-table-column");

        colBidId.setCellFactory(column -> centeredTextCell());
        colAuctionId.setCellFactory(column -> centeredTextCell());
        colBidTime.setCellFactory(column -> centeredTimeCell());
        colBidAmount.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                Label label = new Label("$" + String.format("%.2f", amount));
                label.setMaxWidth(Double.MAX_VALUE);
                label.setAlignment(javafx.geometry.Pos.CENTER);
                label.getStyleClass().add("bid-history-amount-label");
                setGraphic(centeredContent(label));
            }
        });
    }

    private TableCell<BidHistoryDTO, String> centeredTextCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                Label label = new Label(text);
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setAlignment(javafx.geometry.Pos.CENTER);
                label.getStyleClass().add("auction-main-cell-label");
                setGraphic(centeredContent(label));
            }
        };
    }

    private TableCell<BidHistoryDTO, String> centeredTimeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                Label label = new Label(time);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setAlignment(javafx.geometry.Pos.CENTER);
                label.getStyleClass().add("bid-history-time-label");
                setGraphic(centeredContent(label));
            }
        };
    }

    private HBox centeredContent(javafx.scene.Node node) {
        HBox box = new HBox(node);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setFillHeight(false);
        box.setMinHeight(48);
        box.setPrefHeight(48);
        box.setMaxHeight(48);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void loadBalance() {
        BalanceResponseDTO response = accountService.getBalance();
        if (response != null && response.getUserId() != null) {
            double balance = response.getBalance();
            ProductDataManager.getInstance().setUserBalance(balance);
            if (sidebarBalanceLabel != null) {
                sidebarBalanceLabel.setText("Ví: $" + String.format("%.2f", balance));
            }
            return;
        }

        if (sidebarBalanceLabel != null) {
            sidebarBalanceLabel.setText("Ví: --");
        }
    }

    private void loadBidHistory() {
        bidHistoryTable.setItems(
                FXCollections.observableArrayList(accountService.getBidHistory())
        );
        bidHistoryTable.refresh();
    }

    @FXML
    private void handleRefreshHistory(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            return;
        }
        loadBidHistory();
        if (messageLabel != null) {
            messageLabel.setText("Đã làm mới lịch sử đặt giá.");
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "UET Auction System - Dashboard", 1280, 800);
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "UET Auction System - Dashboard", 1280, 800);
    }

    @FXML
    private void handleSidebarProducts(ActionEvent event) {
        if (!SessionManager.hasRole("Seller")) {
            if (messageLabel != null) {
                messageLabel.setText("Chỉ seller mới được quản lý sản phẩm.");
                messageLabel.setStyle("-fx-text-fill: #d92d20;");
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "UET Auction System - Kho hàng", 1280, 800);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/Account.fxml", "UET Auction System - Tài khoản", 1280, 800);
    }
}
