package com.cit.usacycling.ant.background.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cit.usacycling.ant.background.devices.IAntDevice;
import com.cit.usacycling.ant.global.Constants;

/**
 * Created by Nikolay on 1.11.2015
 */
public abstract class AntServiceBase extends Service implements IAntServiceBase {

    @Override
    public void sendUpdate(String type, String message) {
        Intent i = new Intent();
        i.setAction(Constants.ACTION_ANT_DEVICE_DATA_UPDATE);
        i.putExtra(Constants.MESSAGE_DATA, message);
        Log.d("Sending Update", message);
        sendBroadcast(i);
    }

    @Override
    public void sendDeviceState(final int state, final String deviceId) {
        Intent deviceStateIntent = new Intent();
        deviceStateIntent.setAction(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
        deviceStateIntent.putExtra(Constants.STATE_EXTRA, state);
        deviceStateIntent.putExtra(Constants.NUMBER_EXTRA, deviceId);
        sendBroadcast(deviceStateIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Override
    public abstract void restartHandle(IAntDevice device);

    @Nullable
    @Override
    public abstract IBinder onBind(Intent intent);
}
