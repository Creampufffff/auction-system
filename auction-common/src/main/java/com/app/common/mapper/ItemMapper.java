package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

public class ItemMapper {

    public static Item createItem(CreateAuctionRequestDTO dto) {
        if (dto == null) return null;

        Item item = null;
        String itemType = dto.getItemType();

        if ("ART".equalsIgnoreCase(itemType)) {
            item = new Art(
                dto.getDescription(),
                dto.getItemName(),
                dto.getStartDateTime(),
                dto.getEndDateTime(),
                dto.getStartPrice(),
                dto.getMinIncrement(),
                defaultIfBlank(dto.getCondition(), "Unknown Author")
            );

        } else if ("ELECTRONICS".equalsIgnoreCase(itemType)) {
            item = new Electronics(
                dto.getDescription(),
                dto.getItemName(),
                dto.getStartDateTime(),
                dto.getEndDateTime(),
                dto.getStartPrice(),
                dto.getMinIncrement(),
                parsePositiveInt(dto.getWarranty(), 12)
            );

        } else if ("VEHICLE".equalsIgnoreCase(itemType)) {
            item = new Vehicle(
                dto.getDescription(),
                dto.getItemName(),
                dto.getStartDateTime(),
                dto.getEndDateTime(),
                dto.getStartPrice(),
                dto.getMinIncrement(),
                defaultIfBlank(dto.getWarranty(), "Unknown Brand")
            );
        }

        if (item != null) {
            item.setSellerId(dto.getSellerId());
        }

        return item;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

