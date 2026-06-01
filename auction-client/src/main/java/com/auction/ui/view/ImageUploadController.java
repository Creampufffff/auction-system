package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.auction.application.service.AuctionService;
import com.auction.ui.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ImageUploadController {
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    @FXML private ComboBox<AuctionListDTO> productComboBox;
    @FXML private Label selectedFileLabel;
    @FXML private Label uploadStatusLabel;
    @FXML private VBox imageListContainer;

    private final AuctionService auctionService = new AuctionService();
    private File selectedFile;

    @FXML
    public void initialize() {
        configureProductComboBox();
        handleRefreshProducts();
    }

    @FXML
    private void handleRefreshProducts() {
        try {
            List<AuctionListDTO> auctions = auctionService.getMyAuctions();
            productComboBox.getItems().setAll(auctions);
            uploadStatusLabel.setText(auctions.isEmpty() ? "Chưa có sản phẩm để upload ảnh." : "");
            renderImageListPlaceholder();
        } catch (Exception e) {
            showError("Không thể tải danh sách sản phẩm", e.getMessage());
        }
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh JPG/PNG", "*.jpg", "*.jpeg", "*.png")
        );

        File file = fileChooser.showOpenDialog(currentStage());
        if (file == null) {
            return;
        }

        if (file.length() > MAX_IMAGE_SIZE_BYTES) {
            showError("Ảnh quá lớn", "Vui lòng chọn ảnh không vượt quá 5MB.");
            return;
        }

        selectedFile = file;
        selectedFileLabel.setText(file.getName());
        uploadStatusLabel.setText("");
    }

    @FXML
    private void handleUploadImage() {
        AuctionListDTO selectedAuction = productComboBox.getValue();
        if (selectedAuction == null) {
            showError("Thiếu sản phẩm", "Vui lòng chọn sản phẩm cần upload ảnh.");
            return;
        }
        if (selectedFile == null) {
            showError("Thiếu ảnh", "Vui lòng chọn file ảnh trước khi upload.");
            return;
        }

        try {
            byte[] imageBlob = Files.readAllBytes(selectedFile.toPath());
            String response = auctionService.uploadAuctionImage(selectedAuction.getAuctionId(), imageBlob);
            if (response == null || !response.startsWith("OK|UPLOAD_IMAGE|")) {
                showError("Upload thất bại", extractServerMessage(response));
                return;
            }

            uploadStatusLabel.setText("Upload ảnh thành công.");
            renderUploadedImage(selectedAuction, selectedFile);
        } catch (IOException e) {
            showError("Không thể đọc ảnh", e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "Quản lý sản phẩm", 1280, 800);
    }

    @FXML
    private void handleClose() {
        Stage stage = currentStage();
        if (stage != null) {
            stage.close();
        }
    }

    private void configureProductComboBox() {
        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(AuctionListDTO auction) {
                return formatAuctionLabel(auction);
            }

            @Override
            public AuctionListDTO fromString(String value) {
                return null;
            }
        });
        productComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AuctionListDTO auction, boolean empty) {
                super.updateItem(auction, empty);
                setText(empty ? null : formatAuctionLabel(auction));
            }
        });
    }

    private String formatAuctionLabel(AuctionListDTO auction) {
        if (auction == null) {
            return "";
        }
        return auction.getName() + " (" + auction.getAuctionId() + ")";
    }

    private void renderImageListPlaceholder() {
        imageListContainer.getChildren().setAll(new Label("Ảnh sẽ được lưu vào cột image_blob của item đã chọn."));
    }

    private void renderUploadedImage(AuctionListDTO auction, File file) {
        imageListContainer.getChildren().setAll(
                new Label("Sản phẩm: " + auction.getName()),
                new Label("File: " + file.getName()),
                new Label("Kích thước: " + file.length() / 1024 + " KB")
        );
    }

    private Stage currentStage() {
        if (productComboBox == null || productComboBox.getScene() == null) {
            return null;
        }
        return (Stage) productComboBox.getScene().getWindow();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi upload ảnh");
        alert.setHeaderText(header);
        alert.setContentText(message == null || message.isBlank() ? "Không rõ lỗi." : message);
        alert.showAndWait();
    }

    private String extractServerMessage(String response) {
        if (response == null || response.isBlank()) {
            return "Server không phản hồi.";
        }
        String[] parts = response.split("\\|", 3);
        return parts.length >= 3 ? parts[2] : response;
    }
}
