package com.xiaolan.serialporttest.mylib;

import android.util.Log;

import com.xiaolan.serialporttest.bean.ComBean;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Objects;

import android_serialport_api.SerialPort;

/**
 * 串口操作工具类
 */
public class SerialPortManager {
    private static final String TAG = "SerialPortManager";
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private SendThread mSendThread;
    private String sPort = "/dev/ttyS3";//默认串口号
    private int iBaudRate = 9600;//波特率
    private String mDataBits = "8";//数据位
    private String mStopBits = "1";//停止位
    private String mParityBits = "N";//校验位
    private boolean _isOpen = false;
    private int iDelay = 50;
    private BufferedInputStream mBufferedInputStream;
    private BufferedOutputStream mBufferedOutputStream;
    private CRC16 mCrc16;
    private static final int SHOW_LENGTH = 30;
    private byte[] mPreMsg = new byte[SHOW_LENGTH];
    private int seq;
    private int mKey;
    private boolean isKilling = false;//强制停止
    private String mText;
    private int mViewStep;
    private int mLights1;
    private int mLights2;
    private int mLights3;
    private int mLightsupper;
    private int mLightlock;
    private int mIsWashing;
    private int mErr;
    private int mMsgInt;
    private int mWashMode;//洗衣模式
    private static final int KEY_1 = 1;//开始
    private static final int KEY_2 = 2;//热水
    private static final int KEY_3 = 3;//温水
    private static final int KEY_4 = 4;//冷水
    private static final int KEY_5 = 5;//精致衣物
    private static final int KEY_6 = 6;//加强洗
    private static final int KEY_8 = 8;//setting
    private int mCount = 0;
    private int mPreIsSupper;


    public SerialPortManager() {
        this("/dev/ttyS3", 9600);
    }

    public SerialPortManager(String sPort) {
        this(sPort, 9600);
    }

    public SerialPortManager(String sPort, String sBaudRate) {
        this(sPort, Integer.parseInt(sBaudRate));
    }

