package com.auction.client.controller;

import com.app.common.dto.LoginResponseDTO;
import com.auction.client.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty()) {
            messageLabel.setText("Vui lòng nhập username!");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            messageLabel.setText("Vui lòng nhập password!");
            return;
        }

        LoginResponseDTO loginResponse = authService.login(username, password);

        if (loginResponse != null) {
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText("Đăng nhập thành công!");

            // đăng nhập thành công thì chuyển sang AuctionList
            switchToAuctionList();
        } else {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Sai username hoặc password!");
        }
    }

    private void switchToAuctionList() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/AuctionList.fxml")
            );

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Hệ thống đấu giá");
            stage.setScene(new Scene(root, 800, 600));

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Không thể mở màn hình danh sách đấu giá");
        }
    }

    @FXML
    private void handleOpenRegister() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/Register.fxml")
            );

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Đăng ký tài khoản");
            stage.setScene(new Scene(root, 800, 600));

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Không thể mở màn hình đăng ký.");
        }
    }


}
