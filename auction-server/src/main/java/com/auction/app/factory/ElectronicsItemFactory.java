package com.auction.app.factory;

import com.app.common.entity.Electronics;
import com.app.common.entity.Item;

public class ElectronicsItemFactory implements ItemFactory {
    @Override
    public Item create(String[] args) {
        if (args == null || args.length != 8) {
            throw new IllegalArgumentException("Electronics item requires 8 parameters: name|description|startDate|endDate|startPrice|minIncrement|warrantyMonths|sellerId");
        }

        String name = require(args[0], "name");
        String description = require(args[1], "description");
        String startDate = require(args[2], "startDate");
        String endDate = require(args[3], "endDate");
        double startPrice = Double.parseDouble(require(args[4], "startPrice"));
        double minIncrement = Double.parseDouble(require(args[5], "minIncrement"));
        int warrantyMonths = Integer.parseInt(require(args[6], "warrantyMonths"));

        Electronics electronics = new Electronics(description, name, startDate, endDate, startPrice, minIncrement, warrantyMonths);
        electronics.setSellerId(require(args[7], "sellerId"));
        return electronics;
    }

    private String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return value.trim();
    }
}

