package com.cit.usacycling.ant.enums;

/**
 * Created by nikolay.nikolov on 20.10.2015
 */
public enum ServiceState {
    ENABLED("ENABLED"),
    DISABLED("DISABLED");

    private final String state;

    ServiceState(String state) {
        this.state = state;
    }

    public String toString() {
        return this.state;
    }
}
