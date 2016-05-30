package com.example.wbf486.accelerometerextract;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.Exception;

public class MainActivity extends AppCompatActivity implements 
		SensorEventListener,
		DataApi.DataListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

		private static final long TIMEOUT_MS = 100;

		// Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

		public static final String ACCELEROMETER_DATA_PATH = "/accelerometer_data";
		public static final String ACCELEROMETER_DATA_ASSET = "/accelerometer_data_asset";

		public static final String ACCCAPTURE_TRIGGER_PATH = "/accelerometer_capture_trigger";
		private static final String ACCCAPTURE_TRIGGER_KEY = "com.example.key.accelerometer_capture_trigger";

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    
    private GoogleApiClient mGoogleApiClient;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastUpdate = 0;
    private float x, y, z;
    private File file;
    private File wearablefile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        setContentView(R.layout.activity_main);
        
				mResolvingError = (savedInstanceState != null) && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        
        mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        // Request access only to the Wearable API
        .addApi(Wearable.API)
        .build();        
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

    protected void onResume() {
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        mGoogleApiClient.connect();
    }

    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
				Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();        
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Now you can use the Data Layer API
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }
    
    @Override
    public void onConnectionSuspended(int cause) {
        showErrorDialog(cause);
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

    public void onButtonStartClick(View view)
    {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        wearAccelerometerCapture(1);
    }

    public void onButtonStopClick(View view) {
        mSensorManager.unregisterListener(this);
        OutputStream os = null;
        wearAccelerometerCapture(0);

        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        boolean externalStorageAvailable = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File path = Environment.getExternalStorageDirectory();//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(path, "MyAccelerometerData.txt");
            try {
                path.mkdirs();
                os = new FileOutputStream(file);
                externalStorageAvailable = true;
            }catch (Exception e)
            {
                Log.d("fileddbell", "Exception " + e);
            }
        }

        try {
            FileInputStream fis = openFileInput("MyAccelerometerData.txt");
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.d("fileddbell", "Data = " + line);
                if(externalStorageAvailable == true) {
                    writeToFile(line, os);
                }
            }
            os.close();
        } catch (Exception e) {
            Log.d("fileddbell", "Exception " + e);
        }
    }

    public void wearAccelerometerCapture(int enable) {
    		if (mGoogleApiClient == null)
            return;

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(ACCCAPTURE_TRIGGER_PATH);
        putDataMapReq.getDataMap().putInt(ACCCAPTURE_TRIGGER_KEY, enable);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest().setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);    	
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().equals(ACCELEROMETER_DATA_PATH)) {
							      DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
							      Asset asset = dataMapItem.getDataMap().getAsset(ACCELEROMETER_DATA_ASSET);
							      writeAssetToFile(asset);
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
            TextView dataView = (TextView)findViewById(R.id.accData_id);
            dataView.setText(accStr);

            // save the data in a file, sample at 1/50ms
            File path = this.getFilesDir();
            file = new File(path, "MyAccelerometerData.txt");

            try {
                FileOutputStream stream = new FileOutputStream(file, true);
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

		void writeAssetToFile(Asset asset) {
		    if (asset == null) {
		        throw new IllegalArgumentException("Asset must be non-null");
		    }
		    
		    ConnectionResult result = mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		    if (!result.isSuccess()) {
		        showErrorDialog(result.getErrorCode());
		    }
		    
		    // convert asset into a file descriptor and block until it's ready
		    InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream();

		    if (assetInputStream == null) {
		        throw new IllegalArgumentException("Requested an unknown Asset");
		    }
		    
		    try {
				    File path = this.getFilesDir();
            wearablefile = new File(path, "MyWearableAccelerometerData.txt");
				    FileOutputStream output = new FileOutputStream(wearablefile);
				    
				    try {
				        try {
				            byte[] buffer = new byte[4 * 1024]; // or other buffer size
				            int read;
				
				            while ((read = assetInputStream.read(buffer)) != -1) {
				                output.write(buffer, 0, read);
				            }
				            output.flush();
				        } finally {
				            output.close();
				        }
				    } catch (Exception e) {
				        e.printStackTrace(); // handle exception, define IOException and others
				    }
				    
				} catch (FileNotFoundException e) {
            e.printStackTrace();          
				} finally {
						try {
				    		assetInputStream.close();
				    } catch (IOException e) {
        				e.printStackTrace();
        		}
				}
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

    private boolean writeToFile(String dataToWrite, OutputStream os)
    {
        if(os == null)
        {
            return false;
        }

        byte [] data = dataToWrite.getBytes();
        int length = dataToWrite.length();
        try {
            for(int i =0; i< length; i++) {
                os.write(data[i]);
            }
            os.write('\n');
        }catch(Exception e){

        }
        return true;
    }
}

