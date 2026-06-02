package com.auction.ui.view;

import com.app.common.dto.BalanceResponseDTO;
import com.auction.application.service.AccountService;
import com.auction.domain.model.ProductDataManager;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AccountController {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label userIdLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label sidebarBalanceLabel;
    @FXML private Button productsSidebarButton;
    @FXML private Button bidHistorySidebarButton;

    @FXML private Label balanceLabel;
    @FXML private Label balanceMessageLabel;

    @FXML private TextField amountField;
    @FXML private Label actionMessageLabel;

    private final AccountService accountService = new AccountService();

    @FXML
    public void initialize() {
        usernameLabel.setText(SessionManager.getCurrentUsername());
        roleLabel.setText(SessionManager.getCurrentUserRole());
        userIdLabel.setText("ID: " + SessionManager.getCurrentUserId());
        if (sidebarRoleLabel != null) {
            sidebarRoleLabel.setText("Role: " + SessionManager.getCurrentUserRole());
        }
        configureSidebarForRole();

        Platform.runLater(this::loadBalance);
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
    private void handleBack(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "Dashboard", 1280, 800);
    }

    @FXML
    private void handleSidebarProducts(ActionEvent event) {
        if (!SessionManager.hasRole("Seller")) {
            if (actionMessageLabel != null) {
                setActionMessage("Chỉ seller mới được quản lý sản phẩm.", true);
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "Quản lý sản phẩm", 1280, 800);
    }

    @FXML
    private void handleSidebarBidHistory(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            if (actionMessageLabel != null) {
                setActionMessage("Quyền truy cập chỉ dành cho người đấu giá.", true);
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/BidHistory.fxml", "Lịch sử đặt giá", 1280, 800);
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "Auction System", 1280, 800);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/Account.fxml", "Tài khoản", 1280, 800);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        ProductDataManager.getInstance().resetSessionState();
        NavigationService.getInstance().navigateToAuth("/fxml/Login.fxml", "Đăng nhập");
    }

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