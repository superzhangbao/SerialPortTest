package com.xiaolan.serialporttest.wash.jurenplus;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngineIService;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;

import java.io.IOException;

/*
 *巨人Pro洗衣机
 */
public class JuRenPlusWashManager implements DeviceEngineIService {

    private JuRenPlusSerialPortHelper mJuRenPlusSerialPortHelper;

    private JuRenPlusWashManager() {
        if (mJuRenPlusSerialPortHelper == null) {
            mJuRenPlusSerialPortHelper = new JuRenPlusSerialPortHelper();
        }
    }

    public static JuRenPlusWashManager getInstance() {
        return JuRenPlusWashHolder.INSTANCE;
    }

    private static class JuRenPlusWashHolder {
        private static JuRenPlusWashManager INSTANCE = new JuRenPlusWashManager();
    }

    @Override
    public void open() throws IOException {
        mJuRenPlusSerialPortHelper.juRenProOpen();
    }

    @Override
    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mJuRenPlusSerialPortHelper.setOnSendInstructionListener(onSendInstructionListener);
    }

    @Override
    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mJuRenPlusSerialPortHelper.setOnCurrentStatusListener(currentStatusListener);
    }

    @Override
    public void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener) {
        mJuRenPlusSerialPortHelper.setOnSerialPortOnlineListener(onSerialPortOnlineListener);
    }

    @Override
    public boolean isOpen() {
        return mJuRenPlusSerialPortHelper.isOpen();
    }

    @Override
    public void close() throws IOException {
        mJuRenPlusSerialPortHelper.close();
    }

    @Override
    public void push(int action) {
        switch (action) {
            case DeviceAction.JuRenPlus.ACTION_START:
                mJuRenPlusSerialPortHelper.sendStartOrStop();
                break;
            case DeviceAction.JuRenPlus.ACTION_HOT:
                mJuRenPlusSerialPortHelper.sendHot();
                break;
            case DeviceAction.JuRenPlus.ACTION_WARM:
                mJuRenPlusSerialPortHelper.sendWarm();
                break;
            case DeviceAction.JuRenPlus.ACTION_COLD:
                mJuRenPlusSerialPortHelper.sendCold();
                break;
            case DeviceAction.JuRenPlus.ACTION_DELICATES:
                mJuRenPlusSerialPortHelper.sendDelicates();
                break;
            case DeviceAction.JuRenPlus.ACTION_SUPER:
                mJuRenPlusSerialPortHelper.sendSuper();
                break;
            case DeviceAction.JuRenPlus.ACTION_SETTING:
                mJuRenPlusSerialPortHelper.sendSetting();
                break;
            case DeviceAction.JuRenPlus.ACTION_KILL:
                mJuRenPlusSerialPortHelper.sendKill();
                break;
            case DeviceAction.JuRenPlus.ACTION_RESET:
                mJuRenPlusSerialPortHelper.sendReset();
                break;
        }
    }

    @Override
    public void pull() {

    }
}


