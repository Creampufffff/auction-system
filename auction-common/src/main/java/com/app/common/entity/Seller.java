package com.app.common.entity;

public class Seller extends User{

    private double balance;

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.balance = 0;
    }


    public void sellItem(String itemId){

    }

}
