package com.xiaolan.serialporttest.bean;

public class Wash {
    private long washStatus;

    private long washMode;

    private boolean isRunning;

    private String text;

    public Wash(long washStatus, long washMode, boolean isRunning, String text) {
        this.washStatus = washStatus;
        this.washMode = washMode;
        this.isRunning = isRunning;
        this.text = text;
    }

    public long getWashStatus() {
        return washStatus;
    }

    public void setWashStatus(long washStatus) {
        this.washStatus = washStatus;
    }

    public long getWashMode() {
        return washMode;
    }

    public void setWashMode(long washMode) {
        this.washMode = washMode;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Wash{" +
                "washStatus=" + washStatus +
                ", washMode=" + washMode +
                ", isRunning=" + isRunning +
                ", text='" + text + '\'' +
                '}';
    }
}
