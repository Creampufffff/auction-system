package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.auction.application.service.AuctionService;
import com.auction.domain.model.ProductDataManager;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class WonItemsController {
    @FXML private Label sidebarRoleLabel;
    @FXML private Label sidebarBalanceLabel;
    @FXML private Button productsSidebarButton;
    @FXML private Button wonItemsSidebarButton;
    @FXML private Button bidHistorySidebarButton;
    @FXML private Button refreshButton;
    @FXML private VBox wonItemsCard;
    @FXML private TableView<AuctionListDTO> wonItemsTable;
    @FXML private TableColumn<AuctionListDTO, String> typeColumn;
    @FXML private TableColumn<AuctionListDTO, String> nameColumn;
    @FXML private TableColumn<AuctionListDTO, Double> priceColumn;
    @FXML private TableColumn<AuctionListDTO, Object> statusColumn;
    @FXML private TableColumn<AuctionListDTO, String> endDateColumn;
    @FXML private Label messageLabel;

    private final AuctionService auctionService = new AuctionService();
    private Task<List<AuctionListDTO>> wonItemsTask;

    @FXML
    public void initialize() {
        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText("Role: " + SessionManager.getCurrentUserRole());
        }
        configureSidebarForRole();
        configureTable();

        Platform.runLater(() -> {
            loadBalanceFromSession();
            loadWonItemsAsync();
        });
    }

    private void configureSidebarForRole() {
        boolean isSeller = SessionManager.hasRole("Seller");
        boolean isBidder = SessionManager.hasRole("Bidder");

        if (productsSidebarButton != null) {
            productsSidebarButton.setVisible(isSeller);
            productsSidebarButton.setManaged(isSeller);
        }
        if (wonItemsSidebarButton != null) {
            wonItemsSidebarButton.setVisible(isBidder);
            wonItemsSidebarButton.setManaged(isBidder);
        }
        if (bidHistorySidebarButton != null) {
            bidHistorySidebarButton.setVisible(isBidder);
            bidHistorySidebarButton.setManaged(isBidder);
        }
        if (wonItemsCard != null) {
            wonItemsCard.setVisible(isBidder);
            wonItemsCard.setManaged(isBidder);
        }
    }

    private void configureTable() {
        if (wonItemsTable == null) {
            return;
        }

        typeColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDateTime"));

        wonItemsTable.setFixedCellSize(52);
        typeColumn.getStyleClass().add("centered-table-column");
        nameColumn.getStyleClass().add("centered-table-column");
        priceColumn.getStyleClass().add("centered-table-column");
        statusColumn.getStyleClass().add("centered-table-column");
        endDateColumn.getStyleClass().add("centered-table-column");

        typeColumn.setCellFactory(column -> centeredTextCell());
        nameColumn.setCellFactory(column -> centeredTextCell());
        endDateColumn.setCellFactory(column -> centeredTextCell());
        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label label = new Label("$" + String.format("%.2f", price));
                label.setMaxWidth(Double.MAX_VALUE);
                label.setAlignment(javafx.geometry.Pos.CENTER);
                label.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                setGraphic(centeredContent(label));
            }
        });
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Object status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label("Đã thắng");
                badge.setAlignment(javafx.geometry.Pos.CENTER);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add("status-finished");
                setGraphic(centeredContent(badge));
            }
        });
    }

    private TableCell<AuctionListDTO, String> centeredTextCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label label = new Label(text);
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setAlignment(javafx.geometry.Pos.CENTER);
                label.getStyleClass().add("auction-main-cell-label");
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

    private void loadBalanceFromSession() {
        ProductDataManager.getInstance().syncBalanceFromSession();
        double balance = SessionManager.getCurrentUserBalance();
        if (sidebarBalanceLabel != null) {
            sidebarBalanceLabel.setText("Ví: $" + String.format("%.2f", balance));
        }
    }

    private void loadWonItemsAsync() {
        if (!SessionManager.hasRole("Bidder")) {
            if (messageLabel != null) {
                messageLabel.setText("Quyền truy cập chỉ dành cho người đấu giá.");
            }
            return;
        }

        if (wonItemsTask != null && wonItemsTask.isRunning()) {
            return;
        }

        if (messageLabel != null) {
            messageLabel.setText("Đang tải kho vật phẩm...");
        }
        if (refreshButton != null) {
            refreshButton.setDisable(true);
        }

        wonItemsTask = new Task<>() {
            @Override
            protected List<AuctionListDTO> call() {
                return auctionService.getMyWonItems();
            }
        };

        wonItemsTask.setOnSucceeded(event -> {
            List<AuctionListDTO> items = wonItemsTask.getValue();
            wonItemsTable.setItems(FXCollections.observableArrayList(items));
            wonItemsTable.refresh();
            if (messageLabel != null) {
                messageLabel.setText("Đã tải " + wonItemsTable.getItems().size() + " vật phẩm.");
            }
            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
        });

        wonItemsTask.setOnFailed(event -> {
            if (messageLabel != null) {
                messageLabel.setText("Không thể tải kho vật phẩm. Vui lòng thử lại.");
            }
            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
        });

        Thread thread = new Thread(wonItemsTask, "won-items-loader");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRefreshItems(ActionEvent event) {
        loadWonItemsAsync();
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        AuctionListDTO selected = wonItemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (messageLabel != null) {
                messageLabel.setText("Vui lòng chọn một vật phẩm.");
            }
            return;
        }
        ProductDataManager.getInstance().setSelectedAuction(selected);
        ProductDataManager.getInstance().setProductDetailReturnTarget("/fxml/WonItems.fxml", "Kho vật phẩm");
        NavigationService.getInstance().navigateTo("/fxml/ProductDetail.fxml", "Chi tiết vật phẩm", 1280, 800);
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "Auction System", 1280, 800);
    }

    @FXML
    private void handleSidebarProducts(ActionEvent event) {
        if (SessionManager.hasRole("Seller")) {
            NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "Quản lý sản phẩm", 1280, 800);
        }
    }

    @FXML
    private void handleSidebarWonItems(ActionEvent event) {
        loadWonItemsAsync();
    }

    @FXML
    private void handleSidebarBidHistory(ActionEvent event) {
        if (SessionManager.hasRole("Bidder")) {
            NavigationService.getInstance().navigateTo("/fxml/BidHistory.fxml", "Lịch sử đặt giá", 1280, 800);
        }
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/Account.fxml", "Tài khoản", 1280, 800);
    }
}
