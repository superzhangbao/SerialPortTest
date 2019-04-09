package com.xiaolan.serialporttest.wash.jurenpro;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.xiaolan.iot.IotClient;
import com.xiaolan.serialporttest.App;
import com.xiaolan.serialporttest.R;
import com.xiaolan.serialporttest.event.InstrctionMode;
import com.xiaolan.serialporttest.event.RRPCEvent;
import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
import com.xiaolan.serialporttest.mylib.utils.MyFunc;
import com.xiaolan.serialporttest.util1.KeybordUtils;
import com.xiaolan.serialporttest.util1.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 巨人pro洗衣机页面
 */
public class JuRenProWashActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, OnSendInstructionListener, CurrentStatusListener, SerialPortOnlineListener {
    private static final String TAG = "JuRenProWashActivity";
    @BindView(R.id.btn_open_port)
    Button mBtnOpenPort;
    @BindView(R.id.btn_clear)
    Button mBtnClear;
    @BindView(R.id.cb_hex_rev)
    RadioButton mCbHexRev;
    @BindView(R.id.cb_text_rev)
    RadioButton mCbTextRev;
    @BindView(R.id.rg_rev)
    RadioGroup mRgRev;
    @BindView(R.id.editTextRecDisp)
    EditText mEditTextRecDisp;
    @BindView(R.id.btn_hot)
    Button mBtnHot;
    @BindView(R.id.btn_warm)
    Button mBtnWarm;
    @BindView(R.id.btn_cold)
    Button mBtnCold;
    @BindView(R.id.btn_super)
    Button mBtnSuper;
    @BindView(R.id.btn_start_stop)
    Button mBtnStartStop;
    @BindView(R.id.ll_wash)
    LinearLayout mLlWash;
    @BindView(R.id.btn_soft)
    Button mBtnSoft;
    @BindView(R.id.btn_kill)
    Button mBtnKill;
    @BindView(R.id.btn_setting)
    Button mBtnSetting;

