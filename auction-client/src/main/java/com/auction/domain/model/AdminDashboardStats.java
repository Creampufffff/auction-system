package com.auction.domain.model;

public class AdminDashboardStats {
    private final int totalUsers;
    private final int totalBidders;
    private final int totalSellers;
    private final int totalAuctions;
    private final int runningAuctions;
    private final int finishedAuctions;
    private final int totalBids;

    public AdminDashboardStats(
            int totalUsers,
            int totalBidders,
            int totalSellers,
            int totalAuctions,
            int runningAuctions,
            int finishedAuctions,
            int totalBids
    ) {
        this.totalUsers = totalUsers;
        this.totalBidders = totalBidders;
        this.totalSellers = totalSellers;
        this.totalAuctions = totalAuctions;
        this.runningAuctions = runningAuctions;
        this.finishedAuctions = finishedAuctions;
        this.totalBids = totalBids;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public int getTotalBidders() {
        return totalBidders;
    }

    public int getTotalSellers() {
        return totalSellers;
    }

    public int getTotalAuctions() {
        return totalAuctions;
    }

    public int getRunningAuctions() {
        return runningAuctions;
    }

    public int getFinishedAuctions() {
        return finishedAuctions;
    }

    public int getTotalBids() {
        return totalBids;
    }
}
