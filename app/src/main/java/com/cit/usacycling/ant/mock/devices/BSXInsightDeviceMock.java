package com.cit.usacycling.ant.mock.devices;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
import com.cit.usacycling.ant.enums.BSXDeviceConnection;
import com.cit.usacycling.ant.enums.DeviceDataType;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.DataCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 18.11.2015
 */
public class BSXInsightDeviceMock {
    Context adopterService;
    String address;
    private Handler mhSendValue;
    private float mfSMO2;
    private Timer statusTimer;
    private Timer changeSMO2ValueTimer;
    private Random randomGenerator;

    @Inject
    DataCollector collector;

    public BSXInsightDeviceMock(final String address, Context adopterServiceContext) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.mhSendValue = new Handler();
        this.adopterService = adopterServiceContext;
        this.address = address;
        this.mfSMO2 = 102.3f;
        this.statusTimer = new Timer();
        this.changeSMO2ValueTimer = new Timer();
        this.randomGenerator = new Random();
        mhSendValue.postDelayed(rSendValue, 500);

        this.statusTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent deviceStateIntent = new Intent();
                deviceStateIntent.setAction(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
                deviceStateIntent.putExtra(Constants.STATE_EXTRA, BSXDeviceConnection.CONNECTED.getConnectionId());
                deviceStateIntent.putExtra(Constants.NUMBER_EXTRA, address);
                adopterService.sendBroadcast(deviceStateIntent);
            }
        }, 100);

        changeSMO2ValueTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mfSMO2 = randomGenerator.nextFloat();
            }
        }, 100, 150);
    }

    public void cancelTimers() {
        try {
            this.changeSMO2ValueTimer.cancel();
            this.statusTimer.cancel();
            Intent deviceStateIntent = new Intent();
            deviceStateIntent.setAction(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
            deviceStateIntent.putExtra(Constants.STATE_EXTRA, BSXDeviceConnection.DISCONNECTED.getConnectionId());
            deviceStateIntent.putExtra(Constants.NUMBER_EXTRA, address);
            adopterService.sendBroadcast(deviceStateIntent);
            mhSendValue.removeCallbacks(rSendValue);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().getSimpleName(), "Exception caught in BSX device mock");
        }
    }

    Runnable rSendValue = new Runnable() {
        @Override
        public void run() {
            if (mfSMO2 < 0) {

            } else {
                JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, mfSMO2);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    mfSMO2 = -1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                collector.addData(
                        parseDeviceDataTypeStruct(
                                address,
                                Constants.BSX_DEVICE_TYPE,
                                DeviceDataType.SMO2),
                        data);
            }
            mhSendValue.postDelayed(rSendValue, 500);
        }
    };

    public DeviceDataTypeStruct parseDeviceDataTypeStruct(String deviceId, String deviceType, DeviceDataType dataType) {
        return new DeviceDataTypeStruct(deviceId, deviceType, dataType);
    }
}
