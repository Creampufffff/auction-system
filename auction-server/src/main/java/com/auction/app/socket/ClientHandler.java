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

import com.auction.app.service.AutoBidService;

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
    private final AutoBidService autoBidService;
    private final AuctionSocketServer socketServer;
    private final ItemFactory artItemFactory;
    private User currentUser;


    public ClientHandler(
            Socket socket,
            UserService userService,
            ItemService itemService,
            AuctionService auctionService,
            BidService bidService,
            AuctionSocketServer socketServer
    ) {
        this(socket, userService, itemService, auctionService, bidService, null, socketServer);
    }

    public ClientHandler(
            Socket socket,
            UserService userService,
            ItemService itemService,
            AuctionService auctionService,
            BidService bidService,
            AutoBidService autoBidService,
            AuctionSocketServer socketServer
    ) {
        this.socket = socket;
        this.userService = userService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.autoBidService = autoBidService;
        this.socketServer = socketServer;
        this.artItemFactory = new ArtItemFactory();
    }

    @Override
    public void run() {
        SocketClientObserver clientObserver = null;
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
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
            return switch (command) {
                case "HELP" -> help();
                case "QUIT", "EXIT" -> "OK|BYE";
                case "LOGIN" -> login(payload);
                case "REGISTER_BIDDER" -> registerBidder(payload);
                case "REGISTER_SELLER" -> registerSeller(payload);
                case "DEPOSIT" -> deposit(payload);
                case "GET_BALANCE" -> getBalance(payload);
                case "LIST_AUCTIONS" -> listAuctions();
                case "GET_AUCTION" -> getAuction(payload);
                case "PLACE_BID" -> placeBid(payload);
                case "SET_AUTO_BID" -> setAutoBid(payload);
                case "CANCEL_AUTO_BID" -> cancelAutoBid(payload);
                case "CREATE_ART_AUCTION" -> createArtAuction(payload);
                case "START_AUCTION" -> startAuction(payload);
                case "END_AUCTION" -> endAuction(payload);
                default -> "ERR|UNKNOWN_COMMAND";
            };
        } catch (Exception e) {
            return toErrorResponse(e);
        }
    }


    private String login(String payload) {
        String[] args = splitBySpace(payload, 2);
        User user = userService.login(args[0], args[1]);
        currentUser = user;
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
        String[] args = requirePayload(payload, "Payload").split("\\s+");
        if (args.length != 1 && args.length != 2) {
            throw new IllegalArgumentException("Requires amount");
        }
        User user = requireCurrentUser();
        String amount = args.length == 1 ? args[0] : args[1];
        userService.deposit(user.getId(), Double.parseDouble(amount));
        return "OK|DEPOSIT|" + user.getId() + "|" + userService.getBalance(user.getId());
    }

    private String getBalance(String payload) {
        String userId = requireCurrentUser().getId();
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
        String[] args = requirePayload(payload, "Payload").split("\\s+");
        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Requires auctionId and amount");
        }
        String auctionId = args[0];
        Bidder bidder = requireCurrentBidder();
        double bidAmount = Double.parseDouble(args.length == 2 ? args[1] : args[2]);

        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null) {
            return "ERR|AUCTION_NOT_FOUND";
        }

        BidTransaction bid = new BidTransaction(bidder, auction, bidAmount);
        bidService.placeBid(bid);
        processAutoBids(auctionId);
        // Phát realtime sự kiện bid mới cho toàn bộ client.
        socketServer.broadcast("EVENT|BID_UPDATED|" + auctionId + "|" + bidAmount + "|" + bidder.getId());
        return "OK|BID_PLACED|" + bid.getId() + "|" + auctionId + "|" + bidAmount;
    }

    private String setAutoBid(String payload) {
        String[] args = splitBySpace(payload, 2);
        Bidder bidder = requireCurrentBidder();
        com.app.common.entity.AutoBid autoBid = new com.app.common.entity.AutoBid(args[0], bidder.getId(), Double.parseDouble(args[1]));
        autoBidService.createAutoBid(autoBid);
        return "OK|AUTO_BID_SET|" + autoBid.getId();
    }

    private String cancelAutoBid(String payload) {
        String autoBidId = requirePayload(payload, "Auto bid id");
        autoBidService.cancelAutoBid(autoBidId);
        return "OK|AUTO_BID_CANCELED|" + autoBidId;
    }

    private String createArtAuction(String payload) {
        String[] rawArgs = requirePayload(payload, "Payload").split("\\|", -1);
        if (rawArgs.length != 7 && rawArgs.length != 8) {
            throw new IllegalArgumentException("Requires name|description|startDate|endDate|startPrice|minIncrement|author");
        }
        // Tạo item thông qua Factory Method để tách logic khởi tạo khỏi command handler.
        Seller seller = requireCurrentSeller();
        String[] args = new String[8];
        System.arraycopy(rawArgs, 0, args, 0, 7);
        args[7] = seller.getId();
        Item item = artItemFactory.create(args);
        Auction auction = new Auction(item);
        auctionService.saveAuction(auction);
        return "OK|CREATE_ART_AUCTION|" + auction.getId() + "|" + item.getId();
    }

    private String startAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        requireCurrentSeller();
        auctionService.startAuction(auctionId);
        socketServer.broadcast("EVENT|AUCTION_STARTED|" + auctionId);
        return "OK|START_AUCTION|" + auctionId;
    }

    private String endAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        requireCurrentSeller();
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
                + "DEPOSIT amount;"
                + "GET_BALANCE;"
                + "LIST_AUCTIONS;"
                + "GET_AUCTION auctionId;"
                + "CREATE_ART_AUCTION name|description|startDate|endDate|startPrice|minIncrement|author;"
                + "START_AUCTION auctionId;"
                + "PLACE_BID auctionId amount;"
                + "SET_AUTO_BID auctionId maxAmount;"
                + "CANCEL_AUTO_BID autoBidId;"
                + "END_AUCTION auctionId;"
                + "QUIT";
    }

    private String[] splitBySpace(String payload, int expectedLength) {
        String[] args = requirePayload(payload, "Payload").split("\\s+");
        if (args.length != expectedLength) {
            throw new IllegalArgumentException("Requires exactly " + expectedLength + " parameters");
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

    private User requireCurrentUser() {
        if (currentUser == null) {
            throw new UserAuthException("Please login first");
        }
        return currentUser;
    }

    private Bidder requireCurrentBidder() {
        User user = requireCurrentUser();
        if (!(user instanceof Bidder)) {
            throw new UserAuthException("Current user is not a bidder");
        }
        return (Bidder) user;
    }

    private Seller requireCurrentSeller() {
        User user = requireCurrentUser();
        if (!(user instanceof Seller)) {
            throw new UserAuthException("Current user is not a seller");
        }
        return (Seller) user;
    }


    private void processAutoBids(String auctionId) {
        if (autoBidService != null) {
            autoBidService.processAutoBidsForAuction(auctionId);
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
