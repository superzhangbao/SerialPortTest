package com.xiaolan.serialporttest.mylib;

import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
import com.xiaolan.serialporttest.wash.juren.JuRenWashManager;
import com.xiaolan.serialporttest.wash.jurenplus.JuRenPlusWashManager;
import com.xiaolan.serialporttest.wash.jurenpro.JuRenProWashManager;
import com.xiaolan.serialporttest.wash.xjl.XjlWashManager;

import java.io.IOException;

/**
 * 提供给外部使用的设备管理引擎
 */
public class DeviceEngine {

    private DeviceEngineIService DEVICE_ENGINE_ISERVICE;

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
                DEVICE_ENGINE_ISERVICE =  JuRenProWashManager.getInstance();
                break;
            case 1:
                DEVICE_ENGINE_ISERVICE =  JuRenWashManager.getInstance();
                break;
            case 2:
                DEVICE_ENGINE_ISERVICE =  XjlWashManager.getInstance();
                break;
            case 3:
                DEVICE_ENGINE_ISERVICE = JuRenPlusWashManager.getInstance();
                break;
        }
    }

    /**
     * 打开串口
     */
    public void open() throws IOException {
        checkMode();
        DEVICE_ENGINE_ISERVICE.open();
    }

    /**
     * 关闭串口
     */
    public void close() throws IOException {
        checkMode();
        DEVICE_ENGINE_ISERVICE.close();
    }

    public synchronized void push(int action) throws IOException {
        checkMode();
        DEVICE_ENGINE_ISERVICE.push(action);
    }

    public void pull() {
        checkMode();
        DEVICE_ENGINE_ISERVICE.pull();
    }

    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        checkMode();
        DEVICE_ENGINE_ISERVICE.setOnSendInstructionListener(onSendInstructionListener);
    }

    public void setOnCurrentStatusListener(CurrentStatusListener onCurrentStatusListener) {
        checkMode();
        DEVICE_ENGINE_ISERVICE.setOnCurrentStatusListener(onCurrentStatusListener);
    }

    public void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener) {
        checkMode();
        DEVICE_ENGINE_ISERVICE.setOnSerialPortOnlineListener(onSerialPortOnlineListener);
    }

    public boolean isOpen() {
        checkMode();
        return DEVICE_ENGINE_ISERVICE.isOpen();
    }

    private void checkMode() {
        if (DEVICE_ENGINE_ISERVICE == null) {
            throw new RuntimeException("please call function selectDevice() first");
        }
    }
}
