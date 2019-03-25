package com.xiaolan.serialporttest.dryer;

import android.support.annotation.NonNull;

import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;

import java.io.IOException;

public class DryerManager implements DeviceEngineDryerService {

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
    public void setOnSendInstructionListener(@NonNull OnSendInstructionListener onSendInstructionListener) {
        mDryerSerialPortHelper.setOnSendInstructionListener(onSendInstructionListener);
    }

    @Override
    public void setOnCurrentStatusListener(@NonNull CurrentStatusListener currentStatusListener) {
        mDryerSerialPortHelper.setOnCurrentStatusListener(currentStatusListener);
    }

    @Override
    public void setOnSerialPortOnlineListener(@NonNull SerialPortOnlineListener onSerialPortOnlineListener) {
        mDryerSerialPortHelper.setOnSerialPortOnlineListener(onSerialPortOnlineListener);
    }

    @Override
    public boolean isOpen() {
        return mDryerSerialPortHelper.isOpen();
    }

    @Override
    public void close() throws IOException {
        mDryerSerialPortHelper.close();
    }

    @Override
    public void push(int action,int coin) {
        mDryerSerialPortHelper.push(action,coin);
    }
}
