package com.xiaolan.serialporttest.mylib;

import java.io.IOException;

public class JuRenPlusWashManager implements DeviceEngineIService {
    private static final String TAG = "JRPStatusManager";

    private SerialPortManager2 mSerialPortManager2;

    private JuRenPlusWashManager() {
        if (mSerialPortManager2 == null) {
            mSerialPortManager2 = new SerialPortManager2();
        }
    }

    public static JuRenPlusWashManager getInstance() {
        return JuRenPlusWashStatusHolder.INSTANCE;
    }

    private static class JuRenPlusWashStatusHolder {
        private static JuRenPlusWashManager INSTANCE = new JuRenPlusWashManager();
    }

    @Override
    public void selectDevice() {

    }

    @Override
    public void open() throws IOException {
        mSerialPortManager2.juRenPlusOpen();
    }

    @Override
    public void setPort(String port) {
        mSerialPortManager2.setPort(port);
    }

    @Override
    public void setBaudRate(int baudRate) {
        mSerialPortManager2.setBaudRate(baudRate);
    }

    @Override
    public void setDataBits(String dataBits) {
        mSerialPortManager2.setDataBits(dataBits);
    }

    @Override
    public void setStopBits(String stopBits) {
        mSerialPortManager2.setStopBits(stopBits);
    }

    @Override
    public void setParityBits(String parityBits) {
        mSerialPortManager2.setParityBits(parityBits);
    }

    @Override
    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mSerialPortManager2.setOnSendInstructionListener(onSendInstructionListener);
    }

    @Override
    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mSerialPortManager2.setOnCurrentStatusListener(currentStatusListener);
    }

    @Override
    public void setOnSerialPortConnectListener(SerialPortReadDataListener onSerialPortReadDataListener) {
        mSerialPortManager2.setOnSerialPortConnectListener(onSerialPortReadDataListener);
    }

    @Override
    public String getPort() {
        return mSerialPortManager2.getPort();
    }

    @Override
    public int getBaudRate() {
        return mSerialPortManager2.getBaudRate();
    }

    @Override
    public String getDataBits() {
        return mSerialPortManager2.getDataBits();
    }

    @Override
    public String getStopBits() {
        return mSerialPortManager2.getStopBits();
    }

    @Override
    public String getParityBits() {
        return mSerialPortManager2.getParityBits();
    }

    @Override
    public boolean isOpen() {
        return mSerialPortManager2.isOpen();
    }

    @Override
    public void sendHex(String s) {
        mSerialPortManager2.sendHex(s);
    }

    @Override
    public void sendText(String s) {
        mSerialPortManager2.sendTxt(s);
    }

    @Override
    public void close() throws IOException {
        mSerialPortManager2.close();
    }

    @Override
    public void push(int action) {
        switch (action) {
            case DeviceAction.ACTION_START:
                mSerialPortManager2.sendStartOrStop();
                break;
            case DeviceAction.ACTION_HOT:
                mSerialPortManager2.sendHot();
                break;
            case DeviceAction.ACTION_WARM:
                mSerialPortManager2.sendWarm();
                break;
            case DeviceAction.ACTION_COLD:
                mSerialPortManager2.sendCold();
                break;
            case DeviceAction.ACTION_DELICATES:
                mSerialPortManager2.sendDelicates();
                break;
            case DeviceAction.ACTION_SUPER:
                mSerialPortManager2.sendSuper();
                break;
            case DeviceAction.ACTION_SETTING:
                mSerialPortManager2.sendSetting();
                break;
            case DeviceAction.ACTION_KILL:
                mSerialPortManager2.sendKill();
                break;
        }
    }

    @Override
    public void pull() {

    }
}


