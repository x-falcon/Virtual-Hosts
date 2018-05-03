package com.github.xfalcon.vhosts.util;

import android.content.Context;
import android.util.Log;

public class LogUtils {


    public static Context context;

    private static void sendLogData(String tag,String msg){

    }

    public static void v(String tag, String msg) {
        sendLogData(tag,msg);
        Log.v(tag,msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        sendLogData(tag,msg+"   ;"+tr.getMessage());
        Log.v(tag,msg,tr);
    }

    public static void d(String tag, String msg) {
        sendLogData(tag,msg);
        Log.d(tag,msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        sendLogData(tag,msg+"   ;"+tr.getMessage());
        Log.d(tag,msg,tr);
    }

    public static void i(String tag, String msg) {
        sendLogData(tag,msg);
        Log.i(tag,msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        sendLogData(tag,msg+"   ;"+tr.getMessage());
        Log.i(tag,msg,tr);
    }

    public static void w(String tag, String msg) {
        sendLogData(tag,msg);
        Log.w(tag,msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        sendLogData(tag,msg+"   ;"+tr.getMessage());
        Log.w(tag,msg,tr);
    }

    public static void e(String tag, String msg) {
        sendLogData(tag,msg);
        Log.e(tag,msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        sendLogData(tag,msg+"   ;"+tr.getMessage());
        Log.e(tag,msg,tr);
    }

}
