package com.xiaolan.serialporttest.mylib;

import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortReadDataListener;
import com.xiaolan.serialporttest.wash.juren.JuRenWashManager;
import com.xiaolan.serialporttest.wash.jurenpro.JuRenProWashManager;

import java.io.IOException;

/**
 * 提供给外部使用的设备管理引擎
 */
public class DeviceEngine {

    private static DeviceEngineIService DEVICE_ENGINE_I_SERVICE;

    private DeviceEngine() {
    }

    public static DeviceEngine getInstance() {
        return DeviceEngineHolder.DEVICE_ENGINE;
    }


    private static class DeviceEngineHolder {
        private static final DeviceEngine DEVICE_ENGINE = new DeviceEngine();
    }

    public void selectDevice(int model) {
        switch (model) {
            case 0:
                DEVICE_ENGINE_I_SERVICE =  JuRenProWashManager.getInstance();
                break;
            case 1:
                DEVICE_ENGINE_I_SERVICE =  JuRenWashManager.getInstance();
                break;
        }
    }

    /**
     * 打开串口
     */
    public void open() throws IOException {
        DEVICE_ENGINE_I_SERVICE.open();
    }

    /**
     * 关闭串口
     */
    public void close() throws IOException {
        DEVICE_ENGINE_I_SERVICE.close();
    }

    public synchronized void push(int action) {
        DEVICE_ENGINE_I_SERVICE.push(action);
    }

    public void pull() {
        DEVICE_ENGINE_I_SERVICE.pull();
    }

    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        DEVICE_ENGINE_I_SERVICE.setOnSendInstructionListener(onSendInstructionListener);
    }

    public void setOnCurrentStatusListener(CurrentStatusListener onCurrentStatusListener) {
        DEVICE_ENGINE_I_SERVICE.setOnCurrentStatusListener(onCurrentStatusListener);
    }

    public void setOnSerialPortReadDataListener(SerialPortReadDataListener onSerialPortReadDataListener) {
        DEVICE_ENGINE_I_SERVICE.setOnSerialPortReadDataListener(onSerialPortReadDataListener);
    }

    public boolean isOpen() {
        return DEVICE_ENGINE_I_SERVICE.isOpen();
    }
}
