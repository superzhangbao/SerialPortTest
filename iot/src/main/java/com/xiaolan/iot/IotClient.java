package com.xiaolan.iot;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.log.IDGenerater;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class IotClient implements IConnectSendListener {

    private final String TAG = "IotClient";

    private int mPrePeriod = -1;//之前的洗衣阶段
    private boolean mPreIsError = false;//之前是否错误
    private boolean mPreIsRunning = false;//之前是否运行中的状态
    private int mPreRunStatus = -1;//0锁定 1运行中 2运行完成 3空闲 4暂停 5kill
    private boolean mIsSendMode = false;//是否上报过mode
    private int mPreTime = -1;
    private Disposable mTimeDispos;
    private Long countDownTime;//120s倒计时

    private IotClient() {
    }

    public static IotClient getInstance() {
        return IotClientHolder.IOT_CLIENT;
    }

    private static class IotClientHolder {
        private static final IotClient IOT_CLIENT = new IotClient();

    }

    /**
     * 如果需要动态注册设备获取设备的deviceSecret， 可以参考本接口实现。
     * 动态注册条件检测：
     * 1.云端开启该设备动态注册功能；
     * 2.首先在云端创建 pk，dn；
     *
     * @param context              上下文
     * @param productKey           产品类型
     * @param deviceName           设备名称 需要现在云端创建
     * @param productSecret        产品密钥
     * @param iConnectSendListener 密钥请求回调
     */
    public void registerDevice(Context context, String productKey, String deviceName, String productSecret, IConnectSendListener iConnectSendListener) {
        InitManager.registerDevice(context, productKey, deviceName, productSecret, iConnectSendListener);
    }

    /**
     * Android 设备端 SDK 初始化示例代码
     *
     * @param context       上下文
     * @param productKey    产品类型
     * @param deviceName    设备名称
     * @param deviceSecret  设备密钥
     * @param productSecret 产品密钥
     * @param iDemoCallback 初始化建联结果回调
     */
    public void init(Context context, String productKey, String deviceName, String deviceSecret, String productSecret, IDemoCallback iDemoCallback) {
        InitManager.init(context, productKey, deviceName, deviceSecret, productSecret, iDemoCallback);
    }

    /**
     * 订阅topic
     *
     * @param iConnectSubscribeListener 订阅topic的回调
     * @param topic                     需要订阅的topic
     */
    public void subscribe(IConnectSubscribeListener iConnectSubscribeListener, String... topic) {
        MqttSubscribeRequest request = new MqttSubscribeRequest();
        for (String aTopic : topic) {
            request.topic = aTopic;
        }
        request.isSubscribe = true;
        LinkKit.getInstance().subscribe(request, iConnectSubscribeListener);
    }

    /**
     * 注册监听
     *
     * @param iConnectNotifyListener 监听回调
     */
    public void registerOnPushListener(IConnectNotifyListener iConnectNotifyListener) {
        LinkKit.getInstance().registerOnPushListener(iConnectNotifyListener);
    }

    /**
     * 上报数据
     *
     * @param productKey           产品类型
     * @param deviceName           设备名称
     * @param topic                产品topic
     * @param payload              上报的数据
     * @param iConnectSendListener 上报数据的回调监听
     */
    public void uploadData(String productKey, String deviceName, String topic, String payload, IConnectSendListener iConnectSendListener) {
        MqttPublishRequest mqttPublishRequest = new MqttPublishRequest();
        // 支持 0 和 1， 默认0
        mqttPublishRequest.qos = 1;
        mqttPublishRequest.isRPC = false;
        mqttPublishRequest.topic = "/" + productKey + "/" + deviceName + "/user/" + topic;//发布topic
        mqttPublishRequest.msgId = String.valueOf(IDGenerater.generateId());
        mqttPublishRequest.payloadObj = payload;
        LinkKit.getInstance().publish(mqttPublishRequest, iConnectSendListener);
    }

    /**
     * 上报错误，主要用来单独上报串口掉线
     * @param productKey  产品类型
     * @param deviceName  设备名称
     * @param orderNumber 订单号
     */
    public void uploadError(String productKey, String deviceName, String orderNumber) {
        uploadData(productKey,deviceName,TopicManager.ERROR_TOPIC,getWash()+"DX,"+orderNumber,this);
    }

    /**
     *错误恢复，主要用来单独上报串口上线
     * @param productKey  产品类型
     * @param deviceName  设备名称
     * @param orderNumber 订单号
     */
    public void uploadRestoreError(String productKey, String deviceName, String orderNumber) {
        if (TextUtils.isEmpty(orderNumber)) {
            uploadData(productKey,deviceName,TopicManager.ERROR_TOPIC,getWash()+"REr",this);
        }else {
            uploadData(productKey,deviceName,TopicManager.ERROR_TOPIC,getWash()+"REr,"+orderNumber,this);
        }
    }

    /**
     * 上报洗衣机状态等数据
     */
    public void uploadWashRunning(boolean isRunning, boolean isError, int period, int washMode, int lightSupper, int viewStep, int err, String text, String text2, String orderNumber, String productKey, String deviceName) {
        uploadRemainTime(text, text2, orderNumber, productKey, deviceName);//上报时间
        if (isError) {//错误模式
            mPreIsError = true;
            String payload;
            switch (err) {
                case 2:
                    payload = getWash() + "1E," + orderNumber;
                    break;
                case 1:
                case 17:
                    payload = getWash() + "dE," + orderNumber;
                    break;
                case 4:
                    payload = getWash() + "UE," + orderNumber;
                    break;
                default:
                    payload = getWash() + "UN," + orderNumber;
                    break;
            }
            uploadData(productKey, deviceName, TopicManager.ERROR_TOPIC, payload, this);//故障topic
            Log.e(TAG,"发送了故障topic----"+TopicManager.ERROR_TOPIC);
        } else if (isRunning) {//运行状态
            //发送故障恢复topic
            if (mPreIsError) {
                mPreIsError = false;
                uploadData(productKey, deviceName, TopicManager.RESTOREERROR_TOPIC, "REr," + orderNumber, this);
                Log.e(TAG,"发送了故障恢复topic----"+TopicManager.RESTOREERROR_TOPIC+"="+err);
            }
            String payload;
            switch (period) {
                case 0://洗涤状态
                    if (mPrePeriod != period) {
                        mPrePeriod = period;
                        payload = "PW," + orderNumber;
                        uploadData(productKey, deviceName, TopicManager.PERIOD_TOPIC, payload, this);
                        Log.e(TAG,"发送了阶段topic----"+TopicManager.PERIOD_TOPIC+"=PW");
                    }
                    break;
                case 1://漂洗状态
                    if (mPrePeriod != period) {
                        mPrePeriod = period;
                        payload = "PR," + orderNumber;
                        uploadData(productKey, deviceName, TopicManager.PERIOD_TOPIC, payload, this);
                        Log.e(TAG,"发送了阶段topic----"+TopicManager.PERIOD_TOPIC+"=PR");
                    }
                    break;
                case 2://脱水状态
                    if (mPrePeriod != period) {
                        mPrePeriod = period;
                        payload = "PE," + orderNumber;
                        uploadData(productKey, deviceName, TopicManager.PERIOD_TOPIC, payload, this);
                        Log.e(TAG,"发送了阶段topic----"+TopicManager.PERIOD_TOPIC+"=PE");
                    }
                    break;
                default:
                    mPrePeriod = -1;
                    break;
            }
            if (viewStep == 2) {//暂停
                if (mPreRunStatus != 4) {//暂停
                    mPreRunStatus = 4;
                    payload = "RP," + orderNumber;
                    uploadData(productKey, deviceName, TopicManager.RUNSTATUS_TOPIC, payload, this);
                    Log.e(TAG,"发送了运行状态topic----"+TopicManager.RUNSTATUS_TOPIC+"=RP");
                }
            } else if (viewStep == 0x0a) {//运行完成
                //发送运行完成
                if (mPreRunStatus != 2) {
                    mPreRunStatus = 2;
                    payload = "RF," + orderNumber;
                    uploadData(productKey, deviceName, TopicManager.RUNSTATUS_TOPIC, payload, this);
                    Log.e(TAG,"发送了运行状态topic----"+TopicManager.RUNSTATUS_TOPIC+"=RF");
                }
                //重置状态
                reset();
            } else {
                //发送运行中
                if (mPreRunStatus != 1) {//运行中
                    mPreRunStatus = 1;
                    payload = "RR," + orderNumber;
                    uploadData(productKey, deviceName, TopicManager.RUNSTATUS_TOPIC, payload, this);
                    Log.e(TAG,"发送了运行状态topic----"+TopicManager.RUNSTATUS_TOPIC+"=RR");
                    if (!mIsSendMode) {
                        mIsSendMode = true;
                        //洗衣模式
                        switch (washMode) {
                            case 0x02://热水
                                if (lightSupper == 1) {
                                    payload = "MHoS"+ orderNumber;//热水加强
                                } else {
                                    payload = "MHo"+ orderNumber;
                                }
                                Log.e(TAG, "洗衣模式：" + "热水");
                                break;
                            case 0x03://温水
                                if (lightSupper == 1) {
                                    payload = "MWaS"+ orderNumber;//温水加强
                                } else {
                                    payload = "MWa"+ orderNumber;
                                }
                                Log.e(TAG, "洗衣模式：" + "温水");
                                break;
                            case 0x04://冷水
                                if (lightSupper == 1) {
                                    payload = "MCoS"+ orderNumber;//冷水加强
                                } else {
                                    payload = "MCo"+ orderNumber;
                                }
                                break;
                            case 0x05://轻柔
                                payload = "MDe"+ orderNumber;
                                break;
                            case 0x08://桶自洁
                                payload = "MTo"+ orderNumber;
                                break;
                        }
                        uploadData(productKey, deviceName, TopicManager.MODE_TOPIC, payload, this);
                        Log.e(TAG,"发送了模式topic----"+TopicManager.MODE_TOPIC+"="+payload);
                    }
                }
            }
        } else {//空闲状态
            if (mPreRunStatus != 3) {//空闲
                String payload = "RI," + orderNumber;
                mPreRunStatus = 3;
                uploadData(productKey, deviceName, TopicManager.RUNSTATUS_TOPIC, payload, this);
                Log.e(TAG,"发送了运行状态topic----"+TopicManager.RUNSTATUS_TOPIC+"=RI");
            }
        }
    }

    /**
     * 上报剩余时间
     */
    private void uploadRemainTime(String text, String text2, final String orderNumber, final String productKey, final String deviceName) {
        try {
            final Integer time = Integer.valueOf(text);
            if (time != mPreTime) {
                String payLoad = "T" + time + "," + orderNumber;
                uploadData(productKey, deviceName, TopicManager.REMAINTIME_TOPIC, payLoad, IotClient.this);
                Log.e(TAG,"发送了上报时间topic----"+TopicManager.REMAINTIME_TOPIC+"剩余时间="+time);
                mPreTime = time;
                if (mTimeDispos != null && !mTimeDispos.isDisposed()) {
                    mTimeDispos.dispose();
                }
                mTimeDispos = Observable.intervalRange(1, 121, 1, 1, TimeUnit.SECONDS)
                        .repeat()
                        .subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) {
                                if (aLong == 121) {//120s 剩余时间无变化直接上报
                                    String payLoad = "T" + time + "," + orderNumber;
                                    uploadData(productKey, deviceName, TopicManager.REMAINTIME_TOPIC, payLoad, IotClient.this);
                                    Log.e(TAG,"发送了上报时间topic----"+TopicManager.REMAINTIME_TOPIC+"剩余时间="+time+"(120s 上报)");
                                }
                            }
                        });
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.e(TAG,"剩余时间解析错误");
        }
    }

    @Override
    public void onResponse(ARequest aRequest, AResponse aResponse) {

    }

    @Override
    public void onFailure(ARequest aRequest, AError aError) {

    }

    private String getWash() {
        return "EC";
    }

    /**
     * 重置IOT上报状态
     */
    private void reset() {
        mPrePeriod = -1;//之前的洗衣阶段
        mPreIsError = false;//之前是否错误
        mPreIsRunning = false;//之前是否运行中的
        mPreRunStatus = -1;//0锁定 1运行中 2运行完成
        mIsSendMode = false;//是否上报过mode
        //结束计时
        if (mTimeDispos != null && !mTimeDispos.isDisposed()) {
            mTimeDispos.dispose();
        }
        Log.e(TAG,"已重置状态");
    }
}
