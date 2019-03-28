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
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 50; j++) {
                if (i ==0) {
                    if (j == 5) break;
                    System.out.println("i:"+i+"  j:"+j);
                }
                if (i ==1) {
                    if (j == 5) break;
                    System.out.println("i:"+i+"  j:"+j);
                }
                if (i ==2) {
                    if (j == 5) break;
                    System.out.println("i:"+i+"  j:"+j);
                }
                if (i ==3) {
                    if (j == 5) break;
                    System.out.println("i:"+i+"  j:"+j);
                }
            }
        }
    }
}