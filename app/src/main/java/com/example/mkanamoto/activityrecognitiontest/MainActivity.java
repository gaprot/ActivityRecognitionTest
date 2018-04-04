package com.example.mkanamoto.activityrecognitiontest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private ActivityDetectionReceiver mActivityDetectionReceiver;
    private List<AccelerometerRecord> mAccelerometerRecords;
    private long mLastSensorEventTime = 0;

    private TextView mActivityTypeTextView;
    private TextView mConfidenceTextView;
    private TextView mAccelerometerXTextView;
    private TextView mAccelerometerYTextView;
    private TextView mAccelerometerZTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        }

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) return;
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (sensors.size() > 0) {
            Sensor s = sensors.get(0);
            sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
        }

        mActivityDetectionReceiver = new ActivityDetectionReceiver();
        registerReceiver(mActivityDetectionReceiver, new IntentFilter(getResources().getString(R.string.notify_intent_action)));
        ActivityRecognitionClient client = ActivityRecognition.getClient(getApplicationContext());
        Intent intent = new Intent(getApplicationContext(), ActivityDetectionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Task<Void> task = client.requestActivityUpdates(0, pendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });

        mActivityTypeTextView = findViewById(R.id.activityTypeTextView);
        mConfidenceTextView = findViewById(R.id.confidenceTextView);
        mAccelerometerXTextView = findViewById(R.id.xTextView);
        mAccelerometerYTextView = findViewById(R.id.yTextView);
        mAccelerometerZTextView = findViewById(R.id.zTextView);

        mAccelerometerRecords = new ArrayList<>();

        Button shButton = findViewById(R.id.shButton);
        shButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toSh();
            }
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mActivityDetectionReceiver);
        super.onDestroy();
    }

    private void toSh() {
        StringBuilder sb = new StringBuilder();
        sb.append(getResources().getString(R.string.sh_initial_content));
        for (AccelerometerRecord record : mAccelerometerRecords) {
            sb.append(String.format(getResources().getString(R.string.sh_content), record.getFromLastTime(), record.getX(), record.getY(), record.getZ()));
        }

        String filePath = String.format(getResources().getString(R.string.filePath),
                Environment.getExternalStorageDirectory().getPath(),
                createCurrentTimeString(),
                getResources().getString(R.string.sh_extension));
        outPut(new String(sb), filePath);
    }

    private String createCurrentTimeString() {
        return new SimpleDateFormat(getResources().getString(R.string.date_format_pattern), Locale.JAPAN).format(new Date());
    }

    private void outPut(String result, String filePath) {
        File file = new File(filePath);
        try (FileOutputStream fileOutputStream =
                     new FileOutputStream(file, true);
             OutputStreamWriter outputStreamWriter =
                     new OutputStreamWriter(fileOutputStream, getResources().getString(R.string.utf_8));
             BufferedWriter bw =
                     new BufferedWriter(outputStreamWriter)
        ) {
            bw.write(result);
            bw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAccelerometerRecords.clear();
    }

    private class AccelerometerRecord {
        final private float x, y, z;
        final private float fromLastTime;

        AccelerometerRecord(float _x, float _y, float _z, float _time) {
            x = _x;
            y = _y;
            z = _z;
            fromLastTime = _time;
        }

        float getX() {
            return x;
        }

        float getY() {
            return y;
        }

        float getZ() {
            return z;
        }

        float getFromLastTime() {
            return fromLastTime;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long time = new Date().getTime();
            if (mLastSensorEventTime == 0) {
                mLastSensorEventTime = time;
            }
            float differenceTimes = (float) ((time - mLastSensorEventTime) * 0.001);
            AccelerometerRecord record = new AccelerometerRecord(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], differenceTimes);
            mAccelerometerRecords.add(record);
            mLastSensorEventTime = time;
            mAccelerometerXTextView.setText(String.format(getResources().getString(R.string.result_x), sensorEvent.values[0]));
            mAccelerometerYTextView.setText(String.format(getResources().getString(R.string.result_y), sensorEvent.values[1]));
            mAccelerometerZTextView.setText(String.format(getResources().getString(R.string.result_z), sensorEvent.values[2]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private class ActivityDetectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int activityType = intent.getIntExtra(getResources().getString(R.string.notify_activity_type), -1);
            final int confidence = intent.getIntExtra(getResources().getString(R.string.notify_confidence), -1);

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivityTypeTextView.setText(String.format(getResources().getString(R.string.activity_type), getNameFromType(activityType)));
                    mConfidenceTextView.setText(String.format(getResources().getString(R.string.activity_confidence), confidence));
                }
            });
        }

        private String getNameFromType(int activityType) {
            switch (activityType) {
                case DetectedActivity.IN_VEHICLE:
                    return getResources().getString(R.string.in_vehicle);
                case DetectedActivity.ON_BICYCLE:
                    return getResources().getString(R.string.on_bicycle);
                case DetectedActivity.ON_FOOT:
                    return getResources().getString(R.string.on_foot);
                case DetectedActivity.STILL:
                    return getResources().getString(R.string.still);
                case DetectedActivity.UNKNOWN:
                    return getResources().getString(R.string.unknown);
                case DetectedActivity.TILTING:
                    return getResources().getString(R.string.tilting);
                case DetectedActivity.WALKING:
                    return getResources().getString(R.string.walking);
                case DetectedActivity.RUNNING:
                    return getResources().getString(R.string.running);
            }
            return getResources().getString(R.string.unknown) + getResources().getString(R.string.with_space_hyphen) + activityType;
        }
    }
}
