package com.cit.usacycling.ant.background.services;

import android.os.Looper;

import com.cit.usacycling.ant.background.devices.IAntDevice;

/**
 * Created by nikolay.nikolov on 20.10.2015
 */
public interface IAntServiceBase {
    /*
        Broadcast received data from ant device to MQTT service
     */
    void sendUpdate(String type, final String message);

    /*
        Broadcast ant device last state and save it for later use
     */
    void sendDeviceState(final int state, final String deviceId);

    /*
        If device search times out, restarting device handle is needed
     */
    void restartHandle(IAntDevice device);

    Looper getLooper();
}
