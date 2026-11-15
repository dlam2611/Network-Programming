package com.multiserver.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model đại diện cho bảng users trong cơ sở dữ liệu PostgreSQL (RMIDATABASE).
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String name;
    private String email;
    private String password;
    private BigDecimal balance;
    private Timestamp lastUpdated;

    public User() {
        this.balance = BigDecimal.ZERO;
        this.lastUpdated = new Timestamp(System.currentTimeMillis());
    }

    public User(int userId, String name, String email, String password, BigDecimal balance, Timestamp lastUpdated) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.lastUpdated = lastUpdated != null ? lastUpdated : new Timestamp(System.currentTimeMillis());
    }

    public User(String name, String email, String password, BigDecimal balance) {
        this(0, name, email, password, balance, new Timestamp(System.currentTimeMillis()));
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", balance=" + balance +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}

