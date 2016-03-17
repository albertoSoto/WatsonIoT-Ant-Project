package com.cit.usacycling.ant.global;

/**
 * Created by mrshah on 10/12/2015.
 * <p/>
 * Global definitions file.
 */
public class Constants {

    public static final String MESSAGE_DATA = "Message Data";
    public static final String CONNECTED = "CONNECTED";
    public static final String NOT_CONNECTED = "DISCONNECTED";
    public static final String BSX_DEVICE_TYPE = "BSXInsight";

    /*
        Intent extras
     */
    public static final String DEVICE_TYPE_EXTRA = "Device Type";
    public static final String DEVICE_NUMBER_EXTRA = "Device Number";
    public static final String PM_SLOPES_EXTRA = "Slopes";
    public static final String SERVICE_TAG = "Service tag";
    public static final String NAME_EXTRA = "name";
    public static final String TYPE_EXTRA = "type";
    public static final String NUMBER_EXTRA = "number";
    public static final String PAIRED_EXTRA = "paired";
    public static final String STATE_EXTRA = "state";
    public static final String PM_MANUAL_CALIBRATION_EXTRA = "calibrationMessage";
    public static final String MATCH_BURNED_EXTRA = "matchBurned";

    /*
        Intent filter actions
     */
    public static final String ACTION_RECONNECT = "Reconnect";
    public static final String ACTION_ANT_DEVICE_STATE_CHANGE = "DeviceStateChange";
    public static final String ACTION_DEVICE_FOUND = "DeviceFound";
    public static final String ACTION_CHANGE_IOT_STATUS = "ChangeIOTStatus";
    public static final String ACTION_ANT_DEVICE_STATUS = "AntDeviceStatus";
    public static final String ACTION_ANT_DEVICE_DATA_UPDATE = "DeviceUpdate";
    public static final String ACTION_REGISTER_DEVICE = "RegisterDevice";
    public static final String ACTION_UNREGISTER_DEVICE = "UnregisterDevice";
    public static final String ACTION_SET_CTF_SLOPE = "SetCTFSlope";
    public static final String ACTION_MANUAL_CALIBRATION = "RequestManualCalibration";
    public static final String ACTION_MANUAL_CALIBRATION_RESULT = "RequestManualCalibrationResult";
    public static final String ACTION_PM_CONF_STATUS = "PMStatus";
    public static final String ACTION_UPDATE_ADAPTER = "updateAdapter";
    public static final String BSX_STATUS = "BsxStatus";
    public static final String ACTION_MATCH_BURNED_CMD = "MatchBurnedCommand";
    public static final String ACTION_SOLO_GLASSES_RIDER_NOTIFICATION = "com.cit.usacycling.ant.NotifyRiderBroadcast";
    public static final int SMO2 = 20;

    public static final String MQTT_MATCH_BURNED_COMMAND_NAME = "matchesburned";
    public static final String STATUS_FLAG = "status";

    /*
        Subscription data
     */
    public static class Topic {
        public static final String EVENT_PREFIX = "iot-2/evt/";
        public static final String EVENT_FORMAT = "/fmt/json";
    }

    /*
        Json message keys
     */
    public static class Json {

        public static final String VALUE_KEY = "v";
        public static final String EST_TIMESTAMP_KEY = "t";
        public static final String DEVICE_ID_KEY = "i";
        public static final String DATA_STATE = "s";

        public static class MainPayload {
            public static final String SYSTEM_TIMESTAMP = "t";
            public static final String MESSAGE_ID = "a";
            public static final String DATA = "d";
        }
    }
}
