package com.cit.usacycling.ant.background.db;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public class MqttConfiguration {
    private String deviceId;
    private String deviceToken;
    private String organizationId;
    private String userIdValue;
    private String hostPostfix;
    private String portValue;
    private int statusMessageQoS;
    private int dataMessageQoS;
    private int isChecked;

    public MqttConfiguration(String deviceId, String deviceToken, String organizationId, String userIdValue, String hostPostfix, String portValue, int statusMessageQoS, int dataMessageQoS, int isChecked) {
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.organizationId = organizationId;
        this.userIdValue = userIdValue;
        this.hostPostfix = hostPostfix;
        this.portValue = portValue;
        this.statusMessageQoS = statusMessageQoS;
        this.dataMessageQoS = dataMessageQoS;
        this.isChecked = isChecked;
    }

    public int getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(int isChecked) {
        this.isChecked = isChecked;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getDataMessageQoS() {
        return dataMessageQoS;
    }

    public void setDataMessageQoS(int dataMessageQoS) {
        this.dataMessageQoS = dataMessageQoS;
    }

    public int getStatusMessageQoS() {
        return statusMessageQoS;
    }

    public void setStatusMessageQoS(int statusMessageQoS) {
        this.statusMessageQoS = statusMessageQoS;
    }

    public String getPortValue() {
        return portValue;
    }

    public void setPortValue(String portValue) {
        this.portValue = portValue;
    }

    public String getHostPostfix() {
        return hostPostfix;
    }

    public void setHostPostfix(String hostPostfix) {
        this.hostPostfix = hostPostfix;
    }

    public String getUserIdValue() {
        return userIdValue;
    }

    public void setUserIdValue(String userIdValue) {
        this.userIdValue = userIdValue;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }
}
