package com.xiaolan.serialporttest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.xiaolan.serialporttest.dryer.DryerActivity;
import com.xiaolan.serialporttest.iot.IOTActivity;
import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.util1.MainActivity;
import com.xiaolan.serialporttest.util1.ToastUtil;
import com.xiaolan.serialporttest.wash.juren.JuRenWashActivity;
import com.xiaolan.serialporttest.wash.jurenplus.JuRenPlusWashActivity;
import com.xiaolan.serialporttest.wash.jurenpro.JuRenProWashActivity;
import com.xiaolan.serialporttest.wash.xjl.XjlWashActivity;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HomeActivity extends AppCompatActivity implements SelectDeviceHelper.OnCheckDeviceListener {
    @BindView(R.id.btn_automatic)
    Button btnAutomatic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_wash_message, R.id.btn_juren_pro, R.id.btn_juren_plus, R.id.btn_juren,
            R.id.btn_xjl, R.id.btn_dryer, R.id.btn_iot, R.id.btn_automatic})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_wash_message:
                startActivity(new Intent(this, MainActivity.class));
                break;
            case R.id.btn_juren_pro:
                DeviceEngine.getInstance().selectDevice(0);
                startActivity(new Intent(this, JuRenProWashActivity.class));
                break;
            case R.id.btn_juren_plus:
                DeviceEngine.getInstance().selectDevice(3);
                startActivity(new Intent(this, JuRenPlusWashActivity.class));
                break;
            case R.id.btn_juren:
                DeviceEngine.getInstance().selectDevice(1);
                startActivity(new Intent(this, JuRenWashActivity.class));
                break;
            case R.id.btn_xjl:
                DeviceEngine.getInstance().selectDevice(2);
                startActivity(new Intent(this, XjlWashActivity.class));
                break;
            case R.id.btn_dryer:
                DeviceEngine.getInstance().selectDevice(-1);
                startActivity(new Intent(this, DryerActivity.class));
                break;
            case R.id.btn_iot:
                startActivity(new Intent(this, IOTActivity.class));
                break;
            case R.id.btn_automatic://自动识别
                automatic();
                break;
        }
    }

    /**
     * 自动识别
     */
    private void automatic() {
        SelectDeviceHelper selectDeviceHelper = new SelectDeviceHelper();
        selectDeviceHelper.setOnCheckDeviceListener(this);
        selectDeviceHelper.checkSendSetting();
        ToastUtil.show("自动识别");
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void device(int type) {
        switch (type) {
            case 0:
                btnAutomatic.setText("这是巨人Pro洗衣机");
                ToastUtil.show("这是巨人Pro洗衣机");
                break;
            case 2:
                btnAutomatic.setText("这是小精灵洗衣机");
                ToastUtil.show("这是小精灵洗衣机");
                break;
            case 3:
                btnAutomatic.setText("这是巨人Plus洗衣机");
                ToastUtil.show("这是巨人Plus洗衣机");
                break;
        }
    }
}
