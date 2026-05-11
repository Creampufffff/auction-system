package com.app.common.mapper;

import com.app.common.dto.*;
import com.app.common.entity.*;

/**
 * ItemMapper - Chuyển đổi giữa Item Entity và các DTO liên quan
 */
public class ItemMapper {

    /**
     * Tạo Item từ CreateAuctionRequestDTO dựa trên itemType
     */
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
                "Unknown Author" // Có thể extend DTO để lấy author
            );

        } else if ("ELECTRONICS".equalsIgnoreCase(itemType)) {
            item = new Electronics(
                dto.getDescription(),
                dto.getItemName(),
                dto.getStartDateTime(),
                dto.getEndDateTime(),
                dto.getStartPrice(),
                dto.getMinIncrement(),
                12 // Default warranty months
            );

        } else if ("VEHICLE".equalsIgnoreCase(itemType)) {
            item = new Vehicle(
                dto.getDescription(),
                dto.getItemName(),
                dto.getStartDateTime(),
                dto.getEndDateTime(),
                dto.getStartPrice(),
                dto.getMinIncrement(),
                "Unknown Brand" // Có thể extend DTO để lấy brand
            );
        }

        if (item != null) {
            item.setSellerId(dto.getSellerId());
        }

        return item;
    }
}


