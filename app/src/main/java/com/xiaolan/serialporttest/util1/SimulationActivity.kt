package com.xiaolan.serialporttest.util1

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.xiaolan.serialporttest.R
import kotlinx.android.synthetic.main.activity_simulation.*

class SimulationActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_hot-> Toast.makeText(this,btn_hot.text,Toast.LENGTH_SHORT).show()
            R.id.btn_warm-> Toast.makeText(this,btn_warm.text,Toast.LENGTH_SHORT).show()
            R.id.btn_cold->Toast.makeText(this,btn_cold.text,Toast.LENGTH_SHORT).show()
            R.id.btn_soft->Toast.makeText(this,btn_soft.text,Toast.LENGTH_SHORT).show()
            R.id.btn_super->Toast.makeText(this,btn_super.text,Toast.LENGTH_SHORT).show()
            R.id.btn_start_stop->Toast.makeText(this,btn_start_stop.text,Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        btn_hot.setOnClickListener(this)
        btn_warm.setOnClickListener(this)
        btn_cold.setOnClickListener(this)
        btn_soft.setOnClickListener(this)
        btn_super.setOnClickListener(this)
        btn_start_stop.setOnClickListener(this)
    }
}
