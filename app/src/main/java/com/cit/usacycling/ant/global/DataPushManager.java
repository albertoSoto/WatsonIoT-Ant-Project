package com.cit.usacycling.ant.global;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
import com.cit.usacycling.ant.background.mqtt.MQTTClient;
import com.cit.usacycling.ant.background.mqtt.TopicFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 04.11.2015
 */
public class DataPushManager extends Service {
    public final static String SERVICE_TAG = DataPushManager.class.getSimpleName();
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

    private long pushIntervalInMillis;
    private int qos;
    private Timer timer;
    private Looper looper;
    private int maxPayloadLength;

    @Inject
    TopicFactory topicFactory;
    @Inject
    DataCollector collector;
    @Inject
    MQTTClient client;
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
        pushIntervalInMillis = buildProperties.getDataPushInterval();
        maxPayloadLength = this.buildProperties.getMqttMessageMaxPayloadLength();

        qos = buildProperties.getDataMessageQoS();
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

    public void setPushIntervalInMillis(long pushIntervalInMillis) {
        this.pushIntervalInMillis = pushIntervalInMillis;
    }

    public void setQosValue(int qos) {
        this.qos = qos;
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        timerTask.cancel();
        super.onDestroy();
    }

    private void prepareJsonMessages() throws JSONException {
        final List<JSONObject> messagesToSend = new ArrayList<>();

        JSONObject payload = getPayload();
        Set<DeviceDataTypeStruct> collectorKeys = collector.getDataContainerKeys();

        try {
            for (DeviceDataTypeStruct str : collectorKeys) {
                List<JSONObject> jsonObjectsFromQueue = collector.pollData(str);
                if (jsonObjectsFromQueue.isEmpty()) {
                    continue;
                }

                int jsonObjectsCount = jsonObjectsFromQueue.size();
                String dataTypeKey = str.getDataType().toString();

                JSONObject wrapper = getWrapper(str.getDeviceId(), dataTypeKey);
                for (int jsonIndex = 0; jsonIndex < jsonObjectsCount; jsonIndex++) {
                    JSONObject json = jsonObjectsFromQueue.get(jsonIndex);
                    if (payload.toString().length() + json.toString().length() + wrapper.toString().length() <= maxPayloadLength) {
                        wrapper.getJSONArray(dataTypeKey).put(json);

                        if (jsonIndex == jsonObjectsCount - 1) {
                            payload.getJSONArray(Constants.Json.MainPayload.DATA).put(wrapper);
                        }
                    } else {
                        payload.getJSONArray(Constants.Json.MainPayload.DATA).put(wrapper);
                        addPreparedMessage(messagesToSend, payload);

                        payload = getPayload();
                        wrapper = getWrapper(str.getDeviceId(), dataTypeKey);
                        wrapper.getJSONArray(dataTypeKey).put(json);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        addPreparedMessage(messagesToSend, payload);

        new Handler(looper).post(new Runnable() {
            @Override
            public void run() {
                if (!client.sendMessage(topicFactory.getEventTopic(TopicFactory.ANT_DATA_EVENT), qos, messagesToSend)) {
                    // TODO: Handle unsuccessful push
                    client.sendMessage(topicFactory.getEventTopic(TopicFactory.ANT_DATA_EVENT), qos, messagesToSend);
                }
            }
        });
    }

    private void fillPayloadMainAttributes(JSONObject payload) throws JSONException {
        payload.put(Constants.Json.MainPayload.SYSTEM_TIMESTAMP, System.currentTimeMillis());
    }

    private JSONObject getPayload() throws JSONException {
        JSONObject payload = new JSONObject();
        fillPayloadMainAttributes(payload);
        payload.put(Constants.Json.MainPayload.DATA, new JSONArray());

        return payload;
    }

    private JSONObject getWrapper(String deviceId, @Nullable String dataTypeKey) throws JSONException {
        JSONObject wrapper = new JSONObject();
        wrapper.put(Constants.Json.DEVICE_ID_KEY, deviceId);

        if (dataTypeKey != null && wrapper.isNull(dataTypeKey)) {
            wrapper.put(dataTypeKey, new JSONArray());
        }

        return wrapper;
    }

    private void addPreparedMessage(List<JSONObject> messagesToSend, JSONObject payload) throws JSONException {
        if (payload.getJSONArray(Constants.Json.MainPayload.DATA).length() > 0) {
            messagesToSend.add(payload);
        }
    }
}
