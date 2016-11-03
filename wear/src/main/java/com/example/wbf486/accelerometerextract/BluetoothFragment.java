package com.example.wbf486.accelerometerextract;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothFragment extends Fragment {

    private static final String TAG = "BluetoothFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mBTService = null;

    OnBTServiceStateChangeListener mCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mBTService == null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
        		mBTService = new BluetoothService(getActivity(), mHandler);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

		@Override
    public void onStop() {
				if (mBTService != null) {
            mBTService.stoppingFragment(); //must be triggered before BluetoothService::stop() to avoid restarting of BT listener
        }     	    	
    		super.onStop();
    }  
		
		@Override
    public void onDetach() {
				if (mBTService != null) {
            mBTService.stop();
        }    	
    		super.onDetach();     		 		
    }
    
    @Override
    public void onDestroy() {    	
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBTService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBTService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBTService.start();
            }
        }        
    }

    // Container Activity must implement this interface
    public interface OnBTServiceStateChangeListener {
        public void onBTServiceStateConnected(boolean state);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnBTServiceStateChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBTServiceStateChangeListener");
        }        
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Log.d(TAG, "Error: " + R.string.not_connected);
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBTService.write(send);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
		        switch (msg.what) {
		            case Constants.MESSAGE_STATE_CHANGE:
		                switch (msg.arg1) {
		                    case BluetoothService.STATE_CONNECTED:
		                        setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
		                        // Send the event to the host activity
		                        mCallback.onBTServiceStateConnected(true);
		                        break;
		                    case BluetoothService.STATE_CONNECTING:
		                        setStatus(R.string.title_connecting);
		                        break;
		                    case BluetoothService.STATE_LISTEN:
		                    case BluetoothService.STATE_NONE:
		                        setStatus(R.string.title_not_connected);
		                        // Send the event to the host activity
		                        mCallback.onBTServiceStateConnected(false);
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
		                
		                break;
		            case Constants.MESSAGE_DEVICE_NAME:
		                // save the connected device's name
		                mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
		                if (null != activity) {
		                    Toast.makeText(activity, "Connected to "
		                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
		                }
		                break;
		            case Constants.MESSAGE_TOAST:
		                if (null != activity) {
		                    Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
		                            Toast.LENGTH_SHORT).show();
		                }
		                break;
		        }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, initialize the BluetoothChatService to perform bluetooth connections
        						mBTService = new BluetoothService(getActivity(), mHandler);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "Error: BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

}
