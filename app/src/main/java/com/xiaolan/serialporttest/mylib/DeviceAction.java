package com.xiaolan.serialporttest.mylib;

public class DeviceAction {

    public static class JuRenPro {
        public final static int ACTION_START = 1;      //开始
        public final static int ACTION_HOT = 2;        //热水
        public final static int ACTION_WARM = 3;       //温水
        public final static int ACTION_COLD = 4;       //冷水
        public final static int ACTION_DELICATES = 5;  //精致衣物
        public final static int ACTION_SUPER = 6;       //加强
        public final static int ACTION_SETTING = 8;    //设置
        public final static int ACTION_CLEAN = 11;
        public final static int ACTION_KILL = 100;     //kill
    }

    public static class JuRen {
        public final static int ACTION_START = 1;      //开始
        public final static int ACTION_WHITES = 2;        //whites
        public final static int ACTION_COLORS = 3;       //colors
        public final static int ACTION_DELICATES = 4;       //delicates
        public final static int ACTION_PERM_PRESS = 5;  //perm.press
        public final static int ACTION_SUPER = 6;       //加强
        public final static int ACTION_SETTING = 13;    //设置
        public final static int ACTION_KILL = 100;     //kill
    }

    public static class Xjl {
        public final static int ACTION_START = 1;      //开始
        public final static int ACTION_MODE1 = 2;        //whites
        public final static int ACTION_MODE2 = 3;       //colors
        public final static int ACTION_MODE3 = 4;       //delicates
        public final static int ACTION_MODE4 = 5;  //perm.press
        public final static int ACTION_SUPER = 6;       //加强
        public final static int ACTION_SETTING = 13;    //设置
        public final static int ACTION_KILL = 100;     //kill
    }
}
