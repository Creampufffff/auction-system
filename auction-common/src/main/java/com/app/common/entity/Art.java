package src.main.java.com.app.common.entity;

public class Art extends Item{
    private String author;

    public Art(String description, String name, String startDateString, String endDateString, double startPrice, double minIncreasement, String author) {
        super(description, name, startDateString, endDateString, startPrice, minIncreasement);
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
