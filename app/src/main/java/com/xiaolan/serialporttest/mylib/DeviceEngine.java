package com.xiaolan.serialporttest.mylib;

import java.io.IOException;

/**
 * 提供给外部使用的设备管理引擎
 */
public class DeviceEngine {

    private DeviceEngine() { }

    public static DeviceEngine getInstance() {
        return DeviceEngineHolder.DEVICE_ENGINE;
    }

    private static class DeviceEngineHolder{
        private static final DeviceEngine DEVICE_ENGINE = new DeviceEngine();
    }

    /**
     * 打开串口
     */
    public void open() throws IOException {
        JuRenPlusWashManager.getInstance().open();
    }

    /**
     * 关闭串口
     */
    public void close() throws IOException {
        JuRenPlusWashManager.getInstance().close();
    }

    public void push(int action) {
        JuRenPlusWashManager.getInstance().push(action);
    }

    public void pull() {
        JuRenPlusWashManager.getInstance().pull();
    }

    public void setPort(String port) {
        JuRenPlusWashManager.getInstance().setPort(port);
    }

    public void setBaudRate(int baudRate) {
        JuRenPlusWashManager.getInstance().setBaudRate(baudRate);
    }

    public void setDataBits(String dataBits) {
        JuRenPlusWashManager.getInstance().setDataBits(dataBits);
    }

    public void setStopBits(String stopBits) {
        JuRenPlusWashManager.getInstance().setStopBits(stopBits);
    }

    public void setParityBits(String parityBits) {
        JuRenPlusWashManager.getInstance().setParityBits(parityBits);
    }

    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        JuRenPlusWashManager.getInstance().setOnSendInstructionListener(onSendInstructionListener);
    }

    public void setOnCurrentStatusListener(CurrentStatusListener onCurrentStatusListener) {
        JuRenPlusWashManager.getInstance().setOnCurrentStatusListener(onCurrentStatusListener);
    }

    public void setOnSerialPortConnectListener(SerialPortReadDataListener onSerialPortReadDataListener) {
        JuRenPlusWashManager.getInstance().setOnSerialPortConnectListener(onSerialPortReadDataListener);
    }

    public String getPort() {
        return JuRenPlusWashManager.getInstance().getPort();
    }

    public int getBaudRate() {
        return JuRenPlusWashManager.getInstance().getBaudRate();
    }

    public String getDataBits() {
        return JuRenPlusWashManager.getInstance().getDataBits();
    }

    public String getStopBits() {
        return JuRenPlusWashManager.getInstance().getStopBits();
    }

    public String getParityBits() {
        return JuRenPlusWashManager.getInstance().getParityBits();
    }

    public boolean isOpen() {
        return JuRenPlusWashManager.getInstance().isOpen();
    }

    public void sendHex(String hex) {
        JuRenPlusWashManager.getInstance().sendHex(hex);
    }

    public void sendText(String text) {
        JuRenPlusWashManager.getInstance().sendText(text);
    }
}
