package com.xiaolan.serialporttest.mylib;

import java.io.IOException;

public interface DeviceEngineIService {
    void selectDevice();

    void open() throws IOException;

    void setPort(String port);

    void setBaudRate(int baudRate);

    void setDataBits(String dataBits);

    void setStopBits(String stopBits);

    void setParityBits(String parityBits);

    void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener);

    void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener);

    void setOnSerialPortConnectListener(SerialPortReadDataListener onSerialPortReadDataListener);

    String getPort();

    int getBaudRate();

    String getDataBits();

    String getStopBits();

    String getParityBits();

    boolean isOpen();

    void sendHex(String s);

    void sendText(String s);

    void close() throws IOException;

    void push(int action) throws IOException;

    void pull() throws IOException;
}
