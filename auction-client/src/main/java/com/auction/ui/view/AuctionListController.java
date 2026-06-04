package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import com.auction.application.service.AuctionService;
import com.auction.application.service.SocketClientService;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

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
    @FXML private Label onlineUsersLabel;    // Phiên đang mở (RUNNING)
    @FXML private Label statBalanceLabel;    // Số dư (stat box riêng, khác sidebar)

    // [MỚI] Pagination & Search
    @FXML private TextField searchField;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;
    @FXML private Button joinBiddingButton;
    @FXML private Button productsSidebarButton;
    @FXML private Button wonItemsSidebarButton;
    @FXML private Button bidHistorySidebarButton;
    @FXML private Label pageInfoLabel;
    @FXML private ComboBox<FilterOption> statusFilterBox;
    @FXML private ComboBox<FilterOption> typeFilterBox;
    @FXML private ComboBox<FilterOption> sortFilterBox;

    private final AuctionService auctionService = new AuctionService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));
        configureAuctionTableVisuals();

        configureSidebarForRole();
        configureSellerActions();
        configureFilters();
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
        if (bidHistorySidebarButton != null) {
            boolean isBidder = SessionManager.hasRole("Bidder");
            bidHistorySidebarButton.setVisible(isBidder);
            bidHistorySidebarButton.setManaged(isBidder);
        }
        if (wonItemsSidebarButton != null) {
            boolean isBidder = SessionManager.hasRole("Bidder");
            wonItemsSidebarButton.setVisible(isBidder);
            wonItemsSidebarButton.setManaged(isBidder);
        }
    }

    private void configureSellerActions() {
        if (joinBiddingButton == null) {
            return;
        }

        boolean isSeller = SessionManager.hasRole("Seller");
        joinBiddingButton.setVisible(!isSeller);
        joinBiddingButton.setManaged(!isSeller);
    }

    private void configureFilters() {
        if (statusFilterBox != null) {
            statusFilterBox.setItems(FXCollections.observableArrayList(
                    new FilterOption("ALL", "Tất cả trạng thái"),
                    new FilterOption("OPEN", "Sắp diễn ra"),
                    new FilterOption("RUNNING", "Đang diễn ra"),
                    new FilterOption("FINISHED", "Đã kết thúc"),
                    new FilterOption("CANCELED", "Đã hủy")
            ));
            statusFilterBox.getSelectionModel().selectFirst();
            statusFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                ProductDataManager.getInstance().setStatusFilter(newVal == null ? "ALL" : newVal.value());
                refreshTablePage();
            });
        }

        if (typeFilterBox != null) {
            typeFilterBox.setItems(FXCollections.observableArrayList(
                    new FilterOption("ALL", "Tất cả danh mục"),
                    new FilterOption("ART", "Nghệ thuật"),
                    new FilterOption("ELECTRONICS", "Điện tử"),
                    new FilterOption("VEHICLE", "Xe cộ")
            ));
            typeFilterBox.getSelectionModel().selectFirst();
            typeFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                ProductDataManager.getInstance().setTypeFilter(newVal == null ? "ALL" : newVal.value());
                refreshTablePage();
            });
        }

        if (sortFilterBox != null) {
            sortFilterBox.setItems(FXCollections.observableArrayList(
                    new FilterOption("NEWEST", "Sắp xếp: Mới nhất"),
                    new FilterOption("ENDING_SOON", "Sắp kết thúc"),
                    new FilterOption("PRICE_ASC", "Giá thấp đến cao"),
                    new FilterOption("PRICE_DESC", "Giá cao đến thấp"),
                    new FilterOption("NAME_ASC", "Tên A-Z")
            ));
            sortFilterBox.getSelectionModel().selectFirst();
            sortFilterBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                ProductDataManager.getInstance().setSortMode(newVal == null ? "NEWEST" : newVal.value());
                refreshTablePage();
            });
        }
    }

    private void configureAuctionTableVisuals() {
        auctionTable.getStyleClass().add("auction-table");
        // Đặt kích thước cell lên 54px giúp bảng giãn đều hàng, giảm bớt khoảng trống thừa thãi bên dưới đáy
        auctionTable.setFixedCellSize(54);
        idColumn.getStyleClass().add("centered-table-column");
        nameColumn.getStyleClass().add("centered-table-column");
        priceColumn.getStyleClass().add("centered-table-column");
        statusColumn.getStyleClass().add("centered-table-column");

        idColumn.setCellFactory(column -> centeredTextCell());
        nameColumn.setCellFactory(column -> centeredTextCell());

        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(null);
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
                getStyleClass().removeAll("status-running", "status-open", "status-finished", "status-canceled");
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                String value = status.toString();
                Label badge = new Label(formatStatus(value));
                badge.setAlignment(javafx.geometry.Pos.CENTER);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add(switch (value) {
                    case "RUNNING" -> "status-running";
                    case "OPEN" -> "status-open";
                    case "FINISHED" -> "status-finished";
                    case "CANCELED" -> "status-canceled";
                    default -> "status-open";
                });
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

    private void loadAuctions() {
        ProductDataManager.getInstance().getServerAuctionList().setAll(auctionService.getActiveAuctions());
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
        if (nameColumn != null && nextPageButton != null) {
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

        // Stat box 2: Phiên đang mở là các phiên đang diễn ra, không tính phiên sắp diễn ra.
        if (onlineUsersLabel != null) {
            long openCount = ProductDataManager.getInstance().getServerAuctionList().stream()
                    .filter(a -> a.getAuctionStatus() == Status.RUNNING)
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
    private void handleResetFilters(ActionEvent event) {
        ProductDataManager.getInstance().setSearchKeyword("");
        ProductDataManager.getInstance().setStatusFilter("ALL");
        ProductDataManager.getInstance().setTypeFilter("ALL");
        ProductDataManager.getInstance().setSortMode("NEWEST");
        ProductDataManager.getInstance().resetAuctionFilters();

        if (searchField != null) {
            searchField.clear();
        }
        if (statusFilterBox != null) {
            statusFilterBox.getSelectionModel().selectFirst();
        }
        if (typeFilterBox != null) {
            typeFilterBox.getSelectionModel().selectFirst();
        }
        if (sortFilterBox != null) {
            sortFilterBox.getSelectionModel().selectFirst();
        }
        refreshTablePage();
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ProductDataManager.getInstance().setSelectedAuction(selected);
            ProductDataManager.getInstance().setProductDetailReturnTarget("/fxml/AuctionList.fxml", "Auction System");
            NavigationService.getInstance().navigateTo("/fxml/ProductDetail.fxml", "Chi tiết sản phẩm", 1280, 800);
        } else {
            if (messageLabel != null) messageLabel.setText("Vui lòng chọn sản phẩm trên bảng!");
        }
    }

    @FXML
    private void handleJoinBidding(ActionEvent event) {
        AuctionListDTO selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Bảo vệ nghiêm ngặt: Seller tuyệt đối không được tham gia đặt giá phiên đấu giá công khai
            if (SessionManager.hasRole("Seller")) {
                if (messageLabel != null) {
                    messageLabel.setText("Tài khoản người bán không có quyền tham gia đấu giá!");
                    messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
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

            // Khởi tạo model dữ liệu cho màn hình LiveBidding
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
            productModel.setMinIncrement(selected.getMinIncrement());

            // Lưu trữ đối tượng tạm để controller đích có thể chủ động cấu hình thay vì bóc tách FXML thủ công phá vỡ cấu trúc Stage
            ProductDataManager.getInstance().setLiveBiddingProductData(productModel);

            // Ép đi qua bộ xử lý tập trung của NavigationService giúp giữ nguyên Icon và định dạng Scene đồng bộ
            NavigationService.getInstance().navigateTo("/fxml/LiveBidding.fxml", "Đấu giá trực tiếp", 1280, 800);
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
        NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "Quản lý sản phẩm", 1280, 800);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/Account.fxml", "Tài khoản", 1280, 800);
    }

    @FXML
    private void handleSidebarBidHistory(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            if (messageLabel != null) {
                messageLabel.setText("Quyền truy cập chỉ dành cho người đấu giá.");
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/BidHistory.fxml", "Lịch sử đặt giá", 1280, 800);
    }

    @FXML
    private void handleSidebarWonItems(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            if (messageLabel != null) {
                messageLabel.setText("Quyền truy cập chỉ dành cho người đấu giá.");
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/WonItems.fxml", "Kho vật phẩm", 1280, 800);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SocketClientService.stopRealtimeListener();
        SessionManager.clear();
        ProductDataManager.getInstance().resetSessionState();
        NavigationService.getInstance().navigateToAuth("/fxml/Login.fxml", "Đăng nhập");
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

    private String formatStatus(String status) {
        return switch (status) {
            case "RUNNING" -> "Đang diễn ra";
            case "OPEN" -> "Sắp diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            case "CANCELED" -> "Đã hủy";
            default -> status;
        };
    }

    private static class FilterOption {
        private final String value;
        private final String label;

        private FilterOption(String value, String label) {
            this.value = value;
            this.label = label;
        }

        private String value() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
