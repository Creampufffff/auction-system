package com.auction.app.socket;

import com.app.common.entity.Auction;
import com.app.common.entity.BidTransaction;
import com.app.common.entity.Bidder;
import com.app.common.entity.Item;
import com.app.common.entity.Seller;
import com.app.common.entity.User;
import com.app.common.exception.AuctionClosedException;
import com.app.common.exception.AuctionNotFoundException;
import com.app.common.exception.InsufficientBalanceException;
import com.app.common.exception.InvalidBidException;
import com.app.common.exception.UserAuthException;
import com.auction.app.factory.ArtItemFactory;
import com.auction.app.factory.ItemFactory;
import com.auction.app.service.AuctionService;
import com.auction.app.service.BidService;
import com.auction.app.service.ItemService;
import com.auction.app.service.UserService;
import com.auction.app.socket.observer.SocketClientObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserService userService;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final AuctionSocketServer socketServer;
    private final ItemFactory artItemFactory;

    public ClientHandler(
            Socket socket,
            UserService userService,
            ItemService itemService,
            AuctionService auctionService,
            BidService bidService,
            AuctionSocketServer socketServer
    ) {
        this.socket = socket;
        this.userService = userService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.socketServer = socketServer;
        this.artItemFactory = new ArtItemFactory();
    }

    @Override
    public void run() {
        PrintWriter writer = null;
        SocketClientObserver clientObserver = null;
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true)
        ) {
            writer = socketWriter;
            // Mỗi client socket đóng vai trò một Observer của luồng sự kiện realtime.
            clientObserver = new SocketClientObserver(writer);
            socketServer.registerObserver(clientObserver);
            writer.println("OK|CONNECTED|Type HELP for commands");

            String line;
            while ((line = reader.readLine()) != null) {
                String response = handle(line.trim());
                writer.println(response);

                if ("OK|BYE".equals(response)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        } finally {
            if (clientObserver != null) {
                // Gỡ observer khi client ngắt kết nối để tránh gửi sự kiện vào socket đã đóng.
                socketServer.removeObserver(clientObserver);
            }
            closeSocket();
        }
    }

    private String handle(String request) {
        if (request == null || request.isBlank()) {
            return "ERR|EMPTY_REQUEST";
        }

        try {
            String[] parts = request.split(" ", 2);
            String command = parts[0].toUpperCase();
            String payload = parts.length > 1 ? parts[1].trim() : "";

            // Router command dạng text nhận từ socket.
            switch (command) {
                case "HELP":
                    return help();
                case "QUIT":
                case "EXIT":
                    return "OK|BYE";
                case "LOGIN":
                    return login(payload);
                case "REGISTER_BIDDER":
                    return registerBidder(payload);
                case "REGISTER_SELLER":
                    return registerSeller(payload);
                case "DEPOSIT":
                    return deposit(payload);
                case "GET_BALANCE":
                    return getBalance(payload);
                case "LIST_AUCTIONS":
                    return listAuctions();
                case "GET_AUCTION":
                    return getAuction(payload);
                case "PLACE_BID":
                    return placeBid(payload);
                case "CREATE_ART_AUCTION":
                    return createArtAuction(payload);
                case "START_AUCTION":
                    return startAuction(payload);
                case "END_AUCTION":
                    return endAuction(payload);
                default:
                    return "ERR|UNKNOWN_COMMAND";
            }
        } catch (Exception e) {
            // Chuẩn hóa response lỗi để client parse ổn định theo dạng ERR|CODE|MESSAGE.
            return toErrorResponse(e);
        }
    }

    private String login(String payload) {
        String[] args = splitBySpace(payload, 2);
        User user = userService.login(args[0], args[1]);
        return "OK|LOGIN|" + user.getId() + "|" + user.getUsername() + "|" + user.getClass().getSimpleName();
    }

    private String registerBidder(String payload) {
        String[] args = splitBySpace(payload, 3);
        Bidder bidder = new Bidder(args[0], args[1], args[2]);
        userService.register(bidder);
        return "OK|REGISTER_BIDDER|" + bidder.getId();
    }

    private String registerSeller(String payload) {
        String[] args = splitBySpace(payload, 3);
        Seller seller = new Seller(args[0], args[1], args[2]);
        userService.register(seller);
        return "OK|REGISTER_SELLER|" + seller.getId();
    }

    private String deposit(String payload) {
        String[] args = splitBySpace(payload, 2);
        // Nạp tiền qua service để tái sử dụng toàn bộ validation nghiệp vụ.
        userService.deposit(args[0], Double.parseDouble(args[1]));
        return "OK|DEPOSIT|" + args[0] + "|" + userService.getBalance(args[0]);
    }

    private String getBalance(String payload) {
        String userId = requirePayload(payload, "User id");
        return "OK|BALANCE|" + userId + "|" + userService.getBalance(userId);
    }

    private String listAuctions() {
        List<Auction> auctions = auctionService.getActiveAuctions();

        if (auctions.isEmpty()) {
            return "OK|AUCTIONS|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|AUCTIONS");
        for (Auction auction : auctions) {
            response.append("|").append(formatAuction(auction));
        }
        return response.toString();
    }

    private String getAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        Auction auction = auctionService.getAuctionById(auctionId);

        if (auction == null) {
            return "ERR|AUCTION_NOT_FOUND";
        }

        return "OK|AUCTION|" + formatAuction(auction);
    }

    private String placeBid(String payload) {
        String[] args = splitBySpace(payload, 3);
        String auctionId = args[0];
        String bidderId = args[1];
        double bidAmount = Double.parseDouble(args[2]);

        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null) {
            return "ERR|AUCTION_NOT_FOUND";
        }

        User user = userService.getById(bidderId);
        if (!(user instanceof Bidder)) {
            return "ERR|BIDDER_NOT_FOUND";
        }

        BidTransaction bid = new BidTransaction((Bidder) user, auction, bidAmount);
        bidService.placeBid(bid);
        // Phát realtime sự kiện bid mới cho toàn bộ client.
        socketServer.broadcast("EVENT|BID_UPDATED|" + auctionId + "|" + bidAmount + "|" + bidderId);
        return "OK|BID_PLACED|" + bid.getId() + "|" + auctionId + "|" + bidAmount;
    }

    private String createArtAuction(String payload) {
        String[] args = splitByPipe(payload, 8);
        // Tạo item thông qua Factory Method để tách logic khởi tạo khỏi command handler.
        Item item = artItemFactory.create(args);
        User seller = userService.getById(args[7]);
        if (!(seller instanceof Seller)) {
            return "ERR|SELLER_NOT_FOUND";
        }
        item.setSellerId(seller.getId());
        Auction auction = new Auction(item);
        auctionService.saveAuction(auction);
        return "OK|CREATE_ART_AUCTION|" + auction.getId() + "|" + item.getId();
    }

    private String startAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        auctionService.startAuction(auctionId);
        socketServer.broadcast("EVENT|AUCTION_STARTED|" + auctionId);
        return "OK|START_AUCTION|" + auctionId;
    }

    private String endAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        auctionService.endAuction(auctionId);
        socketServer.broadcast("EVENT|AUCTION_ENDED|" + auctionId);
        return "OK|END_AUCTION|" + auctionId;
    }

    private String formatAuction(Auction auction) {
        Item item = auction.getItem();
        double currentPrice = item.getHighestCurrentPrice() > 0 ? item.getHighestCurrentPrice() : item.getStartPrice();
        return auction.getId()
                + "," + item.getId()
                + "," + item.getName()
                + "," + currentPrice
                + "," + auction.getAuctionStatus();
    }

    private String help() {
        return "OK|COMMANDS|"
                + "LOGIN username password;"
                + "REGISTER_BIDDER username password email;"
                + "REGISTER_SELLER username password email;"
                + "DEPOSIT userId amount;"
                + "GET_BALANCE userId;"
                + "LIST_AUCTIONS;"
                + "GET_AUCTION auctionId;"
                + "CREATE_ART_AUCTION name|description|startDate|endDate|startPrice|minIncrement|author|sellerId;"
                + "START_AUCTION auctionId;"
                + "PLACE_BID auctionId bidderId amount;"
                + "END_AUCTION auctionId;"
                + "QUIT";
    }

    private String[] splitBySpace(String payload, int expectedLength) {
        String[] args = requirePayload(payload, "Payload").split("\\s+");
        if (args.length != expectedLength) {
            throw new IllegalArgumentException("Expected " + expectedLength + " arguments");
        }
        return args;
    }

    private String[] splitByPipe(String payload, int expectedLength) {
        String[] args = requirePayload(payload, "Payload").split("\\|", -1);
        if (args.length != expectedLength) {
            throw new IllegalArgumentException("Expected " + expectedLength + " pipe-separated arguments");
        }
        return args;
    }

    private String requirePayload(String payload, String fieldName) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return payload.trim();
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Cannot close client socket: " + e.getMessage());
        }
    }

    private String toErrorResponse(Exception exception) {
        if (exception instanceof AuctionNotFoundException) {
            return "ERR|AUCTION_NOT_FOUND|" + exception.getMessage();
        }
        if (exception instanceof AuctionClosedException) {
            return "ERR|AUCTION_CLOSED|" + exception.getMessage();
        }
        if (exception instanceof InvalidBidException) {
            return "ERR|INVALID_BID|" + exception.getMessage();
        }
        if (exception instanceof InsufficientBalanceException) {
            return "ERR|INSUFFICIENT_BALANCE|" + exception.getMessage();
        }
        if (exception instanceof UserAuthException) {
            return "ERR|AUTH_FAILED|" + exception.getMessage();
        }
        if (exception instanceof IllegalArgumentException) {
            return "ERR|VALIDATION_FAILED|" + exception.getMessage();
        }
        return "ERR|INTERNAL_ERROR|Unexpected server error";
    }
}
