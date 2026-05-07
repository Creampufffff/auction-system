package com.auction.client.controller;

import com.app.common.dto.RegisterResponseDTO;
import com.auction.client.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username == null || username.trim().isEmpty()) {
            showError("Vui lòng nhập username.");
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            showError("Vui lòng nhập email.");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            showError("Vui lòng nhập password.");
            return;
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            showError("Vui lòng nhập lại password.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu nhập lại không khớp.");
            return;
        }

        RegisterResponseDTO registerResponse = authService.register(username, password, email);

        if (registerResponse != null && registerResponse.isSuccess()) {
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Đăng ký thành công. Hãy quay lại đăng nhập.");
        } else {
            String message = registerResponse == null ? "Đăng ký thất bại." : registerResponse.getMessage();
            showError(message);
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/Login.fxml")
            );

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Đăng nhập vào hệ thống đấu giá");
            stage.setScene(new Scene(root, 800, 600));

        } catch (Exception e) {
            e.printStackTrace();
            showError("Không thể quay lại màn hình đăng nhập.");
        }
    }

    private void showError(String message) {
        messageLabel.setStyle("-fx-text-fill: red;");
        messageLabel.setText(message);
    }
}
