package com.auction.ui.view;

import com.auction.domain.model.Product;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class AddProductController {
    private static final DateTimeFormatter AUCTION_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> auctionTypeBox;
    @FXML private Label conditionLabel;
    @FXML private TextField conditionField;
    @FXML private TextArea descriptionArea;
    @FXML private VBox typeSpecificBox;
    @FXML private Label warrantyLabel;
    @FXML private TextField warrantyField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextField minIncrementField;
    @FXML private Label imageUploadTitle;
    @FXML private Label imageUploadHelp;

    private byte[] selectedImageBlob;
    private int existingProductCount;

    public static Optional<AddProductRequest> showAndCollect(int existingProductCount) {
        try {
            FXMLLoader loader = new FXMLLoader(AddProductController.class.getResource("/fxml/AddProduct.fxml"));
            Parent root = loader.load();
            AddProductController controller = loader.getController();
            controller.configure(existingProductCount);

            Dialog<AddProductRequest> dialog = new Dialog<>();
            dialog.setTitle("Đăng sản phẩm mới");
            dialog.setHeaderText(null);
            ButtonType postButtonType = new ButtonType("Đăng bài", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(postButtonType, cancelButtonType);
            dialog.getDialogPane().getStyleClass().add("product-dialog-pane");
            dialog.getDialogPane().getStylesheets().add(
                    AddProductController.class.getResource("/css/style.css").toExternalForm()
            );
            dialog.getDialogPane().setPrefWidth(820);
            dialog.getDialogPane().setMinWidth(820);
            dialog.getDialogPane().setPrefHeight(700);
            dialog.getDialogPane().setMinHeight(700);
            dialog.getDialogPane().setContent(root);

            Button postButton = (Button) dialog.getDialogPane().lookupButton(postButtonType);
            postButton.getStyleClass().add("product-dialog-primary-button");
            postButton.addEventFilter(ActionEvent.ACTION, event -> {
                if (!controller.validateInput()) {
                    event.consume();
                }
            });
            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
            cancelButton.getStyleClass().add("product-dialog-secondary-button");

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton != postButtonType) {
                    return null;
                }
                return controller.buildRequest();
            });
            return dialog.showAndWait();
        } catch (IOException e) {
            showOpenDialogError(e);
            return Optional.empty();
        }
    }

    @FXML
    public void initialize() {
        auctionTypeBox.getItems().setAll("ART", "ELECTRONICS", "VEHICLE");
        auctionTypeBox.setValue("ART");
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(7));
        startTimeField.setText("09:00");
        endTimeField.setText("18:00");
        updateTypeSpecificFields("ART");
        auctionTypeBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateTypeSpecificFields(newValue == null ? "ART" : newValue)
        );
    }

    private void configure(int existingProductCount) {
        this.existingProductCount = existingProductCount;
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh JPG/PNG", "*.jpg", "*.jpeg", "*.png")
        );

        File selectedFile = fileChooser.showOpenDialog(nameField.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        if (selectedFile.length() > 5L * 1024L * 1024L) {
            showDialogError("Ảnh không được vượt quá 5MB.");
            return;
        }

        try {
            selectedImageBlob = Files.readAllBytes(selectedFile.toPath());
            imageUploadTitle.setText(selectedFile.getName());
            imageUploadHelp.setText("Đã chọn " + selectedImageBlob.length / 1024 + " KB");
        } catch (IOException e) {
            showDialogError("Không thể đọc file ảnh: " + e.getMessage());
        }
    }

    private AddProductRequest buildRequest() {
        double price = Double.parseDouble(priceField.getText().trim());
        String auctionType = currentAuctionType();
        String typeSpecificValue = normalizeTypeSpecificValue(auctionType);
        String id = "MINE-" + (existingProductCount + 101);
        Product product = new Product(
                id,
                auctionType,
                nameField.getText().trim(),
                price,
                "Đang mở",
                valueOrEmpty(conditionField.getText()).trim(),
                valueOrEmpty(descriptionArea.getText()).trim(),
                typeSpecificValue
        );

        return new AddProductRequest(
                product,
                buildDateTimeString(startDatePicker.getValue(), startTimeField),
                buildDateTimeString(endDatePicker.getValue(), endTimeField),
                minIncrementField.getText().trim(),
                auctionType,
                selectedImageBlob
        );
    }

    private String normalizeTypeSpecificValue(String auctionType) {
        String value = valueOrEmpty(warrantyField.getText()).trim();
        if (!"ELECTRONICS".equals(auctionType)) {
            return value;
        }
        if (value.isBlank()) {
            return "0";
        }

        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+").matcher(value);
            if (!matcher.find()) {
                return "0";
            }
            int warrantyMonths = Integer.parseInt(matcher.group());
            return String.valueOf(Math.max(0, warrantyMonths));
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    private void updateTypeSpecificFields(String auctionType) {
        boolean isArt = "ART".equals(auctionType);
        if ("ELECTRONICS".equals(auctionType)) {
            conditionLabel.setText("Tình trạng:");
            conditionField.setPromptText("Nhập tình trạng sản phẩm");
            warrantyLabel.setText("Bảo hành (tháng):");
            warrantyField.setPromptText("Ví dụ: 12");
        } else if ("VEHICLE".equals(auctionType)) {
            conditionLabel.setText("Tình trạng:");
            conditionField.setPromptText("Nhập tình trạng xe");
            warrantyLabel.setText("Hãng xe:");
            warrantyField.setPromptText("Ví dụ: Toyota");
        } else {
            conditionLabel.setText("Tác giả:");
            conditionField.setPromptText("Nhập tên tác giả (nếu có)");
            warrantyField.clear();
        }

        typeSpecificBox.setVisible(!isArt);
        typeSpecificBox.setManaged(!isArt);
    }

    private boolean validateInput() {
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

    private String currentAuctionType() {
        return auctionTypeBox.getValue() == null ? "ART" : auctionTypeBox.getValue();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void showOpenDialogError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText("Không thể mở cửa sổ đăng sản phẩm");
        alert.setContentText(e.getClass().getSimpleName() + ": " + e.getMessage());
        alert.showAndWait();
    }

    private void showDialogError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Dữ liệu không hợp lệ");
        alert.setHeaderText("Vui lòng kiểm tra thông tin đấu giá");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static final class AddProductRequest {
        private final Product product;
        private final String startDateTime;
        private final String endDateTime;
        private final String minIncrement;
        private final String auctionType;
        private final byte[] imageBlob;

        private AddProductRequest(
                Product product,
                String startDateTime,
                String endDateTime,
                String minIncrement,
                String auctionType,
                byte[] imageBlob
        ) {
            this.product = product;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
            this.minIncrement = minIncrement;
            this.auctionType = auctionType;
            this.imageBlob = imageBlob;
        }

        public Product product() {
            return product;
        }

        public String startDateTime() {
            return startDateTime;
        }

        public String endDateTime() {
            return endDateTime;
        }

        public String minIncrement() {
            return minIncrement;
        }

        public String auctionType() {
            return auctionType;
        }

        public byte[] imageBlob() {
            return imageBlob;
        }
    }
}
