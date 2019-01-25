package com.xiaolan.serialporttest;

import android.util.Log;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    private final static Map<Byte, String> textTable = new HashMap<Byte, String>() {
        {
            put((byte) 0x00, "");
            put((byte) 0x3F, "0");
            put((byte) 0x06, "1");
            put((byte) 0x5B, "2");
            put((byte) 0x4F, "3");
            put((byte) 0x66, "4");
            put((byte) 0x6D, "5");
            put((byte) 0x7D, "6");
            put((byte) 0x27, "7");
            put((byte) 0x7F, "8");
            put((byte) 0x6F, "9");
            put((byte) 0x77, "A");
            put((byte) 0x7C, "b");
            put((byte) 0x5E, "d");
            put((byte) 0x79, "E");
            put((byte) 0x67, "g");
            put((byte) 0x74, "h");
            put((byte) 0x39, "C");
            put((byte) 0x76, "H");
            put((byte) 0x38, "L");
            put((byte) 0x54, "n");
            put((byte) 0x73, "P");
            put((byte) 0x78, "t");
            put((byte) 0x71, "F");
            put((byte) 0x50, "r");
            put((byte) 0x5C, "o");
            put((byte) 0x3E, "U");
            put((byte) 0x58, "c");
        }
    };

    private int light1 = 0;
    private int light2 = 0;
    private int light3 = 0;
    private int light4 = 0;
    private int light5 = 0;     // 加强
    private int lightst = 0;    // 开始
    private int lights1 = 0;    // 第1步
    private int lights2 = 0;    // 第2步
    private int lights3 = 0;    // 第3步
    private int lightlk = 0;    // 锁
    private int lighttxt = 0;
    private String text;

    private byte[] msg = {0x02,0x06,0x00,0x00,0x00,0x00,0x76,0x6d,0x3e,0x73,0x20,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x7b, 0x72,0x03};

    @Test
    public void addition_isCorrect() {
        int l1 = msg[5];
        int l2 = msg[10];
        light1 += (l2 & 0x20) >> 5;
        light2 += (l2 & 0x40) >> 6;
        light3 += (l1 & 0x04) >> 2;
        light4 += (l1 & 0x08) >> 3;
        light5 += (l1 & 0x10) >> 4;
        lightst += (l2 & 0x01);
        lights1 += (l2 & 0x02) >> 1;
        lights2 += (l2 & 0x04) >> 2;
        lights3 += (l2 & 0x08) >> 3;
        lightlk += (l2 & 0x10) >> 4;

        String t = textTable.get(msg[9]) + textTable.get(msg[8]) + textTable.get(msg[7]) + textTable.get(msg[6]);
        if (t.length() > 0) {
            this.text = t;
            lighttxt += 1;
        }
        Log.e("text",text);
    }
}