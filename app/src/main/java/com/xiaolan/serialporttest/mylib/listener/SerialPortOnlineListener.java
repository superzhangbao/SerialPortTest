package com.xiaolan.serialporttest.mylib.listener;

public interface SerialPortOnlineListener {
    void onSerialPortOnline(byte[] bytes);

    void onSerialPortOffline(String msg);
}
