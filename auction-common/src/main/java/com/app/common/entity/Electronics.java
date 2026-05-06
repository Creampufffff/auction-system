package com.app.common.entity;

public class Electronics extends Item{
    private int warrantyMonths;

    public Electronics(String description, String name, String startDateString, String endDateString, double startPrice, double minIncreasement, int warrantyMonths) {
        super(description, name, startDateString, endDateString, startPrice, minIncreasement);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
