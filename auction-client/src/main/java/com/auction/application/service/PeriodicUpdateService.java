package com.auction.application.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý tất cả các periodic tasks (cập nhật định kỳ) cho client.
 * - Periodic Price & Status Refresh
 * - Balance Update Scheduler
 * - Auction List Auto-Refresh
 * - Connection Health Check (Heartbeat)
 * - Bid History Sync
 */
public class PeriodicUpdateService {
    private static final PeriodicUpdateService INSTANCE = new PeriodicUpdateService();
    private final ConcurrentHashMap<String, Timer> activeTimers = new ConcurrentHashMap<>();

    private static final long PRICE_REFRESH_INTERVAL = 5000;  // 5 giây
    private static final long BALANCE_REFRESH_INTERVAL = 30000; // 30 giây
    private static final long AUCTION_LIST_REFRESH_INTERVAL = 15000; // 15 giây
    private static final long HEARTBEAT_INTERVAL = 60000; // 1 phút
    private static final long BID_HISTORY_SYNC_INTERVAL = 10000; // 10 giây
    private static final long STATUS_REFRESH_INTERVAL = 10000; // 10 giây - Refresh trạng thái ProductDetail

    private PeriodicUpdateService() {
    }

    public static PeriodicUpdateService getInstance() {
        return INSTANCE;
    }

    /**
     * Bắt đầu periodic price refresh cho LiveBidding
     */
    public void startPeriodicPriceRefresh(Runnable refreshTask) {
        stopPeriodicPriceRefresh();
        Timer timer = new Timer("periodic-price-refresh", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    refreshTask.run();
                } catch (Exception e) {
                    System.err.println("Error in periodic price refresh: " + e.getMessage());
                }
            }
        }, PRICE_REFRESH_INTERVAL, PRICE_REFRESH_INTERVAL);
        activeTimers.put("priceRefresh", timer);
        System.out.println("[PeriodicUpdateService] Started periodic price refresh (interval: " + PRICE_REFRESH_INTERVAL + "ms)");
    }

    /**
     * Dừng periodic price refresh
     */
    public void stopPeriodicPriceRefresh() {
        Timer timer = activeTimers.remove("priceRefresh");
        if (timer != null) {
            timer.cancel();
            System.out.println("[PeriodicUpdateService] Stopped periodic price refresh");
        }
    }

    /**
     * Bắt đầu periodic balance refresh
     */
    public void startPeriodicBalanceRefresh(Runnable refreshTask) {
        stopPeriodicBalanceRefresh();
        Timer timer = new Timer("periodic-balance-refresh", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    refreshTask.run();
                } catch (Exception e) {
                    System.err.println("Error in periodic balance refresh: " + e.getMessage());
                }
            }
        }, BALANCE_REFRESH_INTERVAL, BALANCE_REFRESH_INTERVAL);
        activeTimers.put("balanceRefresh", timer);
        System.out.println("[PeriodicUpdateService] Started periodic balance refresh (interval: " + BALANCE_REFRESH_INTERVAL + "ms)");
    }

    /**
     * Dừng periodic balance refresh
     */
    public void stopPeriodicBalanceRefresh() {
        Timer timer = activeTimers.remove("balanceRefresh");
        if (timer != null) {
            timer.cancel();
            System.out.println("[PeriodicUpdateService] Stopped periodic balance refresh");
        }
    }

    /**
     * Bắt đầu periodic auction list refresh
     */
    public void startPeriodicAuctionListRefresh(Runnable refreshTask) {
        stopPeriodicAuctionListRefresh();
        Timer timer = new Timer("periodic-auction-list-refresh", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    refreshTask.run();
                } catch (Exception e) {
                    System.err.println("Error in periodic auction list refresh: " + e.getMessage());
                }
            }
        }, AUCTION_LIST_REFRESH_INTERVAL, AUCTION_LIST_REFRESH_INTERVAL);
        activeTimers.put("auctionListRefresh", timer);
        System.out.println("[PeriodicUpdateService] Started periodic auction list refresh (interval: " + AUCTION_LIST_REFRESH_INTERVAL + "ms)");
    }

    /**
     * Dừng periodic auction list refresh
     */
    public void stopPeriodicAuctionListRefresh() {
        Timer timer = activeTimers.remove("auctionListRefresh");
        if (timer != null) {
            timer.cancel();
            System.out.println("[PeriodicUpdateService] Stopped periodic auction list refresh");
        }
    }

    /**
     * Bắt đầu heartbeat (kiểm tra kết nối)
     */
    public void startHeartbeat(Runnable heartbeatTask) {
        stopHeartbeat();
        Timer timer = new Timer("heartbeat", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    heartbeatTask.run();
                } catch (Exception e) {
                    System.err.println("Error in heartbeat: " + e.getMessage());
                }
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL);
        activeTimers.put("heartbeat", timer);
        System.out.println("[PeriodicUpdateService] Started heartbeat (interval: " + HEARTBEAT_INTERVAL + "ms)");
    }

    /**
     * Dừng heartbeat
     */
    public void stopHeartbeat() {
        Timer timer = activeTimers.remove("heartbeat");
        if (timer != null) {
            timer.cancel();
            System.out.println("[PeriodicUpdateService] Stopped heartbeat");
        }
    }

    /**
     * Bắt đầu periodic bid history sync
     */
    public void startPeriodicBidHistorySync(Runnable syncTask) {
        stopPeriodicBidHistorySync();
        Timer timer = new Timer("periodic-bid-history-sync", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    syncTask.run();
                } catch (Exception e) {
                    System.err.println("Error in periodic bid history sync: " + e.getMessage());
                }
            }
        }, BID_HISTORY_SYNC_INTERVAL, BID_HISTORY_SYNC_INTERVAL);
        activeTimers.put("bidHistorySync", timer);
        System.out.println("[PeriodicUpdateService] Started periodic bid history sync (interval: " + BID_HISTORY_SYNC_INTERVAL + "ms)");
    }

    /**
     * Dừng periodic bid history sync
     */
    public void stopPeriodicBidHistorySync() {
        Timer timer = activeTimers.remove("bidHistorySync");
        if (timer != null) {
            timer.cancel();
            System.out.println("[PeriodicUpdateService] Stopped periodic bid history sync");
        }
    }

    /**
     * Bắt đầu periodic status refresh cho ProductDetail
     */
    public void startPeriodicStatusRefresh(Runnable refreshTask) {
        stopPeriodicStatusRefresh();
        Timer timer = new Timer("periodic-status-refresh", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    refreshTask.run();
                } catch (Exception e) {
                    System.err.println("Error in periodic status refresh: " + e.getMessage());
                }
            }
        }, STATUS_REFRESH_INTERVAL, STATUS_REFRESH_INTERVAL);
        activeTimers.put("statusRefresh", timer);
        System.out.println("[PeriodicUpdateService] Started periodic status refresh (interval: " + STATUS_REFRESH_INTERVAL + "ms)");
    }

    /**
     * Dừng periodic status refresh
     */
    public void stopPeriodicStatusRefresh() {
        Timer timer = activeTimers.remove("statusRefresh");
        if (timer != null) {
            timer.cancel();
            System.out.println("[PeriodicUpdateService] Stopped periodic status refresh");
        }
    }

    /**
     * Dừng tất cả timers (khi logout hoặc close app)
     */
    public void stopAllTimers() {
        for (String key : activeTimers.keySet()) {
            Timer timer = activeTimers.remove(key);
            if (timer != null) {
                timer.cancel();
            }
        }
        System.out.println("[PeriodicUpdateService] Stopped all timers");
    }
}