package com.auction.app.controller;

import com.app.common.dto.CreateAuctionRequestDTO;
import com.app.common.dto.ApiResponseDTO;
import com.auction.app.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class AuctionControllerTest {
    private AuctionService auctionService;
    private AuctionController controller;

    @BeforeEach
    void setUp() {
        auctionService = Mockito.mock(AuctionService.class);
        controller = new AuctionController(auctionService);
    }

    @Test
    void createAuction_success() {
        CreateAuctionRequestDTO dto = new CreateAuctionRequestDTO(
                "Mona Lisa",
                "A famous painting",
                "New",
                "None",
                1000.0,
                10.0,
                "2026-06-01T10:00:00",
                "2026-06-01T12:00:00",
                "seller-1",
                "ART"
        );

        // auctionService.saveAuction should not throw
        Mockito.doNothing().when(auctionService).saveAuction(Mockito.any());

        ApiResponseDTO resp = controller.createAuction(dto);
        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertTrue(resp.getMessage().toLowerCase().contains("created"));
    }

    @Test
    void startAuction_failure() {
        String auctionId = "nonexistent";
        Mockito.doThrow(new IllegalArgumentException("Auction not found")).when(auctionService).startAuction(auctionId);

        ApiResponseDTO resp = controller.startAuction(auctionId);
        assertNotNull(resp);
        assertFalse(resp.isSuccess());
        assertTrue(resp.getMessage().toLowerCase().contains("error"));
    }
}

