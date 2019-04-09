package com.xiaolan.serialporttest.wash.jurenplus;

import android.os.SystemClock;
import android.util.Log;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceWorkType;
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
import java.util.concurrent.atomic.AtomicBoolean;

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 巨人Plus串口操作工具类
 */

public class JuRenPlusSerialPortHelper {
    private static final String TAG = "JuRenProSerialPort";
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
    private boolean isKilling = false;//强制停止
    private static final int INSTRUCTION_MODE = 1;//指令类型1：巨人+的指令   0：巨人Pro的指令
    private int mCount = 0;
    private int mPreIsSupper;
    private JuRenPlusWashStatus mJuRenPlusWashStatus;
    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortOnlineListener mSerialPortOnlineListener;
    private CurrentStatusListener mCurrentStatusListener;
    private WashStatusEvent mWashStatusEvent;
    private long mRecCount = 0;//接收到的报文次数
    //    private int mSendCount = 0;//发送的某个报文次数
    private Disposable mKill;
    private Disposable mHotDisposable;
    private Disposable mWarmDisposable;
    private Disposable mColdDisposable;
    private Disposable mDelicatesDisposable;
    private Disposable mSuperDisposable;
    private Disposable mStartDisposable;
    private Disposable mSettingDisposable;
    private Disposable mResetDisposable;
    private boolean isOnline = false;
    private boolean hasOnline = false;
    private long mPreOnlineTime = 0;
    private long readThreadStartTime = 0;
    private long mPeriod = 100;//发送单条指令的时间间隔，单位ms
    private byte[] msg;
    private AtomicBoolean isSuccess;


    public JuRenPlusSerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

    public JuRenPlusSerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
        isSuccess = new AtomicBoolean(false);
        msg = new byte[]{0x02, 0x06, 0x00, 0x00, (byte) 0x80, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
    }

    public void juRenProOpen() throws SecurityException, IOException, InvalidParameterException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, mDataBits, mStopBits, mParityBits);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();

        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
        mCrc16 = new CRC16();
        mJuRenPlusWashStatus = new JuRenPlusWashStatus();
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
        mCurrentStatusListener = null;
        mOnSendInstructionListener = null;
        mSerialPortOnlineListener = null;
        isOnline = false;
        hasOnline = false;
        readThreadStartTime = 0;
        dispose(mKey);
    }

    private class ReadThread extends Thread {
        private final int DATA_LENGTH = 64;

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
                                //读巨人+洗衣机上报报文
                                JuRenPlusWashRead(buffer, len);
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
    private void JuRenPlusWashRead(byte[] buffer, int len) {
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
                            mWashStatusEvent = mJuRenPlusWashStatus.analyseStatus(subarray);
                            mRecCount++;
                            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(integer -> {
                                        if (mCurrentStatusListener != null) {
                                            mCurrentStatusListener.currentStatus(mWashStatusEvent);
                                        }
                                    });
                        }
                    }
                }
