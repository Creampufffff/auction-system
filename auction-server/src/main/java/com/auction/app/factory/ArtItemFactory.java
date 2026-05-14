package com.auction.app.factory;

import com.app.common.entity.Art;
import com.app.common.entity.Item;

public class ArtItemFactory implements ItemFactory {
    @Override
    public Item create(String[] args) {
        if (args == null || args.length != 8) {
            throw new IllegalArgumentException("Art item requires 8 parameters: name|description|startDate|endDate|startPrice|minIncrement|author|sellerId");
        }

        String name = require(args[0], "name");
        String description = require(args[1], "description");
        String startDate = require(args[2], "startDate");
        String endDate = require(args[3], "endDate");
        double startPrice = Double.parseDouble(require(args[4], "startPrice"));
        double minIncrement = Double.parseDouble(require(args[5], "minIncrement"));
        String author = require(args[6], "author");

        Art art = new Art(description, name, startDate, endDate, startPrice, minIncrement, author);
        art.setSellerId(require(args[7], "sellerId"));
        return art;
    }

    private String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return value.trim();
    }
}
