package com.xiaolan.serialporttest.mylib;

import com.xiaolan.serialporttest.event.WashStatusEvent;

public interface OnSendInstructionListener {

    void sendInstructionSuccess(int key, WashStatusEvent washStatusEvent);

    void sendInstructionFail(int key, String message);
}
