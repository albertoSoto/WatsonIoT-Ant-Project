package com.cit.usacycling.ant.enums;

/**
 * Created by nikolay.nikolov on 04.11.2015
 */
public enum DeviceDataType {
    HEARTRATE("h"),
    POWER("p"),
    CADENCE("c"),
    TORQUE("t"),
    SMO2("o"),
    RRINTERVAL("r");

    private final String dataType;

    DeviceDataType(String dataType) {
        this.dataType = dataType;
    }

    public static boolean compare(DeviceDataType firstType, DeviceDataType secondType) {
        return (firstType.toString().equals(secondType.toString()));
    }

    public String toString() {
        return this.dataType;
    }
}
