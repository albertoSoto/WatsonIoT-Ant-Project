package com.cit.usacycling.ant.enums;

/**
 * Created by nikolay.nikolov on 29.1.2016
 */
public enum CloudantDeviceTypeMap {
    HEARTRATE(0),
    POWER(1),
    SMO2(2);

    private final int dataType;

    CloudantDeviceTypeMap(int dataType) {
        this.dataType = dataType;
    }

    public int getDataTypeId() {
        return this.dataType;
    }
}
