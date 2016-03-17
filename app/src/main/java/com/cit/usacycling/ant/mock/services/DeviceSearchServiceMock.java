package com.cit.usacycling.ant.mock.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.db.UnitOfWork;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.Constants;
import com.dsi.ant.plugins.antplus.pcc.MultiDeviceSearch;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;

import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import static com.dsi.ant.plugins.antplus.pcc.defines.DeviceType.BIKE_CADENCE;
import static com.dsi.ant.plugins.antplus.pcc.defines.DeviceType.BIKE_POWER;
import static com.dsi.ant.plugins.antplus.pcc.defines.DeviceType.BIKE_SPD;
import static com.dsi.ant.plugins.antplus.pcc.defines.DeviceType.BIKE_SPDCAD;
import static com.dsi.ant.plugins.antplus.pcc.defines.DeviceType.HEARTRATE;

/**
 * Created by nikolay.nikolov on 30.10.2015
 */
public class DeviceSearchServiceMock extends Service {
    public static final String SERVICE_TAG = DeviceSearchServiceMock.class.getSimpleName();
    static BluetoothAdapter mBTAdapter = null;
    private final Handler bsxSearchHandler = new Handler();
    private final Handler antSearchHandler = new Handler();
    @Inject
    UnitOfWork unitOfWork;
    @Inject
    CToast cToast;

    private MultiDeviceSearch mds;
    private final Runnable searchAntDevices = new Runnable() {
        @Override
        public void run() {
            searchDevices();
        }
    };

