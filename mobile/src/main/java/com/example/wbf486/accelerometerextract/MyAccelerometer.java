package com.example.wbf486.accelerometerextract;

import android.os.Handler;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MyAccelerometer implements SensorEventListener {

    private float xAxis;
    private float yAxis;
    private float zAxis;

    private float[] gravityV = new float[3];
    
    private long lastUpdate = 0;
    
    SensorManager mSensorManager;
    Sensor mAccelerometer;
    Handler mHandler;

    public MyAccelerometer(Service service, Handler handler) {
    		mHandler = handler;
        mSensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
				  // Success! There's a accelerometer.
				  mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				}
				else {
				  // Failure! No accelerometer.
				}
     }

		public int getAccMinDelay(){
				return mAccelerometer.getMinDelay();
		}

    public void startCapture(){
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
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
				//if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
						xAxis = event.values[0];
						yAxis = event.values[1];
						zAxis = event.values[2];
				//}

				//final float alpha = 0.8f;
				////gravity is calculated here
				//gravityV[0] = alpha * gravityV[0] + (1 - alpha) * event.values[0];
				//gravityV[1] = alpha * gravityV[1] + (1 - alpha)* event.values[1];
				//gravityV[2] = alpha * gravityV[2] + (1 - alpha) * event.values[2];
				////acceleration retrieved from the event and the gravity is removed
				//xAxis = event.values[0] - gravityV[0];
				//yAxis = event.values[1] - gravityV[1];
				//zAxis = event.values[2] - gravityV[2];
							
				String accStr = " | " + xAxis + " | " + yAxis + " | " + zAxis + " | \n";
				
				long curTime = System.currentTimeMillis();
				long period = curTime - lastUpdate;
				lastUpdate = curTime;
				
				String tmpStr = period + "ms" + accStr;
		
				mHandler.obtainMessage(Constants.MESSAGE_ACCELEROMETER_DATA, tmpStr).sendToTarget();
    }

}
