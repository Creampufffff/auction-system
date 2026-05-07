package com.app.common.dto;

/**
 * AuctionTimeInfoDTO: Provides time information for an auction
 * Helps clients know how much time remains before closing
 */
public class AuctionTimeInfoDTO {
    private String auctionId;
    private String endDateTime;
    private long secondsRemaining;
    private int extensionCount;
    private boolean canBeExtended;

    public AuctionTimeInfoDTO(String auctionId, String endDateTime, long secondsRemaining,
                              int extensionCount, boolean canBeExtended) {
        this.auctionId = auctionId;
        this.endDateTime = endDateTime;
        this.secondsRemaining = secondsRemaining;
        this.extensionCount = extensionCount;
        this.canBeExtended = canBeExtended;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    public int getExtensionCount() {
        return extensionCount;
    }

    public boolean isCanBeExtended() {
        return canBeExtended;
    }

    public String getStatus() {
        if (secondsRemaining < 0) {
            return "ENDED";
        } else if (secondsRemaining < 300) {  // Less than 5 minutes
            return "ENDING_SOON";
        } else if (secondsRemaining < 3600) {  // Less than 1 hour
            return "ACTIVE";
        } else {
            return "PLENTY_OF_TIME";
        }
    }
}

