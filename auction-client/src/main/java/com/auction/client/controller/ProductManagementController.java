package com.auction.client.controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class ProductManagementController {
    @FXML private TableView<?> myProductsTable;

    @FXML public void initialize() {}
    @FXML private void handleAddProduct(ActionEvent event) {}
    @FXML private void handleEditProduct(ActionEvent event) {}
    @FXML private void handleDeleteProduct(ActionEvent event) {}
}