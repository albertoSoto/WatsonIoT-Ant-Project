package com.cit.usacycling.ant.global;

import android.content.SharedPreferences;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.ui.DevicesListItem;

import java.util.Set;

import javax.inject.Inject;

/**
 * This class provides methods for modifying user settings and preferences on
 * device.
 *
 * @author nikolay.nikolov
 */
public class SharedSettings {
    public static final String STATUS = "_status";
    public static final String STARTED_ITEM_ID = "startedItemId";
    public static final String TOTAL_MESSAGES_COUNT = "totalMessagesCount";
    public static final String TOTAL_DATA_MESSAGES_COUNT = "totalDataMessagesCount";
    public static final String TOTAL_STATUS_MESSAGES_COUNT = "totalStatusMessagesCount";
    public static final String TOTAL_DELIVERED_MESSAGES_COUNT = "totalDeliveredMessagesCount";
    public static final String DELIVERED_DATA_MESSAGES_COUNT = "deliveredDataMessagesCount";
    public static final String DELIVERED_STATUS_MESSAGES_COUNT = "deliveredStatusMessagesCount";
    public static final String FORCED_CLOSE_STATE = "forcedCloseState";
    public static final String POWER_DEVICE = "powerDevice";
    public static final String IOT_CREDENTIALS = "iotCredentials";
    public static final String DATA_MSG_QOS = "dataMsgQos";
    public static final String STATUS_MSG_QOS = "statusMsgQos";
    public static final String PM_SLOPE_POSTFIX = "_slope";
    public static final String CALIBRATION_MESSAGE = "calibrationMessage_";

    @Inject
    SharedPreferences preferences;

    public SharedSettings() {
        USACyclingApplication.getObjectGraph().inject(this);
    }

