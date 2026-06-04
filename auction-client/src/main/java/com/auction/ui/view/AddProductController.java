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
            dialog.setTitle("Dang san pham moi");
            dialog.setHeaderText(null);
            ButtonType postButtonType = new ButtonType("Dang bai", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(postButtonType, ButtonType.CANCEL);
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
            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
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
        fileChooser.setTitle("Chon anh san pham");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Anh JPG/PNG", "*.jpg", "*.jpeg", "*.png")
        );

        File selectedFile = fileChooser.showOpenDialog(nameField.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        if (selectedFile.length() > 5L * 1024L * 1024L) {
            showDialogError("Anh khong duoc vuot qua 5MB.");
            return;
        }

        try {
            selectedImageBlob = Files.readAllBytes(selectedFile.toPath());
            imageUploadTitle.setText(selectedFile.getName());
            imageUploadHelp.setText("Da chon " + selectedImageBlob.length / 1024 + " KB");
        } catch (IOException e) {
            showDialogError("Khong the doc file anh: " + e.getMessage());
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
                "Dang mo",
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
        if (!"ELECTRONICS".equals(auctionType) || value.isBlank()) {
            return value;
        }

        try {
            int warrantyMonths = Integer.parseInt(value);
            return String.valueOf(Math.max(0, warrantyMonths));
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private void updateTypeSpecificFields(String auctionType) {
        boolean isArt = "ART".equals(auctionType);
        if ("ELECTRONICS".equals(auctionType)) {
            conditionLabel.setText("Tinh trang:");
            conditionField.setPromptText("Nhap tinh trang san pham");
            warrantyLabel.setText("Bao hanh (thang):");
            warrantyField.setPromptText("Vi du: 12");
        } else if ("VEHICLE".equals(auctionType)) {
            conditionLabel.setText("Tinh trang:");
            conditionField.setPromptText("Nhap tinh trang xe");
            warrantyLabel.setText("Hang xe:");
            warrantyField.setPromptText("Vi du: Toyota");
        } else {
            conditionLabel.setText("Tac gia:");
            conditionField.setPromptText("Nhap ten tac gia (neu co)");
            warrantyField.clear();
        }

        typeSpecificBox.setVisible(!isArt);
        typeSpecificBox.setManaged(!isArt);
    }

    private boolean validateInput() {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showDialogError("Ten san pham khong duoc de trong.");
            return false;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            double minIncrement = Double.parseDouble(minIncrementField.getText().trim());
            if (price <= 0 || minIncrement <= 0) {
                showDialogError("Gia va buoc gia phai lon hon 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showDialogError("Gia va buoc gia phai la so hop le.");
            return false;
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showDialogError("Vui long chon ngay bat dau va ngay ket thuc.");
            return false;
        }

        try {
            LocalDateTime startDateTime = buildDateTime(startDatePicker.getValue(), startTimeField);
            LocalDateTime endDateTime = buildDateTime(endDatePicker.getValue(), endTimeField);
            if (!endDateTime.isAfter(startDateTime)) {
                showDialogError("Thoi gian ket thuc phai sau thoi gian bat dau.");
                return false;
            }
        } catch (DateTimeParseException e) {
            showDialogError("Gio phai co dinh dang HH:mm, vi du 09:00 hoac 18:30.");
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
        alert.setTitle("Loi");
        alert.setHeaderText("Khong the mo cua so dang san pham");
        alert.setContentText(e.getClass().getSimpleName() + ": " + e.getMessage());
        alert.showAndWait();
    }

    private void showDialogError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Du lieu khong hop le");
        alert.setHeaderText("Vui long kiem tra thong tin dau gia");
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
