package com.xiaolan.serialporttest;

import android.util.Log;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.utils.CRC16;
import com.xiaolan.serialporttest.util1.ToastUtil;
import com.xiaolan.serialporttest.wash.jurenpro.JuRenProWashStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class SelectDeviceHelper implements OnSendInstructionListener {
    private final String TAG = "SelectDeviceHelper";

    private int seq;
    private String sPort;
    private int iBaudRate;
    private byte[] msg1;
    private byte[] msg2;
    private byte[] msg3;
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private BufferedInputStream mBufferedInputStream;
    private BufferedOutputStream mBufferedOutputStream;
    private CRC16 mCrc16;
    private JuRenProWashStatus mJuRenProWashStatus;
    private boolean _isOpen;
//    private ReadThread mReadThread;
    private Disposable mSetting1Disposable;
    private int mKey;
    private boolean sendSuccess;
    private Disposable mSubscribe;
    private Disposable mSubscribe1;
    private Disposable mSubscribe2;

    public SelectDeviceHelper() {
//        this("/dev/ttyS3", 9600);
    }

    public SelectDeviceHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
//        //巨人Pro
//        msg1 = new byte[]{0x02, 0x06, 0, (byte) (seq & 0xff), (byte) 0x80, 0x20, 0, 1, 0, 0, 3};
//        //巨人plus
//        msg2 = new byte[]{0x02, 0x06, 0x00, 0x00, (byte) 0x80, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
//        //老版小精灵
//        msg3 = new byte[]{0x02, 0x06, 0, (byte) (seq & 0xff), (byte) 0x80, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
    }

