package src.main.java.com.app.common.entity;

public class Bidder extends User{
    private double balance;

    public Bidder(String username, String password, String email) {
        super(username, password, email);
        this.balance = 0;
    }


}
