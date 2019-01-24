package com.xiaolan.serialporttest.mylib.listener;

public interface SerialPortReadDataListener {
    void onSerialPortReadDataSuccess(byte[] bytes);

    void onSerialPortReadDataFail(String msg);
}
