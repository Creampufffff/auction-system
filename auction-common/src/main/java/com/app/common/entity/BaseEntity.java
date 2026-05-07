package com.app.common.entity;

import java.io.Serializable;
import java.util.UUID;

abstract public class BaseEntity implements Serializable {
    private String id;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public BaseEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public BaseEntity(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                '}';
    }
}
