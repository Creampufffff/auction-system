package com.auction.app.socket;

import com.auction.app.service.AuctionService;
import com.auction.app.service.BidService;
import com.auction.app.service.ItemService;
import com.auction.app.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionSocketServer {
    private final int port;
    private final UserService userService;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final BidService bidService;

    public AuctionSocketServer(
            int port,
            UserService userService,
            ItemService itemService,
            AuctionService auctionService,
            BidService bidService
    ) {
        this.port = port;
        this.userService = userService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.bidService = bidService;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(
                        clientSocket,
                        userService,
                        itemService,
                        auctionService,
                        bidService
                );
                new Thread(handler).start();
            }
        }
    }
}
