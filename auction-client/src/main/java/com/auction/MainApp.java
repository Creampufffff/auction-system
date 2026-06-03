package com.auction;

import javafx.application.Application;
import javafx.stage.Stage;
import com.auction.ui.navigation.NavigationService;
import com.auction.application.service.PeriodicUpdateService;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        // 1. Nạp icon cho Stage ngay từ đầu để mọi màn hình sau này dùng chung
        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/img.png") // Đường dẫn file ảnh trong resources
            ));
        } catch (Exception e) {
            System.err.println("Không thể nạp icon ứng dụng: " + e.getMessage());
        }

        // 2. Định hướng luồng chạy và hiển thị màn hình Login
        NavigationService.getInstance().setPrimaryStage(stage);
        NavigationService.getInstance().navigateToAuth("/fxml/Login.fxml", "Auction System");

        // Dừng tất cả periodic tasks khi application đóng
        stage.setOnCloseRequest(event -> {
            System.out.println("[MainApp] Shutting down application...");
            PeriodicUpdateService.getInstance().stopAllTimers();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}