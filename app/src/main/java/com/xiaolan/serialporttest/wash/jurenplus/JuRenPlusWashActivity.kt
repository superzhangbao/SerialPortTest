package com.xiaolan.serialporttest.wash.jurenplus

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import com.xiaolan.iot.IotClient
import com.xiaolan.serialporttest.App
import com.xiaolan.serialporttest.R
import com.xiaolan.serialporttest.mylib.DeviceAction
import com.xiaolan.serialporttest.mylib.DeviceEngine
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener
import com.xiaolan.serialporttest.mylib.utils.MyFunc
import com.xiaolan.serialporttest.util1.KeybordUtils
import com.xiaolan.serialporttest.util1.ToastUtil
import kotlinx.android.synthetic.main.activity_ju_ren_plus_wash.*
import java.io.IOException
import java.util.*

/**
 *巨人+Activity
 */
class JuRenPlusWashActivity : AppCompatActivity(), RadioGroup.OnCheckedChangeListener, OnSendInstructionListener, CurrentStatusListener, SerialPortOnlineListener, View.OnClickListener {
    companion object {
        const val TAG: String = "JuRenPlusWashActivity"
    }

    private var revType = 0
    private val mButtons = ArrayList<Button>()
    private var mDispQueueThread: DispQueueThread? = null
    private var mWashStatusEvent: WashStatusEvent? = null
    private val ordernumber: String = "Ojrp1234567" +
            ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ju_ren_plus_wash)
        KeybordUtils.isSoftInputShow(this)

        cb_hex_rev.isChecked = true

        mButtons.add(btn_hot)
        mButtons.add(btn_warm)
        mButtons.add(btn_cold)
        mButtons.add(btn_soft)

        btn_finsh.setOnClickListener(this)
        btn_open_port.setOnClickListener(this)
        btn_clear.setOnClickListener(this)
        btn_hot.setOnClickListener(this)
        btn_warm.setOnClickListener(this)
        btn_cold.setOnClickListener(this)
        btn_soft.setOnClickListener(this)
        btn_super.setOnClickListener(this)
        btn_start_stop.setOnClickListener(this)
        btn_kill.setOnClickListener(this)
        btn_setting.setOnClickListener(this)
        btn_reset.setOnClickListener(this)
        btn_self_cleaning.setOnClickListener(this)
        rg_rev.setOnCheckedChangeListener(this)

        mDispQueueThread = DispQueueThread()
        mDispQueueThread?.start()
        //设置发送指令监听
        DeviceEngine.getInstance().setOnSendInstructionListener(this)
        //设置上报状态监听
        DeviceEngine.getInstance().setOnCurrentStatusListener(this)
        //设置读取串口数据监听
        DeviceEngine.getInstance().setOnSerialPortOnlineListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_finsh -> {
                val open = DeviceEngine.getInstance().isOpen
                if (open) {
                    DeviceEngine.getInstance().close()
                    finish()
                }
            }
            R.id.btn_open_port -> {
                //打开/关闭串口
                if (btn_open_port.text == "打开串口") {
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
            R.id.btn_clear -> {//清空数据接收区
                editTextRecDisp.setText("")
            }
            R.id.btn_hot -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_HOT)
            }
            R.id.btn_warm -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_WARM)
            }
            R.id.btn_cold -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_COLD)
            }
            R.id.btn_soft -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_DELICATES)
            }
            R.id.btn_super -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SUPER)
            }
            R.id.btn_start_stop -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_START)
            }
            R.id.btn_kill -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_KILL)
            }
            R.id.btn_setting -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SETTING)
            }
            R.id.btn_reset -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_RESET)
            }
            R.id.btn_self_cleaning -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING)
            }
        }
    }

    override fun onSerialPortOnline(bytes: ByteArray?) {
        val s = MyFunc.ByteArrToHex(bytes)
        Log.e(TAG, "串口上线:$s")
        ToastUtil.show("串口上线$s")
        IotClient.getInstance().uploadRestoreError(App.productKey, App.deviceName, ordernumber)
    }

    override fun onSerialPortOffline(msg: String?) {
        Log.e(TAG, msg)
        ToastUtil.show(msg)
        IotClient.getInstance().uploadDxError(App.productKey, App.deviceName, ordernumber)
    }

    override fun currentStatus(washStatusEvent: Any?) {
        if (washStatusEvent is WashStatusEvent) {
            mWashStatusEvent = washStatusEvent
            val washMode = washStatusEvent.washMode
            val lightSupper = washStatusEvent.lightSupper
            val viewStep = washStatusEvent.viewStep
            val err = washStatusEvent.err
            val text = washStatusEvent.text
            val text2 = washStatusEvent.text2
            val isRunning = washStatusEvent.isRunning
            val isError = washStatusEvent.isError
            val isWashing = washStatusEvent.isWashing
            val period = washStatusEvent.period//洗衣阶段
            //IOT去执行解析数据,上报数据
            IotClient.getInstance().uploadWashRunning("1.0", isWashing, isRunning, isError, period,
                    washMode, lightSupper, viewStep, err, text, text2, ordernumber, App.productKey, App.deviceName)
            Log.e(TAG, "屏显：" + washStatusEvent.logmsg.toString())
            mDispQueueThread?.AddQueue(washStatusEvent)
        }
    }

    override fun sendInstructionSuccess(key: Int, `object`: Any?) {
        if (`object` is WashStatusEvent) {
            var washStatusEvent = `object`
            when (key) {
                DeviceAction.JuRenPlus.ACTION_START -> {
                    Log.e(TAG, "开始指令成功")
                    ToastUtil.show("开始指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_HOT -> {
                    Log.e(TAG, "热水指令成功")
                    ToastUtil.show("热水指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_WARM -> {
                    Log.e(TAG, "温水指令成功")
                    ToastUtil.show("温水指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_COLD -> {
                    Log.e(TAG, "冷水指令成功")
                    ToastUtil.show("冷水指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_DELICATES -> {
                    Log.e(TAG, "精致衣物指令成功")
                    ToastUtil.show("精致衣物指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_SUPER -> {
                    Log.e(TAG, "加强洗指令成功")
                    ToastUtil.show("加强洗指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_SETTING -> {
                    Log.e(TAG, "设置指令成功")
                    ToastUtil.show("设置指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_RESET -> {
                    Log.e(TAG, "复位指令成功")
                    ToastUtil.show("复位指令成功")
                }
                DeviceAction.JuRenPlus.ACTION_KILL -> {
                    Log.e(TAG, "kill指令成功")
                    ToastUtil.show("kill指令成功")
                }
            }
            `object`.let { mDispQueueThread?.AddQueue(washStatusEvent) }//线程定时刷新显示
        }
    }

    override fun sendInstructionFail(key: Int, message: String?) {
        when (key) {
            1 -> {
                Log.e(TAG, "开始指令失败")
                ToastUtil.show("开始指令失败")
            }
            2 -> {
                Log.e(TAG, "热水指令失败")
                ToastUtil.show("热水指令失败")
            }
            3 -> {
                Log.e(TAG, "温水指令失败")
                ToastUtil.show("温水指令失败")
            }
            4 -> {
                Log.e(TAG, "冷水指令失败")
                ToastUtil.show("冷水指令失败")
            }
            5 -> {
                Log.e(TAG, "精致衣物指令失败")
                ToastUtil.show("精致衣物指令失败")
            }
            6 -> {
                Log.e(TAG, "加强洗指令失败")
                ToastUtil.show("加强洗指令失败")
            }
            8 -> {
                Log.e(TAG, "设置指令失败")
                ToastUtil.show("设置指令失败")
            }
            10 -> {
                Log.e(TAG, "kill指令失败")
                ToastUtil.show("kill指令失败")
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

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (checkedId == cb_hex_rev.id) {
            revType = 0
        } else if (checkedId == cb_text_rev.id) {
            revType = 1
        }
    }

    //----------------------------------------------------刷新显示线程
    private inner class DispQueueThread : Thread() {
        private val QueueList = LinkedList<WashStatusEvent>()

        override fun run() {
            super.run()
            while (!isInterrupted) {
                while (QueueList.poll() != null) {
                    runOnUiThread { mWashStatusEvent?.let { DispRecData2(it) } }
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
        internal fun AddQueue(washStatusEvent: WashStatusEvent) {
            QueueList.add(washStatusEvent)
        }
    }

    //----------------------------------------------------显示接收数据
    private fun DispRecData2(washStatusEvent: WashStatusEvent) {
        editTextRecDisp.append(washStatusEvent.logmsg)
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
