package src.main.java.com.app.common.entity;

import java.io.Serializable;

abstract public class Entity implements Serializable {
    private String id;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                '}';
    }
}
