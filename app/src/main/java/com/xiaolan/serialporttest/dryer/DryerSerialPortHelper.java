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

import android_serialport_api.SerialPort;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
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
    //    private static final int SHOW_LENGTH = 30;
//    private byte[] mPreMsg = new byte[SHOW_LENGTH];
    private int seq = 1;
    //    private int mKey;
    private boolean isOnline = false;
    private boolean hasOnline = false;
    private long mPreOnlineTime = 0;
    private long readThreadStartTime = 0;
    private OnSendInstructionListener mOnSendInstructionListener;
    private SerialPortOnlineListener mSerialPortOnlineListener;
    private CurrentStatusListener mCurrentStatusListener;

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
    private long click = 0;
    private Disposable mInitSetDisposable;
    private Disposable mPushDisposable;
    private Disposable mKillDisposable;


    public DryerSerialPortHelper() {
        this("/dev/ttyS3", 9600);
    }

    public DryerSerialPortHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
        mText = LightMsg.getText();
        showStr = new StringBuilder();
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
        Log.e(TAG, "off7F:" + off7F);
        while (off7F >= 0) {
            if (len >= off7F + size && buf[off7F + 1] == 0x70 && (buf[off7F + 2] + buf[off7F + 3] + buf[off7F + 4] + buf[off7F + 5]) != 0) {
                byte[] valid = ArrayUtils.subarray(buf, off7F, off7F + size);
                boolean isDiffWord = dryerMsg(valid);
                Log.e(TAG, "isDiffWord:" + isDiffWord);
                state = dryerState();
                Log.e(TAG, "state:" + state);
                if (mCurrentStatusListener != null) {
                    mCurrentStatusListener.currentStatus(state);
                }
            }
            off7F = ArrayUtils.indexOf(buf, (byte) 0x7F, off7F + 1);
            Log.e(TAG, "off7F:" + off7F);
        }
    }

    /**
     * 分析烘干机状态
     *
     * @return 状态
     */
    private int dryerState() {
        if (TextUtils.equals(showStr.toString(), "End")) {
            //结束状态
            return STATE_END;
        } else if ((lamp_1 == LAMP_GLINT || lamp_2 == LAMP_GLINT)//灯 闪烁
                && lamp_3 == LAMP_BRIGHT//灯 长亮
                && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)) {//灯 长亮
            //运行状态
            return STATE_RUN;
        } else if (lamp_1 == LAMP_CLOSE//灯 不亮
                && lamp_2 == LAMP_CLOSE//灯 不亮
                && (lamp_4 != LAMP_CLOSE || lamp_5 != LAMP_CLOSE || lamp_6 != LAMP_CLOSE || lamp_7 != LAMP_CLOSE)) {
            //空闲状态
            return STATE_FREE;
        } else if ((lamp_1 == LAMP_BRIGHT || lamp_2 == LAMP_BRIGHT)//灯 长亮
                && lamp_3 == LAMP_GLINT//灯 闪烁
                && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)//灯 长亮
                && NumberUtils.isCreatable(showStr.toString())) {
            //开门状态
            return STATE_OPEN;
        } else {
            //未知状态
            return STATE_UNKNOWN;
        }
    }

    /**
     * 分析烘干机屏幕内容及灯的状态
     *
     * @param buf 字节数组
     * @return 屏幕显示是否变化
     */
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
            return LAMP_BRIGHT;//灯 长亮
        } else if (b == 0x00) {
            return LAMP_CLOSE;//灯 不亮
        } else if (b == 0x01 || b == 0x07) {
            return LAMP_NORMAL;//灯 过度状态
        } else {
            return LAMP_GLINT;//灯 闪烁
        }
    }

    private void kill() {
        Log.e(TAG, "kill: 开始");
        synchronized (DryerSerialPortHelper.class) {
            mKillDisposable = Observable.just(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(throwable -> {
                        return Observable.empty();
                    })
                    .subscribe(integer -> {
                        click = 0;
                        for (; ; ) {
                            do {
                                writeOne(DeviceAction.Dryer.ACTION_HIGH);
//                    readAll();
                            } while (!NumberUtils.isCreatable(showStr.toString()));

                            writeOne(DeviceAction.Dryer.ACTION_SETTING);
                            for (int i = 0; i < 3; i++) {
                                writeOne(DeviceAction.Dryer.ACTION_MED);
                            }
                            writeOne(DeviceAction.Dryer.ACTION_START);
//                readAll();
                            if (!TextUtils.equals(showStr.toString(), "LgC1")) {
                                continue;
                            }

                            do {
                                writeOne(DeviceAction.Dryer.ACTION_MED);
//                    readAll();
                            } while (!TextUtils.equals(showStr.toString(), "h1LL"));

                            writeOne(DeviceAction.Dryer.ACTION_START);
                            for (int i = 0; i < 17; i++) {
                                writeOne(DeviceAction.Dryer.ACTION_MED);
                            }
                            for (Integer v; ; ) {
//                    readAll();
                                try {
                                    v = Integer.parseInt(showStr.toString());
                                } catch (Exception ex) {
                                    break;
                                }
                                if (v > 17) {
                                    writeOne(DeviceAction.Dryer.ACTION_LOW);
                                } else if (v < 17) {
                                    writeOne(DeviceAction.Dryer.ACTION_MED);
                                } else {
                                    writeOne(DeviceAction.Dryer.ACTION_START);
                                }
                            }
//                readAll();
                            if (TextUtils.equals(showStr.toString(), "End")) {
                                Log.e(TAG, "kill: 成功");
                                Observable.just(1)
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .doOnNext(integer1 -> {
                                            if (mOnSendInstructionListener != null) {
                                                mOnSendInstructionListener.sendInstructionSuccess(DeviceAction.Dryer.ACTION_KILL, null);
                                            }
                                            if (mKillDisposable != null && !mKillDisposable.isDisposed()) {
                                                mKillDisposable.dispose();
                                            }
                                        })
                                        .subscribe();
                                return;
                            }
                        }
                    });

        }
    }

    @Override
    public void push(int action, int coin) {
        synchronized (DryerSerialPortHelper.class) {
            switch (action) {
                case DeviceAction.Dryer.ACTION_KILL:
                    kill();
                    break;
                case DeviceAction.Dryer.ACTION_INIT:
                    initSet();
                    break;
                default:
                    mPushDisposable = Observable.just(1)
                            .subscribeOn(Schedulers.io())
                            .onErrorResumeNext(throwable -> {
                                return Observable.empty();
                            })
                            .subscribe(integer -> makePush(action, coin));
                    break;
            }
        }
    }

    private void makePush(int action, int coin) throws IOException, InterruptedException {
        if (state != STATE_FREE) return;
        coin = coin > 9 ? 9 : coin;
        String time = String.valueOf(coin * 10 / 60 * 100 + coin * 10 % 60);
        click = 0;
        int value;
        for (; ; ) {
            for (; ; ) {
//                    readAll();
                try {
                    value = Integer.parseInt(showStr.toString());
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
//                    readAll();
                if ((lamp_1 == LAMP_GLINT || lamp_2 == LAMP_GLINT)//灯 闪烁
                        && lamp_3 == LAMP_BRIGHT//灯 长亮
                        && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)) {
                    return;
                } else if (lamp_1 == LAMP_BRIGHT//灯 长亮
                        && lamp_2 == LAMP_BRIGHT
                        && (lamp_4 == LAMP_BRIGHT || lamp_5 == LAMP_BRIGHT || lamp_6 == LAMP_BRIGHT || lamp_7 == LAMP_BRIGHT)
                        && TextUtils.equals(showStr.toString(), time)) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    Thread.sleep(800);
                } else if (TextUtils.equals(showStr.toString(), time)) {
                    writeOne(action);
                }
            }
        }
    }

    private void coin() throws IOException {
        if (++click > MAX_CLICK) {
            throw new IOException("click error");
        }
        byte[] strByte = new byte[]{0x7F, (byte) (seq & 0xff), (byte) 0xF1, 0x00};
        for (int i = 0; i < 12; ++i) {
            mBufferedOutputStream.write(strByte);
            mBufferedOutputStream.flush();
            SystemClock.sleep(100);
        }
        ++seq;
    }

    private void writeOne(int key) throws IOException, InterruptedException {
        if (state != STATE_OPEN && ++click > MAX_CLICK) {
            throw new IOException("click error");
        }
        byte[] strByte = new byte[]{0x7F, (byte) (seq & 0xff), (byte) 0xF0, (byte) (key & 0xff)};
        for (int i = 0; i < 12; ++i) {
            mBufferedOutputStream.write(strByte);
            mBufferedOutputStream.flush();
            Thread.sleep(100);
        }
        ++seq;
    }

    /**
     * 初始化烘干机
     */
    private void initSet() {
        synchronized (DeviceEngineDryerService.class) {
            click = -500;
            mInitSetDisposable = Observable.just(1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext(throwable -> {
                        return Observable.empty();
                    })
                    .subscribe(integer -> {
                        mode();
                        price();
                        timeUnit();
                        timeDefault();
                        coin1();
                        coin2();
                    });
        }
    }

    private void mode() throws IOException, InterruptedException {
        Log.e(TAG, "mode: 开始设置");
        for (; ; ) {
            do {
                writeOne(DeviceAction.Dryer.ACTION_HIGH);
//                readAll();
            } while (!NumberUtils.isCreatable(showStr.toString()));

            writeOne(DeviceAction.Dryer.ACTION_SETTING);
            for (int i = 0; i < 3; i++) {
                writeOne(DeviceAction.Dryer.ACTION_MED);
            }
            writeOne(DeviceAction.Dryer.ACTION_START);
//            readAll();
            if (!TextUtils.equals(showStr.toString(), "LgC1")) {
                continue;
            }

            for (; ; ) {
                if (TextUtils.equals(showStr.toString(), "LgC1")) {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
                } else if (TextUtils.equals(showStr.toString(), "tE5t")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "5tUP")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                } else if (TextUtils.equals(showStr.toString(), "r9Pr")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "PdtP")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                } else if (TextUtils.equals(showStr.toString(), "FrEE")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "PA4")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
                }
//                readAll();
                if (NumberUtils.isCreatable(showStr.toString())
                        && lamp_1 == LAMP_CLOSE
                        && lamp_2 == LAMP_CLOSE
                        && lamp_3 == LAMP_CLOSE) {
                    Log.e(TAG, "mode: 设置完成");
                    return;
                }
            }
        }
    }

    private void price() throws IOException, InterruptedException {
        Log.e(TAG, "price: 开始设置");
        for (; ; ) {
            writeOne(DeviceAction.Dryer.ACTION_SETTING);
            for (int i = 0; i < 3; i++) {
                writeOne(DeviceAction.Dryer.ACTION_MED);
            }
            writeOne(DeviceAction.Dryer.ACTION_START);
//            readAll();
            if (!TextUtils.equals(showStr.toString(), "LgC1")) {
                continue;
            }

            do {
                if (TextUtils.equals(showStr.toString(), "LgC1")) {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
                } else if (TextUtils.equals(showStr.toString(), "tE5t")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "5tUP")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                } else if (TextUtils.equals(showStr.toString(), "r9Pr")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
                }
//                readAll();
            } while (!NumberUtils.isCreatable(showStr.toString()));


            for (Integer v; ; ) {
//                readAll();
                try {
                    v = Integer.parseInt(showStr.toString());
                } catch (Exception ex) {
                    break;
                }
                if (v < 100) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (v > 100) {
                    writeOne(DeviceAction.Dryer.ACTION_LOW);
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    Log.e(TAG, "price: 设置完成");
                    return;
                }
            }
        }
    }

    private void timeUnit() throws IOException, InterruptedException {
        Log.e(TAG, "timeUnit: 开始设置");
        for (; ; ) {
            for (; ; ) {
                if (!TextUtils.equals(showStr.toString(), "5tUP")) {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
//                    readAll();
                } else {
                    break;
                }
            }

            writeOne(DeviceAction.Dryer.ACTION_START);
            for (; ; ) {
//                readAll();
                if (TextUtils.equals(showStr.toString(), "r9Pr")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "toPt")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                }
            }

            for (; ; ) {
//                readAll();
                int value;
                try {
                    value = Integer.parseInt(showStr.toString());
                } catch (Exception ex) {
                    break;
                }
                if (value < 10) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (value > 10) {
                    writeOne(DeviceAction.Dryer.ACTION_LOW);
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                }
            }

//            readAll();
            if (TextUtils.equals(showStr.toString(), "bEEP")) {
                Log.e(TAG, "timeUnit: 设置完成");
                return;
            }
        }
    }

    private void timeDefault() throws IOException, InterruptedException {
        Log.e(TAG, "timeDefault: 设置开始");
        for (; ; ) {
            for (; ; ) {
                if (!TextUtils.equals(showStr.toString(), "5tUP")) {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
//                    readAll();
                } else {
                    break;
                }
            }

            writeOne(DeviceAction.Dryer.ACTION_START);
            for (; ; ) {
//                readAll();
                if (TextUtils.equals(showStr.toString(), "r9Pr")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "rPdC")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                }
            }

            for (; ; ) {
//                readAll();
                int value;
                try {
                    value = Integer.parseInt(showStr.toString());
                } catch (Exception ex) {
                    break;
                }
                if (value < 10) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (value > 10) {
                    writeOne(DeviceAction.Dryer.ACTION_LOW);
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                }
            }

//            readAll();
            if (TextUtils.equals(showStr.toString(), "5PdC")) {
                Log.e(TAG, "timeDefault: 设置完成");
                return;
            }
        }
    }

    private void coin1() throws IOException, InterruptedException {
        Log.e(TAG, "coin1: 设置开始");
        for (; ; ) {
            for (; ; ) {
                if (!TextUtils.equals(showStr.toString(), "5tUP")) {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
//                    readAll();
                } else {
                    break;
                }
            }

            writeOne(DeviceAction.Dryer.ACTION_START);
            for (; ; ) {
//                readAll();
                if (TextUtils.equals(showStr.toString(), "r9Pr")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "Co1")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                }
            }

            for (; ; ) {
//                readAll();
                int value;
                try {
                    value = Integer.parseInt(showStr.toString());
                    if (value < 10) {
                        value *= 100;
                    }
                } catch (Exception ex) {
                    break;
                }
                if (value < 100) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (value > 100) {
                    writeOne(DeviceAction.Dryer.ACTION_LOW);
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                }
            }

//            readAll();
            if (TextUtils.equals(showStr.toString(), "Co2")) {
                Log.e(TAG, "coin1: 设置完成");
                return;
            }
        }
    }

    private void coin2() throws IOException, InterruptedException {
        Log.e(TAG, "coin2: 设置开始");
        for (; ; ) {
            for (; ; ) {
                if (lamp_1 == LAMP_CLOSE
                        && lamp_2 == LAMP_CLOSE
                        && lamp_3 == LAMP_CLOSE
                        && (TextUtils.equals(showStr.toString(), "1") || TextUtils.equals(showStr.toString(), "100"))) {
                    Log.e(TAG, "coin2: 设置完成");
                    return;
                } else if (!TextUtils.equals(showStr.toString(), "5tUP")) {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
//                    readAll();
                } else {
                    break;
                }
            }

            writeOne(DeviceAction.Dryer.ACTION_START);
            for (; ; ) {
//                readAll();
                if (TextUtils.equals(showStr.toString(), "r9Pr")) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (TextUtils.equals(showStr.toString(), "Co2")) {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                }
            }

            for (; ; ) {
//                readAll();
                int value;
                try {
                    value = Integer.parseInt(showStr.toString());
                    if (value < 10) {
                        value *= 100;
                    }
                } catch (Exception ex) {
                    break;
                }
                if (value < 100) {
                    writeOne(DeviceAction.Dryer.ACTION_MED);
                } else if (value > 100) {
                    writeOne(DeviceAction.Dryer.ACTION_LOW);
                } else {
                    writeOne(DeviceAction.Dryer.ACTION_START);
                    break;
                }
            }

//            readAll();
            if (TextUtils.equals(showStr.toString(), "P1Po")) {
                for (; ; ) {
                    writeOne(DeviceAction.Dryer.ACTION_HIGH);
//                    readAll();
                    if (TextUtils.equals(showStr.toString(), "5tUP")) {
                        writeOne(DeviceAction.Dryer.ACTION_HIGH);
                        Log.e(TAG, "coin2: 设置完成");
//                        readAll();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void open() throws IOException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, "8",  "1", "N");
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
        dispose();
    }

    private void dispose() {
        if (mKillDisposable != null && !mKillDisposable.isDisposed()) {
            mKillDisposable.dispose();
        }
        if (mInitSetDisposable != null && !mInitSetDisposable.isDisposed()) {
            mInitSetDisposable.dispose();
        }
        if (mPushDisposable != null && !mPushDisposable.isDisposed()) {
            mPushDisposable.dispose();
        }
    }
}
