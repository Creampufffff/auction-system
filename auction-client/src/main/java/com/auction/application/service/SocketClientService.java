package com.auction.application.service;

import com.auction.ui.view.LiveBiddingController;
import com.auction.domain.model.ProductDataManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.app.common.dto.LoginResponseDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

public final class SocketClientService {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5001;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Object LISTENER_LOCK = new Object();

    private static volatile boolean listenerRunning;
    private static Socket listenerSocket;
    private static BufferedReader listenerReader;
    // Session socket that can be authenticated (used for sending commands that require login)
    private static volatile Socket sessionSocket;
    private static volatile BufferedReader sessionReader;
    private static volatile PrintWriter sessionWriter;
    // Queue to store responses from session socket (for synchronous request-response)
    private static final LinkedBlockingQueue<String> sessionResponseQueue = new LinkedBlockingQueue<>();

    private SocketClientService() {
    }

    public static String sendJson(String requestJson) {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            reader.readLine();
            writer.println(requestJson);
            return reader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể kết nối tới server socket.", e);
        }
    }

    // Send a legacy plain-text command (non-JSON) to the server and return single-line response
    public static String sendText(String requestText) {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Consume server welcome line (same behavior as sendJson)
            reader.readLine();
            logTextCommand("sendText", requestText);
            writer.println(requestText);
            String response = reader.readLine();
            logTextResponse("sendText", response);
            return response;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể kết nối tới server socket.", e);
        }
    }

    public static void startRealtimeListener() {
        // Deprecated: prefer openSessionAndLogin which starts the authenticated listener on the session socket.
        synchronized (LISTENER_LOCK) {
            if (listenerRunning) return;
            try {
                listenerSocket = new Socket(SERVER_HOST, SERVER_PORT);
                listenerReader = new BufferedReader(new InputStreamReader(listenerSocket.getInputStream()));
                listenerReader.readLine();

                listenerRunning = true;
                Thread listenerThread = new Thread(() -> {
                    try {
                        String line;
                        while (listenerRunning && (line = listenerReader.readLine()) != null) {
                            handleRealtimeMessage(line);
                        }
                    } catch (IOException e) {
                        if (listenerRunning) {
                            System.err.println("Realtime listener stopped: " + e.getMessage());
                        }
                    } finally {
                        stopRealtimeListener();
                    }
                }, "auction-realtime-listener");
                listenerThread.setDaemon(true);
                listenerThread.start();
            } catch (IOException e) {
                stopRealtimeListener();
                throw new IllegalStateException("Không thể khởi động realtime listener.", e);
            }
        }
    }

    public static void stopRealtimeListener() {
        synchronized (LISTENER_LOCK) {
            listenerRunning = false;

            if (listenerReader != null) {
                try {
                    listenerReader.close();
                } catch (IOException ignored) {
                }
                listenerReader = null;
            }

            if (listenerSocket != null) {
                try {
                    listenerSocket.close();
                } catch (IOException ignored) {
                }
                listenerSocket = null;
            }

            if (sessionReader != null) {
                try { sessionReader.close(); } catch (IOException ignored) {}
                sessionReader = null;
            }

            if (sessionWriter != null) {
                sessionWriter.close();
                sessionWriter = null;
            }

            if (sessionSocket != null) {
                try { sessionSocket.close(); } catch (IOException ignored) {}
                sessionSocket = null;
            }

            sessionResponseQueue.clear();
        }
    }

    public static JsonNode sendJson(JsonNode request) {
        try {
            String response = sendJson(MAPPER.writeValueAsString(request));
            return response == null ? null : MAPPER.readTree(response);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể xử lý phản hồi JSON từ server.", e);
        }
    }

    /**
     * Open a persistent session socket and perform text LOGIN on it. If successful,
     * the session socket will be kept open and a realtime listener reading from
     * this authenticated socket will be started.
     * Returns a LoginResponseDTO parsed from server response (role may be empty if server used text protocol).
     */
    public static LoginResponseDTO openSessionAndLogin(String username, String password) {
        try {
            Socket sock = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader r = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            PrintWriter w = new PrintWriter(sock.getOutputStream(), true);

            // consume welcome
            r.readLine();

            // send legacy text login
            String loginCmd = String.format("LOGIN %s %s", username, password);
            logTextCommand("openSessionAndLogin", loginCmd);
            w.println(loginCmd);
            String resp = r.readLine();
            logTextResponse("openSessionAndLogin", resp);

            if (resp == null) {
                try { sock.close(); } catch (Exception ignored) {}
                throw new IllegalStateException("Server không phản hồi khi đăng nhập.");
            }

            if (!resp.startsWith("OK|LOGIN|")) {
                // close socket on failed login
                try { sock.close(); } catch (Exception ignored) {}
                return null;
            }

            // parse OK|LOGIN|userId|username|role
            String[] parts = resp.split("\\|", 5);
            String userId = parts.length >= 3 ? parts[2] : null;
            String uname = parts.length >= 4 ? parts[3] : username;
            String role = parts.length >= 5 ? parts[4] : null;

            // store session socket/writer/reader
            sessionSocket = sock;
            sessionReader = r;
            sessionWriter = w;

            // start listener thread on session socket
            listenerRunning = true;
            Thread listenerThread = new Thread(() -> {
                try {
                    String line;
                    while (listenerRunning && (line = sessionReader.readLine()) != null) {
                        // Determine if this is a realtime event (BID_UPDATED, AUCTION_ENDED, etc.)
                        // or a command response (OK|..., ERR|...)
                        System.out.println("[SocketClientService] session listener recv: " + line);
                        handleSessionMessage(line);
                    }
                } catch (IOException e) {
                    if (listenerRunning) {
                        System.err.println("Session realtime listener stopped: " + e.getMessage());
                    }
                } finally {
                    stopRealtimeListener();
                }
            }, "auction-session-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            LoginResponseDTO dto = new LoginResponseDTO();
            dto.setUserId(userId);
            dto.setUsername(uname);
            dto.setRole(role);
            dto.setSuccess(true);
            // Try to fetch current balance on the authenticated session
            try {
                String balanceResp = sendSessionCommand("GET_BALANCE");
                if (balanceResp != null && balanceResp.startsWith("OK|BALANCE|")) {
                    String[] bparts = balanceResp.split("\\|", 4);
                    if (bparts.length >= 4) {
                        try {
                            double bal = Double.parseDouble(bparts[3]);
                            dto.setBalance(bal);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                // ignore balance fetch errors, login still valid
                System.err.println("Warning: cannot fetch balance after login: " + e.getMessage());
            }

            return dto;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể mở kết nối tới server.", e);
        }
    }

    /**
     * Handle messages from the authenticated session socket.
     * If it's an EVENT (realtime), process as event.
     * Otherwise, treat as command response and put in queue for synchronous handlers.
     */
    private static void handleSessionMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        System.out.println("[SocketClientService] session message <- " + message);
        // Check if it's an event (realtime broadcast)
        if (message.startsWith("EVENT|") || (message.startsWith("{") && message.contains("\"type\":\"EVENT"))) {
            handleRealtimeMessage(message);
        } else {
            // Treat as command response (OK|..., ERR|...)
            sessionResponseQueue.offer(message);
        }
    }

    public static void sendSessionText(String command) {
        if (sessionWriter == null) throw new IllegalStateException("No authenticated session");
        logTextCommand("sendSessionText", command);
        sessionWriter.println(command);
    }

    /**
     * Send a text command on the authenticated session socket and wait for response.
     * Uses a shared response queue read by the session listener.
     * The next line read from the session socket will be treated as the response.
     * Timeout: 10 seconds.
     */
    public static String sendSessionCommand(String command) throws Exception {
        if (sessionWriter == null) throw new IllegalStateException("No authenticated session");

        logTextCommand("sendSessionCommand", command);
        sessionWriter.println(command);

        // Wait for response from session socket (read by listener thread)
        String response = sessionResponseQueue.poll(10, TimeUnit.SECONDS);
        if (response == null) {
            throw new IOException("Server did not respond within 10 seconds");
        }
        logTextResponse("sendSessionCommand", response);
        return response;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    private static void handleRealtimeMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        try {
            if (message.startsWith("{")) {
                JsonNode root = MAPPER.readTree(message);
                String type = root.path("type").asText("");
                JsonNode payload = root.path("payload");

                if ("EVENT_BID_UPDATED".equalsIgnoreCase(type)) {
                    handleBidUpdated(
                            payload.path("auctionId").asText(null),
                            payload.path("currentPrice").asDouble(Double.NaN),
                            payload.path("leadingBidderId").asText(null)
                    );
                }
                return;
            }

            if (message.startsWith("EVENT|BID_UPDATED|")) {
                String[] parts = message.split("\\|", 5);
                if (parts.length >= 5) {
                    handleBidUpdated(parts[2], parseDouble(parts[3]), parts[4]);
                }
                return;
            }

            if (message.startsWith("EVENT|AUCTION_ENDED|")) {
                String[] parts = message.split("\\|", 3);
                if (parts.length >= 3) {
                    handleAuctionEnded(parts[2]);
                }
                return;
            }

            if (message.startsWith("EVENT|AUCTION_EXTENDED|")) {
                String[] parts = message.split("\\|", 4);
                if (parts.length >= 4) {
                    handleAuctionExtended(parts[2], parts[3]);
                }
            }
        } catch (Exception e) {
            System.err.println("Cannot process realtime message: " + message + " | " + e.getMessage());
        }
    }

    private static void handleBidUpdated(String auctionId, double currentPrice, String leadingBidderId) {
        if (auctionId == null || leadingBidderId == null || Double.isNaN(currentPrice)) {
            return;
        }

        ProductDataManager manager = ProductDataManager.getInstance();
        manager.setCurrentPrice(auctionId, currentPrice);
        manager.setLeadingUser(auctionId, leadingBidderId);
        LiveBiddingController.onBidUpdated(auctionId, currentPrice, leadingBidderId);
    }

    private static void handleAuctionEnded(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        ProductDataManager manager = ProductDataManager.getInstance();
        manager.closeAuction(auctionId);
        LiveBiddingController.onAuctionEnded(auctionId);
    }

    private static void handleAuctionExtended(String auctionId, String newEndDateTime) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        ProductDataManager manager = ProductDataManager.getInstance();
        manager.updateAuctionEndDate(auctionId, newEndDateTime);
        LiveBiddingController.onAuctionExtended(auctionId, newEndDateTime);
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static void logTextCommand(String source, String command) {
        System.out.println("[SocketClientService] " + source + " -> " + command);
    }

    private static void logTextResponse(String source, String response) {
        System.out.println("[SocketClientService] " + source + " <- " + response);
    }

    /**
     * Kiểm tra kết nối server (health check / heartbeat)
     * Gửi lệnh PING và kiểm tra xem server có phản hồi không
     * @return true nếu kết nối khỏe, false nếu bị ngắt
     */
    public static boolean sendHeartbeat() {
        try {
            String response = sendText("PING");
            boolean isHealthy = response != null && response.contains("PONG");
            if (!isHealthy) {
                System.err.println("[SocketClientService] Heartbeat failed: " + response);
                // Cố gắng reconnect
                attemptReconnect();
            }
            return isHealthy;
        } catch (Exception e) {
            System.err.println("[SocketClientService] Heartbeat error: " + e.getMessage());
            attemptReconnect();
            return false;
        }
    }

    /**
     * Kiểm tra xem session socket có còn kết nối hay không
     */
    public static boolean isSessionAlive() {
        synchronized (LISTENER_LOCK) {
            return sessionSocket != null && sessionSocket.isConnected() && !sessionSocket.isClosed() && sessionWriter != null;
        }
    }

    /**
     * Cố gắng kết nối lại nếu session bị mất
     */
    private static void attemptReconnect() {
        if (isSessionAlive()) {
            return;
        }
        System.out.println("[SocketClientService] Attempting to reconnect to server...");
        synchronized (LISTENER_LOCK) {
            stopRealtimeListener();
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        while ((userInput = userIn.readLine()) != null) {
            System.out.println(sendText(userInput));
        }
    }
}

