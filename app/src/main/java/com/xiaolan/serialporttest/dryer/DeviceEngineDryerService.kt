package com.xiaolan.serialporttest.dryer

import com.xiaolan.serialporttest.mylib.listener.CurrentStatusListener
import com.xiaolan.serialporttest.mylib.listener.OnSendInstructionListener
import com.xiaolan.serialporttest.mylib.listener.SerialPortOnlineListener
import java.io.IOException

interface DeviceEngineDryerService {
    @Throws(IOException::class)
    abstract fun open()

    abstract fun setOnSendInstructionListener(onSendInstructionListener: OnSendInstructionListener)

    abstract fun setOnCurrentStatusListener(currentStatusListener: CurrentStatusListener)

    abstract fun setOnSerialPortOnlineListener(onSerialPortOnlineListener: SerialPortOnlineListener)

    abstract fun isOpen(): Boolean

    @Throws(IOException::class)
    abstract fun close()

    @Throws(IOException::class)
    abstract fun push(action: Int,coin :Int)
}