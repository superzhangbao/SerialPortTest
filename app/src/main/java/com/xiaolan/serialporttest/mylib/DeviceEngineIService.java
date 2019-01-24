package com.xiaolan.serialporttest.mylib;

import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortReadDataListener;

import java.io.IOException;

public interface DeviceEngineIService {
    void open() throws IOException;

    void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener);

    void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener);

    void setOnSerialPortReadDataListener(SerialPortReadDataListener onSerialPortReadDataListener);

    boolean isOpen();

    void close() throws IOException;

    void push(int action);

    void pull();
}
