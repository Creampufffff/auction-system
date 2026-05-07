package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainApp extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        setRoot("/fxml/AuctionList.fxml", "UET Auction System");
        primaryStage.show();
    }

    public static void setRoot(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(MainApp.class.getResource(fxml));
            Scene scene = new Scene(root);

            // Ép nạp file CSS của bạn
            String css = MainApp.class.getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}