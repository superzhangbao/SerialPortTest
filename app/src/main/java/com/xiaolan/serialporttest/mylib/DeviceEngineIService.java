package com.xiaolan.serialporttest.mylib;

import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;

import java.io.IOException;

public interface DeviceEngineIService {
    void open() throws IOException;

    void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener);

    void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener);

    void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener);

    boolean isOpen();

    void close() throws IOException;

    void push(int action) throws IOException;

}
