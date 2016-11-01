package com.example.wbf486.accelerometerextract;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.os.CountDownTimer;

public class GunShotDetected extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		
				WindowManager.LayoutParams windowManagerParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, 
						WindowManager.LayoutParams.FLAG_DIM_BEHIND, PixelFormat.TRANSLUCENT);
    		
				WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    		
				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				View myView = inflater.inflate(R.layout.gunshot_detected, null);
    		
				wm.addView(myView, windowManagerParams);
    		
				//3000ms timer with 1000ms interval
				CountDownTimer countDownTimer = new CountDownTimer(3000, 1000) {
						public void onTick(long millisUntilFinished) {
						    //TODO: Do something every second
						}
    		
						public void onFinish() {
						    finish();
						    //YourActivity.finish();  outside the actvitiy
						}
			}.start();				
		}
				
}