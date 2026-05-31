package com.auction.ui.view;

import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.BidHistoryDTO;
import com.auction.domain.model.ProductDataManager;
import com.auction.application.service.AccountService;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;

public class AccountController {

    // --- Profile section ---
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label userIdLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label sidebarBalanceLabel;
    @FXML private Button productsSidebarButton;

    // --- Balance section ---
    @FXML private Label balanceLabel;
    @FXML private Label balanceMessageLabel;

    // --- Deposit / Withdraw ---
    @FXML private TextField amountField;
    @FXML private Label actionMessageLabel;

    // --- Bid history table ---
    @FXML private VBox bidHistoryCard;
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
        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText("Role: " + SessionManager.getCurrentUserRole());
        }
        configureSidebarForRole();
        configureBidHistoryForRole();

        // Bid history table columns
        if (bidHistoryTable != null) {
            colBidId.setText("Lo\u1ea1i s\u1ea3n ph\u1ea9m");
            colAuctionId.setText("T\u00ean s\u1ea3n ph\u1ea9m");
            colBidId.setCellValueFactory(new PropertyValueFactory<>("itemType"));
            colAuctionId.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            colBidAmount.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
            colBidTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        }

        Platform.runLater(() -> {
            loadBalance();
            if (SessionManager.hasRole("Bidder")) {
                loadBidHistory();
            }
        });
    }

    private void configureSidebarForRole() {
        if (productsSidebarButton != null) {
            boolean isSeller = SessionManager.hasRole("Seller");
            productsSidebarButton.setVisible(isSeller);
            productsSidebarButton.setManaged(isSeller);
        }
    }

    private void configureBidHistoryForRole() {
        if (bidHistoryCard != null) {
            boolean isBidder = SessionManager.hasRole("Bidder");
            bidHistoryCard.setVisible(isBidder);
            bidHistoryCard.setManaged(isBidder);
        }
    }

    private void loadBalance() {
        BalanceResponseDTO response = accountService.getBalance();
        if (response != null && response.getUserId() != null) {
            double balance = response.getBalance();
            ProductDataManager.getInstance().setUserBalance(balance);
            String balanceText = "$" + String.format("%.2f", balance);
            balanceLabel.setText(balanceText);
            if (sidebarBalanceLabel != null) {
                sidebarBalanceLabel.setText("Ví: " + balanceText);
            }
            if (balance < 100) {
                balanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;");
                if (sidebarBalanceLabel != null) {
                    sidebarBalanceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;");
                }
            } else {
                balanceLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2d6cdf;");
                if (sidebarBalanceLabel != null) {
                    sidebarBalanceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
                }
            }
            if (balanceMessageLabel != null) balanceMessageLabel.setText(response.getMessage());
        } else {
            balanceLabel.setText("--");
            if (sidebarBalanceLabel != null) {
                sidebarBalanceLabel.setText("Ví: --");
            }
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
            updateBalanceLabels(response.getBalance());
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
            updateBalanceLabels(response.getBalance());
            setActionMessage("Rút tiền thành công: $" + String.format("%.2f", amount), false);
        } else {
            setActionMessage(response != null ? response.getMessage() : "Rút tiền thất bại.", true);
        }
        amountField.clear();
    }

    @FXML
    private void handleRefreshHistory(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            return;
        }
        loadBidHistory();
        if (messageLabel != null) messageLabel.setText("Đã làm mới lịch sử đặt giá.");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "UET Auction System - Dashboard", 1040, 660);
    }

    @FXML
    private void handleSidebarProducts(ActionEvent event) {
        if (!SessionManager.hasRole("Seller")) {
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "Quản lý sản phẩm", 1040, 660);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        ProductDataManager.getInstance().resetSessionState();
        NavigationService.getInstance().navigateToAuth("/fxml/Login.fxml", "Đăng nhập");
    }

    // --- Helpers ---

    private void updateBalanceLabels(double balance) {
        ProductDataManager.getInstance().setUserBalance(balance);
        String balanceText = "$" + String.format("%.2f", balance);
        balanceLabel.setText(balanceText);
        if (sidebarBalanceLabel != null) {
            sidebarBalanceLabel.setText("Ví: " + balanceText);
            sidebarBalanceLabel.setStyle(balance < 100
                    ? "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;"
                    : "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        }
        balanceLabel.setStyle(balance < 100
                ? "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ff4d4d;"
                : "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2d6cdf;");
    }

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
}

