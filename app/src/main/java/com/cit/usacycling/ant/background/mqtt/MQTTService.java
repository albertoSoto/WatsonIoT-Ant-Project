package com.cit.usacycling.ant.background.mqtt;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import com.cit.usacycling.ant.R;
import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.global.SharedSettings;
import com.cit.usacycling.ant.ui.IoTBasicConnectionSetActivity;
import com.cit.usacycling.ant.ui.MainActivity;

import javax.inject.Inject;

/**
 * Created by mrshah on 10/9/2015.
 * <p/>
 * MQTT Service publishes data to MQTT Broker.
 * Customized to subscribe to broadcasts from other services.
 */

public class MQTTService extends Service {
    public static final String SERVICE_TAG = MQTTService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private HandlerThread thread;

    @Inject
    MQTTClient client;

    @Inject
    SharedSettings settings;

    @Override
    public void onCreate() {
        thread = new HandlerThread(SERVICE_TAG + ".background",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        super.onCreate();
    }

    /*
        Get the MQTT Details from the intent and connect to the MQTT broker.
        Create system notification for running service.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        USACyclingApplication.getObjectGraph().inject(this);
        createBackgroundNotification();
        client.setLooper(thread.getLooper());

        if (client == null) {
            Log.d(SERVICE_TAG, "Connecting");
            client.connect();
        } else if (!client.isClientConnected()) {
            Log.d(SERVICE_TAG, "Connecting");
            client.connect();
        } else if (client.isClientConnected()) {
            Log.d(SERVICE_TAG, "Re-Connecting");
            client.disconnect();
            client.connect();
        }

        Log.d(SERVICE_TAG, "Created");

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(SERVICE_TAG, "Destroying");

        try {
            if (client != null && client.isClientConnected()) {
                client.disconnect();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Log.d(SERVICE_TAG, "Broadcast receiver for updating data is not registered");
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void createBackgroundNotification() {
        final int ONGOING_NOTIFICATION_ID = 1;

        Intent notificationIntent = new Intent(this, settings.getIoTCredentials() != null ? MainActivity.class : IoTBasicConnectionSetActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Resources res = getResources();
        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setTicker(res.getString(R.string.notification_ticker))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle(res.getString(R.string.app_name))
                .setContentText(res.getString(R.string.notification_context_text));

        Notification n = builder.build();
        n.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(ONGOING_NOTIFICATION_ID, n);
    }

    private class LocalBinder extends Binder {
        MQTTService getService() {
            return MQTTService.this;
        }
    }
}