    public SerialPortManager(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public void open() throws SecurityException, IOException, InvalidParameterException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0, mDataBits, mStopBits, mParityBits);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();

        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
        mBufferedOutputStream = new BufferedOutputStream(mOutputStream, 1024 * 64);
        mCrc16 = new CRC16();
        mReadThread = new ReadThread();
        mReadThread.start();
        mSendThread = new SendThread();
        mSendThread.setSuspendFlag();
        mSendThread.start();
        _isOpen = true;
    }

    public void close() {
        if (mReadThread != null)
            mReadThread.interrupt();
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
                mInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
                mOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mBufferedInputStream != null) {
            try {
                mBufferedInputStream.close();
                mBufferedInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mBufferedOutputStream != null) {
            try {
                mBufferedOutputStream.close();
                mBufferedOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        _isOpen = false;
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
            while (!mReadThread.isInterrupted()) {
                try {
                    if (mBufferedInputStream == null)
                        mBufferedInputStream = new BufferedInputStream(mInputStream, 1024 * 64);
                    byte[] buffer = new byte[DATA_LENGTH];
                    int len;
                    if (mBufferedInputStream.available() <= 0) {
                        continue;
                    } else {
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        len = mBufferedInputStream.read(buffer);
                        if (len != -1) {
                            //Log.e("buffer", "len:" + len + "值：" + MyFunc.ByteArrToHex(buffer));
                            if (len >= 30) {
                                //读巨人洗衣机上报报文
                                JuRenPlusWashRead(buffer, len);
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: 数据读取异常：" + e.toString());
                    break;
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
//                            Log.e(TAG, "false" + Arrays.toString(ArrayUtils.subarray(buffer, 0, off02 + 1)));
                            byte[] subarray = ArrayUtils.subarray(buffer, off02, off02 + size);
                            if (!Objects.deepEquals(mPreMsg, subarray)) {
                                Log.e(TAG, "接收：" + MyFunc.ByteArrToHex(subarray));
                                sendData(subarray);
                                mPreMsg = subarray;
                                //根据上报的报文，分析设备状态
                                setWashStatus(subarray);
                            }
                        }
                    }
                }
            }
        }

        private void sendData(byte[] bytes) {
            ComBean ComRecData = new ComBean(sPort, bytes, 0);
            if (mOnDataReceivedListener != null) {
                mOnDataReceivedListener.onDataReceived(ComRecData);
            }
        }
    }

    /**
     * 设置洗衣机状态
     */
    private void setWashStatus(byte[] msg) {
        if (msg.length == 30) {
            //洗衣模式
            mWashMode = (msg[2] & 0xf);
            //洗涤灯
            mLights1 = (msg[3] & 0x80) >> 7;
            //漂洗灯
            mLights2 = (msg[3] & 0x40) >> 6;
            //脱水灯
            mLights3 = (msg[3] & 0x20) >> 5;
            //是否加强洗
            mLightsupper = ((msg[21] & 0xf0) >> 6 > 0) ? 1 : 0;//1是 0否
            //门锁的状态
            mLightlock = (msg[23] & 0x01);//0 未锁  01 已锁
            //30  40可用来区分是否在洗涤
            mIsWashing = (msg[5] & 0xf0);//30未洗 40在洗  10设置模式
            //获取工作状态值 如msg[3] = E1,viewStep = 1  msg[3] = E6,viewStep = 6 viewStep = 2(暂停)
            mViewStep = (msg[3] & 0xf);
            //故障代码 比如02
            mErr = msg[4];
            //3C & 0xf = C;3D & 0xf = D
            mMsgInt = msg[6] & 0xf;
            //屏显(当msgInt = 0xC || 0xD || 0x3)的时候，屏幕显示的就是msg[7]的值
            if (mMsgInt == 0xc || mMsgInt == 0xd || mMsgInt == 0x3) {
                mText = "" + msg[7];
            } else {
                Map<Byte, String> textTable = LightMsg.getText();
                mText = textTable.get(msg[7]) + textTable.get(msg[8]) + textTable.get(msg[13]) + textTable.get(msg[14]);
            }
            Log.e(TAG, "lights1:" + mLights1 + "--" + "lights2:" + mLights2 + "--" + "lights3:" + mLights3 + "--" + "lightsupper:" + mLightsupper + "--" + "lightlock:" + mLightlock + "--"
                    + "mIsWashing:" + mIsWashing + "--" + "viewStep:" + mViewStep + "--" + "err:" + mErr + "--" + "msgInt:" + mMsgInt + "--" + "text:" + mText);
            //分析当前洗衣机的状态
            //1.判断当前是否是设置模式
            if (mIsWashing == 0x10) {//设置模式
                Log.e(TAG, "设置模式:" + "是 --->屏显：" + mText);
            } else {//正常模式
                Log.e(TAG, "设置模式:" + "否");
                //判断是否是错误状态
                if (isError()) {//是错误状态
                    Log.e(TAG, "错误状态:" + "是");
                    switch (mErr) {
                        case 0x02:
                            Log.e(TAG, "错误原因:" + "进水管故障");
                            break;
                        case 0x11:
                            Log.e(TAG, "错误原因:" + "关门失败");
                            break;
                    }
                } else {//不是错误状态
                    Log.e(TAG, "错误状态:" + "否");
                    if (mWashMode == 0x00) {
                        Log.e(TAG, "push状态" + "是");
                    }
                    //洗衣模式
                    switch (mWashMode) {
                        case 0x02:
                            Log.e(TAG, "洗衣模式：" + "热水");
                            break;
                        case 0x03:
                            Log.e(TAG, "洗衣模式：" + "温水");
                            break;
                        case 0x04:
                            Log.e(TAG, "洗衣模式：" + "冷水");
                            break;
                        case 0x05:
                            Log.e(TAG, "洗衣模式：" + "精致衣物");
                            break;
                    }
                    //是否是加强洗
                    if (mLightsupper == 1) {
                        Log.e(TAG, "加强洗：" + "是");
                    } else if (mLightsupper == 0) {
                        Log.e(TAG, "加强洗：" + "否");
                    }
                    if (mIsWashing == 0x30) {//洗衣状态
                        Log.e(TAG, "洗衣状态：" + "未开始洗衣");
                    } else if (mIsWashing == 0x40) {
                        if (mViewStep == 6) {
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "正在洗衣");
                        } else if (mViewStep == 2) {
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "暂停洗衣");
                        } else if (mViewStep == 1) {
                            Log.e(TAG, "洗衣状态：已开始洗衣--->" + "刚开始洗衣");
                        }
                    }
                    //是否是锁门状态
                    if (mLightlock == 0x01) {//是锁门状态
                        Log.e(TAG, "门锁状态：" + "已锁");
                    } else {//门未锁
                        Log.e(TAG, "门锁状态：" + "未锁");
                    }
                    //屏显
                    Log.e(TAG, "屏显：" + mText);
                }
            }
        }
    }

    private boolean isSettingMode() {
        return mIsWashing == 0x10;
    }

    private class SendThread extends Thread {
        private boolean suspendFlag = true;// 控制线程的执行

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                synchronized (this) {
                    while (suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!isKilling) {
                    sendData(mKey, 0);
                    stopSend();
                } else {
                    kill();
                    stopSend();
                }
                try {
                    Thread.sleep(iDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //线程暂停
        void setSuspendFlag() {
            this.suspendFlag = true;
        }

        //唤醒线程
        synchronized void setResume() {
            this.suspendFlag = false;
            notify();
        }
    }

    public void sendHot() {
        mKey = KEY_2;
        startSend();
    }

    public void sendWarm() {
        mKey = KEY_3;
        startSend();
    }

    public void sendCold() {
        mKey = KEY_4;
        startSend();
    }

    public void sendSoft() {
        mKey = KEY_5;
        startSend();
    }

    public void sendSuper() {
        mKey = KEY_6;
        startSend();
    }

    public void sendStartOrStop() {
        mKey = KEY_1;
        startSend();
    }

    public void sendSetting() {
        mKey = KEY_8;
        startSend();
    }

    public void sendKill() {
        isKilling = true;
        startSend();
    }

    /**
     * 执行kill过程
     */
    public void kill() {
        mKey = KEY_8;
        sendData(mKey, 0);
        if (isSettingMode() && mText.equals("0")) {
            Log.e(TAG, "kill step1 success");
        } else {
            Log.e(TAG, "kill step1 error");
        }
        mKey = KEY_3;
        for (int i = 0; i < 3; i++) {
            sendData(mKey, 0);
            if (isSettingMode()&& mText.equals(i + 1 + "")) {
                Log.e(TAG, "kill step2->" + i + "success");
            } else {
                Log.e(TAG, "kill step2->" + i + "error");
            }
        }
        mKey = KEY_1;
        sendData(mKey, 0);
        if (isSettingMode() && mText.equals("LgC1")) {
            Log.e(TAG, "kill step3 success");
        } else {
            Log.e(TAG, "kill step3 error");
        }
        mKey = KEY_3;
        for (int i = 0; i < 4; i++) {
            sendData(mKey, 0);
            if (isSettingMode()) {
                switch (i) {
                    case 0:
                        if (mText.equals("tcL")) {
                            Log.e(TAG, "kill step4->" + i + "success");
                        } else {
                            Log.e(TAG, "kill step4->" + i + "error");
                        }
                        break;
                    case 1:
                        if (mText.equals("PA55")) {
                            Log.e(TAG, "kill step4->" + i + "success");
                        } else {
                            Log.e(TAG, "kill step4->" + i + "error");
                        }
                        break;
                    case 2:
                        if (mText.equals("dUCt")) {
                            Log.e(TAG, "kill step4->" + i + "success");
                        } else {
                            Log.e(TAG, "kill step4->" + i + "error");
                        }
                        break;
                    case 3:
                        if (mText.equals("h1LL")) {
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
        if (isSettingMode() && mText.equals("0")) {
            Log.e(TAG, "kill step5 success");
        } else {
            Log.e(TAG, "kill step5 error");
        }
        mKey = KEY_3;
        for (int i = 0; i < 17; i++) {
            sendData(mKey, 0);
            if (isSettingMode() && mText.equals(i + 1 + "")) {
                Log.e(TAG, "kill step6 success");
            } else {
                Log.e(TAG, "kill step6 error");
            }
        }
        mKey = KEY_1;
        sendData(mKey, 0);
        if (!isSettingMode()) {
            Log.e(TAG, "kill success");
        }else {
            Log.e(TAG, "kill error");
        }
        isKilling = false;
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
            if (checkWashStatus()) {
                try {
                    mBufferedOutputStream.write(msg);
                    mBufferedOutputStream.flush();
                    if (mOnDataSendListener != null) {
                        ComBean comBean = new ComBean(sPort, msg, 1);
                        mOnDataSendListener.onDataSendSuccess(comBean);
                    }
                    Log.e(TAG, "发送：" + MyFunc.ByteArrToHex(msg));
                } catch (IOException e) {
                    e.printStackTrace();
                    if (mOnDataSendListener != null) {
                        mOnDataSendListener.onDataSendFail(e);
                    }
                }
            } else {
                break;
            }
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
                if (isError()) {
                    sendStartOrStop();
                }
                break;
            case KEY_3:
                if (isError()) {
                    sendStartOrStop();
                }
                break;
            case KEY_4:
                if (isError()) {
                    sendStartOrStop();
                }
                break;
            case KEY_5:
                if (isError()) {
                    sendStartOrStop();
                }
                break;
            case KEY_6://加强洗
                if (mCount == 0) {
                    mPreIsSupper = mLightsupper;
                } else {
                    if (mPreIsSupper == 1) {
                        if (mLightsupper == 0) {
                            mPreIsSupper = mLightsupper;
                            mCount = 0;
                            return false;
                        }
                    } else if (mPreIsSupper == 0) {
                        if (mLightsupper == 1) {
                            mPreIsSupper = mLightsupper;
                            mCount = 0;
                            return false;
                        }
                    }
                }
                mCount++;
                break;
            case KEY_8:
                if (isSettingMode() && mText.equals("0")) {
                    Log.e(TAG, "setting mode success");
                }else {
                    break;
                }
                break;
        }
        return true;
    }

    /**
     * 判断是否处于错误状态
     */
    private boolean isError() {
//        boolean b1 = mViewStep == 2;
        boolean b2 = mErr > 0;
//        return b1 || b2;
        return b2;
    }

    /**
     * 判断是否是闲置的
     */
    private boolean isIdle() {
        return !isRunning() && !isError();
    }

    /**
     * 判断是否处于运行状态
     */
    private boolean isRunning() {
        return (mViewStep == 6 || mViewStep == 7 || mViewStep == 8) && mErr == 0;
    }

    /**
     * 灯是否亮
     */
    private boolean isOn(int light) {
        return light > 0;
    }

    private boolean isOff(int light) {
        return light == 0;
    }

    public int getBaudRate() {
        return iBaudRate;
    }

    public boolean setBaudRate(int iBaud) {
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
            this.mDataBits = dataBits;
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
            this.mStopBits = stopBits;
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
            this.mParityBits = parityBits;
            return true;
        }
    }

    public boolean isOpen() {
        return _isOpen;
    }

    private void startSend() {
        if (mSendThread != null) {
            mSendThread.setResume();
        }
    }

    public void stopSend() {
        if (mSendThread != null) {
            mSendThread.setSuspendFlag();
        }
    }

    public interface OnDataReceivedListener {
        void onDataReceived(ComBean comBean);
    }

    private OnDataReceivedListener mOnDataReceivedListener;

    public void setOnDataReceivedListener(OnDataReceivedListener onDataReceivedListener) {
        mOnDataReceivedListener = onDataReceivedListener;
    }

    public interface OnDataSendListener {
        void onDataSendSuccess(ComBean comBean);

        void onDataSendFail(IOException e);
    }

    private OnDataSendListener mOnDataSendListener;

    public void setOnDataSendListener(OnDataSendListener onDataSendListener) {
        mOnDataSendListener = onDataSendListener;
    }

    public int getiDelay() {
        return iDelay;
    }

    public void setiDelay(int iDelay) {
        this.iDelay = iDelay;
    }
}
