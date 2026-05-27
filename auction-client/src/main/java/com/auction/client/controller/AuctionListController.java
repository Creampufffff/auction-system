package com.auction.client.controller;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import com.auction.client.service.AuctionService;
import com.auction.client.session.SessionManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class AuctionListController {

    @FXML private TableView<AuctionListDTO> auctionTable;
    @FXML private TableColumn<AuctionListDTO, String> idColumn;
    @FXML private TableColumn<AuctionListDTO, String> nameColumn;
    @FXML private TableColumn<AuctionListDTO, Double> priceColumn;
    @FXML private TableColumn<AuctionListDTO, Object> statusColumn;
    @FXML private Label messageLabel;

    // Sidebar balance (bottom-left panel)
    @FXML private Label accountRoleLabel;
    @FXML private Label accountBalanceLabel;

    // [MỚI] 3 stat boxes ở trên
    @FXML private Label totalSessionsLabel;  // Tổng phiên đang mở
    @FXML private Label onlineUsersLabel;    // Phiên đang mở (OPEN + RUNNING)
    @FXML private Label statBalanceLabel;    // Số dư (stat box riêng, khác sidebar)

    // [MỚI] Pagination & Search
    @FXML private TextField searchField;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;
    @FXML private Button productsSidebarButton;
    @FXML private Label pageInfoLabel;

    private final AuctionService auctionService = new AuctionService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));

        configureSidebarForRole();
        loadAuctions();

        auctionTable.refresh();

        // [MỚI] Setup tìm kiếm
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                ProductDataManager.getInstance().setSearchKeyword(newVal);
                refreshTablePage();
            });
        }

        Platform.runLater(() -> {
            updateUIWithBalance();
        });
    }

    private void configureSidebarForRole() {
        if (productsSidebarButton != null) {
            boolean isSeller = SessionManager.hasRole("Seller");
            productsSidebarButton.setVisible(isSeller);
            productsSidebarButton.setManaged(isSeller);
        }
    }

    private void loadAuctions() {
        if (ProductDataManager.getInstance().getServerAuctionList().isEmpty()) {
            ProductDataManager.getInstance().getServerAuctionList().addAll(auctionService.getActiveAuctions());
        }
        refreshTablePage();
    }

    // [MỚI] Refresh table theo trang hiện tại
    private void refreshTablePage() {
        auctionTable.setItems(ProductDataManager.getInstance().getPagedAuctions());
        auctionTable.refresh();
        updatePageInfo();
    }

    // [MỚI] Cập nhật thông tin phân trang
    private void updatePageInfo() {
        int currentPage = ProductDataManager.getInstance().getCurrentPage();
        int totalPages = ProductDataManager.getInstance().getTotalPages();
        int totalItems = ProductDataManager.getInstance().getTotalFilteredItems();

        if (pageInfoLabel != null) {
            if (totalPages == 0) {
                pageInfoLabel.setText("Không có phiên nào");
            } else {
                pageInfoLabel.setText(String.format("Trang %d / %d (%d phiên)", currentPage, totalPages, totalItems));
            }
        }

        if (previousPageButton != null) {
            previousPageButton.setDisable(currentPage <= 1);
        }
        if (nextPageButton != null) {
            nextPageButton.setDisable(currentPage >= totalPages);
        }
    }

    private void updateUIWithBalance() {
        ProductDataManager.getInstance().syncBalanceFromSession();
        double balance = ProductDataManager.getInstance().getUserBalance();
        String balanceText = "$" + String.format("%.2f", balance);

        if (accountRoleLabel != null) {
            String role = SessionManager.getCurrentUserRole();
            accountRoleLabel.setText("Role: " + (role == null ? "--" : role));
        }

        // Sidebar label (bottom-left)
        if (accountBalanceLabel != null) {
            accountBalanceLabel.setText("Ví: " + balanceText);
            if (balance < 100) {
                accountBalanceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;");
            }
        } else {
            System.out.println("DEBUG: accountBalanceLabel vẫn đang bị null!");
        }

        // Stat box 1: Tổng tất cả phiên
        if (totalSessionsLabel != null) {
            int count = ProductDataManager.getInstance().getServerAuctionList().size();
            totalSessionsLabel.setText(String.valueOf(count));
        }

        // Stat box 2: Phiên đang mở (OPEN hoặc RUNNING)
        if (onlineUsersLabel != null) {
            long openCount = ProductDataManager.getInstance().getServerAuctionList().stream()
                    .filter(a -> a.getAuctionStatus() == Status.OPEN || a.getAuctionStatus() == Status.RUNNING)
                    .count();
            onlineUsersLabel.setText(String.valueOf(openCount));
        }

        // Stat box 3: Số dư tài khoản
        if (statBalanceLabel != null) {
            statBalanceLabel.setText(balanceText);
            if (balance < 100) {
                statBalanceLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;");
            }
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
            switchScene("/fxml/ProductDetail.fxml", "Chi tiết sản phẩm", 1040, 660);
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
                        selected.getItemType(),
                        selected.getName(),
                        ProductDataManager.getInstance().getCurrentPrice(selected.getAuctionId(), selected.getCurrentPrice()),
                        selected.getAuctionStatus().toString(),
                        "New",
                        "No description",
                        "No warranty",
                        selected.getEndDateTime()
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
        if (!SessionManager.hasRole("Seller")) {
            if (messageLabel != null) {
                messageLabel.setText("Chỉ seller mới được quản lý sản phẩm.");
            }
            return;
        }
        switchScene("/fxml/ProductManagement.fxml", "Quản lý sản phẩm", 1040, 660);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        switchScene("/fxml/Account.fxml", "UET Auction System - Tài khoản", 1040, 660);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        ProductDataManager.getInstance().resetSessionState();
        switchScene("/fxml/Login.fxml", "Đăng nhập", 800, 600);
    }

    // [MỚI] Phương thức xử lý phân trang
    @FXML
    private void handlePreviousPage(ActionEvent event) {
        ProductDataManager.getInstance().previousPage();
        refreshTablePage();
    }

    @FXML
    private void handleNextPage(ActionEvent event) {
        ProductDataManager.getInstance().nextPage();
        refreshTablePage();
    }

    private void switchScene(String fxmlPath, String title, double width, double height) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            Scene scene = new Scene(root, width, height);
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
