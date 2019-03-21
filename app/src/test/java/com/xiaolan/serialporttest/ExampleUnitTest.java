package com.xiaolan.serialporttest;

import com.xiaolan.serialporttest.mylib.utils.LightMsg;

import org.junit.Test;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {


    private Map<Byte, String> mTextMap;
    private List<byte[]> his = new LinkedList<>();
    private String text;
    private int light1 = 0;     //whites灯
    private int light2 = 0;     //colors灯
    private int light3 = 0;     //delicates灯
    private int light4 = 0;     //perm.press灯
    private int light5 = 0;     // 加强
    private int lightst = 0;    // 开始
    private int lights1 = 0;    // 第1步 洗涤灯
    private int lights2 = 0;    // 第2步 漂洗灯
    private int lights3 = 0;    // 第3步 脱水灯
    private int lightlk = 0;    // 锁    门锁灯
    private int lighttxt = 0;
    @Test
    public void addition_isCorrect() {
        mTextMap = LightMsg.getText();
        byte[] msg1 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg2 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg3 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg4 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};

        byte[] msg5 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg6 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg7 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg8 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};

        byte[] msg9 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg10 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg11 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg12 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};

        byte[] msg13 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg14 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg15 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5F, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        byte[] msg16 = {0x02, 0x06, 0x00, 0x00, 0x00, 0x00, 0x3F, 0x4F, 0x00, 0x00, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03};
        for (int i = 0; i < 16; i++) {
            if (his.size()==2) {
                enqueueState(msg1);
            }
            if (his.size() > 12) dequeueState();
            System.out.println(light1);
            System.out.println(light2);
            System.out.println(light3);
            System.out.println(light4);
            System.out.println(light5);
            System.out.println(lightst);
            System.out.println(lights1);
            System.out.println(lights2);
            System.out.println(lights3);
            System.out.println(lightlk);
            System.out.println(this.text);
            System.out.println("--------------------------------------------------------------");
        }
    }

    private void enqueueState(byte[] msg) {
        his.add(msg);
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

        String t = mTextMap.get(msg[9]) + mTextMap.get(msg[8]) + mTextMap.get(msg[7]) + mTextMap.get(msg[6]);
        if (t.length() > 0) {
            this.text = t;
            lighttxt += 1;
        }
    }

    private void dequeueState() {
        if (his.isEmpty()) {
            return;
        }
        byte[] msg = his.remove(0);
        int l1 = msg[5];
        int l2 = msg[10];
        light1 -= (l2 & 0x20) >> 5;
        light2 -= (l2 & 0x40) >> 6;
        light3 -= (l1 & 0x04) >> 2;
        light4 -= (l1 & 0x08) >> 3;
        light5 -= (l1 & 0x10) >> 4;
        lightst -= (l2 & 0x01);
        lights1 -= (l2 & 0x02) >> 1;
        lights2 -= (l2 & 0x04) >> 2;
        lights3 -= (l2 & 0x08) >> 3;
        lightlk -= (l2 & 0x10) >> 4;
        String t = mTextMap.get(msg[9]) + mTextMap.get(msg[8]) + mTextMap.get(msg[7]) + mTextMap.get(msg[6]);
        if (t.length() > 0) {
            lighttxt -= 1;
        }
    }
}