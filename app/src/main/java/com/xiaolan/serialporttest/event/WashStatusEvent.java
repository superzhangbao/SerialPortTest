package com.xiaolan.serialporttest.event;

public class WashStatusEvent {
    private int mWashMode;
    private int mLights1;
    private int mLights2;
    private int mLights3;
    private int mLightSupper;
    private int mLightlock;
    private int mIsWashing;
    private int mViewStep;
    private byte mErr;
    private int mMsgInt;
    private String mText;
    private boolean mIsSetting;
    private StringBuilder mLogmsg;

    public WashStatusEvent(boolean isSetting,int washMode, int lights1, int lights2, int lights3, int lightsupper, int lightlock, int isWashing, int viewStep, byte err, int msgInt, String text,StringBuilder logmsg) {
        mIsSetting = isSetting;
        mWashMode = washMode;
        mLights1 = lights1;
        mLights2 = lights2;
        mLights3 = lights3;
        mLightSupper = lightsupper;
        mLightlock = lightlock;
        mIsWashing = isWashing;
        mViewStep = viewStep;
        mErr = err;
        mMsgInt = msgInt;
        mText = text;
        mLogmsg = logmsg;
    }

    public int getWashMode() {
        return mWashMode;
    }

    public int getLights1() {
        return mLights1;
    }

    public int getLights2() {
        return mLights2;
    }

    public int getLights3() {
        return mLights3;
    }

    public int getLightSupper() {
        return mLightSupper;
    }

    public int getLightlock() {
        return mLightlock;
    }

    public int getIsWashing() {
        return mIsWashing;
    }

    public int getViewStep() {
        return mViewStep;
    }

    public byte getErr() {
        return mErr;
    }

    public int getMsgInt() {
        return mMsgInt;
    }

    public String getText() {
        return mText;
    }

    public boolean isSetting() {
        return mIsSetting;
    }

    public StringBuilder getLogmsg() {
        return mLogmsg;
    }
}
