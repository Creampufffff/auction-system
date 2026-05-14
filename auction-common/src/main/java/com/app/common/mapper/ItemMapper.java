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
                "Unknown Author"
            );

        } else if ("ELECTRONICS".equalsIgnoreCase(itemType)) {
            item = new Electronics(
                dto.getDescription(),
                dto.getItemName(),
                dto.getStartDateTime(),
                dto.getEndDateTime(),
                dto.getStartPrice(),
                dto.getMinIncrement(),
                12
            );

        } else if ("VEHICLE".equalsIgnoreCase(itemType)) {
            item = new Vehicle(
                dto.getDescription(),
                dto.getItemName(),
                dto.getStartDateTime(),
                dto.getEndDateTime(),
                dto.getStartPrice(),
                dto.getMinIncrement(),
                "Unknown Brand"
            );
        }

        if (item != null) {
            item.setSellerId(dto.getSellerId());
        }

        return item;
    }
}

