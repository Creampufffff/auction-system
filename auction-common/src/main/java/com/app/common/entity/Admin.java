package com.app.common.entity;

public class Admin extends User {
    public static final int permissionLevel = 0;
    public Admin(String username, String password, String email){
        super(username, password, email);
    }

}
