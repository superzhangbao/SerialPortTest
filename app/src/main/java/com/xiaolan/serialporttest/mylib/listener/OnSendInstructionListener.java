package com.xiaolan.serialporttest.mylib.listener;

import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;

public interface OnSendInstructionListener {

    void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent);

    void sendInstructionFail(int key, String message);
}
