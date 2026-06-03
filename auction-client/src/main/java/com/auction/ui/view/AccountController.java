package com.auction.ui.view;

import com.app.common.dto.BalanceResponseDTO;
import com.auction.application.service.AccountService;
import com.auction.domain.model.ProductDataManager;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
    @FXML private Button wonItemsSidebarButton;
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

        Platform.runLater(() -> {
            loadBalanceFromSession();
            refreshBalanceAsync();
        });
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
        if (wonItemsSidebarButton != null) {
            boolean isBidder = SessionManager.hasRole("Bidder");
            wonItemsSidebarButton.setVisible(isBidder);
            wonItemsSidebarButton.setManaged(isBidder);
        }
    }

    private void loadBalanceFromSession() {
        ProductDataManager.getInstance().syncBalanceFromSession();
        updateBalanceLabels(SessionManager.getCurrentUserBalance());
        if (balanceMessageLabel != null) {
            balanceMessageLabel.setText("So du gan nhat");
        }
    }

    private void refreshBalanceAsync() {
        if (balanceMessageLabel != null) {
            balanceMessageLabel.setText("Dang cap nhat so du...");
        }

        Task<BalanceResponseDTO> task = new Task<>() {
            @Override
            protected BalanceResponseDTO call() {
                return accountService.getBalance();
            }
        };

        task.setOnSucceeded(event -> {
            BalanceResponseDTO response = task.getValue();
            if (response != null && response.getUserId() != null) {
                applyBalanceResponse(response);
                return;
            }
            if (balanceMessageLabel != null) {
                balanceMessageLabel.setText("Dang hien thi so du gan nhat.");
            }
        });
        task.setOnFailed(event -> {
            if (balanceMessageLabel != null) {
                balanceMessageLabel.setText("Dang hien thi so du gan nhat.");
            }
        });

        Thread thread = new Thread(task, "account-balance-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyBalanceResponse(BalanceResponseDTO response) {
        updateBalanceLabels(response.getBalance());
        if (balanceMessageLabel != null) {
            balanceMessageLabel.setText(response.getMessage());
        }
    }

    @FXML
    private void handleDeposit(ActionEvent event) {
        double amount = parseAmount();
        if (amount <= 0) return;

        BalanceResponseDTO response = accountService.deposit(amount);
        if (response != null && response.getUserId() != null) {
            updateBalanceLabels(response.getBalance());
            setActionMessage("Nap tien thanh cong: $" + String.format("%.2f", amount), false);
        } else {
            setActionMessage(response != null ? response.getMessage() : "Nap tien that bai.", true);
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
            setActionMessage("Rut tien thanh cong: $" + String.format("%.2f", amount), false);
        } else {
            setActionMessage(response != null ? response.getMessage() : "Rut tien that bai.", true);
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
                setActionMessage("Chi seller moi duoc quan ly san pham.", true);
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/ProductManagement.fxml", "Quan ly san pham", 1280, 800);
    }

    @FXML
    private void handleSidebarBidHistory(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            if (actionMessageLabel != null) {
                setActionMessage("Quyen truy cap chi danh cho nguoi dau gia.", true);
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/BidHistory.fxml", "Lich su dat gia", 1280, 800);
    }

    @FXML
    private void handleSidebarWonItems(ActionEvent event) {
        if (!SessionManager.hasRole("Bidder")) {
            if (actionMessageLabel != null) {
                setActionMessage("Quyen truy cap chi danh cho nguoi dau gia.", true);
            }
            return;
        }
        NavigationService.getInstance().navigateTo("/fxml/WonItems.fxml", "Kho vat pham", 1280, 800);
    }

    @FXML
    private void handleCurrentAuctions(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "Auction System", 1280, 800);
    }

    @FXML
    private void handleSidebarAccount(ActionEvent event) {
        NavigationService.getInstance().navigateTo("/fxml/Account.fxml", "Tai khoan", 1280, 800);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.clear();
        ProductDataManager.getInstance().resetSessionState();
        NavigationService.getInstance().navigateToAuth("/fxml/Login.fxml", "Dang nhap");
    }

    private void updateBalanceLabels(double balance) {
        ProductDataManager.getInstance().setUserBalance(balance);
        String balanceText = "$" + String.format("%.2f", balance);
        balanceLabel.setText(balanceText);
        if (sidebarBalanceLabel != null) {
            sidebarBalanceLabel.setText("Vi: " + balanceText);
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
                setActionMessage("So tien phai lon hon 0.", true);
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            setActionMessage("Vui long nhap so tien hop le.", true);
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
