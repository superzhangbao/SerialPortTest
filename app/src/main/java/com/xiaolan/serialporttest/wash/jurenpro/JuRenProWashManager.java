package com.xiaolan.serialporttest.wash.jurenpro;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngineIService;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;

import java.io.IOException;

/*
 *巨人Pro洗衣机
 */
public class JuRenProWashManager implements DeviceEngineIService {

    private JuRenProSerialPortHelper mJuRenProSerialPortHelper;

    private JuRenProWashManager() {
        if (mJuRenProSerialPortHelper == null) {
            mJuRenProSerialPortHelper = new JuRenProSerialPortHelper();
        }
    }

    public static JuRenProWashManager getInstance() {
        return JuRenProWashHolder.INSTANCE;
    }

    private static class JuRenProWashHolder {
        private static JuRenProWashManager INSTANCE = new JuRenProWashManager();
    }

    @Override
    public void open() throws IOException {
        mJuRenProSerialPortHelper.juRenProOpen();
    }

    @Override
    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mJuRenProSerialPortHelper.setOnSendInstructionListener(onSendInstructionListener);
    }

    @Override
    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mJuRenProSerialPortHelper.setOnCurrentStatusListener(currentStatusListener);
    }

    @Override
    public void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener) {
        mJuRenProSerialPortHelper.setOnSerialPortOnlineListener(onSerialPortOnlineListener);
    }

    @Override
    public boolean isOpen() {
        return mJuRenProSerialPortHelper.isOpen();
    }

    @Override
    public void close() throws IOException {
        mJuRenProSerialPortHelper.close();
    }

    @Override
    public void push(int action) {
        switch (action) {
            case DeviceAction.JuRenPro.ACTION_START:
                mJuRenProSerialPortHelper.sendStartOrStop();
                break;
            case DeviceAction.JuRenPro.ACTION_HOT:
                mJuRenProSerialPortHelper.sendHot();
                break;
            case DeviceAction.JuRenPro.ACTION_WARM:
                mJuRenProSerialPortHelper.sendWarm();
                break;
            case DeviceAction.JuRenPro.ACTION_COLD:
                mJuRenProSerialPortHelper.sendCold();
                break;
            case DeviceAction.JuRenPro.ACTION_DELICATES:
                mJuRenProSerialPortHelper.sendDelicates();
                break;
            case DeviceAction.JuRenPro.ACTION_SUPER:
                mJuRenProSerialPortHelper.sendSuper();
                break;
            case DeviceAction.JuRenPro.ACTION_SETTING:
                mJuRenProSerialPortHelper.sendSetting();
                break;
            case DeviceAction.JuRenPro.ACTION_KILL:
                mJuRenProSerialPortHelper.sendKill();
                break;
            case DeviceAction.JuRenPro.ACTION_RESET:
                mJuRenProSerialPortHelper.sendReset();
                break;
            case DeviceAction.JuRenPro.ACTION_SELF_CLEANING:
                mJuRenProSerialPortHelper.sendSelfCleaning();
                break;
        }
    }
}