    private boolean getBooleanPreference(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public void startService(String serviceTag) {
        setBooleanPreference(serviceTag, true);
    }

    private void setBooleanPreference(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void setStringPreference(String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private Set<String> getStringSetPreference(String key) {
        return preferences.getStringSet(key, null);
    }

    private void setStringSetPreference(String key, Set<String> value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(key, value);
        editor.apply();
    }

    private void clearPreferenceValue(String key) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }

    private String getStringPreference(String key) {
        return preferences.getString(key, null);
    }

    protected int getIntPreference(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    protected void setIntPreference(String key, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    protected long getLongPreference(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    protected void setLongPreference(String key, long value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public boolean getForceClosedState() {
        return this.getBooleanPreference(FORCED_CLOSE_STATE, false);
    }

    public void setForceClosedState(boolean state) {
        this.setBooleanPreference(FORCED_CLOSE_STATE, state);
    }

    public String getItemStartedService(String serviceTag) {
        return getStringPreference(serviceTag + STARTED_ITEM_ID);
    }

    public void stopService(String serviceTag) {
        clearPreferenceValue(serviceTag);
        clearItemStartedSerevice(serviceTag);
    }

    private void clearItemStartedSerevice(String serviceTag) {
        clearPreferenceValue(serviceTag + STARTED_ITEM_ID);
    }

    public void setDeviceLastStatus(String deviceNumber, int status) {
        setIntPreference(deviceNumber + STATUS, status);
    }

    public int getDeviceLastStatus(String deviceNumber) {
        return getIntPreference(deviceNumber + STATUS, 0);
    }

    public void setTotalMessagesCount(long count) {
        this.setLongPreference(TOTAL_MESSAGES_COUNT, count);
    }

    public long getTotalMessagesCount() {
        long countByVar = this.getLongPreference(TOTAL_MESSAGES_COUNT, 0);
        long countBySum = getTotalDataMessagesCount() + getTotalStatusMessagesCount();
        return countByVar >= countBySum ? countByVar : countBySum;
    }

    public void setTotalDataMessagesCount(long count) {
        this.setLongPreference(TOTAL_DATA_MESSAGES_COUNT, count);
    }

    public long getTotalDataMessagesCount() {
        return this.getLongPreference(TOTAL_DATA_MESSAGES_COUNT, 0);
    }

    public void setTotalStatusMessagesCount(long count) {
        this.setLongPreference(TOTAL_STATUS_MESSAGES_COUNT, count);
    }

    public long getTotalStatusMessagesCount() {
        return this.getLongPreference(TOTAL_STATUS_MESSAGES_COUNT, 0);
    }

    public void setTotalDeliveredMessagesCount(long count) {
        this.setLongPreference(TOTAL_DELIVERED_MESSAGES_COUNT, count);
    }

    public long getTotalDeliveredMessagesCount() {
        return this.getLongPreference(TOTAL_DELIVERED_MESSAGES_COUNT, 0);
    }

    public void setDeliveredDataMessagesCount(long count) {
        this.setLongPreference(DELIVERED_DATA_MESSAGES_COUNT, count);
    }

    public long getDeliveredDataMessagesCount() {
        return this.getLongPreference(DELIVERED_DATA_MESSAGES_COUNT, 0);
    }

    public void setDeliveredStatusMessagesCount(long count) {
        this.setLongPreference(DELIVERED_STATUS_MESSAGES_COUNT, count);
    }

    public long getDeliveredStatusMessagesCount() {
        return this.getLongPreference(DELIVERED_STATUS_MESSAGES_COUNT, 0);
    }

    public long getTotalLostMessagesCount() {
        return this.getLostDataMessagesCount() + this.getLostStatusMessagesCount();
    }

    public long getLostDataMessagesCount() {
        long result = this.getTotalDataMessagesCount() - this.getDeliveredDataMessagesCount();
        return result > 0 ? result : 0;
    }

    public long getLostStatusMessagesCount() {
        long result = this.getTotalStatusMessagesCount() - this.getDeliveredStatusMessagesCount();
        return result > 0 ? result : 0;
    }

    public void setPMPowerValue(String deviceId, int powerValue) {
        if (powerValue < 0) {
            return;
        }
        this.setIntPreference(POWER_DEVICE + "p" + deviceId, powerValue);
    }

    public int getPMPowerValue(String deviceId) {
        return this.getIntPreference(POWER_DEVICE + "p" + deviceId, 0);
    }

    public void increasePMPowerValue(String deviceId) {
        this.setPMPowerValue(deviceId, getPMPowerValue(deviceId) + 10);
    }

    public void decreasePMPowerValue(String deviceId) {
        int currentPower = getPMPowerValue(deviceId);
        this.setPMPowerValue(deviceId, currentPower - 10 < 0 ? 0 : getPMPowerValue(deviceId) - 10);
    }


    public void setCadenceValue(String deviceId, int powerValue) {
        if (powerValue < 0) {
            return;
        }
        this.setIntPreference(POWER_DEVICE + "c" + deviceId, powerValue);
    }

    public int getCadenceValue(String deviceId) {
        return this.getIntPreference(POWER_DEVICE + "c" + deviceId, 0);
    }

    public void increaseCadenceValue(String deviceId) {
        this.setCadenceValue(deviceId, getCadenceValue(deviceId) + 10);
    }

    public void decreaseCadenceValue(String deviceId) {
        int currentPower = getPMPowerValue(deviceId);
        this.setCadenceValue(deviceId, currentPower - 10 < 0 ? 0 : getCadenceValue(deviceId) - 10);
    }


    public String[] getIoTCredentials() {
        String rawData = this.getStringPreference(IOT_CREDENTIALS);
        if (rawData != null) {
            return rawData.split(" ");
        }

        return null;
    }

    public void setIoTCredentials(String id, String token) {
        this.setStringPreference(IOT_CREDENTIALS, id + " " + token);
    }

    public void clearIoTCredentials() {
        this.clearPreferenceValue(IOT_CREDENTIALS);
    }

    public String getPowerMeterSlope(String deviceId) {
        return this.getStringPreference(deviceId + PM_SLOPE_POSTFIX);
    }

    public void setPowerMeterSlope(DevicesListItem pm, String slope) {
        this.setStringPreference(pm.getNumber() + PM_SLOPE_POSTFIX, slope);
    }

    public boolean iotSettingsReconnectRequired() {
        return this.getBooleanPreference("IoTReconnection", false);
    }

    public void setIotSettingsReconnectRequired(boolean isRequired) {
        this.setBooleanPreference("IoTReconnection", isRequired);
    }

    public void setLastCalibrationMessageForDevice(String deviceId, String message) {
        this.setStringPreference(CALIBRATION_MESSAGE + deviceId, message);
    }

    public String getLastCalibrationMessageForDevice(String deviceId) {
        return this.getStringPreference(CALIBRATION_MESSAGE + deviceId);
    }

    public void setDataMessageQoS(int qos) {
        this.setIntPreference(DATA_MSG_QOS, qos);
    }

    public int getDataMessageQoS() {
        return this.getIntPreference(DATA_MSG_QOS, 0);
    }

    public void setStatusMessageQoS(int qos) {
        this.setIntPreference(STATUS_MSG_QOS, qos);
    }

    public int getStatusMessageQoS() {
        return this.getIntPreference(STATUS_MSG_QOS, 0);
    }
}
