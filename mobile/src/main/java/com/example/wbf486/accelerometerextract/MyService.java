package com.example.wbf486.accelerometerextract;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class MyService extends Service {

    private final static String SERVICE_TAG = "ServiceNeedStart";

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent pIntent, int flags, int startId) {
        // TODO Auto-generated method stub
        int status = pIntent.getIntExtra(SERVICE_TAG, 0);
        Toast.makeText(this, "MyService", Toast.LENGTH_LONG).show();
        Log.i("MyService", "status = " + status);

        return super.onStartCommand(pIntent, flags, startId);
    }
}
