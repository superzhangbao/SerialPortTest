package com.xiaolan.serialporttest.wash.juren;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngineIService;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortReadDataListener;

import java.io.IOException;

public class JuRenWashManager implements DeviceEngineIService {

    private JuRenSerialPortHelper mJuRenSerialPortHelper;


    public JuRenWashManager() {
        if (mJuRenSerialPortHelper == null) {
            mJuRenSerialPortHelper = new JuRenSerialPortHelper();
        }
    }

    public static JuRenWashManager getInstance() {
        return JuRenWashHolder.INSTANCE;
    }

    private static class JuRenWashHolder {
        private static JuRenWashManager INSTANCE = new JuRenWashManager();
    }

    @Override
    public void open() throws IOException {
        mJuRenSerialPortHelper.juRenOpen();
    }


    @Override
    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mJuRenSerialPortHelper.setOnSendInstructionListener(onSendInstructionListener);
    }

    @Override
    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mJuRenSerialPortHelper.setOnCurrentStatusListener(currentStatusListener);
    }

    @Override
    public void setOnSerialPortReadDataListener(SerialPortReadDataListener serialPortReadDataListener) {
        mJuRenSerialPortHelper.setOnSerialPortReadDataListener(serialPortReadDataListener);
    }

    @Override
    public boolean isOpen() {
        return mJuRenSerialPortHelper.isOpen();
    }

    @Override
    public void close() throws IOException {
        mJuRenSerialPortHelper.juRenClose();
    }

    @Override
    public void push(int action) {
        switch (action) {
            case DeviceAction.JuRen.ACTION_START:
                mJuRenSerialPortHelper.sendStart();
                break;
            case DeviceAction.JuRen.ACTION_WHITES:
                mJuRenSerialPortHelper.sendWhites();
                break;
            case DeviceAction.JuRen.ACTION_COLORS:
                mJuRenSerialPortHelper.sendColors();
                break;
            case DeviceAction.JuRen.ACTION_DELICATES:
                mJuRenSerialPortHelper.sendDelicates();
                break;
            case DeviceAction.JuRen.ACTION_PERM_PRESS:
                mJuRenSerialPortHelper.sendPermPress();
                break;
            case DeviceAction.JuRen.ACTION_SUPER:
                mJuRenSerialPortHelper.sendSuper();
                break;
            case DeviceAction.JuRen.ACTION_SETTING:
                mJuRenSerialPortHelper.sendSetting();
                break;
            case DeviceAction.JuRen.ACTION_KILL:
                mJuRenSerialPortHelper.sendKill();
                break;
        }
    }

    @Override
    public void pull() {

    }
}
