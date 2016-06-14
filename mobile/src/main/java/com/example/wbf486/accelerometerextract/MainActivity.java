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
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.IntentSender.SendIntentException;


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
		BluetoothFragment.OnBTServiceDataAvailableListener {

		private static final long TIMEOUT_MS = 100;

		// Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    
    /*true indicates start wear accelerometer data parsing, false indicates stop*/
    private boolean mAccelerometerCapture = false;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastUpdate = 0;
    private long lastUpdateWear = 0;
    private float x, y, z;
    private File file;
    private File wearablefile;
    private EditText mEdit;
    public String gunShotFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        setContentView(R.layout.activity_main);
        
				mResolvingError = (savedInstanceState != null) && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);       
				
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

    protected void onResume() {
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
        
    }

    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
				
               
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


    public void onButtonStartClick(View view)
    {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mAccelerometerCapture = true;

        mEdit = (EditText)findViewById(R.id.file_name);
        Log.d("fileddbell", "file length " + mEdit.getText().length());
        if(mEdit.getText().length() < 1){
            gunShotFile = "GunShot.txt";
        }
        else {
            gunShotFile = mEdit.getText().toString();
        }
    }

    public void onButtonStopClick(View view) {
        mSensorManager.unregisterListener(this);
        OutputStream os = null;
        mAccelerometerCapture = false;

        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
        boolean externalStorageAvailable = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File file;
            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/MotoGunShot");//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            path.mkdirs();
            // Link the filename with the input text
            if(gunShotFile.indexOf( ".txt" ) == -1) {
                file = new File(path, gunShotFile + ".txt");
            }else{
                file = new File(path, gunShotFile);
            }

            try {
                //file.mkdirs();
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

        //delete internal files after save it to external storage
        File path = this.getFilesDir();
        file = new File(path, "MyAccelerometerData.txt");

        try{
            file.delete();
        }catch (Exception e){
            Log.d("fileddbell", "Exception " + e);
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
    
    public void onBTServiceDataRead(String readMessage) {
				if(mAccelerometerCapture) {
		    		long curTime = System.currentTimeMillis();
		    		
						if ((curTime - lastUpdateWear) > 100) {
				        lastUpdateWear = curTime;
				        
								TextView dataView = (TextView)findViewById(R.id.accWearData_id);
				        dataView.setText(readMessage);
				
				        // save the data in a file, sample at 1/50ms
				        File path = this.getFilesDir();
				        file = new File(path, "MyAccelerometerWearData.txt");
				
				        try {
				            FileOutputStream stream = new FileOutputStream(file, true);
				            stream.write(readMessage.getBytes());
				            stream.flush();
				            stream.close();
				        } catch (FileNotFoundException e) {
				            e.printStackTrace();
				        } catch (IOException e) {
				            e.printStackTrace();
				        }
				    }
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

