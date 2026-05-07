package com.auction.client.controller;

import com.app.common.dto.RegisterResponseDTO;
import com.auction.client.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 1. Kiểm tra trống
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng điền đầy đủ các trường thông tin.");
            return;
        }

        // 2. Kiểm tra định dạng Username
        if (!username.matches("^[a-zA-Z0-9_]{3,18}$")) {
            showError("Username từ 3-18 ký tự, không chứa ký tự đặc biệt.");
            return;
        }

        // 3. Kiểm tra khớp mật khẩu
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không trùng khớp.");
            return;
        }

        new Thread(() -> {
            RegisterResponseDTO response = authService.register(username, password, email);
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    messageLabel.setStyle("-fx-text-fill: #2ecc71;");
                    messageLabel.setText("Đăng ký thành công! Đang chuyển hướng...");
                    usernameField.setDisable(true);
                    emailField.setDisable(true);

                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(this::handleBackToLogin);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    String message = response != null ? response.getMessage() : "Server không phản hồi.";
                    showError(message);
                }
            });
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Đăng nhập - UET Auction System");
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) {
            e.printStackTrace();
            // Nếu lỗi nạp file Login, hiện thông báo lên messageLabel
            if (messageLabel != null) {
                messageLabel.setText("Không tìm thấy file Login.fxml");
            }
        }
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: #e74c3c;"); // Màu đỏ lỗi
        messageLabel.setText(message);
    }
}