    private int revType = 0;
    private ArrayList<Button> mButtons = new ArrayList<>();
    private int mSelectMode = -1;
    private boolean mSuper = false;
    private int mBtnStatus = 0;
    private DispQueueThread2 mDispQueueThread2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);
        KeybordUtils.isSoftInputShow(this);

        mCbHexRev.setChecked(true);

        mButtons.add(mBtnHot);
        mButtons.add(mBtnWarm);
        mButtons.add(mBtnCold);
        mButtons.add(mBtnSoft);

        mRgRev.setOnCheckedChangeListener(this);

        mDispQueueThread2 = new DispQueueThread2();
        mDispQueueThread2.start();
        //设置发送指令监听
        DeviceEngine.getInstance().setOnSendInstructionListener(this);
        //设置上报状态监听
        DeviceEngine.getInstance().setOnCurrentStatusListener(this);
        //设置读取串口数据监听
        DeviceEngine.getInstance().setOnSerialPortOnlineListener(this);
        EventBus.getDefault().register(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n")
    @OnClick({R.id.btn_finsh, R.id.btn_open_port, R.id.btn_clear, R.id.btn_hot,
            R.id.btn_warm, R.id.btn_cold, R.id.btn_soft, R.id.btn_super, R.id.btn_start_stop,
            R.id.btn_kill, R.id.btn_setting, R.id.btn_reset, R.id.btn_self_cleaning, R.id.btn_test})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_test:
//                Observable.intervalRange(0, 5, 0, 3, TimeUnit.SECONDS)
//                        .doOnNext(aLong -> updatePropertyValue(0, 0, false, "测试"))
//                        .subscribe();
                break;
            case R.id.btn_finsh:
                boolean open = DeviceEngine.getInstance().isOpen();
                if (open) {
                    try {
                        DeviceEngine.getInstance().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        finish();
                    }
                }
                break;
            case R.id.btn_open_port:
                //打开/关闭串口
                if (mBtnOpenPort.getText().equals("打开串口")) {
                    try {
                        DeviceEngine.getInstance().open();
                        mBtnOpenPort.setText("关闭串口");
                    } catch (Exception e) {
                        e.printStackTrace();
                        mBtnOpenPort.setText("打开串口");
                    }
                } else {
                    try {
                        DeviceEngine.getInstance().close();
                        mBtnOpenPort.setText("打开串口");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.btn_clear:
                //清空数据接收区
                mEditTextRecDisp.setText("");
                break;
            case R.id.btn_hot:
                checkIsOpen();
                mSelectMode = 0;
                setBackgroundColor(R.id.btn_hot);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_HOT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_warm:
                checkIsOpen();
                mSelectMode = 1;
                setBackgroundColor(R.id.btn_warm);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_WARM);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_cold:
                checkIsOpen();
                mSelectMode = 2;
                setBackgroundColor(R.id.btn_cold);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_COLD);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_soft:
                checkIsOpen();
                mSelectMode = 3;
                setBackgroundColor(R.id.btn_soft);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_DELICATES);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_super:
                checkIsOpen();
                mSuper = !mSuper;
                setBackgroundColor(R.id.btn_super);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SUPER);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_start_stop:
                checkIsOpen();
                if (mBtnStatus == 0) {
                    mBtnStatus = 1;
                } else if (mBtnStatus == 1) {
                    mBtnStatus = 2;
                } else if (mBtnStatus == 2) {
                    mBtnStatus = 1;
                }
                setBackgroundColor(R.id.btn_start_stop);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_START);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_setting:
                checkIsOpen();
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SETTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_kill:
                checkIsOpen();
                setBackgroundColor(R.id.btn_kill);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_KILL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_reset:
                checkIsOpen();
                setBackgroundColor(R.id.btn_reset);
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_RESET);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_self_cleaning:
                checkIsOpen();
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SELF_CLEANING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void checkIsOpen() {
        if (!DeviceEngine.getInstance().isOpen()) {
            try {
                DeviceEngine.getInstance().open();
                mBtnOpenPort.setText("关闭串口");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置按钮背景
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setBackgroundColor(int id) {
        if (id == R.id.btn_super) {
            if (mSuper) {
                mBtnSuper.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
            } else {
                mBtnSuper.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            }
        } else if (id == R.id.btn_start_stop) {
            if (mSelectMode == -1) {
                mBtnHot.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
            }
            if (mBtnStatus == 1) {
                mBtnStartStop.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                mBtnStartStop.setText("暂停");
            } else if (mBtnStatus == 2) {
                mBtnStartStop.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
                mBtnStartStop.setText("开始");
            }
        } else {
            for (int i = 0; i < mButtons.size(); i++) {
                Button button = mButtons.get(i);
                if (button.getId() == id) {
                    button.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
                } else {
                    button.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                }
            }
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == mCbHexRev.getId()) {
            revType = 0;
        } else if (checkedId == mCbTextRev.getId()) {
            revType = 1;
        }
    }

    @Override
    public void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent) {
        switch (key) {
            case 1:
                Log.e(TAG, "开始指令成功");
                ToastUtil.show("开始指令成功");
                break;
            case 2:
                Log.e(TAG, "热水指令成功");
                ToastUtil.show("热水指令成功");
                break;
            case 3:
                Log.e(TAG, "温水指令成功");
                ToastUtil.show("温水指令成功");
                break;
            case 4:
                Log.e(TAG, "冷水指令成功");
                ToastUtil.show("冷水指令成功");
                break;
            case 5:
                Log.e(TAG, "精致衣物指令成功");
                ToastUtil.show("精致衣物指令成功");
                break;
            case 6:
                Log.e(TAG, "加强洗指令成功");
                ToastUtil.show("加强洗指令成功");
                break;
            case 8:
                Log.e(TAG, "设置指令成功");
                ToastUtil.show("设置指令成功");
                break;
            case 10:
                Log.e(TAG, "kill指令成功");
                ToastUtil.show("kill指令成功");
                break;
        }
        mDispQueueThread2.AddQueue(washStatusEvent);//线程定时刷新显示
    }

    @Override
    public void sendInstructionFail(int key, String message) {
        switch (key) {
            case 1:
                Log.e(TAG, "开始指令失败");
                break;
            case 2:
                Log.e(TAG, "热水指令失败");
                break;
            case 3:
                Log.e(TAG, "温水指令失败");
                break;
            case 4:
                Log.e(TAG, "冷水指令失败");
                break;
            case 5:
                Log.e(TAG, "精致衣物指令失败");
                break;
            case 6:
                Log.e(TAG, "加强洗指令失败");
                break;
            case 8:
                Log.e(TAG, "设置指令失败");
                break;
            case 10:
                Log.e(TAG, "kill指令失败");
                break;
        }
    }

    @Override
    public void onSerialPortOnline(byte[] bytes) {
        String s = MyFunc.ByteArrToHex(bytes);
        Log.e(TAG, "串口上线:" + s);
        Toast.makeText(this, "串口上线" + s, Toast.LENGTH_SHORT).show();
        IotClient.getInstance().uploadRestoreError(App.productKey,App.deviceName,"O963147630");
    }

    @Override
    public void onSerialPortOffline(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        IotClient.getInstance().uploadError(App.productKey,App.deviceName,"O963147630");
    }

    @Override
    public void currentStatus(Object washStatusEvent) {
        if (washStatusEvent instanceof WashStatusEvent) {
            WashStatusEvent washStatus = (WashStatusEvent) washStatusEvent;
            int washMode = washStatus.getWashMode();
            int lightSupper = washStatus.getLightSupper();
            int viewStep = washStatus.getViewStep();
            int err = washStatus.getErr();
            String text = washStatus.getText();
            String text2 = washStatus.getText2();
            boolean isRunning = washStatus.getIsRunning();
            boolean isError = washStatus.getIsError();
            int isWashing = washStatus.getIsWashing();
            int period = washStatus.getPeriod();//洗衣阶段
            //IOT去执行解析数据,上报数据
            IotClient.getInstance().uploadWashRunning(isWashing,isRunning,isError,period,washMode,lightSupper,viewStep,err,text,text2,"O963147630",App.productKey,App.deviceName);
            Log.e(TAG, "屏显：" + washStatus.getLogmsg().toString());
            mDispQueueThread2.AddQueue(washStatus);
        }
    }

//    private void updatePropertyValue(String payLoad, String topic) {
//        MqttPublishRequest request = new MqttPublishRequest();
//        // 支持 0 和 1， 默认0
//        request.qos = 1;
//        request.isRPC = false;
//        request.topic = "/" + App.productKey + "/" + App.deviceName + "/user/" + topic;//发布topic
//        request.msgId = String.valueOf(IDGenerater.generateId());
//        request.payloadObj = payLoad + ",123456789";
//        LinkKit.getInstance().publish(request, new IConnectSendListener() {
//            @Override
//            public void onResponse(ARequest aRequest, AResponse aResponse) {
//                Log.e(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + aResponse + "]");
//            }
//
//            @Override
//            public void onFailure(ARequest aRequest, AError aError) {
//                Log.e(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError.getCode() + "---" + aError.getMsg() + "]");
//            }
//        });
//    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onInstrctionMode(InstrctionMode instrctionMode) {
        if (instrctionMode != null) {
            int mode = instrctionMode.getInstrctionMode();
            switch (mode) {
                case 0:
                    Log.e(TAG, "收到指令0");
//                    try {
//                        DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_KILL);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    break;
                case 1:
                    Log.e(TAG, "收到指令1");
//                    try {
//                        DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SELF_CLEANING);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    break;
                default:
                    Log.e(TAG, "收到指令" + mode);
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onRRPCEvent(RRPCEvent rrpcEvent) {
        if (rrpcEvent != null) {
            ToastUtil.show("收到RRPC消息");
        }
    }


    //----------------------------------------------------刷新显示线程
    private class DispQueueThread2 extends Thread {
        private Queue<WashStatusEvent> QueueList = new LinkedList<>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final WashStatusEvent washStatusEvent;
                while ((washStatusEvent = QueueList.poll()) != null) {
                    runOnUiThread(() -> DispRecData2(washStatusEvent));
                    try {
                        Thread.sleep(100);//显示性能高的话，可以把此数值调小。
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        synchronized void AddQueue(WashStatusEvent washStatusEvent) {
            QueueList.add(washStatusEvent);
        }
    }

    //----------------------------------------------------显示接收数据
    private void DispRecData2(WashStatusEvent washStatusEvent) {
        mEditTextRecDisp.append(washStatusEvent.getLogmsg());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        try {
            DeviceEngine.getInstance().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----------------------------------------------------刷新显示线程
//    private class DispQueueThread extends Thread {
//        private Queue<ComBean> QueueList = new LinkedList<>();
//
//        @Override
//        public void run() {
//            super.run();
//            while (!isInterrupted()) {
//                final ComBean ComData;
//                while ((ComData = QueueList.poll()) != null) {
//                    runOnUiThread(() -> DispRecData(ComData));
//                    try {
//                        Thread.sleep(100);//显示性能高的话，可以把此数值调小。
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                }
//            }
//        }
//
//        synchronized void AddQueue(ComBean comBean) {
//            QueueList.add(comBean);
//        }
//    }

//    private int count = 0;

    //----------------------------------------------------显示接收数据
//    private void DispRecData(ComBean comBean) {
//        StringBuilder sMsg = new StringBuilder();
//        sMsg.append(comBean.sRecTime);
//        sMsg.append("[");
//        sMsg.append(comBean.sComPort);
//        sMsg.append("]");
//        if (revType == 0) {
//            sMsg.append("[Hex] ");
//            String hex = MyFunc.ByteArrToHex(comBean.bRec);
//            String[] split = hex.split(" ");
//            sMsg.append(hex);
//            sMsg.append("------->").append(count).append("------->size:").append(split.length);
//            Log.w(TAG, hex);
//        } else {
//            sMsg.append("[Text] ");
//            String text = new String(comBean.bRec);
//            sMsg.append(text);
//            sMsg.append("------->").append(count).append("------->size:").append(text.length());
//            Log.w(TAG, text);
//        }
//        count++;
//        if (comBean.mRecOrSend == 0) {
//            sMsg.append("---[收]");
//        } else if (comBean.mRecOrSend == 1) {
//            sMsg.append("---[发]");
//        }
//        sMsg.append("\r\n");
//        mEditTextRecDisp.append(sMsg);
//    }
}