//                else if (buffer[off02 + 1] == 0x15) {//需要执行复位
//                    sendReset();
//                }
            }
        }
    }

    /*
     * 发送热水指令
     */
    public void sendHot() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_HOT;
        ++seq;
        long recCount = mRecCount;
        isSuccess.set(false);
        mHotDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (!isSuccess.get()) {
                        if (mWashStatusEvent != null) {//不是设置模式
                            if (!mWashStatusEvent.isSetting()) {
                                if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x02) {//未开始洗衣 && 洗衣模式为热水
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_HOT, mWashStatusEvent);
                                    }
                                } else {
                                    //TODO
                                }
                            } else {//设置模式
                                if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                    if (mRecCount > recCount) {//收到新报文
                                        isSuccess.set(true);
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_HOT, mWashStatusEvent);
                                        }
                                    } else {
                                        //todo
                                    }
                                } else {
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_HOT, mWashStatusEvent);
                                    }
                                }
                            }
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /*
     * 发送温水指令
     */
    public void sendWarm() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        ++seq;
        long recCount = mRecCount;
        isSuccess.set(false);
        mWarmDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (!isSuccess.get()) {
                        if (mWashStatusEvent != null) {//不是设置模式
                            if (!mWashStatusEvent.isSetting()) {
                                if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x03) {//未开始洗衣 && 洗衣模式为热水
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_WARM, mWashStatusEvent);
                                    }
                                } else {
                                    //TODO
                                }
                            } else {//设置模式
                                if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                    if (mRecCount > recCount) {//收到新报文
                                        isSuccess.set(true);
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_WARM, mWashStatusEvent);
                                        }
                                    } else {
                                        //todo
                                    }
                                } else {
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_WARM, mWashStatusEvent);
                                    }
                                }
                            }
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /*
     * 发送冷水指令
     */
    public void sendCold() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_COLD;
        ++seq;
        long recCount = mRecCount;
        isSuccess.set(false);
        mColdDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (!isSuccess.get()) {
                        if (mWashStatusEvent != null) {//不是设置模式，
                            if (!mWashStatusEvent.isSetting()) {
                                if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x04) {//未开始洗衣 && 洗衣模式为热水
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_COLD, mWashStatusEvent);
                                    }
                                } else {
                                    //todo
                                }
                            } else {//设置模式
                                if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                    if (mRecCount > recCount) {//收到新报文
                                        isSuccess.set(true);
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_COLD, mWashStatusEvent);
                                        }
                                    } else {
                                        //todo
                                    }
                                } else {
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_COLD, mWashStatusEvent);
                                    }
                                }
                            }
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /*
     * 发送精致衣物指令
     */
    public void sendDelicates() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_DELICATES;
        ++seq;
        long recCount = mRecCount;
        isSuccess.set(false);
        mDelicatesDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (!isSuccess.get()) {
                        if (mWashStatusEvent != null) {//不是设置模式，
                            if (!mWashStatusEvent.isSetting()) {
                                if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x05) {//未开始洗衣 && 洗衣模式为热水
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_DELICATES, mWashStatusEvent);
                                    }
                                } else {
                                    //todo
                                }
                            } else {//设置模式
                                if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                    if (mRecCount > recCount) {//收到新报文
                                        isSuccess.set(true);
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_DELICATES, mWashStatusEvent);
                                        }
                                    } else {
                                        //todo
                                    }
                                } else {
                                    isSuccess.set(true);
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_DELICATES, mWashStatusEvent);
                                    }
                                }
                            }
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /*
     * 发送加强指令
     */
    public void sendSuper() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_SUPER;
        ++seq;
        isSuccess.set(false);
        mSuperDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (!isSuccess.get()) {
                        if (mWashStatusEvent != null) {
                            if (mCount == 0) {
                                mPreIsSupper = mWashStatusEvent.getLightSupper();
                            } else {
                                if (mPreIsSupper == 1) {
                                    if (mWashStatusEvent.getLightSupper() == 0) {
                                        mPreIsSupper = mWashStatusEvent.getLightSupper();
                                        mCount = 0;
                                        isSuccess.set(true);
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_SUPER, mWashStatusEvent);
                                        }
                                    }
                                } else if (mPreIsSupper == 0) {
                                    if (mWashStatusEvent.getLightSupper() == 1) {
                                        mPreIsSupper = mWashStatusEvent.getLightSupper();
                                        mCount = 0;
                                        isSuccess.set(true);
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_SUPER, mWashStatusEvent);
                                        }
                                    }
                                }
                            }
                            mCount++;
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /*
     * 发送开始指令
     */
    public void sendStartOrStop() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_START;
        ++seq;
        long recCount = mRecCount;
        isSuccess.set(false);
        mStartDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (!isSuccess.get()) {
                        if (mRecCount > recCount) {//收到新报文
                            isSuccess.set(true);
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_START, mWashStatusEvent);
                            }
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /*
     * 发送设置指令
     */
    public void sendSetting() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_SETTING;
        ++seq;
        isSuccess.set(false);
        mSettingDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (!isSuccess.get()) {
                        if (mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "setting mode success");
                            isSuccess.set(true);
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_SETTING, mWashStatusEvent);
                            }
                        } else {
                            Log.e(TAG, "setting mode error");
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SETTING, "setting mode error");
                            }
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /*
     * 发送kill指令
     */
    public void sendKill() {
        //判断是处于洗衣状态才可以使用kill
        Observable.create(e -> {
            kill();
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    /*
     * 执行kill过程
     */
    private void kill() {
        isKilling = true;
        dispose(mKey);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting()) {//设置模式
            mKey = DeviceAction.JuRenPlus.ACTION_HOT;
            ++seq;
            do {
                sendData(mKey);
                SystemClock.sleep(mPeriod);
            } while (mWashStatusEvent.isSetting() || mWashStatusEvent.getErr() > 0);//不是设置状态，也不是错误状态
        }

        if (mWashStatusEvent != null && mWashStatusEvent.getErr() > 0) {
            mKey = DeviceAction.JuRenPlus.ACTION_START;
            ++seq;
            do {
                sendData(mKey);
                SystemClock.sleep(mPeriod);
            } while (mWashStatusEvent == null || mWashStatusEvent.getIsWashing() != 0x40);
        }

        mKey = DeviceAction.JuRenPlus.ACTION_SETTING;
        ++seq;
        for (; ; ) {
            sendData(mKey);
            SystemClock.sleep(mPeriod);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step1 success");
                break;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        for (int i = 0; i < 3; i++) {
            ++seq;
            do {
                sendData(mKey);
                SystemClock.sleep(mPeriod);
            } while (mWashStatusEvent == null || !mWashStatusEvent.isSetting() || !(i + 1 + "").equals(mWashStatusEvent.getText()));
        }

        mKey = DeviceAction.JuRenPlus.ACTION_START;
        ++seq;
        for (; ; ) {
            sendData(mKey);
            SystemClock.sleep(mPeriod);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("LgC1").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step3 success");
                break;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        for (int i = 0; i < 4; i++) {
            ++seq;
            for (; ; ) {
                sendData(mKey);
                SystemClock.sleep(mPeriod);
                if (mWashStatusEvent != null && mWashStatusEvent.isSetting()) {
                    if (i == 0) {
                        if (("tcL").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                            break;
                        }
                    } else if (i == 1) {
                        if (("PA55").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                            break;
                        }
                    } else if (i == 2) {
                        if (("dUCt").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                            break;
                        }
                    } else {
                        if (("h1LL").equals(mWashStatusEvent.getText())) {
                            Log.e(TAG, "kill step4->" + i + "success");
                            break;
                        }
                    }
                }
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_START;
        ++seq;
        for (; ; ) {
            sendData(mKey);
            SystemClock.sleep(mPeriod);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step5 success");
                break;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        for (int i = 0; i < 17; i++) {
            ++seq;
            for (; ; ) {
                sendData(mKey);
                SystemClock.sleep(mPeriod);
                if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                    Log.e(TAG, "kill step6 success");
                    break;
                }
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_START;
        ++seq;
        for (int i = 0; i < 50; i++) {
            sendData(mKey);
            SystemClock.sleep(mPeriod);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step7 success");
                break;
            }
        }
        --seq;
        sendStartOrStop();
        mKill = Observable.intervalRange(0, 240, 0, 1, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(aLong -> {
                    Log.e(TAG, "doOnNext--->" + aLong);
                    if (mWashStatusEvent != null) {
                        if (mWashStatusEvent.getViewStep() == DeviceWorkType.WORKTYPR_END && !mWashStatusEvent.isSetting() && mWashStatusEvent.getLightlock() == 0) {//end状态
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_KILL, mWashStatusEvent);
                            }
                            Log.e(TAG, "kill success");
                            if (mKill != null && !mKill.isDisposed()) {
                                mKill.dispose();
                            }
                        } else {
                            if (aLong == 239) {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_KILL, "kill error");
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

    /**
     * 复位
     */
    public void sendReset() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_RESET;
        ++seq;
        mResetDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .doOnNext(aLong -> sendData(mKey))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (isOnline) {
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_RESET, mWashStatusEvent);
                        }
                    } else if (aLong == 50) {
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_RESET, "sendReset fail");
                        }
                    }
                })
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .subscribe();
    }

    /**
     * 发送桶自洁指令
     */
    public void sendSelfCleaning() {
        Observable.create(e -> {
            selfCleaning();
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    private synchronized void selfCleaning() {
        dispose(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_SETTING;
        ++seq;
        int killIntervalCount = 50;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "selfCleaning step1 success");
                break;
            }
            if (i == killIntervalCount - 1) {
                Log.e(TAG, "selfCleaning step1 error");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, "selfCleaning step1 error");
                            }
                        })
                        .subscribe();
                return;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        for (int i = 0; i < 3; i++) {
            ++seq;
            for (int j = 0; j < killIntervalCount; j++) {
                sendData(mKey);
                SystemClock.sleep(50);
                if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                    Log.e(TAG, "selfCleaning step2->" + i + "success");
                    break;
                }
                if (j == killIntervalCount - 1) {
                    Log.e(TAG, "selfCleaning step2->" + i + "error");
                    Observable.just(1)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext(integer -> {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, "selfCleaning step2->\" + i + \"error");
                                }
                            })
                            .subscribe();
                    return;
                }
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_START;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("LgC1").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "selfCleaning step3 success");
                break;
            }
            if (i == killIntervalCount - 1) {
                Log.e(TAG, "selfCleaning step3 error");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, "selfCleaning step3 error");
                            }
                        })
                        .subscribe();
                return;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("tcL").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "selfCleaning step4 success");
                break;
            }
            if (i == killIntervalCount - 1) {
                Log.e(TAG, "selfCleaning step4 error");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, "selfCleaning step4 error");
                            }
                        })
                        .subscribe();
                return;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_START;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "selfCleaning step5 success");
                break;
            }
            if (i == killIntervalCount - 1) {
                Log.e(TAG, "selfCleaning step5 error");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, "selfCleaning step5 error");
                            }
                        })
                        .subscribe();
                return;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("1").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "selfCleaning step6 success");
                break;
            }
            if (i == killIntervalCount - 1) {
                Log.e(TAG, "selfCleaning step6 error");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, "selfCleaning step6 error");
                            }
                        })
                        .subscribe();
                return;
            }
        }

        mKey = DeviceAction.JuRenPlus.ACTION_START;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
            if (mWashStatusEvent != null && (mWashStatusEvent.getViewStep() == 1 || mWashStatusEvent.getViewStep() == 6) && mWashStatusEvent.getWashMode() == 0x08) {
                Log.e(TAG, "selfCleaning success");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, mWashStatusEvent);
                            }
                        })
                        .subscribe();
                break;
            }
            if (i == killIntervalCount - 1) {
                Log.e(TAG, "selfCleaning error");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING, "selfCleaning error");
                            }
                        })
                        .subscribe();
                return;
            }
        }
        --seq;
        sendStartOrStop();
    }

    /**
     * 设置音量到HIGH
     */
    public void sendVomHigh() {
        Observable.create(e -> {
            vomHigh();
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();

    }

    private void vomHigh() {
        //调音量的设置
        mKey = DeviceAction.JuRenPlus.ACTION_SETTING;
        sendData(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        for (int i = 0; i < 3; i++) {
            sendData(mKey);
        }
        mKey = DeviceAction.JuRenPlus.ACTION_START;
        sendData(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_HOT;
        sendData(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        sendData(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_START;
        sendData(mKey);
        mKey = DeviceAction.JuRenPlus.ACTION_WARM;
        for (int i = 0; i < 9; i++) {
            sendData(mKey);
        }
        mKey = DeviceAction.JuRenPlus.ACTION_START;
        sendData(mKey);
        while (!mWashStatusEvent.getText().equals("H19H")) {
            mKey = DeviceAction.JuRenPlus.ACTION_WARM;
            sendData(mKey);
        }

        mKey = DeviceAction.JuRenPlus.ACTION_START;
        sendData(mKey);
    }

    private void sendData(int key) {
        msg[2] = (byte) (key & 0xff);
        msg[3] = (byte) (seq & 0xff);
        short crc16_a = mCrc16.getCrc(msg, 0, msg.length - 3);
        msg[msg.length - 2] = (byte) (crc16_a >> 8);
        msg[msg.length - 3] = (byte) (crc16_a & 0xff);
        if (mBufferedOutputStream != null) {
            try {
                mBufferedOutputStream.write(msg);
                mBufferedOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "发送：" + MyFunc.ByteArrToHex(msg));
    }

    private void dispose(int key) {
        switch (key) {
            case DeviceAction.JuRenPlus.ACTION_START:
                if (mStartDisposable != null && !mStartDisposable.isDisposed()) {
                    mStartDisposable.dispose();
                }
                break;
            case DeviceAction.JuRenPlus.ACTION_HOT:
                if (mHotDisposable != null && !mHotDisposable.isDisposed()) {
                    mHotDisposable.dispose();
                }
                break;
            case DeviceAction.JuRenPlus.ACTION_WARM:
                if (mWarmDisposable != null && !mWarmDisposable.isDisposed()) {
                    mWarmDisposable.dispose();
                }
                break;
            case DeviceAction.JuRenPlus.ACTION_COLD:
                if (mColdDisposable != null && !mColdDisposable.isDisposed()) {
                    mColdDisposable.dispose();
                }
                break;
            case DeviceAction.JuRenPlus.ACTION_DELICATES:
                if (mDelicatesDisposable != null && !mDelicatesDisposable.isDisposed()) {
                    mDelicatesDisposable.dispose();
                }
                break;
            case DeviceAction.JuRenPlus.ACTION_SUPER:
                if (mSuperDisposable != null && !mSuperDisposable.isDisposed()) {
                    mSuperDisposable.dispose();
                }
                break;
            case DeviceAction.JuRenPlus.ACTION_SETTING:
                if (mSettingDisposable != null && !mSettingDisposable.isDisposed()) {
                    mSettingDisposable.dispose();
                }
                break;
            case DeviceAction.JuRenPlus.ACTION_RESET:
                if (mResetDisposable != null && !mResetDisposable.isDisposed()) {
                    mResetDisposable.dispose();
                }
                break;
        }
        if (mKill != null && !mKill.isDisposed()) {
            mKill.dispose();
        }
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

