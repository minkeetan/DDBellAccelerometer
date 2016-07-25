package com.example.wbf486.accelerometerextract;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by WBF486 on 7/15/2016.
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "MyReceiver";

    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_ON_BUTTON_PRESSED") || intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_OFF_BUTTON_PRESSED")) {
            Intent AccIntent = new Intent(context, MainService.class);
            if(intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_ON_BUTTON_PRESSED")){
            		Log.i(TAG, "starting service from receiver...");
                context.startService(AccIntent);
            }else{
            		Log.i(TAG, "stopping service from receiver...");
                AccIntent.setAction(MainService.ACTION_END_SERVICE);
                context.startService(AccIntent);
            }
        }
    }
}