package com.cit.usacycling.ant.background.devices;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
import com.cit.usacycling.ant.background.callbacks.AntBikePowerCalibrationCallbackReceiver;
import com.cit.usacycling.ant.background.services.IAntServiceBase;
import com.cit.usacycling.ant.enums.DeviceDataType;
import com.cit.usacycling.ant.global.CToast;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.DataCollector;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestStatus;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPlusCommonPcc;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.EnumSet;

import javax.inject.Inject;

/**
 * Created by Nikolay on 1.11.2015
 */
public class BikePowerDevice implements IAntDevice {
    @Inject
    DataCollector collector;
    @Inject
    CToast cToast;

    private String deviceId;
    private AntPluginPcc pwrPcc;
    private IAntServiceBase adopterService;
    private DeviceState deviceState;

    private final AntPluginPcc.IDeviceStateChangeReceiver base_IDeviceStateChangeReceiver =
            new AntPluginPcc.IDeviceStateChangeReceiver() {
                @Override
                public void onDeviceStateChange(final DeviceState newDeviceState) {
                    new Handler(adopterService.getLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            deviceState = newDeviceState;
                            adopterService.sendDeviceState(newDeviceState.getIntValue(), deviceId);
                        }
                    });
                }
            };

    /*
        Callback object for getting the status of connect call.
     */
    private final AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc> base_IPluginAccessResultReceiver =
            new AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc>() {
                //Handle the result, connecting to events on success or reporting failure to user.
                @Override
                public void onResultReceived(AntPlusBikePowerPcc result, RequestAccessResult resultCode,
                                             DeviceState initialDeviceState) {
                    switch (resultCode) {
                        case SUCCESS:
                            pwrPcc = result;
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

    final AntPlusCommonPcc.IRequestFinishedReceiver requestFinishedReceiver =
            new AntPlusCommonPcc.IRequestFinishedReceiver() {
                @Override
                public void onNewRequestFinished(final RequestStatus requestStatus) {
                    new Handler(adopterService.getLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            switch (requestStatus) {
                                case SUCCESS:
                                    break;
                                case FAIL_PLUGINS_SERVICE_VERSION:
                                    cToast.makeText("Plugin Service Upgrade Required",
                                            Toast.LENGTH_SHORT);
                                    break;
                                default:
                                    cToast.makeText("Request Failed to be Sent",
                                            Toast.LENGTH_SHORT);
                                    break;
                            }
                        }
                    });
                }
            };

    public BikePowerDevice(String deviceId, IAntServiceBase adopterService) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.deviceId = deviceId;
        this.adopterService = adopterService;
    }

    public IAntDevice getInstance() {
        return this;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    public boolean setCtfSlope(String slope) {
        BigDecimal newSlope;
        try {
            newSlope = new BigDecimal(slope);

            if (pwrPcc != null) {
                boolean setSlope = ((AntPlusBikePowerPcc) pwrPcc).requestSetCtfSlope(newSlope, requestFinishedReceiver);
                return setSlope;
            }
        } catch (Exception e) {
            Log.e(BikePowerDevice.class.getSimpleName(), e.toString());
            return false;
        }

        return false;
    }

    public boolean setAutoZero() {
        return ((AntPlusBikePowerPcc) pwrPcc).requestSetAutoZero(true, requestFinishedReceiver);
    }

    public void requestManualCalibration(final AntBikePowerCalibrationCallbackReceiver receiver) {
        ((AntPlusBikePowerPcc) pwrPcc).subscribeCalibrationMessageEvent(new AntPlusBikePowerPcc.ICalibrationMessageReceiver() {
            @Override
            public void onNewCalibrationMessage(long l, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.CalibrationMessage calibrationMessage) {
                receiver.onCalibrationMessageArrived(calibrationMessage, getDeviceId());
            }
        });

        ((AntPlusBikePowerPcc) pwrPcc).requestManualCalibration(requestFinishedReceiver);
    }

    @Override
    public void subscribeToAntDeviceEvents() {
        ((AntPlusBikePowerPcc) pwrPcc).subscribeCalculatedPowerEvent(new AntPlusBikePowerPcc.ICalculatedPowerReceiver() {
            @Override
            public void onNewCalculatedPower(long estTimestamp, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.DataSource dataSource, BigDecimal calculatedPower) {
                String calculatedPowerStr = calculatedPower.toString();
                final JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, calculatedPowerStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    data.put(Constants.Json.DATA_STATE, dataSource.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new Handler(adopterService.getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        String.valueOf(pwrPcc.getAntDeviceNumber()),
                                        DeviceType.BIKE_POWER,
                                        DeviceDataType.POWER),
                                data);
                    }
                });
            }
        });

        ((AntPlusBikePowerPcc) pwrPcc).subscribeCalculatedTorqueEvent(new AntPlusBikePowerPcc.ICalculatedTorqueReceiver() {
            @Override
            public void onNewCalculatedTorque(
                    final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                    final AntPlusBikePowerPcc.DataSource dataSource,
                    final BigDecimal calculatedTorque) {
                String torqueStr = calculatedTorque.toString();
                final JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, torqueStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    data.put(Constants.Json.DATA_STATE, dataSource.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                new Handler(adopterService.getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        String.valueOf(pwrPcc.getAntDeviceNumber()),
                                        DeviceType.BIKE_POWER,
                                        DeviceDataType.TORQUE),
                                data);
                    }
                });
            }
        });

        ((AntPlusBikePowerPcc) pwrPcc).subscribeCalculatedCrankCadenceEvent(new AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver() {
            @Override
            public void onNewCalculatedCrankCadence(
                    final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                    final AntPlusBikePowerPcc.DataSource dataSource,
                    final BigDecimal calculatedCrankCadence) {
                String calculatedCrankCadenceStr = calculatedCrankCadence.toString();
                final JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, calculatedCrankCadenceStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    data.put(Constants.Json.DATA_STATE, dataSource.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new Handler(adopterService.getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        String.valueOf(pwrPcc.getAntDeviceNumber()),
                                        DeviceType.BIKE_POWER,
                                        DeviceDataType.CADENCE),
                                data);
                    }
                });
            }
        });
    }

    public DeviceState getDeviceState() {
        return this.deviceState;
    }

    public AntPluginPcc.IPluginAccessResultReceiver<AntPlusBikePowerPcc> getAccessResultReceiver() {
        return this.base_IPluginAccessResultReceiver;
    }

    public AntPluginPcc.IDeviceStateChangeReceiver getDeviceStateChangeReceiver() {
        return this.base_IDeviceStateChangeReceiver;
    }

    @Override
    public DeviceDataTypeStruct parseDeviceDataTypeStruct(String deviceId, DeviceType deviceType, DeviceDataType dataType) {
        return new DeviceDataTypeStruct(deviceId, deviceType, dataType);
    }
}

