package com.auction.client.controller;

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

        // 4. Giả lập xử lý (Bỏ qua gọi Server để tránh ConnectException)
        messageLabel.setStyle("-fx-text-fill: #2ecc71;"); // Màu xanh lá thành công
        messageLabel.setText("Đăng ký thành công! Đang chuyển hướng...");

        // Disable nút bấm để tránh người dùng nhấn liên tục
        usernameField.setDisable(true);
        emailField.setDisable(true);

        // Đợi 1.5 giây rồi tự động chuyển về màn hình Login
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                Platform.runLater(this::handleBackToLogin);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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