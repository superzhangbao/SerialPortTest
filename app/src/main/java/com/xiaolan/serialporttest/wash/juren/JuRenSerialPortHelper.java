package com.xiaolan.serialporttest.wash.juren;

import android.util.Log;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.WashStatusManager;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortReadDataListener;
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

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 巨人洗衣机串口操作工具类
 */
public class JuRenSerialPortHelper {
    private static final String TAG = "JuRenSerialPortHelper";

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
    private long mRecCount = 0;//接收到的报文次数
    private int mSendCount = 0;//发送的某个报文次数
    private WashStatusManager mWashStatusManager;

    private Disposable mKill;
    private Disposable mHotDisposable;
    private Disposable mWarmDisposable;
    private Disposable mColdDisposable;
    private Disposable mDelicatesDisposable;
    private Disposable mSuperDisposable;
    private Disposable mStartDisposable;
    private Disposable mSettingDisposable;

    private WashStatusEvent mWashStatusEvent;


    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortReadDataListener mSerialPortReadDataListener;
    private CurrentStatusListener mCurrentStatusListener;

    public JuRenSerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

    public JuRenSerialPortHelper(String sPort) {
        this(sPort, 9600);
    }

    public JuRenSerialPortHelper(String sPort, String sBaudRate) {
        this(sPort, Integer.parseInt(sBaudRate));
    }

