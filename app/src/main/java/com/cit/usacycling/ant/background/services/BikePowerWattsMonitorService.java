package com.cit.usacycling.ant.background.services;

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
import android.os.Process;
import android.util.Log;

import com.cit.usacycling.ant.background.callbacks.AntBikePowerCalibrationCallbackReceiver;
import com.cit.usacycling.ant.background.devices.BikePowerDevice;
import com.cit.usacycling.ant.background.devices.IAntDevice;
import com.cit.usacycling.ant.global.Constants;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by nikolay.nikolov on 15.10.2015
 */
public class BikePowerWattsMonitorService extends AntServiceBase implements AntBikePowerCalibrationCallbackReceiver {
    public static final String SERVICE_TAG = BikePowerWattsMonitorService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();

    private Hashtable<String, IAntDevice> devices;
    private Dictionary<String, PccReleaseHandle<AntPlusBikePowerPcc>> releaseHandles;
    private HandlerThread thread;
    private Looper looper;
    private final BroadcastReceiver addDeviceReceiver = new BroadcastReceiver() {
        @Override
        @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
            String number = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
            devices.put(number, new BikePowerDevice(number, getServiceContext()));
            IAntDevice device = devices.get(number);
            releaseHandles.put(
                    number,
                    AntPlusBikePowerPcc.requestAccess(
                            getServiceContext(),
                            Integer.parseInt(number),
                            0,
                            (AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>) device.getAccessResultReceiver(),
                            device.getDeviceStateChangeReceiver()));
        }
    };

    private final BroadcastReceiver removeDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String number = intent.getStringExtra(Constants.DEVICE_NUMBER_EXTRA);
            try {
                releaseHandles.get(number).close();
                releaseHandles.remove(number);
                devices.remove(number);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver setCtfSlopesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = "";
            try {
                message = "Successfully sent 'set slopes command' for running power meters";
                ArrayList<String> deviceAndSlopeArr = intent.getStringArrayListExtra(Constants.PM_SLOPES_EXTRA);

                for (int i = 0; i < deviceAndSlopeArr.size(); i++) {
                    String[] currentSet = deviceAndSlopeArr.get(i).split(" _ ");
                    BikePowerDevice device = (BikePowerDevice) devices.get(currentSet[0]);
                    if (device.getDeviceState().getIntValue() == DeviceState.TRACKING.getIntValue()) {
                        ((BikePowerDevice) devices.get(currentSet[0])).setCtfSlope(currentSet[1]);
                    } else {
                        message = "Failed to set slope value. " + device.getDeviceId() + " is not ready to communicate";
                        break;
                    }

                }
            } catch (Exception e) {
                message = "Failed to set power meter slopes due to internal exception";
                Log.e(SERVICE_TAG, e.toString());
            } finally {
                sendPMStatusMessage(message);
            }
        }
    };

    private final BroadcastReceiver manualCalibrationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = "";
            try {
                if (devices.size() == 0) {
                    message = "No running power meters";
                }

                for (Map.Entry<String, IAntDevice> set : devices.entrySet()) {
                    BikePowerDevice dev = ((BikePowerDevice) set.getValue());

                    if (dev.getDeviceState().getIntValue() == DeviceState.TRACKING.getIntValue()) {
                        ((BikePowerDevice) set.getValue()).requestManualCalibration(BikePowerWattsMonitorService.this);
                    } else {
                        message = "Manual calibration failed. s%" + dev.getDeviceId() + " is not ready to communicate";
                        break;
                    }
                }
            } catch (Exception e) {
                message = "Manual calibration failed due to internal exception";
                Log.e(SERVICE_TAG, e.toString());
            } finally {
                Intent statusIntent = new Intent(Constants.ACTION_PM_CONF_STATUS);
                statusIntent.putExtra(Constants.STATE_EXTRA, message);
                sendBroadcast(statusIntent);
            }
        }
    };

    @Override
    public void onCreate() {
        thread = new HandlerThread(SERVICE_TAG + ".background",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        looper = thread.getLooper();

        IntentFilter registerDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_REGISTER_DEVICE);
        registerReceiver(addDeviceReceiver, registerDeviceFilter);

        IntentFilter unregisterDeviceFilter = new IntentFilter(SERVICE_TAG + Constants.ACTION_UNREGISTER_DEVICE);
        registerReceiver(removeDeviceReceiver, unregisterDeviceFilter);

        IntentFilter setCtfSlopeFilter = new IntentFilter(Constants.ACTION_SET_CTF_SLOPE);
        registerReceiver(setCtfSlopesReceiver, setCtfSlopeFilter);

        IntentFilter setAutoZeroFilter = new IntentFilter(Constants.ACTION_MANUAL_CALIBRATION);
        registerReceiver(manualCalibrationReceiver, setAutoZeroFilter);
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
            }
        }

        unregisterReceiver(addDeviceReceiver);
        unregisterReceiver(removeDeviceReceiver);
        unregisterReceiver(manualCalibrationReceiver);
        unregisterReceiver(setCtfSlopesReceiver);
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restartHandle(final IAntDevice device) {
        new Handler(looper).post(new Runnable() {
            @Override
            public void run() {
                PccReleaseHandle<AntPlusBikePowerPcc> deviceHandle = releaseHandles.get(device.getDeviceId());
                if (deviceHandle != null) {
                    try {
                        deviceHandle.close();
                        releaseHandles.remove(device.getDeviceId());
                        deviceHandle = AntPlusBikePowerPcc.requestAccess(
                                getServiceContext(),
                                Integer.parseInt(device.getDeviceId()),
                                0,
                                (AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>) device.getAccessResultReceiver(),
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
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public Looper getLooper() {
        return this.looper;
    }

    private BikePowerWattsMonitorService getServiceContext() {
        return this;
    }

    @Override
    public void onCalibrationMessageArrived(AntPlusBikePowerPcc.CalibrationMessage message, String deviceId) {
        Intent calibrationIntent = new Intent(Constants.ACTION_MANUAL_CALIBRATION);
        calibrationIntent.putExtra(Constants.DEVICE_NUMBER_EXTRA, deviceId);
        calibrationIntent.putExtra(Constants.PM_MANUAL_CALIBRATION_EXTRA, message);
        sendBroadcast(calibrationIntent);
    }

    private class LocalBinder extends Binder {
        /*
            Return a handle to this service.
         */
        BikePowerWattsMonitorService getService() {
            return BikePowerWattsMonitorService.this;
        }
    }

    private void sendPMStatusMessage(String message) {
        Intent statusIntent = new Intent(Constants.ACTION_PM_CONF_STATUS);
        statusIntent.putExtra(Constants.STATE_EXTRA, message);
        sendBroadcast(statusIntent);
    }
}
