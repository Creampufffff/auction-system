package src.main.java.com.app.common.entity;

abstract public class Item extends Entity {

    private String name;
    private String description;
    private double startPrice;
    private double minIncreasement;
    private String endDateString;
    private String startDateString;
    private double highestCurrentPrice;


    public Item(String name, double startPrice, double minIncreasement){
        this.name = name;
        this.startPrice = startPrice;
        this.minIncreasement = minIncreasement;
    }
    public double getStartPrice() {
        return startPrice;
    }
    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public double getMinIncreasement() {
        return minIncreasement;
    }
    public void setMinIncreasement(double minIncreasement) {
        this.minIncreasement = minIncreasement;
    }

    public String getEndDateString() {
        return endDateString;
    }
    public void setEndDateString(String endDateString) {
        this.endDateString = endDateString;
    }


    public String getStartDateString() {
        return startDateString;
    }
    public void setStartDateString(String startDateString) {
        this.startDateString = startDateString;
    }

    public double getHighestCurrentPrice() {
        return highestCurrentPrice;
    }
    public void setHighestCurrentPrice(double highestCurrentPrice) {
        this.highestCurrentPrice = highestCurrentPrice;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

}
