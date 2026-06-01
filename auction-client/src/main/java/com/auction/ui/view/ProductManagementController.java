package com.auction.ui.view;

import com.auction.domain.model.Product;
import com.auction.domain.model.ProductDataManager;
import com.auction.application.service.AuctionService;
import com.auction.ui.navigation.NavigationService;
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

        configureProductTableVisuals(typeCol, nameCol, priceCol, statusCol);
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
            case "RUNNING" -> "\u0110ang di\u1ec5n ra";
            case "OPEN" -> "S\u1eafp di\u1ec5n ra";
            case "FINISHED" -> "\u0110\u00e3 k\u1ebft th\u00fac";
            case "CANCELED" -> "\u0110\u00e3 h\u1ee7y";
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
        dialog.setTitle("\u0110\u0103ng s\u1ea3n ph\u1ea9m m\u1edbi");
        dialog.setHeaderText(null);

        ButtonType postButtonType = new ButtonType("\u0110\u0103ng b\u00e0i", ButtonBar.ButtonData.OK_DONE);
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
        Label conditionLabel = new Label("T\u00e1c gi\u1ea3:");
        Label warrantyLabel = new Label("Th\u00f4ng tin ph\u1ee5:");
        nameField.setPromptText("Nh\u1eadp t\u00ean s\u1ea3n ph\u1ea9m");
        priceField.setPromptText("Nh\u1eadp gi\u00e1 kh\u1edfi \u0111i\u1ec3m");
        conditionField.setPromptText("Nh\u1eadp t\u00ean t\u00e1c gi\u1ea3 (n\u1ebfu c\u00f3)");
        descriptionArea.setPromptText("Nh\u1eadp m\u00f4 t\u1ea3 chi ti\u1ebft v\u1ec1 s\u1ea3n ph\u1ea9m...");
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

        updateTypeSpecificFields(auctionTypeBox.getValue(), conditionLabel, warrantyLabel, warrantyField);
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
            }
            updateTypeSpecificFields(selectedType, conditionLabel, warrantyLabel, warrantyField);
        });

        VBox headerText = new VBox(4,
                styledLabel("\u0110\u0103ng s\u1ea3n ph\u1ea9m m\u1edbi", "product-dialog-title"),
                styledLabel("\u0110i\u1ec1n th\u00f4ng tin chi ti\u1ebft \u0111\u1ec3 t\u1ea1o phi\u00ean \u0111\u1ea5u gi\u00e1", "product-dialog-subtitle")
        );
        Label headerIcon = styledLabel("\u2696", "product-dialog-header-icon");
        HBox header = new HBox(16, headerIcon, headerText);
        header.getStyleClass().add("product-dialog-custom-header");

        VBox basicFields = new VBox(12,
                dialogField("T\u00ean s\u1ea3n ph\u1ea9m *", nameField),
                dialogField("Lo\u1ea1i auction *", auctionTypeBox),
                dialogField("Gi\u00e1 kh\u1edfi \u0111i\u1ec3m ($) *", priceField),
                dialogField(conditionLabel, conditionField)
        );
        basicFields.setPrefWidth(540);

        final byte[][] selectedImageBlob = new byte[1][];
        Label imageUploadTitle = styledLabel("Th\u00eam \u1ea3nh s\u1ea3n ph\u1ea9m", "product-dialog-upload-title");
        Label imageUploadHelp = styledLabel("K\u00e9o th\u1ea3 ho\u1eb7c nh\u1ea5n \u0111\u1ec3 ch\u1ecdn", "product-dialog-upload-help");
        VBox imageBox = new VBox(10,
                styledLabel("\u25a3", "product-dialog-upload-icon"),
                imageUploadTitle,
                imageUploadHelp,
                styledLabel("JPG, PNG (t\u1ed1i \u0111a 5MB)", "product-dialog-upload-note")
        );
        imageBox.getStyleClass().add("product-dialog-upload-box");
        imageBox.setOnMouseClicked(mouseEvent -> chooseProductImage(dialog, selectedImageBlob, imageUploadTitle, imageUploadHelp));
        HBox basicRow = new HBox(28, basicFields, imageBox);

        VBox basicSection = new VBox(16,
                sectionTitle("Th\u00f4ng tin c\u01a1 b\u1ea3n"),
                basicRow,
                dialogField("M\u00f4 t\u1ea3 chi ti\u1ebft", descriptionArea),
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
        timeGrid.add(dialogField("Ng\u00e0y b\u1eaft \u0111\u1ea7u *", startDatePicker), 0, 0);
        timeGrid.add(dialogField("Ng\u00e0y k\u1ebft th\u00fac *", endDatePicker), 1, 0);
        timeGrid.add(dialogField("Gi\u1edd b\u1eaft \u0111\u1ea7u *", startTimeField), 0, 1);
        timeGrid.add(dialogField("Gi\u1edd k\u1ebft th\u00fac *", endTimeField), 1, 1);

        VBox timeSection = new VBox(14, sectionTitle("Th\u1eddi gian \u0111\u1ea5u gi\u00e1"), timeGrid);
        timeSection.getStyleClass().add("product-dialog-section");

        VBox incrementSection = new VBox(10, dialogField("B\u01b0\u1edbc gi\u00e1 t\u1ed1i thi\u1ec3u ($) *", minIncrementField));
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

    private void chooseProductImage(
            Dialog<?> dialog,
            byte[][] selectedImageBlob,
            Label imageUploadTitle,
            Label imageUploadHelp
    ) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ch\u1ecdn \u1ea3nh s\u1ea3n ph\u1ea9m");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("\u1ea2nh JPG/PNG", "*.jpg", "*.jpeg", "*.png")
        );

        File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        if (selectedFile.length() > 5L * 1024L * 1024L) {
            showDialogError("\u1ea2nh kh\u00f4ng \u0111\u01b0\u1ee3c v\u01b0\u1ee3t qu\u00e1 5MB.");
            return;
        }

        try {
            selectedImageBlob[0] = Files.readAllBytes(selectedFile.toPath());
            imageUploadTitle.setText(selectedFile.getName());
            imageUploadHelp.setText("\u0110\u00e3 ch\u1ecdn " + selectedImageBlob[0].length / 1024 + " KB");
        } catch (IOException e) {
            showDialogError("Kh\u00f4ng th\u1ec3 \u0111\u1ecdc file \u1ea3nh: " + e.getMessage());
        }
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
        Label icon = styledLabel("\u25a3", "product-dialog-section-icon");
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
            conditionLabel.setText("T\u00e1c gi\u1ea3:");
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
        nameDialog.showAndWait().ifPresent(rawName -> {
            String newName = rawName == null ? "" : rawName.trim();
            if (newName.isEmpty()) {
                showDialogError("T\u00ean s\u1ea3n ph\u1ea9m kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng.");
                return;
            }

            String response = auctionService.renameAuction(selected.getId(), escapePipe(newName));
            if (response == null || !response.startsWith("OK|UPDATE_AUCTION|")) {
                showServerActionError("Kh\u00f4ng th\u1ec3 c\u1eadp nh\u1eadt s\u1ea3n ph\u1ea9m", response);
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
    private void handleDeleteProduct(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("X\u00f3a s\u1ea3n ph\u1ea9m");
            confirm.setHeaderText("X\u00f3a s\u1ea3n ph\u1ea9m kh\u1ecfi server?");
            confirm.setContentText(selected.getName());

            Optional<ButtonType> answer = confirm.showAndWait();
            if (answer.isEmpty() || answer.get() != ButtonType.OK) {
                return;
            }

            String response = auctionService.deleteAuction(selected.getId());
            if (response == null || !response.startsWith("OK|DELETE_AUCTION|")) {
                showServerActionError("Kh\u00f4ng th\u1ec3 x\u00f3a s\u1ea3n ph\u1ea9m", response);
                return;
            }

            ProductDataManager.getInstance().deleteProductAndAuction(selected.getId());
            myProductsTable.refresh();
        }
    }

    private void showServerActionError(String header, String response) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("L\u1ed7i server");
        alert.setHeaderText(header);
        alert.setContentText(extractServerMessage(response));
        alert.showAndWait();
    }

    private String extractServerMessage(String response) {
        if (response == null || response.isBlank()) {
            return "Server kh\u00f4ng ph\u1ea3n h\u1ed3i.";
        }
        String[] parts = response.split("\\|", 3);
        return parts.length >= 3 ? parts[2] : response;
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AuctionList.fxml"));
            // Lấy Stage từ nút bấm
            Stage stage = (Stage) backButton.getScene().getWindow();

            Scene scene = new Scene(root, 1280, 800);

            // Nạp file CSS tổng vào Scene mới
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle("UET Auction System");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "UET Auction System", 1280, 800);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/Account.fxml", "UET Auction System - Tài khoản", 1280, 800);
    }

}
