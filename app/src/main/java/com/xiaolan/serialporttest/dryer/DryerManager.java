package com.xiaolan.serialporttest.dryer;

import com.xiaolan.serialporttest.mylib.DeviceEngineIService;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;

import java.io.IOException;

public class DryerManager implements DeviceEngineIService {

    private DryerSerialPortHelper mDryerSerialPortHelper;

    private DryerManager() {
        if (mDryerSerialPortHelper == null) {
            mDryerSerialPortHelper = new DryerSerialPortHelper();
        }
    }

    public static DryerManager getInstance() {
        return DryerManagerHolder.DRYER_MANAGER;
    }

    private static class DryerManagerHolder {
        private final static DryerManager DRYER_MANAGER = new DryerManager();
    }

    @Override
    public void open() throws IOException {
        mDryerSerialPortHelper.open();
    }

    @Override
    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {

    }

    @Override
    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {

    }

    @Override
    public void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener) {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void push(int action) throws IOException {

    }

    @Override
    public void pull() {

    }
}
