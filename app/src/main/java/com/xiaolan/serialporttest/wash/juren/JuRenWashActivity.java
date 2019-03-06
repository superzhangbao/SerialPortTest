package com.xiaolan.serialporttest.wash.juren;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.xiaolan.serialporttest.R;
import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener;
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener;
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener;
import com.xiaolan.serialporttest.mylib.utils.MyFunc;
import com.xiaolan.serialporttest.util1.ToastUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 巨人洗衣机页面
 */
public class JuRenWashActivity extends AppCompatActivity implements OnSendInstructionListener, SerialPortOnlineListener, CurrentStatusListener {

    private static final String TAG = "JuRenWashActivity";
    @BindView(R.id.btn_finsh)
    Button mBtnFinsh;
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
    @BindView(R.id.btn_whites)
    Button mBtnWhites;
    @BindView(R.id.btn_colors)
    Button mBtnColors;
    @BindView(R.id.btn_delicates)
    Button mBtnDelicates;
    @BindView(R.id.btn_perm_press)
    Button mBtnPress;
    @BindView(R.id.btn_super)
    Button mBtnSuper;
    @BindView(R.id.btn_start)
    Button mBtnStart;
    @BindView(R.id.btn_setting)
    Button mBtnSetting;
    @BindView(R.id.btn_kill)
    Button mBtnKill;

    private DispQueueThread2 mDispQueueThread2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ju_ren_wash);
        ButterKnife.bind(this);
        mDispQueueThread2 = new DispQueueThread2();
        mDispQueueThread2.start();
        DeviceEngine.getInstance().setOnSendInstructionListener(this);
        DeviceEngine.getInstance().setOnSerialPortOnlineListener(this);
        DeviceEngine.getInstance().setOnCurrentStatusListener(this);
    }

    @Override
    public void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent) {
        switch (key) {
            case 1:
                Log.e(TAG, "开始指令成功");
                ToastUtil.show("开始指令成功");
                break;
            case 2:
                Log.e(TAG, "whites指令成功");
                ToastUtil.show("whites指令成功");
                break;
            case 3:
                Log.e(TAG, "colors指令成功");
                ToastUtil.show("colors指令成功");
                break;
            case 4:
                Log.e(TAG, "delicates指令成功");
                ToastUtil.show("delicates指令成功");
                break;
            case 5:
                Log.e(TAG, "perm.press指令成功");
                ToastUtil.show("perm.press指令成功");
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
                ToastUtil.show("开始指令失败");
                break;
            case 2:
                Log.e(TAG, "热水指令失败");
                ToastUtil.show("热水指令失败");
                break;
            case 3:
                Log.e(TAG, "温水指令失败");
                ToastUtil.show("温水指令失败");
                break;
            case 4:
                Log.e(TAG, "冷水指令失败");
                ToastUtil.show("冷水指令失败");
                break;
            case 5:
                Log.e(TAG, "精致衣物指令失败");
                ToastUtil.show("精致衣物指令失败");
                break;
            case 6:
                Log.e(TAG, "加强洗指令失败");
                ToastUtil.show("加强洗指令失败");
                break;
            case 8:
                Log.e(TAG, "设置指令失败");
                ToastUtil.show("设置指令失败");
                break;
            case 10:
                Log.e(TAG, "kill指令失败");
                ToastUtil.show("kill指令失败");
                break;
        }
    }

    @Override
    public void currentStatus(WashStatusEvent washStatusEvent) {
        if (washStatusEvent != null) {
            Log.e(TAG, "屏显：" + washStatusEvent.getText());
            mDispQueueThread2.AddQueue(washStatusEvent);//线程定时刷新显示
        }
    }

    @OnClick({R.id.btn_finsh, R.id.btn_open_port, R.id.btn_clear, R.id.btn_whites, R.id.btn_colors,
            R.id.btn_delicates, R.id.btn_perm_press, R.id.btn_super, R.id.btn_start, R.id.btn_setting, R.id.btn_kill})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_finsh:
                finish();
                break;
            case R.id.btn_open_port:
                if (DeviceEngine.getInstance().isOpen()) {
                    try {
                        DeviceEngine.getInstance().close();
                        mBtnOpenPort.setText("打开串口");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
                    try {
                        DeviceEngine.getInstance().open();
                        mBtnOpenPort.setText("关闭串口");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "打开串口异常");
                        Toast.makeText(this, "打开串口异常", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btn_clear:
                //清空数据接收区
                mEditTextRecDisp.setText("");
                break;
            case R.id.btn_whites:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_WHITES);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_colors:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_COLORS);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_delicates:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_DELICATES);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_perm_press:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_PERM_PRESS);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_super:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_SUPER);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_start:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_START);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_setting:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_SETTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_kill:
                try {
                    DeviceEngine.getInstance().push(DeviceAction.JuRen.ACTION_KILL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onSerialPortOnline(byte[] bytes) {
        String s = MyFunc.ByteArrToHex(bytes);
        Log.e(TAG, "串口上线:" + s);
        Toast.makeText(this, "串口上线" + s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSerialPortOffline(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        try {
            DeviceEngine.getInstance().close();
        } catch (IOException e) {
            Toast.makeText(this, "关闭串口异常", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "关闭串口异常");
            e.printStackTrace();
        }
    }
}
