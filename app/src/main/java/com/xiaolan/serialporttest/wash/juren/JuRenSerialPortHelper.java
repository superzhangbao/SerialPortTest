package com.xiaolan.serialporttest.wash.juren;

import android.os.SystemClock;
import android.util.Log;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
import com.xiaolan.serialporttest.mylib.utils.CRC16;
import com.xiaolan.serialporttest.mylib.utils.LightMsg;
import com.xiaolan.serialporttest.mylib.utils.MyFunc;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    private Disposable mKill;
    private Disposable mHotDisposable;
    private Disposable mWarmDisposable;
    private Disposable mColdDisposable;
    private Disposable mDelicatesDisposable;
    private Disposable mSuperDisposable;
    private Disposable mStartDisposable;
    private Disposable mSettingDisposable;

    private WashStatusEvent mWashStatusEvent;

    private final static int HIS_SIZE = 12;
    private List<byte[]> his = new LinkedList<>();
    private String text;
    private int light1 = 0;     //whites灯
    private int light2 = 0;     //colors灯
    private int light3 = 0;     //delicates灯
    private int light4 = 0;     //perm.press灯
    private int light5 = 0;     // 加强
    private int lightst = 0;    // 开始
    private int lights1 = 0;    // 第1步 洗涤灯
    private int lights2 = 0;    // 第2步 漂洗灯
    private int lights3 = 0;    // 第3步 脱水灯
    private int lightlk = 0;    // 锁    门锁灯
    private int lighttxt = 0;

    private boolean isOnline = false;
    private boolean hasOnline = false;
    private long mPreOnlineTime = 0;
    private long readThreadStartTime = 0;

    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortOnlineListener mSerialPortOnlineListener;
    private CurrentStatusListener mCurrentStatusListener;
    private JuRenWashStatus mJuRenWashStatus;
    private Map<Byte, String> mTextMap;
    private final long mPeriod = 200;

    public JuRenSerialPortHelper() {
        this("/dev/ttyS3", 9600);
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
                            Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
                            if (len >= 23) {
                                //读巨人洗衣机上报报文
                                JuRenWashRead(buffer, len);
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

    private void JuRenWashRead(byte[] buffer, int len) {
        //查找数组中是否有0x02
        int off02 = ArrayUtils.indexOf(buffer, (byte) 0x02);
        int size = 23;
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
                    short crc16_msg = (short) (buffer[off02 + 20] << 8 | (buffer[off02 + 21] & 0xff));
                    if (crc16_a == crc16_msg) {
                        byte[] msg = ArrayUtils.subarray(buffer, off02, off02 + size);
                        //判断串口是否掉线
                        if (!isOnline) {
                            isOnline = true;
                            hasOnline = true;
                            mPreOnlineTime = System.currentTimeMillis();
                            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                    .doOnComplete(() -> {
                                        if (mSerialPortOnlineListener != null) {
                                            mSerialPortOnlineListener.onSerialPortOnline(msg);
                                            Log.e(TAG, "串口上线");
                                        }
                                    }).subscribe();
                        } else {
                            mPreOnlineTime = System.currentTimeMillis();
                        }

                        if (his.isEmpty() || !Objects.deepEquals(msg, his.get(his.size() - 1))) {
                            Log.e(TAG, "接收：" + MyFunc.ByteArrToHex(msg));
                            //根据上报的报文，分析设备状态
                            mWashStatusEvent = mJuRenWashStatus.analyseStatus(msg);
                            mRecCount++;
                            Observable.just(1).observeOn(AndroidSchedulers.mainThread())
                                    .doOnNext(integer -> {
                                        if (mCurrentStatusListener != null) {
                                            mCurrentStatusListener.currentStatus(mWashStatusEvent);
                                        }
                                    })
                                    .subscribe();
                        }
                        enqueueState(msg);
                        if (his.size() > HIS_SIZE) dequeueState();
//                        Log.e("light", "isRunning:-->" + isRunning());
//                        Log.e("light", "light1灯的值：-->" + light1 + ",light1.isOn-->" + isOn(light1) + ",light1.isOff-->" + isOff(light1) + ",light1.isFlash-->" + isFlash(light1));
//                        Log.e("light", "light2灯的值：-->" + light2 + ",light2.isOn-->" + isOn(light2) + ",light2.isOff-->" + isOff(light2) + ",light2.isFlash-->" + isFlash(light2));
//                        Log.e("light", "light3灯的值：-->" + light3 + ",light3.isOn-->" + isOn(light3) + ",light3.isOff-->" + isOff(light3) + ",light3.isFlash-->" + isFlash(light3));
//                        Log.e("light", "light4灯的值：-->" + light4 + ",light4.isOn-->" + isOn(light4) + ",light4.isOff-->" + isOff(light4) + ",light4.isFlash-->" + isFlash(light4));
//                        Log.e("light", "light5灯的值：-->" + light5 + ",light5.isOn-->" + isOn(light5) + ",light5.isOff-->" + isOff(light5) + ",light5.isFlash-->" + isFlash(light5));
//                        Log.e("light", "lightst灯的值：-->" + lightst + ",lightst.isOn-->" + isOn(lightst) + ",lightst.isOff-->" + isOff(lightst) + ",lightst.isFlash-->" + isFlash(lightst));
//                        Log.e("light", "lights1灯的值：-->" + lights1 + ",lights1.isOn-->" + isOn(lights1) + ",lights1.isOff-->" + isOff(lights1) + ",lights1.isFlash-->" + isFlash(lights1));
//                        Log.e("light", "lights2灯的值：-->" + lights2 + ",lights2.isOn-->" + isOn(lights2) + ",lights2.isOff-->" + isOff(lights2) + ",lights2.isFlash-->" + isFlash(lights2));
//                        Log.e("light", "lights3灯的值：-->" + lights3 + ",lights3.isOn-->" + isOn(lights3) + ",lights3.isOff-->" + isOff(lights3) + ",lights3.isFlash-->" + isFlash(lights3));
//                        Log.e("light", "lightlk灯的值：-->" + lightlk + ",lightlk.isOn-->" + isOn(lightlk) + ",lightlk.isOff-->" + isOff(lightlk) + ",lightlk.isFlash-->" + isFlash(lightlk));
//                        Log.e("light", "lighttxt的值：-->" + lighttxt + ",lighttxt.isOn-->" + isOn(lighttxt) + ",lighttxt.isOff-->" + isOff(lighttxt) + ",lighttxt.isFlash-->" + isFlash(lighttxt));
//                        Log.e("light", "text:" + text);
                    } else {
                        //Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, off02 + 1)));
                    }
                } else {
//                    Log.e(TAG, "JuRenWashRead: 串口掉线");
                }
            }
        }
    }

    private void enqueueState(byte[] msg) {
        his.add(msg);

        int l1 = msg[5];
        int l2 = msg[10];
        light1 += (l2 & 0x20) >> 5;
        light2 += (l2 & 0x40) >> 6;
        light3 += (l1 & 0x04) >> 2;
        light4 += (l1 & 0x08) >> 3;
        light5 += (l1 & 0x10) >> 4;
        lightst += (l2 & 0x01);
        lights1 += (l2 & 0x02) >> 1;
        lights2 += (l2 & 0x04) >> 2;
        lights3 += (l2 & 0x08) >> 3;
        lightlk += (l2 & 0x10) >> 4;

        String t = mTextMap.get(msg[9]) + mTextMap.get(msg[8]) + mTextMap.get(msg[7]) + mTextMap.get(msg[6]);
        if (t.length() > 0) {
            this.text = t;
            lighttxt += 1;
        }
    }

    private void dequeueState() {
        if (his.isEmpty()) {
            return;
        }
        byte[] msg = his.remove(0);
        int l1 = msg[5];
        int l2 = msg[10];
        light1 -= (l2 & 0x20) >> 5;
        light2 -= (l2 & 0x40) >> 6;
        light3 -= (l1 & 0x04) >> 2;
        light4 -= (l1 & 0x08) >> 3;
        light5 -= (l1 & 0x10) >> 4;
        lightst -= (l2 & 0x01);
        lights1 -= (l2 & 0x02) >> 1;
        lights2 -= (l2 & 0x04) >> 2;
        lights3 -= (l2 & 0x08) >> 3;
        lightlk -= (l2 & 0x10) >> 4;
        String t = mTextMap.get(msg[9]) + mTextMap.get(msg[8]) + mTextMap.get(msg[7]) + mTextMap.get(msg[6]);
        if (t.length() > 0) {
            lighttxt -= 1;
        }
    }

    private boolean isRunning() {
        return isOn(lightst) && (isFlash(lights1) || isFlash(lights2) || isFlash(lights3));
    }

    private boolean isOn(int light) {
        return light > HIS_SIZE - 3;
    }

    private boolean isOff(int light) {
        return light < 3;
    }

    private boolean isFlash(int light) {
        return !isOn(light) && !isOff(light);
    }

    public void sendStart() {
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_START;
        ++seq;
        long recCount = mRecCount;
        mStartDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnComplete(() -> {
                    if (mRecCount > recCount) {//收到新报文
                        if (mOnSendInstructionListener != null) {
                            mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
                        }
                    }
                })
                .subscribe(aLong -> {
//                    sendData(mKey);
                    if (aLong <= 7) {
                        sendData(mKey);
                    } else {
                        sendEmptyData(mKey);
                    }
                });
//        long recCount = mRecCount;
//        mStartDisposable = Observable.create(e -> {
//            sendData(mKey);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnComplete(() -> {
//                    if (mRecCount > recCount) {//收到新报文
//                        if (mOnSendInstructionListener != null) {
//                            mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                        }
//                        dispose(mKey);
//                    } else {
//                        if (mSendCount <= 3) {
//                            mSendCount++;
//                            sendStart();
//                        } else {//连续发3轮没收到新报文则判定指令发送失败
//                            if (mOnSendInstructionListener != null) {
//                                mOnSendInstructionListener.sendInstructionFail(mKey, "send startOrStop error");
//                            }
//                            dispose(mKey);
//                        }
//                    }
//                })
//                .subscribe();
    }

    public void sendWhites() {
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_WHITES;
        ++seq;
        long recCount = mRecCount;
        mHotDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnComplete(() -> {
                })
                .subscribe(aLong -> {
//                    sendData(mKey);
                    if (aLong <= 7) {
                        sendData(mKey);
                    } else {
                        sendEmptyData(mKey);
                    }
                });
//        long recCount = mRecCount;
//        mHotDisposable = Observable.create(e -> {
//            sendData(mKey);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnComplete(() -> {
//                })
//                .subscribe();
    }

    public void sendColors() {
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_COLORS;
        ++seq;
        long recCount = mRecCount;
        mWarmDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnComplete(() -> {
                })
                .subscribe(aLong -> {
//                    sendData(mKey);
                    if (aLong <= 7) {
                        sendData(mKey);
                    } else {
                        sendEmptyData(mKey);
                    }
                });

//        long recCount = mRecCount;
//        mWarmDisposable = Observable.create(e -> {
//            sendData(mKey);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
//                .doOnComplete(() -> {
//                }).subscribe();
    }

    public void sendDelicates() {
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_DELICATES;
        ++seq;
        long recCount = mRecCount;
        mColdDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnComplete(() -> {
                })
                .subscribe(aLong -> {
//                    sendData(mKey);
                    if (aLong <= 7) {
                        sendData(mKey);
                    } else {
                        sendEmptyData(mKey);
                    }
                });
//        long recCount = mRecCount;
//        mColdDisposable = Observable.create(e -> {
//            sendData(mKey);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnComplete(() -> {
//                })
//                .subscribe();
    }

    public void sendPermPress() {
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_PERM_PRESS;
        ++seq;
        long recCount = mRecCount;
        mDelicatesDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnComplete(() -> {
                })
                .subscribe(aLong -> {
//                    sendData(mKey);
                    if (aLong <= 7) {
                        sendData(mKey);
                    } else {
                        sendEmptyData(mKey);
                    }
                });
//        long recCount = mRecCount;
//        mDelicatesDisposable = Observable.create(e -> {
//            sendData(mKey);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnComplete(() -> {
//                })
//                .subscribe();
    }

    public void sendSuper() {
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_SUPER;
        ++seq;
        mSuperDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnComplete(() -> {
                })
                .subscribe(aLong -> {
                    if (aLong == 0) {
                        sendData(mKey);
                    } else {
                        sendEmptyData(mKey);
                    }
                });
//        mSuperDisposable = Observable.create(e -> {
//            sendData(mKey);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnComplete(() -> {
//                })
//                .subscribe();
    }

    public void sendSetting() {
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_SETTING;
        ++seq;
        mSettingDisposable = Observable.interval(0, mPeriod, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnComplete(() -> {
//                    if (mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
//                        Log.e(TAG, "setting mode success");
//                        if (mOnSendInstructionListener != null) {
//                            mOnSendInstructionListener.sendInstructionSuccess(mKey, mWashStatusEvent);
//                        }
//                    } else {
//                        Log.e(TAG, "setting mode error");
//                        if (mOnSendInstructionListener != null) {
//                            mOnSendInstructionListener.sendInstructionFail(mKey, "setting mode error");
//                        }
//                    }
                })
                .subscribe(aLong -> {
                    if (aLong <= 7) {
                        sendData(mKey);
                    } else {
                        sendEmptyData(mKey);
                    }
                });
//        mSettingDisposable = Observable.create(e -> {
//            sendData(mKey);
//            e.onComplete();
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnComplete(() -> {
//                })
//                .subscribe();
    }


    public void sendKill() {
        //TODO 判断是处于洗衣状态才可以使用kill
        Observable.create(e -> {
            kill();
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .subscribe();
    }

    private void kill() throws IOException {

        int killIntervalCount = 16;
        dispose(mKey);
        mKey = DeviceAction.JuRen.ACTION_SETTING;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
//            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
//                Log.e(TAG, "kill step1 success");
//                break;
//            }
//            if (i == killIntervalCount - 1) {
//                Log.e(TAG, "kill step1 error");
//                break;
//            }
        }

        mKey = DeviceAction.JuRen.ACTION_COLORS;
        for (int i = 0; i < 3; i++) {
            ++seq;
            for (int j = 0; j < killIntervalCount; j++) {
                sendData(mKey);
                SystemClock.sleep(50);
//                if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
//                    Log.e(TAG, "kill step2->" + i + "success");
//                    break;
//                }
//                if (j == killIntervalCount - 1) {
//                    Log.e(TAG, "kill step2->" + i + "error");
//                    break;
//                }
            }
        }
        mKey = DeviceAction.JuRen.ACTION_START;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
//            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("LgC1").equals(mWashStatusEvent.getText())) {
//                Log.e(TAG, "kill step3 success");
//                break;
//            }
//            if (i == killIntervalCount - 1) {
//                Log.e(TAG, "kill step3 error");
//                break;
//            }
        }

        mKey = DeviceAction.JuRen.ACTION_COLORS;
        for (int i = 0; i < 2; i++) {
            ++seq;
            for (int j = 0; j < killIntervalCount; j++) {
                sendData(mKey);
                SystemClock.sleep(50);
            }
//            if (mWashStatusEvent != null && mWashStatusEvent.isSetting()) {
//                switch (i) {
//                    case 0:
//                        if (("tcL").equals(mWashStatusEvent.getText())) {
//                            Log.e(TAG, "kill step4->" + i + "success");
//                        } else {
//                            Log.e(TAG, "kill step4->" + i + "error");
//                        }
//                        break;
//                    case 1:
//                        if (("PA55").equals(mWashStatusEvent.getText())) {
//                            Log.e(TAG, "kill step4->" + i + "success");
//                        } else {
//                            Log.e(TAG, "kill step4->" + i + "error");
//                        }
//                        break;
//                    case 2:
//                        if (("dUCt").equals(mWashStatusEvent.getText())) {
//                            Log.e(TAG, "kill step4->" + i + "success");
//                        } else {
//                            Log.e(TAG, "kill step4->" + i + "error");
//                        }
//                        break;
//                    case 3:
//                        if (("h1LL").equals(mWashStatusEvent.getText())) {
//                            Log.e(TAG, "kill step4->" + i + "success");
//                        } else {
//                            Log.e(TAG, "kill step4->" + i + "error");
//                        }
//                        break;
//                }
//            } else {
//                Log.e(TAG, "kill step4->" + i + "error");
//            }
        }
        mKey = DeviceAction.JuRen.ACTION_START;
        ++seq;
        for (int i = 0; i < killIntervalCount; i++) {
            sendData(mKey);
            SystemClock.sleep(50);
//            if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
//                Log.e(TAG, "kill step5 success");
//                break;
//            }
//            if (i == killIntervalCount - 1) {
//                Log.e(TAG, "kill step5 error");
//                break;
//            }
        }
        mKey = DeviceAction.JuRen.ACTION_COLORS;
        for (int i = 0; i < 17; i++) {
            ++seq;
            for (int j = 0; j < killIntervalCount; j++) {
                sendData(mKey);
                SystemClock.sleep(50);
//                if (mWashStatusEvent != null && mWashStatusEvent.isSetting() && (i + 1 + "").equals(mWashStatusEvent.getText())) {
//                    Log.e(TAG, "kill step6_+" + i + "+ success");
//                    break;
//                }
//                if (j == killIntervalCount - 1) {
//                    Log.e(TAG, "kill step6_+" + i + "+ error");
//                    break;
//                }
            }
        }
        sendStart();
        mKill = Observable.intervalRange(0, 240, 0, 1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .doOnNext(aLong -> {
                    Log.e(TAG, "doOnNext--->" + aLong);
//                    if (mWashStatusEvent != null) {
//                        if (!mWashStatusEvent.isSetting() && mWashStatusEvent.getText().equals("PU5H")) {
//                            if (mOnSendInstructionListener != null) {
//                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRen.ACTION_KILL, mWashStatusEvent);
//                            }
//                            Log.e(TAG, "kill success");
//                            if (mKill != null && !mKill.isDisposed()) {
//                                mKill.dispose();
//                            }
//                        } else {
//                            if (aLong == 239) {
//                                if (mOnSendInstructionListener != null) {
//                                    mOnSendInstructionListener.sendInstructionFail(DeviceAction.JuRen.ACTION_KILL, "kill error");
//                                }
//                                Log.e(TAG, "kill error");
//                                if (mKill != null && !mKill.isDisposed()) {
//                                    mKill.dispose();
//                                }
//                            }
//                        }
//                    }
                })
                .doOnComplete(() -> {
                    mRecCount = 0;
                })
                .subscribe();

//        mKey = DeviceAction.JuRen.ACTION_SETTING;
//        sendData(mKey);
//        mKey = DeviceAction.JuRen.ACTION_COLORS;
//        for (int i = 0; i < 3; i++) {
//            sendData(mKey);
//        }
//        mKey = DeviceAction.JuRen.ACTION_START;
//        sendData(mKey);
//        mKey = DeviceAction.JuRen.ACTION_COLORS;
//        for (int i = 0; i < 2; i++) {
//            sendData(mKey);
//        }
//        mKey = DeviceAction.JuRen.ACTION_START;
//        sendData(mKey);
//        mKey = DeviceAction.JuRen.ACTION_COLORS;
//        for (int i = 0; i < 17; i++) {
//            sendData(mKey);
//        }
//        mKey = DeviceAction.JuRen.ACTION_START;
//        sendData(mKey);
    }

    private void sendData(int key) throws IOException {
        byte[] msg = {0x02, 0x06, (byte) (key), (byte) (seq), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        short crc16_a = mCrc16.getCrc(msg, 0, msg.length - 3);
        msg[msg.length - 2] = (byte) (crc16_a >> 8);
        msg[msg.length - 3] = (byte) (crc16_a & 0xff);
        if (mBufferedOutputStream != null) {
            mBufferedOutputStream.write(msg);
            mBufferedOutputStream.flush();
        }
        Log.e("send", "发送：" + MyFunc.ByteArrToHex(msg));
    }

//    private void sendData(int key) throws IOException {
//        byte[] msg = {0x02, 0x06, (byte) (key & 0xff), (byte) (seq & 0xff), (byte) 0x80, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
//        short crc16_a = mCrc16.getCrc(msg, 0, msg.length - 3);
//        msg[msg.length - 2] = (byte) (crc16_a >> 8);
//        msg[msg.length - 3] = (byte) (crc16_a & 0xff);
//        if (mBufferedOutputStream != null) {
//            mBufferedOutputStream.write(msg);
//            mBufferedOutputStream.flush();
//        }
//        Log.e("send", "发送：" + MyFunc.ByteArrToHex(msg));
//    }

    private void sendEmptyData(int key) throws IOException {
        byte[] msg = {0x02, 0x06, 0x00, (byte) (seq), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        short crc16_a = mCrc16.getCrc(msg, 0, msg.length - 3);
        msg[msg.length - 2] = (byte) (crc16_a >> 8);
        msg[msg.length - 3] = (byte) (crc16_a & 0xff);
        if (mBufferedOutputStream != null) {
            mBufferedOutputStream.write(msg);
            mBufferedOutputStream.flush();
        }
        Log.e("send", "发送：" + MyFunc.ByteArrToHex(msg));
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
        mJuRenWashStatus = new JuRenWashStatus();
        mReadThread = new ReadThread();
        mReadThread.start();
        _isOpen = true;
        mTextMap = LightMsg.getText();
    }

    void juRenClose() throws IOException {
        _isOpen = false;
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
