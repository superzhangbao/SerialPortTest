package com.xiaolan.serialporttest.mylib;

public interface SerialPortReadDataListener {
    void onSerialPortReadDataSuccess();

    void onSerialPortReadDataFail(String msg);
}
