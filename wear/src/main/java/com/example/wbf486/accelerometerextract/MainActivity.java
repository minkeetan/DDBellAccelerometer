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
import android.support.v4.app.FragmentManager;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.IntentSender.SendIntentException;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.IllegalStateException;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
		SensorEventListener,
		BluetoothFragment.OnBTServiceStateChangeListener,
		WatchViewStub.OnLayoutInflatedListener {

		private Timer myTimer;

		private String mAccDataTmp = null;
		
		// Debugging
    private static final String TAG = "MainActivity";

		static String mBTFragmentTag = "BT_FRAGMENT_TAG";

    private TextView mTextView;
    
    private boolean mBTStateConnected = false;
    
    private final static int SAMPLING_RATE_NS = 10000000;
    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastUpdate = 0;
    private float x, y, z;
    private File mAccelerometerDataFile;  

		private BluetoothFragment mBTFragment = null;

		private Runnable Timer_Tick = new Runnable() {
				public void run() {
						//This method runs in the same thread as the UI.    	       
						//Do something to the UI thread here
					  
					  long curTime = System.nanoTime();
			      long diffTime = curTime - lastUpdate;
			      lastUpdate = curTime;
			      
			      Log.d(TAG, diffTime + "ns" + mAccDataTmp);

			      mTextView.setText(mAccDataTmp);		//display the data on the watch
					  
					  //Send the accelerometer data to mobile app via BT connection
					  if( (mBTFragment != null) && (mBTStateConnected == true) ) {
					  		mBTFragment.sendMessage(mAccDataTmp);
					  }
				}
		};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(this);

		    try {
		        FragmentManager manager = getSupportFragmentManager();
						mBTFragment = (BluetoothFragment) manager.findFragmentByTag(mBTFragmentTag);
		        if (mBTFragment == null) {
		            //fragment not in back stack, create it.		            
		    				mBTFragment = new BluetoothFragment();
		    				mBTFragment.setRetainInstance(true);
		            FragmentTransaction transaction = manager.beginTransaction();
		            transaction.replace(R.id.sample_content_fragment, mBTFragment, mBTFragmentTag);
		
		            Log.d(TAG, "addToBackStack " + mBTFragmentTag);
		            transaction.addToBackStack(null);
		
		            transaction.commit();
		        } else {
		            manager.popBackStack();
		        }
		        manager.executePendingTransactions();
		    } catch (IllegalStateException exception) {
		        Log.d(TAG, "Unable to commit fragment, could be activity has been killed");
		    }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
				if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
				  // Success! There's a accelerometer.
				  mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				  if (mAccelerometer.getMinDelay() != 0) {
				  	//this is a streaming sensor
				  	Log.d(TAG, "minimum time interval (ms) = " + mAccelerometer.getMinDelay());
				  }
				}
				else {
				  // Failure! No accelerometer.
				}
				
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		  	myTimer = new Timer();
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
    		mTextView = (TextView) stub.findViewById(R.id.accWearData_id);

    		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    		
    		myTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						TimerMethod();
					}
				}, 0, (SAMPLING_RATE_NS/1000000)); //start the task immediately with period of 30ms
    }
      
    @Override
    public void onDestroy() {
        super.onDestroy();
                
        myTimer.cancel();
    		
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
            
            mAccDataTmp = " | " + x + " | " + y + " | " + z + " | \n";

		        //long curTime = System.nanoTime();
		        //long diffTime = curTime - lastUpdate;
		        //if (diffTime >= SAMPLING_RATE_NS) {
		            //lastUpdate = curTime;
		
		            //String accStr = " | " + x + " | " + y + " | " + z + " | \n";
		            //Log.d(TAG, diffTime + "ns | " + x + " | " + y + " | " + z + " |");

		            //mTextView.setText(accStr);
		            
		            // save the data in a file, sample at 1/50ms
		            //File path = this.getFilesDir();
		            //mAccelerometerDataFile = new File(path, "MyAccelerometerWearData.txt");
		            //
		            //try {
		            //    FileOutputStream stream = new FileOutputStream(mAccelerometerDataFile, true);
		            //    stream.write(accStr.getBytes());
		            //    stream.flush();
		            //    stream.close();
		            //} catch (FileNotFoundException e) {
		            //    e.printStackTrace();
		            //} catch (IOException e) {
		            //    e.printStackTrace();
		            //}
		        //}
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
		
		private void TimerMethod()
		{
			//This method is called directly by the timer
			//and runs in the same thread as the timer.
	
			//We call the method that will work with the UI
			//through the runOnUiThread method.
			this.runOnUiThread(Timer_Tick);
		}		
}

