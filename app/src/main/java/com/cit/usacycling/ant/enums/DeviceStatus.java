package com.cit.usacycling.ant.enums;

/**
 * Created by nikolay.nikolov on 30.10.2015
 */
public enum DeviceStatus {
    PAIRED("PAIRED"),
    NOT_PAIRED("NOT PAIRED");

    private final String status;

    DeviceStatus(String state) {
        this.status = state;
    }

    public String toString() {
        return this.status;
    }
}
