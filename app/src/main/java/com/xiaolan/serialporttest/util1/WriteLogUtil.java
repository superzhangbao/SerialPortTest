package com.xiaolan.serialporttest.util1;

import android.os.Environment;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteLogUtil {
    private static FileWriter fw;
    public static int writeLog(String log) {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        String sDate = dateFormat.format(now);
        try {
            // FILE_PATH : /mnt/sdcard/
            if (fw == null) {
                fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/ble_log.txt", true);
            }
            fw.write(sDate + " : " + log + "\r\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void writeLogClose() {
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
