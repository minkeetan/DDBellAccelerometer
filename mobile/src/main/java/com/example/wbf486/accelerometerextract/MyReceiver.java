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
    private static final String SERVICE_TAG = "ServiceNeedStart";

    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_ON_BUTTON_PRESSED") || intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_OFF_BUTTON_PRESSED")) {
            Log.i(TAG, "starting service...");

            Intent AccIntent = new Intent(context, MyService.class);

            if(intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_ON_BUTTON_PRESSED")){
                AccIntent.putExtra(SERVICE_TAG, 1);
            }else{
                AccIntent.putExtra(SERVICE_TAG, 0);
            }

            context.startService(AccIntent);
        }
    }
}
