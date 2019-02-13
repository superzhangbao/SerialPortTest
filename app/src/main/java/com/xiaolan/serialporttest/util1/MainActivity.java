package com.xiaolan.serialporttest.util1;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.xiaolan.serialporttest.wash.jurenpro.JuRenProWashActivity;
import com.xiaolan.serialporttest.R;
import com.xiaolan.serialporttest.mylib.utils.ComBean;
import com.xiaolan.serialporttest.mylib.utils.MyFunc;
import com.xiaolan.serialporttest.util1.SerialPortManager.OnDataSendListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android_serialport_api.SerialPortFinder;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, SerialPortManager.OnDataReceivedListener, OnDataSendListener {
    private static final String TAG = "MainActivity";
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
    @BindView(R.id.btn_writelog)
    Button mBtnWritelog;

    private MyHandler mMyHandler;
    private int sendType = 0;
    private int revType = 0;
    private SerialPortManager mPortHelper;
    private DispQueueThread mDispQueueThread;
    private SerialPortFinder mPortFinder;
    private ArrayList<Button> mButtons = new ArrayList<>();
    private int mSelectMode = -1;
    private boolean mSuper = false;
    private int mBtnStatus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        KeybordUtils.isSoftInputShow(this);
        mEt.clearFocus();

        mCbHex.setChecked(true);
        mCbHexRev.setChecked(true);

        mRg.setOnCheckedChangeListener(this);
        mRgRev.setOnCheckedChangeListener(this);
        mMyHandler = new MyHandler();
        mDispQueueThread = new DispQueueThread();
        mDispQueueThread.start();
        mPortFinder = new SerialPortFinder();
        mPortHelper = new SerialPortManager();
        mPortHelper.setOnDataReceivedListener(this);
        mPortHelper.setOnDataSendListener(this);

        mButtons.add(mBtnHot);
        mButtons.add(mBtnWarm);
        mButtons.add(mBtnCold);
        mButtons.add(mBtnSoft);
    }

    public void notifySystemToScan() {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(Environment.getExternalStorageDirectory().getPath());
        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        sendBroadcast(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("SetTextI18n")
    @OnClick({R.id.btn_writelog, R.id.btn_activity2, R.id.btn_set_port, R.id.btn_set_baud, R.id.btn_set_data, R.id.btn_set_stop,
            R.id.btn_set_verify, R.id.btn_open_port, R.id.btn_clear, R.id.btn_send, R.id.btn_hot,
            R.id.btn_warm, R.id.btn_cold, R.id.btn_soft, R.id.btn_super, R.id.btn_start_stop,
            R.id.btn_kill, R.id.btn_wash, R.id.btn_setting})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_writelog:
                boolean writeLog = mPortHelper.isWriteLog();
                writeLog = !writeLog;
                if (writeLog) {
                    mPortHelper.writeLog();
                    mBtnWritelog.setText("停止记录报文");
                } else {
                    mPortHelper.stopWriteLog();
                    mBtnWritelog.setText("开始记录报文");
                }
                break;
            case R.id.btn_activity2:
                startActivity(new Intent(this, JuRenProWashActivity.class));
                break;
            case R.id.btn_set_port:
                new AlertDialog.Builder(this)
                        .setTitle("选择串口")//设置对话框标题
                        .setIcon(android.R.drawable.ic_menu_info_details)//设置对话框图标
                        .setSingleChoiceItems(mPortFinder.getAllDevices(), 0, (dialog, which) -> {
                            String device = mPortFinder.getAllDevicesPath()[which];
                            mBtnSetPort.setText(device);
                            mPortHelper.setPort(device);
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
                            mPortHelper.setBaudRate(baudRate);
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
                            mPortHelper.setDataBits(dataBits);
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
                            mPortHelper.setStopBits(stopBits);
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
                            mPortHelper.setParityBits(parityBits);
                            dialog.dismiss();
                        })
                        .show();
                break;
            case R.id.btn_open_port:
                //打开/关闭串口
                if (mBtnOpenPort.getText().equals("打开串口")) {
                    try {
                        mBtnSetPort.setText(mPortHelper.getPort());
                        mBtnSetBaud.setText(String.valueOf(mPortHelper.getBaudRate()));
                        mBtnSetData.setText(mPortHelper.getDataBits());
                        mBtnSetStop.setText(mPortHelper.getStopBits());
                        mBtnSetVerify.setText(mPortHelper.getParityBits());
                        mPortHelper.open();
                        mBtnOpenPort.setText("关闭串口");
                    } catch (Exception e) {
                        e.printStackTrace();
                        mBtnOpenPort.setText("打开串口");
                    }
                } else {
                    mPortHelper.close();
                    mBtnOpenPort.setText("打开串口");
                    notifySystemToScan();
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
                if (mPortHelper != null && !mPortHelper.isOpen()) {
                    Toast.makeText(this, "请打开串口", Toast.LENGTH_SHORT).show();
                    return;
                }
                String trim = mEt.getText().toString().trim();
                if (!TextUtils.isEmpty(trim)) {
                    if (sendType == 0) {
                        mPortHelper.sendHex(trim);
                    } else {
                        mPortHelper.sendTxt(trim);
                    }
                } else {
                    Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_hot:
                mSelectMode = 0;
                setBackgroundColor(R.id.btn_hot);
                mPortHelper.sendHot();
                break;
            case R.id.btn_warm:
                mSelectMode = 1;
                setBackgroundColor(R.id.btn_warm);
                mPortHelper.sendWarm();
                break;
            case R.id.btn_cold:
                mSelectMode = 2;
                setBackgroundColor(R.id.btn_cold);
                mPortHelper.sendCold();
                break;
            case R.id.btn_soft:
                mSelectMode = 3;
                setBackgroundColor(R.id.btn_soft);
                mPortHelper.sendSoft();
                break;
            case R.id.btn_super:
                mSuper = !mSuper;
                setBackgroundColor(R.id.btn_super);
                mPortHelper.sendSuper();
                break;
            case R.id.btn_start_stop:
                if (mBtnStatus == 0) {
                    mBtnStatus = 1;
                } else if (mBtnStatus == 1) {
                    mBtnStatus = 2;
                } else if (mBtnStatus == 2) {
                    mBtnStatus = 1;
                }
                setBackgroundColor(R.id.btn_start_stop);
                mPortHelper.sendStartOrStop();
                break;
            case R.id.btn_setting:
                mPortHelper.sendSetting();
                break;
            case R.id.btn_kill:
                setBackgroundColor(R.id.btn_kill);
                mPortHelper.sendKill();
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
    public void onDataReceived(final ComBean comBean) {
        runOnUiThread(() -> {
            mDispQueueThread.AddQueue(comBean);//线程定时刷新显示
        });
    }

    @Override
    public void onDataSendSuccess(final ComBean comBean) {
        runOnUiThread(() -> {
            mDispQueueThread.AddQueue(comBean);//线程定时刷新显示
        });
    }

    @Override
    public void onDataSendFail(final IOException e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    runOnUiThread(new Runnable() {
                        public void run() {
                            DispRecData(ComData);
                        }
                    });
                    try {
                        Thread.sleep(50);//显示性能高的话，可以把此数值调小。
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
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

    private static class MyHandler extends Handler {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMyHandler != null) {
            mMyHandler.removeCallbacksAndMessages(null);
            mMyHandler = null;
        }
        mPortHelper.close();
    }
}
