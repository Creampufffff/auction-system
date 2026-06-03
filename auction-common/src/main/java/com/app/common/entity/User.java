package com.app.common.entity;

public abstract class User extends BaseEntity {
    private String username, passwordHash, email;
    private double balance;
    private double heldBalance;
//    public static final int permissionLevel = 3;

    public User(String username, String passwordHash, String email){
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.balance = 0;
        this.heldBalance = 0;
//        System.out.println("Tai khoan da duoc tao."); // Sau nay luu log lai
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Deprecated
    public String getPassword() {
        return getPasswordHash();
    }

    @Deprecated
    public void setPassword(String password) {
        setPasswordHash(password);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        this.balance = balance;
    }

    public double getHeldBalance() {
        return heldBalance;
    }

    public void setHeldBalance(double heldBalance) {
        if (heldBalance < 0) {
            throw new IllegalArgumentException("Held balance cannot be negative");
        }
        this.heldBalance = heldBalance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than 0");
        }
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be greater than 0");
        }
        if (amount > this.balance) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        this.balance -= amount;
    }

    public void reserve(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Reserve amount must be greater than 0");
        }
        if (amount > this.balance) {
            throw new IllegalArgumentException("Insufficient balance to reserve");
        }
        this.balance -= amount;
        this.heldBalance += amount;
    }

    public void releaseHeld(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Release amount must be greater than 0");
        }
        if (amount > this.heldBalance) {
            throw new IllegalArgumentException("Insufficient held balance");
        }
        this.heldBalance -= amount;
        this.balance += amount;
    }

    public void consumeHeld(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Consume amount must be greater than 0");
        }
        if (amount > this.heldBalance) {
            throw new IllegalArgumentException("Insufficient held balance");
        }
        this.heldBalance -= amount;
    }
}
