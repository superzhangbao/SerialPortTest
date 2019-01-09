package com.xiaolan.serialporttest.mylib;

import java.util.HashMap;
import java.util.Map;

public class LightMsg {
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

    public static Map<Byte, String> getText() {
        return textTable;
    }
}
