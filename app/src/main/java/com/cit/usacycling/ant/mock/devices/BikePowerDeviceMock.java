package com.cit.usacycling.ant.mock.devices;

import android.os.Handler;
import android.util.Log;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.DeviceDataTypeStruct;
import com.cit.usacycling.ant.background.callbacks.AntBikePowerCalibrationCallbackReceiver;
import com.cit.usacycling.ant.background.devices.IAntDevice;
import com.cit.usacycling.ant.background.services.IAntServiceBase;
import com.cit.usacycling.ant.enums.DeviceDataType;
import com.cit.usacycling.ant.global.Constants;
import com.cit.usacycling.ant.global.DataCollector;
import com.cit.usacycling.ant.global.SharedSettings;
import com.dsi.ant.plugins.antplus.pcc.AntPlusBikePowerPcc;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EnumSet;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

/**
 * Created by Nikolay on 1.11.2015
 */
public class BikePowerDeviceMock implements IAntDevice {
    @Inject
    DataCollector collector;
    @Inject
    SharedSettings settings;
    private String deviceId;
    private AntPluginPcc pwrPcc;
    private IAntServiceBase adopterService;
    private Timer sendDeviceStatusTimer;
    private Timer sendPowerTimer;
    private Timer sendTorqueTimer;
    private Timer sendCadenceTimer;
    private DeviceState deviceState;
    private static int POWER_VALUE = 50;
    private static int CADENCE_VALUE = 50;

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

    public BikePowerDeviceMock(final String deviceId, final IAntServiceBase adopterService) {
        USACyclingApplication.getObjectGraph().inject(this);
        this.deviceId = deviceId;
        this.adopterService = adopterService;
        base_IDeviceStateChangeReceiver.onDeviceStateChange(DeviceState.CLOSED);
        sendCadenceTimer = new Timer();
        sendDeviceStatusTimer = new Timer();
        sendPowerTimer = new Timer();
        sendTorqueTimer = new Timer();
        settings.setPMPowerValue(deviceId, POWER_VALUE);
        settings.setCadenceValue(deviceId, CADENCE_VALUE);

        sendDeviceStatusTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                base_IDeviceStateChangeReceiver.onDeviceStateChange(DeviceState.TRACKING);
            }
        }, 200);

        sendCadenceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                String calculatedCrankCadenceStr = String.valueOf(settings.getCadenceValue(deviceId));
                final JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, calculatedCrankCadenceStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    data.put(Constants.Json.DATA_STATE, AntPlusBikePowerPcc.DataSource.CRANK_TORQUE_DATA.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new Handler(adopterService.getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        deviceId,
                                        DeviceType.BIKE_POWER,
                                        DeviceDataType.CADENCE),
                                data);
                    }
                });
            }
        }, 500, 250);

        sendPowerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                String calculatedPowerStr = String.valueOf(settings.getPMPowerValue(deviceId));
                final JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, calculatedPowerStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
                    data.put(Constants.Json.DATA_STATE, AntPlusBikePowerPcc.DataSource.POWER_ONLY_DATA.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new Handler(adopterService.getLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        collector.addData(
                                parseDeviceDataTypeStruct(
                                        deviceId,
                                        DeviceType.BIKE_POWER,
                                        DeviceDataType.POWER),
                                data);
                    }
                });
            }
        }, 450, 250);

