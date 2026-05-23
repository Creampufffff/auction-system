package com.auction.app.factory;

import com.app.common.entity.Vehicle;
import com.app.common.entity.Item;

public class VehicleItemFactory implements ItemFactory {
    @Override
    public Item create(String[] args) {
        if (args == null || args.length != 8) {
            throw new IllegalArgumentException("Vehicle item requires 8 parameters: name|description|startDate|endDate|startPrice|minIncrement|brand|sellerId");
        }

        String name = require(args[0], "name");
        String description = require(args[1], "description");
        String startDate = require(args[2], "startDate");
        String endDate = require(args[3], "endDate");
        double startPrice = Double.parseDouble(require(args[4], "startPrice"));
        double minIncrement = Double.parseDouble(require(args[5], "minIncrement"));
        String brand = require(args[6], "brand");

        Vehicle vehicle = new Vehicle(description, name, startDate, endDate, startPrice, minIncrement, brand);
        vehicle.setSellerId(require(args[7], "sellerId"));
        return vehicle;
    }

    private String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return value.trim();
    }
}

