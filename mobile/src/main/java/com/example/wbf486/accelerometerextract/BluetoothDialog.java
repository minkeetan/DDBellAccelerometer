package com.example.wbf486.accelerometerextract;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

public class BluetoothDialog extends Activity {

		private static final String TAG = "BluetoothDialog";

		private TextView myTextView;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mBluetoothAdapter = null;

		static final String ACTION_CLOSE = "com.example.wbf486.accelerometerextract.ACTION_CLOSE";

		LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_CLOSE)){
            		myTextView.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
                finish();
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bt_dialog);

				myTextView = (TextView)findViewById(R.id.btInfo_id);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_CLOSE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            myTextView.setText(R.string.bt_not_available_leaving);
            notifyService(MainService.ACTION_END_MAINSERVICE, null);
            finish();
        }
		}
		
    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the BT session
        } else {
        		notifyService(MainService.ACTION_INIT_BTSERVICE, null);
						// Launch the DeviceListActivity to see devices and do scan
						Log.d(TAG, "Launch the DeviceListActivity: Secure connect");
						Intent serverIntent = new Intent(this, DeviceListActivity.class);
						serverIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);	
        }
    }		

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        notifyService(MainService.ACTION_START_BTSERVICE, null);
    }

		@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "On activity request: " + requestCode + " with result: " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                		Log.d(TAG, "Triggered connectDevice() - secure");
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                		Log.d(TAG, "Triggered connectDevice() - insecure");
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, initialize the BluetoothService to perform bluetooth connections
        						notifyService(MainService.ACTION_INIT_BTSERVICE, null);
        						
        						// Launch the DeviceListActivity to see devices and do scan
										Log.d(TAG, "Launch the DeviceListActivity: Secure connect");
										Intent serverIntent = new Intent(this, DeviceListActivity.class);
										serverIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
										startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);	
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    myTextView.setText(R.string.bt_not_enabled_leaving);
                    notifyService(MainService.ACTION_END_MAINSERVICE, null);
                    finish();
                }
                break;
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
    		myTextView.setText(R.string.title_connecting);
    		
    		// Get the selected BT device name
    		String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
    		BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
    		mConnectedDeviceName = device.getName();
    		
    		// Pass the data to the service for actual BT connection
        notifyService(MainService.ACTION_CONNECT_DEVICE, data);
    }

		private void notifyService(String actionCode, final Intent data) {
		    final Intent intent = new Intent(this, MainService.class);
		    intent.setAction(actionCode);
		    intent.putExtra(MainService.EXTRA_DATA, data);
		    startService(intent);
		}
		
}

