package com.xiaolan.serialporttest.dryer;

public class DryerStatus {
    private int status;

    private String text;

    public DryerStatus(int status, String text) {
        this.status = status;
        this.text = text;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "DryerStatus{" +
                "status=" + status +
                ", text='" + text + '\'' +
                '}';
    }
}
