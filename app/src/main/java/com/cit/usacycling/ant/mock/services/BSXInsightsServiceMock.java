package com.cit.usacycling.ant.mock.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.mock.devices.BSXInsightDeviceMock;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BSXInsightsServiceMock extends Service {

    public static final String SERVICE_TAG = BSXInsightsServiceMock.class.getSimpleName();
    static BluetoothAdapter mBTAdapter = null;
    private final IBinder mBinder = new LocalBinder();
    HashSet<String> addressesToTurnOn;
    HashSet<String> addressesToTurnOff;
    Hashtable<String, BSXInsightDeviceMock> connectedDevices;

    private final BroadcastReceiver addDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    String address = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
                    addressesToTurnOn.add(address);
                    addressesToTurnOff.remove(address);
                    if (addressesToTurnOn.contains(address)) {
                        Log.d("BSX", "Turning ON Device");
                        BSXInsightDeviceMock newDevice = new BSXInsightDeviceMock(address, getApplicationContext());

                        addressesToTurnOn.remove(address);
                        connectedDevices.put(address, newDevice);
                    }
                }
            });
        }
    };

    private final BroadcastReceiver removeDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
            addressesToTurnOff.add(address);
            addressesToTurnOn.remove(address);

            if (addressesToTurnOff.contains(address)) {
                Log.d("BSX", "Turning OFF Device");
                addressesToTurnOff.remove(address);
                connectedDevices.get(address).cancelTimers();
                connectedDevices.remove(address);
            }

            Intent deviceStateIntent = new Intent();
            deviceStateIntent.setAction(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
            deviceStateIntent.putExtra(Constants.STATE_EXTRA, DeviceState.DEAD.getIntValue());
            deviceStateIntent.putExtra(Constants.NUMBER_EXTRA, address);
            sendBroadcast(deviceStateIntent);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(SERVICE_TAG + ".background",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        this.addressesToTurnOff = new HashSet<>();
        this.addressesToTurnOn = new HashSet<>();
        this.connectedDevices = new Hashtable<>();

        IntentFilter registerDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_REGISTER_DEVICE);
        registerReceiver(addDeviceReceiver, registerDeviceFilter);

        IntentFilter unregisterDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_UNREGISTER_DEVICE);
        registerReceiver(removeDeviceReceiver, unregisterDeviceFilter);

        Log.d(SERVICE_TAG, "OnCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(addDeviceReceiver);
        unregisterReceiver(removeDeviceReceiver);
        addressesToTurnOn.clear();
        addressesToTurnOff.clear();
        Set<String> keys = connectedDevices.keySet();
        for (Map.Entry<String, BSXInsightDeviceMock> entry : connectedDevices.entrySet()) {
            entry.getValue().cancelTimers();
        }

        try {
            List<String> devToRemove = new ArrayList();
            for (String key : keys) {
                devToRemove.add(key);
            }

            for (String devKey : devToRemove) {
                connectedDevices.remove(devKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class LocalBinder extends Binder {
        BSXInsightsServiceMock getService() {
            return BSXInsightsServiceMock.this;
        }
    }
}
