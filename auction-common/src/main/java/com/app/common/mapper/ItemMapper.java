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
                parseWarrantyMonths(dto.getWarranty(), 0)
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
            item.setImageBlob(dto.getImageBlob());
        }

        return item;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int parseWarrantyMonths(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("-?\\d+").matcher(value.trim());
            if (!matcher.find()) {
                return fallback;
            }
            int parsed = Integer.parseInt(matcher.group());
            return Math.max(0, parsed);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

