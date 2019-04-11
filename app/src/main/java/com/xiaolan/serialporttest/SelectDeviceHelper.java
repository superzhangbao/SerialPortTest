package com.xiaolan.serialporttest;

import android.util.Log;

import com.xiaolan.serialporttest.dryer.DryerStatus;
import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * 自动选择设备
 */
public class SelectDeviceHelper implements OnSendInstructionListener {
    private final String TAG = "SelectDeviceHelper";
    private boolean sendSuccess;
    private int mCount;
    private boolean isFinisned;

    private OnCheckDeviceListener onCheckDeviceListener;


    public interface OnCheckDeviceListener {
        void device(int type);
    }

    public interface OnCheckDeviceFinishListener {
        void checkFinish();

        void checkUnFinish();
    }


    public void setOnCheckDeviceListener(OnCheckDeviceListener onCheckDeviceListener) {
        this.onCheckDeviceListener = onCheckDeviceListener;
    }

    public void checkSendSetting() {
        checkOne(0, new OnCheckDeviceFinishListener() {
            @Override
            public void checkFinish() {
                DeviceEngine.getInstance().selectDevice(0);
                if (onCheckDeviceListener != null) {
                    onCheckDeviceListener.device(0);
                }
                Log.e(TAG, "这台设备是：巨人pro洗衣机");
            }

            @Override
            public void checkUnFinish() {
                checkOne(2, new OnCheckDeviceFinishListener() {
                    @Override
                    public void checkFinish() {
                        DeviceEngine.getInstance().selectDevice(2);
                        if (onCheckDeviceListener != null) {
                            onCheckDeviceListener.device(2);
                        }
                        Log.e(TAG, "这台设备是：小精灵洗衣机");
                    }

                    @Override
                    public void checkUnFinish() {
                        checkOne(3, new OnCheckDeviceFinishListener() {
                            @Override
                            public void checkFinish() {
                                DeviceEngine.getInstance().selectDevice(3);
                                if (onCheckDeviceListener != null) {
                                    onCheckDeviceListener.device(3);
                                }
                                Log.e(TAG, "这台设备是：巨人plus洗衣机");
                            }

                            @Override
                            public void checkUnFinish() {
                                checkOne(-1, new OnCheckDeviceFinishListener() {
                                    @Override
                                    public void checkFinish() {
                                        DeviceEngine.getInstance().selectDevice(-1);
                                        if (onCheckDeviceListener != null) {
                                            onCheckDeviceListener.device(-1);
                                        }
                                        Log.e(TAG, "这台设备是：烘干机");
                                    }

                                    @Override
                                    public void checkUnFinish() {
                                        if (onCheckDeviceListener != null) {
                                            onCheckDeviceListener.device(-100);
                                        }
                                        Log.e(TAG, "这台设备是：未知");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void checkOne(int i, OnCheckDeviceFinishListener onCheckDeviceFinishListener) {
        DeviceEngine.getInstance().selectDevice(i);
        DeviceEngine.getInstance().setOnSendInstructionListener(SelectDeviceHelper.this);
        try {
            DeviceEngine.getInstance().open();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "open 报错");
        }


//        Observable.just(1)
//                .observeOn(AndroidSchedulers.mainThread())
//                .repeatWhen(objectObservable -> objectObservable.flatMap((Function<Object, ObservableSource<?>>) o -> {
//                    mCount++;
//                    if (mCount > 2 && !sendSuccess) {
//                        try {
//                            DeviceEngine.getInstance().close();
//                            switch (i) {
//                                case 0:
//                                    Log.e(TAG, "close 巨人Pro");
//                                    break;
//                                case 2:
//                                    Log.e(TAG, "close 小精灵");
//                                    break;
//                                case 3:
//                                    Log.e(TAG, "close 巨人Plus");
//                                    break;
//                                case -1:
//                                    Log.e(TAG, "close 烘干机");
//                                    break;
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        onCheckDeviceFinishListener.checkUnFinish();
//                        return Observable.empty();
//                    } else if (sendSuccess) {
////                        onCheckDeviceFinishListener.checkFinish();
//                        return Observable.empty();
//                    } else {
//                        return Observable.just(1).delay(1600, TimeUnit.MILLISECONDS);
//                    }
//                }))
//                .doOnNext(integer -> {
//                    Log.e(TAG,Thread.currentThread().getName());
//                    if (!sendSuccess) {
//                        switch (i) {
//                            case 0:
//                                Log.e(TAG, "push(DeviceAction.JuRenPro.ACTION_SETTING)");
//                                try {
//                                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SETTING);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                break;
//                            case 2:
//                                Log.e(TAG, "push(DeviceAction.Xjl.ACTION_SETTING)");
//                                try {
//                                    DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SETTING);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                break;
//                            case 3:
//                                Log.e(TAG, "push(DeviceAction.JuRenPlus.ACTION_SETTING)");
//                                try {
//                                    DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SETTING);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                break;
//                            case -1:
//                                Log.e(TAG, "push(DeviceAction.Dryer.ACTION_SETTING)");
//                                try {
//                                    DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_SETTING, 0);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                break;
//                        }
//                    } else {
//                        onCheckDeviceFinishListener.checkFinish();
//                    }
//                })
//                .doOnComplete(() -> Log.e(TAG, "doOnComplete"))
//                .subscribe();

        Observable.intervalRange(1000,3,1000,1600,TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
               .doOnNext(aLong -> {
                   Log.e(TAG,"flatMap:----------"+"sendSuccess:"+sendSuccess);
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
                           case -1:
                               Log.e(TAG,"push(DeviceAction.Dryer.ACTION_SETTING)");
                               try {
                                   DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_SETTING,0);
                               } catch (IOException e) {
                                   e.printStackTrace();
                               }
                               break;
                       }
                   }else {
                       if (!isFinisned) {
                           onCheckDeviceFinishListener.checkFinish();
                           isFinisned = true;
                       }
                   }
               })
                .doOnComplete(() -> {
                    if (!sendSuccess) {
                        try {
                            DeviceEngine.getInstance().close();
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
                                case -1:
                                    Log.e(TAG,"close 烘干机");
                                    break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        onCheckDeviceFinishListener.checkUnFinish();
                    }
                })
                .subscribe();
    }

    @Override
    public void sendInstructionSuccess(int key, Object object) {
        if (object instanceof WashStatusEvent) {
            sendSuccess = true;
            Log.e(TAG, "设置指令成功");
        } else if (object instanceof DryerStatus) {
            sendSuccess = true;
            Log.e(TAG, "设置指令成功");
        }
    }

    @Override
    public void sendInstructionFail(int key, String message) {

    }
}
