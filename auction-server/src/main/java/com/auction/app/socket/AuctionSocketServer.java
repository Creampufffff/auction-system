package com.auction.app.socket;

import com.auction.app.service.AuctionService;
import com.auction.app.service.BidService;
import com.auction.app.service.ItemService;
import com.auction.app.service.UserService;
import com.auction.app.socket.observer.Observer;
import com.auction.app.socket.observer.Subject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionSocketServer implements Subject<String> {
    private final int port;
    private final UserService userService;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final BidService bidService;

    private final Set<Observer<String>> observers = ConcurrentHashMap.newKeySet();

    public AuctionSocketServer(int port, UserService userService, ItemService itemService, AuctionService auctionService, BidService bidService) {
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
                // Mỗi client kết nối sẽ được xử lý trên một luồng riêng.
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, userService, itemService, auctionService, bidService, this);
                new Thread(handler).start();
            }
        }
    }

    @Override
    public void registerObserver(Observer<String> observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer<String> observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String event) {
        observers.forEach(observer -> observer.onEvent(event));
    }

    // Giữ tên hàm cũ để tránh đổi nhiều vị trí gọi trong handler.
    public void broadcast(String message) {
        notifyObservers(message);
    }
}
