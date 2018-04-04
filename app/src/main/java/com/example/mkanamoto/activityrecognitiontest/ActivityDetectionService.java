package com.example.mkanamoto.activityrecognitiontest;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityDetectionService extends IntentService {
    private static final String TAG = ActivityDetectionService.class.getSimpleName();

    public ActivityDetectionService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        DetectedActivity mostProbableActivity = result.getMostProbableActivity();
        Intent notifyIntent = new Intent(getResources().getString(R.string.notify_intent_action));
        notifyIntent.setPackage(getPackageName());
        notifyIntent.putExtra(getResources().getString(R.string.notify_activity_type), mostProbableActivity.getType());
        notifyIntent.putExtra(getResources().getString(R.string.notify_confidence), mostProbableActivity.getConfidence());
        sendBroadcast(notifyIntent);
    }
}