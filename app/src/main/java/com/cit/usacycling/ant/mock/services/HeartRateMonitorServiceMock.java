package com.cit.usacycling.ant.mock.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.cit.usacycling.ant.background.devices.HeartRateDevice;
import com.cit.usacycling.ant.background.devices.IAntDevice;
import com.cit.usacycling.ant.background.services.AntServiceBase;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.mock.devices.HeartRateDeviceMock;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by mrshah on 10/12/2015.
 * <p/>
 * This service connects to the ANT+ heart rate monitor and broadcasts its data and status.
 */
public class HeartRateMonitorServiceMock extends AntServiceBase {
    public static final String SERVICE_TAG = HeartRateMonitorServiceMock.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private HandlerThread thread;
    private Looper looper;
    private Hashtable<String, IAntDevice> devices;
    private Dictionary<String, PccReleaseHandle<AntPlusHeartRatePcc>> releaseHandles;

    @SuppressWarnings("unchecked")
    private final BroadcastReceiver addDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String number = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
            if (number.equals("57510")) {
                devices.put(number, new HeartRateDevice(number, getServiceContext()));
                IAntDevice device = devices.get(number);
                releaseHandles.put(
                        number,
                        AntPlusHeartRatePcc.requestAccess(
                                getServiceContext(),
                                Integer.parseInt(number),
                                0,
                                (AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>) device.getAccessResultReceiver(),
                                device.getDeviceStateChangeReceiver()));
            } else {
                devices.put(number, new HeartRateDeviceMock(number, getServiceContext()));

            }
        }
    };

    private final BroadcastReceiver removeDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String number = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
            try {
                IAntDevice device = devices.get(number);
                if (device instanceof HeartRateDeviceMock) {
                    ((HeartRateDeviceMock) device).cancelTimers();
                } else {
                    releaseHandles.get(number).close();
                    releaseHandles.remove(number);
                }
                devices.remove(number);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        thread = new HandlerThread(SERVICE_TAG + ".background",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        looper = thread.getLooper();

        IntentFilter registerDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_REGISTER_DEVICE);
        registerReceiver(addDeviceReceiver, registerDeviceFilter);

        IntentFilter unregisterDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_UNREGISTER_DEVICE);
        registerReceiver(removeDeviceReceiver, unregisterDeviceFilter);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        releaseHandles = new Hashtable<>();
        devices = new Hashtable<>();
        Log.d(SERVICE_TAG, "Started");

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Release the handle if it exists
        if (releaseHandles != null) {
            Enumeration keys = releaseHandles.keys();
            while (keys.hasMoreElements()) {
                releaseHandles.get(keys.nextElement()).close();
            }

            for (Map.Entry<String, IAntDevice> entry : devices.entrySet()) {
                IAntDevice device = entry.getValue();
                releaseHandles.remove(device.getDeviceId());
                if (device instanceof HeartRateDeviceMock) {
                    ((HeartRateDeviceMock) device).cancelTimers();
                }
            }
        }

        unregisterReceiver(addDeviceReceiver);
        unregisterReceiver(removeDeviceReceiver);
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restartHandle(final IAntDevice device) {
        new Handler(looper).post(new Runnable() {
            @Override
            public void run() {
                PccReleaseHandle<AntPlusHeartRatePcc> deviceHandle = releaseHandles.get(device.getDeviceId());
                if (deviceHandle != null) {
                    try {
                        deviceHandle.close();
                        releaseHandles.remove(device.getDeviceId());
                        deviceHandle = AntPlusHeartRatePcc.requestAccess(
                                getServiceContext(),
                                Integer.parseInt(device.getDeviceId()),
                                0,
                                (AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>) device.getAccessResultReceiver(),
                                device.getDeviceStateChangeReceiver());
                        releaseHandles.put(device.getDeviceId(), deviceHandle);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public Looper getLooper() {
        return this.looper;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private HeartRateMonitorServiceMock getServiceContext() {
        return this;
    }

    private class LocalBinder extends Binder {
        /*
            Return a handle to this service.
         */
        HeartRateMonitorServiceMock getService() {
            return HeartRateMonitorServiceMock.this;
        }
    }
}
