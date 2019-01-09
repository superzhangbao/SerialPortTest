package com.xiaolan.serialporttest.mylib;

import com.xiaolan.serialporttest.event.WashStatusEvent;

public interface CurrentStatusListener {

    void currentStatus(WashStatusEvent washStatusEvent);
}
