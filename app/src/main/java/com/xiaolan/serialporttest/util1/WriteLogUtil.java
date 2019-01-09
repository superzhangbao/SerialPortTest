package com.xiaolan.serialporttest.util1;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WriteLogUtil {
    private FileWriter fw;

    public WriteLogUtil() {
        if (fw == null) {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/baowen_log_"+System.currentTimeMillis()+".txt");
            try {
                fw = new FileWriter(file, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeLog(String log) {
        try {
            // FILE_PATH : /mnt/sdcard/
            Date now = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
            String sDate = dateFormat.format(now);
            fw.write(sDate + " : " + log + "\r\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeLogClose() {
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
