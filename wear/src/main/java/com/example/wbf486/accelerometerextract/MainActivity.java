package com.example.wbf486.accelerometerextract;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.IntentSender.SendIntentException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ErrorDialogFragment;

import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.PutDataMapRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

public class MainActivity extends Activity implements
		SensorEventListener,
		DataApi.DataListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

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
    
    private GoogleApiClient mGoogleApiClient;

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
        
        mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        // Request access only to the Wearable API
        .addApi(Wearable.API)
        .build();
        
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
    }

		@Override
    public void onStart() {
    		super.onStart();
    		mGoogleApiClient.connect();           
    }
    
		@Override
    public void onStop() {
    		mGoogleApiClient.disconnect();
    		super.onStop();
    }    

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        
        if(1 == mAccelerometerCapture)
        {
        		mSensorManager.unregisterListener(this);
        }
        
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mTextView.setText("onConnected: " + connectionHint);
        // Now you can use the Data Layer API
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }
    
    @Override
    public void onConnectionSuspended(int cause) {
        mTextView.setText("onConnectionSuspended: " + cause);
    }

		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		    if (requestCode == REQUEST_RESOLVE_ERROR) {
		        mResolvingError = false;
		        if (resultCode == RESULT_OK) {
		            // Make sure the app is not already connected or attempting to connect
		            if (!mGoogleApiClient.isConnecting() &&
		                    !mGoogleApiClient.isConnected()) {
		                mGoogleApiClient.connect();
		            }
		        }
		    }
		}

		@Override
		protected void onSaveInstanceState(Bundle outState) {
		    super.onSaveInstanceState(outState);
		    outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
		}
		
    @Override
    public void onConnectionFailed(ConnectionResult result) {
				if (mResolvingError) {
    				// Already attempting to resolve an error.
    				return;
    		} else if (result.hasResolution()) {
		        try {
		            mResolvingError = true;
		            result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
		        } catch (SendIntentException e) {
		            // There was an error with the resolution intent. Try again.
		            mGoogleApiClient.connect();
		        }
		    } else {
		        // Show dialog using GoogleApiAvailability.getErrorDialog()
		        showErrorDialog(result.getErrorCode());
		        mResolvingError = true;
		    }
    }    

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals(ACCCAPTURE_TRIGGER_PATH)) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if(dataMap.getInt(ACCCAPTURE_TRIGGER_KEY) == 0) {
                    		//Stop accelerometer capture on wear
                    		mTextView.setText("Stop capture");
                    		mAccelerometerCapture = 0;
                    		mSensorManager.unregisterListener(this);
                    		
                    		//Send the data as asset
                    		sendAccelerometerData();
                    }
                    else if(dataMap.getInt(ACCCAPTURE_TRIGGER_KEY) == 1) {                    	
                    		//Start accelerometer capture on wear
                    		mTextView.setText("Start capture");
                    		mAccelerometerCapture = 1;
                    		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                    else {
                    }
                    	
                }				                
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
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

				Asset asset = Asset.createFromBytes(bytes);
				PutDataMapRequest dataMap = PutDataMapRequest.create(ACCELEROMETER_DATA_PATH);
				dataMap.getDataMap().putAsset(ACCELEROMETER_DATA_ASSET, asset);
				PutDataRequest request = dataMap.asPutDataRequest();
				PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
		}

/*----- The rest of this code is all about building the error dialog -----*/

    /* 1. Creates a dialog for an error message */
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

    /* 2. Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }
    
    /* 3. A fragment to display an error dialog */
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
}

