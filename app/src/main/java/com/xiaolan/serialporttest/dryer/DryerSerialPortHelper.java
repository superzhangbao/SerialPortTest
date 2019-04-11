package com.xiaolan.serialporttest.dryer;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
import com.xiaolan.serialporttest.mylib.utils.LightMsg;
import com.xiaolan.serialporttest.mylib.utils.MyFunc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class DryerSerialPortHelper implements DeviceEngineDryerService {
    private static final String TAG = "DryerSerialPortHelper";
    private static final long MAX_CLICK = 100;
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private String sPort = "/dev/ttyS3";//默认串口号
    private int iBaudRate = 9600;//波特率
    private boolean _isOpen = false;
    private BufferedInputStream mBufferedInputStream;
    private BufferedOutputStream mBufferedOutputStream;
    private int seq = 1;
    private int mKey = -1;
    private boolean isOnline = false;
    private boolean hasOnline = false;
    private long mPreOnlineTime = 0;
    private long readThreadStartTime = 0;
    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortOnlineListener mSerialPortOnlineListener;
    private CurrentStatusListener mCurrentStatusListener;

    private int lamp_text;
    private int lamp_1;
    private int lamp_2;
    private int lamp_3;
    private int lamp_4;
    private int lamp_5;
    private int lamp_6;
    private int lamp_7;
    /*烘干机 空闲状态*/
    public static final int STATE_FREE = 0;
    /*烘干机 运行状态-dry*/
    public static final int STATE_RUN_DRY = 1;
    /*烘干机 运行状态-cooling*/
    public static final int STATE_RUN_COOLING = 2;
    /*烘干机 开门状态*/
    public static final int STATE_OPEN = 3;
    /*烘干机 结束状态*/
    public static final int STATE_END = 4;
    /*烘干机 设置状态*/
    public static final int STATE_SETTING = 5;
    /*烘干机 未知状态*/
    public static final int STATE_UNKNOWN = 6;

    /*灯 不亮*/
    private static final int LAMP_CLOSE = 0;
    /*灯 闪烁*/
    private static final int LAMP_GLINT = 1;
    /*灯 长亮*/
    private static final int LAMP_BRIGHT = -1;
    /*灯 过度状态*/
    private static final int LAMP_NORMAL = -2;
    private volatile int state;
    private volatile StringBuilder showStr;
    private Map<Byte, String> mText;
    private long click = 0;
    private Disposable mInitSetDisposable;
    private Disposable mKillDisposable;
    private boolean initSuccess = false;//是否初始化成功
    private DryerStatus mDryerStatus;
    private boolean firstData = true;//是否是串口上线后的第一条数据
    private Disposable mHighDisposable;
    private Disposable mMedDisposable;
    private Disposable mLowDisposable;
    private Disposable mNoHeatDisposable;
    private Disposable mSettingDisposable;
    private byte[] mCoinByte;
    private byte[] mKeyByte;
    private volatile String mShowText;
    private StringBuilder showWord;


    public DryerSerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

    public DryerSerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
        mText = LightMsg.getText();
        mCoinByte = new byte[]{0x7F, (byte) (seq & 0xff), (byte) 0xF1, 0x00};
        mKeyByte = new byte[]{0x7F, (byte) (seq & 0xff), (byte) 0xF0, (byte) (0)};
        showStr = new StringBuilder();
        showWord = new StringBuilder();
    }

    private class ReadThread extends Thread {
        private static final int DATA_LENGTH = 32;

        @Override
        public void run() {
            super.run();
            //读取数据
            while (isOpen()) {
                try {
                    if (mBufferedInputStream == null)
                        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
                    byte[] buffer = new byte[DATA_LENGTH];
                    int len;
                    if (mBufferedInputStream.available() > 0) {
                        try {
                            sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        len = mBufferedInputStream.read(buffer);
                        if (len != -1) {
//                            Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
                            if (len >= 14) {
                                //读烘干机上报报文
                                dryerRead(buffer, len);
                            }
                        }
                    } else {
                        long now = System.currentTimeMillis();
                        if (hasOnline) {
                            //上线过
                            if (now - mPreOnlineTime > 5000) {
                                //掉线
                                mPreOnlineTime = now;
                                isOnline = false;
                                firstData = true;
                                Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                        .doOnComplete(() -> {
                                            if (mSerialPortOnlineListener != null) {
                                                mSerialPortOnlineListener.onSerialPortOffline("串口掉线");
                                            }
                                        }).subscribe();
                            }
                        } else {
                            //没上线过
                            if (!isOnline) {
                                long current = System.currentTimeMillis();
                                if (current - readThreadStartTime > 5000) {
                                    //5秒未读到数据
                                    Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                            .doOnComplete(() -> {
                                                if (mSerialPortOnlineListener != null) {
                                                    mSerialPortOnlineListener.onSerialPortOffline("串口未连接");
                                                }
                                            }).subscribe();
                                    readThreadStartTime = current;
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Log.e(TAG, "run: 数据读取异常：" + t.toString());
                    break;
                }
            }
        }
    }

    private void dryerRead(byte[] buf, int len) {
        int size = 14;
        int off7F = ArrayUtils.indexOf(buf, (byte) 0x7F);
        if (off7F >= 0) {
            if (len >= off7F + size && buf[off7F + 1] == 0x70 && (buf[off7F + 2] + buf[off7F + 3] + buf[off7F + 4] + buf[off7F + 5]) != 0) {
                byte[] valid = ArrayUtils.subarray(buf, off7F, off7F + size);
//                Log.e(TAG,"valid:"+MyFunc.ByteArrToHex(valid));
                //判断串口是否掉线
                if (!isOnline) {
                    isOnline = true;
                    hasOnline = true;
                    mPreOnlineTime = System.currentTimeMillis();
                    Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete(() -> {
                                if (mSerialPortOnlineListener != null) {
                                    mSerialPortOnlineListener.onSerialPortOnline(valid);
                                    Log.e(TAG, "串口上线");
                                }
                            }).subscribe();
                } else {
                    mPreOnlineTime = System.currentTimeMillis();
                }

                boolean isDiffWord = dryerMsg(valid);
//              Log.e(TAG, "isDiffWord:" + isDiffWord);
                int stateNow = dryerState();
//              Log.e(TAG, "state:" + state);
                if (firstData) {
                    firstData = false;
                    state = stateNow;
                    Log.e(TAG, "dryerRead: -----------state:" + getStatus(state) + "  byte[]:" + MyFunc.ByteArrToHex(valid) + " , text:" + showStr);
                    if (mDryerStatus == null) {
                        mDryerStatus = new DryerStatus(state, mShowText);
                    } else {
                        mDryerStatus.setStatus(state);
                        mDryerStatus.setText(mShowText);
                    }
                    sendCurrentStatus();
                    return;
                }
                if (isDiffWord || stateNow != state) {
                    state = stateNow;
                    Log.e(TAG, "isDiffWord: -----------state:" + getStatus(state) + "  byte[]:" + MyFunc.ByteArrToHex(valid) + " , text:" + showStr);
                    if (mDryerStatus == null) {
                        mDryerStatus = new DryerStatus(state, mShowText);
                    } else {
                        mDryerStatus.setStatus(state);
                        mDryerStatus.setText(mShowText);
                    }
                    sendCurrentStatus();
                }
            }
        }
    }

    private String getStatus(int state) {
        switch (state) {
            case STATE_FREE:
                return "STATE_FREE";
            case STATE_RUN_DRY:
                return "STATE_RUN_DRY";
            case STATE_RUN_COOLING:
                return "STATE_RUN_COOLING";
            case STATE_OPEN:
                return "STATE_OPEN";
            case STATE_END:
                return "STATE_END";
            case STATE_SETTING:
                return "STATE_SETTING";
            case STATE_UNKNOWN:
                return "STATE_UNKNOWN";
        }
        return "NULL";
    }

    private void sendCurrentStatus() {
        Observable.just(1)
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnNext(integer -> {
                    if (mCurrentStatusListener != null) {
                        mCurrentStatusListener.currentStatus(mDryerStatus);
                    }
                })
                .subscribe();
    }

    /**
     * 分析烘干机状态
     *
     * @return 状态
     */
    private int dryerState() {
        if (TextUtils.equals(mShowText, "End")) {
            //结束状态
            return STATE_END;
        }
        if ((lamp_1 == LAMP_GLINT || lamp_2 == LAMP_GLINT)//Dry || Cooling 灯 闪烁
                && lamp_3 == LAMP_BRIGHT//start灯 长亮
                && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)) { //模式 灯 长亮
            if (lamp_1 != LAMP_BRIGHT && lamp_1 != LAMP_CLOSE) {
                return STATE_RUN_COOLING;
            } else {
                return STATE_RUN_DRY;
            }
        }
        if (lamp_1 == LAMP_CLOSE
                && lamp_2 == LAMP_CLOSE
                && (lamp_4 != LAMP_CLOSE || lamp_5 != LAMP_CLOSE || lamp_6 != LAMP_CLOSE || lamp_7 != LAMP_CLOSE)) {
            //空闲状态
            return STATE_FREE;
        }
        if ((lamp_1 == LAMP_BRIGHT || lamp_2 == LAMP_BRIGHT)
                && lamp_3 == LAMP_GLINT
                && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)
                && NumberUtils.isCreatable(mShowText)) {
            //开门状态
            return STATE_OPEN;
        }
        if (lamp_1 == LAMP_CLOSE && lamp_2 == LAMP_CLOSE && lamp_3 == LAMP_CLOSE && lamp_4 == LAMP_CLOSE && lamp_5 == LAMP_CLOSE && lamp_6 == LAMP_CLOSE && lamp_7 == LAMP_CLOSE) {
            return STATE_SETTING;
        }
        //未知状态
        return STATE_UNKNOWN;

    }

    /**
     * 分析烘干机屏幕内容及灯的状态
     *
     * @param buf 字节数组
     * @return 屏幕显示是否变化
     */
    private boolean dryerMsg(@NonNull byte[] buf) {

        showWord.append(mText.get(buf[2])).append(mText.get(buf[3])).append(mText.get(buf[4])).append(mText.get(buf[5]));
        boolean isDiff = !TextUtils.equals(showStr.toString(), showWord.toString());
        showStr.delete(0, showStr.length());
        showStr.append(showWord);
        showWord.delete(0, showWord.length());
        mShowText = showStr.toString();

        lamp_text = lampState(buf[6]);//text
        lamp_1 = lampState(buf[7]);//cooling
        lamp_2 = lampState(buf[8]);//Dry
        lamp_3 = lampState(buf[9]);//start
        lamp_4 = lampState(buf[10]);//high temp
        lamp_5 = lampState(buf[11]);//med temp
        lamp_6 = lampState(buf[12]);//low temp
        lamp_7 = lampState(buf[13]);//noheat

        return isDiff;
    }

    private int lampState(byte b) {
        if (b == 0x08) {
            return LAMP_BRIGHT;//灯 长亮
        } else if (b == 0x00) {
            return LAMP_CLOSE;//灯 不亮
        } else if (b == 0x01 || b == 0x07) {
            return LAMP_NORMAL;//灯 过度状态
        } else {
            return LAMP_GLINT;//灯 闪烁
        }
    }

    @Override
    public void push(int action, int coin) {
        synchronized (DryerSerialPortHelper.class) {
            mKey = action;
            if (coin == 0) {//发送的单独按钮指令
                sendInstruction(mKey);
            } else {
                sendCoinInstructionStart(mKey, coin);
            }

        }
    }

    private void sendCoinInstructionStart(int mKey, int coin) {
        switch (mKey) {
            case DeviceAction.Dryer.ACTION_HIGH:
                sendHighCoinInstructionStart(coin);
                break;
            case DeviceAction.Dryer.ACTION_MED:
                sendMedCoinInstructionStart(coin);
                break;
            case DeviceAction.Dryer.ACTION_LOW:
                sendLowCoinInstructionStart(coin);
                break;
            case DeviceAction.Dryer.ACTION_NOHEAT:
                sendNoHeatCoinInstructionStart(coin);
                break;
        }
    }

    /**
     * High模式一键启动
     *
     * @param coin 投币数量
     */
    private void sendHighCoinInstructionStart(int coin) {
        mHighDisposable = Observable.just(1)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe(integer -> makePush(mKey, coin));
    }

    /**
     * Med模式一键启动
     *
     * @param coin 投币数量
     */
    private void sendMedCoinInstructionStart(int coin) {
        mMedDisposable = Observable.just(1)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe(integer -> makePush(mKey, coin));
    }

    /**
     * Low模式一键启动
     *
     * @param coin 投币数量
     */
    private void sendLowCoinInstructionStart(int coin) {
        mLowDisposable = Observable.just(1)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe(integer -> makePush(mKey, coin));
    }

    /**
     * NoHeat模式一键启动
     *
     * @param coin 投币数量
     */
    private void sendNoHeatCoinInstructionStart(int coin) {
        mNoHeatDisposable = Observable.just(1)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe(integer -> makePush(mKey, coin));
    }

    /**
     * 发送单独指令
     */
    private void sendInstruction(int key) {
        switch (key) {
            case DeviceAction.Dryer.ACTION_KILL:
                mKillDisposable = Observable.just(1)
                        .subscribeOn(Schedulers.io())
                        .onErrorResumeNext(throwable -> {
                            return Observable.empty();
                        })
                        .subscribe(integer -> kill());

                break;
            case DeviceAction.Dryer.ACTION_INIT:
                mInitSetDisposable = Observable.just(1)
                        .subscribeOn(Schedulers.io())
                        .onErrorResumeNext(throwable -> {
                            return Observable.empty();
                        })
                        .subscribe(integer -> initSet());
                break;
            case DeviceAction.Dryer.ACTION_SETTING:
                Log.e(TAG,"DeviceAction.Dryer.ACTION_SETTING");
                if (mSettingDisposable != null && !mSettingDisposable.isDisposed()) {
                    mSettingDisposable.dispose();
                }
                mSettingDisposable = Observable.intervalRange(0,2,0,1200,TimeUnit.MILLISECONDS)
                        .subscribeOn(Schedulers.io())
                        .doOnNext(integer -> {
                            if (integer == 0) {
                                writeOne(key);
                            }
                        })
                        .onErrorResumeNext(throwable -> {
                            return Observable.empty();
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete(() -> {
                            Log.e(TAG,"mShowText:"+mShowText+"======================mOnSendInstructionListener"+mOnSendInstructionListener);
                            if ("0".equals(mShowText) && state == STATE_SETTING) {
                                if (mOnSendInstructionListener!= null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(key,mDryerStatus);
                                    Log.e(TAG,"发送回调");
                                }
                            }
                        })
                        .subscribe();
                break;
            default:
                Observable.just(1)
                        .subscribeOn(Schedulers.io())
                        .subscribe(integer -> writeOne(key));
                break;
        }
    }

    private void makePush(int action, int coin) throws IOException, InterruptedException {
        if (state != STATE_FREE) return;
        coin = coin > 9 ? 9 : coin;
        String time = String.valueOf(coin * 10 / 60 * 100 + coin * 10 % 60);
        click = 0;
        int value;
        for (; ; ) {
            for (; ; ) {//投币
                try {
                    value = Integer.parseInt(mShowText);
                } catch (Exception ex) {
                    break;
                }

                if ((value / 100 * 6 + value % 100 / 10) >= coin
                        && (lamp_4 + lamp_5 + lamp_6 + lamp_7) > 3) {
                    break;
                }
                coin();
            }

            for (; ; ) {
                if ((lamp_1 == LAMP_GLINT || lamp_2 == LAMP_GLINT)//灯 闪烁
                        && lamp_3 == LAMP_BRIGHT//灯 长亮
                        && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)) {
                    Log.e(TAG, "makePush: NoHeat开始成功");
                    return;
                } else if (lamp_1 == LAMP_BRIGHT//灯 长亮
                        && lamp_2 == LAMP_BRIGHT
                        && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)
                        && TextUtils.equals(mShowText, time)) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    Thread.sleep(800);
                } else if (TextUtils.equals(mShowText, time)) {
                    writeOne(action);
                }
            }
        }
    }

    private void coin() throws IOException {
        mCoinByte[1] = (byte) (seq & 0xff);
        for (int i = 0; i < 10; ++i) {
            mBufferedOutputStream.write(mCoinByte);
            mBufferedOutputStream.flush();
            SystemClock.sleep(80);
        }
        ++seq;
    }

    private void writeOne(int key) throws IOException, InterruptedException {
//        if (state != STATE_OPEN && ++click > MAX_CLICK) {
//            throw new IOException("click error");
//        }
        byte[] strByte = new byte[]{0x7F, (byte) (seq & 0xff), (byte) 0xF0, (byte) (key & 0xff)};
        for (int i = 0; i < 10; ++i) {
            Log.e(TAG,MyFunc.ByteArrToHex(strByte)+"========"+key);
            mBufferedOutputStream.write(strByte);
            mBufferedOutputStream.flush();
            Thread.sleep(80);
        }
        ++seq;
    }


    private void writeOne1(int key) {
//        if (state != STATE_OPEN && ++click > MAX_CLICK) {
//            throw new IOException("click error");
//        }
        mKeyByte[1] = (byte) (seq & 0xff);
        mKeyByte[3] = (byte) (key & 0xff);
        try {
            mBufferedOutputStream.write(mKeyByte);
            mBufferedOutputStream.flush();
            Thread.sleep(120);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 执行kill操作
     */
    private void kill() {
        Log.e(TAG, "kill: 开始");
        click = 0;
        dispose(mKey);
        mKey = DeviceAction.Dryer.ACTION_HIGH;
        ++seq;
        do {
            writeOne1(mKey);
        } while (!NumberUtils.isCreatable(mShowText));

        mKey = DeviceAction.Dryer.ACTION_SETTING;
        ++seq;
        do {
            writeOne1(DeviceAction.Dryer.ACTION_SETTING);//执行设置按钮
        } while (!"0".equals(mShowText));
        Log.e(TAG, "kill step1 success");

        mKey = DeviceAction.Dryer.ACTION_MED;
        for (int i = 1; i < 4; ++i) {                    //执行Med按钮3遍
            ++seq;
            for (; ; ) {
                writeOne1(mKey);
                Log.e(TAG, "kill: /执行Med按钮3遍");
                if ((i + "").equals(mShowText)) {
                    Log.e(TAG, "kill step2 success" + (i));
                    break;
                }
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;          //执行start按钮
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("LgC1").equals(mShowText)) {
                Log.e(TAG, "kill step3 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;            //执行到h1LL
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("h1LL").equals(mShowText)) {
                Log.e(TAG, "kill step4 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;          //执行start按钮
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("0").equals(mShowText)) {
                Log.e(TAG, "kill step5 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        for (int i = 1; i < 18; ++i) {                    //执行Med按钮17遍
            ++seq;
            for (; ; ) {
                if ((i + "").equals(mShowText)) {
                    Log.e(TAG, "kill step6 success" + (i));
                    break;
                }
                writeOne1(mKey);
                Log.e(TAG, "kill: 执行Med按钮17遍");
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;          //执行start按钮
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("End").equals(mShowText)) {
                Log.e(TAG, "kill: 成功");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorResumeNext(throwable -> {
                            return Observable.empty();
                        })
                        .doOnNext(integer1 -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.Dryer.ACTION_KILL, null);
                            }
                            if (mKillDisposable != null && !mKillDisposable.isDisposed()) {
                                mKillDisposable.dispose();
                            }
                        })
                        .subscribe();
                break;
            }
        }
    }

    /**
     * 初始化烘干机
     */
    private void initSet() {
        Log.e(TAG, "initSet: 开始");
        click = -500;
        dispose(mKey);
        mode();
        price();
        timeUnit();
        timeDefault();
        coin1();
        coin2();
        Log.e(TAG, "initSet: 完成");
        Observable.just(1)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(integer -> {
                    Log.e(TAG, "accept: " + Thread.currentThread().getName());
                    if (mOnSendInstructionListener != null) {
                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.Dryer.ACTION_INIT, null);
                    }
                    if (mInitSetDisposable != null && !mInitSetDisposable.isDisposed()) {
                        mInitSetDisposable.dispose();
                    }
                })
                .subscribe();

    }

    /**
     * 设置模式PA4(pay模式)
     */
    private void mode() {
        Log.e(TAG, "mode: 开始设置");
        mKey = DeviceAction.Dryer.ACTION_HIGH;
        ++seq;
        for (; ; ) {
            if (!NumberUtils.isCreatable(mShowText)) {
                writeOne1(mKey);
            } else {
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_SETTING;        //执行设置按钮
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if ("0".equals(mShowText)) {
                Log.e(TAG, "mode step1 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        for (int i = 1; i < 4; ++i) {                    //执行Med按钮3遍
            ++seq;
            for (; ; ) {
                writeOne1(mKey);
                if ((String.valueOf(i)).equals(mShowText)) {
                    Log.e(TAG, "mode step2 success" + (i));
                    break;
                }
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;          //执行start按钮
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("LgC1").equals(mShowText)) {
                Log.e(TAG, "mode step3 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_HIGH;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("tE5t").equals(mShowText)) {
                Log.e(TAG, "mode step4 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("5tUP").equals(mShowText)) {
                Log.e(TAG, "mode step5 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("r9Pr").equals(mShowText)) {
                Log.e(TAG, "mode step6 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("PdtP").equals(mShowText)) {
                Log.e(TAG, "mode step7 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("PA4").equals(mShowText)) {
                mKey = DeviceAction.Dryer.ACTION_START;
                ++seq;
                for (; ; ) {
                    writeOne1(mKey);
                    if (NumberUtils.isCreatable(mShowText)
                            && lamp_1 == LAMP_CLOSE
                            && lamp_2 == LAMP_CLOSE
                            && lamp_3 == LAMP_CLOSE) {
                        Log.e(TAG, "mode: 设置完成");
                        return;
                    }
                }
            } else if ("FrEE".equals(mShowText)) {
                mKey = DeviceAction.Dryer.ACTION_MED;
                ++seq;
                for (; ; ) {
                    writeOne1(mKey);
                    if (("PA4").equals(mShowText)) {
                        mKey = DeviceAction.Dryer.ACTION_START;
                        ++seq;
                        for (; ; ) {
                            writeOne1(mKey);
                            if (NumberUtils.isCreatable(mShowText)
                                    && lamp_1 == LAMP_CLOSE
                                    && lamp_2 == LAMP_CLOSE
                                    && lamp_3 == LAMP_CLOSE) {
                                Log.e(TAG, "mode: 设置完成");
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置价格
     */
    private void price() {
        Log.e(TAG, "price: 开始设置");
        mKey = DeviceAction.Dryer.ACTION_SETTING;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if ("0".equals(mShowText)) {
                Log.e(TAG, "price step1 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        for (int i = 1; i < 4; ++i) {                    //执行Med按钮3遍
            ++seq;
            for (; ; ) {
                writeOne1(mKey);
                if ((String.valueOf(i)).equals(mShowText)) {
                    Log.e(TAG, "price step2 success" + (i));
                    break;
                }
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;          //执行start按钮
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("LgC1").equals(mShowText)) {
                Log.e(TAG, "price step3 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_HIGH;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("tE5t").equals(mShowText)) {
                Log.e(TAG, "price step4 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("5tUP").equals(mShowText)) {
                Log.e(TAG, "price step5 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (("r9Pr").equals(mShowText)) {
                Log.e(TAG, "price step6 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (NumberUtils.isCreatable(mShowText)) {
                Log.e(TAG, "price step7 success");
                break;
            }
        }

        for (Integer v; ; ) {
            try {
                v = Integer.parseInt(mShowText);
            } catch (Exception ex) {
                break;
            }
            if (mShowText.length() == 3) {//区分两个版本的烘干机
                if (v > 100) {
                    mKey = DeviceAction.Dryer.ACTION_LOW;
                    ++seq;
                    for (int i = 0; i < 12; i++) {
                        writeOne1(mKey);
                    }
                } else if (v < 100) {
                    mKey = DeviceAction.Dryer.ACTION_MED;
                    ++seq;
                    for (int i = 0; i < 12; i++) {
                        writeOne1(mKey);
                    }
                } else {
                    mKey = DeviceAction.Dryer.ACTION_START;
                    ++seq;
                    for (int i = 0; i < 12; i++) {
                        writeOne1(mKey);
                    }
                    Log.e(TAG, "price: 设置完成");
                    return;
                }
            } else {
                if (v > 1) {
                    mKey = DeviceAction.Dryer.ACTION_LOW;
                    ++seq;
                    for (int i = 0; i < 12; i++) {
                        writeOne1(mKey);
                    }
                } else if (v < 1) {
                    mKey = DeviceAction.Dryer.ACTION_MED;
                    ++seq;
                    for (int i = 0; i < 12; i++) {
                        writeOne1(mKey);
                    }
                } else {
                    mKey = DeviceAction.Dryer.ACTION_START;
                    ++seq;
                    for (int i = 0; i < 12; i++) {
                        writeOne1(mKey);
                    }
                    Log.e(TAG, "price: 设置完成");
                    return;
                }
            }
        }
    }

    /**
     * 设置timeUnit
     */
    private void timeUnit() {
        Log.e(TAG, "timeUnit: 开始设置");

        for (; ; ) {
            if (!TextUtils.equals(mShowText, "5tUP")) {
                mKey = DeviceAction.Dryer.ACTION_HIGH;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                Log.e(TAG, "timeUnit step1 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "r9Pr")) {
                Log.e(TAG, "timeUnit step2 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "PdtP")) {
                Log.e(TAG, "timeUnit step3 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "rEFr")) {
                Log.e(TAG, "timeUnit step4 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "bEEP")) {
                Log.e(TAG, "timeUnit step5 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "toPt")) {
                Log.e(TAG, "timeUnit step6 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (NumberUtils.isCreatable(mShowText)) {
                Log.e(TAG, "timeUnit step7 success");
                break;
            }
        }

        for (Integer value; ; ) {
            try {
                value = Integer.parseInt(mShowText);
            } catch (Exception ex) {
                break;
            }
            if (value < 10) {
                mKey = DeviceAction.Dryer.ACTION_MED;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else if (value > 10) {
                mKey = DeviceAction.Dryer.ACTION_LOW;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                mKey = DeviceAction.Dryer.ACTION_START;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
                break;
            }
        }

        if (TextUtils.equals(mShowText, "bEEP")) {
            Log.e(TAG, "timeUnit: 设置完成");
        }
    }

    /**
     * 设置timeDefault
     */
    private void timeDefault() {
        Log.e(TAG, "timeDefault: 设置开始");
        for (; ; ) {
            if (!TextUtils.equals(mShowText, "5tUP")) {
                mKey = DeviceAction.Dryer.ACTION_HIGH;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                Log.e(TAG, "timeDefault step1 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "r9Pr")) {
                Log.e(TAG, "timeDefault step2 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "PdtP")) {
                Log.e(TAG, "timeDefault step3 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "rEFr")) {
                Log.e(TAG, "timeDefault step4 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "bEEP")) {
                Log.e(TAG, "timeDefault step5 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "toPt")) {
                Log.e(TAG, "timeDefault step6 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "P1Po")) {
                Log.e(TAG, "timeDefault step7 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "Co2")) {
                Log.e(TAG, "timeDefault step8 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "Co1")) {
                Log.e(TAG, "timeDefault step9 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "t55d")) {
                Log.e(TAG, "timeDefault step10 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "rt")) {
                Log.e(TAG, "timeDefault step11 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "nrPC")) {
                Log.e(TAG, "timeDefault step12 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "CCCC")) {
                Log.e(TAG, "timeDefault step13 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "5PdC")) {
                Log.e(TAG, "timeDefault step14 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "rPdC")) {
                Log.e(TAG, "timeDefault step15 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (NumberUtils.isCreatable(mShowText)) {
                Log.e(TAG, "timeUnit step16 success");
                break;
            }
        }

        for (Integer value; ; ) {
            try {
                value = Integer.parseInt(mShowText);
            } catch (Exception ex) {
                break;
            }
            if (value < 10) {
                mKey = DeviceAction.Dryer.ACTION_MED;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else if (value > 10) {
                mKey = DeviceAction.Dryer.ACTION_LOW;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                mKey = DeviceAction.Dryer.ACTION_START;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
                break;
            }
        }

        if (TextUtils.equals(mShowText, "5PdC")) {
            Log.e(TAG, "timeDefault: 设置完成");
        }
    }

    /**
     * 设置Co1
     */
    private void coin1() {
        Log.e(TAG, "coin1: 设置开始");
        for (; ; ) {
            if (!TextUtils.equals(mShowText, "5tUP")) {
                mKey = DeviceAction.Dryer.ACTION_HIGH;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                Log.e(TAG, "coin1 step1 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "r9Pr")) {
                Log.e(TAG, "coin1 step2 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "PdtP")) {
                Log.e(TAG, "coin1 step3 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "rEFr")) {
                Log.e(TAG, "coin1 step4 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "bEEP")) {
                Log.e(TAG, "coin1 step5 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "toPt")) {
                Log.e(TAG, "coin1 step6 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "P1Po")) {
                Log.e(TAG, "coin1 step7 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "Co2")) {
                Log.e(TAG, "coin1 step8 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "Co1")) {
                Log.e(TAG, "coin1 step9 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (NumberUtils.isCreatable(mShowText)) {
                Log.e(TAG, "coin1 step10 success");
                break;
            }
        }

        for (Integer value; ; ) {
            try {
                value = Integer.parseInt(mShowText);
                if (value < 10) {
                    value *= 100;
                }
            } catch (Exception ex) {
                break;
            }
            if (value < 100) {
                mKey = DeviceAction.Dryer.ACTION_MED;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else if (value > 100) {
                mKey = DeviceAction.Dryer.ACTION_LOW;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                mKey = DeviceAction.Dryer.ACTION_START;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
                break;
            }
        }

        if (TextUtils.equals(showStr.toString(), "Co2")) {
            Log.e(TAG, "coin1: 设置完成");
        }
    }

    /**
     * 设置Co2
     */
    private void coin2() {
        Log.e(TAG, "coin2: 设置开始");
        for (; ; ) {
            if (lamp_1 == LAMP_CLOSE
                    && lamp_2 == LAMP_CLOSE
                    && lamp_3 == LAMP_CLOSE
                    && (TextUtils.equals(mShowText, "1") || TextUtils.equals(mShowText, "100"))) {
                Log.e(TAG, "coin2: 设置完成");
                return;
            } else if (!TextUtils.equals(mShowText, "5tUP")) {
                mKey = DeviceAction.Dryer.ACTION_HIGH;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                Log.e(TAG, "coin2 step1 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "r9Pr")) {
                Log.e(TAG, "coin2 step2 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "PdtP")) {
                Log.e(TAG, "coin2 step3 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "rEFr")) {
                Log.e(TAG, "coin2 step4 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "bEEP")) {
                Log.e(TAG, "coin2 step5 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "toPt")) {
                Log.e(TAG, "coin2 step6 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "P1Po")) {
                Log.e(TAG, "coin2 step7 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_MED;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (TextUtils.equals(mShowText, "Co2")) {
                Log.e(TAG, "coin2 step8 success");
                break;
            }
        }

        mKey = DeviceAction.Dryer.ACTION_START;
        ++seq;
        for (; ; ) {
            writeOne1(mKey);
            if (NumberUtils.isCreatable(mShowText)) {
                Log.e(TAG, "coin2 step9 success");
                break;
            }
        }

        for (; ; ) {
            int value;
            try {
                value = Integer.parseInt(mShowText);
                if (value < 10) {
                    value *= 100;
                }
            } catch (Exception ex) {
                break;
            }
            if (value < 100) {
                mKey = DeviceAction.Dryer.ACTION_MED;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else if (value > 100) {
                mKey = DeviceAction.Dryer.ACTION_LOW;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
            } else {
                mKey = DeviceAction.Dryer.ACTION_START;
                ++seq;
                for (int i = 0; i < 12; i++) {
                    writeOne1(mKey);
                }
                break;
            }
        }

        if (TextUtils.equals(mShowText, "P1Po")) {
            mKey = DeviceAction.Dryer.ACTION_HIGH;
            ++seq;
            for (; ; ) {
                writeOne1(mKey);
                if (TextUtils.equals(mShowText, "5tUP")) {
                    mKey = DeviceAction.Dryer.ACTION_HIGH;
                    ++seq;
                    do {
                        writeOne1(mKey);
                    } while (mDryerStatus.getStatus() != STATE_FREE || !mShowText.equals("100"));
                    break;
                }
            }
        }
        Log.e(TAG, "coin2: 设置完成");
    }

    @Override
    public void open() throws IOException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, "8", "1", "N");
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();

        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
        mReadThread = new ReadThread();
        mReadThread.start();
        readThreadStartTime = System.currentTimeMillis();
        _isOpen = true;
    }

    @Override
    public void setOnSendInstructionListener(@NonNull OnSendInstructionListener onSendInstructionListener) {
        mOnSendInstructionListener = onSendInstructionListener;
    }

    @Override
    public void setOnCurrentStatusListener(@NonNull CurrentStatusListener currentStatusListener) {
        mCurrentStatusListener = currentStatusListener;
    }

    @Override
    public void setOnSerialPortOnlineListener(@NonNull SerialPortOnlineListener onSerialPortOnlineListener) {
        mSerialPortOnlineListener = onSerialPortOnlineListener;
    }

    public boolean isOpen() {
        return _isOpen;
    }

    @Override
    public void close() throws IOException {
        _isOpen = false;
        isOnline = false;
        hasOnline = false;
        readThreadStartTime = 0;
        dispose(mKey);
        if (mReadThread != null)
            mReadThread.interrupt();
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        if (mInputStream != null) {
            mInputStream.close();
            mInputStream = null;
        }
        if (mOutputStream != null) {
            mOutputStream.close();
            mOutputStream = null;
        }
        if (mBufferedInputStream != null) {
            mBufferedInputStream.close();
            mBufferedInputStream = null;
        }
        if (mBufferedOutputStream != null) {
            mBufferedOutputStream.close();
            mBufferedOutputStream = null;
        }
    }

    private void dispose(int mKey) {
        switch (mKey) {
            case DeviceAction.Dryer.ACTION_KILL:
                if (mKillDisposable != null && !mKillDisposable.isDisposed()) {
                    mKillDisposable.dispose();
                }
                break;
            case DeviceAction.Dryer.ACTION_INIT:
                if (mInitSetDisposable != null && !mInitSetDisposable.isDisposed()) {
                    mInitSetDisposable.dispose();
                }
                break;
            case DeviceAction.Dryer.ACTION_HIGH:
                if (mHighDisposable != null && !mHighDisposable.isDisposed()) {
                    mHighDisposable.dispose();
                }
                break;
            case DeviceAction.Dryer.ACTION_MED:
                if (mMedDisposable != null && !mMedDisposable.isDisposed()) {
                    mMedDisposable.dispose();
                }
                break;
            case DeviceAction.Dryer.ACTION_LOW:
                if (mLowDisposable != null && !mLowDisposable.isDisposed()) {
                    mLowDisposable.dispose();
                }
                break;
            case DeviceAction.Dryer.ACTION_NOHEAT:
                if (mNoHeatDisposable != null && !mNoHeatDisposable.isDisposed()) {
                    mNoHeatDisposable.dispose();
                }
                break;
            case DeviceAction.Dryer.ACTION_SETTING:
                if (mSettingDisposable != null && !mSettingDisposable.isDisposed()) {
                    mSettingDisposable.dispose();
                }
                break;
        }
    }
}
