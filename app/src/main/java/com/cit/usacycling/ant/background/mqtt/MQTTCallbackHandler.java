package com.cit.usacycling.ant.background.mqtt;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.SharedSettings;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 19.10.2015
 */
public class MQTTCallbackHandler implements MqttCallback {
    private final String TAG = this.getClass().getSimpleName();
    private final String CONNECTION_LOST_EXC_CODE = "32109";

    @Inject
    SharedSettings settings;
    private Context context;
    private long DELIVERED_MESSAGES_COUNT;
    private long DELIVERED_DATA_MESSAGES_COUNT;
    private long DELIVERED_STATUS_MESSAGES_COUNT;

    private int matchesBurnedCount = 0;


    public MQTTCallbackHandler(Context context) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.context = context;
        DELIVERED_MESSAGES_COUNT = settings.getTotalDeliveredMessagesCount();
        DELIVERED_DATA_MESSAGES_COUNT = settings.getDeliveredDataMessagesCount();
        DELIVERED_STATUS_MESSAGES_COUNT = settings.getDeliveredStatusMessagesCount();
    }

    @Override
    public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
        String message = arg1.toString();
        JSONObject jsonMsg = new JSONObject(message);
        Log.d("COMMAND RECEIVED: ", message);

        if (arg0.contains(Constants.MQTT_MATCH_BURNED_COMMAND_NAME)) {
            Intent matchBurnedDebugIntent = new Intent(Constants.ACTION_MATCH_BURNED_CMD);
            matchBurnedDebugIntent.putExtra(Constants.MATCH_BURNED_EXTRA, message);
            context.sendBroadcast(matchBurnedDebugIntent);
            matchesBurnedCount = jsonMsg.getInt("count");
        }

        // We prepare the rider data to be sent to the SOLO app
        Intent soloGlassesIntent = new Intent(Constants.ACTION_SOLO_GLASSES_RIDER_NOTIFICATION);
        String riderDataJson = RiderDataParser.getRiderDataJson(matchesBurnedCount);
        soloGlassesIntent.putExtra("riderDataJson", riderDataJson);
        context.sendBroadcast(soloGlassesIntent);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        settings.setTotalDeliveredMessagesCount(DELIVERED_MESSAGES_COUNT++);

        try {
            JSONObject message = new JSONObject(arg0.getMessage().toString());
            if (!message.has(Constants.STATUS_FLAG)) {
                settings.setDeliveredDataMessagesCount(DELIVERED_DATA_MESSAGES_COUNT++);
            } else {
                settings.setDeliveredStatusMessagesCount(DELIVERED_STATUS_MESSAGES_COUNT++);
            }
        } catch (MqttException | JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "DELIVERED MESSAGES COUNT: " + DELIVERED_MESSAGES_COUNT);
    }

    @Override
    public void connectionLost(Throwable exception) {
        /*
            If user did not order to disconnect, reconnect.
            Temporarily disabled, until receiving commands
        */
        if (!(exception.toString().contains(CONNECTION_LOST_EXC_CODE)
                && exception.getCause() instanceof EOFException)) {
            Intent i = new Intent();
            i.setAction(Constants.ACTION_RECONNECT);
            context.sendBroadcast(i);
        }

        exception.printStackTrace();
    }
}
