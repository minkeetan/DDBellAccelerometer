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
        Log.i(TAG, "calling onReceive");

        Intent AccIntent = new Intent(context, MainService.class);

        Log.i(TAG, "Intent action = " + intent.getAction());

        //if (intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_VOLUME_UP_BUTTON_PRESSED") ||
                //intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_VOLUME_DOWN_BUTTON_PRESSED")) {
        if(intent.getAction().equalsIgnoreCase("android.intent.action.BOOT_COMPLETED")){
            Log.i(TAG, "starting GUN SHOT service from receiver...");
            AccIntent.setAction(MainService.ACTION_START_MAINSERVICE);
            context.startService(AccIntent);
        }
        else{
            if(intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_ON_BUTTON_PRESSED")){
            //if(intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_VOLUME_UP_BUTTON_PRESSED")){
            		Log.i(TAG, "starting GUN SHOT data capturing...");
            		AccIntent.setAction(MainService.ACTION_START_CAPTURE);
            }else if(intent.getAction().equalsIgnoreCase("android.intent.action.ACTION_SLIDER_OFF_BUTTON_PRESSED")){
            		Log.i(TAG, "stopping GUN SHOT data capturing...");
                AccIntent.setAction(MainService.ACTION_STOP_CAPTURE);
            }
            
            context.startService(AccIntent);
        }
    }
}
