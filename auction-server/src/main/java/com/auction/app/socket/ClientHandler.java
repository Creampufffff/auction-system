package com.auction.app.socket;

import com.app.common.dto.*;
import com.app.common.entity.Bidder;
import com.app.common.entity.Seller;
import com.app.common.entity.User;
import com.app.common.exception.AuctionClosedException;
import com.app.common.exception.AuctionNotFoundException;
import com.app.common.exception.InsufficientBalanceException;
import com.app.common.exception.InvalidBidException;
import com.app.common.exception.UserAuthException;
import com.auction.app.controller.AuctionController;
import com.auction.app.controller.AutoBidController;
import com.auction.app.controller.BidController;
import com.auction.app.controller.UserController;
import com.auction.app.service.AuctionService;
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
    private final UserController userController;
    private final AuctionController auctionController;
    private final BidController bidController;
    private final AutoBidController autoBidController;
    private final AuctionSocketServer socketServer;
    private final UserService userService;
    private final AuctionService auctionService;
    private User currentUser;


    public ClientHandler(
            Socket socket,
            UserService userService,
            AuctionService auctionService,
            UserController userController,
            AuctionController auctionController,
            BidController bidController,
            AutoBidController autoBidController,
            AuctionSocketServer socketServer
    ) {
        this.socket = socket;
        this.userService = userService;
        this.auctionService = auctionService;
        this.userController = userController;
        this.auctionController = auctionController;
        this.bidController = bidController;
        this.autoBidController = autoBidController;
        this.socketServer = socketServer;
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
                case "CREATE_ELECTRONICS_AUCTION" -> createElectronicsAuction(payload);
                case "CREATE_VEHICLE_AUCTION" -> createVehicleAuction(payload);
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
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername(args[0]);
        request.setPassword(args[1]);

        LoginResponseDTO response = userController.login(request);
        if (response == null || !response.isSuccess()) {
            return "ERR|LOGIN_FAILED|Invalid credentials";
        }

        // Store current user from userService to maintain session
        currentUser = userService.getById(response.getUserId());
        return "OK|LOGIN|" + response.getUserId() + "|" + response.getUsername() + "|" + response.getRole();
    }

    private String registerBidder(String payload) {
        String[] args = splitBySpace(payload, 3);
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(args[0]);
        request.setPassword(args[1]);
        request.setEmail(args[2]);
        request.setRole("BIDDER");

        RegisterResponseDTO response = userController.register(request);
        if (!response.isSuccess()) {
            return "ERR|REGISTRATION_FAILED|" + response.getMessage();
        }
        return "OK|REGISTER_BIDDER|" + response.getUserId();
    }

    private String registerSeller(String payload) {
        String[] args = splitBySpace(payload, 3);
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setUsername(args[0]);
        request.setPassword(args[1]);
        request.setEmail(args[2]);
        request.setRole("SELLER");

        RegisterResponseDTO response = userController.register(request);
        if (!response.isSuccess()) {
            return "ERR|REGISTRATION_FAILED|" + response.getMessage();
        }
        return "OK|REGISTER_SELLER|" + response.getUserId();
    }

    private String deposit(String payload) {
        String[] args = requirePayload(payload, "Payload").split("\\s+");
        if (args.length != 1 && args.length != 2) {
            throw new IllegalArgumentException("Requires amount");
        }
        User user = requireCurrentUser();
        String amount = args.length == 1 ? args[0] : args[1];

        DepositRequestDTO request = new DepositRequestDTO(user.getId(), Double.parseDouble(amount));
        ApiResponseDTO response = userController.deposit(request);

        if (!response.isSuccess()) {
            return "ERR|DEPOSIT_FAILED|" + response.getMessage();
        }

        // Get updated balance
        BalanceResponseDTO balanceResponse = userController.getBalance(user.getId());
        return "OK|DEPOSIT|" + user.getId() + "|" + balanceResponse.getBalance();
    }

    private String getBalance(String payload) {
        String userId = requireCurrentUser().getId();
        BalanceResponseDTO response = userController.getBalance(userId);
        if (response == null) {
            return "ERR|BALANCE_ERROR|User not found";
        }
        return "OK|BALANCE|" + userId + "|" + response.getBalance();
    }

    private String listAuctions() {
        List<AuctionListDTO> auctions = auctionController.getActiveAuctions();

        if (auctions.isEmpty()) {
            return "OK|AUCTIONS|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|AUCTIONS");
        for (AuctionListDTO auction : auctions) {
            response.append("|").append(formatAuctionDTO(auction));
        }
        return response.toString();
    }

    private String getAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        AuctionListDTO auction = auctionController.getAuction(auctionId);

        if (auction == null) {
            return "ERR|AUCTION_NOT_FOUND";
        }

        return "OK|AUCTION|" + formatAuctionDTO(auction);
    }

    private String placeBid(String payload) {
        String[] args = requirePayload(payload, "Payload").split("\\s+");
        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Requires auctionId and amount");
        }
        String auctionId = args[0];
        Bidder bidder = requireCurrentBidder();
        double bidAmount = Double.parseDouble(args.length == 2 ? args[1] : args[2]);

        PlaceBidRequestDTO request = new PlaceBidRequestDTO(auctionId, bidder.getId(), bidAmount);
        PlaceBidResponseDTO response = bidController.placeBid(request);

        if (!response.isSuccess()) {
            return "ERR|BID_FAILED|" + response.getMessage();
        }

        processAutoBids(auctionId);
        // Phát realtime sự kiện bid mới cho toàn bộ client.
        socketServer.broadcast("EVENT|BID_UPDATED|" + auctionId + "|" + bidAmount + "|" + bidder.getId());
        // Nếu phiên được gia hạn (anti-sniping), phát sự kiện mở rộng
        if (response.isAuctionExtended()) {
            String newEndDate = response.getNewEndDate();
            socketServer.broadcast("EVENT|AUCTION_EXTENDED|" + auctionId + "|" + (newEndDate == null ? "" : newEndDate));
        }
        return "OK|BID_PLACED|" + response.getBidId() + "|" + auctionId + "|" + bidAmount;
    }

    private String setAutoBid(String payload) {
        String[] args = splitBySpace(payload, 2);
        Bidder bidder = requireCurrentBidder();

        SetAutoBidRequestDTO request = new SetAutoBidRequestDTO(args[0], bidder.getId(), Double.parseDouble(args[1]));
        ApiResponseDTO response = autoBidController.setAutoBid(request);

        if (!response.isSuccess()) {
            return "ERR|AUTO_BID_ERROR|" + response.getMessage();
        }

        return "OK|AUTO_BID_SET|" + response.getMessage().split("ID: ")[1];
    }

    private String cancelAutoBid(String payload) {
        String autoBidId = requirePayload(payload, "Auto bid id");
        ApiResponseDTO response = autoBidController.cancelAutoBid(autoBidId);

        if (!response.isSuccess()) {
            return "ERR|AUTO_BID_ERROR|" + response.getMessage();
        }

        return "OK|AUTO_BID_CANCELED|" + autoBidId;
    }

    private String createArtAuction(String payload) {
        String[] rawArgs = requirePayload(payload, "Payload").split("\\|", -1);
        if (rawArgs.length < 7 || rawArgs.length > 8) {
            throw new IllegalArgumentException("Requires name|description|startDate|endDate|startPrice|minIncrement|author");
        }

        Seller seller = requireCurrentSeller();

        CreateAuctionRequestDTO request = new CreateAuctionRequestDTO(
            rawArgs[0],                              // itemName
            rawArgs[1],                              // description
            rawArgs.length > 7 ? rawArgs[2] : "",   // condition
            "",                                      // warranty
            Double.parseDouble(rawArgs[5]),          // startPrice
            Double.parseDouble(rawArgs[6]),          // minIncrement
            rawArgs[3],                              // startDateTime
            rawArgs[4],                              // endDateTime
            seller.getId(),                          // sellerId
            "ART"                                    // itemType
        );

        ApiResponseDTO response = auctionController.createAuction(request);
        if (!response.isSuccess()) {
            return "ERR|CREATE_AUCTION_FAILED|" + response.getMessage();
        }

        // Extract ID from message (format: "Auction created successfully. ID: {id}")
        String auctionId = response.getMessage().split("ID: ")[1];
        return "OK|CREATE_ART_AUCTION|" + auctionId + "|Item created";
    }

    private String createElectronicsAuction(String payload) {
        String[] rawArgs = requirePayload(payload, "Payload").split("\\|", -1);
        if (rawArgs.length < 7 || rawArgs.length > 8) {
            throw new IllegalArgumentException("Requires name|description|startDate|endDate|startPrice|minIncrement|warrantyMonths");
        }

        Seller seller = requireCurrentSeller();

        CreateAuctionRequestDTO request = new CreateAuctionRequestDTO(
            rawArgs[0],                              // itemName
            rawArgs[1],                              // description
            "",                                      // condition
            rawArgs[6],                              // warranty (warrantyMonths)
            Double.parseDouble(rawArgs[4]),          // startPrice
            Double.parseDouble(rawArgs[5]),          // minIncrement
            rawArgs[2],                              // startDateTime
            rawArgs[3],                              // endDateTime
            seller.getId(),                          // sellerId
            "ELECTRONICS"                            // itemType
        );

        ApiResponseDTO response = auctionController.createAuction(request);
        if (!response.isSuccess()) {
            return "ERR|CREATE_AUCTION_FAILED|" + response.getMessage();
        }

        String auctionId = response.getMessage().split("ID: ")[1];
        return "OK|CREATE_ELECTRONICS_AUCTION|" + auctionId + "|Item created";
    }

    private String createVehicleAuction(String payload) {
        String[] rawArgs = requirePayload(payload, "Payload").split("\\|", -1);
        if (rawArgs.length < 7 || rawArgs.length > 8) {
            throw new IllegalArgumentException("Requires name|description|startDate|endDate|startPrice|minIncrement|brand");
        }

        Seller seller = requireCurrentSeller();

        CreateAuctionRequestDTO request = new CreateAuctionRequestDTO(
            rawArgs[0],                              // itemName
            rawArgs[1],                              // description
            "",                                      // condition
            rawArgs[6],                              // warranty (brand)
            Double.parseDouble(rawArgs[4]),          // startPrice
            Double.parseDouble(rawArgs[5]),          // minIncrement
            rawArgs[2],                              // startDateTime
            rawArgs[3],                              // endDateTime
            seller.getId(),                          // sellerId
            "VEHICLE"                                // itemType
        );

        ApiResponseDTO response = auctionController.createAuction(request);
        if (!response.isSuccess()) {
            return "ERR|CREATE_AUCTION_FAILED|" + response.getMessage();
        }

        String auctionId = response.getMessage().split("ID: ")[1];
        return "OK|CREATE_VEHICLE_AUCTION|" + auctionId + "|Item created";
    }

    private String startAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        requireCurrentSeller();

        ApiResponseDTO response = auctionController.startAuction(auctionId);
        if (!response.isSuccess()) {
            return "ERR|START_AUCTION_FAILED|" + response.getMessage();
        }

        socketServer.broadcast("EVENT|AUCTION_STARTED|" + auctionId);
        return "OK|START_AUCTION|" + auctionId;
    }

    private String endAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        requireCurrentSeller();

        ApiResponseDTO response = auctionController.endAuction(auctionId);
        if (!response.isSuccess()) {
            return "ERR|END_AUCTION_FAILED|" + response.getMessage();
        }

        socketServer.broadcast("EVENT|AUCTION_ENDED|" + auctionId);
        return "OK|END_AUCTION|" + auctionId;
    }

    private String formatAuctionDTO(AuctionListDTO auction) {
        return auction.getAuctionId()
                + "," + auction.getItemId()
                + "," + auction.getName()
                + "," + auction.getCurrentPrice()
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
                + "CREATE_ELECTRONICS_AUCTION name|description|startDate|endDate|startPrice|minIncrement|warrantyMonths;"
                + "CREATE_VEHICLE_AUCTION name|description|startDate|endDate|startPrice|minIncrement|brand;"
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
        if (autoBidController != null) {
            autoBidController.processAutoBids(auctionId);
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
