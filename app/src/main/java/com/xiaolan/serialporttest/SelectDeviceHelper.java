package com.xiaolan.serialporttest;

import android.os.SystemClock;
import android.util.Log;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SelectDeviceHelper implements OnSendInstructionListener, SerialPortOnlineListener {
    private final String TAG = "SelectDeviceHelper";
    private boolean sendSuccess;

    public void checkSendSetting() {
        DeviceEngine.getInstance().selectDevice(0);
        DeviceEngine.getInstance().setOnSendInstructionListener(SelectDeviceHelper.this);
        DeviceEngine.getInstance().setOnSerialPortOnlineListener(this);
        try {
            DeviceEngine.getInstance().open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent) {
        sendSuccess = true;
        Log.e(TAG, "设置指令成功");
    }

    @Override
    public void sendInstructionFail(int key, String message) {

    }

    @Override
    public void onSerialPortOnline(byte[] bytes) {
        Log.e(TAG,"设备上线成功");
        try {
            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SETTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialPortOffline(String msg) {

    }
}
