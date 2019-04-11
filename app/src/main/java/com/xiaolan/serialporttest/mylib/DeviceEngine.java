package com.xiaolan.serialporttest.mylib;

import android.util.Log;

import com.xiaolan.serialporttest.dryer.DeviceEngineDryerService;
import com.xiaolan.serialporttest.dryer.DryerManager;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
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
public class DeviceEngine  {

    private static final String TAG = "DeviceEngine";
    private static DeviceEngineIService DEVICE_ENGINE_ISERVICE = null;

    private static DeviceEngineDryerService DEVICE_ENGINE_DRYERSERVICE = null;

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
            case -1:
                DEVICE_ENGINE_DRYERSERVICE = DryerManager.getInstance();
                break;
            case 0:
                DEVICE_ENGINE_ISERVICE = JuRenProWashManager.getInstance();
                break;
            case 1:
                DEVICE_ENGINE_ISERVICE = JuRenWashManager.getInstance();
                break;
            case 2:
                DEVICE_ENGINE_ISERVICE = XjlWashManager.getInstance();
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
        if (DEVICE_ENGINE_DRYERSERVICE != null) {
            DEVICE_ENGINE_DRYERSERVICE.open();
        }
        if (DEVICE_ENGINE_ISERVICE != null) {
            DEVICE_ENGINE_ISERVICE.open();
        }
    }

    /**
     * 关闭串口
     */
    public void close() throws IOException {
        checkMode();
        if (DEVICE_ENGINE_ISERVICE != null) {
            DEVICE_ENGINE_ISERVICE.close();
            DEVICE_ENGINE_ISERVICE = null;
        }
        if (DEVICE_ENGINE_DRYERSERVICE != null) {
            DEVICE_ENGINE_DRYERSERVICE.close();
            DEVICE_ENGINE_DRYERSERVICE = null;
        }
    }

    public synchronized void push(int action) throws IOException {
        checkMode();
        if (DEVICE_ENGINE_ISERVICE!= null) {
            DEVICE_ENGINE_ISERVICE.push(action);
        }
    }

    public synchronized void push(int action, int coin) throws IOException {
        checkMode();
        if (DEVICE_ENGINE_DRYERSERVICE!= null) {
            DEVICE_ENGINE_DRYERSERVICE.push(action, coin);
        }
    }

    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        checkMode();
        Log.e(TAG,"DEVICE_ENGINE_ISERVICE:"+DEVICE_ENGINE_ISERVICE+",--------DEVICE_ENGINE_DRYERSERVICE:"+DEVICE_ENGINE_DRYERSERVICE);
        if (DEVICE_ENGINE_ISERVICE != null) {
            DEVICE_ENGINE_ISERVICE.setOnSendInstructionListener(onSendInstructionListener);
        }
        if (DEVICE_ENGINE_DRYERSERVICE != null) {
            DEVICE_ENGINE_DRYERSERVICE.setOnSendInstructionListener(onSendInstructionListener);
        }
    }

    public void setOnCurrentStatusListener(CurrentStatusListener onCurrentStatusListener) {
        checkMode();
        if (DEVICE_ENGINE_ISERVICE != null) {
            DEVICE_ENGINE_ISERVICE.setOnCurrentStatusListener(onCurrentStatusListener);
        }
        if (DEVICE_ENGINE_DRYERSERVICE != null) {
            DEVICE_ENGINE_DRYERSERVICE.setOnCurrentStatusListener(onCurrentStatusListener);
        }
    }

    public void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener) {
        checkMode();
        if (DEVICE_ENGINE_ISERVICE != null) {
            DEVICE_ENGINE_ISERVICE.setOnSerialPortOnlineListener(onSerialPortOnlineListener);
        }
        if (DEVICE_ENGINE_DRYERSERVICE != null) {
            DEVICE_ENGINE_DRYERSERVICE.setOnSerialPortOnlineListener(onSerialPortOnlineListener);
        }
    }

    public boolean isOpen() {
        checkMode();
        if (DEVICE_ENGINE_ISERVICE != null) {
            return DEVICE_ENGINE_ISERVICE.isOpen();
        } else if (DEVICE_ENGINE_DRYERSERVICE != null) {
           return DEVICE_ENGINE_DRYERSERVICE.isOpen();
        }else {
            return false;
        }
    }

    private void checkMode() {
        if (DEVICE_ENGINE_ISERVICE == null && DEVICE_ENGINE_DRYERSERVICE == null) {
            throw new RuntimeException("please call function selectDevice() first");
        }
    }
}
