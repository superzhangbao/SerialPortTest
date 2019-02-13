package com.xiaolan.serialporttest.wash.jurenpro;

import android.util.Log;

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

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 巨人Pro串口操作工具类
 */

public class JuRenProSerialPortHelper {
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
    private static final int KEY_RESTORATION = 0;//复位
    private static final int KEY_START = 1;//开始
    private static final int KEY_HOT = 2;//热水
    private static final int KEY_WARM = 3;//温水
    private static final int KEY_COLD = 4;//冷水
    private static final int KEY_DELICATES = 5;//精致衣物
    private static final int KEY_SUPER = 6;//加强洗
    private static final int KEY_SETTING = 8;//setting
    private static final int KEY_KILL = 10;//kill
    private static final int INSTRUCTION_MODE = 0;//指令类型
    private int mCount = 0;
    private int mPreIsSupper;
    private JuRenProWashStatus mJuRenProWashStatus;
    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortOnlineListener mSerialPortOnlineListener;
    private CurrentStatusListener mCurrentStatusListener;
    private WashStatusEvent mWashStatusEvent;
    private long mRecCount = 0;//接收到的报文次数
    private int mSendCount = 0;//发送的某个报文次数
    private Disposable mKill;
    private Disposable mHotDisposable;
    private Disposable mWarmDisposable;
    private Disposable mColdDisposable;
    private Disposable mDelicatesDisposable;
    private Disposable mSuperDisposable;
    private Disposable mStartDisposable;
    private Disposable mSettingDisposable;
    private boolean isOnline = false;
    private boolean hasOnline = false;
    private long mPreOnlineTime = 0;
    private long readThreadStartTime = 0;


    public JuRenProSerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

//    public JuRenProSerialPortHelper(String sPort) {
//        this(sPort, 9600);
//    }
//
//    public JuRenProSerialPortHelper(String sPort, String sBaudRate) {
//        this(sPort, Integer.parseInt(sBaudRate));
//    }

    public JuRenProSerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public void juRenProOpen() throws SecurityException, IOException, InvalidParameterException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, mDataBits, mStopBits, mParityBits);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();

        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
        mCrc16 = new CRC16();
        mJuRenProWashStatus = new JuRenProWashStatus();
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
                            mWashStatusEvent = mJuRenProWashStatus.analyseStatus(subarray);
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

    /*
     * 发送热水指令
     */
    public void sendHot() {
        mKey = KEY_HOT;
        long recCount = mRecCount;
        mHotDisposable = Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
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

    /*
     * 发送温水指令
     */
    public void sendWarm() {
        mKey = KEY_WARM;
        long recCount = mRecCount;
        mWarmDisposable = Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
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

    /*
     * 发送冷水指令
     */
    public void sendCold() {
        mKey = KEY_COLD;
        long recCount = mRecCount;
        mColdDisposable = Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
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
                .subscribe();
    }

    /*
     * 发送精致衣物指令
     */
    public void sendDelicates() {
        mKey = KEY_DELICATES;
        long recCount = mRecCount;
        mDelicatesDisposable = Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
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

    /*
     * 发送加强指令
     */
    public void sendSuper() {
        mKey = KEY_SUPER;
        mSuperDisposable = Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
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

    /*
     * 发送开始指令
     */
    public void sendStartOrStop() {
        mKey = KEY_START;
        long recCount = mRecCount;
        mStartDisposable = Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
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

    /*
     * 发送设置指令
     */
    public void sendSetting() {
        mKey = KEY_SETTING;
        mSettingDisposable = Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
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

    /*
     * 发送kill指令
     */
    public void sendKill() {
        //判断是处于洗衣状态才可以使用kill
        if (mWashStatusEvent != null && mWashStatusEvent.getIsWashing() == 0x40) {
            Observable.create(e -> {
                kill();
                e.onComplete();
            }).subscribeOn(Schedulers.io())
                    .subscribe();
        }
    }

    /*
     * 执行kill过程
     */
    private void kill() {
        isKilling = true;
        mKey = KEY_SETTING;
        sendData(mKey, INSTRUCTION_MODE);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step1 success");
        } else {
            Log.e(TAG, "kill step1 error");
        }
        mKey = KEY_WARM;
        for (int i = 0; i < 3; i++) {
            sendData(mKey, INSTRUCTION_MODE);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step2->" + i + "success");
            } else {
                Log.e(TAG, "kill step2->" + i + "error");
            }
        }
        mKey = KEY_START;
        sendData(mKey, INSTRUCTION_MODE);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("LgC1").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step3 success");
        } else {
            Log.e(TAG, "kill step3 error");
        }
        mKey = KEY_WARM;
        for (int i = 0; i < 4; i++) {
            sendData(mKey, INSTRUCTION_MODE);
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
        sendData(mKey, INSTRUCTION_MODE);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step5 success");
        } else {
            Log.e(TAG, "kill step5 error");
        }
        mKey = KEY_WARM;
        for (int i = 0; i < 17; i++) {
            sendData(mKey, INSTRUCTION_MODE);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step6 success");
            } else {
                Log.e(TAG, "kill step6 error");
            }
        }
        mKey = KEY_START;
        sendData(mKey, INSTRUCTION_MODE);
        mKill = Observable.intervalRange(0, 240, 0, 1, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(aLong -> {
                    Log.e(TAG, "doOnNext--->" + aLong);
                    if (mWashStatusEvent != null) {
                        if (mWashStatusEvent.getViewStep() == DeviceWorkType.WORKTYPR_END && !mWashStatusEvent.isSetting() && mWashStatusEvent.getLightlock() == 0) {//end状态
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
        mKey = KEY_SETTING;
        sendData(mKey, INSTRUCTION_MODE);
        mKey = KEY_WARM;
        for (int i = 0; i < 3; i++) {
            sendData(mKey, INSTRUCTION_MODE);
        }
        mKey = KEY_START;
        sendData(mKey, INSTRUCTION_MODE);
        mKey = KEY_HOT;
        sendData(mKey, INSTRUCTION_MODE);
        mKey = KEY_WARM;
        sendData(mKey, INSTRUCTION_MODE);
        mKey = KEY_START;
        sendData(mKey, INSTRUCTION_MODE);
        mKey = KEY_WARM;
        for (int i = 0; i < 9; i++) {
            sendData(mKey, INSTRUCTION_MODE);
        }
        mKey = KEY_START;
        sendData(mKey, INSTRUCTION_MODE);
        while (!mWashStatusEvent.getText().equals("H19H")) {
            mKey = KEY_WARM;
            sendData(mKey, INSTRUCTION_MODE);
        }
        mKey = KEY_START;
        sendData(mKey, INSTRUCTION_MODE);
    }

    private void sendData(int key, int t) {
        byte[] msg;
        if (t == 1) {
            msg = new byte[]{0x02, 0x06, (byte) (key & 0xff), (byte) (seq & 0xff), (byte) 0x80, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        } else {
            msg = new byte[]{0x02, 0x06, (byte) (key & 0xff), (byte) (seq & 0xff), (byte) 0x80, 0x20, 0, 1, 0, 0, 3};
        }
        short crc16_a = mCrc16.getCrc(msg, 0, msg.length - 3);
        msg[msg.length - 2] = (byte) (crc16_a >> 8);
        msg[msg.length - 3] = (byte) (crc16_a & 0xff);
        for (int i = 0; i < 8; i++) {
            try {
                mBufferedOutputStream.write(msg);
                mBufferedOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "发送：" + MyFunc.ByteArrToHex(msg));
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        seq++;
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

