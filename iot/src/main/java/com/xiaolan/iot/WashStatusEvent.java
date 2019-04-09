package com.xiaolan.iot;

/**
 * 洗衣机状态类
 */
public class WashStatusEvent {
    private boolean isSetting;
    private int washMode;
    private int lights1;
    private int lights2;
    private int lights3;
    private int lightSupper;
    private int lightlock;
    private int isWashing;
    private int viewStep;
    private int err;
    private int msgInt;
    private String text;
    private String text2;

    private StringBuilder logmsg;

    public WashStatusEvent() {}

    public WashStatusEvent(boolean isSetting, int washMode, int lights1, int lights2, int lights3, int lightSupper, int lightlock, int isWashing, int viewStep, int err, int msgInt, String text, String text2, StringBuilder logmsg) {
        this.isSetting = isSetting;
        this.washMode = washMode;
        this.lights1 = lights1;
        this.lights2 = lights2;
        this.lights3 = lights3;
        this.lightSupper = lightSupper;
        this.lightlock = lightlock;
        this.isWashing = isWashing;
        this.viewStep = viewStep;
        this.err = err;
        this.msgInt = msgInt;
        this.text = text;
        this.text2 = text2;
        this.logmsg = logmsg;
    }

    public int getWashMode() {
        return washMode;
    }

    public void setWashMode(int washMode) {
        this.washMode = washMode;
    }

    public int getLights1() {
        return lights1;
    }

    public void setLights1(int lights1) {
        this.lights1 = lights1;
    }

    public int getLights2() {
        return lights2;
    }

    public void setLights2(int lights2) {
        this.lights2 = lights2;
    }

    public int getLights3() {
        return lights3;
    }

    public void setLights3(int lights3) {
        this.lights3 = lights3;
    }

    public int getLightSupper() {
        return lightSupper;
    }

    public void setLightSupper(int lightSupper) {
        this.lightSupper = lightSupper;
    }

    public int getLightlock() {
        return lightlock;
    }

    public void setLightlock(int lightlock) {
        this.lightlock = lightlock;
    }

    public int getIsWashing() {
        return isWashing;
    }

    public void setIsWashing(int isWashing) {
        this.isWashing = isWashing;
    }

    public int getViewStep() {
        return viewStep;
    }

    public void setViewStep(int viewStep) {
        this.viewStep = viewStep;
    }

    public int getErr() {
        return err;
    }

    public void setErr(int err) {
        this.err = err;
    }

    public int getMsgInt() {
        return msgInt;
    }

    public void setMsgInt(int msgInt) {
        this.msgInt = msgInt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText2() {
        return text2;
    }

    public void setText2(String text2) {
        this.text2 = text2;
    }

    public boolean isSetting() {
        return isSetting;
    }

    public void setSetting(boolean setting) {
        isSetting = setting;
    }

    public StringBuilder getLogmsg() {
        return logmsg;
    }

    public void setLogmsg(StringBuilder logmsg) {
        this.logmsg = logmsg;
    }

    @Override
    public String toString() {
        return "WashStatusEvent{" +
                "isSetting=" + isSetting +
                ", washMode=" + washMode +
                ", lights1=" + lights1 +
                ", lights2=" + lights2 +
                ", lights3=" + lights3 +
                ", lightSupper=" + lightSupper +
                ", lightlock=" + lightlock +
                ", isWashing=" + isWashing +
                ", viewStep=" + viewStep +
                ", err=" + err +
                ", msgInt=" + msgInt +
                ", text='" + text + '\'' +
                ", text2='" + text2 + '\'' +
                ", logmsg=" + logmsg +
                '}';
    }
}
