package com.auction.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Centralized navigation service to manage scene transitions and window management.
 * Eliminates duplicated switchScene() methods across controllers.
 */
public class NavigationService {

    private static NavigationService instance;
    private Stage primaryStage;

    // Default dimensions for different screen types
    private static final double DEFAULT_WIDTH = 1280;
    private static final double DEFAULT_HEIGHT = 800;
    private static final double AUTH_WIDTH = 800;
    private static final double AUTH_HEIGHT = 600;
    private static final String CSS_PATH = "/css/style.css";

    private NavigationService() {}

    public static synchronized NavigationService getInstance() {
        if (instance == null) {
            instance = new NavigationService();
        }
        return instance;
    }

    /**
     * Set the primary stage (typically called from MainApp during startup).
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Navigate to a scene with default dimensions (1280x800).
     *
     * @param fxmlPath the FXML resource path (e.g., "/fxml/AuctionList.fxml")
     * @param title    the window title
     */
    public void navigateTo(String fxmlPath, String title) {
        navigateTo(fxmlPath, title, DEFAULT_WIDTH, DEFAULT_HEIGHT, true);
    }

    /**
     * Navigate to a scene with custom dimensions.
     *
     * @param fxmlPath the FXML resource path (e.g., "/fxml/AuctionList.fxml")
     * @param title    the window title
     * @param width    the window width
     * @param height   the window height
     */
    public void navigateTo(String fxmlPath, String title, double width, double height) {
        navigateTo(fxmlPath, title, width, height, true);
    }

    /**
     * Navigate to a scene with custom dimensions and center option.
     *
     * @param fxmlPath      the FXML resource path
     * @param title         the window title
     * @param width         the window width
     * @param height        the window height
     * @param centerOnScreen whether to center the window on screen
     */
    public void navigateTo(String fxmlPath, String title, double width, double height, boolean centerOnScreen) {
        navigateTo(fxmlPath, title, width, height, centerOnScreen, true);
    }

    private void navigateTo(String fxmlPath, String title, double width, double height, boolean centerOnScreen, boolean maximize) {
        try {
            if (primaryStage == null) {
                throw new IllegalStateException("Primary stage not set. Call setPrimaryStage() first.");
            }

            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalArgumentException("FXML resource not found: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root, width, height);

            // Apply CSS if available
            URL cssUrl = getClass().getResource(CSS_PATH);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(maximize);
            if (!maximize) {
                primaryStage.setWidth(width);
                primaryStage.setHeight(height);
            }

            if (centerOnScreen) {
                primaryStage.centerOnScreen();
            }

            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error navigating to: " + fxmlPath);

            // Fallback UI so the application still opens with a visible window.
            if (primaryStage != null) {
                StackPane fallbackRoot = new StackPane(new Label("Không thể tải giao diện: " + fxmlPath));
                fallbackRoot.setStyle("-fx-padding: 24; -fx-alignment: center; -fx-background-color: white;");
                primaryStage.setTitle(title);
                primaryStage.setScene(new Scene(fallbackRoot, width, height));
                primaryStage.setMaximized(maximize);
                primaryStage.show();
            }
        }
    }

    /**
     * Navigate to an authentication screen (Login/Register) with standard dimensions.
     *
     * @param fxmlPath the FXML resource path
     * @param title    the window title
     */
    public void navigateToAuth(String fxmlPath, String title) {
        navigateTo(fxmlPath, title, AUTH_WIDTH, AUTH_HEIGHT, true, false);
    }

    /**
     * Get the currently active stage for advanced use cases.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Load FXML and return the root node without changing the scene.
     * Useful for modal/modal-like dialogs.
     *
     * @param fxmlPath the FXML resource path
     * @return the loaded root node
     */
    public Parent loadFxml(String fxmlPath) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IllegalArgumentException("FXML resource not found: " + fxmlPath);
            }
            return FXMLLoader.load(fxmlUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
