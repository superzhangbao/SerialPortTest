package com.xiaolan.serialporttest;

import java.util.concurrent.atomic.AtomicBoolean;

public class SelectDeviceHelper {
    private String sPort;
    private int iBaudRate;
    private byte[] msg;

    public SelectDeviceHelper() {
        this("/dev/ttyS3", 9600);
    }

    public SelectDeviceHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
        msg = new byte[]{0x02, 0x06, 0x00, 0x00, (byte) 0x80, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
    }


}
