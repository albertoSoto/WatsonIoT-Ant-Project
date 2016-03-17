package com.cit.usacycling.ant.background.mqtt;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.global.BuildProperties;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.SharedSettings;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 04.11.2015
 */
public class MQTTClient {
    public final String TAG = this.getClass().getSimpleName();

    private static String mHost = "";
    private static String mPort = "";
    private static String mUsername = "";
    private static String mPassword = "";
    private static String mClientID = "";

    @Inject
    Context context;
    @Inject
    BuildProperties buildProperties;
    @Inject
    SharedSettings settings;
    private MqttClient client;
    private boolean isClientConnected;
    private boolean isConnecting;
    private Looper looper;

    public void setLooper(Looper looper) {
        this.looper = looper;
    }

    public MQTTClient() {
        USACyclingApplication.getObjectGraph().inject(this);
        setHostAndPort(buildProperties.getHostValue(), buildProperties.getPortValue());
        setUsernameAndPassword(buildProperties.getUserIdValue(), buildProperties.getSelectedDevicePass());
        setClientID(buildProperties.getClientIdValue());
        TOTAL_MESSAGES_COUNT = settings.getTotalMessagesCount();
        DATA_MESSAGE_ID = settings.getTotalDataMessagesCount();
        STATUS_MESSAGES_COUNT = settings.getTotalStatusMessagesCount();
    }

    public boolean isClientConnected() {
        return isClientConnected;
    }

    public boolean connect() {
        if (client != null && client.isConnected()) {
            return true;
        }

        if (!isConnecting && !isClientConnected) {
            Log.i(TAG, "Creating client");
            new Handler(looper).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        isConnecting = true;
                        client = new MqttClient(
                                "tcp://" + buildProperties.getHostValue() + ":" + buildProperties.getPortValue(), buildProperties.getClientIdValue().isEmpty() ? MqttClient.generateClientId() : buildProperties.getClientIdValue(), new MemoryPersistence());
                        client.setCallback(new MQTTCallbackHandler(context));
                        MqttConnectOptions options = new MqttConnectOptions();

                        if (!mUsername.isEmpty()) {
                            options.setUserName(mUsername);
                            options.setPassword(mPassword.toCharArray());
                            client.connect(options);
                        } else {
                            client.connect();
                        }

                        isClientConnected = true;
                        client.subscribe(new TopicFactory().getCommandTopic("matchesburned"));
                        sendIotStatusBroadcast(Constants.CONNECTED);
                        Log.i(TAG, "Connected");
                        isConnecting = false;
                    } catch (MqttException e) {
                        isClientConnected = false;
                        isConnecting = false;
                        disconnect();
                        Intent i = new Intent();
                        i.setAction(Constants.ACTION_RECONNECT);
                        context.sendBroadcast(i);
                        Log.e(TAG, "A MqttException occurred, trying to connect to IOT. Check network connectivity");
                    }
                }
            });
        }

        return isClientConnected;
    }

    public boolean sendMessage(String topic, int qos, JSONObject... messages) {
        return sendMessage(topic, qos, Arrays.asList(messages));
    }

    private long TOTAL_MESSAGES_COUNT;
    private long DATA_MESSAGE_ID;
    private long STATUS_MESSAGES_COUNT;
    private static long DATA_MESSAGE_START_APP_ID = 0;

    public boolean sendMessage(final String topic, final int qos, final List<JSONObject> messages) {
        if (client != null && !client.isConnected()) {
            if (!connect()) {
                return false;
            }
        }

        try {
            new Handler(looper).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (JSONObject message : messages) {
                            if (!message.has(Constants.STATUS_FLAG)) {
                                settings.setTotalDataMessagesCount(++DATA_MESSAGE_ID);
                                message.put(Constants.Json.MainPayload.MESSAGE_ID, ++DATA_MESSAGE_START_APP_ID);
                            } else {
                                settings.setTotalStatusMessagesCount(STATUS_MESSAGES_COUNT++);
                            }
                            String messageStr = message.toString();
                            settings.setTotalMessagesCount(TOTAL_MESSAGES_COUNT++);
                            client.publish(topic, messageStr.getBytes(), qos, false);
                            Log.e(TAG, " PAYLOAD LENGTH" + messageStr.length());
                            Log.i(TAG, "MAIN MSG: " + message);
                        }
                    } catch (MqttSecurityException e) {
                        Log.e(TAG, "Invalid Username/password");
                        isClientConnected = false;
                        isConnecting = false;
                        e.printStackTrace();
                    } catch (Exception e) {
                        isConnecting = false;
                        isClientConnected = false;
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }


    private void sendIotStatusBroadcast(String message) {
        Intent i = new Intent();
        i.setAction(Constants.ACTION_CHANGE_IOT_STATUS);
        i.putExtra(Constants.MESSAGE_DATA, message);
        Log.d("Sending Update", "IOT " + message);
        context.sendBroadcast(i);
    }

    /*
        Disconnect from MQTT Broker
     */
    public void disconnect() {
        if (client != null && !client.isConnected()) {
            return;
        }

        isClientConnected = false;

        try {
            if (client != null) {
                client.disconnect();
                sendIotStatusBroadcast(Constants.NOT_CONNECTED);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void setHostAndPort(String ip, String port) {
        if (mHost == null) {
            mHost = "";
        }
        if (mPort == null) {
            mPort = "";
        }
        mHost = ip;
        mPort = port;
    }

    public void setUsernameAndPassword(String username, String password) {
        mUsername = username;
        mPassword = password;
        if (mUsername == null) {
            mUsername = "";
        }
        if (mPassword == null) {
            mPassword = "";
        }
    }

    private void setClientID(String clientid) {
        mClientID = clientid;
    }
}