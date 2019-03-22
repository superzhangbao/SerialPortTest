package com.xiaolan.serialporttest.dryer

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.xiaolan.serialporttest.R
import com.xiaolan.serialporttest.mylib.DeviceEngine
import com.xiaolan.serialporttest.mylib.event.WashStatusEvent
import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener
import com.xiaolan.serialporttest.mylib.utils.MyFunc
import com.xiaolan.serialporttest.util1.KeybordUtils
import com.xiaolan.serialporttest.util1.ToastUtil
import com.xiaolan.serialporttest.wash.jurenplus.JuRenPlusWashActivity
import kotlinx.android.synthetic.main.activity_ju_ren_plus_wash.*
import java.io.IOException
import java.util.*

class DryerActivity : AppCompatActivity(), CurrentStatusListener, OnSendInstructionListener, SerialPortOnlineListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dryer)
        KeybordUtils.isSoftInputShow(this)
        DeviceEngine.getInstance().setOnCurrentStatusListener(this)
        DeviceEngine.getInstance().setOnSendInstructionListener(this)
        DeviceEngine.getInstance().setOnSerialPortOnlineListener(this)
    }

    override fun onSerialPortOnline(bytes: ByteArray?) {
        val s = MyFunc.ByteArrToHex(bytes)
        Log.e(JuRenPlusWashActivity.TAG, "串口上线:$s")
        ToastUtil.show("串口上线$s")
    }

    override fun onSerialPortOffline(msg: String?) {
        Log.e(JuRenPlusWashActivity.TAG, msg)
        ToastUtil.show(msg)
    }

    override fun sendInstructionSuccess(key: Int, washStatusEvent: WashStatusEvent?) {

    }

    override fun sendInstructionFail(key: Int, message: String?) {

    }

    override fun currentStatus(washStatusEvent: WashStatusEvent?) {
        if (washStatusEvent != null) {
            Log.e(JuRenPlusWashActivity.TAG, "屏显：" + washStatusEvent.logmsg.toString())
        }
    }

    //----------------------------------------------------刷新显示线程
    private inner class DispQueueThread : Thread() {
        private val QueueList = LinkedList<WashStatusEvent>()

        override fun run() {
            super.run()
            while (!isInterrupted) {
                while (QueueList.poll() != null) {
                    runOnUiThread {
//                        mWashStatusEvent?.let { DispRecData2(it) }
                    }
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

    private fun checkIsOpen() {
        if (!DeviceEngine.getInstance().isOpen) {
            try {
                DeviceEngine.getInstance().open()
                btn_open_port.setText("关闭串口")
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
