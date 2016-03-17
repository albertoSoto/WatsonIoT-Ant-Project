package com.cit.usacycling.ant.background;

import com.cit.usacycling.ant.enums.DeviceDataType;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceType;

/**
 * Created by nikolay.nikolov on 3.11.2015
 */
public class DeviceDataTypeStruct {
    private final String deviceId;
    private final String deviceType;
    private final DeviceDataType dataType;

    private int hash;

    public DeviceDataTypeStruct(String deviceId, DeviceType deviceType, DeviceDataType dataType) {
        this.deviceId = deviceId;
        this.deviceType = deviceType.toString();
        this.dataType = dataType;
        this.hash = deviceType.getIntValue();
    }

    public DeviceDataTypeStruct(String deviceId, String deviceType, DeviceDataType dataType) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.dataType = dataType;
        this.hash = 9999;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public DeviceDataType getDataType() {
        return dataType;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DeviceDataTypeStruct) {
            DeviceDataTypeStruct comparer = (DeviceDataTypeStruct) o;
            return this.deviceId.equals(comparer.getDeviceId())
                    && (this.deviceType.equals(comparer.getDeviceType())
                    && DeviceDataType.compare(dataType, comparer.getDataType()));
        } else {
            return super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        return hash
                + this.deviceId.hashCode()
                + (this.deviceType + this.dataType).hashCode();
    }
}
