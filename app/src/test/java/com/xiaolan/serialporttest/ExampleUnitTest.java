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
//        int i = 0;
//        do {
//            System.out.println(i++);
//        }while (i<1000);
        for (int i = 0; i < 1000; i++) {
            System.out.println(i);
        }
    }
}