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
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service {
		private static final String TAG = "MainService";
		
		private Looper mServiceLooper;
		private ServiceHandler mServiceHandler;

		//Method 1 - start
		private Looper mTimerLooper;
		private TimerHandler mTimerHandler;
		private static Timer mAccTimer = new Timer();
		public static final int TIMER_5MS = 5;
		private long lastUpdate = 0;
		//Method 1 - end
		
		public String tmpGunShotData = " | 0 | 0 | 0 | \n";
		public String tmpGunShotWearData = " | 0 | 0 | 0 | \n";
		
		public String gunShotFile;
		public String gunShotWearFile;
		
		public MyAccelerometer mAccelero;
		
		static boolean mDataCaptureConnected = false;
		//static boolean mDataCapture = false;
		
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

		//Method 1 - start
		//Handling timer via a separate Handler using TimerTask - better performance
		private final class TimerHandler extends Handler {      
		    public TimerHandler(Looper looper) {
		        super(looper);
		    }
		    
		    @Override
		    public void handleMessage(Message msg) {
						handleTimeOut();
		    }
		}
    
    private class accTimerTask extends TimerTask
    { 
        public void run() 
        {
            mTimerHandler.sendEmptyMessage(0);
        }
    }
    //Method 1 - end

		//Method 2 - start
		//Handling timer via a separate Runnable via triggering postDelayed() within itself - slow performance
		//private Handler mTimerHandler = new Handler();
    //private Runnable mTimerRunnable = new Runnable() {
    //		@Override
    //    public void run() 
    //    {
		//				// do what you need to do
		//	      handleTimeOut();
		//	      // and here comes the "trick"
		//	      mTimerHandler.postDelayed(this, TIMER_5MS);
    //    }
    //};
    //Method 2 - end

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
										        mDataCaptureConnected = true;
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
                    tmpGunShotWearData = readMessage; //Method 1 & 2
                    //if(true == mDataCapture) {
                    //		printAccelerometerData(readMessage, gunShotWearFile);
                    //}
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
                case Constants.MESSAGE_ACCELEROMETER_DATA:
                    tmpGunShotData = (String) msg.obj; //Method 1 & 2
										//if(true == mDataCapture) {
                    //		printAccelerometerData((String) msg.obj, gunShotFile);
                  	//}
                    break;                    
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

		  	mAccelero = new MyAccelerometer(this, mServiceHandler);
				mAccelero.startCapture();
		  	
		  	//Method 1 - start
		  	HandlerThread timer_thread = new HandlerThread("AccelerometerTimerThread", Process.THREAD_PRIORITY_MORE_FAVORABLE);
		  	timer_thread.start();
		  	mTimerLooper = timer_thread.getLooper();
		  	mTimerHandler = new TimerHandler(mTimerLooper);
		  	//Method 1 - end
		}
		
		@Override
		public int onStartCommand(Intent intent, int flags, int startId) {		
		    // For each start request, send a message to start a job and deliver the
		    // start ID so we know which request we're stopping when we finish the job
		    //Message msg = mServiceHandler.obtainMessage();
		    //msg.arg1 = startId;
		    //mServiceHandler.sendMessage(msg);

				if (intent != null) {
		        	final String action = intent.getAction();
		        if (action != null) {
		            switch (action) {
		                //handleData(intent.getParcelableExtra(EXTRA_DATA));
		                // Implement your handleData method. Remember not to confuse Intents, or even better make your own Parcelable
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
												if(true == mDataCaptureConnected)
												{
														//Start the accelerometer capture
														Toast.makeText(this, "start capturing Acclerometer data with min delay: " + mAccelero.getAccMinDelay() + "us", Toast.LENGTH_SHORT).show();
														// Get the current date and time for the data capture file name
														Date curDate = new Date();
														Calendar calendar = new GregorianCalendar();
							
														int millisecond= calendar.get(Calendar.MILLISECOND);
														Log.i(TAG, "milisecond = " + millisecond);
							
														SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_hhmmss." + millisecond);
														String DateToStr = format.format(curDate);
							
														gunShotFile = "ACC_" + DateToStr+".txt";
														gunShotWearFile = "ACC_" + DateToStr+"_wear.txt";
							
														//mDataCapture = true;
														mAccTimer.scheduleAtFixedRate(new accTimerTask(), 0, TIMER_5MS); //Method 1
														//mTimerHandler.postDelayed(mTimerRunnable, TIMER_5MS); //Method 2
												}
												break;
										case ACTION_STOP_CAPTURE:
												if(true == mDataCaptureConnected)
												{
														//Stop the accelerometer capture
														Toast.makeText(this, "stop capturing Acclerometer data ...", Toast.LENGTH_SHORT).show();
							
														//mDataCapture = false;
														mAccTimer.cancel(); //Method 1
														//mTimerHandler.removeCallbacks(mTimerRunnable); //Method 2
												}
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
				mAccelero.stopCapture();
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
			File path = new File(secondStore + "/MotoGunShot/");
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
		
		MainService getMyService() {
		    return MainService.this;
		}

		//Method 1 & 2 - start
		public void handleTimeOut() {
			long curTime = System.currentTimeMillis();
			long period = curTime - lastUpdate;
			lastUpdate = curTime;
			//Log.d(TAG, "MESSAGE_TIMER_EXPIRED " + period + "ms");
    
			String localAccData = period + "ms" + tmpGunShotData;
			String localAccWearData = period + "ms" + tmpGunShotWearData;
			
			printAccelerometerData(localAccData, gunShotFile);
			printAccelerometerData(localAccWearData, gunShotWearFile);
		}
		//Method 1 & 2 - end
}
