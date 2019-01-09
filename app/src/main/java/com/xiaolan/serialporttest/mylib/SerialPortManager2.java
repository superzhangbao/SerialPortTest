package com.xiaolan.serialporttest.mylib;

import android.util.Log;

import com.xiaolan.serialporttest.event.WashStatusEvent;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.Objects;

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * 串口操作工具类
 *
 */

/**
 * 测试push
 */

/**
 * 测试github push
 */
public class SerialPortManager2 {
    private static final String TAG = "SerialPortManager2";
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
    private static final int KEY_1 = 1;//开始
    private static final int KEY_2 = 2;//热水
    private static final int KEY_3 = 3;//温水
    private static final int KEY_4 = 4;//冷水
    private static final int KEY_5 = 5;//精致衣物
    private static final int KEY_6 = 6;//加强洗
    private static final int KEY_8 = 8;//setting
    private static final int KEY_KILL = 10;//setting
    private static final int INSTRUCTION_MODE = 0;
    private int mCount = 0;
    private int mPreIsSupper;
    private WashStatusManager mWashStatusManager;
    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortReadDataListener mSerialPortReadDataListener;
    private CurrentStatusListener mCurrentStatusListener;
    private WashStatusEvent mWashStatusEvent;
    private long mRecCount = 0;//接收到的报文次数
    private int mSendCount = 0;//发送的某个报文次数
    private long mDataTrueData = 0;
    private long mFirstDataTime = 0;
    private boolean mFirstDisconnect = true;
    private long mFirstDisconnectTime = 0;
    private boolean mFirstData = true;
    private long mNotFirstDataTime = 0;



    public SerialPortManager2() {
        this("/dev/ttyS3", 9600);
    }

    public SerialPortManager2(String sPort) {
        this(sPort, 9600);
    }

    public SerialPortManager2(String sPort, String sBaudRate) {
        this(sPort, Integer.parseInt(sBaudRate));
    }

