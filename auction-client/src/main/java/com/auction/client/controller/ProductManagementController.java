package com.auction.client.controller;

import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import com.auction.client.service.AuctionService;
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
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class ProductManagementController {
    private static final DateTimeFormatter AUCTION_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private TableView<Product> myProductsTable;
    @FXML private Label titleLabel;

    private final ObservableList<Product> productData = ProductDataManager.getInstance().getProductList();
    private final AuctionService auctionService = new AuctionService();

    @FXML private Button backButton;

    @FXML
    public void initialize() {
        titleLabel.setText("Kho h\u00e0ng c\u1ee7a t\u00f4i");

        TableColumn<Product, String> typeCol = new TableColumn<>("Lo\u1ea1i s\u1ea3n ph\u1ea9m");
        typeCol.setCellValueFactory(cellData -> cellData.getValue().typeProperty());

        TableColumn<Product, String> nameCol = new TableColumn<>("T\u00ean s\u1ea3n ph\u1ea9m");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        TableColumn<Product, Number> priceCol = new TableColumn<>("Gi\u00e1 ($)");
        priceCol.setCellValueFactory(cellData -> cellData.getValue().priceProperty());

        TableColumn<Product, String> statusCol = new TableColumn<>("Tr\u1ea1ng th\u00e1i");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        myProductsTable.getColumns().setAll(typeCol, nameCol, priceCol, statusCol);
        myProductsTable.setItems(productData);
        loadMyProductsFromServer();

        if (backButton != null) {
            // Ốp style xanh đen UET và khử viền xám triệt để
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

    @FXML
    private void handleAddProduct(ActionEvent event) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("\u0110\u0103ng s\u1ea3n ph\u1ea9m m\u1edbi");
        dialog.setHeaderText("\u0110i\u1ec1n chi ti\u1ebft k\u1ef9 thu\u1eadt cho s\u1ea3n ph\u1ea9m");

        ButtonType postButtonType = new ButtonType("\u0110\u0103ng b\u00e0i", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(postButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().getStyleClass().add("product-dialog-pane");
        dialog.getDialogPane().setPrefWidth(560);
        String dialogCss = getClass().getResource("/css/style.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(dialogCss);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("product-dialog-grid");
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 8, 2, 8));
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(150);
        labelColumn.setPrefWidth(160);
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, inputColumn);

        TextField nameField = new TextField();
        TextField priceField = new TextField();
        ComboBox<String> auctionTypeBox = new ComboBox<>();
        auctionTypeBox.getItems().addAll("ART", "ELECTRONICS", "VEHICLE");
        auctionTypeBox.setValue("ART");
        auctionTypeBox.setMaxWidth(Double.MAX_VALUE);
        TextField conditionField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        TextField warrantyField = new TextField();
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        TextField startTimeField = new TextField("09:00");
        TextField endTimeField = new TextField("18:00");
        TextField minIncrementField = new TextField("1");
        Label conditionLabel = new Label("T\u00e1c gi\u1ea3:");
        Label warrantyLabel = new Label("Th\u00f4ng tin ph\u1ee5:");
        warrantyField.setPromptText("C\u00f3 th\u1ec3 b\u1ecf tr\u1ed1ng");
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

        auctionTypeBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            String selectedType = newValue == null ? "ART" : newValue;
            if ("ELECTRONICS".equals(selectedType)) {
                conditionLabel.setText("T\u00ecnh tr\u1ea1ng:");
                warrantyLabel.setText("B\u1ea3o h\u00e0nh (th\u00e1ng):");
                warrantyField.setPromptText("V\u00ed d\u1ee5: 12");
            } else if ("VEHICLE".equals(selectedType)) {
                conditionLabel.setText("T\u00ecnh tr\u1ea1ng:");
                warrantyLabel.setText("H\u00e3ng xe:");
                warrantyField.setPromptText("V\u00ed d\u1ee5: Toyota");
            } else {
                conditionLabel.setText("T\u00e1c gi\u1ea3:");
                warrantyLabel.setText("Th\u00f4ng tin ph\u1ee5:");
                warrantyField.setPromptText("C\u00f3 th\u1ec3 b\u1ecf tr\u1ed1ng");
            }
        });

        grid.add(new Label("T\u00ean s\u1ea3n ph\u1ea9m:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Lo\u1ea1i auction:"), 0, 1);
        grid.add(auctionTypeBox, 1, 1);
        grid.add(new Label("Gi\u00e1 kh\u1edfi \u0111i\u1ec3m ($):"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(conditionLabel, 0, 3);
        grid.add(conditionField, 1, 3);
        grid.add(new Label("M\u00f4 t\u1ea3 chi ti\u1ebft:"), 0, 4);
        grid.add(descriptionArea, 1, 4);
        grid.add(warrantyLabel, 0, 5);
        grid.add(warrantyField, 1, 5);
        grid.add(new Label("Ng\u00e0y b\u1eaft \u0111\u1ea7u:"), 0, 6);
        grid.add(startDatePicker, 1, 6);
        grid.add(new Label("Gi\u1edd b\u1eaft \u0111\u1ea7u:"), 0, 7);
        grid.add(startTimeField, 1, 7);
        grid.add(new Label("Ng\u00e0y k\u1ebft th\u00fac:"), 0, 8);
        grid.add(endDatePicker, 1, 8);
        grid.add(new Label("Gi\u1edd k\u1ebft th\u00fac:"), 0, 9);
        grid.add(endTimeField, 1, 9);
        grid.add(new Label("B\u01b0\u1edbc gi\u00e1 t\u1ed1i thi\u1ec3u:"), 0, 10);
        grid.add(minIncrementField, 1, 10);

        dialog.getDialogPane().setContent(grid);
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
                    return new Product(id, auctionType, nameField.getText(), price, "\u0110ang m\u1edf",
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
                        extra
                );
                case "VEHICLE" -> auctionService.createVehicleAuction(
                        name,
                        desc,
                        startDate,
                        endDate,
                        newProduct.getPrice(),
                        minIncrement,
                        extra
                );
                default -> auctionService.createArtAuction(
                        name,
                        desc,
                        startDate,
                        endDate,
                        newProduct.getPrice(),
                        minIncrement,
                        condition
                );
            };

            if (response == null || response.isBlank()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("L\u1ed7i t\u1ea1o phi\u00ean \u0111\u1ea5u gi\u00e1");
                alert.setHeaderText("Kh\u00f4ng th\u1ec3 t\u1ea1o phi\u00ean tr\u00ean server");
                alert.setContentText("Server kh\u00f4ng ph\u1ea3n h\u1ed3i.");
                alert.showAndWait();
                return;
            }

            if (response.startsWith("ERR|")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("L\u1ed7i t\u1ea1o phi\u00ean \u0111\u1ea5u gi\u00e1");
                alert.setHeaderText("Kh\u00f4ng th\u1ec3 t\u1ea1o phi\u00ean tr\u00ean server");
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
                alert.setTitle("L\u1ed7i t\u1ea1o phi\u00ean \u0111\u1ea5u gi\u00e1");
                alert.setHeaderText("Kh\u00f4ng th\u1ec3 t\u1ea1o phi\u00ean tr\u00ean server");
                alert.setContentText("Ph\u1ea3n h\u1ed3i kh\u00f4ng h\u1ee3p l\u1ec7: " + response);
                alert.showAndWait();
                return;
            }

            productData.add(newProduct);
            ProductDataManager.getInstance().pushToGlobalAuction(newProduct);
        });

    }

    // Escape any '|' characters in text fields to avoid breaking pipe-separated protocol
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
            showDialogError("T\u00ean s\u1ea3n ph\u1ea9m kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng.");
            return false;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            double minIncrement = Double.parseDouble(minIncrementField.getText().trim());
            if (price <= 0 || minIncrement <= 0) {
                showDialogError("Gi\u00e1 v\u00e0 b\u01b0\u1edbc gi\u00e1 ph\u1ea3i l\u1edbn h\u01a1n 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showDialogError("Gi\u00e1 v\u00e0 b\u01b0\u1edbc gi\u00e1 ph\u1ea3i l\u00e0 s\u1ed1 h\u1ee3p l\u1ec7.");
            return false;
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showDialogError("Vui l\u00f2ng ch\u1ecdn ng\u00e0y b\u1eaft \u0111\u1ea7u v\u00e0 ng\u00e0y k\u1ebft th\u00fac.");
            return false;
        }

        try {
            LocalDateTime startDateTime = buildDateTime(startDatePicker.getValue(), startTimeField);
            LocalDateTime endDateTime = buildDateTime(endDatePicker.getValue(), endTimeField);
            if (!endDateTime.isAfter(startDateTime)) {
                showDialogError("Th\u1eddi gian k\u1ebft th\u00fac ph\u1ea3i sau th\u1eddi gian b\u1eaft \u0111\u1ea7u.");
                return false;
            }
        } catch (DateTimeParseException e) {
            showDialogError("Gi\u1edd ph\u1ea3i c\u00f3 \u0111\u1ecbnh d\u1ea1ng HH:mm, v\u00ed d\u1ee5 09:00 ho\u1eb7c 18:30.");
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
        alert.setTitle("D\u1eef li\u1ec7u kh\u00f4ng h\u1ee3p l\u1ec7");
        alert.setHeaderText("Vui l\u00f2ng ki\u1ec3m tra th\u00f4ng tin \u0111\u1ea5u gi\u00e1");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadMyProductsFromServer() {
        try {
            List<Product> products = auctionService.getMyAuctions()
                    .stream()
                    .map(auction -> new Product(
                            auction.getAuctionId(),
                            auction.getItemType(),
                            auction.getName(),
                            auction.getCurrentPrice(),
                            auction.getAuctionStatus().toString(),
                            auction.getCondition(),
                            auction.getDescription(),
                            auction.getWarranty(),
                            auction.getEndDateTime()
                    ))
                    .collect(java.util.stream.Collectors.toList());
            productData.setAll(products);
        } catch (IllegalStateException e) {
            System.err.println("Cannot load seller products: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditProduct(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog nameDialog = new TextInputDialog(selected.getName());
        nameDialog.setTitle("S\u1eeda nhanh");
        nameDialog.setHeaderText("C\u1eadp nh\u1eadt t\u00ean s\u1ea3n ph\u1ea9m:");
        nameDialog.showAndWait().ifPresent(newName -> {
            selected.nameProperty().set(newName);
            myProductsTable.refresh();
        });
    }

    @FXML
    private void handleDeleteProduct(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Gọi hàm xóa ở cả 2 danh sách trong Manager
            ProductDataManager.getInstance().deleteProductAndAuction(selected.getId());

            // UI sẽ tự cập nhật vì productData là ObservableList được quan sát bởi Manager
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AuctionList.fxml"));
            // Lấy Stage từ nút bấm
            Stage stage = (Stage) backButton.getScene().getWindow();

            Scene scene = new Scene(root, 1040, 660);

            // Nạp file CSS tổng vào Scene mới
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle("UET Auction System");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
