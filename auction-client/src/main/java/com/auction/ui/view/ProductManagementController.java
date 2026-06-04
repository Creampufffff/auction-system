package com.auction.ui.view;

import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import com.auction.application.service.AuctionService;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ProductManagementController {

    @FXML private TableView<Product> myProductsTable;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;

    // BỔ SUNG: Khai báo các Label điều khiển thông tin tài khoản đồng bộ với file FXML mới
    @FXML private Label accountRoleLabel;
    @FXML private Label accountBalanceLabel;
    @FXML private Button productsSidebarButton;
    @FXML private Button bidHistorySidebarButton;

    private final ObservableList<Product> productData = ProductDataManager.getInstance().getProductList();
    private final AuctionService auctionService = new AuctionService();

    @FXML private Button backButton;

    @FXML
    public void initialize() {
        titleLabel.setText("Kho hàng của tôi");

        // BỔ SUNG: Cập nhật thông tin tài khoản và số dư ngay khi màn hình khởi tạo
        refreshAccountSidebarInfo();
        configureSidebarForRole();

        TableColumn<Product, String> typeCol = new TableColumn<>("Loại sản phẩm");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());

        TableColumn<Product, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        TableColumn<Product, Number> priceCol = new TableColumn<>("Giá ($)");
        priceCol.setCellValueFactory(cellData -> cellData.getValue().priceProperty());

        TableColumn<Product, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        configureProductTableVisuals(typeCol, nameCol, priceCol, statusCol);
        myProductsTable.getColumns().setAll(typeCol, nameCol, priceCol, statusCol);
        myProductsTable.setItems(productData);
        loadMyProductsFromServerAsync();

        if (backButton != null) {
            // Ốp style xanh đen hệ thống và khử viền xám triệt để
            String normalStyle =
                    "-fx-background-color: rgba(255, 255, 255, 0.05) !important;" +
                            "-fx-background-insets: 0 !important;" +
                            "-fx-background-radius: 6 !important;" +
                            "-fx-border-color: rgba(255, 255, 255, 0.2) !important;" +
                            "-fx-border-width: 1 !important;" +
                            "-fx-border-radius: 6 !important;" +
                            "-fx-text-fill: #afb9c7 !important;" +
                            "-fx-effect: null !important;" +
                            "-fx-padding: 11 14 !important;";

            backButton.setStyle(normalStyle);

            // Hiệu ứng hover cho đồng bộ với Live Bidding
            backButton.setOnMouseEntered(e -> backButton.setStyle(
                    "-fx-background-color: #2d6cdf !important;" +
                            "-fx-text-fill: white !important;" +
                            "-fx-border-color: #2d6cdf !important;" +
                            "-fx-background-radius: 6 !important;" +
                            "-fx-background-insets: 0 !important;" +
                            "-fx-effect: null !important;" +
                            "-fx-padding: 11 14 !important;"
            ));

            backButton.setOnMouseExited(e -> backButton.setStyle(normalStyle));
        }
    }

    // BỔ SUNG: Hàm trợ giúp lấy thông tin mới nhất từ SessionManager và ProductDataManager để đẩy lên UI Sidebar
    private void refreshAccountSidebarInfo() {
        // Đồng bộ số dư từ SessionManager vào ProductDataManager trước
        ProductDataManager.getInstance().syncBalanceFromSession();

        if (accountRoleLabel != null) {
            if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
                // Hiển thị tên Role thực tế của người dùng
                String roleName = SessionManager.hasRole("Seller") ? "Seller" : "Bidder";
                accountRoleLabel.setText("Role: " + roleName);
            } else {
                accountRoleLabel.setText("Vai trò: Khách");
            }
        }

        if (accountBalanceLabel != null) {
            // Lấy trực tiếp số dư định dạng tiền tệ từ ProductDataManager vừa được sync
            double balance = ProductDataManager.getInstance().getUserBalance();
            accountBalanceLabel.setText("Vĩ: $" + String.format("%.2f", balance));
        }
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

    private void configureProductTableVisuals(
            TableColumn<Product, String> typeCol,
            TableColumn<Product, String> nameCol,
            TableColumn<Product, Number> priceCol,
            TableColumn<Product, String> statusCol
    ) {
        if (!myProductsTable.getStyleClass().contains("auction-table")) {
            myProductsTable.getStyleClass().add("auction-table");
        }
        myProductsTable.setFixedCellSize(48);

        typeCol.getStyleClass().add("centered-table-column");
        nameCol.getStyleClass().add("centered-table-column");
        priceCol.getStyleClass().add("centered-table-column");
        statusCol.getStyleClass().add("centered-table-column");

        typeCol.setCellFactory(column -> centeredTextCell());
        nameCol.setCellFactory(column -> centeredTextCell());
        priceCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                Label label = new Label("$" + String.format("%.2f", price.doubleValue()));
                label.setMaxWidth(Double.MAX_VALUE);
                label.setAlignment(javafx.geometry.Pos.CENTER);
                label.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                setGraphic(centeredContent(label));
            }
        });
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-running", "status-open", "status-finished", "status-canceled");
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                Label badge = new Label(formatProductStatus(status));
                badge.setAlignment(javafx.geometry.Pos.CENTER);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add(statusStyleClass(status));
                setGraphic(centeredContent(badge));
            }
        });
    }

    private TableCell<Product, String> centeredTextCell() {
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

    private String formatProductStatus(String status) {
        return switch (status) {
            case "RUNNING" -> "Đang diễn ra";
            case "OPEN" -> "Sắp diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            case "CANCELED" -> "Đã hủy";
            default -> status;
        };
    }

    private String statusStyleClass(String status) {
        return switch (status) {
            case "RUNNING" -> "status-running";
            case "OPEN" -> "status-open";
            case "FINISHED" -> "status-finished";
            case "CANCELED" -> "status-canceled";
            default -> "status-open";
        };
    }

    @FXML
    private void handleAddProduct(ActionEvent event) {
        AddProductController.showAndCollect(productData.size()).ifPresent(addProductRequest -> {
            Product newProduct = addProductRequest.product();
            String name = escapePipe(newProduct.getName());
            String desc = escapePipe(newProduct.getDescription());
            String startDate = addProductRequest.startDateTime();
            String endDate = addProductRequest.endDateTime();
            newProduct.setEndDateTime(endDate);
            String minIncrement = addProductRequest.minIncrement();
            String auctionType = addProductRequest.auctionType();
            String condition = escapePipe(newProduct.getCondition());
            String extra = escapePipe(newProduct.getWarranty());

            String response = switch (auctionType) {
                case "ELECTRONICS" -> auctionService.createElectronicsAuction(
                        name,
                        desc,
                        startDate,
                        endDate,
                        newProduct.getPrice(),
                        minIncrement,
                        extra,
                        addProductRequest.imageBlob()
                );
                case "VEHICLE" -> auctionService.createVehicleAuction(
                        name,
                        desc,
                        startDate,
                        endDate,
                        newProduct.getPrice(),
                        minIncrement,
                        extra,
                        addProductRequest.imageBlob()
                );
                default -> auctionService.createArtAuction(
                        name,
                        desc,
                        startDate,
                        endDate,
                        newProduct.getPrice(),
                        minIncrement,
                        condition,
                        addProductRequest.imageBlob()
                );
            };

            if (response == null || response.isBlank()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Loi tao phien dau gia");
                alert.setHeaderText("Khong the tao phien tren server");
                alert.setContentText("Server khong phan hoi.");
                alert.showAndWait();
                return;
            }

            if (response.startsWith("ERR|")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Loi tao phien dau gia");
                alert.setHeaderText("Khong the tao phien tren server");
                alert.setContentText(response.substring(4));
                alert.showAndWait();
                return;
            }

            String expectedPrefix = "OK|CREATE_" + auctionType + "_AUCTION|";
            if (response.startsWith(expectedPrefix)) {
                String[] parts = response.split("\\|", 4);
                if (parts.length >= 3) {
                    newProduct.setId(parts[2]);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Loi tao phien dau gia");
                alert.setHeaderText("Khong the tao phien tren server");
                alert.setContentText("Phan hoi khong hop le: " + response);
                alert.showAndWait();
                return;
            }

            productData.add(newProduct);
            ProductDataManager.getInstance().pushToGlobalAuction(newProduct);
            refreshAccountSidebarInfo();
        });
    }

    private String escapePipe(String input) {
        if (input == null) return "";
        return input.replace("|", " ");
    }

    private void showDialogError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Dữ liệu không hợp lệ");
        alert.setHeaderText("Vui lòng kiểm tra thông tin đấu giá");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadMyProductsFromServerAsync() {
        if (subtitleLabel != null) {
            subtitleLabel.setText("Đang tải kho hàng...");
        }

        CompletableFuture
                .supplyAsync(this::loadMyProductsFromServer)
                .thenAccept(products -> javafx.application.Platform.runLater(() -> {
                    productData.setAll(products);
                    if (subtitleLabel != null) {
                        subtitleLabel.setText("Quản lý các sản phẩm và phiên đấu giá của bạn.");
                    }
                    // Đồng bộ lại UI tài khoản sau khi hoàn thành tải dữ liệu
                    refreshAccountSidebarInfo();
                }))
                .exceptionally(error -> {
                    javafx.application.Platform.runLater(() -> {
                        if (subtitleLabel != null) {
                            subtitleLabel.setText("Không thể tải kho hàng từ server.");
                        }
                        System.err.println("Cannot load seller products: " + error.getMessage());
                    });
                    return null;
                });
    }

    private List<Product> loadMyProductsFromServer() {
        try {
            return auctionService.getMyAuctions()
                    .stream()
                    .map(auction -> {
                        Product product = new Product(
                                auction.getAuctionId(),
                                auction.getItemType(),
                                auction.getName(),
                                auction.getCurrentPrice(),
                                auction.getAuctionStatus().toString(),
                                auction.getCondition(),
                                auction.getDescription(),
                                auction.getWarranty(),
                                auction.getStartDateTime(),
                                auction.getEndDateTime()
                        );
                        product.setMinIncrement(auction.getMinIncrement());
                        return product;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (IllegalStateException e) {
            throw e;
        }
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showDialogError("Vui lòng chọn một sản phẩm để xem chi tiết.");
            return;
        }

        ProductDataManager.getInstance().setSelectedAuction(toAuctionSummary(selected));
        ProductDataManager.getInstance().setProductDetailReturnTarget("/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
        NavigationService.getInstance().navigateTo("/fxml/ProductDetail.fxml", "Chi tiết sản phẩm", 1280, 800);
    }

    private AuctionListDTO toAuctionSummary(Product product) {
        AuctionListDTO auction = new AuctionListDTO();
        auction.setAuctionId(product.getId());
        auction.setItemId(product.getId());
        auction.setItemType(product.getType());
        auction.setName(product.getName());
        auction.setCurrentPrice(product.getPrice());
        auction.setAuctionStatus(parseStatus(product.getStatus()));
        auction.setCondition(product.getCondition());
        auction.setDescription(product.getDescription());
        auction.setWarranty(product.getWarranty());
        auction.setStartDateTime(product.getStartDateTime());
        auction.setEndDateTime(product.getEndDateTime());
        auction.setMinIncrement(product.getMinIncrement());
        return auction;
    }

    @FXML
    private void handleMonitorAuction(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showDialogError("Vui lòng chọn một sản phẩm để theo dõi.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LiveBidding.fxml"));
            Parent root = loader.load();
            LiveBiddingController controller = loader.getController();
            controller.setSellerMonitorMode(true);
            controller.setReturnTarget("/fxml/ProductManagement.fxml", "Quản lý sản phẩm");
            controller.setProduct(toLiveBiddingProduct(selected));

            Stage stage = (Stage) myProductsTable.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1280, 800);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            String css = getClass().getResource("/css/style.css").toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }

            stage.setTitle("Theo dõi đấu giá");
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showDialogError("Không thể mở màn hình theo dõi đấu giá.");
        }
    }

    private Product toLiveBiddingProduct(Product selected) {
        Product product = new Product(
                selected.getId(),
                selected.getType(),
                selected.getName(),
                selected.getPrice(),
                selected.getStatus(),
                selected.getCondition(),
                selected.getDescription(),
                selected.getWarranty(),
                selected.getStartDateTime(),
                selected.getEndDateTime()
        );
        product.setMinIncrement(selected.getMinIncrement());
        return product;
    }

    private Status parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return Status.OPEN;
        }

        try {
            return Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            return Status.OPEN;
        }
    }

    @FXML
    private void handleEditProduct(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog nameDialog = new TextInputDialog(selected.getName());
        nameDialog.setTitle("Sửa nhanh");
        nameDialog.setHeaderText("Cập nhật tên sản phẩm:");
        nameDialog.showAndWait().ifPresent(rawName -> {
            String newName = rawName == null ? "" : rawName.trim();
            if (newName.isEmpty()) {
                showDialogError("Tên sản phẩm không được để trống.");
                return;
            }

            String response = auctionService.renameAuction(selected.getId(), escapePipe(newName));
            if (response == null || !response.startsWith("OK|UPDATE_AUCTION|")) {
                showServerActionError("Không thể cập nhật sản phẩm", response);
                return;
            }

            selected.nameProperty().set(newName);
            ProductDataManager.getInstance().getServerAuctionList().stream()
                    .filter(auction -> auction.getAuctionId().equals(selected.getId()))
                    .findFirst()
                    .ifPresent(auction -> auction.setName(newName));
            myProductsTable.refresh();
        });
    }

    @FXML
    private void handleEditAuctionFull(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showDialogError("Vui lòng chọn một sản phẩm để chỉnh sửa.");
            return;
        }

        AuctionEditController.showAndUpdate(selected, auctionService)
                .ifPresent(updated -> myProductsTable.refresh());
    }

    @FXML
    private void handleDeleteProduct(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xóa sản phẩm");
            confirm.setHeaderText("Xóa sản phẩm khỏi server?");
            confirm.setContentText(selected.getName());

            Optional<ButtonType> answer = confirm.showAndWait();
            if (answer.isEmpty() || answer.get() != ButtonType.OK) {
                return;
            }

            String response = auctionService.deleteAuction(selected.getId());
            if (response == null || !response.startsWith("OK|DELETE_AUCTION|")) {
                showServerActionError("Không thể xóa sản phẩm", response);
                return;
            }

            ProductDataManager.getInstance().deleteProductAndAuction(selected.getId());
            myProductsTable.refresh();

            // Đồng bộ lại thông tin ví tiền đề phòng có cơ chế hoàn cọc/phí niêm yết từ server
            refreshAccountSidebarInfo();
        }
    }

    private void showServerActionError(String header, String response) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi server");
        alert.setHeaderText(header);
        alert.setContentText(extractServerMessage(response));
        alert.showAndWait();
    }

    private String extractServerMessage(String response) {
        if (response == null || response.isBlank()) {
            return "Server không phản hồi.";
        }
        String[] parts = response.split("\\|", 3);
        return parts.length >= 3 ? parts[2] : response;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AuctionList.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();

            Scene scene = new Scene(root, 1280, 800);

            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle("Auction System");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSidebarProducts(ActionEvent event) {
        if (!SessionManager.hasRole("Seller")) {
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "Quản lý sản phẩm", 1280, 800);
    }

    @FXML
    private void handleSidebarBidHistory(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/BidHistory.fxml", "Auction System - Lịch sử đặt giá", 1280, 800);
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "Auction System", 1280, 800);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/Account.fxml", "Auction System - Tài khoản", 1280, 800);
    }
}
