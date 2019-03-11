package com.xiaolan.serialporttest

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.xiaolan.serialporttest.util1.SerialPortHelper
import kotlinx.android.synthetic.main.activity_water.*

class WaterActivity : AppCompatActivity(), View.OnClickListener {
    private var serialPortHelper:SerialPortHelper? = null
    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.open->{
                serialPortHelper?.sendTxt("A")
            }
            R.id.close->{
                serialPortHelper?.sendTxt("a")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water)
        open.setOnClickListener(this)
        close.setOnClickListener(this)
        serialPortHelper = SerialPortHelper()
        serialPortHelper?.port = "/dev/ttyS3"
        serialPortHelper?.baudRate = 9600
        serialPortHelper?.open()
    }

    override fun onDestroy() {
        super.onDestroy()
        serialPortHelper?.close()
    }
}
