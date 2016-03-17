package com.cit.usacycling.ant.enums;

import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;

/**
 * Created by nikolay.nikolov on 18.11.2015
 */
public enum BSXDeviceConnection {
    CONNECTED(DeviceState.TRACKING.getIntValue(), "CONNECTED"),
    STARTING(DeviceState.PROCESSING_REQUEST.getIntValue(), "STARTING"),
    CONNECTING(DeviceState.PROCESSING_REQUEST.getIntValue(), "CONNECTING"),
    TURNING_OFF(DeviceState.CLOSED.getIntValue(), "TURNING OFF"),
    DISCONNECTED(DeviceState.DEAD.getIntValue(), "DISCONNECTED"),
    UNKNOWN(DeviceState.UNRECOGNIZED.getIntValue(), "UNKNOWN");

    private String state;
    private int stateId;

    BSXDeviceConnection(int stateId, String state) {
        this.stateId = stateId;
        this.state = state;
    }

    public static String getStateById(int id) {
        if (id == DeviceState.TRACKING.getIntValue()) {
            return CONNECTED.toString();
        } else {
            return DISCONNECTED.toString();
        }
    }

    public int getConnectionId() {
        return this.stateId;
    }

    public String toString() {
        return this.state;
    }
}