    public SerialPortManager2(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public void juRenPlusOpen() throws SecurityException, IOException, InvalidParameterException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, mDataBits, mStopBits, mParityBits);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();

        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
        mCrc16 = new CRC16();
        mWashStatusManager = new WashStatusManager();
        mReadThread = new ReadThread();
        mReadThread.start();
        _isOpen = true;
    }

    public void close() throws IOException {
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
        _isOpen = false;
        mFirstData = true;
        mNotFirstDataTime = 0;
        mFirstDisconnect = true;
        mFirstDataTime = 0;
        mDataTrueData = 0;
        mFirstDisconnectTime = 0;
    }

    public void send(byte[] bOutArray) {
        try {
            mBufferedOutputStream.write(bOutArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendHex(String sHex) {
        byte[] bOutArray = MyFunc.HexToByteArr(sHex);
        send(bOutArray);
    }

    public void sendTxt(String sTxt) {
        byte[] bOutArray = sTxt.getBytes();
        send(bOutArray);
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
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mBufferedInputStream.available() <= 0) {
                        //串口断开会走这里
                        if (mFirstDisconnect) {
                            mFirstDisconnectTime = new Date().getTime();
                            mFirstDisconnect = false;
                            mDataTrueData = 0;
                            mNotFirstDataTime = 0;
                            mFirstData = true;
                        }else {
                            long time = new Date().getTime();
                            if (time - mFirstDisconnectTime > 5000) {
                                if (mSerialPortReadDataListener != null) {
                                    mSerialPortReadDataListener.onSerialPortReadDataFail("串口断开");
                                }
                                mFirstDisconnectTime = time;
                                Log.e(TAG,"串口断开");
                            }
                        }
                    } else {
                        if (mFirstData) {
                            mFirstDataTime = new Date().getTime();
                            mFirstData = false;
                        }else {
                            mNotFirstDataTime = new Date().getTime();
                            if (mNotFirstDataTime - mFirstDataTime > 5000) {
                                if (mSerialPortReadDataListener != null) {
                                    mSerialPortReadDataListener.onSerialPortReadDataFail("串口无数据");
                                    Log.e(TAG,"串口无数据");
                                }
                                mFirstData = true;
                            }
                            mFirstDataTime = mNotFirstDataTime;
                        }
                        len = mBufferedInputStream.read(buffer);
                        if (len != -1) {
//                            Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
                            if (len >= 30) {
                                //读巨人洗衣机上报报文
                                JuRenPlusWashRead(buffer, len);
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    if (mSerialPortReadDataListener != null) {
                        mSerialPortReadDataListener.onSerialPortReadDataFail(e.toString());
                    }
                    Log.e(TAG, "run: 数据读取异常：" + e.toString());
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
                        //判断串口是否掉线
                        if (mDataTrueData == 0) {
                            mDataTrueData = new Date().getTime();
                            Log.e(TAG,"串口上报正确数据");
                        }else if (mDataTrueData > 0){
                            long time = new Date().getTime();
                            if (time - mDataTrueData > 5000) {
                                if (mSerialPortReadDataListener != null) {
                                    mSerialPortReadDataListener.onSerialPortReadDataFail("串口无正确数据");
                                    Log.e(TAG,"串口无正确数据");
                                }
                            }
                            mDataTrueData = time;
                        }
//                            Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, off02 + 1)));
                        byte[] subarray = ArrayUtils.subarray(buffer, off02, off02 + size);
                        if (!Objects.deepEquals(mPreMsg, subarray)) {
                            Log.e(TAG, "接收：" + MyFunc.ByteArrToHex(subarray));
                            mPreMsg = subarray;
                            //根据上报的报文，分析设备状态
                            mWashStatusEvent = mWashStatusManager.analyseStatus(subarray);
                            mRecCount++;
                            Observable.create(e -> {
                                if (mCurrentStatusListener != null) {
                                    mCurrentStatusListener.currentStatus(mWashStatusEvent);
                                }
                                e.onComplete();
                            }).observeOn(AndroidSchedulers.mainThread())
                                    .subscribe();
                        }
                    }
                }
            }
        }
    }

    /**
     * 发送热水指令
     */
    public void sendHot() {
        mKey = KEY_2;
        long recCount = mRecCount;
        Observable.create(e -> {
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
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendHot();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send hot error");
                                    }
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendHot();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send hot error");
                                        }
                                    }
                                }
                            }else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                            }
                        }
                    }
                })
                .subscribe();
    }

    /**
     * 发送温水指令
     */
    public void sendWarm() {
        mKey = KEY_3;
        long recCount = mRecCount;
        Observable.create(e -> {
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
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendWarm();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send warm error");
                                    }
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendWarm();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send warm error");
                                        }
                                    }
                                }
                            }else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                            }
                        }
                    }
                }).subscribe();
    }

    /**
     * 发送冷水指令
     */
    public void sendCold() {
        mKey = KEY_4;
        long recCount = mRecCount;
        Observable.create(e -> {
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
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendCold();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send cold error");
                                    }
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendCold();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send cold error");
                                        }
                                    }
                                }
                            }else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                            }
                        }
                    }
                })
                .subscribe();
    }

    /**
     * 发送精致衣物指令
     */
    public void sendDelicates() {
        mKey = KEY_5;
        long recCount = mRecCount;
        Observable.create(e -> {
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
                            } else {
                                if (mSendCount <= 3) {
                                    mSendCount++;
                                    sendDelicates();
                                } else {//连续发3轮没收到新报文则判定指令发送失败
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send delicates error");
                                    }
                                }
                            }
                        } else {//设置模式
                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
                                if (mRecCount > recCount) {//收到新报文
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                } else {
                                    if (mSendCount <= 3) {
                                        mSendCount++;
                                        sendDelicates();
                                    } else {//连续发3轮没收到新报文则判定指令发送失败
                                        if (mOnSendInstructionListener != null) {
                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send delicates error");
                                        }
                                    }
                                }
                            }else {
                                if (mOnSendInstructionListener != null) {
                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                }
                            }
                        }
                    }
                })
                .subscribe();
    }

    /**
     * 发送加强指令
     */
    public void sendSuper() {
        mKey = KEY_6;
        Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
//                    if (mRecCount > recCount) {//收到新报文
//                        if (mOnSendInstructionListener != null) {
//                            mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                        }
//                    } else {
//                        if (mSendCount <= 3) {
//                            mSendCount++;
//                            sendSuper();
//                        } else {//连续发3轮没收到新报文则判定指令发送失败
//                            if (mOnSendInstructionListener != null) {
//                                mOnSendInstructionListener.sendInstructionFail(mKey,"send super error");
//                            }
//                        }
//                    }
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
                                }
                            } else if (mPreIsSupper == 0) {
                                if (mWashStatusEvent.getLightSupper() == 1) {
                                    mPreIsSupper = mWashStatusEvent.getLightSupper();
                                    mCount = 0;
                                    if (mOnSendInstructionListener != null) {
                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                                    }
                                }
                            }
                        }
                        mCount++;
                    }
                })
                .subscribe();
    }

    /**
     * 发送开始指令
     */
    public void sendStartOrStop() {
        mKey = KEY_1;
        long recCount = mRecCount;
        Observable.create(e -> {
            sendData(mKey, INSTRUCTION_MODE);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    if (mRecCount > recCount) {//收到新报文
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                        }
                    } else {
                        if (mSendCount <= 3) {
                            mSendCount++;
                            sendStartOrStop();
                        } else {//连续发3轮没收到新报文则判定指令发送失败
                            if (mOnSendInstructionListener != null) {
                                mOnSendInstructionListener.sendInstructionFail(mKey, "send startOrStop error");
                            }
                        }
                    }
                })
                .subscribe();
    }

    /**
     * 发送设置指令
     */
    public void sendSetting() {
        mKey = KEY_8;
        Observable.create(e -> {
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
                    } else {
                        Log.e(TAG, "setting mode error");
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionFail(mKey, "setting mode error");
                        }
                    }
                })
                .subscribe();
    }

    /**
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

    /**
     * 执行kill过程
     */
    public void kill() {
        isKilling = true;
        mKey = KEY_8;
        sendData(mKey, 0);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step1 success");
        } else {
            Log.e(TAG, "kill step1 error");
        }
        mKey = KEY_3;
        for (int i = 0; i < 3; i++) {
            sendData(mKey, 0);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step2->" + i + "success");
            } else {
                Log.e(TAG, "kill step2->" + i + "error");
            }
        }
        mKey = KEY_1;
        sendData(mKey, 0);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("LgC1").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step3 success");
        } else {
            Log.e(TAG, "kill step3 error");
        }
        mKey = KEY_3;
        for (int i = 0; i < 4; i++) {
            sendData(mKey, 0);
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
        mKey = KEY_1;
        sendData(mKey, 0);
        if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
            Log.e(TAG, "kill step5 success");
        } else {
            Log.e(TAG, "kill step5 error");
        }
        mKey = KEY_3;
        for (int i = 0; i < 17; i++) {
            sendData(mKey, 0);
            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
                Log.e(TAG, "kill step6 success");
            } else {
                Log.e(TAG, "kill step6 error");
            }
        }
        mKey = KEY_1;
        sendData(mKey, 0);
        if (mWashStatusEvent != null && !mWashStatusEvent.isSetting()) {
            if (mOnSendInstructionListener != null) {
                mOnSendInstructionListener.sendInstructionSuccess(KEY_KILL, mWashStatusEvent);
            }
            Log.e(TAG, "kill success");
        } else {
            if (mOnSendInstructionListener != null) {
                mOnSendInstructionListener.sendInstructionFail(mKey, "kill error");
            }
            Log.e(TAG, "kill error");
        }
        isKilling = false;
        mRecCount = 0;
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
//            if (checkWashStatus()) {
            try {
                mBufferedOutputStream.write(msg);
                mBufferedOutputStream.flush();
                Log.e(TAG, "发送：" + MyFunc.ByteArrToHex(msg));
            } catch (IOException e) {
                e.printStackTrace();
            }
