package com.xiaolan.serialporttest;

import android.app.Application;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.aliyun.alink.linksdk.tools.AError;
import com.xiaolan.iot.IDemoCallback;
import com.xiaolan.iot.IotClient;
import com.xiaolan.serialporttest.util1.ToastUtil;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class App extends Application {
    private static final String TAG = "App";
    //        public static String productKey = "a1KiHgH1DKF", deviceName = "cs101", deviceSecret = "W0s1rWphk4RQcF0l0QVu2Wzgy5wsOBnr", productSecret = "nBdHgxetPnAIxcaR";
    public static String productKey = "a1NsB5D5zEA", deviceName = "5ca4410effbbe13984074aa9", deviceSecret = "2HATLfmWrakhVI69UV11YqpvU0pFWhyM", productSecret = "GCy9Ginaa0ZptrjB";
    public final static String CONNECT_ID = "LINK_PERSISTENT";
    public static boolean isInitDone;

    @Override
    public void onCreate() {
        super.onCreate();
        MultiDex.install(this);
        ToastUtil.init(this);

//        IotClient.getInstance().registerDevice(this, productKey, deviceName, productSecret, new IConnectSendListener() {
//            @Override
//            public void onResponse(ARequest aRequest, AResponse aResponse) {
//                Log.e(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + (aResponse == null ? "null" : aResponse.data) + "]");
//                if (aResponse != null && aResponse.data != null) {
//                    // 解析云端返回的数据
//                    ResponseModel<Map<String, String>> response = JSONObject.parseObject(aResponse.data.toString(),
//                            new TypeReference<ResponseModel<Map<String, String>>>() {
//                            }.getType());
//                    if ("200".equals(response.code) && response.data != null && response.data.containsKey("deviceSecret") &&
//                            !TextUtils.isEmpty(response.data.get("deviceSecret"))) {
//                        /**
//                         * 建议将ds保存在非应用目录，确保卸载之后ds仍然可以读取到。
//                         */
//                        deviceSecret = response.data.get("deviceSecret");
//                        // getDeviceSecret success, to build connection.
//                        // 持久化 deviceSecret 初始化建联的时候需要
//                        // 用户需要按照实际场景持久化设备的三元组信息，用于后续的连接
//                        SharedPreferences preferences = getSharedPreferences("deviceAuthInfo", 0);
//                        SharedPreferences.Editor editor = preferences.edit();
//                        editor.putString("deviceId", productKey + deviceName);
//                        editor.putString("deviceSecret", deviceSecret);
//                        //提交当前数据
//                        editor.commit();
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(ARequest aRequest, AError aError) {
//                Log.e(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
//                ToastUtil.show("注册设备失败");
//            }
//        });

//        if (!isInitDone) {
//            connect();
//        } else {
//            ToastUtil.show("已初始化");
//        }
    }

    private void connect() {
        Log.e(TAG, "connect() called");
        // SDK初始化
        IotClient.getInstance().init(this, productKey, deviceName, deviceSecret, productSecret, new IDemoCallback() {
            @Override
            public void onError(AError aError) {
                Log.e(TAG, "onError() called with: aError = [" + aError + "]");
                // 初始化失败，初始化失败之后需要用户负责重新初始化
                // 如一开始网络不通导致初始化失败，后续网络回复之后需要重新初始化
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            ToastUtil.show("初始化失败");
                            isInitDone = false;
                        })
                        .subscribe();
            }

            @Override
            public void onInitDone(Object data) {
                Log.e(TAG, "onInitDone() called with: data = [" + data + "]");
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(integer -> {
                            ToastUtil.show("初始化成功");
                            isInitDone = true;
                        })
                        .subscribe();
            }
        });
    }
}
