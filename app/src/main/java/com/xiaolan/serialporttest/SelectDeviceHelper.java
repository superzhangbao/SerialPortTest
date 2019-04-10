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
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SelectDeviceHelper implements OnSendInstructionListener {
    private final String TAG = "SelectDeviceHelper";
    private boolean sendSuccess;
    private Disposable subscribe;

    private OnCheckDeviceListener onCheckDeviceListener;
    public interface OnCheckDeviceListener {
        void  device(int type);
    }

    public void setOnCheckDeviceListener(OnCheckDeviceListener onCheckDeviceListener) {
        this.onCheckDeviceListener = onCheckDeviceListener;
    }

    public void checkSendSetting() {
        subscribe = Observable.intervalRange(1, 4, 0, 4, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterNext(aLong -> {
                    if (aLong == 1) {
                        checkOne(0);
                    }else if (aLong == 2) {
                        if (sendSuccess) {
                            subscribe.dispose();
                            Log.e(TAG,"这台洗衣机是：巨人pro");
                            if (onCheckDeviceListener!= null) {
                                onCheckDeviceListener.device(0);
                            }
                        }else {
                            checkOne(2);
                        }
                    }else if (aLong == 3){
                        if (sendSuccess) {
                            subscribe.dispose();
                            Log.e(TAG,"这台洗衣机是：小精灵");
                            if (onCheckDeviceListener!= null) {
                                onCheckDeviceListener.device(2);
                            }
                        }else {
                            checkOne(3);
                        }
                    }else {
                        if (sendSuccess) {
                            subscribe.dispose();
                            Log.e(TAG,"这台洗衣机是：巨人plus");
                            if (onCheckDeviceListener!= null) {
                                onCheckDeviceListener.device(3);
                            }
                        }else{
                            Log.e(TAG,"这台洗衣机是：未知");
                        }
                    }
                }).subscribe();
    }

    private void checkOne(int i) {
        DeviceEngine.getInstance().selectDevice(i);
        DeviceEngine.getInstance().setOnSendInstructionListener(SelectDeviceHelper.this);
        try {
            DeviceEngine.getInstance().open();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "open 报错");
        }
        Observable.intervalRange(1,3,1,1,TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> {
                    try {
                        DeviceEngine.getInstance().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    switch (i) {
                        case 0:
                            Log.e(TAG,"close 巨人Pro");
                            break;
                        case 2:
                            Log.e(TAG,"close 小精灵");
                            break;
                        case 3:
                            Log.e(TAG,"close 巨人Plus");
                            break;
                    }
                })
                .doAfterNext(integer -> {
                    if (!sendSuccess) {
                        switch (i) {
                            case 0:
                                Log.e(TAG,"push(DeviceAction.JuRenPro.ACTION_SETTING)");
                                try {
                                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SETTING);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 2:
                                Log.e(TAG,"push(DeviceAction.Xjl.ACTION_SETTING)");
                                try {
                                    DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SETTING);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 3:
                                Log.e(TAG,"push(DeviceAction.JuRenPlus.ACTION_SETTING)");
                                try {
                                    DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SETTING);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                    }
                })
                .subscribe();
    }

    @Override
    public void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent) {
        sendSuccess = true;
        Log.e(TAG, "设置指令成功");
    }

    @Override
    public void sendInstructionFail(int key, String message) {

    }
}
