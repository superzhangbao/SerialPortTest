package com.xiaolan.serialporttest.wash.xjl;

import android.util.Log;

import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.utils.LightMsg;

import java.util.Map;

public class XjlWashStatus {

    private static final String TAG = "XjlWashStatus";
    private int mWashMode = 0;
    private int mLightSupper = 0;   // 加强
    private int lightst = 0;    // 开始
    private int mLights1 = 0;    // 第1步
    private int mLights2 = 0;    // 第2步
    private int mLights3 = 0;    // 第3步
    private int mLightlock = 0;    // 锁
    private int view;
    private int mViewStep;
    private int mErr = 0;
    private int mMsgInt = 0;
    private String mText;
    private String mText2;
    private Map<Byte, String> textTable = LightMsg.getText();
    private boolean mIsSetting = false;
    private int mIsWashing = 0;
    private StringBuilder mLogmsg;
    private WashStatusEvent mWashStatusEvent;

    public XjlWashStatus() {
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
            mLightSupper = ((msg[21] & 0xf0) >> 6 > 0) ? 1 : 0;
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
                mText = textTable.get(msg[7]) + textTable.get(msg[8]) + textTable.get(msg[12]) + textTable.get(msg[13]);
            }
            mText2 = "" + msg[11] + msg[10];
            if (mText2.length() > 1 && mText2.startsWith("0")) {
                mText2 = mText2.substring(1, mText2.length());
            }
            mIsSetting = mIsWashing == 0x10;
            Log.e(TAG, "----------------------------start--------------------------------------");
            Log.e(TAG, "lights1:" + mLights1 + "--" + "lights2:" + mLights2 + "--" + "lights3:" + mLights3 + "--" + "lightsupper:" + mLightSupper + "--" + "lightlock:" + mLightlock + "--"
                    + "mIsWashing:" + mIsWashing + "--" + "viewStep:" + mViewStep + "--" + "err:" + mErr + "--" + "msgInt:" + mMsgInt + "--" + "text:" + mText + "--" + "text2:" + mText2);
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
                            Log.e(TAG, "错误原因：" + "进水管故障,1E");
                            mLogmsg.append("错误原因：" + "进水管故障,1E").append("\r\n");
                            break;
                        case 0x01:
                        case 0x17:
                            Log.e(TAG, "错误原因：" + "关门失败,dE");
                            mLogmsg.append("错误原因：" + "关门失败,dE").append("\r\n");
                            break;
                        case 0x04:
                            Log.e(TAG, "错误原因：" + "不平衡,UE");
                            mLogmsg.append("错误原因：" + "不平衡,UE").append("\r\n");
                            break;
                        default:
                            Log.e(TAG, "错误原因：" + "未知错误");
                            mLogmsg.append("错误原因：" + "未知错误").append("\r\n");
                            break;
                    }
                    //屏显
                    Log.e(TAG, "屏显：text1:" + mText + "----text2:" + mText2);
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
                    Log.e(TAG, "屏显：text1:" + mText + "----text2:" + mText2);
                    mLogmsg.append("屏显：").append(mText).append("----text2:").append(mText2).append("\r\n");
                }
            }
            Log.e(TAG, "-----------------------------end---------------------------------------");
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
            mWashStatusEvent.setText2(mText2);
            mWashStatusEvent.setLogmsg(mLogmsg);
            mWashStatusEvent.setIsRunning(isRunning());
            mWashStatusEvent.setIsError(isError());
            mWashStatusEvent.setIsIdle(isIdle());
            mWashStatusEvent.setWashPeriod(getPeriod(mViewStep));
        }else {
            mWashStatusEvent = new WashStatusEvent(mIsSetting, mWashMode, mLights1, mLights2, mLights3,
                    mLightSupper, mLightlock, mIsWashing, mViewStep, mErr, mMsgInt, mText,mText2,
                    mLogmsg,isRunning(),isError(),isIdle(),getPeriod(mViewStep));
        }
        return mWashStatusEvent;    }

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
        return (mViewStep == 6 || mViewStep == 7 || mViewStep == 8 || mViewStep == 2||mViewStep == 0x0a) && mErr == 0;
    }

    public int getPeriod(int viewStep) {
        switch (viewStep) {
            case 6:
                return 0;//洗涤
            case 7:
                return 1;//漂洗
            case 8:
                return 2;//脱水
            default:
                return -1;
        }
    }
}
