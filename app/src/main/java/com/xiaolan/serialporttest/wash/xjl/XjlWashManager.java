package com.xiaolan.serialporttest.wash.xjl;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngineIService;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;

import java.io.IOException;

public class XjlWashManager implements DeviceEngineIService {

    private XjlSerialPortHelper mXjlSerialPortHelper;

    public XjlWashManager() {
        mXjlSerialPortHelper = new XjlSerialPortHelper();
    }

    public static XjlWashManager getInstance() {
        return XjlWashHolder.INSTANCE;
    }

    private static class XjlWashHolder {
        private static XjlWashManager INSTANCE = new XjlWashManager();
    }

    @Override
    public void open() throws IOException {
        mXjlSerialPortHelper.xjlOpen();
    }

    @Override
    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mXjlSerialPortHelper.setOnSendInstructionListener(onSendInstructionListener);
    }

    @Override
    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mXjlSerialPortHelper.setOnCurrentStatusListener(currentStatusListener);
    }

    @Override
    public void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener) {
        mXjlSerialPortHelper.setOnSerialPortOnlineListener(onSerialPortOnlineListener);
    }

    @Override
    public boolean isOpen() {
        return mXjlSerialPortHelper.isOpen();
    }

    @Override
    public void close() throws IOException {
        mXjlSerialPortHelper.close();
    }

    @Override
    public void push(int action) {
        switch (action) {
            case DeviceAction.Xjl.ACTION_START:
                mXjlSerialPortHelper.sendStartOrStop();
                break;
            case DeviceAction.Xjl.ACTION_MODE1:
                mXjlSerialPortHelper.sendHot();
                break;
            case DeviceAction.Xjl.ACTION_MODE2:
                mXjlSerialPortHelper.sendWarm();
                break;
            case DeviceAction.Xjl.ACTION_MODE3:
                mXjlSerialPortHelper.sendCold();
                break;
            case DeviceAction.Xjl.ACTION_MODE4:
                mXjlSerialPortHelper.sendDelicates();
                break;
            case DeviceAction.Xjl.ACTION_SUPER:
                mXjlSerialPortHelper.sendSuper();
                break;
            case DeviceAction.Xjl.ACTION_SETTING:
                mXjlSerialPortHelper.sendSetting();
                break;
            case DeviceAction.Xjl.ACTION_KILL:
                mXjlSerialPortHelper.sendKill();
                break;
            case DeviceAction.Xjl.ACTION_SELF_CLEANING:
                mXjlSerialPortHelper.sendSelfCleaning();
                break;
        }
    }
}
