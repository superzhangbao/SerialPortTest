package com.xiaolan.serialporttest.iot

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.aliyun.alink.linkkit.api.LinkKit
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest
import com.aliyun.alink.linksdk.cmp.core.base.AMessage
import com.aliyun.alink.linksdk.cmp.core.base.ARequest
import com.aliyun.alink.linksdk.cmp.core.base.AResponse
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener
import com.aliyun.alink.linksdk.tools.AError
import com.xiaolan.iot.IDemoCallback
import com.xiaolan.iot.IotClient
import com.xiaolan.serialporttest.App
import com.xiaolan.serialporttest.App.CONNECT_ID
import com.xiaolan.serialporttest.R
import com.xiaolan.serialporttest.event.RRPCEvent
import com.xiaolan.serialporttest.util1.KeybordUtils
import com.xiaolan.serialporttest.util1.ToastUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_iot.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 测试IOT
 */
class IOTActivity : AppCompatActivity(), View.OnClickListener, IConnectNotifyListener, IConnectSendListener {

    companion object {
        const val TAG: String = "IOTActivity"
    }

    private var mDispQueueThread: DispQueueThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iot)
        KeybordUtils.isSoftInputShow(this)
        EventBus.getDefault().register(this)
        mDispQueueThread = DispQueueThread()
        mDispQueueThread?.start()

        initialize.setOnClickListener(this)
        send1.setOnClickListener(this)
        send2.setOnClickListener(this)
        send3.setOnClickListener(this)
        send4.setOnClickListener(this)
        send5.setOnClickListener(this)
        send6.setOnClickListener(this)
        send7.setOnClickListener(this)
        IotClient.getInstance().registerOnPushListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.initialize -> {
                if (!TextUtils.isEmpty(App.deviceSecret)) {
                    if (!App.isInitDone) {
                        connect()
                    } else {
                        ToastUtil.show("已初始化")
                    }
                }
            }
            R.id.send1 -> {
                Observable.intervalRange(0, 1, 0, 1000, TimeUnit.MILLISECONDS)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "system", "SVe1.0,O123456789", this) }
                        .subscribe()
            }
            R.id.send2 -> {
                Observable.just(1)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "error", "EC1E,O123456789", this) }
                        .subscribe()
                Observable.just(1)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "error", "ECdE,O123456789", this) }
                        .subscribe()
                Observable.just(1)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "error", "ECUE,O123456789", this) }
                        .subscribe()
                Observable.just(1)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "error", "ECUN,O123456789", this) }
                        .subscribe()
                Observable.just(1)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "error", "ECDX,O123456789", this) }
                        .subscribe()
            }
            R.id.send3 -> {
                Observable.intervalRange(0, 1, 0, 1000, TimeUnit.MILLISECONDS)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "mode", "MHo,O123456789", this) }
                        .subscribe()
            }
            R.id.send4 -> {
                Observable.intervalRange(0, 1, 0, 1000, TimeUnit.MILLISECONDS)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "period", "PW,O123456789", this) }
                        .subscribe()
            }
            R.id.send5 -> {
                Observable.intervalRange(0, 1, 0, 1000, TimeUnit.MILLISECONDS)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "order", "COr,O123456789", this) }
                        .subscribe()
            }
            R.id.send6 -> {
                Observable.intervalRange(0, 1, 0, 1000, TimeUnit.MILLISECONDS)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "remainTime", "T32,O123456789", this) }
                        .subscribe()
            }
            R.id.send7 -> {
                Observable.intervalRange(0, 1, 0, 1000, TimeUnit.MILLISECONDS)
                        .doOnNext { IotClient.getInstance().uploadData(App.productKey, App.deviceName, "runState", "RR,O123456789", this) }
                        .subscribe()
            }
        }
    }

    private fun connect() {
        Log.e(TAG, "connect() called")
        // SDK初始化
        IotClient.getInstance().init(this, App.productKey, App.deviceName, App.deviceSecret, App.productSecret, object : IDemoCallback {
            override fun onInitDone(data: Any?) {
                Log.e(TAG, "onInitDone() called with: data = [$data]")
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext { ToastUtil.show("初始化成功") }
                        .subscribe()
                IotClient.getInstance().subscribe(object : IConnectSubscribeListener {
                    override fun onSuccess() {
                        ToastUtil.show("订阅成功")
                    }

                    override fun onFailure(p0: AError?) {
                        ToastUtil.show("订阅成功")
                    }
                }, "/a1KiHgH1DKF/cs101/user/sub2", "/a1KiHgH1DKF/cs101/user/sub")
            }

            override fun onError(aError: AError?) {
                Log.e(TAG, "onError() called with: aError = [$aError]")
                // 初始化失败，初始化失败之后需要用户负责重新初始化
                // 如一开始网络不通导致初始化失败，后续网络回复之后需要重新初始化
                Observable.just(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext { ToastUtil.show("初始化失败") }
                        .subscribe()
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onInstrctionMode(s: String?) {
        if (s != null) {
            Log.e(TAG, "收到下行消息")
            mDispQueueThread?.AddQueue("收到下行消息")
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    fun onRRPCEvent(rrpcEvent: RRPCEvent?) {
        if (rrpcEvent != null) {

        }
    }

    override fun onNotify(connectId: String, topic: String, aMessage: AMessage) {
        val s = String(aMessage.data as ByteArray)
        Log.e(TAG, "onNotify() called with: connectId = [$connectId], topic = [$topic], aMessage = $s")
        if (CONNECT_ID == connectId && !TextUtils.isEmpty(topic) && topic.startsWith("/ext/rrpc/")) {
            ToastUtil.show("收到RRPC消息")
            mDispQueueThread?.AddQueue("\"收到RRPC消息\"")
            val request = MqttPublishRequest()
            request.isRPC = false
            request.topic = "/a1KiHgH1DKF/cs101/user/test"
            request.replyTopic = "/a1KiHgH1DKF/cs101/user/test"
            request.topic = topic.replace("request", "response")
            val resId = topic.substring(topic.indexOf("rrpc/request/") + 13)
            request.msgId = resId
            // TODO 用户根据实际情况填写 仅做参考
            request.payloadObj = "RL," + System.currentTimeMillis() + ",O123456789"
            LinkKit.getInstance().publish(request, object : IConnectSendListener {
                override fun onResponse(aRequest: ARequest, aResponse: AResponse) {
                    Log.e(TAG, "返回topic成功 =======onResponse() called with: aRequest = [$aRequest], aResponse = [$aResponse]")
                    ToastUtil.show("返回topic成功")
                }

                override fun onFailure(aRequest: ARequest, aError: AError) {
                    Log.e(TAG, "返回topic失败 =======onFailure() called with: aRequest = [$aRequest], aError = [$aError]")
                    ToastUtil.show("返回topic失败")
                }
            })
        } else {
            when {
                topic.endsWith("sub") -> {
                    ToastUtil.show("收到sub下行消息")
                    Log.e(TAG, "收到sub下行消息")
                }
                topic.endsWith("sub2") -> {
                    ToastUtil.show("收到sub2下行消息")
                    Log.e(TAG, "收到sub2下行消息")
                }
                else -> {
                    ToastUtil.show("收到下行消息")
                    Log.e(TAG, "收到下行消息")
                }
            }
//            EventBus.getDefault().post(s)
        }
    }

    override fun shouldHandle(s: String, topic: String): Boolean {
        return true
    }

    override fun onConnectStateChange(s: String, connectState: ConnectState) {
        Log.e(TAG, "s:===$s,connectState:===$connectState")
        if (connectState == ConnectState.CONNECTED) {
            //            connect();
        }
    }

    override fun onResponse(aRequest: ARequest?, aResponse: AResponse?) {
        Log.e(TAG, "onResponse() called with: aRequest = [$aRequest], aResponse = [$aResponse]")
    }


    override fun onFailure(aRequest: ARequest?, aError: AError?) {
        Log.e(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError?.code + "---" + aError?.msg + "]")
    }


//    private fun updatePropertyValue(viewStep: Int, washMode: Int, isRunning: Boolean, text: String, topic: String) {
//        val request = MqttPublishRequest()
//        // 支持 0 和 1， 默认0
//        request.qos = 1
//        request.isRPC = false
//        request.topic = "/"+App.productKey+"/" + App.deviceName + "/user/$topic"//发布topic
//        val resId = IDGenerater.generateId().toString()
//        request.msgId = resId
//        // TODO 用户根据实际情况填写 仅做参考
////        val wash = Wash(viewStep.toLong(), washMode.toLong(), isRunning, text)
//        request.payloadObj = "$text,O123456789"
//        Log.e(TAG,"走了")
//        LinkKit.getInstance().publish(request, object : IConnectSendListener {
//            override fun onResponse(aRequest: ARequest, aResponse: AResponse) {
//            }
//
//            override fun onFailure(aRequest: ARequest, aError: AError) {
//
//            }
//        })
//    }

    //----------------------------------------------------刷新显示线程
    private inner class DispQueueThread : Thread() {
        private val QueueList = LinkedList<String>()

        override fun run() {
            super.run()
            while (!isInterrupted) {
                val s: String = ""
                while ((QueueList.poll()) != null) {
                    runOnUiThread { DispRecData(s) }
                    try {
                        Thread.sleep(100)//显示性能高的话，可以把此数值调小。
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    break
                }
            }
        }

        @Synchronized
        internal fun AddQueue(s: String) {
            QueueList.add(s)
        }
    }

    //----------------------------------------------------显示接收数据
    private fun DispRecData(s: String) {
        et.append(s)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}