//        sendTorqueTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                    Random rand = new Random();
//
//                    String torqueStr = String.valueOf(rand.nextInt(50) + 10);
//                    final JSONObject data = new JSONObject();
//                    try {
//                        data.put(Constants.Json.VALUE_KEY, torqueStr);
//                        data.put(Constants.Json.EST_TIMESTAMP_KEY, System.currentTimeMillis());
//                        data.put(Constants.Json.DATA_STATE, AntPlusBikePowerPcc.DataSource.WHEEL_TORQUE_DATA.getIntValue());
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//
//                    Handler b = new Handler(Looper.getMainLooper());
//                    b.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            collector.addData(
//                                    parseDeviceDataTypeStruct(
//                                            deviceId,
//                                            DeviceType.BIKE_POWER,
//                                            DeviceDataType.TORQUE),
//                                    data);
//                        }
//                    });
//                }
//        }, 400, 250);
    }

    public void cancelTimers() {
        try {
            sendDeviceStatusTimer.cancel();
            sendPowerTimer.cancel();
            sendCadenceTimer.cancel();
            sendTorqueTimer.cancel();
            base_IDeviceStateChangeReceiver.onDeviceStateChange(DeviceState.DEAD);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().getSimpleName(), "Exception caught in Bike power device mock");
        }
    }

    public IAntDevice getInstance() {
        return this;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public void subscribeToAntDeviceEvents() {
        ((AntPlusBikePowerPcc) pwrPcc).subscribeCalculatedPowerEvent(new AntPlusBikePowerPcc.ICalculatedPowerReceiver() {
            @Override
            public void onNewCalculatedPower(long estTimestamp, EnumSet<EventFlag> enumSet, AntPlusBikePowerPcc.DataSource dataSource, BigDecimal calculatedPower) {
                String calculatedPowerStr = calculatedPower.toString();
                JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, calculatedPowerStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, estTimestamp);
                    data.put(Constants.Json.DATA_STATE, dataSource.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                collector.addData(
                        parseDeviceDataTypeStruct(
                                String.valueOf(pwrPcc.getAntDeviceNumber()),
                                DeviceType.BIKE_POWER,
                                DeviceDataType.POWER),
                        data);
            }
        });

        ((AntPlusBikePowerPcc) pwrPcc).subscribeCalculatedTorqueEvent(new AntPlusBikePowerPcc.ICalculatedTorqueReceiver() {
            @Override
            public void onNewCalculatedTorque(
                    final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                    final AntPlusBikePowerPcc.DataSource dataSource,
                    final BigDecimal calculatedTorque) {
                String torqueStr = calculatedTorque.toString();
                JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, torqueStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, estTimestamp);
                    data.put(Constants.Json.DATA_STATE, dataSource.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                collector.addData(
                        parseDeviceDataTypeStruct(
                                String.valueOf(pwrPcc.getAntDeviceNumber()),
                                DeviceType.BIKE_POWER,
                                DeviceDataType.TORQUE),
                        data);
            }
        });


        ((AntPlusBikePowerPcc) pwrPcc).subscribeCalculatedCrankCadenceEvent(new AntPlusBikePowerPcc.ICalculatedCrankCadenceReceiver() {
            @Override
            public void onNewCalculatedCrankCadence(
                    final long estTimestamp, final EnumSet<EventFlag> eventFlags,
                    final AntPlusBikePowerPcc.DataSource dataSource,
                    final BigDecimal calculatedCrankCadence) {

                String calculatedCrankCadenceStr = calculatedCrankCadence.toString();
                JSONObject data = new JSONObject();
                try {
                    data.put(Constants.Json.VALUE_KEY, calculatedCrankCadenceStr);
                    data.put(Constants.Json.EST_TIMESTAMP_KEY, estTimestamp);
                    data.put(Constants.Json.DATA_STATE, dataSource.getIntValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                collector.addData(
                        parseDeviceDataTypeStruct(
                                String.valueOf(pwrPcc.getAntDeviceNumber()),
                                DeviceType.BIKE_POWER,
                                DeviceDataType.CADENCE),
                        data);
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

    public boolean setCtfSlope(String slope) {
        return true;
    }

    public boolean setAutoZero() {
        return true;
    }

    public void requestManualCalibration(final AntBikePowerCalibrationCallbackReceiver receiver) {
        Random r = new Random();
        AntPlusBikePowerPcc.CalibrationMessage calibrationMessage = new AntPlusBikePowerPcc.CalibrationMessage(
                AntPlusBikePowerPcc.CalibrationId.CTF_ZERO_OFFSET,
                Integer.parseInt(String.valueOf(new Date().getTime() % (86400000))),
                r.nextInt(10) + 5,
                "this is test".getBytes());
        receiver.onCalibrationMessageArrived(calibrationMessage, getDeviceId());
    }
}
