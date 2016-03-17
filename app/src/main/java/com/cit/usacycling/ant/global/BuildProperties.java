package com.cit.usacycling.ant.global;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.widget.Toast;

import com.cit.usacycling.ant.USACyclingApplication;
import com.cit.usacycling.ant.background.websocket.Notifiable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;

import javax.inject.Inject;

/**
 * Created by nikolay.nikolov on 25.11.2015
 */
public class BuildProperties implements Notifiable {
    private boolean isMockRequired;
    private String applicationName;
    private String organizationId;
    private String userIdValue;
    private String deviceType;
    private String[] deviceIds;
    private String hostValue;
    private String[] devicePasswordValues;
    private String clientIdValue;
    private String portValue;
    private int statusMessageQoS;
    private int dataMessageQoS;
    private String selectedDevicePass;
    private boolean areCredentialsChanged;
    private String selectedDeviceId;
    private String backendAddress;
    private long statusPushInterval;
    private long dataPushInterval;
    private int mqttMessageMaxPayloadLength;
    Properties properties;
    InputStream inStream;

    @Inject
    SharedSettings settings;
    @Inject
    CToast cToast;

    public BuildProperties() {
        USACyclingApplication.getObjectGraph().inject(this);
        try {
            areCredentialsChanged = false;
            initializeStream();
            this.selectedDeviceId = this.properties.getProperty("deviceIds");
            this.selectedDevicePass = this.properties.getProperty("devicePasswordValues");
            this.backendAddress = this.properties.getProperty("backendAddress");
            this.isMockRequired = Boolean.parseBoolean(this.properties.getProperty("mockData"));
            this.applicationName = this.properties.getProperty("applicationName");
            this.organizationId = this.properties.getProperty("organizationId");
            this.userIdValue = this.properties.getProperty("userIdValue");
            this.deviceType = this.properties.getProperty("deviceType");
            this.hostValue = getOrganizationId() + this.properties.getProperty("hostPostfix");
            updateClientIdValue(selectedDeviceId);
            this.portValue = this.properties.getProperty("portValue");
            this.statusPushInterval = Long.parseLong(this.properties.getProperty("statusPushInterval"));
            this.dataPushInterval = Long.parseLong(this.properties.getProperty("dataPushInterval"));
            this.mqttMessageMaxPayloadLength = Integer.parseInt(this.properties.getProperty("mqttMessageMaxPayloadLength"));
            this.statusMessageQoS = Integer.parseInt(this.properties.getProperty("statusMessageQoS"));
            this.dataMessageQoS = Integer.parseInt(this.properties.getProperty("dataMessageQos"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (this.inStream != null) {
                try {
                    this.inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateClientIdValue(String deviceId) {
        this.clientIdValue = "d:" + getOrganizationId() + ":" + getDeviceType() + ":" + deviceId;
    }

    public boolean areCredentialsChanged() {
        return areCredentialsChanged;
    }

    public void credentialsChangeAqquired() {
        areCredentialsChanged = false;
    }

    public void setSelectedDeviceId(String selectedDeviceId) {
        areCredentialsChanged = true;
        updateClientIdValue(selectedDeviceId);
        this.selectedDeviceId = selectedDeviceId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public void setUserIdValue(String userIdValue) {
        this.userIdValue = userIdValue;
    }

    public void setHostValue(String hostValue) {
        this.hostValue = hostValue;
    }

    public void setClientIdValue(String clientIdValue) {
        this.clientIdValue = clientIdValue;
    }

    public void setPortValue(String portValue) {
        this.portValue = portValue;
    }

    public String getBackendAddress() {
        return this.backendAddress;
    }

    public String getSelectedDevicePass() {
        return selectedDevicePass;
    }

    public void setSelectedDevicePass(String selectedDevicePass) {
        areCredentialsChanged = true;
        this.selectedDevicePass = selectedDevicePass;
    }

    public boolean isMockRequired() {
        return isMockRequired;
    }

    public String getPortValue() {
        return portValue;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getUserIdValue() {
        return userIdValue;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String[] getDeviceIds() {
        return deviceIds;
    }

    public String[] getDevicePasswordValues() {
        return devicePasswordValues;
    }

    public String getHostValue() {
        return hostValue;
    }

    public String getClientIdValue() {
        return clientIdValue;
    }

    public long getStatusPushInterval() {
        return statusPushInterval;
    }

    public long getDataPushInterval() {
        return dataPushInterval;
    }

    public void setDataPushInterval(long interval) {
        dataPushInterval = interval;
    }

    public void setStatusPushInterval(long interval) {
        statusPushInterval = interval;
    }

    public int getMqttMessageMaxPayloadLength() {
        return mqttMessageMaxPayloadLength;
    }

    public void setMqttMessageMaxPayloadLength(int mqttMessageMaxPayloadLength) {
        this.mqttMessageMaxPayloadLength = mqttMessageMaxPayloadLength;
    }

    public void setStatusMessageQoS(int qos) {
        statusMessageQoS = qos;
    }

    public void setDataMessageQoS(int qos) {
        dataMessageQoS = qos;
    }

    public int getStatusMessageQoS() {
        return statusMessageQoS;
    }

    public int getDataMessageQoS() {
        return dataMessageQoS;
    }

    public String getSelectedDeviceId() {
        return this.selectedDeviceId;
    }

    public void setDeviceIds(String[] deviceIds) {
        this.deviceIds = deviceIds;
    }

    public void setDevicePasswordValues(String[] passwordValues) {
        this.devicePasswordValues = passwordValues;
    }

    private void initializeStream() throws IOException {
        this.properties = new Properties();
        String FILE_NAME = "build.properties";
        this.inStream = getClass().getClassLoader().getResourceAsStream("assets/" + FILE_NAME);

        if (this.inStream != null) {
            properties.load(inStream);
        } else {
            throw new FileNotFoundException("property file '" + FILE_NAME + "' not found'");
        }
    }

    int requestIds = 1;
    HashSet<String> sentRequests = new HashSet<>();
    ProgressDialog completeDialog;


    @Override
    public void notifyForResponse(String id, final int statusCode, final String message, final Activity context) {
        JSONArray content = null;
        try {
            content = new JSONObject(message).getJSONArray("content");
        } catch (JSONException e) {
            Log.e("BuildProperties", e.toString());
            e.printStackTrace();
        } finally {
            if (content == null) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cToast.makeText("Invalid response format. Could not parse android devices", Toast.LENGTH_SHORT);
                    }
                });
            }
        }

        if (content == null) {
            return;
        }

        if (ResponseCode.OK.compare(statusCode)) {
            this.deviceIds = new String[content.length()];
            this.devicePasswordValues = new String[content.length()];
            for (int i = 0; i < content.length(); i++) {
                try {
                    JSONObject json = (JSONObject) content.get(i);
                    this.deviceIds[i] = json.getString("identifier");
                    this.devicePasswordValues[i] = json.getString("token");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cToast.makeText("Android devices extracted successfully", Toast.LENGTH_SHORT);
                }
            });

        } else {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cToast.makeText("Could not extract android devices due to error: Code: " + statusCode, Toast.LENGTH_SHORT);
                }
            });
        }

        sentRequests.remove(id);
        completeDialog.dismiss();
    }
}
