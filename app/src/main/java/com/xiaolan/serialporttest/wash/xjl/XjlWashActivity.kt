package com.xiaolan.serialporttest.wash.xjl

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.xiaolan.serialporttest.R
import com.xiaolan.serialporttest.mylib.DeviceAction
import com.xiaolan.serialporttest.mylib.DeviceEngine
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener
import com.xiaolan.serialporttest.mylib.utils.MyFunc
import com.xiaolan.serialporttest.util1.KeybordUtils
import kotlinx.android.synthetic.main.activity_xjl_wash.*
import java.io.IOException
import java.util.*

class XjlWashActivity : AppCompatActivity(), View.OnClickListener, OnSendInstructionListener, CurrentStatusListener, SerialPortOnlineListener {
    companion object {
        private const val TAG = "XjlWashActivity"
    }

    private var mSelectMode: Int = -1

    private var mSuper: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xjl_wash)
        KeybordUtils.isSoftInputShow(this)
        cb_hex_rev.isChecked = true

        btn_finsh.setOnClickListener(this)
        btn_open_port.setOnClickListener(this)
        btn_hot.setOnClickListener(this)
        btn_warm.setOnClickListener(this)
        btn_soft.setOnClickListener(this)
        btn_cold.setOnClickListener(this)
        btn_open_port.setOnClickListener(this)
        btn_setting.setOnClickListener(this)
        btn_super.setOnClickListener(this)
        btn_start_stop.setOnClickListener(this)
        btn_kill.setOnClickListener(this)
        btn_clear.setOnClickListener(this)
        btn_self_cleaning.setOnClickListener(this)

        mDispQueueThread2 = DispQueueThread2()
        mDispQueueThread2?.start()
//        mPortFinder = new SerialPortFinder();
        //设置发送指令监听
        DeviceEngine.getInstance().setOnSendInstructionListener(this)
        //设置上报状态监听
        DeviceEngine.getInstance().setOnCurrentStatusListener(this)
        //设置读取串口数据监听
        DeviceEngine.getInstance().setOnSerialPortOnlineListener(this)
    }

    override fun onSerialPortOnline(bytes: ByteArray?) {
        val s = MyFunc.ByteArrToHex(bytes)
        Log.e(TAG, "串口上线:$s")
        Toast.makeText(this, "串口上线$s", Toast.LENGTH_SHORT).show()
    }

    override fun onSerialPortOffline(msg: String?) {
        Log.e(TAG, msg)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun currentStatus(washStatusEvent: Any?) {
        if (washStatusEvent is WashStatusEvent) {
            Log.e(TAG, "屏显：${washStatusEvent.text}")
        }
    }

    override fun sendInstructionSuccess(key: Int, washStatusEvent: WashStatusEvent?) {
        when (key) {
            1 -> Log.e(TAG, "开始指令成功")
            2 -> Log.e(TAG, "热水指令成功")
            3 -> Log.e(TAG, "温水指令成功")
            4 -> Log.e(TAG, "冷水指令成功")
            5 -> Log.e(TAG, "精致衣物指令成功")
            6 -> Log.e(TAG, "加强洗指令成功")
            8 -> Log.e(TAG, "设置指令成功")
            10 -> Log.e(TAG, "kill指令成功")
        }
        washStatusEvent?.let { mDispQueueThread2?.addQueue(it) }//线程定时刷新显示
    }

    override fun sendInstructionFail(key: Int, message: String?) {
        when (key) {
            1 -> Log.e(TAG, "开始指令失败")
            2 -> Log.e(TAG, "热水指令失败")
            3 -> Log.e(TAG, "温水指令失败")
            4 -> Log.e(TAG, "冷水指令失败")
            5 -> Log.e(TAG, "精致衣物指令失败")
            6 -> Log.e(TAG, "加强洗指令失败")
            8 -> Log.e(TAG, "设置指令失败")
            10 -> Log.e(TAG, "kill指令失败")
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
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
            R.id.btn_clear -> {
                //清空数据接收区
                editTextRecDisp.setText("")
            }
            R.id.btn_hot -> {
                checkIsOpen()
                mSelectMode = 0
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE1)
            }
            R.id.btn_warm -> {
                checkIsOpen()
                mSelectMode = 1
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE2)
            }
            R.id.btn_cold -> {
                checkIsOpen()
                mSelectMode = 2
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE3)
            }
            R.id.btn_soft -> {
                checkIsOpen()
                mSelectMode = 3
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE4)
            }
            R.id.btn_super -> {
                checkIsOpen()
                mSuper = !mSuper
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SUPER)
            }
            R.id.btn_start_stop -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_START)
            }
            R.id.btn_setting -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SETTING)
            }
            R.id.btn_kill -> {
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_KILL)
            }
            R.id.btn_reset->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_RESET)
            }
            R.id.btn_self_cleaning->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SELF_CLEANING)
            }
            R.id.btn_finsh -> {
                val open = DeviceEngine.getInstance().isOpen
                if (open) {
                    try {
                        DeviceEngine.getInstance().close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } finally {
                        finish()
                    }
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

    private var mDispQueueThread2: DispQueueThread2? = null

    //----------------------------------------------------刷新显示线程
    private inner class DispQueueThread2 : Thread() {
        private val queueList = LinkedList<WashStatusEvent>()

        override fun run() {
            super.run()
            while (!isInterrupted) {
                val washStatusEvent: WashStatusEvent? = queueList.poll()
                while (washStatusEvent != null) {
                    runOnUiThread { dispRecData2(washStatusEvent) }
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
        internal fun addQueue(washStatusEvent: WashStatusEvent) {
            queueList.add(washStatusEvent)
        }
    }

    //----------------------------------------------------显示接收数据
    private fun dispRecData2(washStatusEvent: WashStatusEvent) {
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