//            } else {
//                break;
////            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        seq++;
    }

    /**
     * 检查上报状态是否符合
     */
    private boolean checkWashStatus() {
        switch (mKey) {
            case KEY_1:
                break;
            case KEY_2:
                break;
            case KEY_3:
                break;
            case KEY_4:
                break;
            case KEY_5:
                break;
            case KEY_6://加强洗
                break;
            case KEY_8:
                break;
        }
        return true;
    }

    int getBaudRate() {
        return iBaudRate;
    }

    boolean setBaudRate(int iBaud) {
        if (_isOpen) {
            return false;
        } else {
            iBaudRate = iBaud;
            return true;
        }
    }

    public String getPort() {
        return sPort;
    }

    public boolean setPort(String sPort) {
        if (_isOpen) {
            return false;
        } else {
            this.sPort = sPort;
            return true;
        }
    }

    public String getDataBits() {
        return mDataBits;
    }

    public boolean setDataBits(String dataBits) {
        if (_isOpen) {
            return false;
        } else {
            mDataBits = dataBits;
            return true;
        }
    }

    public String getStopBits() {
        return mStopBits;
    }

    public boolean setStopBits(String stopBits) {
        if (_isOpen) {
            return false;
        } else {
            mStopBits = stopBits;
            return true;
        }
    }

    public String getParityBits() {
        return mParityBits;
    }

    public boolean setParityBits(String parityBits) {
        if (_isOpen) {
            return false;
        } else {
            mParityBits = parityBits;
            return true;
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

    public void setOnSerialPortConnectListener(SerialPortReadDataListener serialPortReadDataListener) {
        mSerialPortReadDataListener = serialPortReadDataListener;
    }
}

