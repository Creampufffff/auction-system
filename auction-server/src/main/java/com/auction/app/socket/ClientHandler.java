package com.auction.app.socket;

import com.app.common.dto.*;
import com.app.common.entity.*;
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
import java.util.Base64;
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
                case "PING" -> "OK|PONG";
                case "HELP" -> help();
                case "QUIT", "EXIT" -> "OK|BYE";
                case "LOGIN" -> login(payload);
                case "REGISTER_BIDDER" -> registerBidder(payload);
                case "REGISTER_SELLER" -> registerSeller(payload);
                case "DEPOSIT" -> deposit(payload);
                case "WITHDRAW" -> withdraw(payload);
                case "GET_BALANCE" -> getBalance();
                case "LIST_AUCTIONS" -> listAuctions();
                case "LIST_MY_AUCTIONS" -> listMyAuctions();
                case "GET_MY_ITEMS" -> getMyItems();
                case "GET_AUCTION" -> getAuction(payload);
                case "GET_BID_HISTORY" -> getBidHistory(payload);
                case "GET_MY_BID_HISTORY" -> getMyBidHistory();
                case "ADMIN_DASHBOARD" -> adminDashboard();
                case "ADMIN_LIST_USERS" -> adminListUsers();
                case "ADMIN_LIST_AUCTIONS" -> adminListAuctions();
                case "ADMIN_LIST_BIDS" -> adminListBids();
                case "PLACE_BID" -> placeBid(payload);
                case "SET_AUTO_BID" -> setAutoBid(payload);
                case "CANCEL_AUTO_BID" -> cancelAutoBid(payload);
                case "CREATE_ART_AUCTION" -> createArtAuction(payload);
                case "CREATE_ELECTRONICS_AUCTION" -> createElectronicsAuction(payload);
                case "CREATE_VEHICLE_AUCTION" -> createVehicleAuction(payload);
                case "UPLOAD_IMAGE" -> uploadImage(payload);
                case "UPDATE_AUCTION" -> updateAuction(payload);
                case "DELETE_AUCTION" -> deleteAuction(payload);
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
        if (args.length != 1) {
            throw new IllegalArgumentException("Requires amount");
        }

        User user = requireCurrentUser();
        String targetUserId = user.getId();
        String amount = args[0];

        DepositRequestDTO request = new DepositRequestDTO(targetUserId, Double.parseDouble(amount));
        ApiResponseDTO response = userController.deposit(request);

        if (!response.isSuccess()) {
            return "ERR|DEPOSIT_FAILED|" + response.getMessage();
        }

        // Get updated balance for the target user
        BalanceResponseDTO balanceResponse = userController.getBalance(targetUserId);
        return "OK|DEPOSIT|" + targetUserId + "|" + balanceResponse.getBalance();
    }

    private String withdraw(String payload) {
        String[] args = requirePayload(payload, "Payload").split("\\s+");
        if (args.length != 1) {
            throw new IllegalArgumentException("Requires amount");
        }

        User user = requireCurrentUser();
        String targetUserId = user.getId();
        String amount = args[0];

        WithdrawRequestDTO request = new WithdrawRequestDTO(targetUserId, Double.parseDouble(amount));
        ApiResponseDTO response = userController.withdraw(request);

        if (!response.isSuccess()) {
            return "ERR|WITHDRAW_FAILED|" + response.getMessage();
        }

        // Get updated balance for the target user
        BalanceResponseDTO balanceResponse = userController.getBalance(targetUserId);
        return "OK|WITHDRAW|" + targetUserId + "|" + balanceResponse.getBalance();
    }

    private String getBalance() {
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
            response.append("|").append(formatAuctionDTO(auction, false));
        }
        return response.toString();
    }

    private String listMyAuctions() {
        Seller seller = requireCurrentSeller();
        List<AuctionListDTO> auctions = auctionController.getAuctionsBySellerId(seller.getId());

        if (auctions.isEmpty()) {
            return "OK|MY_AUCTIONS|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|MY_AUCTIONS");
        for (AuctionListDTO auction : auctions) {
            response.append("|").append(formatAuctionDTO(auction, false));
        }
        return response.toString();
    }

    private String getMyItems() {
        Bidder bidder = requireCurrentBidder();
        List<AuctionListDTO> auctions = auctionController.getWonAuctionsByBidderId(bidder.getId());

        if (auctions.isEmpty()) {
            return "OK|MY_ITEMS|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|MY_ITEMS");
        for (AuctionListDTO auction : auctions) {
            response.append("|").append(formatAuctionDTO(auction, false));
        }
        return response.toString();
    }

    private String getAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        AuctionListDTO auction = auctionController.getAuction(auctionId);

        if (auction == null) {
            return "ERR|AUCTION_NOT_FOUND";
        }

        return "OK|AUCTION|" + formatAuctionDTO(auction, true);
    }

    private String getBidHistory(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        List<BidHistoryDTO> bids = bidController.getAuctionBidHistory(auctionId);
        return formatBidHistoryResponse(bids);
    }

    private String getMyBidHistory() {
        Bidder bidder = requireCurrentBidder();
        List<BidHistoryDTO> bids = bidController.getBidderBidHistory(bidder.getId());
        return formatBidHistoryResponse(bids);
    }

    private String adminDashboard() {
        requireCurrentAdmin();
        List<User> users = userService.getAllUser();
        List<AuctionListDTO> auctions = auctionController.getAllAuctions();
        List<BidHistoryDTO> bids = bidController.getAllBids();

        long bidderCount = users.stream().filter(user -> user instanceof Bidder).count();
        long sellerCount = users.stream().filter(user -> user instanceof Seller).count();
        long runningCount = auctions.stream()
                .filter(auction -> auction.getAuctionStatus() != null && "RUNNING".equals(auction.getAuctionStatus().name()))
                .count();
        long finishedCount = auctions.stream()
                .filter(auction -> auction.getAuctionStatus() != null && "FINISHED".equals(auction.getAuctionStatus().name()))
                .count();

        return "OK|ADMIN_DASHBOARD|"
                + users.size() + ","
                + bidderCount + ","
                + sellerCount + ","
                + auctions.size() + ","
                + runningCount + ","
                + finishedCount + ","
                + bids.size();
    }

    private String adminListUsers() {
        requireCurrentAdmin();
        List<User> users = userService.getAllUser();
        if (users.isEmpty()) {
            return "OK|ADMIN_USERS|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|ADMIN_USERS");
        for (User user : users) {
            response.append("|")
                    .append(nullToEmpty(user.getId()))
                    .append(",")
                    .append(escapeCsvField(user.getUsername()))
                    .append(",")
                    .append(escapeCsvField(user.getEmail()))
                    .append(",")
                    .append(resolveRole(user))
                    .append(",")
                    .append(user.getBalance())
                    .append(",")
                    .append(user.getHeldBalance());
        }
        return response.toString();
    }

    private String adminListAuctions() {
        requireCurrentAdmin();
        List<AuctionListDTO> auctions = auctionController.getAllAuctions();
        if (auctions.isEmpty()) {
            return "OK|ADMIN_AUCTIONS|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|ADMIN_AUCTIONS");
        for (AuctionListDTO auction : auctions) {
            response.append("|").append(formatAuctionDTO(auction, false));
        }
        return response.toString();
    }

    private String adminListBids() {
        requireCurrentAdmin();
        List<BidHistoryDTO> bids = bidController.getAllBids();
        return formatAdminBidHistoryResponse(bids);
    }

    private String formatBidHistoryResponse(List<BidHistoryDTO> bids) {
        if (bids == null || bids.isEmpty()) {
            return "OK|BID_HISTORY|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|BID_HISTORY");
        for (BidHistoryDTO bid : bids) {
            response.append("|")
                    .append(nullToEmpty(bid.getBidId()))
                    .append(",")
                    .append(nullToEmpty(bid.getAuctionId()))
                    .append(",")
                    .append(escapeCsvField(bid.getItemType()))
                    .append(",")
                    .append(escapeCsvField(bid.getItemName()))
                    .append(",")
                    .append(nullToEmpty(bid.getBidderUsername()))
                    .append(",")
                    .append(bid.getBidAmount())
                    .append(",")
                    .append(nullToEmpty(bid.getBidTime()));
        }
        return response.toString();
    }

    private String formatAdminBidHistoryResponse(List<BidHistoryDTO> bids) {
        if (bids == null || bids.isEmpty()) {
            return "OK|ADMIN_BIDS|EMPTY";
        }

        StringBuilder response = new StringBuilder("OK|ADMIN_BIDS");
        for (BidHistoryDTO bid : bids) {
            response.append("|")
                    .append(nullToEmpty(bid.getBidId()))
                    .append(",")
                    .append(nullToEmpty(bid.getAuctionId()))
                    .append(",")
                    .append(escapeCsvField(bid.getItemType()))
                    .append(",")
                    .append(escapeCsvField(bid.getItemName()))
                    .append(",")
                    .append(escapeCsvField(bid.getBidderUsername()))
                    .append(",")
                    .append(bid.getBidAmount())
                    .append(",")
                    .append(nullToEmpty(bid.getBidTime()));
        }
        return response.toString();
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
        BidHistoryDTO topBid = getTopBid(auctionId);
        double broadcastAmount = topBid == null ? bidAmount : topBid.getBidAmount();
        String broadcastBidder = topBid == null ? bidder.getId() : nullToEmpty(topBid.getBidderUsername());
        // Phát realtime sự kiện bid mới cho toàn bộ client.
        socketServer.broadcast("EVENT|BID_UPDATED|" + auctionId + "|" + broadcastAmount + "|" + broadcastBidder);
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

        processAutoBids(args[0]);
        BidHistoryDTO topBid = getTopBid(args[0]);
        if (topBid != null) {
            socketServer.broadcast("EVENT|BID_UPDATED|" + args[0] + "|" + topBid.getBidAmount()
                    + "|" + nullToEmpty(topBid.getBidderUsername()));
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
        if (rawArgs.length != 7 && rawArgs.length != 8) {
            throw new IllegalArgumentException("Requires name|description|startDate|endDate|startPrice|minIncrement|author");
        }

        Seller seller = requireCurrentSeller();
        byte[] imageBlob = decodeOptionalImage(rawArgs, 7);

        CreateAuctionRequestDTO request = new CreateAuctionRequestDTO(
            rawArgs[0],                              // itemName
            rawArgs[1],                              // description
            rawArgs[6],                              // condition (author for art)
            "",                                      // warranty
            Double.parseDouble(rawArgs[4]),          // startPrice
            Double.parseDouble(rawArgs[5]),          // minIncrement
            rawArgs[2],                              // startDateTime
            rawArgs[3],                              // endDateTime
            seller.getId(),                          // sellerId
            "ART",                                   // itemType
            imageBlob
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
        byte[] imageBlob = decodeOptionalImage(rawArgs, 7);

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
            "ELECTRONICS",                           // itemType
            imageBlob
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
        byte[] imageBlob = decodeOptionalImage(rawArgs, 7);

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
            "VEHICLE",                               // itemType
            imageBlob
        );

        ApiResponseDTO response = auctionController.createAuction(request);
        if (!response.isSuccess()) {
            return "ERR|CREATE_AUCTION_FAILED|" + response.getMessage();
        }

        String auctionId = response.getMessage().split("ID: ")[1];
        return "OK|CREATE_VEHICLE_AUCTION|" + auctionId + "|Item created";
    }

    private String uploadImage(String payload) {
        String[] rawArgs = requirePayload(payload, "Payload").split("\\|", -1);
        if (rawArgs.length != 2) {
            throw new IllegalArgumentException("Requires auctionId|base64ImageData");
        }

        Seller seller = requireCurrentSeller();
        Auction existing = requireOwnedAuction(seller, rawArgs[0], "upload image for");
        byte[] imageBlob = decodeOptionalImage(rawArgs, 1);
        if (imageBlob == null || imageBlob.length == 0) {
            throw new IllegalArgumentException("Image data cannot be empty");
        }

        existing.getItem().setImageBlob(imageBlob);
        ApiResponseDTO response = auctionController.updateAuction(existing);
        if (!response.isSuccess()) {
            return "ERR|UPLOAD_IMAGE_FAILED|" + response.getMessage();
        }
        return "OK|UPLOAD_IMAGE|" + rawArgs[0];
    }

    private String updateAuction(String payload) {
        String[] rawArgs = requirePayload(payload, "Payload").split("\\|", -1);
        if (rawArgs.length != 2 && rawArgs.length != 8) {
            throw new IllegalArgumentException("Requires auctionId|name or auctionId|name|description|startDate|endDate|startPrice|minIncrement|typeSpecificValue");
        }

        Seller seller = requireCurrentSeller();
        String auctionId = rawArgs[0];
        Auction existing = auctionService.getAuctionById(auctionId);
        if (existing == null || existing.getItem() == null) {
            return "ERR|AUCTION_NOT_FOUND|Auction not found";
        }
        if (!seller.getId().equals(existing.getItem().getSellerId())) {
            return "ERR|AUTH_FAILED|Cannot update another seller's auction";
        }

        Item updatedItem = rawArgs.length == 2
                ? createRenamedItem(existing, rawArgs[1], seller.getId())
                : createUpdatedItem(existing, rawArgs, seller.getId());
        Auction updatedAuction = new Auction(updatedItem);
        updatedAuction.setId(existing.getId());
        updatedAuction.setAuctionStatus(existing.getAuctionStatus());

        ApiResponseDTO response = auctionController.updateAuction(updatedAuction);
        if (!response.isSuccess()) {
            return "ERR|UPDATE_AUCTION_FAILED|" + response.getMessage();
        }
        return "OK|UPDATE_AUCTION|" + auctionId;
    }

    private Item createRenamedItem(Auction existing, String name, String sellerId) {
        Item oldItem = existing.getItem();
        String extra;
        if (oldItem instanceof Electronics) {
            extra = String.valueOf(((Electronics) oldItem).getWarrantyMonths());
        } else if (oldItem instanceof Vehicle) {
            extra = ((Vehicle) oldItem).getBrand();
        } else if (oldItem instanceof Art) {
            extra = ((Art) oldItem).getAuthor();
        } else {
            extra = "";
        }

        String[] args = {
                existing.getId(),
                name,
                oldItem.getDescription(),
                oldItem.getStartDateString(),
                oldItem.getEndDateString(),
                String.valueOf(oldItem.getStartPrice()),
                String.valueOf(oldItem.getMinIncreasement()),
                extra
        };
        return createUpdatedItem(existing, args, sellerId);
    }

    private Item createUpdatedItem(Auction existing, String[] rawArgs, String sellerId) {
        Item oldItem = existing.getItem();
        String type = resolveItemType(oldItem);
        String name = rawArgs[1];
        String description = rawArgs[2];
        String startDate = rawArgs[3];
        String endDate = rawArgs[4];
        double startPrice = oldItem.getStartPrice();
        double minIncrement = oldItem.getMinIncreasement();
        String extra = rawArgs[7];

        Item item;
        if ("ELECTRONICS".equals(type)) {
            item = new Electronics(description, name, startDate, endDate, startPrice, minIncrement, Integer.parseInt(extra));
        } else if ("VEHICLE".equals(type)) {
            item = new Vehicle(description, name, startDate, endDate, startPrice, minIncrement, extra);
        } else {
            item = new Art(description, name, startDate, endDate, startPrice, minIncrement, extra);
        }
        item.setId(oldItem.getId());
        item.setSellerId(sellerId);
        item.setHighestCurrentPrice(oldItem.getHighestCurrentPrice());
        item.setImageBlob(oldItem.getImageBlob());
        return item;
    }

    private byte[] decodeOptionalImage(String[] rawArgs, int index) {
        if (rawArgs.length <= index || rawArgs[index] == null || rawArgs[index].isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(rawArgs[index]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Image data must be valid base64");
        }
    }

    private String resolveItemType(Item item) {
        if (item instanceof Electronics) {
            return "ELECTRONICS";
        }
        if (item instanceof Vehicle) {
            return "VEHICLE";
        }
        return "ART";
    }

    private String deleteAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        Seller seller = requireCurrentSeller();
        Auction existing = auctionService.getAuctionById(auctionId);
        if (existing == null || existing.getItem() == null) {
            return "ERR|AUCTION_NOT_FOUND|Auction not found";
        }
        if (!seller.getId().equals(existing.getItem().getSellerId())) {
            return "ERR|AUTH_FAILED|Cannot delete another seller's auction";
        }

        ApiResponseDTO response = auctionController.deleteAuction(auctionId);
        if (!response.isSuccess()) {
            return "ERR|DELETE_AUCTION_FAILED|" + response.getMessage();
        }
        socketServer.broadcast("EVENT|AUCTION_ENDED|" + auctionId);
        return "OK|DELETE_AUCTION|" + auctionId;
    }

    private String startAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        Seller seller = requireCurrentSeller();
        requireOwnedAuction(seller, auctionId, "start");

        ApiResponseDTO response = auctionController.startAuction(auctionId);
        if (!response.isSuccess()) {
            return "ERR|START_AUCTION_FAILED|" + response.getMessage();
        }

        socketServer.broadcast("EVENT|AUCTION_STARTED|" + auctionId);
        return "OK|START_AUCTION|" + auctionId;
    }

    private String endAuction(String payload) {
        String auctionId = requirePayload(payload, "Auction id");
        Seller seller = requireCurrentSeller();
        requireOwnedAuction(seller, auctionId, "end");

        ApiResponseDTO response = auctionController.endAuction(auctionId);
        if (!response.isSuccess()) {
            return "ERR|END_AUCTION_FAILED|" + response.getMessage();
        }

        socketServer.broadcast("EVENT|AUCTION_ENDED|" + auctionId);
        return "OK|END_AUCTION|" + auctionId;
    }

    private String formatAuctionDTO(AuctionListDTO auction, boolean includeImage) {
        String record = auction.getAuctionId()
                + "," + auction.getItemId()
                + "," + auction.getItemType()
                + "," + auction.getName()
                + "," + auction.getCurrentPrice()
                + "," + auction.getAuctionStatus()
                + "," + nullToEmpty(auction.getStartDateTime())
                + "," + nullToEmpty(auction.getEndDateTime())
                + "," + escapeCsvField(auction.getCondition())
                + "," + escapeCsvField(auction.getDescription())
                + "," + escapeCsvField(auction.getWarranty())
                + "," + auction.getMinIncrement();
        if (!includeImage) {
            return record;
        }
        return record + "," + encodeOptionalImage(auction.getImageBlob());
    }

    private String encodeOptionalImage(byte[] imageBlob) {
        return imageBlob == null || imageBlob.length == 0 ? "" : Base64.getEncoder().encodeToString(imageBlob);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeCsvField(String value) {
        return nullToEmpty(value).replace(",", " ");
    }

    private String help() {
        return "OK|COMMANDS|"
                + "LOGIN username password;"
                + "REGISTER_BIDDER username password email;"
                + "REGISTER_SELLER username password email;"
                + "DEPOSIT amount;"
                + "WITHDRAW amount;"
                + "GET_BALANCE;"
                + "LIST_AUCTIONS;"
                + "LIST_MY_AUCTIONS;"
                + "GET_MY_ITEMS;"
                + "GET_AUCTION auctionId;"
                + "GET_BID_HISTORY auctionId;"
                + "GET_MY_BID_HISTORY;"
                + "ADMIN_DASHBOARD;"
                + "ADMIN_LIST_USERS;"
                + "ADMIN_LIST_AUCTIONS;"
                + "ADMIN_LIST_BIDS;"
                + "CREATE_ART_AUCTION name|description|startDate|endDate|startPrice|minIncrement|author;"
                + "CREATE_ELECTRONICS_AUCTION name|description|startDate|endDate|startPrice|minIncrement|warrantyMonths;"
                + "CREATE_VEHICLE_AUCTION name|description|startDate|endDate|startPrice|minIncrement|brand;"
                + "UPLOAD_IMAGE auctionId|base64ImageData;"
                + "UPDATE_AUCTION auctionId|name;"
                + "DELETE_AUCTION auctionId;"
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

    private Admin requireCurrentAdmin() {
        User user = requireCurrentUser();
        if (!(user instanceof Admin)) {
            throw new UserAuthException("Current user is not an admin");
        }
        return (Admin) user;
    }

    private String resolveRole(User user) {
        if (user instanceof Admin) {
            return "ADMIN";
        }
        if (user instanceof Seller) {
            return "SELLER";
        }
        return "BIDDER";
    }

    private Auction requireOwnedAuction(Seller seller, String auctionId, String action) {
        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null || auction.getItem() == null) {
            throw new AuctionNotFoundException("Auction not found");
        }
        if (!seller.getId().equals(auction.getItem().getSellerId())) {
            throw new UserAuthException("Cannot " + action + " another seller's auction");
        }
        return auction;
    }


    private void processAutoBids(String auctionId) {
        if (autoBidController != null) {
            autoBidController.processAutoBids(auctionId);
        }
    }

    private BidHistoryDTO getTopBid(String auctionId) {
        List<BidHistoryDTO> bids = bidController.getAuctionBidHistory(auctionId);
        return bids == null || bids.isEmpty() ? null : bids.get(0);
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
