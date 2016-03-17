package com.cit.usacycling.ant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cit.usacycling.ant.global.SharedSettings;

import javax.inject.Inject;

public class USACyclingExceptionHandler implements Thread.UncaughtExceptionHandler {
    Context mContext;

    @Inject
    SharedSettings settings;

    public USACyclingExceptionHandler(Context context) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.mContext = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        Log.e("UNCAUGHT EXCEPTION", throwable.toString());
        throwable.printStackTrace();
        settings.setForceClosedState(true);

//        Intent restartIntent = mContext.getPackageManager()
//                .getLaunchIntentForPackage(mContext.getPackageName());
//        PendingIntent intent = PendingIntent.getActivity(
//                mContext, 0,
//                restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//        AlarmManager manager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
//        manager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, intent);
//        System.exit(2);
    }
}