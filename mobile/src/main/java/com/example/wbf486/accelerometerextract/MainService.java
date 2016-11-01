package com.example.wbf486.accelerometerextract;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class MainService extends Service {
		private static final String TAG = "MainService";
		
		private Looper mServiceLooper;
		private ServiceHandler mServiceHandler;
		private long lastUpdate = 0;
		
		//public String gunShotFile;
		public String gunShotWearFile;
		
		//public MyAccelerometer mAccelero;
		
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mBTService = null;

		static final String ACTION_START_MAINSERVICE = "com.example.wbf486.accelerometerextract.ACTION_START_MAINSERVICE";
		static final String ACTION_END_MAINSERVICE = "com.example.wbf486.accelerometerextract.ACTION_END_MAINSERVICE";
		static final String ACTION_INIT_BTSERVICE = "com.example.wbf486.accelerometerextract.ACTION_INIT_BTSERVICE";
		static final String ACTION_CONNECT_DEVICE = "com.example.wbf486.accelerometerextract.ACTION_CONNECT_DEVICE";
		static final String ACTION_START_BTSERVICE = "com.example.wbf486.accelerometerextract.ACTION_START_BTSERVICE";
		static final String ACTION_START_CAPTURE = "com.example.wbf486.accelerometerextract.ACTION_START_CAPTURE";
		static final String ACTION_STOP_CAPTURE = "com.example.wbf486.accelerometerextract.ACTION_STOP_CAPTURE";
		static final String EXTRA_DATA = "com.example.wbf486.accelerometerextract.EXTRA_DATA";

		static boolean DataCaptureEnabled = false;

		private double y_old = 0, z_old = 0;

		// Handler that receives messages from the thread
		private final class ServiceHandler extends Handler {      
		    public ServiceHandler(Looper looper) {
		        super(looper);
		    }
		    
		    @Override
		    public void handleMessage(Message msg) {
		    		MainService myService = getMyService();
		    		switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            Log.d(TAG, getResources().getString(R.string.title_connected_to, mConnectedDeviceName));
														LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
										                .getInstance(myService);
										        localBroadcastManager.sendBroadcast(new Intent(
										                BluetoothDialog.ACTION_CLOSE));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            Log.d(TAG, getResources().getString(R.string.title_connecting));
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            Log.d(TAG, getResources().getString(R.string.title_not_connected));
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                  
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
					if(DataCaptureEnabled){
						patternCompare(readMessage);
						//printAccelerometerData(readMessage, gunShotWearFile);
					}
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != myService) {
                        Toast.makeText(myService, getResources().getString(R.string.title_connected_to, mConnectedDeviceName), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != myService) {
                        Toast.makeText(myService, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    }
                    break;
				//case Constants.MESSAGE_ACCELEROMETER_DATA:
				//	if(DataCaptureEnabled) {
				//		printAccelerometerData((String) msg.obj, gunShotFile);
				//	}
				//	break;
		        }		          
		    }
		}
		
		@Override
		public void onCreate() {
		  	// Start up the thread running the service.  Note that we create a
		  	// separate thread because the service normally runs in the process's
		  	// main thread, which we don't want to block.  We also make it
		  	// background priority so CPU-intensive work will not disrupt our UI.
		  	HandlerThread thread = new HandlerThread("AccelerometerDataThread", Process.THREAD_PRIORITY_BACKGROUND);
		  	thread.start();
		
		  	// Get the HandlerThread's Looper and use it for our Handler
		  	mServiceLooper = thread.getLooper();
		  	mServiceHandler = new ServiceHandler(mServiceLooper);

		  	//mAccelero = new MyAccelerometer(this, mServiceHandler);
		}
		
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {		
				if (intent != null) {
		        	final String action = intent.getAction();
		        if (action != null) {
		            switch (action) {
		                case ACTION_START_MAINSERVICE:
		                		//Start the main service
		                		Intent dialogIntent = new Intent(this, BluetoothDialog.class);
		                		dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								Log.i(TAG, "Start main service");
		                		startActivity(dialogIntent);
		                    break;		                
		                case ACTION_END_MAINSERVICE:
		                		//End the main service
		                    stopSelf(startId);
		                    break;
		                case ACTION_INIT_BTSERVICE:
		                		if(mBTService == null) {
		                    		//Initialize the BluetoothService to perform bluetooth connections
		                    		mBTService = new BluetoothService(null, mServiceHandler);
		                    }
		                    break;
		                case ACTION_CONNECT_DEVICE:
		                		Intent extraData = intent.getParcelableExtra(EXTRA_DATA);
		                		if(extraData != null) {
		                				// Get the device MAC address
		                				String address = extraData.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		                    		Log.d(TAG, "Connecting to device MAC address: " + address);
        										
		                    		// Get the BluetoothDevice object
		                    		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        										
		                    		// Attempt to connect to the device
		                    		mBTService.connect(device, true);
		                		}
		                    break;
		                case ACTION_START_BTSERVICE:
		                		if(mBTService != null) {
								            // Only if the state is STATE_NONE, do we know that we haven't started already
								            if (mBTService.getState() == BluetoothService.STATE_NONE) {
		                    				// Start the Bluetooth chat services
		                    				mBTService.start();
								            }
		                    }
		                    break;
						case ACTION_START_CAPTURE:
							//Start the accelerometer capture
							Toast.makeText(this, "start capturing Acclerometer data ...", Toast.LENGTH_SHORT).show();
							// Get the current date and time for the data capture file name
							Date curDate = new Date();
							Calendar calendar = new GregorianCalendar();

							int millisecond= calendar.get(Calendar.MILLISECOND);
							Log.i(TAG, "milisecond = " + millisecond);

							SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_hhmmss." + millisecond);
							String DateToStr = format.format(curDate);

							//gunShotFile = "ACC_" + DateToStr+".txt";
							gunShotWearFile = "ACC_" + DateToStr+"_wear.txt";

							DataCaptureEnabled = true;

							//mAccelero.startCapture();
							break;
						case ACTION_STOP_CAPTURE:
							//Stop the accelerometer capture
							Toast.makeText(this, "stop capturing Acclerometer data ...", Toast.LENGTH_SHORT).show();

							DataCaptureEnabled = false;

							//mAccelero.stopCapture();
							break;
		            }
		        }
		    }

		    // If we get killed, after returning from here, restart
		    return START_STICKY;
		}
		
		@Override
		public IBinder onBind(Intent intent) {
		    // We don't provide binding, so return null
		    return null;
		}
		
		@Override
		public void onDestroy() {
				Toast.makeText(this, "Accelerometer service done", Toast.LENGTH_SHORT).show();
        if (mBTService != null) {
            mBTService.stop();
        }				
		}
		
		public void printAccelerometerData(String accData, String accFile) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
			String secondStore = System.getenv("SECONDARY_STORAGE");

            File file;
            //File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/../storage/sdcard1/MotoGunShot");
			File path = new File(secondStore + "/MotoGunShot_wearableOnly/");
			//Log.i(TAG, "printAccelerometerData: " + path);

            path.mkdirs();
            // Link the filename with the input text
            if(accFile.indexOf( ".txt" ) == -1) {
                file = new File(path, accFile + ".txt");
            }else{
                file = new File(path, accFile);
            }

            try {
                FileOutputStream stream = new FileOutputStream(file, true);
                stream.write(accData.getBytes());
                stream.flush();
                stream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		}

		public void patternCompare(String accData) {	
				String[] retStr = accData.split("\\|");
				//Log.i(TAG, "accData_test = " + accData + " retStr[0] = " + retStr[0] + " retStr[1] = " + retStr[1]  + " retStr[2] = " + retStr[2]);
				double y = Math.abs(Double.parseDouble(retStr[1]));
				double z = Math.abs(Double.parseDouble(retStr[2]));
				
				Log.i(TAG, "value y = " + y + "value y_old = " + y_old);
				Log.i(TAG, "substration = " + (Math.abs(y_old) - y));
				
				if(y_old != 0 && z_old != 0 && ((Math.abs(y_old) - y) > 5) && ((Math.abs(z_old) - z) > 2)) {
					Log.i(TAG, "condition matched!");
					displayAlertDialog();
					y_old = 0;
					z_old = 0;
				}
				else{
					y_old = y;
					z_old = z;
				}
		}
		
		public void displayAlertDialog() {
				Intent dialogIntent = new Intent(this, GunShotDetected.class);
				dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Log.i(TAG, "Gun shot detected, displaying notification");
				startActivity(dialogIntent);	                		
		}
	
		MainService getMyService() {
		    return MainService.this;
		}
}
