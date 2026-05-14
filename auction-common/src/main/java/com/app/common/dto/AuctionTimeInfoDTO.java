package com.app.common.dto;

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
        } else if (secondsRemaining < 300) {
            return "ENDING_SOON";
        } else if (secondsRemaining < 3600) {
            return "ACTIVE";
        } else {
            return "PLENTY_OF_TIME";
        }
    }
}

