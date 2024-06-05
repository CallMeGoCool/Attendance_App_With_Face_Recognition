package com.example.attendanceapp;

public class StudentItem {
    private long sid;
    private int roll;
    private String name;
    private String status;
    private boolean isChanged;

    public StudentItem(long sid, int roll, String name) {
        this.sid = sid;
        this.roll = roll;
        this.name = name;
        status = "";

    }

    public int getRoll() {
        return roll;
    }

    public void setRoll(int roll) {
        if (roll <= 0) {
            throw new IllegalArgumentException("Roll number must be a positive integer");
        }
        this.roll = roll;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getSid() {
        return sid;
    }

    public void setSid(long sid) {
        this.sid = sid;
    }

    public boolean isChanged() {
        return isChanged;
    }

    public void setChanged(boolean changed) {
        isChanged = changed;
    }
}
