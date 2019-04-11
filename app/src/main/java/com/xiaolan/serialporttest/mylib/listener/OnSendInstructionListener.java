package com.xiaolan.serialporttest.mylib.listener;

import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;

public interface OnSendInstructionListener {

    void sendInstructionSuccess(int key, Object object);

    void sendInstructionFail(int key, String message);
}
