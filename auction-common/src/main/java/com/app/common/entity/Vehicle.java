package com.app.common.entity;

public class Vehicle extends Item{
    private String brand;

    public Vehicle(String description, String name, String startDateString, String endDateString, double startPrice, double minIncreasement, String brand) {
        super(description, name, startDateString, endDateString, startPrice, minIncreasement);
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
