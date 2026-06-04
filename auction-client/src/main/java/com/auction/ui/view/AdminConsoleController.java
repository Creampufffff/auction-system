package com.auction.ui.view;

import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.BidHistoryDTO;
import com.auction.application.service.AdminService;
import com.auction.application.service.SocketClientService;
import com.auction.domain.model.AdminDashboardStats;
import com.auction.domain.model.AdminUserRow;
import com.auction.domain.model.ProductDataManager;
import com.auction.shared.session.SessionManager;
import com.auction.ui.navigation.NavigationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class AdminConsoleController {
    @FXML private Button dashboardButton;
    @FXML private Button usersButton;
    @FXML private Button auctionsButton;
    @FXML private Button bidsButton;
    @FXML private Label adminNameLabel;
    @FXML private Label messageLabel;

    @FXML private Pane dashboardPane;
    @FXML private Pane usersPane;
    @FXML private Pane auctionsPane;
    @FXML private Pane bidsPane;

    @FXML private Label totalUsersLabel;
    @FXML private Label totalBiddersLabel;
    @FXML private Label totalSellersLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label runningAuctionsLabel;
    @FXML private Label finishedAuctionsLabel;
    @FXML private Label totalBidsLabel;

    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> roleFilterBox;
    @FXML private TableView<AdminUserRow> userTable;
    @FXML private TableColumn<AdminUserRow, String> userIdColumn;
    @FXML private TableColumn<AdminUserRow, String> usernameColumn;
    @FXML private TableColumn<AdminUserRow, String> emailColumn;
    @FXML private TableColumn<AdminUserRow, String> roleColumn;
    @FXML private TableColumn<AdminUserRow, Double> balanceColumn;
    @FXML private TableColumn<AdminUserRow, Double> heldBalanceColumn;

    @FXML private TextField auctionSearchField;
    @FXML private ComboBox<String> auctionStatusFilterBox;
    @FXML private TableView<AuctionListDTO> auctionTable;
    @FXML private TableColumn<AuctionListDTO, String> auctionIdColumn;
    @FXML private TableColumn<AuctionListDTO, String> itemNameColumn;
    @FXML private TableColumn<AuctionListDTO, String> itemTypeColumn;
    @FXML private TableColumn<AuctionListDTO, Object> auctionStatusColumn;
    @FXML private TableColumn<AuctionListDTO, Double> currentPriceColumn;
    @FXML private TableColumn<AuctionListDTO, String> startDateColumn;
    @FXML private TableColumn<AuctionListDTO, String> endDateColumn;

    @FXML private TextField bidSearchField;
    @FXML private TableView<BidHistoryDTO> bidTable;
    @FXML private TableColumn<BidHistoryDTO, String> bidIdColumn;
    @FXML private TableColumn<BidHistoryDTO, String> bidAuctionIdColumn;
    @FXML private TableColumn<BidHistoryDTO, String> bidItemColumn;
    @FXML private TableColumn<BidHistoryDTO, String> bidderColumn;
    @FXML private TableColumn<BidHistoryDTO, Double> bidAmountColumn;
    @FXML private TableColumn<BidHistoryDTO, String> bidTimeColumn;

    private final AdminService adminService = new AdminService();
    private final ObservableList<AdminUserRow> allUsers = FXCollections.observableArrayList();
    private final ObservableList<AuctionListDTO> allAuctions = FXCollections.observableArrayList();
    private final ObservableList<BidHistoryDTO> allBids = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configureHeader();
        configureTables();
        configureFilters();
        refreshAll();
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        showPane(dashboardPane, dashboardButton);
    }

    @FXML
    private void showUsers() {
        showPane(usersPane, usersButton);
    }

    @FXML
    private void showAuctions() {
        showPane(auctionsPane, auctionsButton);
    }

    @FXML
    private void showBids() {
        showPane(bidsPane, bidsButton);
    }

    @FXML
    private void handleRefresh() {
        refreshAll();
    }

    @FXML
    private void handleLogout() {
        SocketClientService.stopRealtimeListener();
        SessionManager.clear();
        ProductDataManager.getInstance().resetSessionState();
        NavigationService.getInstance().navigateToAuth("/fxml/Login.fxml", "Đăng nhập");
    }

    private void configureHeader() {
        String username = SessionManager.getCurrentUsername();
        if (adminNameLabel != null) {
            adminNameLabel.setText(username == null || username.isBlank() ? "Admin" : username);
        }
    }

    private void configureTables() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        heldBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("heldBalance"));

        auctionIdColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        itemTypeColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        auctionStatusColumn.setCellValueFactory(new PropertyValueFactory<>("auctionStatus"));
        currentPriceColumn.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDateTime"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDateTime"));

        bidIdColumn.setCellValueFactory(new PropertyValueFactory<>("bidId"));
        bidAuctionIdColumn.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        bidItemColumn.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        bidderColumn.setCellValueFactory(new PropertyValueFactory<>("bidderUsername"));
        bidAmountColumn.setCellValueFactory(new PropertyValueFactory<>("bidAmount"));
        bidTimeColumn.setCellValueFactory(new PropertyValueFactory<>("bidTime"));

        userTable.getStyleClass().add("auction-table");
        auctionTable.getStyleClass().add("auction-table");
        bidTable.getStyleClass().add("auction-table");
    }

    private void configureFilters() {
        roleFilterBox.setItems(FXCollections.observableArrayList("ALL", "ADMIN", "SELLER", "BIDDER"));
        roleFilterBox.getSelectionModel().selectFirst();

        auctionStatusFilterBox.setItems(FXCollections.observableArrayList("ALL", "OPEN", "RUNNING", "FINISHED", "CANCELED"));
        auctionStatusFilterBox.getSelectionModel().selectFirst();

        userSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyUserFilters());
        roleFilterBox.valueProperty().addListener((obs, oldValue, newValue) -> applyUserFilters());
        auctionSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyAuctionFilters());
        auctionStatusFilterBox.valueProperty().addListener((obs, oldValue, newValue) -> applyAuctionFilters());
        bidSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyBidFilters());
    }

    private void refreshAll() {
        try {
            AdminDashboardStats stats = adminService.getDashboardStats();
            updateStats(stats);

            allUsers.setAll(adminService.getUsers());
            allAuctions.setAll(adminService.getAuctions());
            allBids.setAll(adminService.getBids());

            applyUserFilters();
            applyAuctionFilters();
            applyBidFilters();
            showMessage("Đã tải dữ liệu admin.");
        } catch (Exception e) {
            showMessage("Không thể tải dữ liệu admin: " + e.getMessage());
        }
    }

    private void updateStats(AdminDashboardStats stats) {
        totalUsersLabel.setText(String.valueOf(stats.getTotalUsers()));
        totalBiddersLabel.setText(String.valueOf(stats.getTotalBidders()));
        totalSellersLabel.setText(String.valueOf(stats.getTotalSellers()));
        totalAuctionsLabel.setText(String.valueOf(stats.getTotalAuctions()));
        runningAuctionsLabel.setText(String.valueOf(stats.getRunningAuctions()));
        finishedAuctionsLabel.setText(String.valueOf(stats.getFinishedAuctions()));
        totalBidsLabel.setText(String.valueOf(stats.getTotalBids()));
    }

    private void applyUserFilters() {
        String keyword = normalize(userSearchField.getText());
        String role = roleFilterBox.getValue();
        Predicate<AdminUserRow> predicate = user -> {
            boolean matchesKeyword = keyword.isBlank()
                    || normalize(user.getUsername()).contains(keyword)
                    || normalize(user.getEmail()).contains(keyword)
                    || normalize(user.getUserId()).contains(keyword);
            boolean matchesRole = role == null || "ALL".equals(role) || role.equalsIgnoreCase(user.getRole());
            return matchesKeyword && matchesRole;
        };
        userTable.setItems(filtered(allUsers, predicate));
    }

    private void applyAuctionFilters() {
        String keyword = normalize(auctionSearchField.getText());
        String status = auctionStatusFilterBox.getValue();
        Predicate<AuctionListDTO> predicate = auction -> {
            String auctionStatus = auction.getAuctionStatus() == null ? "" : auction.getAuctionStatus().name();
            boolean matchesKeyword = keyword.isBlank()
                    || normalize(auction.getAuctionId()).contains(keyword)
                    || normalize(auction.getName()).contains(keyword)
                    || normalize(auction.getItemType()).contains(keyword);
            boolean matchesStatus = status == null || "ALL".equals(status) || status.equalsIgnoreCase(auctionStatus);
            return matchesKeyword && matchesStatus;
        };
        auctionTable.setItems(filtered(allAuctions, predicate));
    }

    private void applyBidFilters() {
        String keyword = normalize(bidSearchField.getText());
        Predicate<BidHistoryDTO> predicate = bid -> keyword.isBlank()
                || normalize(bid.getBidId()).contains(keyword)
                || normalize(bid.getAuctionId()).contains(keyword)
                || normalize(bid.getItemName()).contains(keyword)
                || normalize(bid.getBidderUsername()).contains(keyword);
        bidTable.setItems(filtered(allBids, predicate));
    }

    private <T> ObservableList<T> filtered(List<T> source, Predicate<T> predicate) {
        ObservableList<T> result = FXCollections.observableArrayList();
        for (T item : source) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private void showPane(Pane activePane, Button activeButton) {
        dashboardPane.setVisible(activePane == dashboardPane);
        dashboardPane.setManaged(activePane == dashboardPane);
        usersPane.setVisible(activePane == usersPane);
        usersPane.setManaged(activePane == usersPane);
        auctionsPane.setVisible(activePane == auctionsPane);
        auctionsPane.setManaged(activePane == auctionsPane);
        bidsPane.setVisible(activePane == bidsPane);
        bidsPane.setManaged(activePane == bidsPane);

        dashboardButton.getStyleClass().remove("active-tab");
        usersButton.getStyleClass().remove("active-tab");
        auctionsButton.getStyleClass().remove("active-tab");
        bidsButton.getStyleClass().remove("active-tab");
        activeButton.getStyleClass().add("active-tab");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }
}
