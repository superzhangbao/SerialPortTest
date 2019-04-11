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

    @Test
    public void addition_isCorrect() {
        int i = 1;
        int x = 2;
        if (i == 1) {
            i= 0;
        }
        if (x==2) {
            x =0;
        }
        System.out.println("i"+i+",x"+x);
    }
}