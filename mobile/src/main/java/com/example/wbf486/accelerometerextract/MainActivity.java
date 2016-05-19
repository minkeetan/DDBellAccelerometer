package com.example.wbf486.accelerometerextract;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastUpdate = 0;
    private float x, y, z;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        setContentView(R.layout.activity_main);
    }

    protected void onResume() {
        //mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
    }

    public void onButtonStartClick(View view)
    {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onButtonStopClick(View view)
    {
        mSensorManager.unregisterListener(this);

        Intent intent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
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
}