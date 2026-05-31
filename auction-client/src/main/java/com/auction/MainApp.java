package com.auction;

import javafx.application.Application;
import javafx.stage.Stage;
import com.auction.ui.navigation.NavigationService;
import com.auction.application.service.PeriodicUpdateService;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        NavigationService.getInstance().setPrimaryStage(stage);
        NavigationService.getInstance().navigateToAuth("/fxml/Login.fxml", "UET Auction System");

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