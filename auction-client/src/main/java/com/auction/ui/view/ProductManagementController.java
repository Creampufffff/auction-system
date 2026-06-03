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
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ProductManagementController {
    private static final DateTimeFormatter AUCTION_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Đăng sản phẩm mới");
        dialog.setHeaderText(null);

        ButtonType postButtonType = new ButtonType("Đăng bài", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(postButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().getStyleClass().add("product-dialog-pane");
        dialog.getDialogPane().setPrefWidth(820);
        dialog.getDialogPane().setMinWidth(820);
        dialog.getDialogPane().setPrefHeight(700);
        dialog.getDialogPane().setMinHeight(700);
        String dialogCss = getClass().getResource("/css/style.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(dialogCss);

        TextField nameField = new TextField();
        TextField priceField = new TextField();
        ComboBox<String> auctionTypeBox = new ComboBox<>();
        auctionTypeBox.getItems().addAll("ART", "ELECTRONICS", "VEHICLE");
        auctionTypeBox.setValue("ART");
        auctionTypeBox.setMaxWidth(Double.MAX_VALUE);
        TextField conditionField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(2);
        TextField warrantyField = new TextField();
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        TextField startTimeField = new TextField("09:00");
        TextField endTimeField = new TextField("18:00");
        TextField minIncrementField = new TextField("1");
        Label conditionLabel = new Label("Tác giả:");
        Label warrantyLabel = new Label("Thông tin phụ:");
        nameField.setPromptText("Nhập tên sản phẩm");
        priceField.setPromptText("Nhập giá khởi điểm");
        conditionField.setPromptText("Nhập tên tác giả (nếu có)");
        descriptionArea.setPromptText("Nhập mô tả chi tiết về sản phẩm...");
        warrantyField.setPromptText("Có thể bỏ trống");
        startTimeField.setPromptText("HH:mm");
        endTimeField.setPromptText("HH:mm");
        styleProductDialogControls(
                nameField,
                priceField,
                auctionTypeBox,
                conditionField,
                descriptionArea,
                warrantyField,
                startDatePicker,
                endDatePicker,
                startTimeField,
                endTimeField,
                minIncrementField
        );

        updateTypeSpecificFields(auctionTypeBox.getValue(), conditionLabel, warrantyLabel, warrantyField);
        auctionTypeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            String selectedType = newValue == null ? "ART" : newValue;
            if ("ELECTRONICS".equals(selectedType)) {
                conditionLabel.setText("Tình trạng:");
                warrantyLabel.setText("Bảo hành (tháng):");
                warrantyField.setPromptText("Ví dụ: 12");
            } else if ("VEHICLE".equals(selectedType)) {
                conditionLabel.setText("Tình trạng:");
                warrantyLabel.setText("Hãng xe:");
                warrantyField.setPromptText("Ví dụ: Toyota");
            } else {
                conditionLabel.setText("Tác giả:");
            }
            updateTypeSpecificFields(selectedType, conditionLabel, warrantyLabel, warrantyField);
        });

        VBox headerText = new VBox(4,
                styledLabel("Đăng sản phẩm mới", "product-dialog-title"),
                styledLabel("Điền thông tin chi tiết để tạo phiên đấu giá", "product-dialog-subtitle")
        );
        Label headerIcon = styledLabel("⚖", "product-dialog-header-icon");
        HBox header = new HBox(16, headerIcon, headerText);
        header.getStyleClass().add("product-dialog-custom-header");

        VBox basicFields = new VBox(12,
                dialogField("Tên sản phẩm *", nameField),
                dialogField("Loại auction *", auctionTypeBox),
                dialogField("Giá khởi điểm ($) *", priceField),
                dialogField(conditionLabel, conditionField)
        );
        basicFields.setPrefWidth(540);

        final byte[][] selectedImageBlob = new byte[1][];
        Label imageUploadTitle = styledLabel("Thêm ảnh sản phẩm", "product-dialog-upload-title");
        Label imageUploadHelp = styledLabel("Kéo thả hoặc nhấn để chọn", "product-dialog-upload-help");
        VBox imageBox = new VBox(10,
                styledLabel("❑", "product-dialog-upload-icon"),
                imageUploadTitle,
                imageUploadHelp,
                styledLabel("JPG, PNG (tối đa 5MB)", "product-dialog-upload-note")
        );
        imageBox.getStyleClass().add("product-dialog-upload-box");
        imageBox.setOnMouseClicked(mouseEvent -> chooseProductImage(dialog, selectedImageBlob, imageUploadTitle, imageUploadHelp));
        HBox basicRow = new HBox(28, basicFields, imageBox);

        VBox basicSection = new VBox(16,
                sectionTitle("Thông tin cơ bản"),
                basicRow,
                dialogField("Mô tả chi tiết", descriptionArea),
                dialogField(warrantyLabel, warrantyField)
        );

        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(28);
        timeGrid.setVgap(12);
        ColumnConstraints leftTimeColumn = new ColumnConstraints();
        leftTimeColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints rightTimeColumn = new ColumnConstraints();
        rightTimeColumn.setHgrow(Priority.ALWAYS);
        timeGrid.getColumnConstraints().addAll(leftTimeColumn, rightTimeColumn);
        timeGrid.add(dialogField("Ngày bắt đầu *", startDatePicker), 0, 0);
        timeGrid.add(dialogField("Ngày kết thúc *", endDatePicker), 1, 0);
        timeGrid.add(dialogField("Giờ bắt đầu *", startTimeField), 0, 1);
        timeGrid.add(dialogField("Giờ kết thúc *", endTimeField), 1, 1);

        VBox timeSection = new VBox(14, sectionTitle("Thời gian đấu giá"), timeGrid);
        timeSection.getStyleClass().add("product-dialog-section");

        VBox incrementSection = new VBox(10, dialogField("Bước giá tối thiểu ($) *", minIncrementField));
        incrementSection.getStyleClass().add("product-dialog-section");

        VBox body = new VBox(16, basicSection, timeSection, incrementSection);
        body.getStyleClass().add("product-dialog-body");

        VBox shell = new VBox(header, body);
        shell.getStyleClass().add("product-dialog-shell");
        ScrollPane scrollPane = new ScrollPane(shell);
        scrollPane.getStyleClass().add("product-dialog-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        dialog.getDialogPane().setContent(scrollPane);
        Button postButton = (Button) dialog.getDialogPane().lookupButton(postButtonType);
        postButton.getStyleClass().add("product-dialog-primary-button");
        postButton.addEventFilter(ActionEvent.ACTION, actionEvent -> {
            if (!validateAuctionDialogInput(
                    nameField,
                    priceField,
                    startDatePicker,
                    startTimeField,
                    endDatePicker,
                    endTimeField,
                    minIncrementField
            )) {
                actionEvent.consume();
            }
        });
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("product-dialog-secondary-button");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == postButtonType) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    String id = "MINE-" + (productData.size() + 101);
                    String auctionType = auctionTypeBox.getValue() == null ? "ART" : auctionTypeBox.getValue();
                    return new Product(id, auctionType, nameField.getText(), price, "Đang mở",
                            conditionField.getText(), descriptionArea.getText(), warrantyField.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(newProduct -> {
            String name = escapePipe(newProduct.getName());
            String desc = escapePipe(newProduct.getDescription());
            LocalDate startDateValue = startDatePicker.getValue();
            LocalDate endDateValue = endDatePicker.getValue();
            String startDate = buildDateTimeString(startDateValue, startTimeField);
            String endDate = buildDateTimeString(endDateValue, endTimeField);
            newProduct.setEndDateTime(endDate);
            String minIncrement = minIncrementField.getText();
            String auctionType = auctionTypeBox.getValue() == null ? "ART" : auctionTypeBox.getValue();
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
                        selectedImageBlob[0]
                );
                case "VEHICLE" -> auctionService.createVehicleAuction(
                        name,
                        desc,
                        startDate,
                        endDate,
                        newProduct.getPrice(),
                        minIncrement,
                        extra,
                        selectedImageBlob[0]
                );
                default -> auctionService.createArtAuction(
                        name,
                        desc,
                        startDate,
                        endDate,
                        newProduct.getPrice(),
                        minIncrement,
                        condition,
                        selectedImageBlob[0]
                );
            };

            if (response == null || response.isBlank()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi tạo phiên đấu giá");
                alert.setHeaderText("Không thể tạo phiên trên server");
                alert.setContentText("Server không phản hồi.");
                alert.showAndWait();
                return;
            }

            if (response.startsWith("ERR|")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi tạo phiên đấu giá");
                alert.setHeaderText("Không thể tạo phiên trên server");
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
                alert.setTitle("Lỗi tạo phiên đấu giá");
                alert.setHeaderText("Không thể tạo phiên trên server");
                alert.setContentText("Phản hồi không hợp lệ: " + response);
                alert.showAndWait();
                return;
            }

            productData.add(newProduct);
            ProductDataManager.getInstance().pushToGlobalAuction(newProduct);

            // Cập nhật lại số dư ví tiền Sidebar đề phòng server có thay đổi phí tạo sàn
            refreshAccountSidebarInfo();
        });
    }

    private void chooseProductImage(
            Dialog<?> dialog,
            byte[][] selectedImageBlob,
            Label imageUploadTitle,
            Label imageUploadHelp
    ) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh JPG/PNG", "*.jpg", "*.jpeg", "*.png")
        );

        File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        if (selectedFile.length() > 5L * 1024L * 1024L) {
            showDialogError("Ảnh không được vượt quá 5MB.");
            return;
        }

        try {
            selectedImageBlob[0] = Files.readAllBytes(selectedFile.toPath());
            imageUploadTitle.setText(selectedFile.getName());
            imageUploadHelp.setText("Đã chọn " + selectedImageBlob[0].length / 1024 + " KB");
        } catch (IOException e) {
            showDialogError("Không thể đọc file ảnh: " + e.getMessage());
        }
    }

    private String escapePipe(String input) {
        if (input == null) return "";
        return input.replace("|", " ");
    }

    private void styleProductDialogControls(Control... controls) {
        for (Control control : controls) {
            control.getStyleClass().add("product-dialog-input");
            control.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private VBox dialogField(String labelText, Control control) {
        return dialogField(styledLabel(labelText, "product-dialog-field-label"), control);
    }

    private VBox dialogField(Label label, Control control) {
        label.getStyleClass().add("product-dialog-field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(7, label, control);
        box.getStyleClass().add("product-dialog-field");
        return box;
    }

    private HBox sectionTitle(String text) {
        Label icon = styledLabel("❑", "product-dialog-section-icon");
        Label title = styledLabel(text, "product-dialog-section-title");
        HBox box = new HBox(10, icon, title);
        box.getStyleClass().add("product-dialog-section-title-row");
        return box;
    }

    private Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private void updateTypeSpecificFields(
            String auctionType,
            Label conditionLabel,
            Label warrantyLabel,
            TextField warrantyField
    ) {
        boolean isArt = auctionType == null || "ART".equals(auctionType);
        if (isArt) {
            conditionLabel.setText("Tác giả:");
            warrantyField.clear();
        }

        warrantyLabel.setVisible(!isArt);
        warrantyLabel.setManaged(!isArt);
        warrantyField.setVisible(!isArt);
        warrantyField.setManaged(!isArt);
    }

    private boolean validateAuctionDialogInput(
            TextField nameField,
            TextField priceField,
            DatePicker startDatePicker,
            TextField startTimeField,
            DatePicker endDatePicker,
            TextField endTimeField,
            TextField minIncrementField
    ) {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showDialogError("Tên sản phẩm không được để trống.");
            return false;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            double minIncrement = Double.parseDouble(minIncrementField.getText().trim());
            if (price <= 0 || minIncrement <= 0) {
                showDialogError("Giá và bước giá phải lớn hơn 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showDialogError("Giá và bước giá phải là số hợp lệ.");
            return false;
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showDialogError("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
            return false;
        }

        try {
            LocalDateTime startDateTime = buildDateTime(startDatePicker.getValue(), startTimeField);
            LocalDateTime endDateTime = buildDateTime(endDatePicker.getValue(), endTimeField);
            if (!endDateTime.isAfter(startDateTime)) {
                showDialogError("Thời gian kết thúc phải sau thời gian bắt đầu.");
                return false;
            }
        } catch (DateTimeParseException e) {
            showDialogError("Giờ phải có định dạng HH:mm, ví dụ 09:00 hoặc 18:30.");
            return false;
        }

        return true;
    }

    private String buildDateTimeString(LocalDate date, TextField timeField) {
        return buildDateTime(date, timeField).format(AUCTION_DATE_TIME_FORMATTER);
    }

    private LocalDateTime buildDateTime(LocalDate date, TextField timeField) {
        return LocalDateTime.of(date, parseTime(timeField.getText()));
    }

    private LocalTime parseTime(String value) {
        if (value == null) {
            throw new DateTimeParseException("Time is empty", "", 0);
        }

        String trimmed = value.trim();
        if (trimmed.matches("\\d{1,2}:\\d{2}")) {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm"));
        }
        if (trimmed.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm:ss"));
        }
        throw new DateTimeParseException("Invalid time", trimmed, 0);
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
