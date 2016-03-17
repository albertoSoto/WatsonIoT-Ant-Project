package com.cit.usacycling.ant.global;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.DbProvider;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.background.mqtt.MQTTClient;
import com.cit.usacycling.ant.background.mqtt.TopicFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 06.11.2015
 */
public class StatusPushManager extends Service {
    public final static String SERVICE_TAG = StatusPushManager.class.getSimpleName();

    private final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            new Handler(looper).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        prepareJsonMessages();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private static long messageId = 1;
    private Timer timer;
    private int qos;
    private Looper looper;

    @Inject
    TopicFactory topicFactory;
    @Inject
    SharedSettings settings;
    @Inject
    MQTTClient client;
    @Inject
    DbProvider db;
    @Inject
    UnitOfWork unitOfWork;
    @Inject
    BuildProperties buildProperties;

    @Override
    public void onCreate() {
        USACyclingApplication.getObjectGraph().inject(this);
        HandlerThread thread = new HandlerThread(SERVICE_TAG + ".background",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        looper = thread.getLooper();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long pushIntervalInMillis = this.buildProperties.getStatusPushInterval();
        qos = buildProperties.getStatusMessageQoS();

        timer = new Timer();
        try {
            timer.schedule(timerTask, pushIntervalInMillis, pushIntervalInMillis);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        timerTask.cancel();
        super.onDestroy();
    }

    private void prepareJsonMessages() throws JSONException {
        JSONObject payload = getMainPayload();
        HashSet<String> activeDevices = unitOfWork.getDeviceRepository().getAllActiveDevices();

        JSONArray deviceStatuses = new JSONArray();
        for (String deviceId : activeDevices) {
            deviceStatuses.put(formStatusJson(deviceId));
        }

        payload.put(Constants.Json.MainPayload.DATA, deviceStatuses);
        client.sendMessage(topicFactory.getEventTopic(TopicFactory.ANT_DATA_EVENT), qos, payload);
    }

    private JSONObject formStatusJson(String deviceNumber) throws JSONException {
        int deviceStatusId = settings.getDeviceLastStatus(deviceNumber);
        return new JSONObject()
                .put(Constants.Json.DEVICE_ID_KEY, deviceNumber)
                .put(Constants.Json.DATA_STATE, deviceStatusId);
    }

    private JSONObject getMainPayload() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put(Constants.STATUS_FLAG, 1);
        payload.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
        payload.put(Constants.Json.MainPayload.MESSAGE_ID, messageId++);

        return payload;
    }
}
