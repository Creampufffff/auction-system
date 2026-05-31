package com.auction.domain.model;

import javafx.beans.property.*;

public class Product {
    private final StringProperty id;
    private final StringProperty type;
    private final StringProperty name;
    private final DoubleProperty price;
    private final StringProperty status;
    private final StringProperty condition;
    private final StringProperty description;
    private final StringProperty warranty;
    private final StringProperty endDateTime;

    public Product(String id, String name, double price, String status, String condition, String description, String warranty) {
        this(id, "ART", name, price, status, condition, description, warranty, null);
    }

    public Product(String id, String type, String name, double price, String status, String condition, String description, String warranty) {
        this(id, type, name, price, status, condition, description, warranty, null);
    }

    public Product(String id, String type, String name, double price, String status, String condition, String description, String warranty, String endDateTime) {
        this.id = new SimpleStringProperty(id);
        this.type = new SimpleStringProperty(type);
        this.name = new SimpleStringProperty(name);
        this.price = new SimpleDoubleProperty(price);
        this.status = new SimpleStringProperty(status);
        this.condition = new SimpleStringProperty(condition);
        this.description = new SimpleStringProperty(description);
        this.warranty = new SimpleStringProperty(warranty);
        this.endDateTime = new SimpleStringProperty(endDateTime);
    }

    public StringProperty idProperty() { return id; }
    public StringProperty typeProperty() { return type; }
    public StringProperty nameProperty() { return name; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty statusProperty() { return status; }
    public StringProperty conditionProperty() { return condition; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty warrantyProperty() { return warranty; }
    public StringProperty endDateTimeProperty() { return endDateTime; }

    public String getId() { return id.get(); }
    public String getType() { return type.get(); }
    public String getName() { return name.get(); }
    public double getPrice() { return price.get(); }
    public String getStatus() { return status.get(); }
    public String getCondition() { return condition.get(); }
    public String getDescription() { return description.get(); }
    public String getWarranty() { return warranty.get(); }
    public String getEndDateTime() { return endDateTime.get(); }

    /* ============================================================
       CÁC HÀM SETTER (THÊM MỚI ĐỂ FIX LỖI)
       ============================================================ */

    public void setId(String value) { this.id.set(value); }

    public void setType(String value) { this.type.set(value); }

    // Hàm này sẽ giúp fix lỗi setPrice(double) trong LiveBiddingController
    public void setPrice(double value) { this.price.set(value); }

    public void setName(String value) { this.name.set(value); }

    public void setStatus(String value) { this.status.set(value); }

    public void setCondition(String value) { this.condition.set(value); }

    public void setDescription(String value) { this.description.set(value); }

    public void setWarranty(String value) { this.warranty.set(value); }

    public void setEndDateTime(String value) { this.endDateTime.set(value); }
}