//    public void opean() throws IOException {
//        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, "8", "1", "N");
//        mOutputStream = mSerialPort.getOutputStream();
//        mInputStream = mSerialPort.getInputStream();
//
//        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
//        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
//        mCrc16 = new CRC16();
//        mJuRenProWashStatus = new JuRenProWashStatus();
//        mReadThread = new ReadThread();
//        mReadThread.start();
//        _isOpen = true;
//    }

    public void checkSendSetting() {
        DeviceEngine.getInstance().selectDevice(0);
        try {
            DeviceEngine.getInstance().open();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DeviceEngine.getInstance().setOnSendInstructionListener(this);
        try {
            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SETTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSubscribe = Observable.intervalRange(1, 5, 1, 1, TimeUnit.SECONDS)
                .doOnNext(aLong -> {
                    if (sendSuccess) {
                        if (mSubscribe != null && !mSubscribe.isDisposed()) {
                            mSubscribe.dispose();
                            Log.e(TAG,"这是巨人pro洗衣机");
                            ToastUtil.show("这是巨人pro洗衣机");
                            DeviceEngine.getInstance().close();
                        }
                    }
                })
                .doOnComplete(() -> {
                    DeviceEngine.getInstance().selectDevice(2);
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    DeviceEngine.getInstance().setOnSendInstructionListener(SelectDeviceHelper.this);
                    try {
                        DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SETTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mSubscribe1 = Observable.intervalRange(1, 5, 1, 1, TimeUnit.SECONDS)
                            .doOnNext(aLong -> {
                                if (sendSuccess) {
                                    if (mSubscribe1 != null && !mSubscribe1.isDisposed()) {
                                        mSubscribe1.dispose();
                                        Log.e(TAG, "这是小精灵洗衣机");
                                        ToastUtil.show("这是小精灵洗衣机");
                                        DeviceEngine.getInstance().close();
                                    }
                                }
                            })
                            .doOnComplete(() -> {
                                DeviceEngine.getInstance().selectDevice(3);
                                try {
                                    DeviceEngine.getInstance().open();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                DeviceEngine.getInstance().setOnSendInstructionListener(SelectDeviceHelper.this);
                                try {
                                    DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SETTING);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                mSubscribe2 = Observable.intervalRange(1, 5, 1, 1, TimeUnit.SECONDS)
                                        .doOnNext(aLong -> {
                                            if (sendSuccess) {
                                                if (mSubscribe2 != null && !mSubscribe2.isDisposed()) {
                                                    mSubscribe2.dispose();
                                                    Log.e(TAG, "这是巨人Plus洗衣机");
                                                    ToastUtil.show("这是巨人Plus洗衣机");
                                                    DeviceEngine.getInstance().close();
                                                }
                                            }
                                        })
                                        .doOnComplete(() -> {
                                            Log.e(TAG, "初始化异常");
                                            ToastUtil.show("初始化异常");
                                        })
                                        .subscribe();
                            })
                            .subscribe();
                })
                .subscribe();
    }

    /*
     * 发送设置指令
     */
//    public synchronized void sendSetting() {
//        mKey = DeviceAction.JuRenPro.ACTION_SETTING;
//        ++seq;
//        mSetting1Disposable = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
//                .subscribeOn(Schedulers.io())
//                .doOnNext(aLong -> sendData(mKey))
//                .observeOn(AndroidSchedulers.mainThread())
//                .doAfterNext(aLong -> {
//                    if (aLong <12) {
//
//                        if (mWashStatusEvent.isSetting() && ("0").equals(mWashStatusEvent.getText())) {
//                            Log.e(TAG, "setting mode success");
//                            isSuccess.set(true);
//                            if (mOnSendInstructionListener != null) {
//                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.JuRenPro.ACTION_SETTING, mWashStatusEvent);
//                            }
//                        }
//                    }else {
//                        if (mSetting1Disposable != null && !mSetting1Disposable.isDisposed()) {
//                            mSetting1Disposable.dispose();
//                        }
//                    }
//                })
//                .onErrorResumeNext(throwable -> {
//                    return Observable.empty();
//                })
//                .subscribe();
//    }

//    private void sendData(int key) {
//        msg1[2] = (byte) (key & 0xff);
//        msg1[3] = (byte) (seq & 0xff);
//        short crc16_a = mCrc16.getCrc(msg1, 0, msg1.length - 3);
//        msg1[msg1.length - 2] = (byte) (crc16_a >> 8);
//        msg1[msg1.length - 3] = (byte) (crc16_a & 0xff);
//        try {
//            if (mBufferedOutputStream != null) {
//                mBufferedOutputStream.write(msg1);
//                mBufferedOutputStream.flush();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.e(TAG, "发送：" + MyFunc.ByteArrToHex(msg1));
//    }

    @Override
    public void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent) {
        sendSuccess = true;
        Log.e(TAG,"设置指令成功");
    }

    @Override
    public void sendInstructionFail(int key, String message) {

    }

//    class ReadThread extends Thread {
//        private final int DATA_LENGTH = 64;
//        @Override
//        public void run() {
//            super.run();
//            while (_isOpen) {
//                try {
//                    if (mBufferedInputStream == null)
//                        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
//                    byte[] buffer = new byte[DATA_LENGTH];
//                    int len;
//                    if (mBufferedInputStream.available() > 0) {
//                        try {
//                            sleep(50);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        len = mBufferedInputStream.read(buffer);
//                        if (len != -1) {
////                            Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
//                            if (len >= 30) {
//                                //读巨人洗衣机上报报文
//                            }
//                        }
//                    }
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                    Log.e(TAG, "run: 数据读取异常：" + t.toString());
//                    break;
//                }
//            }
//        }
//    }

//    public void close() throws IOException {
//        _isOpen = false;
//        if (mReadThread != null)
//            mReadThread.interrupt();
//        if (mSerialPort != null) {
//            mSerialPort.close();
//            mSerialPort = null;
//        }
//        if (mInputStream != null) {
//            mInputStream.close();
//            mInputStream = null;
//        }
//        if (mOutputStream != null) {
//            mOutputStream.close();
//            mOutputStream = null;
//        }
//        if (mBufferedInputStream != null) {
//            mBufferedInputStream.close();
//            mBufferedInputStream = null;
//        }
//        if (mBufferedOutputStream != null) {
//            mBufferedOutputStream.close();
//            mBufferedOutputStream = null;
//        }
//    }
}
