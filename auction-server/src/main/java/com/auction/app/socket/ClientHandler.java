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
import com.auction.app.controller.AuctionController;
import com.auction.app.controller.AutoBidController;
import com.auction.app.controller.BidController;
import com.auction.app.controller.UserController;
import com.auction.app.controller.ItemController;

import com.app.common.dto.PlaceBidRequestDTO;
import com.app.common.dto.PlaceBidResponseDTO;
import com.app.common.dto.AuctionListDTO;
import com.app.common.dto.LoginRequestDTO;
import com.app.common.dto.LoginResponseDTO;
import com.app.common.dto.RegisterRequestDTO;
import com.app.common.dto.RegisterResponseDTO;
import com.app.common.dto.DepositRequestDTO;
import com.app.common.dto.BalanceResponseDTO;
import com.app.common.dto.CreateAuctionRequestDTO;
import com.app.common.dto.SetAutoBidRequestDTO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    // Controllers for delegating to DTO-based handlers
    private final BidController bidController;
    private final AuctionController auctionController;
    private final UserController userController;
    private final ItemController itemController;
    private final AutoBidController autoBidController;
    private User currentUser;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // Initialize controllers that operate on DTOs
        this.bidController = new BidController(bidService, auctionService, userService);
        this.auctionController = new AuctionController(auctionService);
        this.userController = new UserController(userService);
        this.itemController = new ItemController(itemService);
        this.autoBidController = autoBidService == null ? null : new AutoBidController(autoBidService, auctionService);
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
        // If input looks like JSON, handle as JSON envelope and use DTOs
        String trimmed = request.trim();
        if (trimmed.startsWith("{")) {
            try {
                return handleJson(trimmed);
            } catch (Exception e) {
                return toErrorResponse(e);
            }
        }

        // Fallback: legacy text protocol
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

    // Handle JSON envelope messages and delegate to DTO-based controllers
    private String handleJson(String jsonString) throws Exception {
        JsonNode root = objectMapper.readTree(jsonString);
        String type = root.path("type").asText(null);
        String requestId = root.path("requestId").asText(null);
        JsonNode payload = root.path("payload");

        if (type == null) {
            throw new IllegalArgumentException("Missing message type");
        }

        switch (type.toUpperCase()) {
            case "PLACE_BID": {
                PlaceBidRequestDTO req = objectMapper.treeToValue(payload, PlaceBidRequestDTO.class);
                Bidder bidder = requireCurrentBidder();
                req.setBidderId(bidder.getId());
                PlaceBidResponseDTO resp = bidController.placeBid(req);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                // Broadcast event (both legacy text and JSON) so mixed clients can receive updates
                if (resp.isSuccess()) {
                    processAutoBids(req.getAuctionId());
                    socketServer.broadcast("EVENT|BID_UPDATED|" + req.getAuctionId() + "|" + req.getBidAmount() + "|" + bidder.getId());
                }
                ObjectNode event = objectMapper.createObjectNode();
                event.put("type", "EVENT_BID_UPDATED");
                ObjectNode evPayload = objectMapper.createObjectNode();
                evPayload.put("auctionId", req.getAuctionId());
                evPayload.put("currentPrice", req.getBidAmount());
                evPayload.put("leadingBidderId", bidder.getId());
                event.set("payload", evPayload);
                if (resp.isSuccess()) {
                    socketServer.broadcast(objectMapper.writeValueAsString(event));
                }
                return objectMapper.writeValueAsString(envelope);
            }
            case "LIST_AUCTIONS": {
                // No payload expected
                java.util.List<AuctionListDTO> list = auctionController.getActiveAuctions();
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", true);
                envelope.set("payload", objectMapper.valueToTree(list));
                return objectMapper.writeValueAsString(envelope);
            }
            case "GET_AUCTION": {
                String auctionId = payload.path("auctionId").asText(null);
                if (auctionId == null) throw new IllegalArgumentException("auctionId is required");
                com.app.common.dto.AuctionListDTO dto = auctionController.getAuction(auctionId);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", dto != null);
                envelope.set("payload", objectMapper.valueToTree(dto));
                return objectMapper.writeValueAsString(envelope);
            }
            case "LOGIN": {
                LoginRequestDTO req = objectMapper.treeToValue(payload, LoginRequestDTO.class);
                LoginResponseDTO resp = userController.login(req);
                if (resp != null && resp.isSuccess()) {
                    currentUser = userService.getById(resp.getUserId());
                }
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp != null && resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "REGISTER_BIDDER":
            case "REGISTER": {
                RegisterRequestDTO req = objectMapper.treeToValue(payload, RegisterRequestDTO.class);
                RegisterResponseDTO resp = userController.register(req);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp != null && resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "REGISTER_SELLER": {
                RegisterRequestDTO req = objectMapper.treeToValue(payload, RegisterRequestDTO.class);
                req.setRole("SELLER");
                RegisterResponseDTO resp = userController.register(req);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp != null && resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "CREATE_AUCTION": {
                Seller seller = requireCurrentSeller();
                CreateAuctionRequestDTO req = objectMapper.treeToValue(payload, CreateAuctionRequestDTO.class);
                req.setSellerId(seller.getId());
                com.app.common.dto.ApiResponseDTO resp = auctionController.createAuction(req);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "START_AUCTION": {
                requireCurrentSeller();
                String auctionId = payload.path("auctionId").asText(null);
                if (auctionId == null) throw new IllegalArgumentException("auctionId is required");
                com.app.common.dto.ApiResponseDTO resp = auctionController.startAuction(auctionId);
                if (resp.isSuccess()) {
                    socketServer.broadcast("EVENT|AUCTION_STARTED|" + auctionId);
                }
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "END_AUCTION": {
                requireCurrentSeller();
                String auctionId = payload.path("auctionId").asText(null);
                if (auctionId == null) throw new IllegalArgumentException("auctionId is required");
                com.app.common.dto.ApiResponseDTO resp = auctionController.endAuction(auctionId);
                if (resp.isSuccess()) {
                    socketServer.broadcast("EVENT|AUCTION_ENDED|" + auctionId);
                }
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "SET_AUTO_BID": {
                Bidder bidder = requireCurrentBidder();
                SetAutoBidRequestDTO req = objectMapper.treeToValue(payload, SetAutoBidRequestDTO.class);
                req.setBidderId(bidder.getId());
                com.app.common.dto.ApiResponseDTO resp = requireAutoBidController().setAutoBid(req);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "CANCEL_AUTO_BID": {
                String autoBidId = payload.path("autoBidId").asText(null);
                if (autoBidId == null) throw new IllegalArgumentException("autoBidId is required");
                com.app.common.dto.ApiResponseDTO resp = requireAutoBidController().cancelAutoBid(autoBidId);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "DEPOSIT": {
                DepositRequestDTO req = objectMapper.treeToValue(payload, DepositRequestDTO.class);
                req.setUserId(requireCurrentUser().getId());
                com.app.common.dto.ApiResponseDTO resp = userController.deposit(req);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp.isSuccess());
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            case "GET_BALANCE": {
                String userId = requireCurrentUser().getId();
                BalanceResponseDTO resp = userController.getBalance(userId);
                ObjectNode envelope = objectMapper.createObjectNode();
                envelope.put("type", "RESPONSE");
                if (requestId != null) envelope.put("requestId", requestId);
                envelope.put("success", resp != null);
                envelope.set("payload", objectMapper.valueToTree(resp));
                return objectMapper.writeValueAsString(envelope);
            }
            default:
                throw new IllegalArgumentException("Unknown JSON command: " + type);
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
        SetAutoBidRequestDTO request = new SetAutoBidRequestDTO(args[0], bidder.getId(), Double.parseDouble(args[1]));
        com.app.common.dto.ApiResponseDTO response = requireAutoBidController().setAutoBid(request);
        if (!response.isSuccess()) {
            return "ERR|AUTO_BID_FAILED|" + response.getMessage();
        }
        return "OK|AUTO_BID_SET|" + response.getMessage();
    }

    private String cancelAutoBid(String payload) {
        String autoBidId = requirePayload(payload, "Auto bid id");
        com.app.common.dto.ApiResponseDTO response = requireAutoBidController().cancelAutoBid(autoBidId);
        if (!response.isSuccess()) {
            return "ERR|AUTO_BID_CANCEL_FAILED|" + response.getMessage();
        }
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

    private String[] splitByPipe(String payload, int expectedLength) {
        String[] args = requirePayload(payload, "Payload").split("\\|", -1);
        if (args.length != expectedLength) {
            throw new IllegalArgumentException("Requires exactly " + expectedLength + " parameters separated by '|'");
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

    private AutoBidController requireAutoBidController() {
        if (autoBidController == null) {
            throw new IllegalStateException("Auto-bid service is not available");
        }
        return autoBidController;
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
