package com.xiaolan.serialporttest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.xiaolan.serialporttest.mylib.DeviceAction;
import com.xiaolan.serialporttest.mylib.DeviceEngine;
import com.xiaolan.serialporttest.util1.MainActivity;
import com.xiaolan.serialporttest.wash.juren.JuRenWashActivity;
import com.xiaolan.serialporttest.wash.jurenplus.JuRenPlusWashActivity;
import com.xiaolan.serialporttest.wash.jurenpro.JuRenProWashActivity;
import com.xiaolan.serialporttest.wash.xjl.XjlWashActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_wash_message, R.id.btn_juren_pro,R.id.btn_juren_plus, R.id.btn_juren,R.id.btn_xjl})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_wash_message:
                startActivity(new Intent(this,MainActivity.class));
                break;
            case R.id.btn_juren_pro:
                DeviceEngine.getInstance().selectDevice(0);
                startActivity(new Intent(this,JuRenProWashActivity.class));
                break;
            case R.id.btn_juren_plus:
                DeviceEngine.getInstance().selectDevice(3);
                startActivity(new Intent(this,JuRenPlusWashActivity.class));
                break;
            case R.id.btn_juren:
                DeviceEngine.getInstance().selectDevice(1);
                startActivity(new Intent(this,JuRenWashActivity.class));
                break;
            case R.id.btn_xjl:
                DeviceEngine.getInstance().selectDevice(2);
                startActivity(new Intent(this,XjlWashActivity.class));
                break;

        }
    }
}
