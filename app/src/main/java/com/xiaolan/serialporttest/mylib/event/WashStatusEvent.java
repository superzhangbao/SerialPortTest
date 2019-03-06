package com.xiaolan.serialporttest.mylib.event;

public class WashStatusEvent {
    private int mWashMode;
    private int mLights1;
    private int mLights2;
    private int mLights3;
    private int mLightSupper;
    private int mLightlock;
    private int mIsWashing;
    private int mViewStep;
    private int mErr;
    private int mMsgInt;
    private String mText;
    private String mText2;
    private boolean mIsSetting;
    private StringBuilder mLogmsg;


    public WashStatusEvent(boolean isSetting, int washMode, int lights1, int lights2, int lights3, int lightsupper, int lightlock, int isWashing, int viewStep, int err, int msgInt, String text,String text2, StringBuilder logmsg) {
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
        mText2 = text2;
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

    public int getErr() {
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

    public String getText2() {
        return mText2;
    }
}
