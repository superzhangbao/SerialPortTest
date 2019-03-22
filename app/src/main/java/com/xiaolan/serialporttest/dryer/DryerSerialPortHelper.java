package com.xiaolan.serialporttest.dryer;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngineIService;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
import com.xiaolan.serialporttest.mylib.utils.CRC16;
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

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class DryerSerialPortHelper implements DeviceEngineIService {
    private static final String TAG = "DryerSerialPortHelper";
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
    private boolean isOnline = false;
    private boolean hasOnline = false;
    private long mPreOnlineTime = 0;
    private long readThreadStartTime = 0;
    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortOnlineListener mSerialPortOnlineListener;
    private CurrentStatusListener mCurrentStatusListener;
    private Disposable mKill;
    private Disposable mHightDisposable;
    private Disposable mMedDisposable;
    private Disposable mLowDisposable;
    private Disposable mNoHeatDisposable;
    private Disposable mStartDisposable;
    private Disposable mSettingDisposable;
    private Disposable mResetDisposable;

    private int lamp_1;
    private int lamp_2;
    private int lamp_3;
    private int lamp_4;
    private int lamp_5;
    private int lamp_6;
    private int lamp_7;
    /*烘干机 空闲状态*/
    public static final int STATE_FREE = 0;
    /*烘干机 运行状态*/
    public static final int STATE_RUN = 1;
    /*烘干机 开门状态*/
    public static final int STATE_OPEN = 2;
    /*烘干机 结束状态*/
    public static final int STATE_END = 3;
    /*烘干机 未知状态*/
    public static final int STATE_UNKNOWN = 4;

    /*灯 不亮*/
    private static final int LAMP_CLOSE = 0;
    /*灯 闪烁*/
    private static final int LAMP_GLINT = 1;
    /*灯 长亮*/
    private static final int LAMP_BRIGHT = -1;
    /*灯 过度状态*/
    private static final int LAMP_NORMAL = -2;
    private int state;
    private StringBuilder showStr;
    private Map<Byte, String> mText;


    public DryerSerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

    public DryerSerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
        mText = LightMsg.getText();
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
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        len = mBufferedInputStream.read(buffer);
                        if (len != -1) {
                            Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
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
        while (off7F >= 0) {
            if (len >= off7F + size && buf[off7F + 1] == 0x70 && (buf[off7F + 2] + buf[off7F + 3] + buf[off7F + 4] + buf[off7F + 5]) != 0) {
                byte[] valid = ArrayUtils.subarray(buf, off7F, off7F + size);
                boolean isDiffWord = dryerMsg(valid);
                Log.e(TAG,"isDiffWord:"+isDiffWord);
                int stateNow = dryerState();
                Log.e(TAG,"state:"+stateNow);
                state = stateNow;
            }
            off7F = ArrayUtils.indexOf(buf, (byte) 0x7F, off7F + 1);
        }
    }

    private int dryerState() {
        if (TextUtils.equals(showStr.toString(), "End")) {
            //结束状态
            return STATE_END;
        } else if ((lamp_1 == LAMP_GLINT || lamp_2 == LAMP_GLINT)
                && lamp_3 == LAMP_BRIGHT
                && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)) {
            //运行状态
            return STATE_RUN;
        } else if (lamp_1 == LAMP_CLOSE
                && lamp_2 == LAMP_CLOSE
                && (lamp_4 != LAMP_CLOSE || lamp_5 != LAMP_CLOSE || lamp_6 != LAMP_CLOSE || lamp_7 != LAMP_CLOSE)) {
            //空闲状态
            return STATE_FREE;
        } else if ((lamp_1 == LAMP_BRIGHT || lamp_2 == LAMP_BRIGHT)
                && lamp_3 == LAMP_GLINT
                && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)
                && NumberUtils.isCreatable(showStr.toString())) {
            //开门状态
            return STATE_OPEN;
        } else {
            //未知状态
            return STATE_UNKNOWN;
        }
    }

    private boolean dryerMsg(@NonNull byte[] buf) {
        StringBuilder showWord = new StringBuilder();
        showWord.append(mText.get(buf[2])).append(mText.get(buf[3])).append(mText.get(buf[4])).append(mText.get(buf[5]));

        lamp_1 = lampState(buf[7]);
        lamp_2 = lampState(buf[8]);
        lamp_3 = lampState(buf[9]);
        lamp_4 = lampState(buf[10]);
        lamp_5 = lampState(buf[11]);
        lamp_6 = lampState(buf[12]);
        lamp_7 = lampState(buf[13]);

        boolean isDiff = !TextUtils.equals(showStr.toString(), showWord.toString());
        showStr.delete(0, showStr.length());
        showStr.append(showWord);

        return isDiff;
    }

    private int lampState(byte b) {
        if (b == 0x08) {
            return LAMP_BRIGHT;
        } else if (b == 0x00) {
            return LAMP_CLOSE;
        } else if (b == 0x01 || b == 0x07) {
            return LAMP_NORMAL;
        } else {
            return LAMP_GLINT;
        }
    }

    @Override
    public void open() throws IOException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, mDataBits, mStopBits, mParityBits);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();

        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
        mCrc16 = new CRC16();
        mReadThread = new ReadThread();
        mReadThread.start();
        readThreadStartTime = System.currentTimeMillis();
        _isOpen = true;
    }

    @Override
    public void setOnSendInstructionListener(OnSendInstructionListener onSendInstructionListener) {
        mOnSendInstructionListener = onSendInstructionListener;
    }

    @Override
    public void setOnCurrentStatusListener(CurrentStatusListener currentStatusListener) {
        mCurrentStatusListener = currentStatusListener;
    }

    @Override
    public void setOnSerialPortOnlineListener(SerialPortOnlineListener onSerialPortOnlineListener) {
        mSerialPortOnlineListener = onSerialPortOnlineListener;
    }

    public boolean isOpen() {
        return _isOpen;
    }

    @Override
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
        dispose(mKey);
    }

    @Override
    public void push(int action) throws IOException {

    }

    @Override
    public void pull() {

    }

    private void dispose(int key) {
        switch (key) {
            case DeviceAction.Dryer.ACTION_START:
                if (mStartDisposable != null && !mStartDisposable.isDisposed()) {
                    mStartDisposable.dispose();
                }
                break;
            case DeviceAction.Dryer.ACTION_HIGH:
                if (mHightDisposable != null && !mHightDisposable.isDisposed()) {
                    mHightDisposable.dispose();
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
            case DeviceAction.Dryer.ACTION_RESET:
                if (mResetDisposable != null && !mResetDisposable.isDisposed()) {
                    mResetDisposable.dispose();
                }
                break;
        }
        if (mKill != null && !mKill.isDisposed()) {
            mKill.dispose();
        }
    }
}
