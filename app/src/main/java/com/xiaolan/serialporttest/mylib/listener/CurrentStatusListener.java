package com.xiaolan.serialporttest.mylib.listener;

import com.xiaolan.serialporttest.mylib.event.WashStatusEvent;

public interface CurrentStatusListener {

    void currentStatus(WashStatusEvent washStatusEvent);
}