    private final Runnable searchBsxDevices = new Runnable() {
        @Override
        public void run() {
            scanLeDevice();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        USACyclingApplication.getObjectGraph().inject(this);
        antSearchHandler.removeCallbacks(searchAntDevices);
        bsxSearchHandler.removeCallbacks(searchBsxDevices);
        antSearchHandler.post(searchAntDevices);
        bsxSearchHandler.post(searchBsxDevices);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                showMockDevices();
            }
        }, 1000);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mds != null) {
            mds.close();
        }
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread(SERVICE_TAG + ".background",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showMockDevices() {
        int powerMetersMockCount = 4;
        int heartRateMonitorsCount = 3;
        int bsxDevicesCount = 4;

        for (int powerMeterIndex = 1; powerMeterIndex <= powerMetersMockCount; powerMeterIndex++) {
            Intent deviceFoundIntent = new Intent();
            deviceFoundIntent.setAction(Constants.ACTION_DEVICE_FOUND);
            deviceFoundIntent.putExtra(Constants.NAME_EXTRA, "PM" + powerMeterIndex);
            deviceFoundIntent.putExtra(Constants.TYPE_EXTRA, DeviceType.BIKE_POWER.toString());
            deviceFoundIntent.putExtra(Constants.NUMBER_EXTRA, powerMeterIndex + "1000");
            deviceFoundIntent.putExtra(Constants.PAIRED_EXTRA, false);
            sendBroadcast(deviceFoundIntent);
        }

        for (int heartMonitorIndex = 1; heartMonitorIndex <= heartRateMonitorsCount; heartMonitorIndex++) {
            Intent deviceFoundIntent = new Intent();
            deviceFoundIntent.setAction(Constants.ACTION_DEVICE_FOUND);
            deviceFoundIntent.putExtra(Constants.NAME_EXTRA, "HRM" + heartMonitorIndex);
            deviceFoundIntent.putExtra(Constants.TYPE_EXTRA, DeviceType.HEARTRATE.toString());
            deviceFoundIntent.putExtra(Constants.NUMBER_EXTRA, heartMonitorIndex + "2000");
            deviceFoundIntent.putExtra(Constants.PAIRED_EXTRA, false);
            sendBroadcast(deviceFoundIntent);
        }

        for (int bsxDeviceIndex = 1; bsxDeviceIndex <= bsxDevicesCount; bsxDeviceIndex++) {
            Intent deviceFoundIntent = new Intent();
            deviceFoundIntent.setAction(Constants.ACTION_DEVICE_FOUND);
            deviceFoundIntent.putExtra(Constants.NAME_EXTRA, "BSX" + bsxDeviceIndex);
            deviceFoundIntent.putExtra(Constants.TYPE_EXTRA, Constants.BSX_DEVICE_TYPE);
            deviceFoundIntent.putExtra(Constants.NUMBER_EXTRA, bsxDeviceIndex + "3000");
            deviceFoundIntent.putExtra(Constants.PAIRED_EXTRA, false);
            sendBroadcast(deviceFoundIntent);
        }
    }

    public void scanLeDevice() {
        mBTAdapter.startLeScan(mLeScanCallback);
        Log.d(SERVICE_TAG, "Scan for BSX devices started");
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();

                    Log.d("BSX", "Found Device");
                    Log.d("BSX", "Device Name=" + deviceName);
                    Log.d("BSX", "Device Type=" + device.getType());
                    Log.d("BSX", "Device Address=" + deviceAddress);
                    if (device.getName() != null) {
                        if (device.getName().equals("insight")) {
                            Intent deviceFoundIntent = new Intent();
                            deviceFoundIntent.setAction(Constants.ACTION_DEVICE_FOUND);
                            deviceFoundIntent.putExtra(Constants.NAME_EXTRA, deviceName);
                            deviceFoundIntent.putExtra(Constants.TYPE_EXTRA, Constants.BSX_DEVICE_TYPE);
                            deviceFoundIntent.putExtra(Constants.NUMBER_EXTRA, deviceAddress);
                            deviceFoundIntent.putExtra(Constants.PAIRED_EXTRA, unitOfWork.getDeviceRepository().getAllPairedDeviceIds().contains(deviceAddress));
                            sendBroadcast(deviceFoundIntent);
                        }
                    }
                }
            };

    private void searchDevices() {
        if (mds != null) {
            mds.close();
            mds = null;
        }

        EnumSet<DeviceType> deviceTypes = EnumSet.of(HEARTRATE, BIKE_CADENCE, BIKE_POWER, BIKE_SPD, BIKE_SPDCAD);
        mds = new MultiDeviceSearch(getApplicationContext(), deviceTypes,
                new MultiDeviceSearch.SearchCallbacks() {
                    @Override
                    public void onSearchStarted(MultiDeviceSearch.RssiSupport rssiSupport) {
                        Log.d(SERVICE_TAG, "Search Started");
                        showMockDevices();
                    }

                    @Override
                    public void onDeviceFound(com.dsi.ant.plugins.antplus.pccbase.MultiDeviceSearch.MultiDeviceSearchResult multiDeviceSearchResult) {
                        Log.d(SERVICE_TAG, "Search - Device found");
                        Log.d(SERVICE_TAG, "Device " + multiDeviceSearchResult.getDeviceDisplayName());
                        Log.d(Constants.DEVICE_TYPE_EXTRA, multiDeviceSearchResult.getAntDeviceType().toString());

                        Intent deviceFoundIntent = new Intent();
                        deviceFoundIntent.setAction(Constants.ACTION_DEVICE_FOUND);
                        deviceFoundIntent.putExtra(Constants.NAME_EXTRA, multiDeviceSearchResult.getDeviceDisplayName());
                        deviceFoundIntent.putExtra(Constants.TYPE_EXTRA, multiDeviceSearchResult.getAntDeviceType().toString());
                        deviceFoundIntent.putExtra(Constants.NUMBER_EXTRA, "" + multiDeviceSearchResult.getAntDeviceNumber());
                        deviceFoundIntent.putExtra(Constants.PAIRED_EXTRA, unitOfWork.getDeviceRepository().getAllPairedDeviceIds().contains(String.valueOf(multiDeviceSearchResult.getAntDeviceNumber())));
                        sendBroadcast(deviceFoundIntent);
                    }

                    @Override
                    public void onSearchStopped(RequestAccessResult requestAccessResult) {
                        /*
                            If the reason for stop searchAntDevices is ADAPTER_NOT_DETECTED, restart the searchAntDevices after 500ms.
                         */
                        Log.d(SERVICE_TAG + "Result Value", "" + requestAccessResult.getIntValue());
                        Log.d(SERVICE_TAG + "Result", "" + requestAccessResult.toString());
                        Log.d(SERVICE_TAG, "Search Stopped");

                        if (requestAccessResult == RequestAccessResult.ADAPTER_NOT_DETECTED
                                || requestAccessResult == RequestAccessResult.CHANNEL_NOT_AVAILABLE
                                || requestAccessResult == RequestAccessResult.OTHER_FAILURE) {
                            mds.close();
                            cToast.makeText("Problem occured while searching.\nReason: " + requestAccessResult.toString() + "\nRestart searchAntDevices again,.", Toast.LENGTH_SHORT);
                            antSearchHandler.removeCallbacks(searchAntDevices);
                            antSearchHandler.postDelayed(searchAntDevices, 500);
                        }
                    }
                });
    }
}
