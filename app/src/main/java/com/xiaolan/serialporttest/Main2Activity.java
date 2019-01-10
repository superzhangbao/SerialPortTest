package com.xiaolan.serialporttest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.xiaolan.serialporttest.bean.ComBean;
import com.xiaolan.serialporttest.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.mylib.MyFunc;
import com.xiaolan.serialporttest.mylib.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.SerialPortReadDataListener;
import com.xiaolan.serialporttest.util1.KeybordUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android_serialport_api.SerialPortFinder;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Main2Activity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, OnSendInstructionListener, CurrentStatusListener, SerialPortReadDataListener {
    private static final String TAG = "Main2Activity";
    @BindView(R.id.btn_set_port)
    Button mBtnSetPort;
    @BindView(R.id.btn_set_baud)
    Button mBtnSetBaud;
    @BindView(R.id.btn_set_data)
    Button mBtnSetData;
    @BindView(R.id.btn_set_stop)
    Button mBtnSetStop;
    @BindView(R.id.btn_set_verify)
    Button mBtnSetVerify;
    @BindView(R.id.cb_hex)
    RadioButton mCbHex;
    @BindView(R.id.cb_text)
    RadioButton mCbText;
    @BindView(R.id.btn_open_port)
    Button mBtnOpenPort;
    @BindView(R.id.btn_clear)
    Button mBtnClear;
    @BindView(R.id.cb_hex_rev)
    RadioButton mCbHexRev;
    @BindView(R.id.cb_text_rev)
    RadioButton mCbTextRev;
    @BindView(R.id.rg)
    RadioGroup mRg;
    @BindView(R.id.rg_rev)
    RadioGroup mRgRev;
    @BindView(R.id.btn_send)
    Button mBtnSend;
    @BindView(R.id.et)
    EditText mEt;
    @BindView(R.id.editTextRecDisp)
    EditText mEditTextRecDisp;
    @BindView(R.id.ll_send)
    LinearLayout mLlSend;
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
    @BindView(R.id.btn_wash)
    Button mBtnWash;
    @BindView(R.id.btn_setting)
    Button mBtnSetting;

    private MyHandler mMyHandler;
    private int sendType = 0;
    private int revType = 0;
    private DispQueueThread mDispQueueThread;
    private SerialPortFinder mPortFinder;
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
        mEt.clearFocus();

        mCbHex.setChecked(true);
        mCbHexRev.setChecked(true);


        mButtons.add(mBtnHot);
        mButtons.add(mBtnWarm);
        mButtons.add(mBtnCold);
        mButtons.add(mBtnSoft);

        mRg.setOnCheckedChangeListener(this);
        mRgRev.setOnCheckedChangeListener(this);
        mMyHandler = new MyHandler();
        mDispQueueThread = new DispQueueThread();
        mDispQueueThread.start();
        mDispQueueThread2 = new DispQueueThread2();
        mDispQueueThread2.start();
        mPortFinder = new SerialPortFinder();
        DeviceEngine.getInstance().setOnSendInstructionListener(this);
        DeviceEngine.getInstance().setOnCurrentStatusListener(this);
        DeviceEngine.getInstance().setOnSerialPortConnectListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n")
    @OnClick({R.id.btn_writelog,R.id.btn_finsh, R.id.btn_set_port, R.id.btn_set_baud, R.id.btn_set_data, R.id.btn_set_stop,
            R.id.btn_set_verify, R.id.btn_open_port, R.id.btn_clear, R.id.btn_send, R.id.btn_hot,
            R.id.btn_warm, R.id.btn_cold, R.id.btn_soft, R.id.btn_super, R.id.btn_start_stop,
            R.id.btn_kill, R.id.btn_wash, R.id.btn_setting})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_writelog:

                break;
            case R.id.btn_finsh:
                finish();
                break;
            case R.id.btn_set_port:
                new AlertDialog.Builder(this)
                        .setTitle("选择串口")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getAllDevices(), 0, (dialog, which) -> {
                            String device = mPortFinder.getAllDevicesPath()[which];
                            mBtnSetPort.setText(device);
                            DeviceEngine.getInstance().setPort(device);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_baud:
                new AlertDialog.Builder(this)
                        .setTitle("选择波特率")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getBaudRateList(), 0, (dialog, which) -> {
                            String bRate = mPortFinder.getBaudRateList()[which];
                            Integer baudRate = Integer.valueOf(bRate);
                            mBtnSetBaud.setText(baudRate);
                            DeviceEngine.getInstance().setBaudRate(baudRate);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_data:
                new AlertDialog.Builder(this)
                        .setTitle("选择数据位")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getDataBits(), 0, (dialog, which) -> {
                            String dataBits = mPortFinder.getDataBits()[which];
                            mBtnSetData.setText(dataBits);
                            DeviceEngine.getInstance().setDataBits(dataBits);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_stop:
                new AlertDialog.Builder(this)
                        .setTitle("选择停止位")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getStopBits(), 0, (dialog, which) -> {
                            String stopBits = mPortFinder.getStopBits()[which];
                            mBtnSetStop.setText(stopBits);
                            DeviceEngine.getInstance().setStopBits(stopBits);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_set_verify:
                new AlertDialog.Builder(this)
                        .setTitle("选择停止位")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getParityBits(), 0, (dialog, which) -> {
                            String parityBits = mPortFinder.getParityBits()[which];
                            mBtnSetVerify.setText(parityBits);
                            DeviceEngine.getInstance().setParityBits(parityBits);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_open_port:
                //打开/关闭串口
                if (mBtnOpenPort.getText().equals("打开串口")) {
                    try {
                        mBtnSetPort.setText(DeviceEngine.getInstance().getPort());
                        mBtnSetBaud.setText(String.valueOf(DeviceEngine.getInstance().getBaudRate()));
                        mBtnSetData.setText(DeviceEngine.getInstance().getDataBits());
                        mBtnSetStop.setText(DeviceEngine.getInstance().getStopBits());
                        mBtnSetVerify.setText(DeviceEngine.getInstance().getParityBits());
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
                count = 0;
                mEt.clearFocus();
                KeybordUtils.closeKeybord(mEt, this);
                break;
            case R.id.btn_send:
                if (DeviceEngine.getInstance().isOpen()) {
                    Toast.makeText(this, "请打开串口", Toast.LENGTH_SHORT).show();
                    return;
                }
                String trim = mEt.getText().toString().trim();
                if (!TextUtils.isEmpty(trim)) {
                    if (sendType == 0) {
                        DeviceEngine.getInstance().sendHex(trim);
                    } else {
                        DeviceEngine.getInstance().sendText(trim);
                    }
                } else {
                    Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_hot:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mSelectMode = 0;
                setBackgroundColor(R.id.btn_hot);
                DeviceEngine.getInstance().push(DeviceAction.ACTION_HOT);
                break;
            case R.id.btn_warm:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mSelectMode = 1;
                setBackgroundColor(R.id.btn_warm);
                DeviceEngine.getInstance().push(DeviceAction.ACTION_WARM);
                break;
            case R.id.btn_cold:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mSelectMode = 2;
                setBackgroundColor(R.id.btn_cold);
                DeviceEngine.getInstance().push(DeviceAction.ACTION_COLD);
                break;
            case R.id.btn_soft:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mSelectMode = 3;
                setBackgroundColor(R.id.btn_soft);
                DeviceEngine.getInstance().push(DeviceAction.ACTION_DELICATES);
                break;
            case R.id.btn_super:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mSuper = !mSuper;
                setBackgroundColor(R.id.btn_super);
                DeviceEngine.getInstance().push(DeviceAction.ACTION_SUPER);
                break;
            case R.id.btn_start_stop:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (mBtnStatus == 0) {
                    mBtnStatus = 1;
                } else if (mBtnStatus == 1) {
                    mBtnStatus = 2;
                } else if (mBtnStatus == 2) {
                    mBtnStatus = 1;
                }
                setBackgroundColor(R.id.btn_start_stop);
                DeviceEngine.getInstance().push(DeviceAction.ACTION_START);
                break;
            case R.id.btn_setting:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                DeviceEngine.getInstance().push(DeviceAction.ACTION_SETTING);
                break;
            case R.id.btn_kill:
                if (!DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().open();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                setBackgroundColor(R.id.btn_kill);
                DeviceEngine.getInstance().push(DeviceAction.ACTION_KILL);
                break;
            case R.id.btn_wash:
                if (mLlWash.getVisibility() == View.VISIBLE) {
                    mBtnWash.setText("模拟洗衣机");
                    mLlSend.setVisibility(View.VISIBLE);
                    mLlWash.setVisibility(View.GONE);
                } else {
                    mBtnWash.setText("返回");
                    mLlSend.setVisibility(View.GONE);
                    mLlWash.setVisibility(View.VISIBLE);
                    mBtnStatus = 0;
                    mSuper = false;
                }
                break;
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
        if (checkedId == mCbHex.getId()) {
            sendType = 0;
        } else if (checkedId == mCbText.getId()) {
            sendType = 1;
        } else if (checkedId == mCbHexRev.getId()) {
            revType = 0;
        } else if (checkedId == mCbTextRev.getId()) {
            revType = 1;
        }
    }

    @Override
    public void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent) {
        switch (key) {
            case 1:
                Log.e(TAG,"开始指令成功");
                break;
            case 2:
                Log.e(TAG,"热水指令成功");
                break;
            case 3:
                Log.e(TAG,"温水指令成功");
                break;
            case 4:
                Log.e(TAG,"冷水指令成功");
                break;
            case 5:
                Log.e(TAG,"精致衣物指令成功");
                break;
            case 6:
                Log.e(TAG,"加强洗指令成功");
                break;
            case 8:
                Log.e(TAG,"设置指令成功");
                break;
            case 10:
                Log.e(TAG,"kill指令成功");
                break;
        }
        mDispQueueThread2.AddQueue(washStatusEvent);//线程定时刷新显示
    }

    @Override
    public void sendInstructionFail(int key, String message) {
        switch (key) {
            case 1:
                Log.e(TAG,"开始指令失败");
                break;
            case 2:
                Log.e(TAG,"热水指令失败");
                break;
            case 3:
                Log.e(TAG,"温水指令失败");
                break;
            case 4:
                Log.e(TAG,"冷水指令失败");
                break;
            case 5:
                Log.e(TAG,"精致衣物指令失败");
                break;
            case 6:
                Log.e(TAG,"加强洗指令失败");
                break;
            case 8:
                Log.e(TAG,"设置指令失败");
                break;
            case 10:
                Log.e(TAG,"kill指令失败");
                break;
        }
    }

    @Override
    public void currentStatus(WashStatusEvent washStatusEvent) {
        Log.e(TAG,"屏显："+washStatusEvent.getText());
    }

    @Override
    public void onSerialPortReadDataSuccess() {
        runOnUiThread(() -> Toast.makeText(Main2Activity.this,"串口读取数据成功",Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSerialPortReadDataFail(String msg) {
        runOnUiThread(() -> Toast.makeText(Main2Activity.this,msg,Toast.LENGTH_SHORT).show());
    }

    //----------------------------------------------------刷新显示线程
    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    runOnUiThread(() -> DispRecData(ComData));
                    try {
                        Thread.sleep(100);//显示性能高的话，可以把此数值调小。
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        synchronized void AddQueue(ComBean comBean) {
            QueueList.add(comBean);
        }
    }

    private int count = 0;

    //----------------------------------------------------显示接收数据
    private void DispRecData(ComBean comBean) {
        StringBuilder sMsg = new StringBuilder();
        sMsg.append(comBean.sRecTime);
        sMsg.append("[");
        sMsg.append(comBean.sComPort);
        sMsg.append("]");
        if (revType == 0) {
            sMsg.append("[Hex] ");
            String hex = MyFunc.ByteArrToHex(comBean.bRec);
            String[] split = hex.split(" ");
            sMsg.append(hex);
            sMsg.append("------->").append(count).append("------->size:").append(split.length);
            Log.w(TAG, hex);
        } else {
            sMsg.append("[Text] ");
            String text = new String(comBean.bRec);
            sMsg.append(text);
            sMsg.append("------->").append(count).append("------->size:").append(text.length());
            Log.w(TAG, text);
        }
        count++;
        if (comBean.mRecOrSend == 0) {
            sMsg.append("---[收]");
        } else if (comBean.mRecOrSend == 1) {
            sMsg.append("---[发]");
        }
        sMsg.append("\r\n");
        mEditTextRecDisp.append(sMsg);
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

    private static class MyHandler extends Handler {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyHandler != null) {
            mMyHandler.removeCallbacksAndMessages(null);
            mMyHandler = null;
        }
        try {
            DeviceEngine.getInstance().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
