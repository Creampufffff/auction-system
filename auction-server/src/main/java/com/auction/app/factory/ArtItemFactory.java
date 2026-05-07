package com.auction.app.factory;

import com.app.common.entity.Art;
import com.app.common.entity.Item;

public class ArtItemFactory implements ItemFactory {
    @Override
    public Item create(String[] args) {
        if (args == null || args.length != 8) {
            throw new IllegalArgumentException("Không thể tạo vật phẩm Art!");
        }

        // Cấu trúc args: name|description|startDate|endDate|startPrice|minIncrement|author|sellerId
        return new Art(
                args[1],
                args[0],
                args[2],
                args[3],
                Double.parseDouble(args[4]),
                Double.parseDouble(args[5]),
                args[6]
        );
    }
}
