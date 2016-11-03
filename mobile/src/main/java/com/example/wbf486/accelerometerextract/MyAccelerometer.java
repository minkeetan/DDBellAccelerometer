package com.example.wbf486.accelerometerextract;

import android.os.Handler;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class MyAccelerometer implements SensorEventListener {
		private static final String TAG = "MyAccelerometer";
		private long lastUpdate = 0;

    private float xAxis;
    private float yAxis;
    private float zAxis;

    //private final static int SAMPLING_RATE_NS = 30000000;

    SensorManager mSensorManager;
    Sensor mAccelerometer;
    Handler mHandler;

    public MyAccelerometer(Service service, Handler handler) {
    		mHandler = handler;
        mSensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
     }

    public void startCapture(){
    	if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
				  // Success! There's a accelerometer.
				  mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				  if (mAccelerometer.getMinDelay() != 0) {
				  	//this is a streaming sensor
				  	Log.d(TAG, "minimum time interval (ms) = " + mAccelerometer.getMinDelay());
				  	mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
				  }
				}
				else {
				  // Failure! No accelerometer.
				}
    }

    public void stopCapture(){
        mSensorManager.unregisterListener(this);
    }

    public float getX(){
        return this.xAxis;
    }

    public float getY(){
        return this.yAxis;
    }

    public float getZ(){
        return this.zAxis;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    public void onSensorChanged(SensorEvent event) {
				if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
						xAxis = event.values[0];
						yAxis = event.values[1];
						zAxis = event.values[2];

						String accStr = " | " + xAxis + " | " + yAxis + " | " + zAxis + " | \n";
						mHandler.obtainMessage(Constants.MESSAGE_ACCELEROMETER_DATA, accStr).sendToTarget();
						
						//long curTime = System.nanoTime();
						//long diffTime = curTime - lastUpdate;
		        //if (diffTime >= SAMPLING_RATE_NS) {
		        //    lastUpdate = curTime; 
		        //    String accStr = " | " + xAxis + " | " + yAxis + " | " + zAxis + " | \n";
		        //    
		        //    Log.d(TAG, diffTime + "ns | " + xAxis + " | " + yAxis + " | " + zAxis + " |");
		        //    
						//		mHandler.obtainMessage(Constants.MESSAGE_ACCELEROMETER_DATA, accStr).sendToTarget();		
						//}
				}
    }

}
