package com.auction.ui.view;

import com.app.common.dto.LoginResponseDTO;
import com.auction.domain.model.ProductDataManager;
import com.auction.application.service.AuthService;
import com.auction.application.service.SocketClientService;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private final AuthService authService = new AuthService();

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label messageLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 1. Kiểm tra rỗng
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            return;
        }

        LoginResponseDTO response;
        try {
            // Open an authenticated session socket and login on that same connection
            response = SocketClientService.openSessionAndLogin(username, password);
        } catch (IllegalStateException ex) {
            showError("Không thể kết nối tới server.");
            return;
        }

        if (response == null || !response.isSuccess()) {
            showError("Sai tên đăng nhập hoặc mật khẩu.");
            return;
        }

        SessionManager.setCurrentUser(response);
        ProductDataManager.getInstance().setUserBalance(response.getBalance());


        messageLabel.setStyle("-fx-text-fill: #2ecc71;");
        messageLabel.setText("Đăng nhập thành công! Đang vào hệ thống...");

        // Chuyển sang Dashboard (Kích thước chuẩn cho bảng đấu giá)
        NavigationService.getInstance().navigateTo("/fxml/AuctionList.fxml", "UET Auction System - Dashboard", 1040, 660);
    }

    @FXML
    private void handleOpenRegister(ActionEvent event) {
        // Chuyển sang màn hình Register (Kích thước 800x600)
        NavigationService.getInstance().navigateToAuth("/fxml/Register.fxml", "Đăng ký tài khoản");
    }

    /**
     * Hàm hỗ trợ chuyển cảnh linh hoạt kích thước
     */

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        messageLabel.setText(message);
    }
}