    public JuRenSerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
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
                                JuRenWashRead(buffer, len);
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete(() -> {
                                if (mSerialPortReadDataListener != null) {
                                    mSerialPortReadDataListener.onSerialPortReadDataFail(t.getMessage());
                                }
                            })
                            .subscribe();
                    Log.e(TAG, "run: 数据读取异常：" + t.toString());
                    break;
                }
            }
        }
    }

    private void JuRenWashRead(byte[] buffer, int len) {
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
//                            Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, off02 + 1)));
                        byte[] subarray = ArrayUtils.subarray(buffer, off02, off02 + size);
                        if (!Objects.deepEquals(mPreMsg, subarray)) {
                            Log.e(TAG, "接收：" + MyFunc.ByteArrToHex(subarray));
                            mPreMsg = subarray;
                            //根据上报的报文，分析设备状态
                            mWashStatusEvent = mWashStatusManager.analyseStatus("DeviceAction.JuRen",subarray);
                            mRecCount++;
                            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                    .doOnComplete(() -> {
                                        if (mSerialPortReadDataListener != null) {
                                            mSerialPortReadDataListener.onSerialPortReadDataSuccess(subarray);
                                        }
                                        if (mCurrentStatusListener != null) {
                                            mCurrentStatusListener.currentStatus(mWashStatusEvent);
                                        }
                                    })
                                    .subscribe();
                        }
                    }
                }
            }
        }
    }

    public void sendStart() {
        mKey = DeviceAction.JuRen.ACTION_START;
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
                            sendStart();
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

    public void sendWhites() {
        mKey = DeviceAction.JuRen.ACTION_WHITES;
        long recCount = mRecCount;
        mHotDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
//                    if (mWashStatusEvent != null) {
//                        if (!mWashStatusEvent.isSetting()) {//不是设置模式
//                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x02) {//未开始洗衣 && 洗衣模式为热水
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            } else {
//                                if (mSendCount <= 3) {
//                                    mSendCount++;
//                                    sendWhites();
//                                } else {//连续发3轮没收到新报文则判定指令发送失败
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send hot error");
//                                    }
//                                    dispose(mKey);
//                                }
//                            }
//                        } else {//设置模式
//                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
//                                if (mRecCount > recCount) {//收到新报文
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                } else {
//                                    if (mSendCount <= 3) {
//                                        mSendCount++;
//                                        sendHot();
//                                    } else {//连续发3轮没收到新报文则判定指令发送失败
//                                        if (mOnSendInstructionListener != null) {
//                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send hot error");
//                                        }
//                                        dispose(mKey);
//                                    }
//                                }
//                            } else {
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            }
//                        }
//                    }
                })
                .subscribe();
    }

    public void sendColors() {
        mKey = DeviceAction.JuRen.ACTION_COLORS;
        long recCount = mRecCount;
        mWarmDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
//                    if (mWashStatusEvent != null) {//不是设置模式
//                        if (!mWashStatusEvent.isSetting()) {
//                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x03) {//未开始洗衣 && 洗衣模式为热水
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            } else {
//                                if (mSendCount <= 3) {
//                                    mSendCount++;
//                                    sendWarm();
//                                } else {//连续发3轮没收到新报文则判定指令发送失败
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send warm error");
//                                    }
//                                    dispose(mKey);
//                                }
//                            }
//                        } else {//设置模式
//                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
//                                if (mRecCount > recCount) {//收到新报文
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                } else {
//                                    if (mSendCount <= 3) {
//                                        mSendCount++;
//                                        sendWarm();
//                                    } else {//连续发3轮没收到新报文则判定指令发送失败
//                                        if (mOnSendInstructionListener != null) {
//                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send warm error");
//                                        }
//                                        dispose(mKey);
//                                    }
//                                }
//                            } else {
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            }
//                        }
//                    }
                }).subscribe();
    }

    public void sendDelicates() {
        mKey = DeviceAction.JuRen.ACTION_DELICATES;
        long recCount = mRecCount;
        mColdDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
//                    if (mWashStatusEvent != null) {//不是设置模式，
//                        if (!mWashStatusEvent.isSetting()) {
//                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x04) {//未开始洗衣 && 洗衣模式为热水
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            } else {
//                                if (mSendCount <= 3) {
//                                    mSendCount++;
//                                    sendCold();
//                                } else {//连续发3轮没收到新报文则判定指令发送失败
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send cold error");
//                                    }
//                                    dispose(mKey);
//                                }
//                            }
//                        } else {//设置模式
//                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
//                                if (mRecCount > recCount) {//收到新报文
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                } else {
//                                    if (mSendCount <= 3) {
//                                        mSendCount++;
//                                        sendCold();
//                                    } else {//连续发3轮没收到新报文则判定指令发送失败
//                                        if (mOnSendInstructionListener != null) {
//                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send cold error");
//                                        }
//                                        dispose(mKey);
//                                    }
//                                }
//                            } else {
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            }
//                        }
//                    }
                })
                .subscribe();
    }

    public void sendPermPress() {
        mKey = DeviceAction.JuRen.ACTION_PERM_PRESS;
        long recCount = mRecCount;
        mDelicatesDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
//                    if (mWashStatusEvent != null) {//不是设置模式，
//                        if (!mWashStatusEvent.isSetting()) {
//                            if (mWashStatusEvent.getIsWashing() == 0x30 && mWashStatusEvent.getWashMode() == 0x05) {//未开始洗衣 && 洗衣模式为热水
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            } else {
//                                if (mSendCount <= 3) {
//                                    mSendCount++;
//                                    sendDelicates();
//                                } else {//连续发3轮没收到新报文则判定指令发送失败
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionFail(mKey, "send delicates error");
//                                    }
//                                    dispose(mKey);
//                                }
//                            }
//                        } else {//设置模式
//                            if (mWashStatusEvent.getIsWashing() == 0x30) {//未开始洗衣
//                                if (mRecCount > recCount) {//收到新报文
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                } else {
//                                    if (mSendCount <= 3) {
//                                        mSendCount++;
//                                        sendDelicates();
//                                    } else {//连续发3轮没收到新报文则判定指令发送失败
//                                        if (mOnSendInstructionListener != null) {
//                                            mOnSendInstructionListener.sendInstructionFail(mKey, "send delicates error");
//                                        }
//                                        dispose(mKey);
//                                    }
//                                }
//                            } else {
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                }
//                                dispose(mKey);
//                            }
//                        }
//                    }
                })
                .subscribe();
    }

    public void sendSuper() {
        mKey = DeviceAction.JuRen.ACTION_SUPER;
        mSuperDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
//                    if (mWashStatusEvent != null) {
//                        if (mCount == 0) {
//                            mPreIsSupper = mWashStatusEvent.getLightSupper();
//                        } else {
//                            if (mPreIsSupper == 1) {
//                                if (mWashStatusEvent.getLightSupper() == 0) {
//                                    mPreIsSupper = mWashStatusEvent.getLightSupper();
//                                    mCount = 0;
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                }
//                            } else if (mPreIsSupper == 0) {
//                                if (mWashStatusEvent.getLightSupper() == 1) {
//                                    mPreIsSupper = mWashStatusEvent.getLightSupper();
//                                    mCount = 0;
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                }
//                            }
//                        }
//                        mCount++;
//                    }
                })
                .subscribe();
    }

    public void sendSetting() {
        mKey = DeviceAction.JuRen.ACTION_SETTING;
        mSuperDisposable = Observable.create(e -> {
            sendData(mKey);
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
//                    if (mWashStatusEvent != null) {
//                        if (mCount == 0) {
//                            mPreIsSupper = mWashStatusEvent.getLightSupper();
//                        } else {
//                            if (mPreIsSupper == 1) {
//                                if (mWashStatusEvent.getLightSupper() == 0) {
//                                    mPreIsSupper = mWashStatusEvent.getLightSupper();
//                                    mCount = 0;
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                }
//                            } else if (mPreIsSupper == 0) {
//                                if (mWashStatusEvent.getLightSupper() == 1) {
//                                    mPreIsSupper = mWashStatusEvent.getLightSupper();
//                                    mCount = 0;
//                                    if (mOnSendInstructionListener != null) {
//                                        mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                                    }
//                                    dispose(mKey);
//                                }
//                            }
//                        }
//                        mCount++;
//                    }
                })
                .subscribe();
    }


    public void sendKill() {
        Observable.create(e -> {
            kill();
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    private void kill() {

    }

    private void sendData(int key) {
        byte[] msg = {0x02, 0x06, (byte) (key & 0xff), (byte) (seq & 0xff), (byte) 0x80, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
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

    public boolean isOpen() {
        return _isOpen;
    }

    private void dispose(int key) {
        switch (key) {
            case DeviceAction.JuRen.ACTION_START:
                if (mStartDisposable != null && !mStartDisposable.isDisposed()) {
                    mStartDisposable.dispose();
                }
                break;
            case DeviceAction.JuRen.ACTION_WHITES:
                if (mHotDisposable != null && !mHotDisposable.isDisposed()) {
                    mHotDisposable.dispose();
                }
                break;
            case DeviceAction.JuRen.ACTION_COLORS:
                if (mWarmDisposable != null && !mWarmDisposable.isDisposed()) {
                    mWarmDisposable.dispose();
                }
                break;
            case DeviceAction.JuRen.ACTION_DELICATES:
                if (mColdDisposable != null && !mColdDisposable.isDisposed()) {
                    mColdDisposable.dispose();
                }
                break;
            case DeviceAction.JuRen.ACTION_PERM_PRESS:
                if (mDelicatesDisposable != null && !mDelicatesDisposable.isDisposed()) {
                    mDelicatesDisposable.dispose();
                }
                break;
            case DeviceAction.JuRen.ACTION_SUPER:
                if (mSuperDisposable != null && !mSuperDisposable.isDisposed()) {
                    mSuperDisposable.dispose();
                }
                break;
            case DeviceAction.JuRen.ACTION_SETTING:
                if (mSettingDisposable != null && !mSettingDisposable.isDisposed()) {
                    mSettingDisposable.dispose();
                }
                break;
        }
    }

    public void juRenOpen() throws SecurityException, IOException, InvalidParameterException {
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

    public void juRenClose() throws IOException {
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
    }

    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mOnSendInstructionListener = onSendInstructionListener;
    }

    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mCurrentStatusListener = currentStatusListener;
    }

    public void setOnSerialPortReadDataListener(SerialPortReadDataListener serialPortReadDataListener) {
        mSerialPortReadDataListener = serialPortReadDataListener;
    }
}
