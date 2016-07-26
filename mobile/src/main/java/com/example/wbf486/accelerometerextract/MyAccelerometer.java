package com.example.wbf486.accelerometerextract;

import android.os.Handler;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MyAccelerometer implements SensorEventListener {

		private long lastUpdate = 0;

    private float xAxis;
    private float yAxis;
    private float zAxis;

    SensorManager mSensorManager;
    Sensor mAccelerometer;
    Handler mHandler;

    public MyAccelerometer(Service service, Handler handler) {
    		mHandler = handler;
        mSensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
        //mAccelerometer = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        //if( (mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) != null ) {
				  // Success! There's a accelerometer.
				  //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL, handler);
				//}
     }

    public void startCapture(){
        if( (mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) != null ) {
            // Success! There's a accelerometer.
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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
				}
				
				long curTime = System.currentTimeMillis();

        if ((curTime - lastUpdate) > 100) {
            lastUpdate = curTime;
            String accStr = " | " + xAxis + " | " + yAxis + " | " + zAxis + " | \n";
						mHandler.obtainMessage(Constants.MESSAGE_ACCELEROMETER_DATA, accStr).sendToTarget();
				}
    }

}
