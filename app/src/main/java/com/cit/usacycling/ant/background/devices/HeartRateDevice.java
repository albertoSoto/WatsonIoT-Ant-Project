package com.cit.usacycling.ant.background.devices;

import android.os.Handler;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
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

import javax.inject.Inject;

/**
 * Created by Nikolay on 1.11.2015
 */
public class HeartRateDevice implements IAntDevice {
    @Inject
    DataCollector collector;
    private String deviceId;
    private AntPluginPcc hrPcc;
    private IAntServiceBase adopterService;

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
                            subscribeToAntDeviceEvents();
                            break;
                        case USER_CANCELLED:
                            break;
                        default:
                            adopterService.restartHandle(getInstance());
                            break;
                    }

                    base_IDeviceStateChangeReceiver.onDeviceStateChange(initialDeviceState);
                }
            };

    public HeartRateDevice(String deviceId, IAntServiceBase adopterService) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.deviceId = deviceId;
        this.adopterService = adopterService;
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
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    data.put(Constants.Json.DATA_STATE, dataState.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new Handler(adopterService.getLooper()).post(new Runnable() {
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
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
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
