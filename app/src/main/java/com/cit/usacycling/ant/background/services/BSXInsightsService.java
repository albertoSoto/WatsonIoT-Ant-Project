package com.cit.usacycling.ant.background.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.cit.usacycling.ant.background.devices.BSXInsightDevice;
import com.cit.usacycling.ant.enums.BSXDeviceConnection;
import com.cit.usacycling.ant.global.Constants;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

public class BSXInsightsService extends Service {

    public static final String SERVICE_TAG = BSXInsightsService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    boolean mScanning = false;
    static BluetoothAdapter mBTAdapter = null;
    HashSet<String> addressesToTurnOn;
    HashSet<String> addressesToTurnOff;
    Hashtable<String, BSXInsightDevice> connectedDevices;
    Handler mHandler;

    public class LocalBinder extends Binder {
        BSXInsightsService getService() {
            return BSXInsightsService.this;
        }
    }

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
        Log.d(SERVICE_TAG, "OnCreate");
        mHandler = new Handler();

        IntentFilter registerDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_REGISTER_DEVICE);
        registerReceiver(addDeviceReceiver, registerDeviceFilter);

        IntentFilter unregisterDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_UNREGISTER_DEVICE);
        registerReceiver(removeDeviceReceiver, unregisterDeviceFilter);

        IntentFilter deviceStateChangeFilter = new IntentFilter(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
        registerReceiver(deviceStateChangeReceiver, deviceStateChangeFilter);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.postDelayed(rAddDevices, 250);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        addressesToTurnOn.clear();
        addressesToTurnOff.clear();
        unregisterReceiver(addDeviceReceiver);
        unregisterReceiver(removeDeviceReceiver);
        unregisterReceiver(deviceStateChangeReceiver);

        mHandler.removeCallbacks(rAddDevices);
        mHandler.removeCallbacks(rRemoveDevices);
        for (Map.Entry<String, BSXInsightDevice> entry : connectedDevices.entrySet()) {
            entry.getValue().disconnectGatt();
            connectedDevices.remove(entry.getKey());
        }

        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(leScanTurnOffDeviceCallback);
            mBTAdapter.stopLeScan(leScanTurnOnDeviceCallback);
        }
    }

    private final Runnable rRemoveDevices = new Runnable() {
        @Override
        public void run() {
//
//            if (!mScanning) {
//                mBTAdapter.startLeScan(leScanTurnOffDeviceCallback);
            //           } else {
            //               new Handler().postDelayed(rRemoveDevices, 250);
            //           }
        }
    };

    private final Runnable rAddDevices = new Runnable() {
        @Override
        public void run() {
            if (!addressesToTurnOn.isEmpty()) {
                Log.d("rAddDevices", "Addresses to turn on size = " + addressesToTurnOn.size());
                if (!mScanning) {
                    Log.d("rAddDevices", "startLeScan Called");
                    mBTAdapter.startLeScan(leScanTurnOnDeviceCallback);
                    mScanning = true;
                } else {
                }
            }
            mHandler.postDelayed(rAddDevices, 1000);
        }
    };

    private final BroadcastReceiver addDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("DeviceReceiver", "addDeviceReceiver Called");
            String address = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
            addressesToTurnOn.add(address);
            addressesToTurnOff.remove(address);
//            mHandler.postDelayed(rAddDevices, 250);
        }
    };

    private void updateStatus(BSXDeviceConnection sStatus, String address) {
        Intent deviceStateIntent = new Intent();
        deviceStateIntent.setAction(Constants.ACTION_ANT_DEVICE_STATE_CHANGE);
        deviceStateIntent.putExtra(Constants.STATE_EXTRA, sStatus.getConnectionId());
        deviceStateIntent.putExtra(Constants.NUMBER_EXTRA, address);
        this.sendBroadcast(deviceStateIntent);
        Log.d("BSX", "Broadcast Sent");
    }

    private final BroadcastReceiver removeDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);

            if (null != connectedDevices.get(address)) {
                Log.d("BSXSERVICE", "Remove device found");
                BSXInsightDevice device = connectedDevices.remove(address);
                //connectedDevices.get(address).disconnectGatt();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                device.disconnectGatt();
            } else {
                Log.d("BSXSERVICE", "Remove device null");
                updateStatus(BSXDeviceConnection.DISCONNECTED, address);
            }

            addressesToTurnOn.remove(address);
            mHandler.postDelayed(rRemoveDevices, 250);
        }
    };

    private BluetoothAdapter.LeScanCallback leScanTurnOnDeviceCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.d("lescancallback", "onlescan");
                    mScanning = false;
                    String deviceAddress = device.getAddress();
                    if ((device.getName() != null && device.getName().equals("insight")) && addressesToTurnOn.contains(deviceAddress)) {
                        Log.d("BSX", "Turning ON Device");
                        Log.d("BSX", "Device Name=" + device.getName());
                        Log.d("BSX", "Device Address=" + deviceAddress);
                        BSXInsightDevice newDevice = new BSXInsightDevice(device, getApplicationContext());
                        if (newDevice.connectGatt()) {
                            Log.d("BSX", "Removing device from turn on list");
                            addressesToTurnOn.remove(deviceAddress);
                            connectedDevices.put(deviceAddress, newDevice);
                        }

                        if (addressesToTurnOn.isEmpty()) {
                            mBTAdapter.stopLeScan(this);
//                            mScanning = false;
                        }
                    }
                }
            };

    private BluetoothAdapter.LeScanCallback leScanTurnOffDeviceCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mScanning = true;
                    String deviceAddress = device.getAddress();
                    if ((device.getName() != null && device.getName().equals("insight")) && addressesToTurnOff.contains(deviceAddress)) {
                        Log.d("BSX", "Turning OFF Device");
                        Log.d("BSX", "Device Name=" + device.getName());
                        Log.d("BSX", "Device Address=" + deviceAddress);
                        BSXInsightDevice connectedDevice = new BSXInsightDevice(device, getApplicationContext());
                        if (connectedDevice.disconnectGatt()) {
                            addressesToTurnOff.remove(deviceAddress);
                            connectedDevices.remove(deviceAddress);
                        }

                        if (addressesToTurnOff.isEmpty()) {
                            mBTAdapter.stopLeScan(this);
                            mScanning = false;
                        }
                    }
                }
            };

    private final BroadcastReceiver deviceStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String state = intent.getStringExtra(Constants.STATE_EXTRA);
            final String number = intent.getStringExtra(Constants.NUMBER_EXTRA);

            if (state != null && state.equals(BSXDeviceConnection.DISCONNECTED.toString())) {
                Log.d("BSXService", "Received Status Broadcast DISCONNECTED");
                Log.d("BSXService", "Removing the device");
                if (null != connectedDevices.remove(number)) {
                    addressesToTurnOn.add(number);
                }
            }
        }
    };
}
