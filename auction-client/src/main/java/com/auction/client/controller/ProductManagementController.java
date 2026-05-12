package com.auction.client.controller;

import com.auction.client.model.Product;
import com.auction.client.model.ProductDataManager;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.util.Optional;

public class ProductManagementController {

    @FXML private TableView<Product> myProductsTable;
    @FXML private Label titleLabel;

    private final ObservableList<Product> productData = ProductDataManager.getInstance().getProductList();

    @FXML private Button backButton;

    @FXML
    public void initialize() {
        titleLabel.setText("Kho hàng của tôi");

        TableColumn<Product, String> idCol = new TableColumn<>("Mã SP");
        idCol.setCellValueFactory(cellData -> cellData.getValue().idProperty());

        TableColumn<Product, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        TableColumn<Product, Number> priceCol = new TableColumn<>("Giá ($)");
        priceCol.setCellValueFactory(cellData -> cellData.getValue().priceProperty());

        TableColumn<Product, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        myProductsTable.getColumns().setAll(idCol, nameCol, priceCol, statusCol);
        myProductsTable.setItems(productData);

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
        dialog.setTitle("Đăng sản phẩm mới");
        dialog.setHeaderText("Điền chi tiết kỹ thuật cho sản phẩm");

        ButtonType postButtonType = new ButtonType("Đăng bài", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(postButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField priceField = new TextField();
        TextField conditionField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        TextField warrantyField = new TextField();

        grid.add(new Label("Tên sản phẩm:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Giá khởi điểm ($):"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Tình trạng:"), 0, 2);
        grid.add(conditionField, 1, 2);
        grid.add(new Label("Mô tả chi tiết:"), 0, 3);
        grid.add(descriptionArea, 1, 3);
        grid.add(new Label("Bảo hành:"), 0, 4);
        grid.add(warrantyField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == postButtonType) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    String id = "MINE-" + (productData.size() + 101);
                    return new Product(id, nameField.getText(), price, "Đang mở",
                            conditionField.getText(), descriptionArea.getText(), warrantyField.getText());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(newProduct -> {
            productData.add(newProduct);
            ProductDataManager.getInstance().pushToGlobalAuction(newProduct);
        });
    }

    @FXML
    private void handleEditProduct(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog nameDialog = new TextInputDialog(selected.getName());
        nameDialog.setTitle("Sửa nhanh");
        nameDialog.setHeaderText("Cập nhật tên sản phẩm:");
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