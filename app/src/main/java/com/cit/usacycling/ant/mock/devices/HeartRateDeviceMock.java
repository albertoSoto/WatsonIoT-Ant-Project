package com.cit.usacycling.ant.mock.devices;

import android.os.Handler;
import android.util.Log;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
import com.cit.usacycling.ant.background.devices.IAntDevice;
import com.cit.usacycling.ant.background.services.IAntServiceBase;
import com.cit.usacycling.ant.enums.DeviceDataType;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.DataCollector;
import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 12.11.2015
 */
public class HeartRateDeviceMock implements IAntDevice {
    @Inject
    DataCollector collector;
    private String deviceId;
    private AntPluginPcc hrPcc;
    private IAntServiceBase adopterService;
    private Timer sendDeviceStatusTimer;
    private Timer sendHeartRateTmer;

    private final AntPluginPcc.IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new AntPluginPcc.IDeviceStateChangeReceiver() {
                @Override
                public void onDeviceStateChange(final DeviceState newDeviceState) {
                    new Handler(adopterService.getLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            adopterService.sendDeviceState(newDeviceState.getIntValue(), deviceId);
                        }
                    });
                }
            };

    /*
        Callback object for getting the status of connect call.
     */
    private final AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> base_IPluginAccessResultReceiver =
            new AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc>() {
                //Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusHeartRatePcc result, RequestAccessResult resultCode,
                                             final DeviceState initialDeviceState) {
                    switch (resultCode) {
                        case SUCCESS:
                            hrPcc = result;
                            break;
                        case USER_CANCELLED:
                            break;
                        default:
                            // adopterService.restartHandle(getInstance());
                            break;
                    }

                    // base_IDeviceStateChangeReceiver.onDeviceStateChange(initialDeviceState);
                }
            };

    public HeartRateDeviceMock(final String deviceId, final IAntServiceBase adopterService) {
        USACyclingApplication.getObjectGraph().inject(this);

        this.deviceId = deviceId;
        this.adopterService = adopterService;
        base_IDeviceStateChangeReceiver.onDeviceStateChange(DeviceState.CLOSED);

        sendDeviceStatusTimer = new Timer();
        sendHeartRateTmer = new Timer();

        sendDeviceStatusTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                base_IDeviceStateChangeReceiver.onDeviceStateChange(DeviceState.TRACKING);
            }
        }, 1000);

        sendHeartRateTmer.schedule(new TimerTask() {
            @Override
            public void run() {
                final JSONObject data = new JSONObject();

                try {
                    data.put(Constants.Json.VALUE_KEY, 75);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    data.put(Constants.Json.DATA_STATE, AntPlusHeartRatePcc.DataState.ZERO_DETECTED.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new Handler(adopterService.getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        deviceId,
                                        DeviceType.HEARTRATE,
                                        DeviceDataType.HEARTRATE),
                                data);
                    }
                });
            }
        }, 550, 400);

    }

    public void cancelTimers() {
        try {
            sendDeviceStatusTimer.cancel();
            sendHeartRateTmer.cancel();
            base_IDeviceStateChangeReceiver.onDeviceStateChange(DeviceState.DEAD);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().getSimpleName(), "Exception caught in Heart rate device mock");
        }
    }

    public IAntDevice getInstance() {
        return this;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    public void subscribeToAntDeviceEvents() {
        ((AntPlusHeartRatePcc) hrPcc).subscribeHeartRateDataEvent(new AntPlusHeartRatePcc.IHeartRateDataReceiver() {
            @Override
            public void onNewHeartRateData(final long estTimestamp, EnumSet<EventFlag> eventFlags,
                                           final int computedHeartRate, final long heartBeatCount,
                                           final BigDecimal heartBeatEventTime, final AntPlusHeartRatePcc.DataState dataState) {
                final JSONObject data = new JSONObject();

                try {
                    data.put(Constants.Json.VALUE_KEY, computedHeartRate);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, estTimestamp);
                    data.put(Constants.Json.DATA_STATE, dataState.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        String.valueOf(hrPcc.getAntDeviceNumber()),
                                        DeviceType.HEARTRATE,
                                        DeviceDataType.HEARTRATE),
                                data);
                    }
                });
            }
        });

        ((AntPlusHeartRatePcc) hrPcc).subscribeCalculatedRrIntervalEvent(new AntPlusHeartRatePcc.ICalculatedRrIntervalReceiver() {
            @Override
            public void onNewCalculatedRrInterval(final long estTimestamp,
                                                  EnumSet<EventFlag> eventFlags, final BigDecimal rrInterval, final AntPlusHeartRatePcc.RrFlag flag) {

                String rrIntervalString = rrInterval.toString();
                final JSONObject data = new JSONObject();

                try {
                    data.put(Constants.Json.VALUE_KEY, rrIntervalString);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, estTimestamp);
                    data.put(Constants.Json.DATA_STATE, flag.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        String.valueOf(hrPcc.getAntDeviceNumber()),
                                        DeviceType.HEARTRATE,
                                        DeviceDataType.RRINTERVAL),
                                data);
                    }
                });

            }
        });
    }

    public AntPluginPcc.IPluginAccessResultReceiver<AntPlusHeartRatePcc> getAccessResultReceiver() {
        return this.base_IPluginAccessResultReceiver;
    }

    public AntPluginPcc.IDeviceStateChangeReceiver getDeviceStateChangeReceiver() {
        return this.base_IDeviceStateChangeReceiver;
    }

    public DeviceDataTypeStruct parseDeviceDataTypeStruct(String deviceId, DeviceType deviceType, DeviceDataType dataType) {
        return new DeviceDataTypeStruct(deviceId, deviceType, dataType);
    }
}
