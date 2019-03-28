package com.xiaolan.serialporttest.wash.jurenpro;

import android.util.Log;

import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.utils.LightMsg;

import java.util.Map;

/**
 * 洗衣机状态管理类
 */
public class JuRenProWashStatus {
    private static final String TAG = "JuRenProWashStatus";
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
    private boolean mIsSetting = false;
    private StringBuilder mLogmsg;

    private WashStatusEvent mWashStatusEvent;

    public JuRenProWashStatus() {
        if (mLogmsg == null) {
            mLogmsg = new StringBuilder();
        }
        if (mWashStatusEvent == null) {
            mWashStatusEvent = new WashStatusEvent();
        }
    }

    public WashStatusEvent analyseStatus(byte[] msg) {
        if (msg.length == 30) {
            //洗衣模式
            mWashMode = (msg[2] & 0xf);
            //洗涤灯
            mLights1 = (msg[3] & 0x80) >> 7;
            //漂洗灯
            mLights2 = (msg[3] & 0x40) >> 6;
            //脱水灯
            mLights3 = (msg[3] & 0x20) >> 5;
            //是否加强洗
            mLightSupper = ((msg[21] & 0xf0) >> 6 > 0) ? 1 : 0;//1是 0否
            //门锁的状态
            mLightlock = (msg[23] & 0x01);//0 未锁  01 已锁
            //30  40可用来区分是否在洗涤  10&0xf0 = 10
            mIsWashing = (msg[5] & 0xf0);//30未洗 40在洗  10设置状态
            //获取工作状态值 如msg[3] = E1,viewStep = 1 ; msg[3] = E6,viewStep = 6(洗涤);msg[3] = 67 viewStep = 7(漂洗);msg[3] = 28 viewStep = 8(脱水); viewStep = 2(暂停)
            mViewStep = (msg[3] & 0xf);
            //故障代码 比如02
            mErr = msg[4];
            //3C & 0xf = C;3D & 0xf = D;  03&0xf = 3
            mMsgInt = msg[6] & 0xf;
            //屏显(当msgInt = 0xC || 0xD || 0x3)的时候，屏幕显示的就是msg[7]的值
            if (mMsgInt == 0xc || mMsgInt == 0xd || mMsgInt == 0x3) {
                mText = "" + msg[7];
            } else {
                Map<Byte, String> textTable = LightMsg.getText();
                mText = textTable.get(msg[7]) + textTable.get(msg[8]) + textTable.get(msg[13]) + textTable.get(msg[14]);
            }
            mIsSetting = mIsWashing == 0x10;
            Log.e(TAG, "lights1:" + mLights1 + "--" + "lights2:" + mLights2 + "--" + "lights3:" + mLights3 + "--" + "lightsupper:" + mLightSupper + "--" + "lightlock:" + mLightlock + "--"
                    + "mIsWashing:" + mIsWashing + "--" + "viewStep:" + mViewStep + "--" + "err:" + mErr + "--" + "msgInt:" + mMsgInt + "--" + "text:" + mText);
            if (mLogmsg != null && mLogmsg.length() > 0) {
                mLogmsg.delete(0, mLogmsg.length());
                mLogmsg.append("当前模式：----->");
            }
            //分析当前洗衣机的状态
            //1.判断当前是否是设置模式
            if (mIsWashing == 0x10) {//设置模式
                Log.e(TAG, "设置模式：" + "是 --->屏显：" + mText);
                mLogmsg.append("设置模式：" + "是 --->屏显：").append(mText).append("\r\n");
            } else {//正常模式
                Log.e(TAG, "设置模式：" + "否");
                mLogmsg.append("设置模式：" + "否").append("\r\n");
                //判断是否是错误状态
                if (isError()) {//是错误状态
                    Log.e(TAG, "错误状态：" + "是");
                    mLogmsg.append("错误状态：" + "是").append("\r\n");
                    switch (mErr) {
                        case 0x02:
                            Log.e(TAG, "错误原因：" + "进水管故障");
                            mLogmsg.append("错误原因：" + "进水管故障").append("\r\n");
                            break;
                        case 0x11:
                            Log.e(TAG, "错误原因：" + "关门失败");
                            mLogmsg.append("错误原因：" + "关门失败").append("\r\n");
                            break;
                        default:
                            Log.e(TAG, "错误原因：" + "未知错误");
                            mLogmsg.append("错误原因：" + "未知错误").append("\r\n");
                            break;
                    }
                    //屏显
                    Log.e(TAG, "屏显：" + mText);
                    mLogmsg.append("屏显：").append(mText).append("\r\n");
                } else {//不是错误状态
                    Log.e(TAG, "错误状态：" + "否");
                    mLogmsg.append("错误状态：" + "否").append("\r\n");
                    if (mWashMode == 0x00 && mViewStep == 0x01) {//模式是0,工作状态是1
                        Log.e(TAG, "push状态：" + "是");
                        mLogmsg.append("push状态：" + "是").append("\r\n");
                    }
                    //洗衣模式
                    switch (mWashMode) {
                        case 0x02:
                            Log.e(TAG, "洗衣模式：" + "热水");
                            mLogmsg.append("洗衣模式：" + "热水").append("\r\n");
                            break;
                        case 0x03:
                            Log.e(TAG, "洗衣模式：" + "温水");
                            mLogmsg.append("洗衣模式：" + "温水").append("\r\n");
                            break;
                        case 0x04:
                            Log.e(TAG, "洗衣模式：" + "冷水");
                            mLogmsg.append("洗衣模式：" + "冷水").append("\r\n");
                            break;
                        case 0x05:
                            Log.e(TAG, "洗衣模式：" + "精致衣物");
                            mLogmsg.append("洗衣模式：" + "精致衣物").append("\r\n");
                            break;
                        case 0x08:
                            Log.e(TAG, "洗衣模式：" + "桶自洁");
                            mLogmsg.append("洗衣模式：" + "桶自洁").append("\r\n");
                            break;
                    }
                    //是否是加强洗
                    if (mLightSupper == 1) {
                        Log.e(TAG, "加强洗：" + "是");
                        mLogmsg.append("加强洗：" + "是").append("\r\n");
                    } else if (mLightSupper == 0) {
                        Log.e(TAG, "加强洗：" + "否");
                        mLogmsg.append("加强洗：" + "否").append("\r\n");
                    }
                    if (mIsWashing == 0x30) {//洗衣状态
                        Log.e(TAG, "洗衣状态：" + "未开始洗衣");
                        mLogmsg.append("洗衣状态：" + "未开始洗衣").append("\r\n");
                    } else if (mIsWashing == 0x40) {
                        if (mViewStep == 6) {//洗涤状态
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "正在洗涤");
                            mLogmsg.append("洗衣状态：已开始洗衣--->" + "正在洗涤").append("\r\n");
                        } else if (mViewStep == 2) {
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "暂停洗衣");
                            mLogmsg.append("洗衣状态：已开始洗衣--->" + "暂停洗衣").append("\r\n");
                        } else if (mViewStep == 1) {
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "刚开始洗衣");
                            mLogmsg.append("洗衣状态：已开始洗衣--->" + "刚开始洗衣").append("\r\n");
                        } else if (mViewStep == 7) {//漂洗状态
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "正在漂洗");
                            mLogmsg.append("洗衣状态：已开始洗衣--->" + "正在漂洗").append("\r\n");
                        } else if (mViewStep == 8) {//脱水状态
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "正在脱水");
                            mLogmsg.append("洗衣状态：已开始洗衣--->" + "正在脱水").append("\r\n");
                        }
                    } else {
                        Log.e(TAG, "洗衣状态：未知");
                        mLogmsg.append("洗衣状态：未知").append("\r\n");
                    }
                    //是否是锁门状态
                    if (mLightlock == 1) {//是锁门状态
                        Log.e(TAG, "门锁状态：" + "已锁");
                        mLogmsg.append("门锁状态：" + "已锁").append("\r\n");
                    } else {//门未锁
                        Log.e(TAG, "门锁状态：" + "未锁");
                        mLogmsg.append("门锁状态：" + "未锁").append("\r\n");
                    }
                    //屏显
                    Log.e(TAG, "屏显：" + mText);
                    mLogmsg.append("屏显：").append(mText).append("\r\n");
                }
            }
        }
        if (mWashStatusEvent != null) {
            mWashStatusEvent.setSetting(mIsSetting);
            mWashStatusEvent.setWashMode(mWashMode);
            mWashStatusEvent.setLights1(mLights1);
            mWashStatusEvent.setLights2(mLights2);
            mWashStatusEvent.setLights3(mLights3);
            mWashStatusEvent.setLightSupper(mLightSupper);
            mWashStatusEvent.setLightlock(mLightlock);
            mWashStatusEvent.setIsWashing(mIsWashing);
            mWashStatusEvent.setViewStep(mViewStep);
            mWashStatusEvent.setErr(mErr);
            mWashStatusEvent.setMsgInt(mMsgInt);
            mWashStatusEvent.setText(mText);
            mWashStatusEvent.setLogmsg(mLogmsg);
        }else {
            mWashStatusEvent = new WashStatusEvent(mIsSetting, mWashMode, mLights1, mLights2, mLights3, mLightSupper, mLightlock, mIsWashing, mViewStep, mErr, mMsgInt, mText,"", mLogmsg);
        }
        return mWashStatusEvent;
    }

    /**
     * 判断是否处于错误状态
     */
    public boolean isError() {
        return mErr > 0;
    }

    /**
     * 判断是否是闲置的
     */
    public boolean isIdle() {
        return !isRunning() && !isError();
    }

    /**
     * 判断是否处于运行状态
     */
    public boolean isRunning() {//mViewStep == 2是暂停状态
        return (mViewStep == 6 || mViewStep == 7 || mViewStep == 8) && mErr == 0;
    }

    /**
     * 灯是否亮
     */
    public boolean isOn(int light) {
        return light > 0;
    }

    public boolean isOff(int light) {
        return light == 0;
    }
}
