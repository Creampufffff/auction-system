package com.auction.client.controller;

import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.BidHistoryDTO;
import com.auction.client.service.AccountService;
import com.auction.client.session.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class AccountController {

    // --- Profile section ---
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label userIdLabel;

    // --- Balance section ---
    @FXML private Label balanceLabel;
    @FXML private Label balanceMessageLabel;

    // --- Deposit / Withdraw ---
    @FXML private TextField amountField;
    @FXML private Label actionMessageLabel;

    // --- Bid history table ---
    @FXML private TableView<BidHistoryDTO> bidHistoryTable;
    @FXML private TableColumn<BidHistoryDTO, String> colBidId;
    @FXML private TableColumn<BidHistoryDTO, String> colAuctionId;
    @FXML private TableColumn<BidHistoryDTO, String> colBidTime;
    @FXML private TableColumn<BidHistoryDTO, Double> colBidAmount;

    // --- Message / status ---
    @FXML private Label messageLabel;

    private final AccountService accountService = new AccountService();

    @FXML
    public void initialize() {
        // Profile
        usernameLabel.setText(SessionManager.getCurrentUsername());
        roleLabel.setText(SessionManager.getCurrentUserRole());
        userIdLabel.setText("ID: " + SessionManager.getCurrentUserId());

        // Bid history table columns
        colBidId.setCellValueFactory(new PropertyValueFactory<>("bidId"));
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colBidAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        colBidTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));

        Platform.runLater(() -> {
            loadBalance();
            loadBidHistory();
        });
    }

    private void loadBalance() {
        BalanceResponseDTO response = accountService.getBalance();
        if (response != null && response.getUserId() != null) {
            balanceLabel.setText("$" + String.format("%.2f", response.getBalance()));
            if (response.getBalance() < 100) {
                balanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;");
            } else {
                balanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2d6cdf;");
            }
            if (balanceMessageLabel != null) balanceMessageLabel.setText(response.getMessage());
        } else {
            balanceLabel.setText("--");
            if (balanceMessageLabel != null) balanceMessageLabel.setText("Không thể tải số dư.");
        }
    }

    private void loadBidHistory() {
        bidHistoryTable.setItems(
                FXCollections.observableArrayList(accountService.getBidHistory())
        );
        bidHistoryTable.refresh();
    }

    @FXML
    private void handleDeposit(ActionEvent event) {
        double amount = parseAmount();
        if (amount <= 0) return;

        BalanceResponseDTO response = accountService.deposit(amount);
        if (response != null && response.getUserId() != null) {
            balanceLabel.setText("$" + String.format("%.2f", response.getBalance()));
            setActionMessage("Nạp tiền thành công: $" + String.format("%.2f", amount), false);
        } else {
            setActionMessage(response != null ? response.getMessage() : "Nạp tiền thất bại.", true);
        }
        amountField.clear();
    }

    @FXML
    private void handleWithdraw(ActionEvent event) {
        double amount = parseAmount();
        if (amount <= 0) return;

        BalanceResponseDTO response = accountService.withdraw(amount);
        if (response != null && response.getUserId() != null) {
            balanceLabel.setText("$" + String.format("%.2f", response.getBalance()));
            setActionMessage("Rút tiền thành công: $" + String.format("%.2f", amount), false);
        } else {
            setActionMessage(response != null ? response.getMessage() : "Rút tiền thất bại.", true);
        }
        amountField.clear();
    }

    @FXML
    private void handleRefreshHistory(ActionEvent event) {
        loadBidHistory();
        if (messageLabel != null) messageLabel.setText("Đã làm mới lịch sử đặt giá.");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        switchScene("/fxml/AuctionList.fxml", "UET Auction System - Dashboard", 1040, 660);
    }

    // --- Helpers ---

    private double parseAmount() {
        try {
            double amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) {
                setActionMessage("Số tiền phải lớn hơn 0.", true);
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            setActionMessage("Vui lòng nhập số tiền hợp lệ.", true);
            return -1;
        }
    }

    private void setActionMessage(String text, boolean isError) {
        if (actionMessageLabel != null) {
            actionMessageLabel.setText(text);
            actionMessageLabel.setStyle(isError
                    ? "-fx-text-fill: #d92d20; -fx-font-weight: bold;"
                    : "-fx-text-fill: #12a594; -fx-font-weight: bold;");
        }
    }

    private void switchScene(String fxmlPath, String title, double width, double height) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Scene scene = new Scene(root, width, height);
            String css = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}