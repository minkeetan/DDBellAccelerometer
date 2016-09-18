package com.example.wbf486.accelerometerextract;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.IntentSender.SendIntentException;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements
		SensorEventListener,
		BluetoothFragment.OnBTServiceStateChangeListener,
		WatchViewStub.OnLayoutInflatedListener {

    private TextView mTextView;
    
    private boolean mBTStateConnected = false;
    
    private final static int SAMPLING_RATE = 5;
    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastUpdate = 0;
    private float x, y, z;
    private File mAccelerometerDataFile;  

		private BluetoothFragment mBTFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(this);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
				  // Success! There's a accelerometer.
				  mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				  if (mAccelerometer.getMinDelay() != 0) {
				  	//this is a streaming sensor
				  }
				}
				else {
				  // Failure! No accelerometer.
				}
				
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

		@Override
    public void onStart() {
    		super.onStart(); 
    }
    
		@Override
    public void onStop() {
    		super.onStop();
    }    

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

		@Override
		protected void onSaveInstanceState(Bundle outState) {
		    super.onSaveInstanceState(outState);
		}

    @Override
    public void onLayoutInflated(WatchViewStub stub) {
    		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    		mBTFragment = new BluetoothFragment();
    		transaction.replace(R.id.sample_content_fragment, mBTFragment);
    		transaction.commit();
    		
    		mTextView = (TextView) stub.findViewById(R.id.accWearData_id);
    		
    		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);       		
    }
      
    @Override
    public void onDestroy() {
        super.onDestroy();
    		mSensorManager.unregisterListener(this);
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Right in here is where you put code to read the current sensor values and
        //update any views you might have that are displaying the sensor information
        //You'd get accelerometer values like this:
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        }

        long curTime = System.currentTimeMillis();

        if ((curTime - lastUpdate) > SAMPLING_RATE) {
            long diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;

            String accStr = " | " + x + " | " + y + " | " + z + " | \n";
            mTextView.setText(accStr);
            
            // save the data in a file, sample at 1/50ms
            File path = this.getFilesDir();
            mAccelerometerDataFile = new File(path, "MyAccelerometerWearData.txt");

            try {
                FileOutputStream stream = new FileOutputStream(mAccelerometerDataFile, true);
                stream.write(accStr.getBytes());
                stream.flush();
                stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            //send the accelerometer data to mobile app via BT connection
            if( (mBTFragment != null) && (mBTStateConnected == true) ) {
            		mBTFragment.sendMessage(accStr);
            }        
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
		
		public void onBTServiceStateConnected(boolean state) {
				if(state) {
						mBTStateConnected = true;
				}
				else {
						mBTStateConnected = false;
				}
		}
}

