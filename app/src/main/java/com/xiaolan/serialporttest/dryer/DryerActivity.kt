package com.xiaolan.serialporttest.dryer

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.xiaolan.serialporttest.R
import com.xiaolan.serialporttest.dryer.DryerSerialPortHelper.*
import com.xiaolan.serialporttest.mylib.DeviceAction
import com.xiaolan.serialporttest.mylib.DeviceEngine
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener
import com.xiaolan.serialporttest.mylib.utils.MyFunc
import com.xiaolan.serialporttest.util1.KeybordUtils
import com.xiaolan.serialporttest.util1.ToastUtil
import kotlinx.android.synthetic.main.activity_dryer.*
import java.io.IOException

/**
 * 烘干机测试Activity
 */
class DryerActivity : AppCompatActivity(), CurrentStatusListener, OnSendInstructionListener, SerialPortOnlineListener, View.OnClickListener {

    companion object {
        private const val TAG = "DryerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dryer)
        KeybordUtils.isSoftInputShow(this)

        btn_open_port.setOnClickListener(this)
        btn_high.setOnClickListener(this)
        btn_med.setOnClickListener(this)
        btn_low.setOnClickListener(this)
        btn_no_heat.setOnClickListener(this)
        btn_start.setOnClickListener(this)
        btn_setting.setOnClickListener(this)
        btn_kill.setOnClickListener(this)
        btn_clear.setOnClickListener(this)
        btn_finsh.setOnClickListener(this)
        DeviceEngine.getInstance().setOnCurrentStatusListener(this)
        DeviceEngine.getInstance().setOnSendInstructionListener(this)
        DeviceEngine.getInstance().setOnSerialPortOnlineListener(this)
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_open_port->{
                //打开/关闭串口
                if (btn_open_port.getText() == "打开串口") {
                    try {
                        DeviceEngine.getInstance().open()
                        btn_open_port.text = "关闭串口"
                    } catch (e: Exception) {
                        e.printStackTrace()
                        btn_open_port.text = "打开串口"
                    }

                } else {
                    try {
                        DeviceEngine.getInstance().close()
                        btn_open_port.text = "打开串口"
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }
            R.id.btn_high->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_HIGH,8)
            }
            R.id.btn_med->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_MED,8)
            }
            R.id.btn_low->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_LOW,8)
            }
            R.id.btn_no_heat->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_NOHEAT,8)
            }
            R.id.btn_start->{}
            R.id.btn_setting->{ }
            R.id.btn_kill->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_KILL,0)
            }
            R.id.btn_clear->{
                editTextRecDisp.setText("")
            }
            R.id.btn_finsh->{
                finish()
            }
        }
    }

    override fun onSerialPortOnline(bytes: ByteArray?) {
        val s = MyFunc.ByteArrToHex(bytes)
        Log.e(TAG, "串口上线:$s")
        ToastUtil.show("串口上线$s")
    }

    override fun onSerialPortOffline(msg: String?) {
        Log.e(TAG, msg)
        ToastUtil.show(msg)
    }

    override fun sendInstructionSuccess(key: Int, washStatusEvent: WashStatusEvent?) {
        when(key) {
            DeviceAction.Dryer.ACTION_KILL->{
                ToastUtil.show("Kill Success!!!")
                Log.e(TAG,"Kill Success!!!")
            }
        }
    }

    override fun sendInstructionFail(key: Int, message: String?) {

    }

    override fun currentStatus(statusEvent: Any?) {
        if (statusEvent is Int) {
            when (statusEvent) {
                STATE_FREE -> {
                    Log.e(TAG, "STATE_FREE")
                }
                STATE_END -> {
                    Log.e(TAG, "STATE_END")
                }
                STATE_OPEN -> {
                    Log.e(TAG, "STATE_OPEN")
                }
                STATE_RUN -> {
                    Log.e(TAG, "STATE_RUN")
                }
                STATE_UNKNOWN -> {
                    Log.e(TAG, "STATE_UNKNOWN")
                }
            }
        }
    }

    private fun checkIsOpen() {
        if (!DeviceEngine.getInstance().isOpen) {
            try {
                DeviceEngine.getInstance().open()
                btn_open_port.text = "关闭串口"
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            DeviceEngine.getInstance().close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
