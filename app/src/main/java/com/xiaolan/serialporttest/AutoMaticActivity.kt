package com.xiaolan.serialporttest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.xiaolan.serialporttest.mylib.DeviceAction
import com.xiaolan.serialporttest.mylib.DeviceEngine
import com.xiaolan.serialporttest.util1.ToastUtil
import kotlinx.android.synthetic.main.activity_auto_matic.*
import kotlinx.android.synthetic.main.activity_dryer.view.*
import java.io.IOException

/**
 * 自动识别页面
 */
class AutoMaticActivity : AppCompatActivity(), SelectDeviceHelper.OnCheckDeviceListener, View.OnClickListener {
    private var mSuper: Boolean = false
    private var mType:Int = -100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto_matic)
        tv_type.text = "正在识别..."

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

        btn_high.setOnClickListener(this)
        btn_med.setOnClickListener(this)
        btn_low.setOnClickListener(this)
        btn_no_heat.setOnClickListener(this)
        btn_start.setOnClickListener(this)
        btn_dryer_setting.setOnClickListener(this)
        btn_dryer_kill.setOnClickListener(this)
        btn_init.setOnClickListener(this)
        btn_noheat_start.setOnClickListener(this)
        //自动识别机器
        automatic()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btn_finsh->{
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
            R.id.btn_open_port->{
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
            R.id.btn_clear->{
                //清空数据接收区
                editTextRecDisp.setText("")
            }
            R.id.btn_hot->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_HOT)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE1)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_HOT)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_warm->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_WARM)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE2)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_WARM)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_cold->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_COLD)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE3)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_COLD)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_soft->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_DELICATES)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_MODE4)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_DELICATES)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_super->{
                checkIsOpen()
                mSuper = !mSuper
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SUPER)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SUPER)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SUPER)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_start_stop->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_START)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_START)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_START)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_setting->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SETTING)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SETTING)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SETTING)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_kill->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_KILL)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_KILL)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_KILL)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_reset->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_RESET)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_RESET)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_RESET)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_self_cleaning->{
                checkIsOpen()
                when(mType) {
                    0->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPro.ACTION_SELF_CLEANING)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    2->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.Xjl.ACTION_SELF_CLEANING)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    3->{
                        try {
                            DeviceEngine.getInstance().push(DeviceAction.JuRenPlus.ACTION_SELF_CLEANING)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            R.id.btn_high->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_HIGH,0)
            }
            R.id.btn_med->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_MED,0)
            }
            R.id.btn_low->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_LOW,0)
            }
            R.id.btn_no_heat->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_NOHEAT,0)
            }
            R.id.btn_start->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_START,0)
            }
            R.id.btn_dryer_setting->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_SETTING,0)
            }
            R.id.btn_dryer_kill->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_KILL,0)
            }
            R.id.btn_init->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_INIT,0)
            }
            R.id.btn_noheat_start->{
                checkIsOpen()
                DeviceEngine.getInstance().push(DeviceAction.Dryer.ACTION_NOHEAT,1)
            }
        }
    }

    override fun device(type: Int) {
        when (type) {
            0 -> {
                tv_type.text = "这是巨人Pro洗衣机"
                mType = type
                ToastUtil.show("这是巨人Pro洗衣机")
                ll_dryer.visibility = View.GONE
                ll_wash.visibility = View.VISIBLE
            }
            2 -> {
                tv_type.text = "这是小精灵洗衣机"
                mType = type
                ToastUtil.show("这是小精灵洗衣机")
                ll_dryer.visibility = View.GONE
                ll_wash.visibility = View.VISIBLE
            }
            3 -> {
                mType = type
                tv_type.text = "这是巨人Plus洗衣机"
                ToastUtil.show("这是巨人Plus洗衣机")
                ll_dryer.visibility = View.GONE
                ll_wash.visibility = View.VISIBLE
            }
            -1->{
                mType = type
                tv_type.text = "这是烘干机"
                ToastUtil.show("这是烘干机")
                ll_dryer.visibility = View.VISIBLE
                ll_wash.visibility = View.GONE
            }
            else -> {
                tv_type.text = "无法识别"
                ll_dryer.visibility = View.GONE
                ll_wash.visibility = View.GONE
            }
        }
    }

    /**
     * 自动识别
     */
    private fun automatic() {
        val selectDeviceHelper = SelectDeviceHelper()
        selectDeviceHelper.setOnCheckDeviceListener(this)
        selectDeviceHelper.checkSendSetting()
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
}
