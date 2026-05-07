package com.auction.client.model;

import com.app.common.dto.AuctionListDTO;
import com.app.common.enums.Status;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ProductDataManager {
    private static ProductDataManager instance;
    private final ObservableList<Product> myProductList;
    private final ObservableList<AuctionListDTO> serverAuctionList;
    private AuctionListDTO selectedAuction;

    private ProductDataManager() {
        myProductList = FXCollections.observableArrayList();
        serverAuctionList = FXCollections.observableArrayList();
    }

    public static ProductDataManager getInstance() {
        if (instance == null) {
            instance = new ProductDataManager();
        }
        return instance;
    }

    public ObservableList<Product> getProductList() {
        return myProductList;
    }

    public ObservableList<AuctionListDTO> getServerAuctionList() {
        return serverAuctionList;
    }

    public void pushToGlobalAuction(Product p) {
        // Khởi tạo DTO bằng Constructor đầy đủ từ Lombok
        AuctionListDTO dto = new AuctionListDTO(
                p.getId(),           // auctionId
                "ITEM-" + p.getId(), // itemId
                p.getName(),         // name
                p.getPrice(),        // currentPrice
                Status.OPEN,         // auctionStatus
                p.getCondition(),    // condition
                p.getDescription(),  // description
                p.getWarranty()      // warranty
        );

        boolean exists = serverAuctionList.stream()
                .anyMatch(a -> a.getAuctionId().equals(p.getId()));

        if (!exists) {
            serverAuctionList.add(dto);
        }
    }

    public AuctionListDTO getSelectedAuction() {
        return selectedAuction;
    }

    public void setSelectedAuction(AuctionListDTO auction) {
        this.selectedAuction = auction;
    }
}