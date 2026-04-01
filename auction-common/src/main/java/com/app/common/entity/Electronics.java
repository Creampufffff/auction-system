package src.main.java.com.app.common.entity;

public class Electronics extends Item{
    private int warrantyMonths;

    public Electronics(String name, double startPrice, double minIncreasement, int warrantyMonths){
        super(name, startPrice, minIncreasement);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
