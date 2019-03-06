package com.xiaolan.serialporttest.wash.xjl;

import android.util.Log;

import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
import com.xiaolan.serialporttest.mylib.utils.CRC16;
import com.xiaolan.serialporttest.mylib.utils.MyFunc;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class XjlSerialPortHelper {
    private static final String TAG = "XjlSerialPortHelper";

    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private String sPort = "/dev/ttyS3";//默认串口号
    private int iBaudRate = 9600;//波特率
    private String mDataBits = "8";//数据位
    private String mStopBits = "1";//停止位
    private String mParityBits = "N";//校验位
    private boolean _isOpen = false;
    private BufferedInputStream mBufferedInputStream;
    private BufferedOutputStream mBufferedOutputStream;
    private CRC16 mCrc16;
    private static final int SHOW_LENGTH = 30;
    private byte[] mPreMsg = new byte[SHOW_LENGTH];
    private int seq;
    private int mKey;
    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortOnlineListener mSerialPortOnlineListener;
    private CurrentStatusListener mCurrentStatusListener;
    private boolean isOnline = false;
    private boolean hasOnline = false;
    private long mPreOnlineTime = 0;
    private long readThreadStartTime = 0;
    private XjlWashStatus mXjlWashStatus;
    private WashStatusEvent mWashStatusEvent;
    private long mRecCount = 0;//接收到的报文次数
    private int mSendCount = 0;//发送的某个报文次数
    private static final int KEY_RESTORATION = 0;//复位
    private static final int KEY_START = 1;//开始
    private static final int KEY_HOT = 2;//热水
    private static final int KEY_WARM = 3;//温水
    private static final int KEY_COLD = 4;//冷水
    private static final int KEY_DELICATES = 5;//精致衣物
    private static final int KEY_SUPER = 7;//加强洗
    private static final int KEY_SETTING = 11;//setting
    private static final int KEY_KILL = 10;//kill
    private boolean isKilling = false;
    private Disposable mHotDisposable;
    private Disposable mStartDisposable;
    private Disposable mWarmDisposable;
    private Disposable mColdDisposable;
    private Disposable mDelicatesDisposable;
    private Disposable mSuperDisposable;
    private Disposable mSettingDisposable;
    private Disposable mKill;
    private int mCount = 0;
    private int mPreIsSupper;

    public XjlSerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

    public XjlSerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public void xjlOpen() throws SecurityException, IOException, InvalidParameterException {
        File device = new File(sPort);
        //检查访问权限，如果没有读写权限，进行文件操作，修改文件访问权限
        if (!device.canRead() || !device.canWrite()) {
            try {
                //通过挂在到linux的方式，修改文件的操作权限
                Process su = Runtime.getRuntime().exec("/system/bin/su");
                //一般的都是/system/bin/su路径，有的也是/system/xbin/su
                String cmd = "chmod 777 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());

                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, mDataBits, mStopBits, mParityBits);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();

        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
        mCrc16 = new CRC16();
        mXjlWashStatus = new XjlWashStatus();
        mReadThread = new ReadThread();
        mReadThread.start();
        readThreadStartTime = System.currentTimeMillis();
        _isOpen = true;
    }

    public void close() throws IOException {
        _isOpen = false;
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
        isOnline = false;
        hasOnline = false;
        readThreadStartTime = 0;
    }

    /**
     * 开始指令
     */
    public synchronized void sendStartOrStop() {
        mKey = KEY_START;
        long recCount = mRecCount;
        mStartDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mRecCount > recCount) {//收到新报文
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                        }
                        dispose(mKey);
                    } else {
                        if (mSendCount <= 3) {
                            mSendCount++;
                            sendStartOrStop();
                        } else {//连续发3轮没收到新报文则判定指令发送失败
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(mKey, "send startOrStop error");
                            }
                            dispose(mKey);
                        }
                    }
                })
                .subscribe();
    }

    /**
     * 第一种模式（白色特脏衣物）
     */
    public synchronized void sendHot() {
        mKey = KEY_HOT;
        long recCount = mRecCount;
        mHotDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mWashStatusEvent != null) {
                        if (!mWashStatusEvent.isSetting()) {//不是设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x02) {//未开始洗衣 && 洗衣模式为热水
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendHot();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send hot error");
                                    }
                                    dispose(mKey);
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                    dispose(mKey);
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendHot();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send hot error");
                                        }
                                        dispose(mKey);
                                    }
                                }
                            } else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            }
                        }
                    }
                })
                .subscribe();
    }

    /**
     * 第二种模式(普通混合衣物)
     */
    public synchronized void sendWarm() {
        mKey = KEY_WARM;
        long recCount = mRecCount;
        mWarmDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mWashStatusEvent != null) {//不是设置模式
                        if (!mWashStatusEvent.isSetting()) {
                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x03) {//未开始洗衣 && 洗衣模式为热水
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendWarm();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send warm error");
                                    }
                                    dispose(mKey);
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                    dispose(mKey);
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendWarm();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send warm error");
                                        }
                                        dispose(mKey);
                                    }
                                }
                            } else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            }
                        }
                    }
                }).subscribe();
    }

    /**
     * 第三种模式(混纺尼龙衣物)
     */
    public synchronized void sendCold() {
        mKey = KEY_COLD;
        long recCount = mRecCount;
        mColdDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mWashStatusEvent != null) {//不是设置模式，
                        if (!mWashStatusEvent.isSetting()) {
                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x04) {//未开始洗衣 && 洗衣模式为热水
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendCold();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send cold error");
                                    }
                                    dispose(mKey);
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                    dispose(mKey);
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendCold();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send cold error");
                                        }
                                        dispose(mKey);
                                    }
                                }
                            } else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            }
                        }
                    }
                })
                .doOnError(throwable -> {
                    throw new IOException();
                })
                .subscribe();
    }

    /**
     * 第四种模式(精致衣物)
     */
    public synchronized void sendDelicates() {
        mKey = KEY_DELICATES;
        long recCount = mRecCount;
        mDelicatesDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mWashStatusEvent != null) {//不是设置模式，
                        if (!mWashStatusEvent.isSetting()) {
                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x05) {//未开始洗衣 && 洗衣模式为热水
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendDelicates();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send delicates error");
                                    }
                                    dispose(mKey);
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                    dispose(mKey);
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendDelicates();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send delicates error");
                                        }
                                        dispose(mKey);
                                    }
                                }
                            } else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                                dispose(mKey);
                            }
                        }
                    }
                })
                .subscribe();
    }

    /**
     * 加强洗
     */
    public synchronized void sendSuper() {
        mKey = KEY_SUPER;
        mSuperDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mWashStatusEvent != null) {
                        if (mCount == 0) {
                            mPreIsSupper = mWashStatusEvent.getLightSupper();
                        } else {
                            if (mPreIsSupper == 1) {
                                if (mWashStatusEvent.getLightSupper() == 0) {
                                    mPreIsSupper = mWashStatusEvent.getLightSupper();
                                    mCount = 0;
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                    dispose(mKey);
                                }
                            } else if (mPreIsSupper == 0) {
                                if (mWashStatusEvent.getLightSupper() == 1) {
                                    mPreIsSupper = mWashStatusEvent.getLightSupper();
                                    mCount = 0;
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                    dispose(mKey);
                                }
                            }
                        }
                        mCount++;
                    }
                })
                .subscribe();
    }

    /**
     * 进入设置模式
     */
    public synchronized void sendSetting() {
        mKey = KEY_SETTING;
        mSettingDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
                        Log.e(TAG, "setting mode success");
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                        }
                        dispose(mKey);
                    } else {
                        Log.e(TAG, "setting mode error");
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionFail(mKey, "setting mode error");
                        }
                        dispose(mKey);
                    }
                })
                .subscribe();
    }

    /**
     * 强制kill
     */
    public synchronized void sendKill() {
        //判断是处于洗衣状态才可以使用kill
        if (mWashStatusEvent != null && mWashStatusEvent.getIsWashing() == 0x40) {
            Observable.create(e -> {
                kill();
                e.onComplete();
            }).subscribeOn(Schedulers.io())
                    .subscribe();
        }
    }

    private synchronized void kill() throws IOException {
        isKilling = true;
        mKey = KEY_SETTING;
        sendData(mKey);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step1 success");
        } else {
            Log.e(TAG, "kill step1 error");
        }
        mKey = KEY_WARM;
        for (int i = 0; i < 3; i++) {
            sendData(mKey);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step2->" + i + "success");
            } else {
                Log.e(TAG, "kill step2->" + i + "error");
            }
        }
        mKey = KEY_START;
        sendData(mKey);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("LgC1").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step3 success");
        } else {
            Log.e(TAG, "kill step3 error");
        }
        mKey = KEY_WARM;
        for (int i = 0; i < 4; i++) {
            sendData(mKey);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting()) {
                switch (i) {
                    case 0:
                        if (("tcL").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                        } else {
                            Log.e(TAG, "kill step4->" + i + "error");
                        }
                        break;
                    case 1:
                        if (("PA55").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                        } else {
                            Log.e(TAG, "kill step4->" + i + "error");
                        }
                        break;
                    case 2:
                        if (("dUCt").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                        } else {
                            Log.e(TAG, "kill step4->" + i + "error");
                        }
                        break;
                    case 3:
                        if (("h1LL").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                        } else {
                            Log.e(TAG, "kill step4->" + i + "error");
                        }
                        break;
                }
            } else {
                Log.e(TAG, "kill step4->" + i + "error");
            }
        }
        mKey = KEY_START;
        sendData(mKey);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step5 success");
        } else {
            Log.e(TAG, "kill step5 error");
        }
        mKey = KEY_WARM;
        for (int i = 0; i < 17; i++) {
            sendData(mKey);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step6 success");
            } else {
                Log.e(TAG, "kill step6 error");
            }
        }
        mKey = KEY_START;
        sendData(mKey);
        mKill = Observable.intervalRange(0, 240, 0, 1, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(aLong -> {
                    Log.e(TAG, "doOnNext--->" + aLong);
                    if (mWashStatusEvent != null) {
                        if (!mWashStatusEvent.isSetting() && mWashStatusEvent.getText().equals("PU5H")) {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionSuccess(KEY_KILL, mWashStatusEvent);
                            }
                            Log.e(TAG, "kill success");
                            if (mKill != null && !mKill.isDisposed()) {
                                mKill.dispose();
                            }
                        } else {
                            if (aLong == 239) {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionFail(KEY_KILL, "kill error");
                                }
                                Log.e(TAG, "kill error");
                                if (mKill != null && !mKill.isDisposed()) {
                                    mKill.dispose();
                                }
                            }
                        }
                    }
                })
                .doOnComplete(() -> {
                    isKilling = false;
                    mRecCount = 0;
                })
                .subscribe();
    }

    private void dispose(int key) {
        switch (key) {
            case KEY_START:
                if (mStartDisposable != null && !mStartDisposable.isDisposed()) {
                    mStartDisposable.dispose();
                }
                break;
            case KEY_HOT:
                if (mHotDisposable != null && !mHotDisposable.isDisposed()) {
                    mHotDisposable.dispose();
                }
                break;
            case KEY_WARM:
                if (mWarmDisposable != null && !mWarmDisposable.isDisposed()) {
                    mWarmDisposable.dispose();
                }
                break;
            case KEY_COLD:
                if (mColdDisposable != null && !mColdDisposable.isDisposed()) {
                    mColdDisposable.dispose();
                }
                break;
            case KEY_DELICATES:
                if (mDelicatesDisposable != null && !mDelicatesDisposable.isDisposed()) {
                    mDelicatesDisposable.dispose();
                }
                break;
            case KEY_SUPER:
                if (mSuperDisposable != null && !mSuperDisposable.isDisposed()) {
                    mSuperDisposable.dispose();
                }
                break;
            case KEY_SETTING:
                if (mSettingDisposable != null && !mSettingDisposable.isDisposed()) {
                    mSettingDisposable.dispose();
                }
                break;
        }
    }

    private class ReadThread extends Thread {
        private static final int DATA_LENGTH = 64;

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
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        len = mBufferedInputStream.read(buffer);
                        if (len != -1) {
//                            Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
                            if (len >= 30) {
                                //读巨人洗衣机上报报文
                                xjlWashRead(buffer, len);
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

    /**
     * 巨人加洗衣机
     */
    private void xjlWashRead(byte[] buffer, int len) {
        //查找数组中是否有0x02
        int off02 = ArrayUtils.indexOf(buffer, (byte) 0x02);
        int size = 30;
        if (off02 < 0) {//没有0x02
//                Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, len)));
        } else if (off02 + size > len) {
//                Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, off02)));
        } else if (buffer[off02 + size - 1] != 0x03) {//有0x02，但末尾不是0x03
//                Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, off02 + 1)));
        } else {
            if (buffer.length > off02 + 2) {
                if (buffer[off02 + 1] == 0x06) {//找到开头02 06的数据
                    // crc校验
                    short crc16_a = mCrc16.getCrc(buffer, off02, size - 3);
                    short crc16_msg = (short) (buffer[off02 + size - 3] << 8 | (buffer[off02 + size - 2] & 0xff));
                    if (crc16_a == crc16_msg) {
                        byte[] subarray = ArrayUtils.subarray(buffer, off02, off02 + size);
                        //判断串口是否掉线
                        if (!isOnline) {
                            isOnline = true;
                            hasOnline = true;
                            mPreOnlineTime = System.currentTimeMillis();
                            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                    .doOnComplete(() -> {
                                        if (mSerialPortOnlineListener != null) {
                                            mSerialPortOnlineListener.onSerialPortOnline(subarray);
                                            Log.e(TAG, "串口上线");
                                        }
                                    }).subscribe();
                        } else {
                            mPreOnlineTime = System.currentTimeMillis();
                        }

                        if (!Objects.deepEquals(mPreMsg, subarray)) {
                            Log.e(TAG, "接收：" + MyFunc.ByteArrToHex(subarray));
                            mPreMsg = subarray;
                            //根据上报的报文，分析设备状态
                            mWashStatusEvent = mXjlWashStatus.analyseStatus(subarray);
                            mRecCount++;
                            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                    .doOnComplete(() -> {
                                        if (mCurrentStatusListener != null) {
                                            mCurrentStatusListener.currentStatus(mWashStatusEvent);
                                        }
                                    })
                                    .subscribe();
                        }
                    } else {
                        //Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, off02 + 1)));
                    }
                }
            }
        }
    }

    private void sendData(int key) throws IOException {
        byte[] msg = {0x02, 0x06, (byte) (key & 0xff), (byte) (seq & 0xff), (byte) 0x80, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        short crc16_a = mCrc16.getCrc(msg, 0, msg.length - 3);
        msg[msg.length - 2] = (byte) (crc16_a >> 8);
        msg[msg.length - 3] = (byte) (crc16_a & 0xff);
        for (int i = 0; i < 12; ++i) {
            mBufferedOutputStream.write(msg);
            mBufferedOutputStream.flush();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        ++seq;
    }

    public boolean isOpen() {
        return _isOpen;
    }

    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mOnSendInstructionListener = onSendInstructionListener;
    }

    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mCurrentStatusListener = currentStatusListener;
    }

    public void setOnSerialPortOnlineListener(SerialPortOnlineListener serialPortOnlineListener) {
        mSerialPortOnlineListener = serialPortOnlineListener;
    }
}
