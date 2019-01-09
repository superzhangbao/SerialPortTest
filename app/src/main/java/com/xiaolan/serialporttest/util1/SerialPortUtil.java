package com.xiaolan.serialporttest.util1;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;

public class SerialPortUtil {
    private static final String TAG = "SerialPortUtil";
    private SerialPort serialPort;
    public boolean serialPortStatus = false; //是否打开串口标志
    public boolean threadStatus; //线程状态，为了安全终止线程
    private InputStream inputStream;
    private OutputStream outputStream;
    public String data_;

    /**
     * 打开串口
     *
     * @return serialPort串口对象
     */
    public SerialPort openSerialPort() {
        String path = "/dev/ttyS3";
        try {
            serialPort = new SerialPort(new File(path), 9600, 0, "8", "1", "N");
            this.serialPortStatus = true;
            threadStatus = false; //线程状态

            //获取打开的串口中的输入输出流，以便于串口数据的收发
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();

            new ReadThread().start(); //开始线程监控是否有数据要接收
        } catch (IOException e) {
            Log.e(TAG, "openSerialPort: 打开串口异常：" + e.toString());
            return serialPort;
        }
        Log.e(TAG, "openSerialPort: 打开串口成功");
        return serialPort;
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        try {
            inputStream.close();
            outputStream.close();
            this.serialPortStatus = false;
            this.threadStatus = true; //线程状态
            serialPort.close();
        } catch (IOException e) {
            Log.e(TAG, "closeSerialPort: 关闭串口异常：" + e.toString());
            return;
        }
        Log.e(TAG, "closeSerialPort: 关闭串口成功");
    }

    /**
     * 发送串口指令（字符串）
     *
     * @param data String数据指令
     */
    public void sendSerialPort(String data) {
        Log.d(TAG, "sendSerialPort: 发送数据");

        try {
            byte[] sendData = data.getBytes(); //string转byte[]
//            this.data_ = new String(sendData); //byte[]转string
            if (sendData.length > 0) {
                outputStream.write(sendData);
                outputStream.write('\n');
                //outputStream.write('\r'+'\n');
                outputStream.flush();
                Log.e(TAG, "sendSerialPort: 串口数据发送成功");
            }
        } catch (IOException e) {
            Log.e(TAG, "sendSerialPort: 串口数据发送失败：" + e.toString());
        }

    }

    int pre = 0;
    int now = 0;

    /**
     * 单开一线程，来读数据
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            //判断进程是否在运行，更安全的结束进程
            StringBuffer stringBuffer = new StringBuffer();
            byte[] data = new byte[256];
            String s = "";
            while (!threadStatus) {
//                Log.d(TAG, "进入线程run");
                //64   1024
                byte[] buffer = new byte[256];
                int size; //读取数据的大小
                try {
                    size = inputStream.read(buffer);
                    if (size > 0) {
                        if (pre > 0) {
                            String str= new String(buffer, 0, size);
                            stringBuffer.append(new String(buffer, 0, size));
                            pre++;
                        } else {
                            Log.e(TAG, "run: 接收到了数据：" + new String(buffer, 0, size));
                            Log.e(TAG, "run: 接收到了数据大小：" + String.valueOf(size));
                            onDataReceiveListener.onDataReceive(buffer, size);
                            pre++;
                        }
                    } else {
                        pre = 0;
                    }
                    try {
                        Thread.sleep(10);//延时50ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "run: 数据读取异常：" + e.toString());
                }
            }

        }
    }

    //这是写了一监听器来监听接收数据
    public OnDataReceiveListener onDataReceiveListener = null;

    public static interface OnDataReceiveListener {
        public void onDataReceive(byte[] buffer, int size);
    }

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }
}
