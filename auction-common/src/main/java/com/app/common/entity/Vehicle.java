package src.main.java.com.app.common.entity;

public class Vehicle extends Item{
    private String brand;

    public Vehicle(String name, double startPrice, double minIncreasement, String brand){
        super(name, startPrice, minIncreasement);
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
