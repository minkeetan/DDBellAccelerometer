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
		SensorEventListener {

		// Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    
    public static final String ACCELEROMETER_DATA_PATH = "/accelerometer_data";
		public static final String ACCELEROMETER_DATA_ASSET = "/accelerometer_data_asset";

		public static final String ACCCAPTURE_TRIGGER_PATH = "/accelerometer_capture_trigger";
		private static final String ACCCAPTURE_TRIGGER_KEY = "com.example.key.accelerometer_capture_trigger";

    private TextView mTextView;

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

		/*1 indicates start accelerometer data capture, 0 indicates stop*/
    private int mAccelerometerCapture = 0;
    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastUpdate = 0;
    private float x, y, z;
    private File mAccelerometerDataFile;  

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    
				mResolvingError = (savedInstanceState != null) && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
              
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
				
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothFragment fragment = new BluetoothFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }				
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

        
        if(1 == mAccelerometerCapture)
        {
        		mSensorManager.unregisterListener(this);
        }
        
    }

		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		    if (requestCode == REQUEST_RESOLVE_ERROR) {
		        mResolvingError = false;
		        if (resultCode == RESULT_OK) {
		            // Make sure the app is not already connected or attempting to connect

		        }
		    }
		}

		@Override
		protected void onSaveInstanceState(Bundle outState) {
		    super.onSaveInstanceState(outState);
		    outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
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

        if ((curTime - lastUpdate) > 100) {
            long diffTime = (curTime - lastUpdate);
            lastUpdate = curTime;

            String accStr = " | " + x + " | " + y + " | " + z + " | \n";
            mTextView.setText(accStr);
            
            // save the data in a file, sample at 1/50ms
            File path = this.getFilesDir();
            mAccelerometerDataFile = new File(path, "MyAccelerometerData.txt");

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
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

		public void sendAccelerometerData() {

				int size = (int) mAccelerometerDataFile.length();
				byte[] bytes = new byte[size];
				try {
				    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(mAccelerometerDataFile));
				    buf.read(bytes, 0, bytes.length);
				    buf.close();
				} catch (FileNotFoundException e) {
				    e.printStackTrace();
				} catch (IOException e) {
				    e.printStackTrace();
				}

		}

/*
//----- The rest of this code is all about building the error dialog -----

    // 1. Creates a dialog for an error message
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        //dialogFragment.show(getSupportFragmentManager(), "errordialog");
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    // 2. Called from ErrorDialogFragment when the dialog is dismissed.
    public void onDialogDismissed() {
        mResolvingError = false;
    }
    
    // 3. A fragment to display an error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }
*/
}

