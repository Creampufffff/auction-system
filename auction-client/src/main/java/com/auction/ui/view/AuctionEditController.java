package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.auction.application.service.AuctionService;
import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class AuctionEditController {
    private static final DateTimeFormatter AUCTION_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField priceField;
    @FXML private TextField minIncrementField;
    @FXML private Label typeSpecificLabel;
    @FXML private TextField typeSpecificField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    @FXML private TextArea descriptionArea;

    private Product product;
    private AuctionService auctionService;

    public static Optional<Product> showAndUpdate(Product product, AuctionService auctionService) {
        try {
            FXMLLoader loader = new FXMLLoader(AuctionEditController.class.getResource("/fxml/AuctionEditDialog.fxml"));
            Parent root = loader.load();
            AuctionEditController controller = loader.getController();
            controller.configure(product, auctionService);

            Dialog<Product> dialog = new Dialog<>();
            dialog.setTitle("Chỉnh sửa phiên đấu giá");
            dialog.setHeaderText(null);
            ButtonType saveButtonType = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            dialog.getDialogPane().getStyleClass().add("product-dialog-pane");
            dialog.getDialogPane().getStylesheets().add(
                    AuctionEditController.class.getResource("/css/style.css").toExternalForm()
            );
            dialog.getDialogPane().setPrefWidth(760);
            dialog.getDialogPane().setMinWidth(760);
            dialog.getDialogPane().setPrefHeight(660);
            dialog.getDialogPane().setMinHeight(660);
            dialog.getDialogPane().setContent(root);

            Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.getStyleClass().add("product-dialog-primary-button");
            saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                if (!controller.validateInput()) {
                    event.consume();
                }
            });
            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelButton.getStyleClass().add("product-dialog-secondary-button");

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton != saveButtonType) {
                    return null;
                }
                return controller.updateAuction();
            });
            return dialog.showAndWait();
        } catch (IOException e) {
            showOpenDialogError(e);
            return Optional.empty();
        }
    }

    private void configure(Product product, AuctionService auctionService) {
        this.product = product;
        this.auctionService = auctionService;

        AuctionListDTO detail = auctionService.getAuctionById(product.getId());
        if (detail != null) {
            applyAuctionDetailToProduct(product, detail);
        }

        String type = product.getType() == null ? "ART" : product.getType();
        LocalDateTime startDateTime = parseDateTimeOrDefault(product.getStartDateTime(), LocalDateTime.now());
        LocalDateTime endDateTime = parseDateTimeOrDefault(product.getEndDateTime(), LocalDateTime.now().plusDays(7));

        nameField.setText(valueOrEmpty(product.getName()));
        typeField.setText(valueOrEmpty(product.getType()));
        priceField.setText("$" + String.format("%.2f", product.getPrice()));
        minIncrementField.setText("$" + String.format("%.2f", product.getMinIncrement()));
        typeSpecificLabel.setText(typeSpecificLabelText(type));
        typeSpecificField.setText(typeSpecificValue(product, type));
        typeSpecificField.setPromptText(typeSpecificPromptText(type));
        startDatePicker.setValue(startDateTime.toLocalDate());
        startTimeField.setText(startDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        endDatePicker.setValue(endDateTime.toLocalDate());
        endTimeField.setText(endDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        descriptionArea.setText(valueOrEmpty(product.getDescription()));
    }

    private Product updateAuction() {
        AuctionEditValues values = new AuctionEditValues(
                nameField.getText().trim(),
                descriptionArea.getText() == null ? "" : descriptionArea.getText().trim(),
                buildDateTimeString(startDatePicker.getValue(), startTimeField),
                buildDateTimeString(endDatePicker.getValue(), endTimeField),
                typeSpecificField.getText() == null ? "" : typeSpecificField.getText().trim()
        );

        String response = auctionService.updateAuction(
                product.getId(),
                escapePipe(values.name()),
                escapePipe(values.description()),
                values.startDateTime(),
                values.endDateTime(),
                product.getPrice(),
                String.valueOf(product.getMinIncrement()),
                escapePipe(values.typeSpecificValue())
        );

        if (response == null || !response.startsWith("OK|UPDATE_AUCTION|")) {
            showServerActionError("Không thể cập nhật phiên đấu giá", response);
            return null;
        }

        applyEditedAuctionValues(product, values);
        ProductDataManager.getInstance().getServerAuctionList().stream()
                .filter(auction -> auction.getAuctionId().equals(product.getId()))
                .findFirst()
                .ifPresent(auction -> applyEditedAuctionValues(auction, product.getType(), values));
        return product;
    }

    private boolean validateInput() {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            showDialogError("Tên sản phẩm không được để trống.");
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

    private void applyAuctionDetailToProduct(Product product, AuctionListDTO detail) {
        product.setName(valueOrEmpty(detail.getName()));
        product.setType(valueOrEmpty(detail.getItemType()));
        product.setPrice(detail.getCurrentPrice());
        product.setStatus(detail.getAuctionStatus() == null ? "OPEN" : detail.getAuctionStatus().toString());
        product.setCondition(valueOrEmpty(detail.getCondition()));
        product.setDescription(valueOrEmpty(detail.getDescription()));
        product.setWarranty(valueOrEmpty(detail.getWarranty()));
        product.setStartDateTime(detail.getStartDateTime());
        product.setEndDateTime(detail.getEndDateTime());
        product.setMinIncrement(detail.getMinIncrement());
    }

    private void applyEditedAuctionValues(Product product, AuctionEditValues values) {
        product.setName(values.name());
        product.setDescription(values.description());
        product.setStartDateTime(values.startDateTime());
        product.setEndDateTime(values.endDateTime());
        if ("ART".equals(product.getType())) {
            product.setCondition(values.typeSpecificValue());
        } else {
            product.setWarranty(values.typeSpecificValue());
        }
    }

    private void applyEditedAuctionValues(AuctionListDTO auction, String type, AuctionEditValues values) {
        auction.setName(values.name());
        auction.setDescription(values.description());
        auction.setStartDateTime(values.startDateTime());
        auction.setEndDateTime(values.endDateTime());
        if ("ART".equals(type)) {
            auction.setCondition(values.typeSpecificValue());
        } else {
            auction.setWarranty(values.typeSpecificValue());
        }
    }

    private String typeSpecificLabelText(String type) {
        return switch (type) {
            case "ELECTRONICS" -> "Bảo hành (tháng)";
            case "VEHICLE" -> "Hãng xe";
            default -> "Tác giả";
        };
    }

    private String typeSpecificPromptText(String type) {
        return switch (type) {
            case "ELECTRONICS" -> "Ví dụ: 12";
            case "VEHICLE" -> "Ví dụ: Toyota";
            default -> "Nhập tên tác giả";
        };
    }

    private String typeSpecificValue(Product product, String type) {
        if ("ART".equals(type)) {
            return valueOrEmpty(product.getCondition());
        }
        return valueOrEmpty(product.getWarranty());
    }

    private String buildDateTimeString(java.time.LocalDate date, TextField timeField) {
        return buildDateTime(date, timeField).format(AUCTION_DATE_TIME_FORMATTER);
    }

    private LocalDateTime buildDateTime(java.time.LocalDate date, TextField timeField) {
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

    private LocalDateTime parseDateTimeOrDefault(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value.trim(), AUCTION_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private String escapePipe(String input) {
        if (input == null) return "";
        return input.replace("|", " ");
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void showOpenDialogError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText("Không thể mở cửa sổ chỉnh sửa phiên đấu giá");
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
        if (!response.startsWith("ERR|")) {
            return response;
        }
        String[] parts = response.split("\\|", 3);
        if (parts.length == 3) {
            return parts[2];
        }
        return response.substring(4);
    }

    private static final class AuctionEditValues {
        private final String name;
        private final String description;
        private final String startDateTime;
        private final String endDateTime;
        private final String typeSpecificValue;

        private AuctionEditValues(
                String name,
                String description,
                String startDateTime,
                String endDateTime,
                String typeSpecificValue
        ) {
            this.name = name;
            this.description = description;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
            this.typeSpecificValue = typeSpecificValue;
        }

        private String name() {
            return name;
        }

        private String description() {
            return description;
        }

        private String startDateTime() {
            return startDateTime;
        }

        private String endDateTime() {
            return endDateTime;
        }

        private String typeSpecificValue() {
            return typeSpecificValue;
        }
    }
}
