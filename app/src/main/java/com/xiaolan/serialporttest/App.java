package com.xiaolan.serialporttest;

import android.app.Application;

import com.xiaolan.serialporttest.util1.ToastUtil;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ToastUtil.init(this);
    }
}
